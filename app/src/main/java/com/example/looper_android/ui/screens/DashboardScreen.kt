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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import dev.chrisbanes.haze.haze
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
import com.example.looper_android.ui.search.SearchScreen
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val initialPage = remember {
        val prefs = context.getSharedPreferences("app_settings_prefs", android.content.Context.MODE_PRIVATE)
        if (prefs.getString("default_screen", "home") == "songs") 1 else 0
    }
    // Default screen is Home tab (index 0) or Songs tab (index 1)
    var selectedTabIndex by rememberSaveable { androidx.compose.runtime.mutableIntStateOf(initialPage) }
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()
    val allSongsListState = androidx.compose.foundation.lazy.rememberLazyListState()

    val childNavController = rememberNavController()

    val allSongsViewModel: com.example.looper_android.ui.library.AllSongsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val homeViewModel: com.example.looper_android.ui.screens.tabs.HomeViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val libraryViewModel: com.example.looper_android.ui.screens.tabs.LibraryViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    
    val isDbLoaded by allSongsViewModel.isDbLoaded.collectAsState()
    val songs by allSongsViewModel.songs.collectAsState()
    val playbackState by playbackViewModel.playbackState.collectAsState()
    
    var hasPermission by remember {
        androidx.compose.runtime.mutableStateOf(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }
        )
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            navController.navigate("permission") {
                popUpTo("dashboard") { inclusive = true }
            }
        }
    }

    if (!hasPermission) return

    val currentBackStack by childNavController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route

    val navTo by navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow<String?>("navigate_to", null)
        ?.collectAsState() ?: remember { mutableStateOf(null) }

    LaunchedEffect(navTo) {
        navTo?.let { target ->
            navController.currentBackStackEntry?.savedStateHandle?.set("navigate_to", null)
            val parts = target.split(":", limit = 2)
            if (parts.size == 2) {
                val (type, name) = parts
                val encoded = Uri.encode(name)
                childNavController.navigate("collection_detail/$type/$encoded")
            }
        }
    }

    // Sync selectedTabIndex with Pager (only when on "tabs" route)
    LaunchedEffect(pagerState.currentPage, currentRoute) {
        if (currentRoute == "tabs" || currentRoute == null) {
            selectedTabIndex = pagerState.currentPage
        }
    }

    // Determine padding based on current route
    val isTopLevel = currentRoute == "tabs" || currentRoute == null
    val density = androidx.compose.ui.platform.LocalDensity.current
    val topInsets = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val currentTopPx = with(density) { topInsets.toPx() }
    
    // Remember the top inset pixels
    var saved by remember { androidx.compose.runtime.mutableStateOf<Float?>(null) }
    var savedTopPx by remember { androidx.compose.runtime.mutableStateOf(0f) }
    
    if (saved == null && currentTopPx > 0f) {
        savedTopPx = currentTopPx
    }
    val targetPx: Float = saved ?: currentTopPx
    val stableTopDp = with(density) { targetPx.toDp() }

    val settingsViewModel: com.example.looper_android.ui.settings.SettingsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val darkTheme by settingsViewModel.darkTheme.collectAsState()
    val dynamicTheming by settingsViewModel.dynamicTheming.collectAsState()
    val effectiveDarkTheme = darkTheme && !dynamicTheming
    val disableBlur by settingsViewModel.disableBlur.collectAsState()
    val enablePlayerGradient by settingsViewModel.enablePlayerGradient.collectAsState()
    val keepBackgroundGradient by settingsViewModel.keepBackgroundGradient.collectAsState()
    val currentSongArtPath = playbackState.currentSong?.artPath
    
    val animatedAccentColor by androidx.compose.animation.animateColorAsState(
        targetValue = playbackState.accentColor,
        animationSpec = tween(durationMillis = 600),
        label = "dashboardAccentAnim"
    )

    val hazeState = remember { dev.chrisbanes.haze.HazeState() }

    androidx.compose.runtime.CompositionLocalProvider(
        com.example.looper_android.ui.theme.LocalAccentColor provides playbackState.accentColor
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (effectiveDarkTheme) Color.Black else androidx.compose.material3.MaterialTheme.colorScheme.background)
                    .haze(hazeState)
            ) {
            val shouldShowBlurredArt = !effectiveDarkTheme && dynamicTheming && !currentSongArtPath.isNullOrEmpty()
            if (shouldShowBlurredArt) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = coil.request.ImageRequest.Builder(LocalContext.current)
                            .data(currentSongArtPath)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(radius = 24.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.72f))
                    )
                }
            } else if ((!effectiveDarkTheme && dynamicTheming) || (enablePlayerGradient && keepBackgroundGradient)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawWithCache {
                            onDrawBehind {
                                drawRect(
                                    brush = Brush.radialGradient(
                                        0.0f to animatedAccentColor.copy(alpha = 0.55f),
                                        0.35f to animatedAccentColor.copy(alpha = 0.40f),
                                        0.60f to animatedAccentColor.copy(alpha = 0.22f),
                                        0.80f to animatedAccentColor.copy(alpha = 0.06f),
                                        1.0f to Color.Black,
                                        center = androidx.compose.ui.geometry.Offset(size.width, 0f),
                                        radius = size.height * 0.88f
                                    )
                                )
                            }
                        }
                )
            }
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
                            viewModel = homeViewModel,
                            onNavigateToSearch = { childNavController.navigate("search/home") },
                            onNavigateToSettings = { childNavController.navigate("settings") },
                            onNavigateToSongs = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(1)
                                }
                            },
                            onNavigateToArtists = { childNavController.navigate("category/artists") },
                            onNavigateToAlbums = { childNavController.navigate("category/albums") },
                            onNavigateToGenres = { childNavController.navigate("category/genres") },
                            onArtistClick = { artistName ->
                                val encoded = android.net.Uri.encode(artistName)
                                childNavController.navigate("collection_detail/artist/$encoded")
                            },
                            onAlbumClick = { albumName ->
                                val encoded = android.net.Uri.encode(albumName)
                                childNavController.navigate("collection_detail/album/$encoded")
                            },
                            onGenreClick = { genreName ->
                                val encoded = android.net.Uri.encode(genreName)
                                childNavController.navigate("collection_detail/genre/$encoded")
                            }
                        )
                        1 -> AllSongsScreen(
                            viewModel = allSongsViewModel,
                            listState = allSongsListState,
                            onNavigateToSearch = { childNavController.navigate("search/songs") },
                            onNavigateToSettings = { childNavController.navigate("settings") },
                            onNavigateToArtist = { artist ->
                                val encoded = Uri.encode(artist)
                                childNavController.navigate("collection_detail/artist/$encoded")
                            },
                            onNavigateToAlbum = { album ->
                                val encoded = Uri.encode(album)
                                childNavController.navigate("collection_detail/album/$encoded")
                            }
                        )
                        2 -> LibraryTab(
                            viewModel = libraryViewModel,
                            onNavigateToSearch = { childNavController.navigate("search/library") },
                            onNavigateToSettings = { childNavController.navigate("settings") },
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
                    onNavigateToSearch = { childNavController.navigate("search/albums") },
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
                    onNavigateToSearch = { childNavController.navigate("search/artists") },
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
                route = "category/queue",
                enterTransition = { categoryEnterTransition },
                exitTransition = { categoryExitTransition },
                popEnterTransition = { categoryPopEnterTransition },
                popExitTransition = { categoryPopExitTransition }
            ) {
                com.example.looper_android.ui.library.QueuesScreen(
                    onBackClick = { childNavController.popBackStack() },
                    viewModel = playbackViewModel
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
                    onNavigateToSearch = { childNavController.navigate("search/genres") },
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
                    onBackClick = { childNavController.popBackStack() },
                    onNavigateToArtist = { artist ->
                        val encoded = Uri.encode(artist)
                        childNavController.navigate("collection_detail/artist/$encoded")
                    },
                    onNavigateToAlbum = { album ->
                        val encoded = Uri.encode(album)
                        childNavController.navigate("collection_detail/album/$encoded")
                    }
                )
            }
            
            composable(
                route = "search/{context}",
                enterTransition = { categoryEnterTransition },
                exitTransition = { categoryExitTransition },
                popEnterTransition = { categoryPopEnterTransition },
                popExitTransition = { categoryPopExitTransition }
            ) { backStackEntry ->
                val context = backStackEntry.arguments?.getString("context") ?: "global"
                SearchScreen(
                    searchContext = context,
                    onBackClick = { childNavController.popBackStack() },
                    onArtistClick = { artistName ->
                        val encoded = android.net.Uri.encode(artistName)
                        childNavController.navigate("collection_detail/artist/$encoded")
                    },
                    onAlbumClick = { albumName ->
                        val encoded = android.net.Uri.encode(albumName)
                        childNavController.navigate("collection_detail/album/$encoded")
                    },
                    onGenreClick = { genreName ->
                        val encoded = android.net.Uri.encode(genreName)
                        childNavController.navigate("collection_detail/genre/$encoded")
                    }
                )
            }

            composable(
                route = "settings",
                enterTransition = { categoryEnterTransition },
                exitTransition = { categoryExitTransition },
                popEnterTransition = { categoryPopEnterTransition },
                popExitTransition = { categoryPopExitTransition }
            ) {
                com.example.looper_android.ui.settings.SettingsScreen(
                    onBackClick = { childNavController.popBackStack() },
                    onNavigateToCategory = { id -> childNavController.navigate("settings/category/$id") },
                    accentColor = playbackState.accentColor
                )
            }

            composable(
                route = "settings/category/{categoryId}",
                enterTransition = { categoryEnterTransition },
                exitTransition = { categoryExitTransition },
                popEnterTransition = { categoryPopEnterTransition },
                popExitTransition = { categoryPopExitTransition },
                arguments = listOf(navArgument("categoryId") { type = NavType.StringType })
            ) { backStackEntry ->
                val catId = backStackEntry.arguments?.getString("categoryId") ?: "theme"
                val title = when (catId) {
                    "theme" -> "Theme"
                    "dashboard" -> "Home Screen"
                    "playback" -> "Playback & Language"
                    "audio_playback" -> "Audio & Playback"
                    "library" -> "Music Library"
                    "about" -> "About & Maintainers"
                    "support_us" -> "Support Us"
                    else -> "Settings"
                }
                com.example.looper_android.ui.settings.SettingsCategoryScreen(
                    categoryId = catId,
                    title = title,
                    onBackClick = { childNavController.popBackStack() },
                    accentColor = playbackState.accentColor
                )
            }
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
            val isBlurEnabled = dynamicTheming && !disableBlur

            com.example.looper_android.ui.widgets.PremiumMusicBar(
                viewModel = playbackViewModel,
                onNavigateToPlayer = { navController.navigate("player") },
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                enableBlur = isBlurEnabled,
                hazeState = hazeState,
                darkTheme = effectiveDarkTheme
            )
            
            PremiumNavbar(
                currentIndex = selectedTabIndex,
                onTap = { index ->
                    if (index == 1 && selectedTabIndex == 1 && (currentRoute == "tabs" || currentRoute == null)) {
                        coroutineScope.launch {
                            allSongsListState.animateScrollToItem(0)
                        }
                    }
                    if (childNavController.previousBackStackEntry != null) {
                        childNavController.popBackStack("tabs", inclusive = false)
                    }
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                },
                accentColor = playbackState.accentColor,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                enableBlur = isBlurEnabled,
                hazeState = hazeState,
                darkTheme = effectiveDarkTheme
            )
        }
    }
}
}
