package com.yubera.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.yubera.app.core.database.dao.AnimeLibraryDao
import com.yubera.app.core.database.dao.AnimeProgressDao
import com.yubera.app.core.database.dao.AnimeSubscriptionDao
import com.yubera.app.core.database.dao.CatalogDao
import com.yubera.app.core.database.dao.NewsDao
import com.yubera.app.core.database.entity.AnimeLibraryEntity
import com.yubera.app.core.database.entity.AnimeProgressEntity
import com.yubera.app.core.database.entity.AnimeSubscriptionEntity
import com.yubera.app.core.database.entity.CatalogAnimeEntity
import com.yubera.app.core.database.entity.CatalogMetadataEntity
import com.yubera.app.core.database.entity.NewsArticleEntity

@Database(
    entities = [
        AnimeLibraryEntity::class,
        AnimeProgressEntity::class,
        AnimeSubscriptionEntity::class,
        CatalogAnimeEntity::class,
        CatalogMetadataEntity::class,
        NewsArticleEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class YuberaDatabase : RoomDatabase() {
    abstract fun animeLibraryDao(): AnimeLibraryDao
    abstract fun animeProgressDao(): AnimeProgressDao
    abstract fun animeSubscriptionDao(): AnimeSubscriptionDao
    abstract fun catalogDao(): CatalogDao
    abstract fun newsDao(): NewsDao
}
