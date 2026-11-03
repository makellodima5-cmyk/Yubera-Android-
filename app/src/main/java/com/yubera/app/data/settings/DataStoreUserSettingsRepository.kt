package com.yubera.app.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DataStoreUserSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : UserSettingsRepository {

    override val settings: Flow<UserSettings> = dataStore.data.map { preferences ->
        UserSettings(
            themeMode = preferences[Keys.themeMode]
                ?.let { value -> ThemeMode.entries.firstOrNull { it.name == value } }
                ?: ThemeMode.SYSTEM,
            notificationsEnabled = preferences[Keys.notificationsEnabled] ?: false,
            newEpisodeNotificationsEnabled = preferences[Keys.newEpisodeNotificationsEnabled] ?: false,
            animeInfoNotificationsEnabled = preferences[Keys.animeInfoNotificationsEnabled] ?: false
        )
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.themeMode] = mode.name }
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.notificationsEnabled] = enabled }
    }

    override suspend fun setNewEpisodeNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.newEpisodeNotificationsEnabled] = enabled }
    }

    override suspend fun setAnimeInfoNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.animeInfoNotificationsEnabled] = enabled }
    }

    private object Keys {
        val themeMode = stringPreferencesKey("theme_mode")
        val notificationsEnabled = booleanPreferencesKey("notifications_enabled")
        val newEpisodeNotificationsEnabled = booleanPreferencesKey("new_episode_notifications_enabled")
        val animeInfoNotificationsEnabled = booleanPreferencesKey("anime_info_notifications_enabled")
    }
}
