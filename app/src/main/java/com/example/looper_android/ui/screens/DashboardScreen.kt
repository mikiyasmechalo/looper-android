package com.example.looper_android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import com.example.looper_android.ui.components.PremiumNavbar
import com.example.looper_android.ui.library.AllSongsScreen
import com.example.looper_android.ui.screens.tabs.HomeTab
import com.example.looper_android.ui.screens.tabs.LibraryTab

@Composable
fun DashboardScreen(
    navController: NavController,
    playbackViewModel: com.example.looper_android.ui.player.PlaybackViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    var selectedTabIndex by remember { mutableIntStateOf(1) } // Default to Songs Tab
    val allSongsViewModel: com.example.looper_android.ui.library.AllSongsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val isDbLoaded by allSongsViewModel.isDbLoaded.collectAsState()
    val songs by allSongsViewModel.songs.collectAsState()
    
    var hasPermission by remember {
        androidx.compose.runtime.mutableStateOf(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            allSongsViewModel.loadSongs()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            val permission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                android.Manifest.permission.READ_MEDIA_AUDIO
            } else {
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            }
            permissionLauncher.launch(permission)
        }
    }

    LaunchedEffect(hasPermission, isDbLoaded) {
        if (hasPermission && isDbLoaded && songs.isEmpty()) {
            allSongsViewModel.loadSongs()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF11110E))
    ) {
        // Main Tab Content
        Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            Box(modifier = Modifier.fillMaxSize()
                .alpha(if (selectedTabIndex == 0) 1f else 0f)
                .zIndex(if (selectedTabIndex == 0) 1f else 0f)
            ) {
                HomeTab()
            }
            Box(modifier = Modifier.fillMaxSize()
                .alpha(if (selectedTabIndex == 1) 1f else 0f)
                .zIndex(if (selectedTabIndex == 1) 1f else 0f)
            ) {
                AllSongsScreen(onShufflePlayClick = {})
            }
            Box(modifier = Modifier.fillMaxSize()
                .alpha(if (selectedTabIndex == 2) 1f else 0f)
                .zIndex(if (selectedTabIndex == 2) 1f else 0f)
            ) {
                LibraryTab()
            }
        }

        // Gradient fading bottom to black behind navbar (like Flutter)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(240.dp) // Taller gradient for mini player
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.35f to Color.Black.copy(alpha = 0.45f),
                        0.7f to Color.Black.copy(alpha = 0.8f),
                        1.0f to Color.Black
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp, top = 2.dp)
        ) {
            com.example.looper_android.ui.widgets.PremiumMusicBar(
                viewModel = playbackViewModel,
                onNavigateToPlayer = { navController.navigate("player") }
            )
            
            PremiumNavbar(
                currentIndex = selectedTabIndex,
                onTap = { selectedTabIndex = it }
            )
        }
    }
}
