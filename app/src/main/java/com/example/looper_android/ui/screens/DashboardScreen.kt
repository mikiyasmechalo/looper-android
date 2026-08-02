package com.example.looper_android.ui.screens

import android.net.Uri
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.looper_android.ui.components.PremiumNavbar
import com.example.looper_android.ui.library.AllSongsScreen
import com.example.looper_android.ui.screens.tabs.HomeTab
import com.example.looper_android.ui.screens.tabs.LibraryTab
import kotlinx.coroutines.launch

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    playbackViewModel: com.example.looper_android.ui.player.PlaybackViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    selectedTabIndex: Int = 0,
    onTabSelected: (Int) -> Unit = {}
) {
    val pagerState = rememberPagerState(initialPage = selectedTabIndex, pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()

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

    LaunchedEffect(selectedTabIndex) {
        if (pagerState.currentPage != selectedTabIndex) {
            pagerState.scrollToPage(selectedTabIndex)
        }
    }

    // Synchronize pager swipe with selectedTabIndex
    LaunchedEffect(pagerState.currentPage) {
        if (selectedTabIndex != pagerState.currentPage) {
            onTabSelected(pagerState.currentPage)
        }
    }

    val density = androidx.compose.ui.platform.LocalDensity.current
    val topInset: androidx.compose.ui.unit.Dp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    var savedTopPx by remember { mutableStateOf<Float?>(null) }
    
    val currentTopPx: Float = with(density) { topInset.toPx() }
    val saved: Float? = savedTopPx
    if (saved == null && currentTopPx > 0f) {
        savedTopPx = currentTopPx
    }
    val targetPx: Float = saved ?: currentTopPx
    val stableTopDp = with(density) { targetPx.toDp() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF11110E))
    ) {
        // Main Tab Content with Horizontal Page Transition
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = stableTopDp)
        ) { page ->
            when (page) {
                0 -> HomeTab(
                    onNavigateToSongs = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(1)
                        }
                    },
                    onNavigateToArtists = { navController.navigate("category/artists") },
                    onNavigateToAlbums = { navController.navigate("category/albums") },
                    onNavigateToGenres = { navController.navigate("category/genres") },
                    onNavigateToArtist = { artist ->
                        val encoded = Uri.encode(artist)
                        navController.navigate("collection_detail/artist/$encoded")
                    },
                    onNavigateToAlbum = { album ->
                        val encoded = Uri.encode(album)
                        navController.navigate("collection_detail/album/$encoded")
                    }
                )
                1 -> AllSongsScreen(
                    onShufflePlayClick = {},
                    onNavigateToArtist = { artist ->
                        val encoded = Uri.encode(artist)
                        navController.navigate("collection_detail/artist/$encoded")
                    },
                    onNavigateToAlbum = { album ->
                        val encoded = Uri.encode(album)
                        navController.navigate("collection_detail/album/$encoded")
                    }
                )
                2 -> LibraryTab(
                    onNavigateToCategory = { categoryKey ->
                        navController.navigate("category/$categoryKey")
                    }
                )
            }
        }
    }
}
