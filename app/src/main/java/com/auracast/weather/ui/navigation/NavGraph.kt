package com.auracast.weather.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.auracast.weather.ui.screens.HomeScreen
import com.auracast.weather.ui.screens.OnboardingScreen
import com.auracast.weather.ui.screens.RadarScreen
import com.auracast.weather.ui.screens.ReportScreen
import com.auracast.weather.ui.screens.SettingsScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val RADAR = "radar"
    const val REPORT = "report"
    const val SETTINGS = "settings"
}

@Composable
fun AuraNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.ONBOARDING) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.HOME) { HomeScreen() }
        composable(Routes.RADAR) { RadarScreen() }
        composable(Routes.REPORT) { ReportScreen() }
        composable(Routes.SETTINGS) { SettingsScreen() }
    }
}
