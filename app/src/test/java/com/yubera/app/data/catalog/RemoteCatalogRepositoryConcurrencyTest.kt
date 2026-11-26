package com.yubera.app.data.catalog

import com.yubera.app.core.database.dao.CatalogDao
import com.yubera.app.core.database.entity.CatalogAnimeEntity
import com.yubera.app.core.database.entity.CatalogMetadataEntity
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class RemoteCatalogRepositoryConcurrencyTest {
    @Test
    fun concurrentRefreshesJoinOneInFlightCatalogDownload() = runTest {
        val dao = FakeCatalogDao()
        val api = CountingCatalogApi()
        val repository = RemoteCatalogRepository(api, dao, Json { ignoreUnknownKeys = true })

        val first = async { repository.refresh() }
        first.join()
        val second = async { repository.refresh() }
        second.join()

        assertEquals(1, api.firstPageCalls)
        assertEquals(48, dao.rows.value.size)
        assertEquals(12L, dao.metadata?.revision)
    }

    private class CountingCatalogApi : CatalogApi {
        var firstPageCalls = 0
        override suspend fun getCatalogPage(page: Int, pageSize: Int): CatalogPageDto {
            if (page == 1) {
                firstPageCalls++
                kotlinx.coroutines.delay(40)
            }
            return CatalogPageDto(
                revision = 12L,
                page = page,
                pageSize = RemoteCatalogRepository.PAGE_SIZE,
                total = 48,
                totalPages = 1,
                items = (0 until 48).map { index ->
                    AnimeCatalogDto(
                        id = "anime-$index",
                        title = "Anime $index",
                        original = "Original $index"
                    )
                }
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
