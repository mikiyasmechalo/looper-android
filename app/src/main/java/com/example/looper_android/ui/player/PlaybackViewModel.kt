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

    private val playerListener = object : Player.Listener {
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            updatePlaybackState()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            updatePlaybackState()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
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

                songObserveJob?.cancel()
                val currentDuration = player.duration.coerceAtLeast(0)
                songObserveJob = viewModelScope.launch(Dispatchers.IO) {
                    val dbSong = libraryDao.getSongById(songId).firstOrNull()
                    val targetSong = dbSong ?: instantSong

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

                    // Stagger the heavy CPU operations AFTER the image is loaded and displayed!
                    // This is CRUCIAL: Since we are running on Dispatchers.IO, extracting the accent color
                    // will synchronously read the file from disk (via MediaMetadataRetriever).
                    // If we do this immediately, we block the IO threads and disk bandwidth that Coil 
                    // desperately needs to decode the uncached image!
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
        val opts = android.graphics.BitmapFactory.Options().apply { inSampleSize = 4 }

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

            var vibrantRedSum = 0L
            var vibrantGreenSum = 0L
            var vibrantBlueSum = 0L
            var vibrantCount = 0

            var allRedSum = 0L
            var allGreenSum = 0L
            var allBlueSum = 0L
            var allCount = 0

            val width = b.width
            val height = b.height
            for (x in 0 until width step 2) {
                for (y in 0 until height step 2) {
                    val pixel = b.getPixel(x, y)
                    val r = android.graphics.Color.red(pixel)
                    val g = android.graphics.Color.green(pixel)
                    val bl = android.graphics.Color.blue(pixel)
                    val max = maxOf(r, g, bl)
                    val min = minOf(r, g, bl)

                    if (max > 30) {
                        allRedSum += r
                        allGreenSum += g
                        allBlueSum += bl
                        allCount++

                        if ((max - min) > 18) {
                            vibrantRedSum += r
                            vibrantGreenSum += g
                            vibrantBlueSum += bl
                            vibrantCount++
                        }
                    }
                }
            }
            b.recycle()

            val rawColor = when {
                vibrantCount > 0 -> {
                    Color((vibrantRedSum / vibrantCount).toInt(), (vibrantGreenSum / vibrantCount).toInt(), (vibrantBlueSum / vibrantCount).toInt())
                }
                allCount > 0 -> {
                    Color((allRedSum / allCount).toInt(), (allGreenSum / allCount).toInt(), (allBlueSum / allCount).toInt())
                }
                else -> fallbackColor
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

        // Boost saturation if too gray/washed out
        if (hsl[1] < 0.15f) {
            hsl[1] = 0.40f
        }

        // Match Flutter theme_provider.dart 1-to-1:
        // double newLightness = hsl.lightness + 0.15;
        // if (newLightness < 0.60) newLightness = 0.60;
        // newLightness = newLightness.clamp(0.0, 0.95);
        var newLightness = hsl[2] + 0.15f
        if (newLightness < 0.60f) {
            newLightness = 0.60f
        }
        hsl[2] = newLightness.coerceIn(0.60f, 0.95f)

        val intColor = androidx.core.graphics.ColorUtils.HSLToColor(hsl)
        return Color(intColor)
    }

    private fun updatePlaybackState() {
        val state = player.playbackState
        val buffering = state == Player.STATE_BUFFERING
        val playing = player.playWhenReady && state != Player.STATE_ENDED && state != Player.STATE_IDLE
        
        _playbackState.value = _playbackState.value.copy(
            isPlaying = playing,
            isBuffering = buffering,
            position = if (pendingTransition) _playbackState.value.position else player.currentPosition.coerceAtLeast(0L),
            duration = if (pendingTransition) _playbackState.value.duration else player.duration.coerceAtLeast(0L),
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
        } else {
            player.play()
        }
    }

    fun skipToNext() {
        if (player.hasNextMediaItem()) {
            pendingTransition = true
            _currentPosition.value = 0L
            _playbackState.value = _playbackState.value.copy(position = 0L)
            player.seekToNextMediaItem()
        }
    }

    fun skipToPrevious() {
        pendingTransition = true
        _currentPosition.value = 0L
        _playbackState.value = _playbackState.value.copy(position = 0L)
        if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
        } else {
            player.seekTo(0L)
        }
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs.coerceIn(0L, player.duration.coerceAtLeast(0L)))
    }

    fun skipToQueueItem(index: Int) {
        if (index in 0 until player.mediaItemCount) {
            pendingTransition = true
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
        val mediaUri = Uri.parse(song.path)
        val mediaItem = MediaItem.Builder()
            .setMediaId(song.id.toString())
            .setUri(mediaUri)
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist ?: "Unknown Artist")
                    .setAlbumTitle(song.album ?: "Unknown Album")
                    .setArtworkUri(song.artPath?.let { Uri.parse(it) })
                    .build()
            )
            .build()

        // Prevent duplicates: remove if already exists in queue
        for (i in 0 until player.mediaItemCount) {
            val item = player.getMediaItemAt(i)
            if (item.mediaId == song.id.toString() || item.localConfiguration?.uri == mediaUri) {
                player.removeMediaItem(i)
                break
            }
        }

        val insertIndex = (player.currentMediaItemIndex + 1).coerceAtMost(player.mediaItemCount)
        player.addMediaItem(insertIndex, mediaItem)
    }

    fun addToQueue(song: SongEntity) {
        val mediaUri = Uri.parse(song.path)
        val mediaItem = MediaItem.Builder()
            .setMediaId(song.id.toString())
            .setUri(mediaUri)
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist ?: "Unknown Artist")
                    .setAlbumTitle(song.album ?: "Unknown Album")
                    .setArtworkUri(song.artPath?.let { Uri.parse(it) })
                    .build()
            )
            .build()

        // Prevent duplicates: remove if already exists in queue
        for (i in 0 until player.mediaItemCount) {
            val item = player.getMediaItemAt(i)
            if (item.mediaId == song.id.toString() || item.localConfiguration?.uri == mediaUri) {
                player.removeMediaItem(i)
                break
            }
        }

        player.addMediaItem(mediaItem)
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

    fun clearQueue() {
        queueManager.clearQueue()
        _playbackState.value = PlaybackState(accentColor = _playbackState.value.accentColor)
    }
}