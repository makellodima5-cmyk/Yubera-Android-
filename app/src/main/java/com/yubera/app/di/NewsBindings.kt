package com.yubera.app.di

import com.yubera.app.core.news.NewsRepository
import com.yubera.app.data.news.RemoteNewsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NewsBindings {
    @Binds
    @Singleton
    abstract fun bindNewsRepository(implementation: RemoteNewsRepository): NewsRepository
}
