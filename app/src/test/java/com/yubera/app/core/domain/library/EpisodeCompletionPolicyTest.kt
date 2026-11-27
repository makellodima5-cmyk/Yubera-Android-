package com.yubera.app.core.domain.library

import com.yubera.app.core.database.entity.AnimeProgressEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EpisodeCompletionPolicyTest {
    @Test
    fun marksEpisodeAtNinetyPercentOrMore() {
        val progress = AnimeProgressEntity(
            animeId = "anime-1", seasonNumber = null, episodeNumber = 3,
            positionMillis = 900_000L, durationMillis = 1_000_000L,
            updatedAtEpochMillis = 123L
        )

        val result = EpisodeCompletionPolicy.merge(progress, null)

        assertEquals(3, result.completedEpisodeNumber)
        assertEquals(123L, result.completedAtEpochMillis)
    }

    @Test
    fun doesNotMarkEpisodeBelowNinetyPercent() {
        val progress = AnimeProgressEntity(
            animeId = "anime-1", seasonNumber = null, episodeNumber = 3,
            positionMillis = 899_999L, durationMillis = 1_000_000L,
            updatedAtEpochMillis = 123L
        )

        val result = EpisodeCompletionPolicy.merge(progress, null)

        assertNull(result.completedEpisodeNumber)
        assertNull(result.completedAtEpochMillis)
    }

    @Test
    fun doesNotInferCompletionWhenExternalPlayerProvidesNoDuration() {
        val openedEpisode = AnimeProgressEntity(
            animeId = "anime-1", seasonNumber = null, episodeNumber = 3,
            positionMillis = 0L, durationMillis = 0L, updatedAtEpochMillis = 123L
        )

        val result = EpisodeCompletionPolicy.merge(openedEpisode, null)

        assertNull(result.completedEpisodeNumber)
        assertNull(result.completedAtEpochMillis)
    }

    @Test
    fun preservesTitleCompletionWhenCurrentEpisodeChanges() {
        val previous = AnimeProgressEntity(
            animeId = "anime-1", seasonNumber = null, episodeNumber = 3,
            positionMillis = 900_000L, durationMillis = 1_000_000L,
            updatedAtEpochMillis = 123L, completedEpisodeNumber = 3,
            completedAtEpochMillis = 123L
        )
        val nextEpisode = AnimeProgressEntity(
            animeId = "anime-1", seasonNumber = null, episodeNumber = 4,
            positionMillis = 0L, durationMillis = 0L, updatedAtEpochMillis = 124L
        )

        val result = EpisodeCompletionPolicy.merge(nextEpisode, previous)

        assertEquals(3, result.completedEpisodeNumber)
        assertEquals(123L, result.completedAtEpochMillis)
    }

    @Test
    fun incompleteEpisodeDoesNotErasePreviouslyCompletedEpisode() {
        val previous = AnimeProgressEntity(
            animeId = "anime-1", seasonNumber = null, episodeNumber = 3,
            positionMillis = 900_000L, durationMillis = 1_000_000L,
            updatedAtEpochMillis = 123L, completedEpisodeNumber = 3,
            completedAtEpochMillis = 123L
        )
        val resumed = previous.copy(positionMillis = 100_000L, updatedAtEpochMillis = 124L)

        val result = EpisodeCompletionPolicy.merge(resumed, previous)

        assertEquals(3, result.completedEpisodeNumber)
        assertEquals(123L, result.completedAtEpochMillis)
    }
}
