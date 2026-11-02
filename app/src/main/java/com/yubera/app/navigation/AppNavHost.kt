package com.yubera.app.navigation
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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
    Scaffold(bottomBar = {
        NavigationBar {
            AppDestination.all.forEach { destination ->
                NavigationBarItem(
                    selected = currentRoute == destination.route,
                    onClick = {
                        navController.navigate(destination.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(destination.icon, contentDescription = destination.label) },
                    label = { Text(destination.label) }
                )
            }
        }
    }) { padding ->
        NavHost(navController, startDestination = AppDestination.Home.route, modifier = Modifier.padding(padding)) {
            composable(AppDestination.Home.route) { HomeScreen() }
            composable(AppDestination.Catalog.route) { CatalogScreen() }
            composable(AppDestination.My.route) { MyScreen() }
            composable(AppDestination.News.route) { NewsScreen() }
            composable(AppDestination.Profile.route) { ProfileScreen() }
        }
    }
}
