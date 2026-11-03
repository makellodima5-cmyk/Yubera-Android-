package com.yubera.app.data.news
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST
interface ShikimoriEpisodeUpdatesApi {
 @POST("api/graphql")
 suspend fun getAnimeUpdates(@Body request: ShikimoriGraphQlRequest): ShikimoriGraphQlResponse
}
@Serializable data class ShikimoriGraphQlRequest(val query: String)
@Serializable data class ShikimoriGraphQlResponse(val data: ShikimoriGraphQlData? = null, val errors: List<ShikimoriGraphQlError> = emptyList())
@Serializable data class ShikimoriGraphQlError(val message: String = "Unknown GraphQL error")
@Serializable data class ShikimoriGraphQlData(val animes: List<ShikimoriAnimeUpdate> = emptyList())
@Serializable data class ShikimoriAnimeUpdate(
 val id: String,
 val name: String = "",
 val russian: String? = null,
 val episodesAired: Int? = null
) {
 val displayName: String get() = russian?.takeIf { it.isNotBlank() } ?: name
}
