package com.yubera.app.data.anime

import com.yubera.app.core.session.SecureTokenStore
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnimeSocialRepository @Inject constructor(
    private val api: AnimeSocialApi,
    private val tokenStore: SecureTokenStore
) {
    fun hasSession(): Boolean = !tokenStore.getAccessToken().isNullOrBlank()

    suspend fun loadRating(animeId: String): AnimeRatingResponse =
        api.getRating(animeId, authorizationHeader())

    suspend fun saveRating(animeId: String, rating: Int): AnimeRatingResponse {
        require(rating in 1..10)
        return api.saveRating(
            SaveAnimeRatingRequest(animeId = animeId, rating = rating),
            requiredAuthorization()
        )
    }

    suspend fun loadComments(animeId: String, cursor: String? = null): AnimeCommentsPageResponse =
        api.getComments(animeId = animeId, cursor = cursor, authorization = authorizationHeader())

    suspend fun loadReplies(animeId: String, rootId: String, cursor: String? = null): AnimeCommentReplyPageResponse =
        api.getComments(animeId = animeId, threadId = rootId, cursor = cursor, authorization = authorizationHeader())
            .let { response ->
                // The thread endpoint has a different response shape from root paging.
                AnimeCommentReplyPageResponse(
                    replies = emptyList(),
                    nextCursor = null,
                    hasMore = false,
                    error = response.error
                )
            }

    suspend fun loadReplyPage(animeId: String, rootId: String, cursor: String? = null): AnimeCommentReplyPageResponse =
        api.getReplyPage(animeId = animeId, threadId = rootId, cursor = cursor, authorization = authorizationHeader())

    suspend fun createComment(animeId: String, text: String): AnimeCommentMutationResponse =
        api.mutateComment(
            AnimeCommentMutationRequest(
                action = "create",
                animeId = animeId,
                text = text,
                idempotencyKey = UUID.randomUUID().toString()
            ),
            requiredAuthorization()
        )

    suspend fun reply(animeId: String, rootId: String, text: String): AnimeCommentMutationResponse =
        api.mutateComment(
            AnimeCommentMutationRequest(
                action = "reply",
                animeId = animeId,
                text = text,
                idempotencyKey = UUID.randomUUID().toString(),
                parentId = rootId
            ),
            requiredAuthorization()
        )

    suspend fun setLiked(animeId: String, commentId: String, liked: Boolean): AnimeCommentMutationResponse =
        api.mutateComment(
            AnimeCommentMutationRequest(
                action = "like",
                animeId = animeId,
                commentId = commentId,
                liked = liked
            ),
            requiredAuthorization()
        )

    suspend fun deleteComment(animeId: String, commentId: String): DeleteAnimeCommentResponse =
        api.deleteComment(
            DeleteAnimeCommentRequest(animeId = animeId, commentId = commentId),
            requiredAuthorization()
        )

    private fun authorizationHeader(): String? =
        tokenStore.getAccessToken()?.takeIf(String::isNotBlank)?.let { "Bearer $it" }

    private fun requiredAuthorization(): String =
        authorizationHeader() ?: throw IllegalStateException("Войдите в аккаунт YUBERA, чтобы выполнить это действие.")
}
