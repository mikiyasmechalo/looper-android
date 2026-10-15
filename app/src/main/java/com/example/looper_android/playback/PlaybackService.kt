package com.example.looper_android.playback

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.collectLatest
import androidx.media3.common.util.BitmapLoader
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.example.looper_android.R
import com.example.looper_android.data.db.LibraryDao
import com.example.looper_android.data.repository.EqualizerRepository
import com.example.looper_android.data.repository.SettingsRepository
import com.example.looper_android.domain.QueueManager
import com.example.looper_android.ui.widgets.WidgetStateRepository
import com.example.looper_android.playback.equalizer.AudioDeviceDetector
import com.example.looper_android.playback.equalizer.AudioOutputDeviceType
import com.example.looper_android.playback.equalizer.dsp.CustomEqualizerAudioProcessor
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors
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
    lateinit var libraryDao: LibraryDao

    @Inject
    lateinit var audioDeviceDetector: AudioDeviceDetector

    @Inject
    lateinit var equalizerRepository: EqualizerRepository

    @Inject
    lateinit var equalizerAudioProcessor: CustomEqualizerAudioProcessor

    @Inject
    lateinit var lyricsFetcher: com.example.looper_android.data.repository.LyricsFetcher

    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private var bitmapExecutor = Executors.newFixedThreadPool(2)
    private var wasPausedByDisconnect = false

    companion object {
        const val ACTION_FAVORITE = "com.example.looper_android.ACTION_FAVORITE"
        const val ACTION_CLOSE = "com.example.looper_android.ACTION_CLOSE"

        fun start(context: Context) {
            try {
                val intent = Intent(context, PlaybackService::class.java)
                androidx.core.content.ContextCompat.startForegroundService(context, intent)
            } catch (_: Exception) {
                try {
                    val intent = Intent(context, PlaybackService::class.java)
                    context.startService(intent)
                } catch (_: Exception) {}
            }
        }
    }

    private val favoriteCommand = SessionCommand(ACTION_FAVORITE, Bundle.EMPTY)
    private val closeCommand = SessionCommand(ACTION_CLOSE, Bundle.EMPTY)

    private fun createFavoriteButton(isFavorite: Boolean): CommandButton {
        return CommandButton.Builder()
            .setDisplayName(if (isFavorite) "Favorited" else "Favorite")
            .setIconResId(if (isFavorite) R.drawable.ic_notif_favorite_filled else R.drawable.ic_notif_favorite_border)
            .setSessionCommand(favoriteCommand)
            .build()
    }

    private fun createCloseButton(): CommandButton {
        return CommandButton.Builder()
            .setDisplayName("Close")
            .setIconResId(R.drawable.ic_notif_close)
            .setSessionCommand(closeCommand)
            .build()
    }

    private fun updateNotificationButtons(isFavorite: Boolean) {
        val favBtn = createFavoriteButton(isFavorite)
        val closeBtn = createCloseButton()
        mediaSession?.setCustomLayout(listOf(favBtn, closeBtn))
    }

    private val headsetStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            // Ignore sticky initial broadcast delivered on registerReceiver (state=0 when no headphones plugged in)
            if (isInitialStickyBroadcast) {
                return
            }
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

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            // Read Player properties strictly on the Main thread
            val currentMediaId = mediaItem?.mediaId?.toLongOrNull()
            val isPlaying = player.isPlaying
            val position = player.currentPosition
            val duration = player.duration.coerceAtLeast(0)

            serviceScope.launch(Dispatchers.IO) {
                if (currentMediaId != null) {
                    val song = libraryDao.getSongByIdSync(currentMediaId)
                    val isFav = song?.isFavorite ?: false
                    serviceScope.launch(Dispatchers.Main) {
                        updateNotificationButtons(isFav)
                    }
                    if (song != null) {
                        WidgetStateRepository.updateState(
                            context = this@PlaybackService,
                            song = song,
                            isPlaying = isPlaying,
                            positionMs = position,
                            durationMs = duration
                        )
                    }
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            // Read Player properties strictly on the Main thread
            val currentMediaId = player.currentMediaItem?.mediaId?.toLongOrNull()
            val position = player.currentPosition
            val duration = player.duration.coerceAtLeast(0)

            serviceScope.launch(Dispatchers.IO) {
                if (currentMediaId != null) {
                    val song = libraryDao.getSongByIdSync(currentMediaId)
                    if (song != null) {
                        WidgetStateRepository.updateState(
                            context = this@PlaybackService,
                            song = song,
                            isPlaying = isPlaying,
                            positionMs = position,
                            durationMs = duration
                        )
                        if (song.lyrics.isNullOrBlank()) {
                            val fetched = lyricsFetcher.fetchLyrics(song)
                            if (!fetched.isNullOrBlank()) {
                                WidgetStateRepository.updateLyrics(this@PlaybackService, song.id, fetched)
                            }
                        }
                    }
                } else {
                    WidgetStateRepository.updatePlayPauseOptimistic(this@PlaybackService, isPlaying)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        if (bitmapExecutor.isShutdown) {
            bitmapExecutor = Executors.newFixedThreadPool(2)
        }

        val bitmapLoader = object : BitmapLoader {
            override fun supportsMimeType(mimeType: String): Boolean = true

            override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> {
                return Futures.submit(java.util.concurrent.Callable<Bitmap> {
                    BitmapFactory.decodeByteArray(data, 0, data.size)
                        ?: throw IllegalArgumentException("Could not decode byte array")
                }, bitmapExecutor)
            }

            override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
                return Futures.submit(java.util.concurrent.Callable<Bitmap> {
                    val currentSong = runCatching {
                        val mediaId = player.currentMediaItem?.mediaId?.toLongOrNull()
                        if (mediaId != null) {
                            kotlinx.coroutines.runBlocking(Dispatchers.IO) {
                                libraryDao.getSongByIdSync(mediaId)
                            }
                        } else null
                    }.getOrNull()

                    val bmp: Bitmap? = com.example.looper_android.data.repository.ArtworkLoaderHelper.loadArtworkBitmap(
                        this@PlaybackService,
                        currentSong?.artPath ?: uri.toString(),
                        currentSong?.path ?: player.currentMediaItem?.localConfiguration?.uri?.toString()
                    )
                    bmp ?: throw IllegalArgumentException("Could not load bitmap for $uri")
                }, bitmapExecutor)
            }
        }

        val initialFavBtn = createFavoriteButton(false)
        val initialCloseBtn = createCloseButton()

        val callback = object : MediaSession.Callback {
            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): MediaSession.ConnectionResult {
                val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                    .add(favoriteCommand)
                    .add(closeCommand)
                    .build()
                return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                    .setAvailableSessionCommands(sessionCommands)
                    .setCustomLayout(listOf(initialFavBtn, initialCloseBtn))
                    .build()
            }

            override fun onCustomCommand(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                customCommand: SessionCommand,
                args: Bundle
            ): ListenableFuture<SessionResult> {
                when (customCommand.customAction) {
                    ACTION_FAVORITE -> {
                        try {
                            // MUST read player on Main thread to avoid ExoPlayer thread-check crash!
                            val currentMediaId = player.currentMediaItem?.mediaId?.toLongOrNull()
                            if (currentMediaId != null) {
                                serviceScope.launch(Dispatchers.IO) {
                                    val song = libraryDao.getSongByIdSync(currentMediaId)
                                    if (song != null) {
                                        val newFav = !song.isFavorite
                                        libraryDao.updateFavorite(song.id, newFav)
                                        serviceScope.launch(Dispatchers.Main) {
                                            updateNotificationButtons(newFav)
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    ACTION_CLOSE -> {
                        serviceScope.launch(Dispatchers.Main) {
                            try {
                                queueManager.saveCurrentStateSync()
                                player.pause()
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                                    stopForeground(STOP_FOREGROUND_REMOVE)
                                } else {
                                    @Suppress("DEPRECATION")
                                    stopForeground(true)
                                }
                                val notifManager = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
                                notifManager?.cancelAll()
                                stopSelf()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
        }

        val channelId = "looper_playback_channel_v4"
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val notifManager = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
            try {
                notifManager?.deleteNotificationChannel("media3_default_channel_id")
                notifManager?.deleteNotificationChannel("looper_playback_channel_v2")
                notifManager?.deleteNotificationChannel("looper_playback_channel_v3")
            } catch (_: Exception) {}

            val channel = android.app.NotificationChannel(
                channelId,
                getString(R.string.playback_channel_name),
                android.app.NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Music playback controls"
                setShowBadge(true)
                setSound(null, null)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            notifManager?.createNotificationChannel(channel)
        }

        try {
            val notificationProvider = LooperMediaNotificationProvider(
                this,
                channelId,
                R.string.playback_channel_name
            )
            setMediaNotificationProvider(notificationProvider)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val sessionActivityPendingIntent = launchIntent?.let {
            android.app.PendingIntent.getActivity(
                this,
                0,
                it,
                android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        val sessionBuilder = MediaSession.Builder(this, player)
            .setCallback(callback)
            .setBitmapLoader(bitmapLoader)
            .setCustomLayout(listOf(initialFavBtn, initialCloseBtn))

        if (sessionActivityPendingIntent != null) {
            sessionBuilder.setSessionActivity(sessionActivityPendingIntent)
        }

        val session = sessionBuilder.build()
        mediaSession = session
        addSession(session)

        player.addListener(playerListener)

        serviceScope.launch {
            settingsRepository.skipSilence.collectLatest { enabled ->
                (player as? ExoPlayer)?.skipSilenceEnabled = enabled
            }
        }
        if (player.mediaItemCount == 0) {
            queueManager.restoreLastQueue()
        }

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

        // Real-Time Widget Lyrics Sync Ticker
        serviceScope.launch(Dispatchers.Main) {
            while (true) {
                try {
                    if (player.isPlaying) {
                        val position = player.currentPosition
                        WidgetStateRepository.updatePosition(this@PlaybackService, position)
                    }
                } catch (_: Exception) {}
                kotlinx.coroutines.delay(350L)
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
                                    (position.toFloat() / crossfadeDurationMs.toFloat()).coerceIn(0f, 1f)
                                }
                                remaining < crossfadeDurationMs -> {
                                    (remaining.toFloat() / crossfadeDurationMs.toFloat()).coerceIn(0f, 1f)
                                }
                                else -> 1f
                            }
                            if (kotlin.math.abs(player.volume - newVolume) > 0.02f) {
                                player.volume = newVolume
                            }
                        }
                    } else if (!crossfadeEnabled && player.volume < 1f) {
                        player.volume = 1f
                    }
                } catch (_: Exception) {}
                kotlinx.coroutines.delay(50)
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
                player.pause()
                stopSelf()
            }
        }
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(headsetStateReceiver)
        } catch (_: Exception) {}
        player.removeListener(playerListener)
        bitmapExecutor.shutdown()
        queueManager.saveCurrentStateSync()
        try {
            player.pause()
        } catch (_: Exception) {}
        mediaSession?.run {
            removeSession(this)
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private class LooperMediaNotificationProvider(
        private val context: Context,
        channelId: String,
        channelNameRes: Int
    ) : androidx.media3.session.MediaNotification.Provider {

        private val defaultProvider = androidx.media3.session.DefaultMediaNotificationProvider.Builder(context)
            .setChannelId(channelId)
            .setChannelName(channelNameRes)
            .build().apply {
                setSmallIcon(R.drawable.ic_notification_session)
            }

        override fun createNotification(
            mediaSession: MediaSession,
            customLayout: com.google.common.collect.ImmutableList<CommandButton>,
            actionFactory: androidx.media3.session.MediaNotification.ActionFactory,
            onNotificationChangedCallback: androidx.media3.session.MediaNotification.Provider.Callback
        ): androidx.media3.session.MediaNotification {
            val mediaNotification = defaultProvider.createNotification(
                mediaSession, customLayout, actionFactory, onNotificationChangedCallback
            )
            val notif = mediaNotification.notification
            val isPlaying = mediaSession.player.isPlaying || mediaSession.player.playWhenReady
            notif.category = android.app.Notification.CATEGORY_TRANSPORT
            notif.visibility = android.app.Notification.VISIBILITY_PUBLIC
            if (notif.contentIntent == null && mediaSession.sessionActivity != null) {
                notif.contentIntent = mediaSession.sessionActivity
            }
            if (isPlaying) {
                notif.flags = notif.flags or android.app.Notification.FLAG_ONGOING_EVENT or android.app.Notification.FLAG_NO_CLEAR
            } else {
                notif.flags = (notif.flags and android.app.Notification.FLAG_ONGOING_EVENT.inv())
            }
            try {
                notif.extras.putBoolean("android.requestPromotedOngoing", true)
                notif.extras.putBoolean("com.samsung.android.extra.LIVE_NOTIFICATION", true)
                notif.extras.putBoolean("com.samsung.android.extra.ONGOING_ACTIVITY", true)
                notif.extras.putBoolean("com.samsung.android.extra.NOW_BAR", true)
            } catch (_: Exception) {}
            return mediaNotification
        }

        override fun handleCustomCommand(
            session: MediaSession,
            action: String,
            extras: Bundle
        ): Boolean {
            return defaultProvider.handleCustomCommand(session, action, extras)
        }
    }
}
