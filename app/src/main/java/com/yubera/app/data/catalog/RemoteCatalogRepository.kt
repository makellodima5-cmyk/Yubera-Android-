package com.yubera.app.data.catalog

import com.yubera.app.core.catalog.AnimeCatalogItem
import com.yubera.app.core.catalog.CatalogRepository
import com.yubera.app.core.database.dao.CatalogDao
import com.yubera.app.core.database.entity.CatalogAnimeEntity
import com.yubera.app.core.database.entity.CatalogMetadataEntity
import javax.inject.Inject
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
        require(page.total >= 0) { "API каталога вернул некорректное количество записей." }
        require(page.totalPages in 1..MAX_PAGES) { "API каталога вернул некорректное количество страниц." }
        require(page.items.size <= PAGE_SIZE) { "API каталога превысил размер страницы." }
        require(page.items.size >= minOf(page.total, PAGE_SIZE)) { "API каталога вернул неполную первую страницу." }
    }

    companion object {
        const val PAGE_SIZE = 48
        const val SUPPORTED_SCHEMA_VERSION = 1
        const val MAX_PAGES = 10_000
        const val MAX_REVISION_ATTEMPTS = 2
    }
}
