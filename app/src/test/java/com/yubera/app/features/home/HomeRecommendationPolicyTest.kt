package com.yubera.app.features.home

import com.yubera.app.core.catalog.AnimeCatalogItem
import com.yubera.app.core.database.entity.AnimeProgressEntity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeRecommendationPolicyTest {
    @Test
    fun completedEpisodeDoesNotExcludeSeriesUntilAllEpisodesAreComplete() {
        val catalog = listOf(anime("series", episodes = 12), anime("other", episodes = 24))
        val progress = listOf(progress("series", currentEpisode = 2, completedEpisode = 1))

        val completed = HomeRecommendationPolicy.fullyCompletedAnimeIds(progress, catalog)
        val recommendations = HomeRecommendationPolicy.selectRecommendations(catalog, progress, dailySeed = 0L)

        assertFalse("An unfinished series should remain eligible", "series" in completed)
        assertTrue(recommendations.any { it.id == "series" })
    }

    @Test
    fun singleEpisodeTitleIsExcludedWhenItsOnlyEpisodeIsCompleted() {
        val catalog = listOf(anime("film", episodes = 1), anime("other", episodes = 12))
        val progress = listOf(progress("film", currentEpisode = 1, completedEpisode = 1))

        val completed = HomeRecommendationPolicy.fullyCompletedAnimeIds(progress, catalog)
        val recommendations = HomeRecommendationPolicy.selectRecommendations(catalog, progress, dailySeed = 0L)

        assertTrue("A fully watched one-episode title should be excluded", "film" in completed)
        assertFalse(recommendations.any { it.id == "film" })
    }

    @Test
    fun ongoingTitleIsNotExcludedEvenWhenAllCurrentlyListedEpisodesAreComplete() {
        val catalog = listOf(anime("ongoing", episodes = 12, status = "Онгоинг"), anime("other", episodes = 24))
        val progress = listOf(progress("ongoing", currentEpisode = 12, completedEpisode = 12))

        val completed = HomeRecommendationPolicy.fullyCompletedAnimeIds(progress, catalog)

        assertFalse("An ongoing title is not fully watched", "ongoing" in completed)
    }

    @Test
    fun titleWithUnknownEpisodeCountIsNotAssumedFullyWatched() {
        val catalog = listOf(anime("unknown", episodes = 0), anime("other", episodes = 12))
        val progress = listOf(progress("unknown", currentEpisode = 5, completedEpisode = 5))

        assertFalse("Unknown episode count must not cause false exclusion", "unknown" in HomeRecommendationPolicy.fullyCompletedAnimeIds(progress, catalog))
    }

    @Test
    fun continueWatchingExcludesFullyCompletedTitles() {
        val finished = anime("finished", episodes = 12)
        assertFalse(isContinueWatchingEligible(finished, progress("finished", currentEpisode = 12, completedEpisode = 12)))
    }

    @Test
    fun continueWatchingKeepsIncompleteAndOngoingTitles() {
        val incomplete = anime("incomplete", episodes = 12)
        val ongoing = anime("ongoing", episodes = 12, status = "Онгоинг")
        assertTrue(isContinueWatchingEligible(incomplete, progress("incomplete", currentEpisode = 5, completedEpisode = 4)))
        assertTrue(isContinueWatchingEligible(ongoing, progress("ongoing", currentEpisode = 12, completedEpisode = 12)))
    }

    private fun progress(animeId: String, currentEpisode: Int, completedEpisode: Int) = AnimeProgressEntity(
        animeId = animeId,
        seasonNumber = null,
        episodeNumber = currentEpisode,
        positionMillis = 0L,
        durationMillis = 0L,
        updatedAtEpochMillis = 1L,
        completedEpisodeNumber = completedEpisode,
        completedAtEpochMillis = 1L
    )

    private fun anime(id: String, episodes: Int, status: String = "Вышел") = AnimeCatalogItem(
        id = id,
        title = id,
        englishTitle = null,
        originalTitle = id,
        year = 2026,
        episodes = episodes,
        rating = 8.0,
        genres = listOf("Fantasy"),
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
        kodikAvailable = true
    )
}
