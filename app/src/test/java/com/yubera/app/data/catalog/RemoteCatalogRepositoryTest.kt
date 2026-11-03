package com.yubera.app.data.catalog

import com.yubera.app.core.database.dao.CatalogDao
import com.yubera.app.core.database.entity.CatalogAnimeEntity
import com.yubera.app.core.database.entity.CatalogMetadataEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteCatalogRepositoryTest {
    @Test
    fun downloadsAllPagesAndCommitsOneCompleteRevision() = runTest {
        val dao = FakeCatalogDao()
        val api = FakeCatalogApi(
            listOf(
                page(1, 7, items("old", 48)),
                page(2, 7, items("old", 1, 48))
            )
        )

        repository(api, dao).refresh()

        assertEquals(49, dao.rows.value.size)
        assertEquals(7L, dao.metadata?.revision)
        assertEquals(listOf(1, 2), api.requestedPages)
    }

    @Test
    fun retriesWhenRevisionChangesBetweenPages() = runTest {
        val dao = FakeCatalogDao()
        val api = FakeCatalogApi(
            listOf(
                page(1, 7, items("old", 48)),
                page(2, 8, items("stale", 1, 48)),
                page(1, 8, items("new", 48)),
                page(2, 8, items("new", 1, 48))
            )
        )

        repository(api, dao).refresh()

        assertEquals(49, dao.rows.value.size)
        assertTrue(dao.rows.value.first().animeId.startsWith("new"))
        assertEquals(8L, dao.metadata?.revision)
    }

    @Test
    fun failedRefreshPreservesExistingSnapshot() = runTest {
        val dao = FakeCatalogDao()
        dao.replaceSnapshot(listOf(entity("cached")), CatalogMetadataEntity(revision = 3, updatedAt = "old"))
        var failed = false
        try {
            repository(FailingCatalogApi(), dao).refresh()
        } catch (_: Exception) {
            failed = true
        }

        assertTrue(failed)
        assertEquals(listOf("cached"), dao.rows.value.map { it.animeId })
        assertEquals(3L, dao.metadata?.revision)
    }

    private fun repository(api: CatalogApi, dao: FakeCatalogDao) =
        RemoteCatalogRepository(api, dao, Json { ignoreUnknownKeys = true })

    private fun page(number: Int, revision: Long, items: List<AnimeCatalogDto>) = CatalogPageDto(
        revision = revision,
        page = number,
        pageSize = 48,
        total = 49,
        totalPages = 2,
        items = items
    )

    private fun items(prefix: String, count: Int, start: Int = 0) = (start until start + count).map { index ->
        AnimeCatalogDto(
            id = "$prefix-$index",
            title = "Title $prefix $index",
            original = "Original $index",
            year = 2026,
            episodes = 12,
            genres = listOf("Фэнтези")
        )
    }

    private fun entity(id: String) = CatalogAnimeEntity(
        animeId = id, title = id, originalTitle = id, year = 2020,
        status = "Вышел", genresJson = "[]", payloadJson = "{}", position = 0
    )

    private class FakeCatalogApi(private val responses: List<CatalogPageDto>) : CatalogApi {
        val requestedPages = mutableListOf<Int>()
        private var index = 0
        override suspend fun getCatalogPage(page: Int, pageSize: Int): CatalogPageDto {
            requestedPages += page
            return responses[index++]
        }
    }

    private class FailingCatalogApi : CatalogApi {
        override suspend fun getCatalogPage(page: Int, pageSize: Int): CatalogPageDto = error("offline")
    }

    private class FakeCatalogDao : CatalogDao {
        val rows = MutableStateFlow<List<CatalogAnimeEntity>>(emptyList())
        var metadata: CatalogMetadataEntity? = null

        override fun observeAll(): Flow<List<CatalogAnimeEntity>> = rows
        override fun observeById(animeId: String): Flow<CatalogAnimeEntity?> = rows.map { items -> items.firstOrNull { it.animeId == animeId } }
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
