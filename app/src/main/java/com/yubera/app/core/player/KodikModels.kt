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

/**
 * Restricts player URLs to HTTPS pages owned by known Kodik hostnames.
 * A dot boundary prevents accepting attacker-owned suffixes such as
 * kodikplayer.com.attacker.example.
 */
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
        val uri = runCatching { java.net.URI(raw) }.getOrNull() ?: return false
        if (!uri.scheme.equals("https", ignoreCase = true)) return false
        if (uri.rawUserInfo != null || (uri.port != -1 && uri.port != 443)) return false
        val host = uri.host?.lowercase(java.util.Locale.ROOT)?.removeSuffix(".") ?: return false
        return trustedHosts.any { host == it || host.endsWith(".$it") }
    }

    fun isSameTrustedHost(first: String?, second: String?): Boolean {
        val firstUri = first?.let { runCatching { java.net.URI(it) }.getOrNull() } ?: return false
        val secondUri = second?.let { runCatching { java.net.URI(it) }.getOrNull() } ?: return false
        val firstHost = firstUri.host?.lowercase(java.util.Locale.ROOT)?.removeSuffix(".") ?: return false
        val secondHost = secondUri.host?.lowercase(java.util.Locale.ROOT)?.removeSuffix(".") ?: return false
        return isTrustedHttpsUrl(first.orEmpty()) &&
            isTrustedHttpsUrl(second.orEmpty()) &&
            firstHost == secondHost
    }
}
