package com.yubera.app.data.settings

import kotlinx.coroutines.flow.Flow

interface UserSettingsRepository {
    val settings: Flow<UserSettings>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setNotificationsEnabled(enabled: Boolean)
    suspend fun setNewEpisodeNotificationsEnabled(enabled: Boolean)
    suspend fun setAnimeInfoNotificationsEnabled(enabled: Boolean)
}
