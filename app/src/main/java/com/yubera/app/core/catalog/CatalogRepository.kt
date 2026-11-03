package com.yubera.app.core.catalog

import kotlinx.coroutines.flow.Flow

interface CatalogRepository {
    fun observeCatalog(): Flow<List<AnimeCatalogItem>>
    fun observeAnime(animeId: String): Flow<AnimeCatalogItem?>
    suspend fun refresh()
}
