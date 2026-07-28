package com.example.looper_android.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.looper_android.ui.player.PlaybackViewModel
import com.example.looper_android.ui.player.PlayerScreen
import com.example.looper_android.ui.screens.DashboardScreen

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController()
) {
    SharedTransitionLayout {
        NavHost(
            navController = navController,
            startDestination = "dashboard"
        ) {
            composable(
                route = "dashboard",
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None }
            ) {
                DashboardScreen(
                    navController = navController,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@composable
                )
            }
            composable(
                route = "player",
                enterTransition = {
                    fadeIn(animationSpec = tween(350))
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(350))
                },
                popEnterTransition = {
                    fadeIn(animationSpec = tween(350))
                },
                popExitTransition = {
                    fadeOut(animationSpec = tween(350))
                }
            ) {
                val playbackViewModel: PlaybackViewModel = hiltViewModel()
                PlayerScreen(
                    viewModel = playbackViewModel,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@composable,
                    onBack = { navController.popBackStack() },
                    onNavigateToLyrics = { navController.navigate("lyrics") },
                    onNavigateToEqualizer = { navController.navigate("equalizer") },
                    onArtistClick = { artist -> },
                    onAlbumClick = { album -> }
                )
            }
            composable("lyrics") {
                // Lyrics Screen
            }
            composable("equalizer") {
                // Equalizer Screen
            }
            composable("settings") {
                // Settings Screen
            }
        }
    }
}
