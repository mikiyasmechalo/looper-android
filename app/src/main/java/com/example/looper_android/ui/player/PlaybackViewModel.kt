package com.example.looper_android.ui.player

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.example.looper_android.data.db.LibraryDao
import com.example.looper_android.data.db.SongEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

import androidx.compose.runtime.Immutable

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
    val accentColor: Color = Color(0xFF808080),
    val isSleepTimerActive: Boolean = false,
    val sleepTimerLabel: String = ""
)

@HiltViewModel
class PlaybackViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val player: Player,
    private val libraryDao: LibraryDao
) : ViewModel() {

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

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

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            pendingTransition = false
            if (mediaItem != null) {
                val songId = mediaItem.mediaId.toLongOrNull() ?: 0L
                val metadata = mediaItem.mediaMetadata
                val instantSong = SongEntity(
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
                songObserveJob = viewModelScope.launch(Dispatchers.IO) {
                    val dbSong = libraryDao.getSongById(songId).firstOrNull()
                    val targetSong = dbSong ?: instantSong
                    val dbQualityText = AudioQualityAnalyzer.getQualityString(targetSong.path)
                    val accent = extractAccentColor(targetSong)

                    _playbackState.value = _playbackState.value.copy(
                        currentSong = targetSong,
                        isFavorite = targetSong.isFavorite,
                        audioQualityText = dbQualityText,
                        accentColor = accent
                    )
                }
            } else {
                _playbackState.value = _playbackState.value.copy(currentSong = null)
            }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _playbackState.value = _playbackState.value.copy(isShuffle = shuffleModeEnabled)
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

    private fun extractAccentColor(song: SongEntity): Color {
        val fallbackColor = Color(0xFF808080)
        val artPath = song.artPath
        val songPath = song.path

        var bitmap: Bitmap? = null
        val opts = BitmapFactory.Options().apply { inSampleSize = 8 }

        try {
            // 1. Try reading from artPath URI/file
            if (!artPath.isNullOrEmpty()) {
                if (artPath.startsWith("content://") || artPath.startsWith("file://")) {
                    context.contentResolver.openInputStream(Uri.parse(artPath))?.use { stream ->
                        bitmap = BitmapFactory.decodeStream(stream, null, opts)
                    }
                } else {
                    val file = File(artPath)
                    if (file.exists()) {
                        bitmap = BitmapFactory.decodeFile(file.absolutePath, opts)
                    }
                }
            }

            // 2. Fallback to MediaMetadataRetriever embedded picture
            if (bitmap == null && songPath.isNotEmpty()) {
                val cleanPath = if (songPath.startsWith("file://")) Uri.parse(songPath).path else songPath
                if (!cleanPath.isNullOrEmpty() && File(cleanPath).exists()) {
                    val mmr = MediaMetadataRetriever()
                    mmr.setDataSource(cleanPath)
                    val pictureBytes = mmr.embeddedPicture
                    mmr.release()
                    if (pictureBytes != null) {
                        bitmap = BitmapFactory.decodeByteArray(pictureBytes, 0, pictureBytes.size, opts)
                    }
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
            isShuffle = player.shuffleModeEnabled,
            repeatMode = when (player.repeatMode) {
                Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                else -> RepeatMode.OFF
            }
        )
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

    fun seekRelative(offsetMs: Long) {
        val target = (player.currentPosition + offsetMs).coerceIn(0L, player.duration.coerceAtLeast(0L))
        player.seekTo(target)
    }

    fun toggleShuffle() {
        val newShuffle = !_playbackState.value.isShuffle
        player.shuffleModeEnabled = newShuffle
        _playbackState.value = _playbackState.value.copy(isShuffle = newShuffle)
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
        viewModelScope.launch(Dispatchers.IO) {
            val updated = songToToggle.copy(isFavorite = !songToToggle.isFavorite)
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

    fun clearQueue() {
        player.stop()
        player.clearMediaItems()
        _playbackState.value = PlaybackState()
    }
}