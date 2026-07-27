package com.example.looper_android.playback

import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.looper_android.domain.QueueManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject
    lateinit var player: Player

    @Inject
    lateinit var queueManager: QueueManager

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        
        mediaSession = MediaSession.Builder(this, player).build()
        queueManager.restoreLastQueue()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        queueManager.saveCurrentState()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
