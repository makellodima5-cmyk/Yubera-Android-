package com.yubera.app.core.player

data class KodikEpisode(
    val number: Int,
    val title: String,
    val link: String
)

data class KodikSource(
    val id: String,
    val title: String,
    val episodes: List<KodikEpisode>
)

interface KodikRepository {
    suspend fun getSources(animeId: String): List<KodikSource>
}

object KodikLinkPolicy {
    private val trustedHosts = setOf(
        "kodik.info",
        "kodik.biz",
        "kodik.cc",
        "kodik.online",
        "kodikplayer.com",
        "kodikres.com"
    )

    fun isTrustedHttpsUrl(raw: String): Boolean {
        return try {
            val uri = java.net.URI(raw)
            val host = uri.host?.lowercase()?.removeSuffix(".") ?: return false
            uri.scheme.equals("https", ignoreCase = true) &&
                trustedHosts.any { host == it || host.endsWith(".$it") }
        } catch (_: Exception) {
            false
        }
    }
}
