package com.example.looper_android.ui.navigation

import android.content.Context
import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.looper_android.ui.components.PremiumNavbar
import com.example.looper_android.ui.library.AlbumsScreen
import com.example.looper_android.ui.library.ArtistsScreen
import com.example.looper_android.ui.library.CollectionDetailScreen
import com.example.looper_android.ui.library.FavoritesScreen
import com.example.looper_android.ui.library.FoldersScreen
import com.example.looper_android.ui.library.GenresScreen
import com.example.looper_android.ui.library.HistoryScreen
import com.example.looper_android.ui.library.PlaylistsScreen
import com.example.looper_android.ui.player.PlaybackViewModel
import com.example.looper_android.ui.player.PlayerScreen
import com.example.looper_android.ui.screens.DashboardScreen
import com.example.looper_android.ui.widgets.PremiumMusicBar
import com.example.looper_android.ui.welcome.WelcomeScreen

private val horizontalSharedAxisEnter: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition) = {
    slideInHorizontally(
        initialOffsetX = { fullWidth -> (fullWidth * 0.35f).toInt() },
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
    ) + fadeIn(
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
    )
}

private val horizontalSharedAxisExit: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition) = {
    slideOutHorizontally(
        targetOffsetX = { fullWidth -> (-fullWidth * 0.25f).toInt() },
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
    ) + fadeOut(
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
    )
}

private val horizontalSharedAxisPopEnter: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition) = {
    slideInHorizontally(
        initialOffsetX = { fullWidth -> (-fullWidth * 0.25f).toInt() },
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
    ) + fadeIn(
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
    )
}

private val horizontalSharedAxisPopExit: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition) = {
    slideOutHorizontally(
        targetOffsetX = { fullWidth -> (fullWidth * 0.35f).toInt() },
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
    ) + fadeOut(
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController(),
    playbackViewModel: PlaybackViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val isWelcomeBypassed = remember {
        val prefs = context.getSharedPreferences("app_settings_prefs", Context.MODE_PRIVATE)
        prefs.getBoolean("is_welcome_bypassed", false)
    }

    val startDestination = if (isWelcomeBypassed) "dashboard" else "welcome"
    var selectedDashboardTabIndex by rememberSaveable { mutableIntStateOf(0) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ""
    val showBottomBar = currentRoute == "dashboard" || currentRoute.startsWith("category/") || currentRoute.startsWith("collection_detail/")

    SharedTransitionLayout {
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(
                    route = "welcome",
                    enterTransition = { fadeIn(animationSpec = tween(350)) },
                    exitTransition = { fadeOut(animationSpec = tween(350)) }
                ) {
                    WelcomeScreen(
                        onNavigateToDashboard = {
                            navController.navigate("dashboard") {
                                popUpTo("welcome") { inclusive = true }
                            }
                        }
                    )
                }

                composable(
                    route = "dashboard",
                    enterTransition = horizontalSharedAxisPopEnter,
                    exitTransition = horizontalSharedAxisExit,
                    popEnterTransition = horizontalSharedAxisPopEnter,
                    popExitTransition = horizontalSharedAxisExit
                ) {
                    DashboardScreen(
                        navController = navController,
                        playbackViewModel = playbackViewModel,
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@composable,
                        selectedTabIndex = selectedDashboardTabIndex,
                        onTabSelected = { selectedDashboardTabIndex = it }
                    )
                }

                composable(
                    route = "player",
                    enterTransition = {
                        slideInVertically(initialOffsetY = { it }, animationSpec = tween(350, easing = FastOutSlowInEasing)) + fadeIn(tween(350))
                    },
                    exitTransition = {
                        slideOutVertically(targetOffsetY = { it }, animationSpec = tween(350, easing = FastOutSlowInEasing)) + fadeOut(tween(350))
                    },
                    popEnterTransition = {
                        fadeIn(tween(350))
                    },
                    popExitTransition = {
                        slideOutVertically(targetOffsetY = { it }, animationSpec = tween(350, easing = FastOutSlowInEasing)) + fadeOut(tween(350))
                    }
                ) {
                    PlayerScreen(
                        viewModel = playbackViewModel,
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@composable,
                        onBack = { navController.popBackStack() },
                        onNavigateToLyrics = { navController.navigate("lyrics") },
                        onNavigateToEqualizer = { navController.navigate("equalizer") },
                        onArtistClick = { artist ->
                            val encoded = Uri.encode(artist)
                            navController.navigate("collection_detail/artist/$encoded")
                        },
                        onAlbumClick = { album ->
                            val encoded = Uri.encode(album)
                            navController.navigate("collection_detail/album/$encoded")
                        }
                    )
                }

                // Category routes with Flutter Horizontal Shared Axis transitions
                composable(
                    route = "category/albums",
                    enterTransition = horizontalSharedAxisEnter,
                    exitTransition = horizontalSharedAxisExit,
                    popEnterTransition = horizontalSharedAxisPopEnter,
                    popExitTransition = horizontalSharedAxisPopExit
                ) {
                    AlbumsScreen(
                        onBackClick = { navController.popBackStack() },
                        onAlbumClick = { album ->
                            val encoded = Uri.encode(album)
                            navController.navigate("collection_detail/album/$encoded")
                        }
                    )
                }

                composable(
                    route = "category/artists",
                    enterTransition = horizontalSharedAxisEnter,
                    exitTransition = horizontalSharedAxisExit,
                    popEnterTransition = horizontalSharedAxisPopEnter,
                    popExitTransition = horizontalSharedAxisPopExit
                ) {
                    ArtistsScreen(
                        onBackClick = { navController.popBackStack() },
                        onArtistClick = { artist ->
                            val encoded = Uri.encode(artist)
                            navController.navigate("collection_detail/artist/$encoded")
                        }
                    )
                }

                composable(
                    route = "category/playlists",
                    enterTransition = horizontalSharedAxisEnter,
                    exitTransition = horizontalSharedAxisExit,
                    popEnterTransition = horizontalSharedAxisPopEnter,
                    popExitTransition = horizontalSharedAxisPopExit
                ) {
                    PlaylistsScreen(
                        onBackClick = { navController.popBackStack() },
                        onPlaylistClick = { playlist ->
                            val encoded = Uri.encode(playlist)
                            navController.navigate("collection_detail/playlist/$encoded")
                        }
                    )
                }

                composable(
                    route = "category/folders",
                    enterTransition = horizontalSharedAxisEnter,
                    exitTransition = horizontalSharedAxisExit,
                    popEnterTransition = horizontalSharedAxisPopEnter,
                    popExitTransition = horizontalSharedAxisPopExit
                ) {
                    FoldersScreen(
                        onBackClick = { navController.popBackStack() },
                        onFolderClick = { folder ->
                            val encoded = Uri.encode(folder)
                            navController.navigate("collection_detail/folder/$encoded")
                        }
                    )
                }

                composable(
                    route = "category/genres",
                    enterTransition = horizontalSharedAxisEnter,
                    exitTransition = horizontalSharedAxisExit,
                    popEnterTransition = horizontalSharedAxisPopEnter,
                    popExitTransition = horizontalSharedAxisPopExit
                ) {
                    GenresScreen(
                        onBackClick = { navController.popBackStack() },
                        onGenreClick = { genre ->
                            val encoded = Uri.encode(genre)
                            navController.navigate("collection_detail/genre/$encoded")
                        }
                    )
                }

                composable(
                    route = "category/favorites",
                    enterTransition = horizontalSharedAxisEnter,
                    exitTransition = horizontalSharedAxisExit,
                    popEnterTransition = horizontalSharedAxisPopEnter,
                    popExitTransition = horizontalSharedAxisPopExit
                ) {
                    FavoritesScreen(onBackClick = { navController.popBackStack() })
                }

                composable(
                    route = "category/history",
                    enterTransition = horizontalSharedAxisEnter,
                    exitTransition = horizontalSharedAxisExit,
                    popEnterTransition = horizontalSharedAxisPopEnter,
                    popExitTransition = horizontalSharedAxisPopExit
                ) {
                    HistoryScreen(onBackClick = { navController.popBackStack() })
                }

                // Collection Detail route with Flutter Horizontal Shared Axis transitions
                composable(
                    route = "collection_detail/{type}/{title}",
                    arguments = listOf(
                        navArgument("type") { type = NavType.StringType },
                        navArgument("title") { type = NavType.StringType }
                    ),
                    enterTransition = horizontalSharedAxisEnter,
                    exitTransition = horizontalSharedAxisExit,
                    popEnterTransition = horizontalSharedAxisPopEnter,
                    popExitTransition = horizontalSharedAxisPopExit
                ) { backStackEntry ->
                    val type = backStackEntry.arguments?.getString("type") ?: "album"
                    val rawTitle = backStackEntry.arguments?.getString("title") ?: ""
                    val title = Uri.decode(rawTitle)

                    CollectionDetailScreen(
                        title = title,
                        type = type,
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable("lyrics") { }
                composable("equalizer") { }
                composable("settings") { }
            }

            // Persistent Mini Player and Bottom Navigation Overlay
            if (showBottomBar) {
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

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 16.dp, top = 2.dp)
                ) {
                    PremiumMusicBar(
                        viewModel = playbackViewModel,
                        onNavigateToPlayer = { navController.navigate("player") },
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = null
                    )

                    PremiumNavbar(
                        currentIndex = selectedDashboardTabIndex,
                        onTap = { index ->
                            selectedDashboardTabIndex = index
                            if (currentRoute != "dashboard") {
                                navController.popBackStack("dashboard", inclusive = false)
                            }
                        },
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = null
                    )
                }
            }
        }
    }
}
