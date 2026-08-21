package com.example.looper_android.ui.library
import kotlinx.coroutines.flow.first

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.looper_android.data.db.LibraryDao
import com.example.looper_android.data.db.SongEntity
import com.example.looper_android.data.repository.AudioFileDeleter
import com.example.looper_android.data.repository.AudioTagWriter
import com.example.looper_android.data.repository.DeleteResult
import com.example.looper_android.data.repository.LibraryCacheManager
import com.example.looper_android.data.repository.SafPermissionRepository
import com.example.looper_android.data.repository.SettingsRepository
import com.example.looper_android.data.repository.WriteResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

import android.content.IntentSender
import com.example.looper_android.data.repository.DeleteBatchResult

data class MassOperationProgress(
    val isRunning: Boolean = false,
    val title: String = "",
    val current: Int = 0,
    val total: Int = 0
)

@HiltViewModel
class CollectionDetailViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val libraryDao: LibraryDao,
    private val tagWriter: AudioTagWriter,
    private val audioFileDeleter: AudioFileDeleter,
    private val safPermissionRepo: SafPermissionRepository,
    private val artworkService: com.example.looper_android.data.repository.ArtworkService,
    private val artistBioService: com.example.looper_android.data.repository.ArtistBioService,
    private val libraryCacheManager: LibraryCacheManager,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val prefs = context.getSharedPreferences("library_prefs", Context.MODE_PRIVATE)

    val isDbLoaded = libraryCacheManager.isDbLoaded
    val artists = libraryCacheManager.artists

    private val _artistBiography = MutableStateFlow<String?>(null)
    val artistBiography: StateFlow<String?> = _artistBiography.asStateFlow()

    fun fetchArtistBiography(artistName: String) {
        viewModelScope.launch {
            _artistBiography.value = artistBioService.getArtistBiography(artistName)
        }
    }

    fun fetchArtistImage(artistName: String) {
        viewModelScope.launch {
            artworkService.getArtistImageUrl(artistName)
        }
    }

    private val _pendingBatchDeletePermission = MutableStateFlow<PendingBatchDeletePermission?>(null)
    val pendingBatchDeletePermission: StateFlow<PendingBatchDeletePermission?> = _pendingBatchDeletePermission.asStateFlow()

    private val _title = MutableStateFlow("")
    private val _type = MutableStateFlow("")
    private val _sortOption = MutableStateFlow(SongSortOption.TRACK_NUMBER)
    private val _isAscending = MutableStateFlow(true)

    val sortOption: StateFlow<SongSortOption> = _sortOption.asStateFlow()
    val isAscending: StateFlow<Boolean> = _isAscending.asStateFlow()
    val currentTitle: StateFlow<String> = _title.asStateFlow()

    private val _progressState = MutableStateFlow(MassOperationProgress())
    val progressState: StateFlow<MassOperationProgress> = _progressState.asStateFlow()

    fun initCollection(title: String, type: String) {
        _title.value = title
        _type.value = type
        if (type.equals("artist", ignoreCase = true)) {
            fetchArtistBiography(title)
        } else {
            _artistBiography.value = null
        }

        val defaultSort = when (type.lowercase()) {
            "album" -> SongSortOption.TRACK_NUMBER
            "artist" -> SongSortOption.ALBUM
            else -> SongSortOption.TITLE
        }
        val defaultAsc = true
        val savedSortName = prefs.getString("collection_sort_option_${type.lowercase()}", defaultSort.name)
        val savedAsc = prefs.getBoolean("collection_sort_ascending_${type.lowercase()}", defaultAsc)
        val sort = try {
            SongSortOption.valueOf(savedSortName ?: defaultSort.name)
        } catch (_: Exception) { defaultSort }

        _sortOption.value = sort
        _isAscending.value = savedAsc
    }

    fun setSortOption(option: SongSortOption) {
        _sortOption.value = option
        val currentType = _type.value.lowercase()
        if (currentType.isNotEmpty()) {
            prefs.edit().putString("collection_sort_option_$currentType", option.name).apply()
        }
    }

    fun toggleAscending() {
        val next = !_isAscending.value
        _isAscending.value = next
        val currentType = _type.value.lowercase()
        if (currentType.isNotEmpty()) {
            prefs.edit().putBoolean("collection_sort_ascending_$currentType", next).apply()
        }
    }

    /**
     * Checks if all songs in this collection have granted SAF storage permission.
     */
    fun isCollectionSafCovered(songs: List<SongEntity>): Boolean {
        if (songs.isEmpty()) return true
        return songs.all { song ->
            song.path.startsWith("content://") || safPermissionRepo.isPathOrUriCovered(song.path) != null
        }
    }

    fun saveSafPermission(treeUri: Uri): Boolean {
        return safPermissionRepo.saveGrantedTreeUri(treeUri)
    }

    private fun savePickedArtworkToDisk(uri: Uri, albumName: String): String? {
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
            val coversDir = File(context.filesDir, "album_covers").apply { if (!exists()) mkdirs() }
            val sanitized = albumName.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val destFile = File(coversDir, "cover_${sanitized}_${System.currentTimeMillis()}.jpg")
            destFile.outputStream().use { it.write(bytes) }
            destFile.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    fun massEditAlbum(
        oldAlbumName: String,
        songs: List<SongEntity>,
        newAlbumName: String,
        newAlbumArtist: String?,
        newYear: Int?,
        newGenre: String?,
        pickedArtUri: Uri?,
        removeArtwork: Boolean,
        onComplete: (success: Boolean, newAlbumTitle: String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _progressState.value = MassOperationProgress(isRunning = true, title = "Updating Album Tags...", current = 0, total = songs.size)

            val artBytes = if (pickedArtUri != null) {
                try { context.contentResolver.openInputStream(pickedArtUri)?.use { it.readBytes() } } catch (_: Exception) { null }
            } else null
            val artMime = if (pickedArtUri != null) {
                context.contentResolver.getType(pickedArtUri) ?: "image/jpeg"
            } else null

            val newArtPath = when {
                removeArtwork -> null
                pickedArtUri != null -> savePickedArtworkToDisk(pickedArtUri, newAlbumName)
                else -> songs.firstOrNull { !it.artPath.isNullOrEmpty() }?.artPath
            }

            val updatedSongs = mutableListOf<SongEntity>()
            var successCount = 0

            songs.forEachIndexed { index, song ->
                _progressState.value = MassOperationProgress(isRunning = true, title = "Updating Album Tags (${index + 1}/${songs.size})", current = index + 1, total = songs.size)

                val edit = AudioTagWriter.TagEdit(
                    album = newAlbumName,
                    albumArtist = newAlbumArtist,
                    year = newYear,
                    genre = newGenre,
                    artworkBytes = artBytes,
                    artworkMimeType = artMime,
                    removeArtwork = removeArtwork
                )

                val writeResult = tagWriter.write(song.path, edit)
                if (writeResult is WriteResult.Success) {
                    successCount++
                }

                val updatedSong = song.copy(
                    album = newAlbumName,
                    albumArtist = newAlbumArtist,
                    year = newYear,
                    genre = newGenre,
                    artPath = if (removeArtwork) null else (newArtPath ?: song.artPath)
                )
                updatedSongs.add(updatedSong)
            }

            if (updatedSongs.isNotEmpty()) {
                libraryDao.updateSongs(updatedSongs)
                libraryDao.updateAlbumDetails(oldAlbumName, newAlbumName, newAlbumArtist, newArtPath)
            }

            _title.value = newAlbumName
            _progressState.value = MassOperationProgress(isRunning = false)
            withContext(Dispatchers.Main) {
                onComplete(true, newAlbumName)
            }
        }
    }

    fun massEditArtist(
        oldArtistName: String,
        songs: List<SongEntity>,
        newArtistName: String,
        pickedImageUri: Uri?,
        removeImage: Boolean,
        onComplete: (success: Boolean, newTitle: String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _progressState.value = MassOperationProgress(isRunning = true, title = "Updating Artist...", current = 0, total = songs.size)

            var newImagePath: String? = null
            if (removeImage) {
                libraryDao.updateArtistImageUrl(oldArtistName, null)
            } else if (pickedImageUri != null) {
                try {
                    val stream = context.contentResolver.openInputStream(pickedImageUri)
                    if (stream != null) {
                        val file = File(context.filesDir, "artist_${System.currentTimeMillis()}.jpg")
                        file.outputStream().use { out -> stream.copyTo(out) }
                        newImagePath = file.absolutePath
                        libraryDao.updateArtistImageUrl(oldArtistName, newImagePath)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val nameChanged = newArtistName.isNotBlank() && !newArtistName.equals(oldArtistName, ignoreCase = true)
            if (nameChanged) {
                val updatedSongs = mutableListOf<SongEntity>()
                songs.forEachIndexed { index, song ->
                    _progressState.value = _progressState.value.copy(current = index + 1)
                    val edit = com.example.looper_android.data.repository.AudioTagWriter.TagEdit(artist = newArtistName)
                    tagWriter.write(song.path, edit)
                    updatedSongs.add(song.copy(artist = newArtistName))
                }
                if (updatedSongs.isNotEmpty()) {
                    libraryDao.updateSongs(updatedSongs)
                }
                libraryDao.updateArtistName(oldArtistName, newArtistName)
                if (newImagePath != null) {
                    libraryDao.updateArtistImageUrl(newArtistName, newImagePath)
                }
                _title.value = newArtistName
            }

            _progressState.value = MassOperationProgress(isRunning = false)
            withContext(Dispatchers.Main) {
                onComplete(true, if (nameChanged) newArtistName else oldArtistName)
            }
        }
    }

    fun massDeleteAlbum(
        albumName: String,
        songs: List<SongEntity>,
        onComplete: (success: Boolean) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _progressState.value = MassOperationProgress(isRunning = true, title = "Deleting Album Songs...", current = 0, total = songs.size)

            val result = audioFileDeleter.deleteBatch(songs.map { it.path })
            when (result) {
                is DeleteBatchResult.Success -> {
                    val deletedPaths = result.deletedPaths.toSet()
                    val deletedSongs = if (deletedPaths.isEmpty()) songs else songs.filter { it.path in deletedPaths }
                    deletedSongs.forEach { song ->
                        libraryDao.deleteSongById(song.id)
                    }
                    libraryDao.deleteAlbumByName(albumName)
                    _progressState.value = MassOperationProgress(isRunning = false)
                    withContext(Dispatchers.Main) { onComplete(true) }
                }
                is DeleteBatchResult.RequiresPermission -> {
                    _pendingBatchDeletePermission.value = PendingBatchDeletePermission(
                        intentSender = result.intentSender,
                        pendingSongIds = songs.map { it.id }
                    )
                    _progressState.value = MassOperationProgress(isRunning = false)
                    withContext(Dispatchers.Main) { onComplete(false) }
                }
                is DeleteBatchResult.Failure -> {
                    _progressState.value = MassOperationProgress(isRunning = false)
                    withContext(Dispatchers.Main) { onComplete(false) }
                }
            }
        }
    }

    fun massDeleteArtist(
        artistName: String,
        songs: List<SongEntity>,
        onComplete: (success: Boolean) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _progressState.value = MassOperationProgress(isRunning = true, title = "Deleting Artist Songs...", current = 0, total = songs.size)

            val result = audioFileDeleter.deleteBatch(songs.map { it.path })
            when (result) {
                is DeleteBatchResult.Success -> {
                    val deletedPaths = result.deletedPaths.toSet()
                    val deletedSongs = if (deletedPaths.isEmpty()) songs else songs.filter { it.path in deletedPaths }
                    deletedSongs.forEach { song ->
                        libraryDao.deleteSongById(song.id)
                    }
                    libraryDao.deleteArtistByName(artistName)
                    _progressState.value = MassOperationProgress(isRunning = false)
                    withContext(Dispatchers.Main) { onComplete(true) }
                }
                is DeleteBatchResult.RequiresPermission -> {
                    _pendingBatchDeletePermission.value = PendingBatchDeletePermission(
                        intentSender = result.intentSender,
                        pendingSongIds = songs.map { it.id }
                    )
                    _progressState.value = MassOperationProgress(isRunning = false)
                    withContext(Dispatchers.Main) { onComplete(false) }
                }
                is DeleteBatchResult.Failure -> {
                    _progressState.value = MassOperationProgress(isRunning = false)
                    withContext(Dispatchers.Main) { onComplete(false) }
                }
            }
        }
    }

    fun onBatchDeletePermissionGranted(collectionName: String, isAlbum: Boolean) {
        val pending = _pendingBatchDeletePermission.value ?: return
        _pendingBatchDeletePermission.value = null
        viewModelScope.launch(Dispatchers.IO) {
            pending.pendingSongIds.forEach { songId ->
                libraryDao.deleteSongById(songId)
            }
            if (isAlbum) {
                libraryDao.deleteAlbumByName(collectionName)
            } else {
                libraryDao.deleteArtistByName(collectionName)
            }
        }
    }

    fun onBatchDeletePermissionDenied() {
        _pendingBatchDeletePermission.value = null
    }

    private data class CollectionConfig(
        val title: String,
        val type: String,
        val sort: SongSortOption,
        val asc: Boolean
    )

    val collectionSongs: StateFlow<List<SongEntity>> = combine(
        libraryCacheManager.songs,
        libraryCacheManager.playlists,
        settingsRepository.useAlbumArtist,
        combine(_title, _type, _sortOption, _isAscending) { title, type, sort, asc ->
            CollectionConfig(title, type, sort, asc)
        }
    ) { allSongs, playlists, useAlbumArtist, config ->
        val title = config.title
        val type = config.type
        val sort = config.sort
        val asc = config.asc
        if (title.isBlank() || type.isBlank()) return@combine emptyList()

        val filtered = when (type.lowercase()) {
            "album" -> allSongs.filter {
                val effectiveAlbum = it.album?.trim()?.ifBlank { null } ?: "Unknown Album"
                effectiveAlbum.equals(title, ignoreCase = true)
            }
            "artist" -> allSongs.filter {
                val artistMatch = it.artist?.trim()?.equals(title, ignoreCase = true) == true
                val albumArtistMatch = it.albumArtist?.trim()?.equals(title, ignoreCase = true) == true
                if (useAlbumArtist) {
                    albumArtistMatch || artistMatch
                } else {
                    artistMatch
                }
            }
            "genre" -> allSongs.filter {
                val effectiveGenre = it.genre?.trim()?.ifBlank { null } ?: "Unknown"
                effectiveGenre.equals(title, ignoreCase = true)
            }
            "folder" -> allSongs.filter {
                val parentName = java.io.File(it.path).parentFile?.name ?: ""
                parentName.equals(title, ignoreCase = true) || (java.io.File(it.path).parent ?: "").equals(title, ignoreCase = true)
            }
            "playlist" -> {
                val currentPlaylist = playlists.find { it.name.equals(title, ignoreCase = true) }
                if (currentPlaylist != null) {
                    val pathSet = try {
                        val json = org.json.JSONArray(currentPlaylist.songPaths)
                        val set = mutableSetOf<String>()
                        for (i in 0 until json.length()) {
                            set.add(json.optString(i))
                        }
                        set
                    } catch (_: Exception) {
                        emptySet()
                    }
                    allSongs.filter { pathSet.contains(it.path) }
                } else {
                    emptyList()
                }
            }
            else -> allSongs
        }

        val sorted = filtered.toMutableList()
        when (sort) {
            SongSortOption.TITLE -> sorted.sortBy { it.title.lowercase() }
            SongSortOption.ALBUM -> {
                sorted.sortWith(
                    compareBy<SongEntity> { (it.album ?: "").lowercase() }
                        .thenBy { it.trackNumber ?: 0 }
                        .thenBy { it.title.lowercase() }
                )
            }
            SongSortOption.TRACK_NUMBER -> sorted.sortBy { it.trackNumber ?: 0 }
            SongSortOption.DURATION -> sorted.sortByDescending { it.duration }
            SongSortOption.DATE_ADDED -> sorted.sortByDescending { it.dateAdded }
            SongSortOption.YEAR -> sorted.sortByDescending { it.year ?: 0 }
        }
        if (!asc) sorted.reverse()
        sorted
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun mergeArtists(
        sourceArtistName: String,
        targetArtistName: String,
        onComplete: (success: Boolean) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _progressState.value = MassOperationProgress(
                isRunning = true,
                title = "Merging artists...",
                current = 0,
                total = 0
            )

            val useAlbumArtist = settingsRepository.useAlbumArtist.first()
            val allSongs = libraryCacheManager.songs.first()

            // Find all songs that belong to the source artist according to the current grouping mode
            val songsToMerge = allSongs.filter { song ->
                val effectiveArtist = if (useAlbumArtist) {
                    song.albumArtist?.trim()?.ifBlank { null } ?: song.artist?.trim()?.ifBlank { null } ?: "Unknown Artist"
                } else {
                    song.artist?.trim()?.ifBlank { null } ?: "Unknown Artist"
                }
                effectiveArtist.equals(sourceArtistName, ignoreCase = true) ||
                song.artist?.equals(sourceArtistName, ignoreCase = true) == true ||
                song.albumArtist?.equals(sourceArtistName, ignoreCase = true) == true
            }

            _progressState.value = MassOperationProgress(
                isRunning = true,
                title = "Updating tags for ${songsToMerge.size} songs...",
                current = 0,
                total = songsToMerge.size
            )

            val updatedSongs = mutableListOf<SongEntity>()
            var successCount = 0

            songsToMerge.forEachIndexed { index, song ->
                _progressState.value = MassOperationProgress(
                    isRunning = true,
                    title = "Updating metadata (${index + 1}/${songsToMerge.size})",
                    current = index + 1,
                    total = songsToMerge.size
                )

                val newArtist: String?
                val newAlbumArtist: String?

                if (useAlbumArtist) {
                    // When 'Use Album Artist' is ON:
                    // Primary artist identity is governed by the Album Artist tag field
                    newAlbumArtist = when {
                        song.albumArtist?.equals(sourceArtistName, ignoreCase = true) == true -> targetArtistName
                        song.albumArtist.isNullOrBlank() && song.artist?.equals(sourceArtistName, ignoreCase = true) == true -> targetArtistName
                        else -> song.albumArtist
                    }
                    newArtist = when {
                        song.artist?.equals(sourceArtistName, ignoreCase = true) == true -> targetArtistName
                        else -> song.artist
                    }
                } else {
                    // When 'Use Album Artist' is OFF:
                    // Primary artist identity is governed by the Track Artist tag field
                    newArtist = when {
                        song.artist?.equals(sourceArtistName, ignoreCase = true) == true -> targetArtistName
                        else -> song.artist
                    }
                    newAlbumArtist = when {
                        song.albumArtist?.equals(sourceArtistName, ignoreCase = true) == true -> targetArtistName
                        else -> song.albumArtist
                    }
                }

                val edit = AudioTagWriter.TagEdit(
                    artist = newArtist,
                    albumArtist = newAlbumArtist
                )

                val writeResult = tagWriter.write(song.path, edit)
                if (writeResult is WriteResult.Success) {
                    successCount++
                }

                updatedSongs.add(song.copy(artist = newArtist, albumArtist = newAlbumArtist))
            }

            if (updatedSongs.isNotEmpty()) {
                libraryDao.updateSongs(updatedSongs)
            }

            libraryDao.updateAlbumArtist(oldArtist = sourceArtistName, newArtist = targetArtistName)
            libraryDao.deleteArtistByName(sourceArtistName)

            withContext(Dispatchers.Main) {
                _progressState.value = MassOperationProgress(isRunning = false)
                onComplete(true)
            }
        }
    }

}
