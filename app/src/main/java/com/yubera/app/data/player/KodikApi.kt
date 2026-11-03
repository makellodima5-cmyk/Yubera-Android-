package com.yubera.app.data.player

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

interface KodikApi {
    @GET("api/kodik")
    suspend fun getSources(@Query("animeId") animeId: String): KodikResponseDto
}

@Serializable
data class KodikResponseDto(
    val sources: List<KodikSourceDto> = emptyList()
)

@Serializable
data class KodikSourceDto(
    val id: String,
    val title: String,
    val episodes: List<KodikEpisodeDto> = emptyList()
)

@Serializable
data class KodikEpisodeDto(
    val number: Int,
    val title: String = "",
    val link: String
)
