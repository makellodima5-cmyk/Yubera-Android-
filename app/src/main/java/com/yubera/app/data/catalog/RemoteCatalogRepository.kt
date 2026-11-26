package com.yubera.app.data.catalog

import com.yubera.app.core.catalog.AnimeCatalogItem
import com.yubera.app.core.catalog.CatalogRepository
import com.yubera.app.core.database.dao.CatalogDao
import com.yubera.app.core.database.entity.CatalogAnimeEntity
import com.yubera.app.core.database.entity.CatalogMetadataEntity
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CatalogRevisionChangedException : Exception("Каталог обновился во время загрузки. Повторите попытку.")

class RemoteCatalogRepository @Inject constructor(
    private val api: CatalogApi,
    private val catalogDao: CatalogDao,
    private val json: Json
) : CatalogRepository {

    override fun observeCatalog(): Flow<List<AnimeCatalogItem>> =
        catalogDao.observeAll().map { rows ->
            rows.map { row -> json.decodeFromString<AnimeCatalogDto>(row.payloadJson).toDomain() }
        }

    override fun observeAnime(animeId: String): Flow<AnimeCatalogItem?> =
        catalogDao.observeById(animeId).map { row ->
            row?.let { json.decodeFromString<AnimeCatalogDto>(it.payloadJson).toDomain() }
        }

    override suspend fun refresh() = withContext(Dispatchers.IO) {
        repeat(MAX_REVISION_ATTEMPTS) {
            val first = api.getCatalogPage(page = 1, pageSize = PAGE_SIZE)
            validateFirstPage(first)

            if (catalogDao.getMetadata()?.revision == first.revision) {
                return@withContext
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
                metadata = CatalogMetadataEntity(revision = first.revision, updatedAt = first.updatedAt)
            )
            return@withContext
        }
        throw CatalogRevisionChangedException()
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

    companion object {
        const val PAGE_SIZE = 48
        const val SUPPORTED_SCHEMA_VERSION = 1
        const val MAX_REVISION_ATTEMPTS = 2
    }
}
