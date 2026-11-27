package com.yubera.app.data.catalog

import com.yubera.app.core.database.dao.CatalogDao
import com.yubera.app.core.database.entity.CatalogAnimeEntity
import com.yubera.app.core.database.entity.CatalogMetadataEntity
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class RemoteCatalogRepositoryConcurrencyTest {
    @Test
    fun serializedRefreshChecksRevisionBeforeDownloadingRemainingPages() = runTest {
        val dao = FakeCatalogDao()
        val api = CountingCatalogApi()
        val repository = RemoteCatalogRepository(api, dao, Json { ignoreUnknownKeys = true })

        val first = async { repository.refresh() }
        val second = async { repository.refresh() }
        first.await()
        second.await()

        // Each refresh needs the first page to learn the current revision. The
        // second refresh must then stop instead of downloading page two again.
        assertEquals(2, api.firstPageCalls)
        assertEquals(1, api.secondPageCalls)
        assertEquals(80, dao.rows.value.size)
        assertEquals(12L, dao.metadata?.revision)
    }

    private class CountingCatalogApi : CatalogApi {
        var firstPageCalls = 0
        var secondPageCalls = 0
        override suspend fun getCatalogPage(page: Int, pageSize: Int): CatalogPageDto {
            kotlinx.coroutines.delay(30)
            return when (page) {
                1 -> {
                    firstPageCalls++
                    CatalogPageDto(
                        revision = 12L,
                        page = 1,
                        pageSize = RemoteCatalogRepository.PAGE_SIZE,
                        total = 80,
                        totalPages = 2,
                        items = items(from = 0, count = 48)
                    )
                }
                2 -> {
                    secondPageCalls++
                    CatalogPageDto(
                        revision = 12L,
                        page = 2,
                        pageSize = RemoteCatalogRepository.PAGE_SIZE,
                        total = 80,
                        totalPages = 2,
                        items = items(from = 48, count = 32)
                    )
                }
                else -> error("Unexpected catalog page: $page")
            }
        }

        private fun items(from: Int, count: Int) = (from until from + count).map { index ->
            AnimeCatalogDto(
                id = "anime-$index",
                title = "Anime $index",
                original = "Original $index"
            )
        }
    }

    private class FakeCatalogDao : CatalogDao {
        val rows = MutableStateFlow<List<CatalogAnimeEntity>>(emptyList())
        var metadata: CatalogMetadataEntity? = null
        override fun observeAll(): Flow<List<CatalogAnimeEntity>> = rows
        override fun observeById(animeId: String): Flow<CatalogAnimeEntity?> =
            rows.map { items -> items.firstOrNull { it.animeId == animeId } }
        override suspend fun getAll(): List<CatalogAnimeEntity> = rows.value
        override suspend fun getById(animeId: String): CatalogAnimeEntity? = rows.value.firstOrNull { it.animeId == animeId }
        override suspend fun upsert(entity: CatalogAnimeEntity) { rows.value = rows.value.filterNot { it.animeId == entity.animeId } + entity }
        override suspend fun getMetadata(): CatalogMetadataEntity? = metadata
        override suspend fun clearCatalog() { rows.value = emptyList() }
        override suspend fun insertAll(items: List<CatalogAnimeEntity>) { rows.value = rows.value + items }
        override suspend fun saveMetadata(metadata: CatalogMetadataEntity) { this.metadata = metadata }
        override suspend fun replaceSnapshot(items: List<CatalogAnimeEntity>, metadata: CatalogMetadataEntity) {
            rows.value = items
            this.metadata = metadata
        }
    }
}
