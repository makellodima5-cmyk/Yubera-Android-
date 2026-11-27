package com.yubera.app.navigation

import android.net.Uri
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import com.yubera.app.PendingAnimeRoute
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.navArgument
import com.yubera.app.core.ui.theme.YuberaBackground
import com.yubera.app.core.ui.theme.YuberaMuted
import com.yubera.app.core.ui.theme.YuberaPrimary
import com.yubera.app.core.ui.theme.YuberaSurface
import com.yubera.app.features.anime.AnimeDetailScreen
import com.yubera.app.features.catalog.CatalogScreen
import com.yubera.app.features.home.HomeScreen
import com.yubera.app.features.home.HomeCategoryScreen
import com.yubera.app.features.home.HomeViewModel
import com.yubera.app.features.home.HomeSimpleDestination
import com.yubera.app.features.my.MyScreen
import com.yubera.app.features.news.NewsScreen
import com.yubera.app.features.news.NewsDetailScreen
import com.yubera.app.features.player.PlayerScreen
import com.yubera.app.features.profile.ProfileScreen

@Composable
fun AppNavHost(
    pendingAnimeRoute: PendingAnimeRoute? = null,
    onAnimeRouteHandled: () -> Unit = {}
) {
    val navController = rememberNavController()
    LaunchedEffect(pendingAnimeRoute) {
        val route = pendingAnimeRoute ?: return@LaunchedEffect
        if (route.episodeNumber != null) {
            navController.navigate("player/${Uri.encode(route.animeId)}?episode=${route.episodeNumber}") { launchSingleTop = true }
        } else {
            navController.navigate("anime/${Uri.encode(route.animeId)}") { launchSingleTop = true }
        }
        onAnimeRouteHandled()
    }
    val entry by navController.currentBackStackEntryAsState()
    val currentRoute = entry?.destination?.route
    val isDetailRoute = currentRoute?.startsWith("anime/") == true ||
        currentRoute?.startsWith("player/") == true || currentRoute?.startsWith("news/") == true

    Scaffold(
        containerColor = YuberaBackground,
        bottomBar = {
            if (!isDetailRoute) {
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
                            label = { androidx.compose.material3.Text(destination.label) },
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
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(AppDestination.Home.route) {
                HomeScreen(
                    onAnimeClick = { id -> navController.navigate("anime/${Uri.encode(id)}") },
                    onContinueClick = { id, episode ->
                        val route = "player/${Uri.encode(id)}" + (episode?.let { "?episode=$it" } ?: "")
                        navController.navigate(route)
                    },
                    onCollectionsClick = { navController.navigate("collections") },
                    onNotificationsClick = { navController.navigate("notifications") },
                    onViewAll = { category -> navController.navigate("home-list/${Uri.encode(category)}") }
                )
            }
            composable(
                route = "home-list/{category}",
                arguments = listOf(navArgument("category") { type = NavType.StringType })
            ) { backStackEntry ->
                val category = backStackEntry.arguments?.getString("category").orEmpty()
                val homeEntry = remember(navController) { navController.getBackStackEntry(AppDestination.Home.route) }
                val sharedHomeViewModel: HomeViewModel = hiltViewModel(homeEntry)
                HomeCategoryScreen(
                    category = category,
                    onAnimeClick = { id -> navController.navigate("anime/${Uri.encode(id)}") },
                    onContinueClick = { id, episode ->
                        val route = "player/${Uri.encode(id)}" + (episode?.let { "?episode=$it" } ?: "")
                        navController.navigate(route)
                    },
                    onBack = { navController.popBackStack() },
                    viewModel = sharedHomeViewModel
                )
            }
            composable("collections") {
                HomeSimpleDestination("Подборки", "Тематические коллекции будут оформлены отдельным техническим заданием.", { navController.popBackStack() })
            }
            composable("notifications") {
                HomeSimpleDestination("Уведомления", "Экран уведомлений будет оформлен отдельным техническим заданием. Сейчас доступны системные уведомления о выходе серий.", { navController.popBackStack() })
            }
            composable(AppDestination.Catalog.route) {
                CatalogScreen(onAnimeClick = { anime -> navController.navigate("anime/${Uri.encode(anime.id)}") })
            }
            composable(AppDestination.My.route) {
                MyScreen(onAnimeClick = { id -> navController.navigate("anime/${Uri.encode(id)}") })
            }
            composable(AppDestination.News.route) { NewsScreen(onArticleClick = { id -> navController.navigate("news/$id") }) }
            composable(AppDestination.Profile.route) { ProfileScreen() }
            composable(
                route = "anime/{animeId}",
                arguments = listOf(navArgument("animeId") { type = NavType.StringType })
            ) { backStackEntry ->
                AnimeDetailScreen(
                    onBack = { navController.popBackStack() },
                    onWatchClick = {
                        val id = backStackEntry.arguments?.getString("animeId").orEmpty()
                        navController.navigate("player/${Uri.encode(id)}")
                    }
                )
            }
            composable(
                route = "news/{articleId}",
                arguments = listOf(navArgument("articleId") { type = NavType.StringType })
            ) {
                NewsDetailScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = "player/{animeId}?episode={episode}",
                arguments = listOf(
                    navArgument("animeId") { type = NavType.StringType },
                    navArgument("episode") { type = NavType.IntType; defaultValue = -1 }
                )
            ) { backStackEntry ->
                PlayerScreen(
                    onBack = { navController.popBackStack() },
                    requestedEpisode = backStackEntry.arguments?.getInt("episode")?.takeIf { it > 0 }
                )
            }
        }
    }
}
