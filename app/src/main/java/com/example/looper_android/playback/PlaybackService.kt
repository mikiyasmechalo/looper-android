package com.example.looper_android.playback

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.looper_android.data.repository.SettingsRepository
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
    lateinit var settingsRepository: SettingsRepository

    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private var wasPausedByDisconnect = false

    private val headsetStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            serviceScope.launch {
                when (action) {
                    AudioManager.ACTION_AUDIO_BECOMING_NOISY -> {
                        val pauseOnDisconnect = settingsRepository.pauseOnDisconnect.first()
                        if (pauseOnDisconnect && player.isPlaying) {
                            wasPausedByDisconnect = true
                            player.pause()
                        }
                    }
                    BluetoothDevice.ACTION_ACL_CONNECTED -> {
                        val resumeOnBt = settingsRepository.resumeOnBluetoothConnect.first()
                        if (resumeOnBt) {
                            val onlyIfPausedByDisconnect = settingsRepository.resumeOnlyIfPausedByDisconnect.first()
                            if (!onlyIfPausedByDisconnect || wasPausedByDisconnect) {
                                wasPausedByDisconnect = false
                                if (!player.isPlaying && player.playbackState != Player.STATE_IDLE) {
                                    player.play()
                                }
                            }
                        }
                    }
                    Intent.ACTION_HEADSET_PLUG -> {
                        val state = intent.getIntExtra("state", -1)
                        if (state == 1) { // 1 = Plugged
                            val resumeOnWired = settingsRepository.resumeOnWiredConnect.first()
                            if (resumeOnWired) {
                                val onlyIfPausedByDisconnect = settingsRepository.resumeOnlyIfPausedByDisconnect.first()
                                if (!onlyIfPausedByDisconnect || wasPausedByDisconnect) {
                                    wasPausedByDisconnect = false
                                    if (!player.isPlaying && player.playbackState != Player.STATE_IDLE) {
                                        player.play()
                                    }
                                }
                            }
                        } else if (state == 0) { // 0 = Unplugged
                            val pauseOnDisconnect = settingsRepository.pauseOnDisconnect.first()
                            if (pauseOnDisconnect && player.isPlaying) {
                                wasPausedByDisconnect = true
                                player.pause()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        
        mediaSession = MediaSession.Builder(this, player).build()
        queueManager.restoreLastQueue()

        val filter = IntentFilter().apply {
            addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(Intent.ACTION_HEADSET_PLUG)
        }
        registerReceiver(headsetStateReceiver, filter)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        serviceScope.launch(Dispatchers.IO) {
            val stop = settingsRepository.stopOnTaskRemoved.first()
            if (stop) {
                queueManager.saveCurrentStateSync()
                player.stop()
                stopSelf()
            }
        }
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(headsetStateReceiver)
        } catch (_: Exception) {}
        queueManager.saveCurrentStateSync()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
