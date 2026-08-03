package com.example.looper_android.ui.screens

import android.net.Uri
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.looper_android.ui.components.PremiumNavbar
import com.example.looper_android.ui.library.AllSongsScreen
import com.example.looper_android.ui.library.AlbumsScreen
import com.example.looper_android.ui.library.ArtistsScreen
import com.example.looper_android.ui.library.CollectionDetailScreen
import com.example.looper_android.ui.library.FavoritesScreen
import com.example.looper_android.ui.library.FoldersScreen
import com.example.looper_android.ui.library.GenresScreen
import com.example.looper_android.ui.library.HistoryScreen
import com.example.looper_android.ui.library.PlaylistsScreen
import com.example.looper_android.ui.screens.tabs.HomeTab
import com.example.looper_android.ui.screens.tabs.LibraryTab
import kotlinx.coroutines.launch

// Material Shared Axis Horizontal Transitions (Matches Flutter 1-to-1)
private val categoryEnterTransition = slideInHorizontally(
    initialOffsetX = { fullWidth -> (fullWidth * 0.15f).toInt() },
    animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
) + fadeIn(animationSpec = tween(durationMillis = 500))

private val categoryExitTransition = slideOutHorizontally(
    targetOffsetX = { fullWidth -> -(fullWidth * 0.15f).toInt() },
    animationSpec = tween(durationMillis = 300, easing = FastOutLinearInEasing)
) + fadeOut(animationSpec = tween(durationMillis = 200))

private val categoryPopEnterTransition = slideInHorizontally(
    initialOffsetX = { fullWidth -> -(fullWidth * 0.15f).toInt() },
    animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
) + fadeIn(animationSpec = tween(durationMillis = 400))

private val categoryPopExitTransition = slideOutHorizontally(
    targetOffsetX = { fullWidth -> (fullWidth * 0.15f).toInt() },
    animationSpec = tween(durationMillis = 250, easing = FastOutLinearInEasing)
) + fadeOut(animationSpec = tween(durationMillis = 180))

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    playbackViewModel: com.example.looper_android.ui.player.PlaybackViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    // Default screen is Home tab (index 0)
    var selectedTabIndex by rememberSaveable { androidx.compose.runtime.mutableIntStateOf(0) }
    val pagerState = rememberPagerState(initialPage = selectedTabIndex, pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()

    val childNavController = rememberNavController()

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

    // Synchronize pager swipe with selectedTabIndex
    LaunchedEffect(pagerState.currentPage) {
        selectedTabIndex = pagerState.currentPage
    }
    // 1. Observe back stack changes safely
    val currentBackStackEntry by navController.currentBackStackEntryAsState()

    // 2. Safely retrieve the parent back stack entry as a nullable type
    val dashboardEntry = remember(currentBackStackEntry) {
        runCatching { navController.getBackStackEntry("dashboard") }.getOrNull()
    }

    // 3. Collect StateFlow safely using safe calls (?.let)
    val navigateTo by remember(dashboardEntry) {
        dashboardEntry?.savedStateHandle?.getStateFlow<String?>("navigate_to", null)
    }?.collectAsState() ?: remember { mutableStateOf(null) }

    // 4. Handle navigation effect
    LaunchedEffect(navigateTo) {
        navigateTo?.let { navString ->
            // Use safe call when updating savedStateHandle
            dashboardEntry?.savedStateHandle?.set("navigate_to", null)

            val parts = navString.split(":", limit = 2)
            if (parts.size == 2) {
                val type = parts[0]
                val value = Uri.encode(parts[1])
                childNavController.navigate("collection_detail/$type/$value")
            }
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
        // Content area with nested NavHost for sub-routes (Albums, Artists, Detail views)
        // Mini player & navbar remain persistently pinned at the bottom on top!
        NavHost(
            navController = childNavController,
            startDestination = "tabs",
            modifier = Modifier.fillMaxSize()
        ) {
            composable("tabs") {
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
                            onNavigateToArtists = { childNavController.navigate("category/artists") },
                            onNavigateToAlbums = { childNavController.navigate("category/albums") },
                            onNavigateToGenres = { childNavController.navigate("category/genres") }
                        )
                        1 -> AllSongsScreen(onShufflePlayClick = {})
                        2 -> LibraryTab(
                            onNavigateToCategory = { categoryKey ->
                                childNavController.navigate("category/$categoryKey")
                            }
                        )
                    }
                }
            }

            // Category Sub-screens
            composable(
                route = "category/albums",
                enterTransition = { categoryEnterTransition },
                exitTransition = { categoryExitTransition },
                popEnterTransition = { categoryPopEnterTransition },
                popExitTransition = { categoryPopExitTransition }
            ) {
                AlbumsScreen(
                    onBackClick = { childNavController.popBackStack() },
                    onAlbumClick = { album ->
                        val encoded = Uri.encode(album)
                        childNavController.navigate("collection_detail/album/$encoded")
                    }
                )
            }

            composable(
                route = "category/artists",
                enterTransition = { categoryEnterTransition },
                exitTransition = { categoryExitTransition },
                popEnterTransition = { categoryPopEnterTransition },
                popExitTransition = { categoryPopExitTransition }
            ) {
                ArtistsScreen(
                    onBackClick = { childNavController.popBackStack() },
                    onArtistClick = { artist ->
                        val encoded = Uri.encode(artist)
                        childNavController.navigate("collection_detail/artist/$encoded")
                    }
                )
            }

            composable(
                route = "category/playlists",
                enterTransition = { categoryEnterTransition },
                exitTransition = { categoryExitTransition },
                popEnterTransition = { categoryPopEnterTransition },
                popExitTransition = { categoryPopExitTransition }
            ) {
                PlaylistsScreen(
                    onBackClick = { childNavController.popBackStack() },
                    onPlaylistClick = { playlist ->
                        val encoded = Uri.encode(playlist)
                        childNavController.navigate("collection_detail/playlist/$encoded")
                    }
                )
            }

            composable(
                route = "category/folders",
                enterTransition = { categoryEnterTransition },
                exitTransition = { categoryExitTransition },
                popEnterTransition = { categoryPopEnterTransition },
                popExitTransition = { categoryPopExitTransition }
            ) {
                FoldersScreen(
                    onBackClick = { childNavController.popBackStack() },
                    onFolderClick = { folder ->
                        val encoded = Uri.encode(folder)
                        childNavController.navigate("collection_detail/folder/$encoded")
                    }
                )
            }

            composable(
                route = "category/genres",
                enterTransition = { categoryEnterTransition },
                exitTransition = { categoryExitTransition },
                popEnterTransition = { categoryPopEnterTransition },
                popExitTransition = { categoryPopExitTransition }
            ) {
                GenresScreen(
                    onBackClick = { childNavController.popBackStack() },
                    onGenreClick = { genre ->
                        val encoded = Uri.encode(genre)
                        childNavController.navigate("collection_detail/genre/$encoded")
                    }
                )
            }

            composable(
                route = "category/favorites",
                enterTransition = { categoryEnterTransition },
                exitTransition = { categoryExitTransition },
                popEnterTransition = { categoryPopEnterTransition },
                popExitTransition = { categoryPopExitTransition }
            ) {
                FavoritesScreen(onBackClick = { childNavController.popBackStack() })
            }

            composable(
                route = "category/history",
                enterTransition = { categoryEnterTransition },
                exitTransition = { categoryExitTransition },
                popEnterTransition = { categoryPopEnterTransition },
                popExitTransition = { categoryPopExitTransition }
            ) {
                HistoryScreen(onBackClick = { childNavController.popBackStack() })
            }

            // Collection Detail route
            composable(
                route = "collection_detail/{type}/{title}",
                arguments = listOf(
                    navArgument("type") { type = NavType.StringType },
                    navArgument("title") { type = NavType.StringType }
                ),
                enterTransition = { categoryEnterTransition },
                exitTransition = { categoryExitTransition },
                popEnterTransition = { categoryPopEnterTransition },
                popExitTransition = { categoryPopExitTransition }
            ) { backStackEntry ->
                val type = backStackEntry.arguments?.getString("type") ?: "album"
                val rawTitle = backStackEntry.arguments?.getString("title") ?: ""
                val title = Uri.decode(rawTitle)

                CollectionDetailScreen(
                    title = title,
                    type = type,
                    onBackClick = { childNavController.popBackStack() }
                )
            }
        }

        // Gradient fading bottom to black behind navbar (like Flutter)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(240.dp)
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.35f to Color.Black.copy(alpha = 0.45f),
                        0.7f to Color.Black.copy(alpha = 0.8f),
                        1.0f to Color.Black
                    )
                )
        )

        // PERSISTENT MINI PLAYER & NAVBAR (Always on top across all library items)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp, top = 2.dp)
        ) {
            com.example.looper_android.ui.widgets.PremiumMusicBar(
                viewModel = playbackViewModel,
                onNavigateToPlayer = { navController.navigate("player") },
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope
            )
            
            PremiumNavbar(
                currentIndex = selectedTabIndex,
                onTap = { index ->
                    if (childNavController.previousBackStackEntry != null) {
                        childNavController.popBackStack("tabs", inclusive = false)
                    }
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                },
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope
            )
        }
    }
}
