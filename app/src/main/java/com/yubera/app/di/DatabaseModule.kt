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
import com.yubera.app.core.database.dao.NewsBookmarkDao
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
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
            .build()

    @Provides fun provideAnimeEpisodeNotificationDao(database: YuberaDatabase): AnimeEpisodeNotificationDao = database.animeEpisodeNotificationDao()
    @Provides fun provideAnimeLibraryDao(database: YuberaDatabase): AnimeLibraryDao = database.animeLibraryDao()
    @Provides fun provideAnimeProgressDao(database: YuberaDatabase): AnimeProgressDao = database.animeProgressDao()
    @Provides fun provideAnimeSubscriptionDao(database: YuberaDatabase): AnimeSubscriptionDao = database.animeSubscriptionDao()
    @Provides fun provideCatalogDao(database: YuberaDatabase): CatalogDao = database.catalogDao()
    @Provides fun provideNewsDao(database: YuberaDatabase): NewsDao = database.newsDao()
    @Provides fun provideNewsBookmarkDao(database: YuberaDatabase): NewsBookmarkDao = database.newsBookmarkDao()

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
            database.execSQL("UPDATE anime_library SET favoriteUpdatedAtEpochMillis = CASE WHEN isFavorite = 1 THEN updatedAtEpochMillis ELSE 0 END, watchLaterUpdatedAtEpochMillis = CASE WHEN isInWatchLater = 1 THEN updatedAtEpochMillis ELSE 0 END")
            database.execSQL("ALTER TABLE anime_subscriptions ADD COLUMN isSubscribed INTEGER NOT NULL DEFAULT 1")
        }
    }

    private val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("CREATE TABLE IF NOT EXISTS news_bookmarks (articleId INTEGER NOT NULL, savedAtEpochMillis INTEGER NOT NULL, PRIMARY KEY(articleId))")
        }
    }

    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE anime_library RENAME TO anime_library_v5")
            database.execSQL("CREATE TABLE anime_library (animeId TEXT NOT NULL, isFavorite INTEGER NOT NULL, isInWatchLater INTEGER NOT NULL, updatedAtEpochMillis INTEGER NOT NULL, favoriteUpdatedAtEpochMillis INTEGER NOT NULL DEFAULT 0, watchLaterUpdatedAtEpochMillis INTEGER NOT NULL DEFAULT 0, accountId TEXT NOT NULL DEFAULT 'legacy-unassigned', PRIMARY KEY(accountId, animeId))")
            database.execSQL("INSERT INTO anime_library (animeId, isFavorite, isInWatchLater, updatedAtEpochMillis, favoriteUpdatedAtEpochMillis, watchLaterUpdatedAtEpochMillis, accountId) SELECT animeId, isFavorite, isInWatchLater, updatedAtEpochMillis, favoriteUpdatedAtEpochMillis, watchLaterUpdatedAtEpochMillis, 'legacy-unassigned' FROM anime_library_v5")
            database.execSQL("DROP TABLE anime_library_v5")

            database.execSQL("ALTER TABLE anime_progress RENAME TO anime_progress_v5")
            database.execSQL("CREATE TABLE anime_progress (animeId TEXT NOT NULL, seasonNumber INTEGER, episodeNumber INTEGER, positionMillis INTEGER NOT NULL, durationMillis INTEGER NOT NULL, updatedAtEpochMillis INTEGER NOT NULL, accountId TEXT NOT NULL DEFAULT 'legacy-unassigned', PRIMARY KEY(accountId, animeId))")
            database.execSQL("INSERT INTO anime_progress (animeId, seasonNumber, episodeNumber, positionMillis, durationMillis, updatedAtEpochMillis, accountId) SELECT animeId, seasonNumber, episodeNumber, positionMillis, durationMillis, updatedAtEpochMillis, 'legacy-unassigned' FROM anime_progress_v5")
            database.execSQL("DROP TABLE anime_progress_v5")

            database.execSQL("ALTER TABLE anime_subscriptions RENAME TO anime_subscriptions_v5")
            database.execSQL("CREATE TABLE anime_subscriptions (animeId TEXT NOT NULL, notificationsEnabled INTEGER NOT NULL, updatedAtEpochMillis INTEGER NOT NULL, lastKnownEpisodesAired INTEGER, isSubscribed INTEGER NOT NULL DEFAULT 1, accountId TEXT NOT NULL DEFAULT 'legacy-unassigned', PRIMARY KEY(accountId, animeId))")
            database.execSQL("INSERT INTO anime_subscriptions (animeId, notificationsEnabled, updatedAtEpochMillis, lastKnownEpisodesAired, isSubscribed, accountId) SELECT animeId, notificationsEnabled, updatedAtEpochMillis, lastKnownEpisodesAired, isSubscribed, 'legacy-unassigned' FROM anime_subscriptions_v5")
            database.execSQL("DROP TABLE anime_subscriptions_v5")

            database.execSQL("ALTER TABLE anime_episode_notifications RENAME TO anime_episode_notifications_v5")
            database.execSQL("CREATE TABLE anime_episode_notifications (animeId TEXT NOT NULL, episodeNumber INTEGER NOT NULL, createdAtEpochMillis INTEGER NOT NULL, deliveredAtEpochMillis INTEGER, accountId TEXT NOT NULL DEFAULT 'legacy-unassigned', PRIMARY KEY(accountId, animeId, episodeNumber))")
            database.execSQL("INSERT INTO anime_episode_notifications (animeId, episodeNumber, createdAtEpochMillis, deliveredAtEpochMillis, accountId) SELECT animeId, episodeNumber, createdAtEpochMillis, deliveredAtEpochMillis, 'legacy-unassigned' FROM anime_episode_notifications_v5")
            database.execSQL("DROP TABLE anime_episode_notifications_v5")
        }
    }
}
