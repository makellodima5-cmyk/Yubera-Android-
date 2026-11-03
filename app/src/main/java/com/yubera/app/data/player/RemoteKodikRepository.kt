package com.yubera.app.data.player

import com.yubera.app.core.player.KodikEpisode
import com.yubera.app.core.player.KodikLinkPolicy
import com.yubera.app.core.player.KodikRepository
import com.yubera.app.core.player.KodikSource
import javax.inject.Inject

class RemoteKodikRepository @Inject constructor(
    private val api: KodikApi
) : KodikRepository {
    override suspend fun getSources(animeId: String): List<KodikSource> {
        require(animeId.matches(Regex("\\d{1,12}")) && animeId.toLongOrNull()?.let { it > 0 } == true) {
            "Некорректный ID аниме."
        }
        return api.getSources(animeId).sources.mapNotNull { source ->
            val episodes = source.episodes
                .filter { it.number > 0 && KodikLinkPolicy.isTrustedHttpsUrl(it.link) }
                .distinctBy { it.number }
                .sortedBy { it.number }
                .map { KodikEpisode(it.number, it.title, it.link) }
            if (source.id.isBlank() || source.title.isBlank() || episodes.isEmpty()) null
            else KodikSource(source.id, source.title, episodes)
        }
    }
}
