package com.example.looper_android.ui.player

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import coil.imageLoader
import com.example.looper_android.data.db.LibraryDao
import com.example.looper_android.data.db.SongEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class RepeatMode {
    OFF, ALL, ONE
}

@Immutable
data class PlaybackState(
    val currentSong: SongEntity? = null,
    val isPlaying: Boolean = false,
    val position: Long = 0L,
    val duration: Long = 0L,
    val isBuffering: Boolean = false,
    val isShuffle: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val isFavorite: Boolean = false,
    val queue: List<SongEntity> = emptyList(),
    val audioQualityText: String = "High Quality • Audio",
    val accentColor: Color = Color(0xFF55DF69),
    val isSleepTimerActive: Boolean = false,
    val sleepTimerLabel: String = "",
    val sleepTimerDurationRemainingMs: Long? = null,
    val sleepTimerSongsRemaining: Int? = null,
    val isStopAfterThisSongActive: Boolean = false,
    val activeQueueId: Long? = null,
    val activeQueueName: String = "Select Queue",
    /**
     * True once the DB song lookup completes. Prevents the mini-player from
     * animating in halfway and getting stuck during cold start initialization.
     */
    val isSongReady: Boolean = false
)

@HiltViewModel
class PlaybackViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val player: Player,
    private val libraryDao: LibraryDao,
    private val queueManager: com.example.looper_android.domain.QueueManager,
    private val settingsRepository: com.example.looper_android.data.repository.SettingsRepository
) : ViewModel() {

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    val allQueues: StateFlow<List<com.example.looper_android.data.db.QueueEntity>> = queueManager.allQueues
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    private var lastIncrementedSongId: Long? = null

    private var songObserveJob: kotlinx.coroutines.Job? = null

    private var pendingTransition = false

    // When true, any attempt by ExoPlayer to start playing is immediately cancelled.
    // Set when a song-count sleep timer / stop-after-this-song fires during a track transition.
    @Volatile private var shouldBlockNextPlay = false

    private val playerListener = object : Player.Listener {
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            updatePlaybackState()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying && shouldBlockNextPlay) {
                player.pause()
                _playbackState.value = _playbackState.value.copy(isPlaying = false)
                return
            }
            updatePlaybackState()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                if (_playbackState.value.isSleepTimerActive) {
                    stopSleepTimer()
                }
                if (player.mediaItemCount > 0) {
                    player.seekToDefaultPosition(0)
                    player.pause()
                    _currentPosition.value = 0L
                    _playbackState.value = _playbackState.value.copy(isPlaying = false, position = 0L)
                }
            }
            updatePlaybackState()
        }

        override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
            updatePlaybackState()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            pendingTransition = false
            if (mediaItem != null) {
                val songId = mediaItem.mediaId.toLongOrNull() ?: 0L
                val metadata = mediaItem.mediaMetadata
                
                // If the transition is just a playlist update and the song is the same, ignore!
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) {
                    if (_playbackState.value.currentSong?.id == songId) {
                        return
                    }
                }

                val instantSong = com.example.looper_android.data.db.SongEntity(
                    id = songId,
                    path = mediaItem.localConfiguration?.uri?.toString() ?: "",
                    title = metadata.title?.toString() ?: "Unknown Title",
                    artist = metadata.artist?.toString() ?: "Unknown Artist",
                    albumArtist = null,
                    album = metadata.albumTitle?.toString() ?: "Unknown Album",
                    genre = null,
                    duration = player.duration.coerceAtLeast(0).toInt(),
                    trackNumber = null,
                    year = null,
                    artPath = metadata.artworkUri?.toString(),
                    dateAdded = System.currentTimeMillis()
                )

                _currentPosition.value = 0L
                _playbackState.value = _playbackState.value.copy(
                    currentSong = instantSong,
                    position = 0L,
                    duration = player.duration.coerceAtLeast(0)
                )


                if (songId > 0L && songId != lastIncrementedSongId) {
                    lastIncrementedSongId = songId
                    viewModelScope.launch(Dispatchers.IO) {
                        libraryDao.incrementPlayCount(songId)
                    }
                }

                // Handle Sleep Timer by Song Count (Multi-song countdown)
                if (reason != Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) {
                    val currentRemainingSongs = _playbackState.value.sleepTimerSongsRemaining
                    if (currentRemainingSongs != null) {
                        val newCount = currentRemainingSongs - 1
                        if (newCount <= 0) {
                            // This is the transition INTO the stopped track — block it from playing
                            shouldBlockNextPlay = true
                            player.pause()
                            _currentPosition.value = 0L
                            _playbackState.value = _playbackState.value.copy(
                                isPlaying = false,
                                position = 0L,
                                isSleepTimerActive = false,
                                isStopAfterThisSongActive = false,
                                sleepTimerSongsRemaining = null,
                                sleepTimerLabel = ""
                            )
                            sleepTimerJob?.cancel()
                            sleepTimerJob = null
                            // Release block after a short window (enough to absorb any late play() calls)
                            viewModelScope.launch(Dispatchers.Main) {
                                kotlinx.coroutines.delay(800L)
                                shouldBlockNextPlay = false
                            }
                        } else if (newCount == 1) {
                            _playbackState.value = _playbackState.value.copy(
                                sleepTimerSongsRemaining = 1,
                                sleepTimerLabel = "1 song left",
                                isStopAfterThisSongActive = true
                            )
                        } else {
                            val label = "$newCount songs left"
                            _playbackState.value = _playbackState.value.copy(
                                sleepTimerSongsRemaining = newCount,
                                sleepTimerLabel = label,
                                isStopAfterThisSongActive = false
                            )
                        }
                    }
                }

                songObserveJob?.cancel()
                val currentDuration = player.duration.coerceAtLeast(0)
                songObserveJob = viewModelScope.launch(Dispatchers.IO) {
                    val initialDbSong = libraryDao.getSongById(songId).firstOrNull()
                    val targetSong = initialDbSong ?: instantSong

                    _playbackState.value = _playbackState.value.copy(
                        currentSong = targetSong,
                        isFavorite = targetSong.isFavorite,
                        position = 0L,
                        duration = currentDuration,
                        audioQualityText = "High Quality • Audio",
                        accentColor = androidx.compose.ui.graphics.Color(0xFF808080),
                        isSongReady = true
                    )
                    _currentPosition.value = 0L

                    if (!targetSong.artPath.isNullOrEmpty()) {
                        val request = coil.request.ImageRequest.Builder(context)
                            .data(targetSong.artPath)
                            .build()
                        context.imageLoader.enqueue(request)
                    }

                    // Calculate quality and accent color once in background
                    launch {
                        kotlinx.coroutines.delay(300)
                        val dbQualityText = AudioQualityAnalyzer.getQualityString(targetSong.path)
                        val dynamicTheming = settingsRepository.dynamicTheming.first()
                        val dynamicAccentColor = settingsRepository.dynamicAccentColor.first()
                        val customAccentColorInt = settingsRepository.customAccentColor.first()
                        val accent = if (dynamicTheming || dynamicAccentColor) {
                            extractAccentColor(targetSong)
                        } else {
                            Color(customAccentColorInt)
                        }

                        if (_playbackState.value.currentSong?.id == targetSong.id) {
                            _playbackState.value = _playbackState.value.copy(
                                audioQualityText = dbQualityText,
                                accentColor = accent
                            )
                        }
                    }

                    // Continuously collect any updates (such as isFavorite toggles or tag edits)
                    libraryDao.getSongById(songId).collect { updatedDbSong ->
                        if (updatedDbSong != null) {
                            _playbackState.value = _playbackState.value.copy(
                                currentSong = updatedDbSong,
                                isFavorite = updatedDbSong.isFavorite
                            )
                        }
                    }
                }
            } else {
                _playbackState.value = _playbackState.value.copy(currentSong = null, isSongReady = false)
            }
        }



        override fun onRepeatModeChanged(repeatModeInt: Int) {
            val mode = when (repeatModeInt) {
                Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                else -> RepeatMode.OFF
            }
            _playbackState.value = _playbackState.value.copy(repeatMode = mode)
        }
    }

    init {
        player.addListener(playerListener)

        val initialItem = player.currentMediaItem
        if (initialItem != null && player.playbackState != Player.STATE_IDLE) {
            playerListener.onMediaItemTransition(initialItem, Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED)
        }
        updatePlaybackState()

        viewModelScope.launch {
            while (true) {
                if (player.isPlaying && !pendingTransition) {
                    _currentPosition.value = player.currentPosition.coerceAtLeast(0L)
                    _playbackState.value = _playbackState.value.copy(position = player.currentPosition.coerceAtLeast(0L))
                }
                kotlinx.coroutines.delay(100)
            }
        }

        viewModelScope.launch {
            queueManager.activeQueueSongs.collectLatest { songs ->
                _playbackState.value = _playbackState.value.copy(queue = songs)
            }
        }

        viewModelScope.launch {
            queueManager.activeQueueId.collectLatest { activeId ->
                _playbackState.value = _playbackState.value.copy(activeQueueId = activeId)
            }
        }
        
        viewModelScope.launch {
            queueManager.isShuffleEnabled.collectLatest { isShuffle ->
                _playbackState.value = _playbackState.value.copy(isShuffle = isShuffle)
            }
        }
        
        viewModelScope.launch {
            queueManager.activeQueueName.collectLatest { activeName ->
                _playbackState.value = _playbackState.value.copy(activeQueueName = activeName)
            }
        }

        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                settingsRepository.dynamicTheming,
                settingsRepository.dynamicAccentColor,
                settingsRepository.customAccentColor
            ) { dynamicTheming, dynamicAccentColor, customAccentColorInt ->
                Triple(dynamicTheming, dynamicAccentColor, customAccentColorInt)
            }.collectLatest { (dynamicTheming, dynamicAccentColor, customAccentColorInt) ->
                val song = _playbackState.value.currentSong
                if (song != null && (dynamicTheming || dynamicAccentColor)) {
                    val extracted = extractAccentColor(song)
                    _playbackState.value = _playbackState.value.copy(accentColor = extracted)
                } else {
                    _playbackState.value = _playbackState.value.copy(accentColor = Color(customAccentColorInt))
                }
            }
        }
    }

    private fun extractAccentColor(song: com.example.looper_android.data.db.SongEntity): Color {
        val fallbackColor = Color(0xFF808080)

        var bitmap: android.graphics.Bitmap? = null
        val opts = android.graphics.BitmapFactory.Options().apply { inSampleSize = 2 }

        try {
            // 1. Read directly from the album art content URI stored during scan
            //    artPath = "content://media/external/audio/albumart/{albumId}"
            val artPath = song.artPath
            if (!artPath.isNullOrEmpty()) {
                try {
                    val artUri = android.net.Uri.parse(artPath)
                    context.contentResolver.openInputStream(artUri)?.use { stream ->
                        bitmap = android.graphics.BitmapFactory.decodeStream(stream, null, opts)
                    }
                } catch (e: Exception) {
                    android.util.Log.w("AccentColor", "artPath open failed for ${song.title}: ${e.message}")
                }
            }

            // 2. Fallback: use MediaMetadataRetriever on the actual audio file path
            //    This reads embedded ID3 cover art directly from the file
            if (bitmap == null) {
                val songPath = song.path
                val cleanPath = when {
                    songPath.startsWith("file://") -> android.net.Uri.parse(songPath).path ?: songPath
                    else -> songPath
                }
                try {
                    val file = java.io.File(cleanPath)
                    if (file.exists() && file.canRead()) {
                        val mmr = android.media.MediaMetadataRetriever()
                        mmr.setDataSource(cleanPath)
                        val pictureBytes = mmr.embeddedPicture
                        mmr.release()
                        if (pictureBytes != null) {
                            bitmap = android.graphics.BitmapFactory.decodeByteArray(pictureBytes, 0, pictureBytes.size, opts)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("AccentColor", "MMR failed for ${song.title}: ${e.message}")
                }
            }

            // 3. Android Q+: try ContentResolver.loadThumbnail with artPath URI
            if (bitmap == null && !artPath.isNullOrEmpty() && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                try {
                    val artUri = android.net.Uri.parse(artPath)
                    bitmap = context.contentResolver.loadThumbnail(artUri, android.util.Size(200, 200), null)
                } catch (e: Exception) {
                    android.util.Log.w("AccentColor", "loadThumbnail failed for ${song.title}: ${e.message}")
                }
            }

            val b = bitmap ?: return fallbackColor

            val palette = androidx.palette.graphics.Palette.from(b).generate()
            b.recycle()

            val chosenSwatch = palette.vibrantSwatch
                ?: palette.lightVibrantSwatch
                ?: palette.darkVibrantSwatch
                ?: palette.dominantSwatch
                ?: palette.mutedSwatch

            val rawColor = if (chosenSwatch != null) {
                Color(chosenSwatch.rgb)
            } else {
                fallbackColor
            }

            return ensureVibrantAccentColor(rawColor)
        } catch (e: Exception) {
            e.printStackTrace()
            return fallbackColor
        }
    }

    private fun ensureVibrantAccentColor(rawColor: Color): Color {
        val hsl = FloatArray(3)
        androidx.core.graphics.ColorUtils.colorToHSL(
            android.graphics.Color.rgb(
                (rawColor.red * 255).toInt(),
                (rawColor.green * 255).toInt(),
                (rawColor.blue * 255).toInt()
            ),
            hsl
        )

        // Only boost saturation if there is some subtle color, preserving the exact hue
        if (hsl[1] < 0.25f && hsl[1] > 0.05f) {
            hsl[1] = 0.45f
        }

        // Ensure proper contrast and brightness for dark theme UI
        var newLightness = hsl[2]
        if (newLightness < 0.55f) {
            newLightness = (newLightness + 0.20f).coerceAtLeast(0.55f)
        }
        hsl[2] = newLightness.coerceIn(0.55f, 0.88f)

        val intColor = androidx.core.graphics.ColorUtils.HSLToColor(hsl)
        return Color(intColor)
    }

    private fun updatePlaybackState() {
        val state = player.playbackState
        val buffering = state == Player.STATE_BUFFERING
        val playing = player.playWhenReady && state != Player.STATE_ENDED && state != Player.STATE_IDLE
        val pos = player.currentPosition.coerceAtLeast(0L)
        val dur = player.duration.coerceAtLeast(0L)
        
        _currentPosition.value = pos
        _playbackState.value = _playbackState.value.copy(
            isPlaying = playing,
            isBuffering = buffering,
            position = pos,
            duration = dur,
            isShuffle = queueManager.isShuffleEnabled.value,
            repeatMode = when (player.repeatMode) {
                Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                else -> RepeatMode.OFF
            }
        )
    }


    override fun onCleared() {
        super.onCleared()
        player.removeListener(playerListener)
    }

    fun togglePlayPause() {
        if (player.isPlaying) {
            player.pause()
            val pos = player.currentPosition.coerceAtLeast(0L)
            _currentPosition.value = pos
            _playbackState.value = _playbackState.value.copy(isPlaying = false, position = pos)
        } else {
            (player as? androidx.media3.exoplayer.ExoPlayer)?.pauseAtEndOfMediaItems = false
            player.play()
        }
    }

    fun skipToNext() {
        if (player.hasNextMediaItem()) {
            _currentPosition.value = 0L
            _playbackState.value = _playbackState.value.copy(position = 0L)
            player.seekToNextMediaItem()
        }
    }

    fun skipToPrevious() {
        _currentPosition.value = 0L
        _playbackState.value = _playbackState.value.copy(position = 0L)
        if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
        } else {
            player.seekTo(0L)
        }
    }

    fun seekTo(positionMs: Long) {
        val target = positionMs.coerceIn(0L, player.duration.coerceAtLeast(0L))
        _currentPosition.value = target
        _playbackState.value = _playbackState.value.copy(position = target)
        player.seekTo(target)
    }

    fun skipToQueueItem(index: Int) {
        if (index in 0 until player.mediaItemCount) {
            _currentPosition.value = 0L
            _playbackState.value = _playbackState.value.copy(position = 0L)
            player.seekToDefaultPosition(index)
            if (!player.playWhenReady) {
                player.play()
            }
        }
    }

    fun seekRelative(offsetMs: Long) {
        val target = (player.currentPosition + offsetMs).coerceIn(0L, player.duration.coerceAtLeast(0L))
        player.seekTo(target)
    }

    fun toggleShuffle() {
        queueManager.toggleShuffle()
    }

    fun nextRepeatMode() {
        val nextMode = when (_playbackState.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        val media3Mode = when (nextMode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }
        player.repeatMode = media3Mode
        _playbackState.value = _playbackState.value.copy(repeatMode = nextMode)
    }

    fun toggleFavorite(targetSong: SongEntity? = null) {
        val songToToggle = targetSong ?: _playbackState.value.currentSong ?: return
        val updated = songToToggle.copy(isFavorite = !songToToggle.isFavorite)
        
        // Optimistically update the UI immediately
        if (_playbackState.value.currentSong?.id == updated.id) {
            _playbackState.value = _playbackState.value.copy(
                currentSong = updated,
                isFavorite = updated.isFavorite
            )
        }
        
        // Update the queue list as well so it persists in the active session
        queueManager.updateSongInQueue(updated)

        viewModelScope.launch(Dispatchers.IO) {
            libraryDao.updateSong(updated)
        }
    }

    fun addNext(song: SongEntity) {
        queueManager.addNext(song)
    }

    fun addToQueue(song: SongEntity) {
        queueManager.addToQueue(song)
    }

    fun setPlaylist(songs: List<SongEntity>, initialIndex: Int = 0, disableShuffleMode: Boolean = false, queueName: String = "All Songs") {
        if (songs.isEmpty()) return
        queueManager.loadAndPlayQueue(
            queueName = queueName,
            songs = songs,
            startIndex = initialIndex,
            forceShuffleOriginal = !disableShuffleMode
        )
    }

    fun shuffleAll(songs: List<SongEntity>, queueName: String = "All Songs") {
        if (songs.isEmpty()) return
        queueManager.loadAndPlayQueue(
            queueName = queueName,
            songs = songs,
            startIndex = songs.indices.random(),
            forceShuffleOriginal = true
        )
    }

    fun switchToQueue(queueId: Long) {
        queueManager.switchToQueue(queueId)
    }

    fun deleteUserQueue(queueId: Long) {
        queueManager.deleteUserQueue(queueId)
    }

    fun deleteQueue(queueId: Long) {
        queueManager.deleteQueue(queueId)
    }

    private var sleepTimerJob: kotlinx.coroutines.Job? = null

    private fun formatTimerMs(ms: Long): String {
        val totalSeconds = (ms / 1000).coerceAtLeast(0)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }

    fun startSleepTimer(durationMinutes: Int? = null, songCount: Int? = null) {
        sleepTimerJob?.cancel()
        sleepTimerJob = null

        if (durationMinutes != null && durationMinutes > 0) {
            var remainingMs = durationMinutes * 60 * 1000L
            _playbackState.value = _playbackState.value.copy(
                isSleepTimerActive = true,
                sleepTimerDurationRemainingMs = remainingMs,
                sleepTimerSongsRemaining = null,
                sleepTimerLabel = formatTimerMs(remainingMs),
                isStopAfterThisSongActive = false
            )

            sleepTimerJob = viewModelScope.launch(Dispatchers.Main) {
                while (remainingMs > 0) {
                    kotlinx.coroutines.delay(1000L)
                    remainingMs -= 1000L
                    if (remainingMs <= 0L) {
                        player.pause()
                        val pos = player.currentPosition.coerceAtLeast(0L)
                        _currentPosition.value = pos
                        _playbackState.value = _playbackState.value.copy(
                            isPlaying = false,
                            position = pos,
                            isSleepTimerActive = false,
                            sleepTimerLabel = "",
                            sleepTimerDurationRemainingMs = null,
                            sleepTimerSongsRemaining = null,
                            isStopAfterThisSongActive = false
                        )
                        sleepTimerJob = null
                        break
                    } else {
                        _playbackState.value = _playbackState.value.copy(
                            sleepTimerDurationRemainingMs = remainingMs,
                            sleepTimerLabel = formatTimerMs(remainingMs)
                        )
                    }
                }
            }
        } else if (songCount != null && songCount > 0) {
            if (player.repeatMode == Player.REPEAT_MODE_ONE) {
                player.repeatMode = Player.REPEAT_MODE_ALL
            }
            val label = if (songCount == 1) "1 song left" else "$songCount songs left"
            _playbackState.value = _playbackState.value.copy(
                isSleepTimerActive = true,
                sleepTimerDurationRemainingMs = null,
                sleepTimerSongsRemaining = songCount,
                sleepTimerLabel = label,
                isStopAfterThisSongActive = (songCount == 1)
            )
        }
    }

    fun stopSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        shouldBlockNextPlay = false
        _playbackState.value = _playbackState.value.copy(
            isSleepTimerActive = false,
            sleepTimerLabel = "",
            sleepTimerDurationRemainingMs = null,
            sleepTimerSongsRemaining = null,
            isStopAfterThisSongActive = false
        )
    }

    fun toggleStopAfterThisSong() {
        if (_playbackState.value.isStopAfterThisSongActive) {
            stopSleepTimer()
        } else {
            startSleepTimer(songCount = 1)
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        queueManager.moveQueueItem(fromIndex, toIndex)
    }

    fun setQueueOrder(newOrder: List<SongEntity>, fromIndex: Int, toIndex: Int) {
        _playbackState.value = _playbackState.value.copy(queue = newOrder)
        queueManager.setQueueOrder(newOrder, fromIndex, toIndex)
    }

    fun removeQueueItem(index: Int) {
        val currentQueue = _playbackState.value.queue
        if (index in currentQueue.indices) {
            val updated = currentQueue.toMutableList().apply { removeAt(index) }
            _playbackState.value = _playbackState.value.copy(queue = updated)
        }
        queueManager.removeQueueItem(index)
    }

    fun removeFromQueue(song: SongEntity) {
        queueManager.removeSong(song.id)
    }

    fun clearQueue() {
        queueManager.clearQueue()
        _playbackState.value = PlaybackState(accentColor = _playbackState.value.accentColor)
    }
}