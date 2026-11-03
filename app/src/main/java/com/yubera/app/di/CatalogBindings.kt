package com.yubera.app.di

import com.yubera.app.core.catalog.CatalogRepository
import com.yubera.app.data.catalog.RemoteCatalogRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CatalogBindings {
    @Binds
    @Singleton
    abstract fun bindCatalogRepository(implementation: RemoteCatalogRepository): CatalogRepository
}
