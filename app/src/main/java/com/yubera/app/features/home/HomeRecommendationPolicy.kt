package com.yubera.app.features.home

import com.yubera.app.core.catalog.AnimeCatalogItem
import com.yubera.app.core.database.entity.AnimeProgressEntity
import com.yubera.app.core.domain.library.EpisodeCompletionPolicy
import java.util.Locale

/**
 * Recommendations use completed episodes to infer genre preferences, but only remove
 * a title from the pool after the user has completed the title's known episode count.
 */
internal object HomeRecommendationPolicy {
    fun fullyCompletedAnimeIds(
        progress: List<AnimeProgressEntity>,
        catalog: List<AnimeCatalogItem>
    ): Set<String> {
        val byId = catalog.associateBy(AnimeCatalogItem::id)
        return progress.mapNotNull { row ->
            val anime = byId[row.animeId] ?: return@mapNotNull null
            val totalEpisodes = anime.episodes.takeIf { it > 0 } ?: return@mapNotNull null
            val status = anime.status.lowercase(Locale.ROOT)
            val isOngoingOrUpcoming = listOf("ongoing", "airing", "онгоинг", "выходит", "анонс", "upcoming", "planned", "not yet aired")
                .any { marker -> status.contains(marker) }
            if (isOngoingOrUpcoming) return@mapNotNull null
            val completedEpisode = maxOf(
                row.completedEpisodeNumber ?: 0,
                if (EpisodeCompletionPolicy.isEpisodeCompleted(row)) row.episodeNumber ?: 0 else 0
            )
            row.animeId.takeIf { completedEpisode >= totalEpisodes }
        }.toSet()
    }

    fun selectRecommendations(
        catalog: List<AnimeCatalogItem>,
        progress: List<AnimeProgressEntity>,
        dailySeed: Long
    ): List<AnimeCatalogItem> {
        val byId = catalog.associateBy(AnimeCatalogItem::id)
        val completedProgress = progress.filter { row ->
            row.completedEpisodeNumber != null || EpisodeCompletionPolicy.isEpisodeCompleted(row)
        }
        val watchedGenres = completedProgress
            .flatMap { byId[it.animeId]?.genres.orEmpty() }
            .groupingBy { it.lowercase(Locale.ROOT) }.eachCount()
        val fullyCompleted = fullyCompletedAnimeIds(progress, catalog)
        val eligible = catalog.filter { it.id !in fullyCompleted && it.kodikAvailable != false }
        val pool = if (watchedGenres.isEmpty()) {
            eligible.sortedWith(compareByDescending<AnimeCatalogItem> { it.rating ?: 0.0 }.thenBy { it.title }).take(20)
        } else {
            eligible.sortedWith(
                compareByDescending<AnimeCatalogItem> { anime ->
                    anime.genres.sumOf { watchedGenres[it.lowercase(Locale.ROOT)] ?: 0 } * 10.0 + (anime.rating ?: 0.0)
                }.thenBy { it.title }
            ).take(20)
        }
        val rotation = if (pool.isEmpty()) 0 else Math.floorMod(dailySeed, pool.size.toLong()).toInt()
        val rotated = pool.drop(rotation) + pool.take(rotation)
        return rotated.take(5)
    }
}
