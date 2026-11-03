package com.yubera.app.core.catalog

import kotlinx.coroutines.flow.Flow

interface CatalogRepository {
    fun observeCatalog(): Flow<List<AnimeCatalogItem>>
    suspend fun refresh()
}
