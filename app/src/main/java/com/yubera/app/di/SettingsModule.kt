package com.yubera.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.yubera.app.data.settings.DataStoreUserSettingsRepository
import com.yubera.app.data.settings.UserSettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsBindings {
    @Binds
    @Singleton
    abstract fun bindUserSettingsRepository(implementation: DataStoreUserSettingsRepository): UserSettingsRepository
}

@Module
@InstallIn(SingletonComponent::class)
object SettingsProviders {
    @Provides
    @Singleton
    fun providePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope = kotlinx.coroutines.CoroutineScope(SupervisorJob() + Dispatchers.IO),
            produceFile = { context.preferencesDataStoreFile("user_settings") }
        )
}
