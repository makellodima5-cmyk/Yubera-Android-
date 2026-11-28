package com.yubera.app.features.anime

import com.yubera.app.core.catalog.AnimeCatalogItem
import org.junit.Assert.assertEquals
import org.junit.Test

class AnimeRelatedPolicyTest {
    @Test
    fun linkedTitlesComeFirstAndGenreFallbackFillsWithoutDuplicates() {
        val current = anime("current", listOf("Fantasy", "Adventure"))
        val catalog = listOf(current, anime("genre", listOf("Fantasy")), anime("linked", listOf("Drama")), anime("both", listOf("Fantasy")))
        val result = AnimeRelatedPolicy.select(current, catalog, listOf("linked", "both", "missing", "linked"))
        assertEquals(listOf("linked", "both", "genre"), result.map { it.id })
    }

    @Test
    fun excludesCurrentAndReturnsEmptyWhenCurrentIsMissing() {
        val current = anime("current", listOf("Fantasy"))
        assertEquals(listOf("other"), AnimeRelatedPolicy.select(current, listOf(current, anime("other", listOf("Fantasy"))), listOf("current")).map { it.id })
        assertEquals(emptyList<AnimeCatalogItem>(), AnimeRelatedPolicy.select(null, listOf(current), emptyList()))
    }

    private fun anime(id: String, genres: List<String>) = AnimeCatalogItem(
        id = id, title = id, englishTitle = null, originalTitle = id, year = 2026, episodes = 12,
        rating = 8.0, genres = genres, status = "Вышел", description = "", posterUrl = null, bannerUrl = null,
        kind = "TV", ageRating = null, nextEpisodeAt = null, episodesAired = null, airedOn = null,
        studios = emptyList(), youtubeTrailerId = null, kodikAvailable = true
    )
}
