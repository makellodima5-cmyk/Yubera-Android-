package com.yubera.app.data.library

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface TelegramLibrarySyncApi {
    @GET("api/telegram/library/sync")
    suspend fun getLibrary(@Header("Authorization") authorization: String): TelegramLibrarySyncResponse

    @POST("api/telegram/library/sync")
    suspend fun syncLibrary(
        @Header("Authorization") authorization: String,
        @Body request: TelegramLibrarySyncRequest
    ): TelegramLibrarySyncResponse
}

@Serializable
data class TelegramLibrarySyncRequest(
    val items: List<TelegramLibrarySyncItem>
)

@Serializable
data class TelegramLibrarySyncResponse(
    val authenticated: Boolean,
    val items: List<TelegramLibrarySyncItem> = emptyList()
)

@Serializable
data class TelegramLibrarySyncItem(
    val itemType: String,
    val animeId: String,
    val payload: TelegramLibrarySyncPayload,
    val updatedAtEpochMillis: Long,
    val deleted: Boolean,
    val sourceId: String
)

@Serializable
data class TelegramLibrarySyncPayload(
    val saved: Boolean? = null,
    val notificationsEnabled: Boolean? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val positionMillis: Long? = null,
    val durationMillis: Long? = null
)
