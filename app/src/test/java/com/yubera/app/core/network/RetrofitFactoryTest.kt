package com.yubera.app.core.network

import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RetrofitFactoryTest {

    private val factory = RetrofitFactory(
        okHttpClient = OkHttpClient(),
        json = Json { ignoreUnknownKeys = true }
    )

    @Test
    fun createsRetrofitForAnHttpsBaseUrl() {
        val retrofit = factory.create("https://api.example.test/")

        assertEquals("https", retrofit.baseUrl().scheme)
        assertEquals("api.example.test", retrofit.baseUrl().host)
    }

    @Test
    fun rejectsCleartextHttpBaseUrls() {
        assertThrows(IllegalArgumentException::class.java) {
            factory.create("http://api.example.test/")
        }
    }

    @Test
    fun rejectsBaseUrlsWithoutTrailingSlash() {
        assertThrows(IllegalArgumentException::class.java) {
            factory.create("https://api.example.test/api")
        }
    }
}
