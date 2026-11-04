package com.yubera.app.data.auth

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface TelegramSessionApi {
    @POST("api/telegram/mobile-auth")
    suspend fun exchangeCode(@Body request: MobileAuthExchangeRequest): MobileAuthExchangeResponse

    @GET("api/telegram/init-data")
    suspend fun getSession(@Header("Authorization") authorization: String): TelegramSessionResponse

    @DELETE("api/telegram/init-data")
    suspend fun logout(@Header("Authorization") authorization: String): TelegramSessionResponse
}

@Serializable
data class MobileAuthExchangeRequest(
    val code: String,
    val codeVerifier: String
)

@Serializable
data class TelegramSessionResponse(
    val authenticated: Boolean,
    val user: TelegramSessionUser? = null
)

@Serializable
data class MobileAuthExchangeResponse(
    val authenticated: Boolean,
    val accessToken: String,
    val tokenType: String,
    val expiresAt: Long,
    val user: TelegramSessionUser
)

@Serializable
data class TelegramSessionUser(
    val id: String,
    val telegramId: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val username: String? = null,
    val languageCode: String? = null
)
