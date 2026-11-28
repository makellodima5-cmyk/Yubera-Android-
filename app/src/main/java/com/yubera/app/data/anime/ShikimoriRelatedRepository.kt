package com.yubera.app.data.anime

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShikimoriRelatedRepository @Inject constructor(private val api: ShikimoriAnimeApi) {
    private data class Cached(val fetchedAtEpochMillis: Long, val animeIds: List<Long>)
    private val cache = ConcurrentHashMap<Long, Cached>()

    suspend fun getAnimeIds(animeId: Long): List<Long> {
        val now = System.currentTimeMillis()
        val previous = cache[animeId]
        if (previous != null && now >= previous.fetchedAtEpochMillis && now - previous.fetchedAtEpochMillis < CACHE_TTL) {
            return previous.animeIds
        }
        return try {
            val ids = api.getRelated(animeId).mapNotNull { it.anime?.id }.filter { it > 0 }.distinct()
            cache[animeId] = Cached(now, ids)
            ids
        } catch (_: Exception) {
            previous?.animeIds.orEmpty()
        }
    }

    companion object { private const val CACHE_TTL = 6 * 60 * 60 * 1000L }
}
