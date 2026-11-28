package com.yubera.app.data.news

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NewsVideoExtractionTest {
    @Test
    fun extractsWatchShortAndEmbedUrlsAndNormalizesHttps() {
        assertEquals("https://www.youtube.com/watch?v=abc123", extractNewsVideoUrl("https://www.youtube.com/watch?v=abc123"))
        assertEquals("https://youtu.be/abc123", extractNewsVideoUrl("http://youtu.be/abc123"))
        assertEquals("https://www.youtube.com/embed/abc123", extractNewsVideoUrl("https://www.youtube.com/embed/abc123"))
    }

    @Test
    fun ignoresNonYoutubeUrls() {
        assertNull(extractNewsVideoUrl("https://example.com/video"))
    }
}
