package com.yubera.app.core.startup

import androidx.room.RoomDatabase
import com.yubera.app.core.database.YuberaDatabase
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface StartupLocalStorageInitializer {
    suspend fun initialize()
}

@Singleton
class RoomStartupLocalStorageInitializer @Inject constructor(
    private val database: YuberaDatabase
) : StartupLocalStorageInitializer {
    override suspend fun initialize() {
        withContext(Dispatchers.IO) {
            // Force Room to open (and validate/migrate) before the first app destination is shown.
            database.openHelper.writableDatabase
        }
    }
}
