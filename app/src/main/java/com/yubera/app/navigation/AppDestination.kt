package com.yubera.app.navigation
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class AppDestination(val route: String, val label: String, val icon: ImageVector) {
    data object Home : AppDestination("home", "Главная", Icons.Filled.Home)
    data object Catalog : AppDestination("catalog", "Каталог", Icons.AutoMirrored.Filled.List)
    data object My : AppDestination("my", "Моё", Icons.Filled.Bookmark)
    data object News : AppDestination("news", "Новости", Icons.Filled.Newspaper)
    data object Profile : AppDestination("profile", "Профиль", Icons.Filled.Person)
    companion object {
        val all = listOf(Home, Catalog, My, News, Profile)
    }
}
