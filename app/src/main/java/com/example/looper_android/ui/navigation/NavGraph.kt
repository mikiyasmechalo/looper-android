package com.example.looper_android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.looper_android.ui.screens.DashboardScreen

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = "dashboard"
    ) {
        composable("dashboard") {
            DashboardScreen(navController = navController)
        }
        composable("player") {
            // Player Screen will be mapped here later
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
