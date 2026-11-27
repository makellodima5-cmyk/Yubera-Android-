package com.yubera.app.data.anime

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface AnimeSocialApi {
    @GET("api/anime-ratings")
    suspend fun getRating(
        @Query("animeId") animeId: String,
        @Header("Authorization") authorization: String? = null
    ): AnimeRatingResponse

    @POST("api/anime-ratings")
    suspend fun saveRating(
        @Body request: SaveAnimeRatingRequest,
        @Header("Authorization") authorization: String
    ): AnimeRatingResponse

    @GET("api/anime-comments")
    suspend fun getComments(
        @Query("animeId") animeId: String,
        @Query("cursor") cursor: String? = null,
        @Query("threadId") threadId: String? = null,
        @Header("Authorization") authorization: String? = null
    ): AnimeCommentsPageResponse

    @GET("api/anime-comments")
    suspend fun getReplyPage(
        @Query("animeId") animeId: String,
        @Query("threadId") threadId: String,
        @Query("cursor") cursor: String? = null,
        @Header("Authorization") authorization: String? = null
    ): AnimeCommentReplyPageResponse

    @POST("api/anime-comments")
    suspend fun mutateComment(
        @Body request: AnimeCommentMutationRequest,
        @Header("Authorization") authorization: String
    ): AnimeCommentMutationResponse

    @HTTP(method = "DELETE", path = "api/anime-comments", hasBody = true)
    suspend fun deleteComment(
        @Body request: DeleteAnimeCommentRequest,
        @Header("Authorization") authorization: String
    ): DeleteAnimeCommentResponse
}

@Serializable
data class AnimeRatingResponse(
    val animeId: String = "",
    val averageRating: Double? = null,
    val voteCount: Int = 0,
    val myRating: Int? = null,
    val authenticated: Boolean = false,
    val error: String? = null
)

@Serializable
data class SaveAnimeRatingRequest(
    val animeId: String,
    val rating: Int
)

@Serializable
data class AnimeCommentDto(
    val id: String,
    val animeId: String,
    val parentId: String? = null,
    val threadRootId: String = "",
    val replyToUserId: String? = null,
    val replyToName: String? = null,
    val body: String = "",
    val likeCount: Int = 0,
    val createdAt: String = "",
    val deleted: Boolean = false,
    val authorId: String? = null,
    val authorName: String = "",
    val avatarUrl: String? = null,
    val avatarFrame: String? = null,
    val liked: Boolean = false
)

@Serializable
data class AnimeCommentThreadDto(
    val id: String,
    val animeId: String,
    val parentId: String? = null,
    val threadRootId: String = "",
    val replyToUserId: String? = null,
    val replyToName: String? = null,
    val body: String = "",
    val likeCount: Int = 0,
    val createdAt: String = "",
    val deleted: Boolean = false,
    val authorId: String? = null,
    val authorName: String = "",
    val avatarUrl: String? = null,
    val avatarFrame: String? = null,
    val liked: Boolean = false,
    val replies: List<AnimeCommentDto> = emptyList(),
    val replyCount: Int = 0,
    val hasMoreReplies: Boolean = false,
    val replyCursor: String? = null
)

@Serializable
data class AnimeCommentsPageResponse(
    val comments: List<AnimeCommentThreadDto> = emptyList(),
    val nextCursor: String? = null,
    val hasMore: Boolean = false,
    val totalCount: Int = 0,
    val viewer: AnimeCommentViewer? = null,
    val error: String? = null
)

@Serializable
data class AnimeCommentViewer(
    val id: String = "",
    val name: String = "",
    val username: String? = null,
    val avatarUrl: String? = null
)

@Serializable
data class AnimeCommentReplyPageResponse(
    val replies: List<AnimeCommentDto> = emptyList(),
    val nextCursor: String? = null,
    val hasMore: Boolean = false,
    val error: String? = null
)

@Serializable
data class AnimeCommentMutationRequest(
    val action: String,
    val animeId: String,
    val text: String? = null,
    val idempotencyKey: String? = null,
    val parentId: String? = null,
    val commentId: String? = null,
    val liked: Boolean? = null
)

@Serializable
data class AnimeCommentMutationResponse(
    val comment: AnimeCommentDto? = null,
    val totalCount: Int? = null,
    val created: Boolean = false,
    val commentId: String? = null,
    val liked: Boolean? = null,
    val likeCount: Int? = null,
    val error: String? = null
)

@Serializable
data class DeleteAnimeCommentRequest(
    val animeId: String,
    val commentId: String
)

@Serializable
data class DeleteAnimeCommentResponse(
    val deleted: Boolean = false,
    val tombstoned: Boolean = false,
    val commentId: String? = null,
    val totalCount: Int? = null,
    val error: String? = null
)
