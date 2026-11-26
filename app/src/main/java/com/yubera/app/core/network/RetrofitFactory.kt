package com.yubera.app.core.network

import javax.inject.Inject
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class RetrofitFactory @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json
) {
    fun create(baseUrl: String): Retrofit {
        require(baseUrl.startsWith("https://", ignoreCase = true)) {
            "YUBERA API base URL must use HTTPS"
        }
        val parsedBaseUrl = baseUrl.toHttpUrlOrNull()
        require(parsedBaseUrl != null && parsedBaseUrl.isHttps) {
            "YUBERA API base URL must be a valid HTTPS URL"
        }
        require(parsedBaseUrl.encodedPath.endsWith("/")) {
            "Retrofit base URL must end with a slash"
        }
        require(parsedBaseUrl.username.isEmpty() && parsedBaseUrl.password.isEmpty()) {
            "API base URL must not contain credentials"
        }
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }
}
