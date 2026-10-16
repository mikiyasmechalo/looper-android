package com.example.looper_android.ui.player

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.looper_android.data.db.LibraryDao
import com.example.looper_android.data.db.SongEntity
import com.example.looper_android.data.models.LyricLine
import com.example.looper_android.data.repository.AudioTagReader
import com.example.looper_android.data.repository.AudioTagWriter
import com.example.looper_android.data.repository.LyricsCache
import com.example.looper_android.data.repository.LyricsFetcher
import com.example.looper_android.data.repository.LyricsParser
import com.example.looper_android.data.repository.SettingsRepository
import com.example.looper_android.data.repository.WriteResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

import dagger.hilt.android.qualifiers.ApplicationContext

data class LyricsUiState(
    val isLoading: Boolean = false,
    val rawLyrics: String? = null,
    val isSynced: Boolean = false,
    val source: String = "None",
    val timingOffsetMs: Long = 0L,
    val isFetchingProvider: String? = null,
    val parsedLines: List<LyricLine> = emptyList(),
    val plainLines: List<String> = emptyList(),
    val userFeedbackMessage: String? = null,
    val showInternetPrompt: Boolean = false
)

@HiltViewModel
class LyricsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val lyricsFetcher: LyricsFetcher,
    private val lyricsCache: LyricsCache,
    private val audioTagReader: AudioTagReader,
    private val tagWriter: AudioTagWriter,
    private val libraryDao: LibraryDao,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LyricsUiState())
    val uiState: StateFlow<LyricsUiState> = _uiState.asStateFlow()

    private val _pendingPermission = MutableStateFlow<PendingWritePermission?>(null)
    val pendingPermission: StateFlow<PendingWritePermission?> = _pendingPermission.asStateFlow()

    private var currentSongId: Long = -1L
    private var currentSongKey: String? = null
    private var loadLyricsJob: kotlinx.coroutines.Job? = null

    private fun songKey(song: SongEntity): String {
        return if (song.id > 0L) "id_${song.id}" else "path_${song.path}_${song.title}"
    }

    private fun getTimingOffset(songId: Long, source: String): Long {
        val prefs = context.getSharedPreferences("lyrics_offsets", Context.MODE_PRIVATE)
        return prefs.getLong("offset_${songId}_${source.uppercase()}", 0L)
    }

    fun adjustTimingOffset(song: SongEntity, deltaMs: Long) {
        val currentOffset = _uiState.value.timingOffsetMs
        val newOffset = currentOffset + deltaMs
        val prefs = context.getSharedPreferences("lyrics_offsets", Context.MODE_PRIVATE)
        prefs.edit().putLong("offset_${song.id}_${_uiState.value.source.uppercase()}", newOffset).apply()

        _uiState.value = _uiState.value.copy(
            timingOffsetMs = newOffset
        )
    }

    fun loadLyricsForSong(song: SongEntity?, forceReload: Boolean = false) {
        if (song == null) {
            loadLyricsJob?.cancel()
            currentSongKey = null
            currentSongId = -1L
            _uiState.value = LyricsUiState(isLoading = false, rawLyrics = null)
            return
        }

        val newKey = songKey(song)
        if (newKey == currentSongKey && !forceReload && _uiState.value.rawLyrics != null) {
            return
        }

        // Immediately cancel any ongoing fetch for the previous song
        loadLyricsJob?.cancel()
        currentSongKey = newKey
        currentSongId = song.id

        // Immediate Cache Check - If available, display instantly without showing a loading screen!
        val cached = if (!forceReload) lyricsFetcher.getCachedLyricsSync(song) else null
        if (cached != null) {
            val parsed = LyricsParser.parseLyrics(cached, (song.duration ?: 180000).toLong())
            val source = lyricsFetcher.extractLyricsSource(cached)
            val offset = getTimingOffset(song.id, source)
            _uiState.value = LyricsUiState(
                isLoading = false,
                rawLyrics = cached,
                isSynced = parsed.isSynced,
                source = source,
                timingOffsetMs = offset,
                parsedLines = parsed.timedLines,
                plainLines = parsed.plainLines
            )

            // If cached lyrics are NOT synced (and not user-picked), fetch online synced lyrics!
            if (!parsed.isSynced && !cached.contains("[picked:user]")) {
                loadLyricsJob = viewModelScope.launch {
                    val fetched = lyricsFetcher.fetchLyrics(song)
                    if (fetched != null && fetched != cached && currentSongKey == newKey) {
                        val newParsed = LyricsParser.parseLyrics(fetched, (song.duration ?: 180000).toLong())
                        val newSource = lyricsFetcher.extractLyricsSource(fetched)
                        val newOffset = getTimingOffset(song.id, newSource)
                        _uiState.value = _uiState.value.copy(
                            rawLyrics = fetched,
                            isSynced = newParsed.isSynced,
                            source = newSource,
                            timingOffsetMs = newOffset,
                            parsedLines = newParsed.timedLines,
                            plainLines = newParsed.plainLines
                        )
                    }
                }
            } else if (lyricsFetcher.canUpgrade(cached)) {
                loadLyricsJob = viewModelScope.launch {
                    val upgraded = lyricsFetcher.upgradeLyrics(song, cached)
                    if (upgraded != null && currentSongKey == newKey) {
                        val upParsed = LyricsParser.parseLyrics(upgraded, (song.duration ?: 180000).toLong())
                        val upSource = lyricsFetcher.extractLyricsSource(upgraded)
                        _uiState.value = _uiState.value.copy(
                            rawLyrics = upgraded,
                            isSynced = upParsed.isSynced,
                            source = upSource,
                            parsedLines = upParsed.timedLines,
                            plainLines = upParsed.plainLines
                        )
                    }
                }
            }
            return
        }

        // Fast Check 2: Immediately display synchronous local lyrics (sidecar, database, or provider cache)
        val localSync = if (!forceReload) lyricsFetcher.getLocalLyricsSync(song) else null
        if (localSync != null) {
            val parsed = LyricsParser.parseLyrics(localSync, (song.duration ?: 180000).toLong())
            val source = lyricsFetcher.extractLyricsSource(localSync)
            val offset = getTimingOffset(song.id, source)
            _uiState.value = LyricsUiState(
                isLoading = false,
                rawLyrics = localSync,
                isSynced = parsed.isSynced,
                source = source,
                timingOffsetMs = offset,
                parsedLines = parsed.timedLines,
                plainLines = parsed.plainLines
            )

            // If not synced, fetch online synced lyrics in background!
            if (!parsed.isSynced && !localSync.contains("[picked:user]")) {
                loadLyricsJob = viewModelScope.launch {
                    val fetched = lyricsFetcher.fetchLyrics(song)
                    if (fetched != null && fetched != localSync && currentSongKey == newKey) {
                        val newParsed = LyricsParser.parseLyrics(fetched, (song.duration ?: 180000).toLong())
                        val newSource = lyricsFetcher.extractLyricsSource(fetched)
                        val newOffset = getTimingOffset(song.id, newSource)
                        _uiState.value = _uiState.value.copy(
                            rawLyrics = fetched,
                            isSynced = newParsed.isSynced,
                            source = newSource,
                            timingOffsetMs = newOffset,
                            parsedLines = newParsed.timedLines,
                            plainLines = newParsed.plainLines
                        )
                    }
                }
            } else if (lyricsFetcher.canUpgrade(localSync)) {
                loadLyricsJob = viewModelScope.launch {
                    val upgraded = lyricsFetcher.upgradeLyrics(song, localSync)
                    if (upgraded != null && currentSongKey == newKey) {
                        val upParsed = LyricsParser.parseLyrics(upgraded, (song.duration ?: 180000).toLong())
                        val upSource = lyricsFetcher.extractLyricsSource(upgraded)
                        _uiState.value = _uiState.value.copy(
                            rawLyrics = upgraded,
                            isSynced = upParsed.isSynced,
                            source = upSource,
                            parsedLines = upParsed.timedLines,
                            plainLines = upParsed.plainLines
                        )
                    }
                }
            }
            return
        }

        loadLyricsJob = viewModelScope.launch {
            // Check embedded tags from audio file in background
            val embedded = if (!forceReload) lyricsFetcher.getLocalOrEmbeddedLyrics(song) else null
            if (embedded != null) {
                val parsed = LyricsParser.parseLyrics(embedded, (song.duration ?: 180000).toLong())
                val source = lyricsFetcher.extractLyricsSource(embedded)
                val offset = getTimingOffset(song.id, source)
                _uiState.value = LyricsUiState(
                    isLoading = false,
                    rawLyrics = embedded,
                    isSynced = parsed.isSynced,
                    source = source,
                    timingOffsetMs = offset,
                    parsedLines = parsed.timedLines,
                    plainLines = parsed.plainLines
                )

                if (parsed.isSynced || embedded.contains("[picked:user]")) {
                    if (lyricsFetcher.canUpgrade(embedded)) {
                        val upgraded = lyricsFetcher.upgradeLyrics(song, embedded)
                        if (upgraded != null && currentSongKey == newKey) {
                            val upParsed = LyricsParser.parseLyrics(upgraded, (song.duration ?: 180000).toLong())
                            val upSource = lyricsFetcher.extractLyricsSource(upgraded)
                            _uiState.value = _uiState.value.copy(
                                rawLyrics = upgraded,
                                isSynced = upParsed.isSynced,
                                source = upSource,
                                parsedLines = upParsed.timedLines,
                                plainLines = upParsed.plainLines
                            )
                        }
                    }
                    return@launch
                }
            } else {
                // Only show loading screen when no cached, local, or embedded lyrics exist at all
                _uiState.value = LyricsUiState(
                    isLoading = true,
                    rawLyrics = null,
                    isSynced = false,
                    source = "None",
                    parsedLines = emptyList(),
                    plainLines = emptyList()
                )
            }

            val raw = lyricsFetcher.fetchLyrics(song)
            if (currentSongKey != newKey) return@launch

            if (raw != null) {
                val parsed = LyricsParser.parseLyrics(raw, (song.duration ?: 180000).toLong())
                val source = lyricsFetcher.extractLyricsSource(raw)
                val offset = getTimingOffset(song.id, source)
                _uiState.value = LyricsUiState(
                    isLoading = false,
                    rawLyrics = raw,
                    isSynced = parsed.isSynced,
                    source = source,
                    timingOffsetMs = offset,
                    parsedLines = parsed.timedLines,
                    plainLines = parsed.plainLines
                )

                if (lyricsFetcher.canUpgrade(raw)) {
                    val upgraded = lyricsFetcher.upgradeLyrics(song, raw)
                    if (upgraded != null && currentSongKey == newKey) {
                        val upParsed = LyricsParser.parseLyrics(upgraded, (song.duration ?: 180000).toLong())
                        val upSource = lyricsFetcher.extractLyricsSource(upgraded)
                        _uiState.value = _uiState.value.copy(
                            rawLyrics = upgraded,
                            isSynced = upParsed.isSynced,
                            source = upSource,
                            parsedLines = upParsed.timedLines,
                            plainLines = upParsed.plainLines
                        )
                    }
                }
            } else if (_uiState.value.rawLyrics == null) {
                _uiState.value = LyricsUiState(
                    isLoading = false,
                    rawLyrics = null,
                    isSynced = false,
                    source = "None",
                    parsedLines = emptyList(),
                    plainLines = emptyList()
                )
            }
        }
    }

    /**
     * Refetches lyrics from a specific user-selected provider (e.g. Genius, LyricFind, LRCLIB, etc.)
     */
    fun refetchWithProvider(song: SongEntity, provider: String) {
        viewModelScope.launch {
            val isInternetEnabled = settingsRepository.enableInternet.first()
            val isOnlineProvider = LyricsFetcher.ALL_ONLINE_PROVIDERS.any { it.equals(provider, ignoreCase = true) }

            if (isOnlineProvider && !isInternetEnabled) {
                _uiState.value = _uiState.value.copy(
                    isFetchingProvider = null,
                    showInternetPrompt = true,
                    userFeedbackMessage = "Internet access is disabled in Settings. Enable 'Use Internet' to fetch online lyrics."
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(isFetchingProvider = provider)

            val raw = lyricsFetcher.fetchFromSpecificProvider(song, provider)
            if (raw != null) {
                val parsed = LyricsParser.parseLyrics(raw, (song.duration ?: 180000).toLong())
                val source = lyricsFetcher.extractLyricsSource(raw)
                val offset = getTimingOffset(song.id, source)
                _uiState.value = _uiState.value.copy(
                    rawLyrics = raw,
                    isSynced = parsed.isSynced,
                    source = source,
                    timingOffsetMs = offset,
                    parsedLines = parsed.timedLines,
                    plainLines = parsed.plainLines,
                    isFetchingProvider = null,
                    userFeedbackMessage = "Updated lyrics from $provider"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isFetchingProvider = null,
                    userFeedbackMessage = "No lyrics found on $provider"
                )
            }
        }
    }

    /**
     * Applies custom lyrics text from an imported local file (.lrc / .txt)
     */
    fun applyCustomLyrics(song: SongEntity, customText: String) {
        if (customText.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            val tagged = if (customText.startsWith("[source:")) customText else "[source:custom]\n$customText"
            lyricsCache.save(song.artist, song.title, tagged)
            lyricsCache.saveProvider(song.artist, song.title, "Custom", tagged)
            if (song.id > 0L) {
                libraryDao.updateSongLyrics(song.id, tagged)
            }
            if (song.path.isNotBlank()) {
                libraryDao.updateSongLyricsByPath(song.path, tagged)
            }
            val parsed = LyricsParser.parseLyrics(tagged, (song.duration ?: 180000).toLong())

            _uiState.value = _uiState.value.copy(
                rawLyrics = tagged,
                isSynced = parsed.isSynced,
                source = "Custom LRC",
                parsedLines = parsed.timedLines,
                plainLines = parsed.plainLines,
                userFeedbackMessage = "Custom lyrics loaded successfully"
            )
        }
    }

    /**
     * Embeds current active lyrics into the audio file metadata tags.
     */
    fun embedLyricsToAudioFile(song: SongEntity) {
        val raw = _uiState.value.rawLyrics
        if (raw.isNullOrBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            val edit = AudioTagWriter.TagEdit(
                title = song.title,
                artist = song.artist,
                album = song.album,
                albumArtist = song.albumArtist,
                year = song.year,
                genre = song.genre,
                lyrics = raw,
                trackNumber = song.trackNumber
            )
            val result = tagWriter.write(song.path, edit)
            withContext(Dispatchers.Main) {
                when (result) {
                    is WriteResult.Success -> {
                        val tagged = if (raw.startsWith("[source:")) raw else "[source:embedded]\n$raw"
                        viewModelScope.launch(Dispatchers.IO) {
                            libraryDao.updateSongLyrics(song.id, tagged)
                        }
                        _uiState.value = _uiState.value.copy(
                            source = "Embedded",
                            userFeedbackMessage = "Embedded lyrics into audio file tags successfully!"
                        )
                    }
                    is WriteResult.RequiresPermission -> {
                        _pendingPermission.value = PendingWritePermission(
                            intentSender = result.intentSender,
                            pendingSong = song,
                            pendingEdit = edit
                        )
                    }
                    is WriteResult.Failure -> {
                        _uiState.value = _uiState.value.copy(
                            userFeedbackMessage = "Failed to embed lyrics: ${result.error}"
                        )
                    }
                }
            }
        }
    }

    fun onPermissionGranted() {
        val pending = _pendingPermission.value ?: return
        _pendingPermission.value = null
        val raw = pending.pendingEdit.lyrics ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val result = tagWriter.write(pending.pendingSong.path, pending.pendingEdit)
            withContext(Dispatchers.Main) {
                if (result is WriteResult.Success) {
                    val tagged = if (raw.startsWith("[source:")) raw else "[source:embedded]\n$raw"
                    viewModelScope.launch(Dispatchers.IO) {
                        libraryDao.updateSongLyrics(pending.pendingSong.id, tagged)
                    }
                    _uiState.value = _uiState.value.copy(
                        source = "Embedded",
                        userFeedbackMessage = "Embedded lyrics into audio file tags successfully!"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        userFeedbackMessage = "Failed to embed lyrics into file."
                    )
                }
            }
        }
    }

    fun onPermissionDenied() {
        _pendingPermission.value = null
        _uiState.value = _uiState.value.copy(
            userFeedbackMessage = "Write permission was denied."
        )
    }

    /**
     * Exports active lyrics to a true .lrc file in Downloads/Looper/Lyrics/ and attempts sidecar writing.
     */
    fun exportLyricsToLrcFile(song: SongEntity, context: Context): Boolean {
        val raw = _uiState.value.rawLyrics ?: return false
        val cleanLrc = buildExportLrcContent(song, raw)
        val safeArtist = (song.artist ?: "Unknown Artist").replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
        val safeTitle = song.title.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
        val fileName = "$safeArtist - $safeTitle.lrc"

        var savedSidecar = trySaveSidecarLrc(song, cleanLrc, context)
        var savedDownloads = false

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Looper/Lyrics")
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(cleanLrc.toByteArray()) }
                    savedDownloads = true
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val looperDir = File(downloadsDir, "Looper/Lyrics").apply { if (!exists()) mkdirs() }
                val destFile = File(looperDir, fileName)
                destFile.writeText(cleanLrc)
                savedDownloads = true
            }
        } catch (e: Exception) {
            Log.e("LyricsViewModel", "Failed writing to Downloads", e)
        }

        if (savedDownloads || savedSidecar) {
            val msg = when {
                savedDownloads && savedSidecar -> "Exported to Downloads/Looper/Lyrics/$fileName and saved sidecar beside song."
                savedDownloads -> "Exported to Downloads/Looper/Lyrics/$fileName"
                else -> "Saved sidecar .lrc beside song file."
            }
            _uiState.value = _uiState.value.copy(userFeedbackMessage = msg)
            return true
        } else {
            _uiState.value = _uiState.value.copy(userFeedbackMessage = "Failed to export lyrics file")
            return false
        }
    }

    private fun trySaveSidecarLrc(song: SongEntity, cleanLrc: String, context: Context): Boolean {
        return try {
            if (song.path.startsWith("content://")) {
                val uri = android.net.Uri.parse(song.path)
                val doc = androidx.documentfile.provider.DocumentFile.fromSingleUri(context, uri)
                val parent = doc?.parentFile
                if (parent != null && parent.canWrite()) {
                    val baseName = doc.name?.substringBeforeLast('.') ?: "lyrics"
                    val existing = parent.findFile("$baseName.lrc")
                    val lrcFile = existing ?: parent.createFile("application/octet-stream", "$baseName.lrc")
                    if (lrcFile != null) {
                        context.contentResolver.openOutputStream(lrcFile.uri, "wt")?.use { it.write(cleanLrc.toByteArray()) }
                        return true
                    }
                }
                false
            } else {
                val songFile = File(song.path)
                val parent = songFile.parentFile
                if (parent != null && parent.exists()) {
                    val baseName = songFile.nameWithoutExtension
                    val sidecar = File(parent, "$baseName.lrc")
                    sidecar.writeText(cleanLrc)
                    sidecar.exists()
                } else false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun buildExportLrcContent(song: SongEntity, rawLyrics: String): String {
        val lines = rawLyrics.lines()
        val contentLines = lines.filter { !it.trim().startsWith("[source:") }
        val sb = StringBuilder()

        if (song.title.isNotBlank()) sb.append("[ti:${song.title.trim()}]\n")
        if (!song.artist.isNullOrBlank()) sb.append("[ar:${song.artist.trim()}]\n")
        if (!song.album.isNullOrBlank()) sb.append("[al:${song.album.trim()}]\n")
        if (song.duration != null && song.duration > 0) {
            val totalMinutes = song.duration / 60000
            val seconds = (song.duration % 60000) / 1000
            val centiseconds = (song.duration % 1000) / 10
            sb.append("[length:${String.format("%02d:%02d.%02d", totalMinutes, seconds, centiseconds)}]\n")
        }
        sb.append("\n")
        sb.append(contentLines.joinToString("\n"))
        return sb.toString().trim()
    }

    fun dismissInternetPrompt() {
        _uiState.value = _uiState.value.copy(showInternetPrompt = false)
    }

    fun clearUserFeedbackMessage() {
        _uiState.value = _uiState.value.copy(userFeedbackMessage = null)
    }
}
