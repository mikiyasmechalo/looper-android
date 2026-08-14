package com.example.looper_android.playback

import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.looper_android.domain.QueueManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject
    lateinit var player: Player

    @Inject
    lateinit var queueManager: QueueManager

    @Inject
    lateinit var settingsRepository: com.example.looper_android.data.repository.SettingsRepository

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        
        mediaSession = MediaSession.Builder(this, player).build()
        queueManager.restoreLastQueue()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        super.onTaskRemoved(rootIntent)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            val stop = settingsRepository.stopOnTaskRemoved.first()
            if (stop) {
                queueManager.saveCurrentStateSync()
                player.stop()
                stopSelf()
            }
        }
    }

    override fun onDestroy() {
        queueManager.saveCurrentStateSync()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
