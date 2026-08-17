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
import com.example.looper_android.playback.equalizer.AudioOutputDeviceType
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

    @Inject
    lateinit var audioDeviceDetector: com.example.looper_android.playback.equalizer.AudioDeviceDetector

    @Inject
    lateinit var equalizerRepository: com.example.looper_android.data.repository.EqualizerRepository

    @Inject
    lateinit var equalizerAudioProcessor: com.example.looper_android.playback.equalizer.dsp.CustomEqualizerAudioProcessor

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

        // Observe active audio output device and auto-apply equalizer profile
        serviceScope.launch {
            audioDeviceDetector.currentOutputDevice.collect { device ->
                if (device.type == AudioOutputDeviceType.BLUETOOTH && device.deviceKey.startsWith("BT_")) {
                    equalizerRepository.registerBluetoothDevice(device.deviceKey, device.displayName)
                }
                val autoSwitch = equalizerRepository.isAutoSwitchEnabled.first()
                if (autoSwitch) {
                    val profile = equalizerRepository.getProfileSync(device.deviceKey, device.displayName)
                    equalizerAudioProcessor.updateParameters(
                        enabled = profile.isEnabled,
                        bandGains = profile.bandLevels,
                        preampDb = profile.preampGainDb,
                        bassBoostPercent = profile.bassBoost,
                        virtualizerPercent = profile.virtualizer
                    )
                }
            }
        }

        // V-Fade (Crossfade) Monitor Loop
        serviceScope.launch {
            var crossfadeEnabled = false
            var crossfadeDurationMs = 3000L

            launch { settingsRepository.enableCrossfade.collect { crossfadeEnabled = it } }
            launch { settingsRepository.crossfadeDuration.collect { crossfadeDurationMs = (it * 1000).toLong() } }

            while (true) {
                try {
                    if (crossfadeEnabled && player.isPlaying && crossfadeDurationMs > 0) {
                        val position = player.currentPosition
                        val duration = player.duration

                        if (duration > 0 && duration != androidx.media3.common.C.TIME_UNSET) {
                            val remaining = duration - position
                            val newVolume = when {
                                position < crossfadeDurationMs -> {
                                    // Fade in at the start of track
                                    (position.toFloat() / crossfadeDurationMs.toFloat()).coerceIn(0f, 1f)
                                }
                                remaining < crossfadeDurationMs -> {
                                    // Fade out at the end of track
                                    (remaining.toFloat() / crossfadeDurationMs.toFloat()).coerceIn(0f, 1f)
                                }
                                else -> 1f
                            }
                            // Only update if change is significant to avoid spamming Volume changes
                            if (kotlin.math.abs(player.volume - newVolume) > 0.02f) {
                                player.volume = newVolume
                            }
                        }
                    } else if (!crossfadeEnabled && player.volume < 1f) {
                        // Restore volume instantly if crossfade is disabled while volume is ducked
                        player.volume = 1f
                    }
                } catch (e: Exception) {
                    // Ignore transient player access exceptions
                }
                kotlinx.coroutines.delay(50) // 20 Hz update rate for smooth volume transition
            }
        }
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
