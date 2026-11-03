package com.yubera.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.yubera.app.core.ui.theme.YuberaBackground
import com.yubera.app.core.ui.theme.YuberaMuted
import com.yubera.app.core.ui.theme.YuberaPrimary
import com.yubera.app.core.ui.theme.YuberaSurface
import com.yubera.app.features.catalog.CatalogScreen
import com.yubera.app.features.home.HomeScreen
import com.yubera.app.features.my.MyScreen
import com.yubera.app.features.news.NewsScreen
import com.yubera.app.features.profile.ProfileScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val entry by navController.currentBackStackEntryAsState()
    val currentRoute = entry?.destination?.route

    Scaffold(
        containerColor = YuberaBackground,
        bottomBar = {
            NavigationBar(containerColor = YuberaBackground) {
                AppDestination.all.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            androidx.compose.material3.Icon(
                                destination.icon,
                                contentDescription = destination.label
                            )
                        },
                        label = {
                            androidx.compose.material3.Text(destination.label)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = YuberaPrimary,
                            selectedTextColor = YuberaPrimary,
                            unselectedIconColor = YuberaMuted,
                            unselectedTextColor = YuberaMuted,
                            indicatorColor = YuberaSurface
                        )
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(AppDestination.Home.route) { HomeScreen() }
            composable(AppDestination.Catalog.route) { CatalogScreen() }
            composable(AppDestination.My.route) { MyScreen() }
            composable(AppDestination.News.route) { NewsScreen() }
            composable(AppDestination.Profile.route) { ProfileScreen() }
        }
    }
}
