package com.yubera.app.features

import com.yubera.app.core.catalog.AnimeCatalogItem
import com.yubera.app.features.catalog.CatalogSortOrder
import com.yubera.app.features.catalog.sortedForCatalog
import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogSortOrderTest {
    private val items = listOf(
        anime("b", "Beta", year = 2022, rating = 8.2),
        anime("c", "Gamma", year = 2025, rating = null),
        anime("a", "Alpha", year = 2024, rating = 9.1),
        anime("d", "Delta", year = 0, rating = 7.5)
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

    private fun anime(id: String, title: String, year: Int, rating: Double?) = AnimeCatalogItem(
        id = id,
        title = title,
        englishTitle = null,
        originalTitle = "",
        year = year,
        episodes = 12,
        rating = rating,
        genres = listOf("Фэнтези"),
        status = "Завершён",
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
