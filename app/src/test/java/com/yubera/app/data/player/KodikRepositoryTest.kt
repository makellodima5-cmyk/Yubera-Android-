package com.yubera.app.data.player

import com.yubera.app.core.player.KodikLinkPolicy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KodikRepositoryTest {
    @Test
    fun acceptsOnlyTrustedHttpsPlayerHosts() {
        assertTrue(KodikLinkPolicy.isTrustedHttpsUrl("https://kodik.info/embed/123"))
        assertTrue(KodikLinkPolicy.isTrustedHttpsUrl("https://sub.kodikplayer.com/123"))
        assertFalse(KodikLinkPolicy.isTrustedHttpsUrl("http://kodik.info/embed/123"))
        assertFalse(KodikLinkPolicy.isTrustedHttpsUrl("https://kodik.info.evil.example/embed/123"))
        assertFalse(KodikLinkPolicy.isTrustedHttpsUrl("https://example.com/embed/123"))
    }

    @Test
    fun filtersUnsafeLinksAndDuplicateEpisodeNumbers() = runTest {
        val api = FakeKodikApi(
            KodikResponseDto(
                sources = listOf(
                    KodikSourceDto(
                        id = "1",
                        title = "Озвучка",
                        episodes = listOf(
                            KodikEpisodeDto(1, "Первая", "https://kodik.info/episode/1"),
                            KodikEpisodeDto(1, "Дубликат", "https://kodik.info/episode/1-copy"),
                            KodikEpisodeDto(2, "Вторая", "https://evil.example/episode/2")
                        )
                    )
                )
            )
        )

        val result = RemoteKodikRepository(api).getSources("123")

        assertEquals(1, result.size)
        assertEquals(listOf(1), result.single().episodes.map { it.number })
        assertEquals("https://kodik.info/episode/1", result.single().episodes.single().link)
    }

    @Test
    fun rejectsNonNumericAnimeIdsBeforeNetworkCall() = runTest {
        val api = FakeKodikApi(KodikResponseDto())
        var failed = false
        try {
            RemoteKodikRepository(api).getSources("../123")
        } catch (_: IllegalArgumentException) {
            failed = true
        }
        assertTrue(failed)
        assertFalse(api.called)
    }

    private class FakeKodikApi(
        private val response: KodikResponseDto
    ) : KodikApi {
        var called = false
        override suspend fun getSources(animeId: String): KodikResponseDto {
            called = true
            return response
        }
    }
}
