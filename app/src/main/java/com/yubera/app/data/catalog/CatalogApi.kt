package com.yubera.app.data.catalog

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

interface CatalogApi {
    @GET("api/mobile/catalog")
    suspend fun getCatalogPage(
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int
    ): CatalogPageDto
}

@Serializable
data class CatalogPageDto(
    val schemaVersion: Int = 1,
    val revision: Long = 0,
    val updatedAt: String = "",
    val page: Int = 1,
    val pageSize: Int = 24,
    val total: Int = 0,
    val totalPages: Int = 1,
    val items: List<AnimeCatalogDto> = emptyList()
)

@Serializable
data class AnimeCatalogDto(
    val id: String,
    val title: String,
    val englishTitle: String? = null,
    val original: String = "",
    val year: Int = 0,
    val episodes: Int = 0,
    val rating: Double? = null,
    val genres: List<String> = emptyList(),
    val status: String = "Неизвестно",
    val description: String = "",
    val posterUrl: String? = null,
    val bannerUrl: String? = null,
    val kind: String? = null,
    val ageRating: String? = null,
    val nextEpisodeAt: String? = null,
    val episodesAired: Int? = null,
    val airedOn: String? = null,
    val studios: List<String> = emptyList(),
    val youtubeTrailerId: String? = null,
    val kodikAvailable: Boolean? = null
) {
    fun toDomain() = com.yubera.app.core.catalog.AnimeCatalogItem(
        id, title, englishTitle, original, year, episodes, rating, genres, status,
        description, posterUrl, bannerUrl, kind, ageRating, nextEpisodeAt,
        episodesAired, airedOn, studios, youtubeTrailerId, kodikAvailable
    )
}
