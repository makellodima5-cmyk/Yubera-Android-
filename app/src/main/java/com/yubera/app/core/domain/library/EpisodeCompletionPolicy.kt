package com.yubera.app.core.domain.library

import com.yubera.app.core.database.entity.AnimeProgressEntity

/**
 * Completion is only recorded when a saved progress snapshot contains a duration
 * and the playhead reaches the 90% threshold. Existing title-level markers are
 * retained when the current episode row is replaced by another episode.
 */
object EpisodeCompletionPolicy {
    fun merge(progress: AnimeProgressEntity, previous: AnimeProgressEntity?): AnimeProgressEntity {
        val completedNow = progress.episodeNumber?.takeIf { episode ->
            progress.durationMillis > 0L &&
                progress.positionMillis >= (progress.durationMillis * COMPLETION_RATIO).toLong()
        }
        val markerEpisode = completedNow ?: previous?.completedEpisodeNumber
        val markerTimestamp = when {
            completedNow != null -> progress.updatedAtEpochMillis
            markerEpisode != null -> previous?.completedAtEpochMillis
            else -> null
        }
        return progress.copy(
            completedEpisodeNumber = markerEpisode,
            completedAtEpochMillis = markerTimestamp
        )
    }

    fun isEpisodeCompleted(progress: AnimeProgressEntity): Boolean =
        progress.episodeNumber != null && progress.durationMillis > 0L &&
            progress.positionMillis >= (progress.durationMillis * COMPLETION_RATIO).toLong()

    private const val COMPLETION_RATIO = 0.90
}
