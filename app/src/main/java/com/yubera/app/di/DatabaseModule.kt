package com.yubera.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.yubera.app.core.database.YuberaDatabase
import com.yubera.app.core.database.dao.AnimeLibraryDao
import com.yubera.app.core.database.dao.AnimeProgressDao
import com.yubera.app.core.database.dao.AnimeSubscriptionDao
import com.yubera.app.core.database.dao.CatalogDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): YuberaDatabase =
        Room.databaseBuilder(context, YuberaDatabase::class.java, "yubera.db")
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides fun provideAnimeLibraryDao(database: YuberaDatabase): AnimeLibraryDao = database.animeLibraryDao()
    @Provides fun provideAnimeProgressDao(database: YuberaDatabase): AnimeProgressDao = database.animeProgressDao()
    @Provides fun provideAnimeSubscriptionDao(database: YuberaDatabase): AnimeSubscriptionDao = database.animeSubscriptionDao()
    @Provides fun provideCatalogDao(database: YuberaDatabase): CatalogDao = database.catalogDao()

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("CREATE TABLE IF NOT EXISTS catalog_anime (animeId TEXT NOT NULL, title TEXT NOT NULL, originalTitle TEXT NOT NULL, year INTEGER NOT NULL, status TEXT NOT NULL, genresJson TEXT NOT NULL, payloadJson TEXT NOT NULL, position INTEGER NOT NULL, PRIMARY KEY(animeId))")
            database.execSQL("CREATE TABLE IF NOT EXISTS catalog_metadata (id INTEGER NOT NULL, revision INTEGER NOT NULL, updatedAt TEXT NOT NULL, PRIMARY KEY(id))")
        }
    }
}
