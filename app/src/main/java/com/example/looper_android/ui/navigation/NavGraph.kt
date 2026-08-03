package com.example.looper_android.ui.navigation

import android.content.Context
import android.net.Uri
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.looper_android.ui.player.PlaybackViewModel
import com.example.looper_android.ui.player.PlayerScreen
import com.example.looper_android.ui.screens.DashboardScreen
import com.example.looper_android.ui.welcome.WelcomeScreen

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

    SharedTransitionLayout {
        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {
            composable(
                route = "welcome",
                enterTransition = { fadeIn(animationSpec = tween(400)) },
                exitTransition = { fadeOut(animationSpec = tween(400)) }
            ) {
                WelcomeScreen(
                    onNavigateToDashboard = {
                        navController.navigate("dashboard") {
                            popUpTo("welcome") { inclusive = true }
                        }
                    }
                )
            }

            // Dashboard hosts main tabs + category & collection detail sub-routes with persistent Mini Player & Navbar!
            composable(
                route = "dashboard",
                enterTransition = { fadeIn(animationSpec = tween(400)) },
                exitTransition = { fadeOut(animationSpec = tween(400)) },
                popEnterTransition = { fadeIn(animationSpec = tween(400)) },
                popExitTransition = { fadeOut(animationSpec = tween(400)) }
            ) {
                DashboardScreen(
                    navController = navController,
                    playbackViewModel = playbackViewModel,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@composable
                )
            }

            composable(
                route = "player",
                enterTransition = { fadeIn(animationSpec = tween(400)) },
                exitTransition = { fadeOut(animationSpec = tween(400)) },
                popEnterTransition = { fadeIn(animationSpec = tween(400)) },
                popExitTransition = { fadeOut(animationSpec = tween(400)) }
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

            composable("lyrics") { }
            composable("equalizer") { }
            composable("settings") { }
        }
    }
}
