package com.yubera.app.data.catalog

import com.yubera.app.core.catalog.AnimeCatalogItem
import com.yubera.app.core.catalog.CatalogRepository
import com.yubera.app.core.database.dao.CatalogDao
import com.yubera.app.core.database.entity.CatalogAnimeEntity
import com.yubera.app.core.database.entity.CatalogMetadataEntity
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CatalogRevisionChangedException : Exception("Каталог обновился во время загрузки. Повторите попытку.")

@Singleton
class RemoteCatalogRepository @Inject constructor(
    private val api: CatalogApi,
    private val catalogDao: CatalogDao,
    private val json: Json
) : CatalogRepository {
    /*
     * A single installation can start refreshes from more than one screen/worker.
     * Serialize them so two callers never download and replace the same snapshot
     * concurrently or overwrite a newer revision with an older response.
     */
    private val refreshMutex = Mutex()
    private val checkedAtMutex = Mutex()

    override fun observeCatalog(): Flow<List<AnimeCatalogItem>> =
        catalogDao.observeAll().map { rows ->
            rows.map { row -> json.decodeFromString<AnimeCatalogDto>(row.payloadJson).toDomain() }
        }

    override fun observeAnime(animeId: String): Flow<AnimeCatalogItem?> =
        catalogDao.observeById(animeId).map { row ->
            row?.let { json.decodeFromString<AnimeCatalogDto>(it.payloadJson).toDomain() }
        }

    override suspend fun refresh() = withContext(Dispatchers.IO) {
        refreshMutex.withLock {
            repeat(MAX_REVISION_ATTEMPTS) {
                val first = api.getCatalogPage(page = 1, pageSize = PAGE_SIZE)
                validateFirstPage(first)
                val cachedMetadata = catalogDao.getMetadata()
                if (cachedMetadata?.revision == first.revision) {
                    catalogDao.saveMetadata(cachedMetadata.copy(checkedAtEpochMillis = System.currentTimeMillis()))
                    return@withLock
                }

                val allItems = first.items.toMutableList()
                var revisionChanged = false
                for (pageNumber in 2..first.totalPages) {
                    val next = api.getCatalogPage(page = pageNumber, pageSize = PAGE_SIZE)
                    if (next.revision != first.revision || next.schemaVersion != first.schemaVersion) {
                        revisionChanged = true
                        break
                    }
                    require(next.page == pageNumber) { "API каталога вернул неверный номер страницы." }
                    require(next.pageSize == first.pageSize) { "Размер страницы изменился во время загрузки каталога." }
                    require(next.items.size <= next.pageSize) { "API каталога превысил размер страницы." }
                    val remainingItems = first.total - allItems.size
                    require(next.items.size >= minOf(remainingItems, next.pageSize)) {
                        "API каталога вернул неполную страницу."
                    }
                    require(next.items.none { it.id.isBlank() }) { "Каталог содержит пустой ID." }
                    require(next.items.map { it.id }.toSet().size == next.items.size) { "Каталог содержит повторяющиеся ID." }
                    allItems += next.items
                }

                if (revisionChanged) return@repeat

                val uniqueItems = allItems.distinctBy { it.id }
                require(uniqueItems.size == first.total) { "Каталог содержит повторяющиеся или отсутствующие ID." }
                val entities = uniqueItems.mapIndexed { index, item ->
                    require(item.id.isNotBlank()) { "Каталог содержит пустой ID." }
                    CatalogAnimeEntity(
                        animeId = item.id,
                        title = item.title,
                        originalTitle = item.original,
                        year = item.year,
                        status = item.status,
                        genresJson = json.encodeToString(item.genres),
                        payloadJson = json.encodeToString(item),
                        position = index
                    )
                }
                catalogDao.replaceSnapshot(
                    items = entities,
                    metadata = CatalogMetadataEntity(revision = first.revision, updatedAt = first.updatedAt, checkedAtEpochMillis = System.currentTimeMillis())
                )
                return@withLock
            }
            throw CatalogRevisionChangedException()
        }
    }

    suspend fun markCheckDeferred() {
        val metadata = catalogDao.getMetadata() ?: return
        catalogDao.saveMetadata(metadata.copy(checkedAtEpochMillis = System.currentTimeMillis()))
    }

    private fun validateFirstPage(page: CatalogPageDto) {
        require(page.schemaVersion == SUPPORTED_SCHEMA_VERSION) { "Версия API каталога не поддерживается." }
        require(page.page == 1) { "API каталога вернул неверную первую страницу." }
        require(page.revision >= 0L) { "API каталога вернул некорректную версию." }
        require(page.total >= 0) { "API каталога вернул некорректное количество записей." }
        require(page.pageSize > 0) { "API каталога вернул некорректный размер страницы." }
        require(page.pageSize == PAGE_SIZE) { "API каталога вернул неожиданный размер страницы." }
        require(page.totalPages == expectedPageCount(page.total, page.pageSize)) {
            "API каталога вернул некорректное количество страниц."
        }
        require(page.items.size <= page.pageSize) { "API каталога превысил размер страницы." }
        require(page.items.size >= minOf(page.total, page.pageSize)) { "API каталога вернул неполную первую страницу." }
        require(page.items.none { it.id.isBlank() }) { "Каталог содержит пустой ID." }
        require(page.items.map { it.id }.toSet().size == page.items.size) { "Каталог содержит повторяющиеся ID." }
    }

    private fun expectedPageCount(total: Int, pageSize: Int): Int =
        if (total == 0) 1 else ((total.toLong() + pageSize - 1L) / pageSize).toInt()

    /**
     * A cheap conditional check for an unchanged catalog. WorkManager runs this
     * less often and only when network and battery constraints allow it.
     */
    suspend fun refreshIfStale(minIntervalMillis: Long = DEFAULT_CHECK_INTERVAL_MILLIS) = withContext(Dispatchers.IO) {
        require(minIntervalMillis >= 0L)
        checkedAtMutex.withLock {
            val metadata = catalogDao.getMetadata()
            val now = System.currentTimeMillis()
            if (metadata != null && metadata.checkedAtEpochMillis > 0L &&
                now >= metadata.checkedAtEpochMillis &&
                now - metadata.checkedAtEpochMillis < minIntervalMillis
            ) {
                return@withLock
            }
            val latest = runCatching { api.getCatalogPage(page = 1, pageSize = PAGE_SIZE) }.getOrElse {
                markCheckDeferred()
                throw it
            }
            validateFirstPage(latest)
            if (metadata?.revision == latest.revision) {
                catalogDao.saveMetadata(metadata.copy(checkedAtEpochMillis = now))
                return@withLock
            }
            refresh()
        }
    }

    companion object {
        const val PAGE_SIZE = 48
        const val SUPPORTED_SCHEMA_VERSION = 1
        const val MAX_REVISION_ATTEMPTS = 2
        val DEFAULT_CHECK_INTERVAL_MILLIS: Long = TimeUnit.HOURS.toMillis(6)
    }
}
