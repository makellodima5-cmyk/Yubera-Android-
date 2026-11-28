package com.yubera.app.features.anime

import com.yubera.app.core.database.entity.AnimeProgressEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EpisodeProgressPresentationTest {
    @Test
    fun completedEpisodesShowFullBarAndCheckMarkState() {
        val result = episodeProgressPresentation(progress(current = 3, completed = 2, position = 0, duration = 0), 2)
        assertTrue(result.completed)
        assertEquals(1f, result.fraction, 0.001f)
    }

    @Test
    fun currentEpisodeShowsSavedFractionAndFutureEpisodeStartsAtZero() {
        val progress = progress(current = 3, completed = 2, position = 25_000, duration = 100_000)
        assertEquals(0.25f, episodeProgressPresentation(progress, 3).fraction, 0.001f)
        assertEquals(0f, episodeProgressPresentation(progress, 4).fraction, 0.001f)
    }

    private fun progress(current: Int, completed: Int, position: Long, duration: Long) = AnimeProgressEntity(
        animeId = "test", seasonNumber = null, episodeNumber = current, positionMillis = position,
        durationMillis = duration, updatedAtEpochMillis = 1, completedEpisodeNumber = completed, completedAtEpochMillis = 1
    )
}
