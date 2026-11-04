package com.yubera.app.core.database
import androidx.room.Database
import androidx.room.RoomDatabase
import com.yubera.app.core.database.dao.*
import com.yubera.app.core.database.entity.*
@Database(entities = [AnimeLibraryEntity::class, AnimeProgressEntity::class, AnimeSubscriptionEntity::class, AnimeEpisodeNotificationEntity::class, CatalogAnimeEntity::class, CatalogMetadataEntity::class, NewsArticleEntity::class, NewsBookmarkEntity::class], version = 7, exportSchema = true)
abstract class YuberaDatabase : RoomDatabase() {
 abstract fun animeEpisodeNotificationDao(): AnimeEpisodeNotificationDao
 abstract fun animeLibraryDao(): AnimeLibraryDao
 abstract fun animeProgressDao(): AnimeProgressDao
 abstract fun animeSubscriptionDao(): AnimeSubscriptionDao
 abstract fun catalogDao(): CatalogDao
 abstract fun newsDao(): NewsDao
 abstract fun newsBookmarkDao(): NewsBookmarkDao
}
