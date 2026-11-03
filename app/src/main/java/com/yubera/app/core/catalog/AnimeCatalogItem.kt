package com.yubera.app.core.catalog

data class AnimeCatalogItem(
    val id: String,
    val title: String,
    val englishTitle: String?,
    val originalTitle: String,
    val year: Int,
    val episodes: Int,
    val rating: Double?,
    val genres: List<String>,
    val status: String,
    val description: String,
    val posterUrl: String?,
    val bannerUrl: String?,
    val kind: String?,
    val ageRating: String?,
    val nextEpisodeAt: String?,
    val episodesAired: Int?,
    val airedOn: String?,
    val studios: List<String>,
    val youtubeTrailerId: String?,
    val kodikAvailable: Boolean?
)
