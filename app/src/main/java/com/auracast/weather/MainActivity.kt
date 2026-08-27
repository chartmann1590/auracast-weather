package com.auracast.weather

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.auracast.weather.data.ads.AdManager
import com.auracast.weather.ui.navigation.AuraNavGraph
import com.auracast.weather.ui.navigation.Routes
import com.auracast.weather.ui.theme.AuraCastTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var adManager: AdManager

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        adManager.initialize()
        setContent {
            AuraCastTheme {
                val navController = rememberNavController()
                val backStack by navController.currentBackStackEntryAsState()
                val currentRoute = backStack?.destination?.route

                Scaffold(
                    bottomBar = {
                        if (currentRoute != Routes.ONBOARDING && currentRoute != null) {
                            NavigationBar {
                                NavigationBarItem(
                                    selected = currentRoute == Routes.HOME,
                                    onClick = { navController.navigate(Routes.HOME) { launchSingleTop = true } },
                                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                                    label = { Text("Home") }
                                )
                                NavigationBarItem(
                                    selected = currentRoute == Routes.RADAR,
                                    onClick = { navController.navigate(Routes.RADAR) { launchSingleTop = true } },
                                    icon = { Icon(Icons.Filled.Map, contentDescription = null) },
                                    label = { Text("Radar") }
                                )
                                NavigationBarItem(
                                    selected = currentRoute == Routes.REPORT,
                                    onClick = { navController.navigate(Routes.REPORT) { launchSingleTop = true } },
                                    icon = { Icon(Icons.Filled.Mic, contentDescription = null) },
                                    label = { Text("Report") }
                                )
                                NavigationBarItem(
                                    selected = currentRoute == Routes.SETTINGS,
                                    onClick = { navController.navigate(Routes.SETTINGS) { launchSingleTop = true } },
                                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                                    label = { Text("Settings") }
                                )
                            }
                        }
                    }
                ) { padding ->
                    Box(Modifier.fillMaxSize().padding(padding)) {
                        AuraNavGraph(navController = navController)
                    }
                }
            }
        }
    }
}
