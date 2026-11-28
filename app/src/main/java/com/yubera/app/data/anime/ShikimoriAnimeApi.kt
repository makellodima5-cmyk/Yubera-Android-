package com.yubera.app.data.anime

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path

interface ShikimoriAnimeApi {
    @GET("api/animes/{id}/related")
    suspend fun getRelated(@Path("id") id: Long): List<ShikimoriRelatedDto>
}

@Serializable
data class ShikimoriRelatedDto(
    val relation: String? = null,
    @SerialName("relation_russian") val relationRussian: String? = null,
    val anime: ShikimoriRelatedAnimeDto? = null
)

@Serializable
data class ShikimoriRelatedAnimeDto(val id: Long? = null)
