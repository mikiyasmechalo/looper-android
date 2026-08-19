package com.example.looper_android

import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.looper_android.playback.PlaybackService
import com.example.looper_android.ui.navigation.AppNavGraph
import com.example.looper_android.ui.theme.LooperandroidTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var mediaController: MediaController? = null
    private var controllerFuture: com.google.common.util.concurrent.ListenableFuture<MediaController>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setBackgroundDrawableResource(android.R.color.black)
        enableEdgeToEdge()
        
        val sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))

        setContent {
            val settingsViewModel: com.example.looper_android.ui.settings.SettingsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            val darkTheme by settingsViewModel.darkTheme.collectAsState()
            val dynamicTheming by settingsViewModel.dynamicTheming.collectAsState()
            val customAccentColorInt by settingsViewModel.customAccentColor.collectAsState()

            LooperandroidTheme(
                darkTheme = darkTheme,
                dynamicTheming = dynamicTheming,
                customAccentColor = androidx.compose.ui.graphics.Color(customAccentColorInt)
            ) {
                AppNavGraph()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        controllerFuture?.let { 
            try {
                MediaController.releaseFuture(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        mediaController = null
    }
}