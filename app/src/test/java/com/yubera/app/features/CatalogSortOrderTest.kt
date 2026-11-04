package com.yubera.app.features

import com.yubera.app.core.catalog.AnimeCatalogItem
import com.yubera.app.features.catalog.CatalogSortOrder
import com.yubera.app.features.catalog.filterForCatalog
import com.yubera.app.features.catalog.sortedForCatalog
import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogSortOrderTest {
    private val items = listOf(
        anime("b", "Beta", year = 2022, rating = 8.2, status = "Завершён", genres = listOf("Фэнтези")),
        anime("c", "Gamma", year = 2025, rating = null, status = "Онгоинг", genres = listOf("Драма")),
        anime("a", "Alpha", year = 2024, rating = 9.1, status = "Завершён", genres = listOf("Фэнтези")),
        anime("d", "Delta", year = 0, rating = 7.5, status = "Анонс", genres = listOf("Комедия"))
    )

    @Test
    fun catalogOrderPreservesApiOrder() {
        assertEquals(listOf("b", "c", "a", "d"), items.sortedForCatalog(CatalogSortOrder.CATALOG).map { it.id })
    }

    @Test
    fun ratingOrderPutsRatedTitlesFirstAndHighestFirst() {
        assertEquals(listOf("a", "b", "d", "c"), items.sortedForCatalog(CatalogSortOrder.RATING).map { it.id })
    }

    @Test
    fun yearOrderPutsUnknownYearsLast() {
        assertEquals(listOf("c", "a", "b", "d"), items.sortedForCatalog(CatalogSortOrder.YEAR).map { it.id })
    }

    @Test
    fun titleOrderIsAlphabetical() {
        assertEquals(listOf("a", "b", "d", "c"), items.sortedForCatalog(CatalogSortOrder.TITLE).map { it.id })
    }

    @Test
    fun filtersCanBeCombinedWithoutChangingOriginalCatalog() {
        assertEquals(
            listOf("b", "a"),
            items.filterForCatalog(query = "", genre = "Фэнтези", year = null, status = "Завершён").map { it.id }
        )
        assertEquals(
            listOf("c"),
            items.filterForCatalog(query = "gamma", genre = null, year = 2025, status = "Онгоинг").map { it.id }
        )
    }

    private fun anime(
        id: String,
        title: String,
        year: Int,
        rating: Double?,
        status: String,
        genres: List<String>
    ) = AnimeCatalogItem(
        id = id,
        title = title,
        englishTitle = null,
        originalTitle = "",
        year = year,
        episodes = 12,
        rating = rating,
        genres = genres,
        status = status,
        description = "",
        posterUrl = null,
        bannerUrl = null,
        kind = "TV",
        ageRating = null,
        nextEpisodeAt = null,
        episodesAired = null,
        airedOn = null,
        studios = emptyList(),
        youtubeTrailerId = null,
        kodikAvailable = null
    )
}
