package com.yubera.app.features.anime

import com.yubera.app.core.database.entity.AnimeProgressEntity

internal data class EpisodeProgressPresentation(val completed: Boolean, val fraction: Float)

internal fun episodeProgressPresentation(progress: AnimeProgressEntity?, episodeNumber: Int): EpisodeProgressPresentation {
    val completed = (progress?.completedEpisodeNumber ?: 0) >= episodeNumber
    if (completed) return EpisodeProgressPresentation(completed = true, fraction = 1f)
    val fraction = if (progress?.episodeNumber == episodeNumber && progress.durationMillis > 0L) {
        (progress.positionMillis.toFloat() / progress.durationMillis.toFloat()).coerceIn(0f, 1f)
    } else 0f
    return EpisodeProgressPresentation(completed = false, fraction = fraction)
}
