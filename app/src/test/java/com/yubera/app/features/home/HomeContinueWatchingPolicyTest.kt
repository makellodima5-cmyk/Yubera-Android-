package com.yubera.app.features.home

import com.yubera.app.core.catalog.AnimeCatalogItem
import com.yubera.app.core.database.entity.AnimeProgressEntity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeContinueWatchingPolicyTest {
    @Test
    fun excludesFullyCompletedNonOngoingTitle() {
        assertFalse(isContinueWatchingEligible(anime(12, "Вышел"), progress(12, 12)))
    }

    @Test
    fun includesIncompleteAndOngoingTitles() {
        assertTrue(isContinueWatchingEligible(anime(12, "Вышел"), progress(5, 4)))
        assertTrue(isContinueWatchingEligible(anime(12, "Онгоинг"), progress(12, 12)))
    }

    @Test
    fun excludesTitlesWithAlmostFinishedCurrentEpisode() {
        assertFalse(isContinueWatchingEligible(anime(12, "Вышел"), progress(5, 4, 91_000, 100_000)))
    }

    private fun progress(current: Int, completed: Int, position: Long = 0, duration: Long = 0) = AnimeProgressEntity(
        animeId = "test", seasonNumber = null, episodeNumber = current, positionMillis = position,
        durationMillis = duration, updatedAtEpochMillis = 1, completedEpisodeNumber = completed, completedAtEpochMillis = 1
    )
    private fun anime(episodes: Int, status: String) = AnimeCatalogItem(
        id = "test", title = "Test", englishTitle = null, originalTitle = "Test", year = 2026, episodes = episodes,
        rating = 8.0, genres = listOf("Fantasy"), status = status, description = "", posterUrl = null, bannerUrl = null,
        kind = "TV", ageRating = null, nextEpisodeAt = null, episodesAired = null, airedOn = null, studios = emptyList(),
        youtubeTrailerId = null, kodikAvailable = true
    )
}
