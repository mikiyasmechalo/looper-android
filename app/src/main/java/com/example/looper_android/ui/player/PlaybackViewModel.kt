package com.example.looper_android.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.example.looper_android.data.db.LibraryDao
import com.example.looper_android.data.db.SongEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlaybackState(
    val currentSong: SongEntity? = null,
    val isPlaying: Boolean = false,
    val position: Long = 0L,
    val duration: Long = 0L,
    val isBuffering: Boolean = false
)

@HiltViewModel
class PlaybackViewModel @Inject constructor(
    private val player: Player,
    private val libraryDao: LibraryDao
) : ViewModel() {

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

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
                
                _playbackState.value = _playbackState.value.copy(
                    currentSong = instantSong,
                    duration = player.duration.coerceAtLeast(0)
                )

                // Optional background sync with Room DB
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    val dbSong = libraryDao.getSongById(songId).firstOrNull()
                    if (dbSong != null) {
                        _playbackState.value = _playbackState.value.copy(currentSong = dbSong)
                    }
                }
            } else {
                _playbackState.value = _playbackState.value.copy(currentSong = null)
            }
        }
    }

    private fun updatePlaybackState() {
        val state = player.playbackState
        val buffering = state == Player.STATE_BUFFERING
        val playing = player.playWhenReady && state != Player.STATE_ENDED && state != Player.STATE_IDLE
        
        _playbackState.value = _playbackState.value.copy(
            isPlaying = playing,
            isBuffering = buffering
        )
    }

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    init {
        player.addListener(playerListener)
        
        // Initialize current state if player is already loaded (e.g. Activity restarted but Service remained)
        val initialItem = player.currentMediaItem
        if (initialItem != null) {
            playerListener.onMediaItemTransition(initialItem, Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED)
        }
        updatePlaybackState()
        
        // Start polling for position without re-emitting entire PlaybackState
        viewModelScope.launch {
            while (true) {
                if (player.isPlaying) {
                    _currentPosition.value = player.currentPosition
                }
                kotlinx.coroutines.delay(1000)
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
        player.seekToNextMediaItem()
    }

    fun skipToPrevious() {
        player.seekToPreviousMediaItem()
    }
}
