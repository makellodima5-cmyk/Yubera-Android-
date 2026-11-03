package com.yubera.app.core.network

import javax.inject.Inject
import kotlinx.serialization.json.Json
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
        require(baseUrl.endsWith("/")) {
            "Retrofit base URL must end with a slash"
        }
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }
}
