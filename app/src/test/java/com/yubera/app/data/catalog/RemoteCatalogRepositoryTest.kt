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
    fun revisionProbeSkipsDownloadingRemainingPagesWhenUnchanged() = runTest {
        val dao = FakeCatalogDao()
        dao.replaceSnapshot(
            listOf(entity("cached")),
            CatalogMetadataEntity(revision = 11, updatedAt = "known", checkedAtEpochMillis = 0L)
        )
        val api = FakeCatalogApi(listOf(page(1, 11, items("remote", 48))))

        repository(api, dao).refreshIfStale(minIntervalMillis = 0L)

        assertEquals(listOf(1), api.requestedPages)
        assertEquals(listOf("cached"), dao.rows.value.map { it.animeId })
        assertTrue((dao.metadata?.checkedAtEpochMillis ?: 0L) > 0L)
    }

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

    @Test
    fun rejectsPageCountInconsistentWithTotalWithoutReplacingCache() = runTest {
        val dao = FakeCatalogDao()
        dao.replaceSnapshot(listOf(entity("cached")), CatalogMetadataEntity(revision = 3, updatedAt = "old"))
        val invalid = page(1, 8, items("invalid", 48)).copy(totalPages = 1)

        val result = runCatching { repository(FakeCatalogApi(listOf(invalid)), dao).refresh() }

        assertTrue(result.isFailure)
        assertEquals(listOf("cached"), dao.rows.value.map { it.animeId })
        assertEquals(3L, dao.metadata?.revision)
    }

    @Test
    fun rejectsUnexpectedPageSize() = runTest {
        val dao = FakeCatalogDao()
        val invalid = page(1, 8, items("invalid", 1)).copy(total = 1, totalPages = 1, pageSize = 24)

        val result = runCatching { repository(FakeCatalogApi(listOf(invalid)), dao).refresh() }

        assertTrue(result.isFailure)
        assertTrue(dao.rows.value.isEmpty())
    }

    @Test
    fun emptyCatalogIsAValidSinglePageSnapshot() = runTest {
        val dao = FakeCatalogDao()
        val empty = page(1, 9, emptyList()).copy(total = 0, totalPages = 1)

        repository(FakeCatalogApi(listOf(empty)), dao).refresh()

        assertTrue(dao.rows.value.isEmpty())
        assertEquals(9L, dao.metadata?.revision)
    }


    @Test
    fun rejectsIncompleteLaterPageWithoutReplacingCache() = runTest {
        val dao = FakeCatalogDao()
        dao.replaceSnapshot(listOf(entity("cached")), CatalogMetadataEntity(revision = 3, updatedAt = "old"))
        val first = page(1, 8, items("new", 48))
        val incomplete = page(2, 8, items("new", 0, 48))

        val result = runCatching { repository(FakeCatalogApi(listOf(first, incomplete)), dao).refresh() }

        assertTrue(result.isFailure)
        assertEquals(listOf("cached"), dao.rows.value.map { it.animeId })
        assertEquals(3L, dao.metadata?.revision)
    }

    @Test
    fun rejectsDuplicateIdsAcrossCatalogPages() = runTest {
        val dao = FakeCatalogDao()
        val first = page(1, 8, items("same", 48))
        val second = page(2, 8, listOf(items("same", 1, 47).single()))

        val result = runCatching { repository(FakeCatalogApi(listOf(first, second)), dao).refresh() }

        assertTrue(result.isFailure)
        assertTrue(dao.rows.value.isEmpty())
    }

    @Test
    fun rejectsNegativeRevision() = runTest {
        val dao = FakeCatalogDao()
        val invalid = page(1, -1, emptyList()).copy(total = 0, totalPages = 1)

        val result = runCatching { repository(FakeCatalogApi(listOf(invalid)), dao).refresh() }

        assertTrue(result.isFailure)
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
