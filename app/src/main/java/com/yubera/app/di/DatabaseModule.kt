package com.yubera.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.yubera.app.core.database.YuberaDatabase
import com.yubera.app.core.database.dao.AnimeEpisodeNotificationDao
import com.yubera.app.core.database.dao.AnimeLibraryDao
import com.yubera.app.core.database.dao.AnimeProgressDao
import com.yubera.app.core.database.dao.AnimeSubscriptionDao
import com.yubera.app.core.database.dao.CatalogDao
import com.yubera.app.core.database.dao.NewsDao
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
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .build()

    @Provides fun provideAnimeEpisodeNotificationDao(database: YuberaDatabase): AnimeEpisodeNotificationDao = database.animeEpisodeNotificationDao()
    @Provides fun provideAnimeLibraryDao(database: YuberaDatabase): AnimeLibraryDao = database.animeLibraryDao()
    @Provides fun provideAnimeProgressDao(database: YuberaDatabase): AnimeProgressDao = database.animeProgressDao()
    @Provides fun provideAnimeSubscriptionDao(database: YuberaDatabase): AnimeSubscriptionDao = database.animeSubscriptionDao()
    @Provides fun provideCatalogDao(database: YuberaDatabase): CatalogDao = database.catalogDao()
    @Provides fun provideNewsDao(database: YuberaDatabase): NewsDao = database.newsDao()

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("CREATE TABLE IF NOT EXISTS catalog_anime (animeId TEXT NOT NULL, title TEXT NOT NULL, originalTitle TEXT NOT NULL, year INTEGER NOT NULL, status TEXT NOT NULL, genresJson TEXT NOT NULL, payloadJson TEXT NOT NULL, position INTEGER NOT NULL, PRIMARY KEY(animeId))")
            database.execSQL("CREATE TABLE IF NOT EXISTS catalog_metadata (id INTEGER NOT NULL, revision INTEGER NOT NULL, updatedAt TEXT NOT NULL, PRIMARY KEY(id))")
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("CREATE TABLE IF NOT EXISTS news_articles (id INTEGER NOT NULL, title TEXT NOT NULL, summary TEXT NOT NULL, createdAt TEXT NOT NULL, commentsCount INTEGER NOT NULL, authorName TEXT NOT NULL, linkedTitle TEXT, category TEXT NOT NULL, sourceUrl TEXT NOT NULL, PRIMARY KEY(id))")
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE anime_subscriptions ADD COLUMN lastKnownEpisodesAired INTEGER")
            database.execSQL("CREATE TABLE IF NOT EXISTS anime_episode_notifications (animeId TEXT NOT NULL, episodeNumber INTEGER NOT NULL, createdAtEpochMillis INTEGER NOT NULL, deliveredAtEpochMillis INTEGER, PRIMARY KEY(animeId, episodeNumber))")
        }
    }

    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE anime_library ADD COLUMN favoriteUpdatedAtEpochMillis INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE anime_library ADD COLUMN watchLaterUpdatedAtEpochMillis INTEGER NOT NULL DEFAULT 0")
            database.execSQL("UPDATE anime_library SET favoriteUpdatedAtEpochMillis = updatedAtEpochMillis, watchLaterUpdatedAtEpochMillis = updatedAtEpochMillis")
            database.execSQL("ALTER TABLE anime_subscriptions ADD COLUMN isSubscribed INTEGER NOT NULL DEFAULT 1")
        }
    }
}
