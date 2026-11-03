package com.yubera.app.data.settings

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val notificationsEnabled: Boolean = false,
    val newEpisodeNotificationsEnabled: Boolean = false,
    val animeInfoNotificationsEnabled: Boolean = false
)
