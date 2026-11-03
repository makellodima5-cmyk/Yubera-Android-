package com.yubera.app.di

import android.content.Context
import androidx.room.Room
import com.yubera.app.core.database.YuberaDatabase
import com.yubera.app.core.database.dao.AnimeLibraryDao
import com.yubera.app.core.database.dao.AnimeProgressDao
import com.yubera.app.core.database.dao.AnimeSubscriptionDao
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
        Room.databaseBuilder(context, YuberaDatabase::class.java, "yubera.db").build()

    @Provides fun provideAnimeLibraryDao(database: YuberaDatabase): AnimeLibraryDao = database.animeLibraryDao()
    @Provides fun provideAnimeProgressDao(database: YuberaDatabase): AnimeProgressDao = database.animeProgressDao()
    @Provides fun provideAnimeSubscriptionDao(database: YuberaDatabase): AnimeSubscriptionDao = database.animeSubscriptionDao()
}
