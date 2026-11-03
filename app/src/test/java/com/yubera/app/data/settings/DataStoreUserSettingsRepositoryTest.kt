package com.yubera.app.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DataStoreUserSettingsRepositoryTest {

    private lateinit var scope: CoroutineScope
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: DataStoreUserSettingsRepository
    private lateinit var directory: File

    @Before
    fun setUp() {
        directory = Files.createTempDirectory("yubera-settings").toFile()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        dataStore = PreferenceDataStoreFactory.create(scope = scope) {
            File(directory, "settings.preferences_pb")
        }
        repository = DataStoreUserSettingsRepository(dataStore)
    }

    @After
    fun tearDown() {
        scope.cancel()
        directory.deleteRecursively()
    }

    @Test
    fun defaultsToSystemThemeAndOptInNotifications() = runBlocking {
        val settings = repository.settings.first()

        assertEquals(ThemeMode.SYSTEM, settings.themeMode)
        assertFalse(settings.notificationsEnabled)
        assertFalse(settings.newEpisodeNotificationsEnabled)
        assertFalse(settings.animeInfoNotificationsEnabled)
    }

    @Test
    fun storesThemeAndNotificationPreferencesIndependently() = runBlocking {
        repository.setThemeMode(ThemeMode.DARK)
        repository.setNewEpisodeNotificationsEnabled(true)
        repository.setNotificationsEnabled(false)

        val settings = repository.settings.first()

        assertEquals(ThemeMode.DARK, settings.themeMode)
        assertFalse(settings.notificationsEnabled)
        assertTrue(settings.newEpisodeNotificationsEnabled)
        assertFalse(settings.animeInfoNotificationsEnabled)
    }
}
