package com.yubera.app.features

import com.yubera.app.core.catalog.AnimeCatalogItem
import com.yubera.app.features.catalog.CatalogFilters
import com.yubera.app.features.catalog.CatalogSortOrder
import com.yubera.app.features.catalog.validate
import com.yubera.app.features.catalog.filterForCatalog
import com.yubera.app.features.catalog.sortedForCatalog
import com.yubera.app.features.catalog.searchPriority
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

    @Test
    fun exactTitleSearchGetsHigherPriorityThanUnmatchedTitles() {
        val query = "Alpha"
        assertEquals(0, searchPriority(items.first { it.title == query }, query))
        assertEquals(3, searchPriority(items.first { it.title == "Beta" }, query))
    }

    @Test
    fun fullCatalogFilterCombinesTypeAgeGenresYearsAndRating() {
        val candidates = listOf(
            anime("match", "Match", 2024, 8.5, "Завершён", listOf("Фэнтези")).copy(kind = "TV", ageRating = "16+"),
            anime("low", "Low rating", 2024, 6.0, "Завершён", listOf("Фэнтези")).copy(kind = "TV", ageRating = "16+"),
            anime("year", "Wrong year", 2022, 9.0, "Завершён", listOf("Фэнтези")).copy(kind = "TV", ageRating = "16+"),
            anime("genre", "Wrong genre", 2024, 9.0, "Завершён", listOf("Драма")).copy(kind = "TV", ageRating = "16+"),
            anime("kind", "Wrong kind", 2024, 9.0, "Завершён", listOf("Фэнтези")).copy(kind = "Movie", ageRating = "16+"),
            anime("age", "Wrong age", 2024, 9.0, "Завершён", listOf("Фэнтези")).copy(kind = "TV", ageRating = "12+")
        )
        val filters = CatalogFilters(status = "Завершён", kind = "TV", genres = setOf("Фэнтези"), ageRating = "16+", yearFrom = 2023, yearTo = 2025, minRating = 8.0)
        assertEquals(listOf("match"), candidates.filterForCatalog("", filters).map { it.id })
    }

    @Test
    fun filterYearValidationRejectsInvalidBoundsAndReversedRanges() {
        assertEquals("Укажите корректный год «От».", CatalogFilters(yearFrom = 1899).validate(2027))
        assertEquals("Укажите корректный год «До».", CatalogFilters(yearTo = 2028).validate(2027))
        assertEquals("Год «От» не может быть больше года «До».", CatalogFilters(yearFrom = 2025, yearTo = 2020).validate(2027))
        assertEquals(null, CatalogFilters(yearFrom = 2020, yearTo = 2025).validate(2027))
    }

    @Test
    fun searchMatchesVisuallySimilarLatinAndCyrillicLetters() {
        val russianTitle = listOf(anime("r", "Рассвет", 2024, 8.0, "Завершён", emptyList()))
        assertEquals(listOf("r"), russianTitle.filterForCatalog(query = "P", genre = null, year = null, status = null).map { it.id })
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
