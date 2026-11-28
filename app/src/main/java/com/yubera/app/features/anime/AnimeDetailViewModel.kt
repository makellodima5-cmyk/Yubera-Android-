package com.yubera.app.features.anime

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yubera.app.core.catalog.AnimeCatalogItem
import com.yubera.app.core.catalog.CatalogRepository
import com.yubera.app.core.database.entity.AnimeLibraryEntity
import com.yubera.app.core.database.entity.AnimeProgressEntity
import com.yubera.app.core.database.entity.AnimeSubscriptionEntity
import com.yubera.app.core.domain.library.PersonalLibraryRepository
import com.yubera.app.core.domain.library.EpisodeCompletionPolicy
import com.yubera.app.core.player.KodikRepository
import com.yubera.app.core.player.KodikLinkPolicy
import com.yubera.app.core.player.KodikSource
import com.yubera.app.data.anime.AnimeCommentDto
import com.yubera.app.data.anime.AnimeCommentReplyPageResponse
import com.yubera.app.data.anime.AnimeCommentThreadDto
import com.yubera.app.data.anime.AnimeRatingResponse
import com.yubera.app.data.anime.AnimeSocialRepository
import com.yubera.app.data.anime.ShikimoriRelatedRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AnimeSocialUiState(
    val rating: AnimeRatingResponse = AnimeRatingResponse(),
    val ratingLoading: Boolean = true,
    val ratingSaving: Boolean = false,
    val comments: List<AnimeCommentThreadDto> = emptyList(),
    val commentsCount: Int = 0,
    val commentsLoading: Boolean = true,
    val commentsHasMore: Boolean = false,
    val commentsNextCursor: String? = null,
    val commentsSubmitting: Boolean = false,
    val repliesLoadingFor: String? = null,
    val commentError: String? = null,
    val ratingError: String? = null,
    val viewer: String? = null,
    val hasLocalSession: Boolean = false
)

data class AnimeDetailUiState(
    val anime: AnimeCatalogItem? = null,
    val isFavorite: Boolean = false,
    val isInWatchLater: Boolean = false,
    val isSubscribed: Boolean = false,
    val isLoading: Boolean = true,
    val progress: AnimeProgressEntity? = null,
    val social: AnimeSocialUiState = AnimeSocialUiState(),
    val availableSources: List<KodikSource> = emptyList(),
    val sourcesLoading: Boolean = false
)

private data class AnimeDetailLibrarySnapshot(
    val favorites: List<AnimeLibraryEntity>,
    val watchLater: List<AnimeLibraryEntity>,
    val subscriptions: List<AnimeSubscriptionEntity>,
    val progress: List<AnimeProgressEntity>
)

@HiltViewModel
class AnimeDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val catalogRepository: CatalogRepository,
    private val libraryRepository: PersonalLibraryRepository,
    private val socialRepository: AnimeSocialRepository,
    private val kodikRepository: KodikRepository,
    private val relatedRepository: ShikimoriRelatedRepository
) : ViewModel() {
    private val animeId: String = savedStateHandle.get<String>("animeId").orEmpty()
    private val sourcesState = MutableStateFlow<List<KodikSource>>(emptyList())
    private val sourcesLoadingState = MutableStateFlow(false)
    private val relatedIdsState = MutableStateFlow<List<String>>(emptyList())
    private val socialState = MutableStateFlow(
        AnimeSocialUiState(hasLocalSession = socialRepository.hasSession())
    )

    private val libraryState = combine(
        libraryRepository.favorites,
        libraryRepository.watchLater,
        libraryRepository.subscriptions,
        libraryRepository.progress
    ) { favorites, watchLater, subscriptions, progress ->
        AnimeDetailLibrarySnapshot(favorites, watchLater, subscriptions, progress)
    }

    private val sourceState = combine(sourcesState, sourcesLoadingState) { sources, loading ->
        sources to loading
    }
    private val detailExtras = combine(sourceState, socialState) { sources, social ->
        sources to social
    }
    private val detailState = combine(libraryState, detailExtras) { library, extras ->
        library to extras
    }

    val uiState = combine(catalogRepository.observeAnime(animeId), detailState) { anime, detail ->
        val library = detail.first
        val (sources, social) = detail.second
        AnimeDetailUiState(
            anime = anime,
            isFavorite = library.favorites.any { it.animeId == animeId && it.isFavorite },
            isInWatchLater = library.watchLater.any { it.animeId == animeId && it.isInWatchLater },
            isSubscribed = library.subscriptions.any { it.animeId == animeId },
            isLoading = false,
            progress = library.progress.firstOrNull { it.animeId == animeId },
            social = social,
            availableSources = sources.first,
            sourcesLoading = sources.second
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnimeDetailUiState())

    val relatedAnime = combine(
        catalogRepository.observeAnime(animeId),
        catalogRepository.observeCatalog(),
        relatedIdsState
    ) { current, all, ids ->
        AnimeRelatedPolicy.select(current, all, ids)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        refreshSocial()
        loadRelatedAnimeIds()
    }

    private fun loadRelatedAnimeIds() {
        val id = animeId.toLongOrNull()?.takeIf { it > 0 } ?: return
        viewModelScope.launch {
            relatedIdsState.value = relatedRepository.getAnimeIds(id).map(Long::toString)
        }
    }

    fun recordEpisodeOpened(sourceId: String, episodeNumber: Int) {
        val source = uiState.value.availableSources.firstOrNull { it.id == sourceId } ?: return
        val episode = source.episodes.firstOrNull { it.number == episodeNumber } ?: return
        if (!KodikLinkPolicy.isTrustedHttpsUrl(episode.link)) return
        viewModelScope.launch {
            val previous = libraryRepository.progress.first().firstOrNull { it.animeId == animeId }
            if (previous?.episodeNumber == episodeNumber) return@launch
            val now = System.currentTimeMillis()
            val opened = AnimeProgressEntity(
                animeId = animeId,
                seasonNumber = previous?.seasonNumber,
                episodeNumber = episodeNumber,
                positionMillis = 0L,
                durationMillis = 0L,
                updatedAtEpochMillis = now,
                completedEpisodeNumber = previous?.completedEpisodeNumber,
                completedAtEpochMillis = previous?.completedAtEpochMillis
            )
            libraryRepository.saveProgress(EpisodeCompletionPolicy.markSequentialAdvance(opened, previous, episodeNumber, now))
        }
    }

    fun toggleFavorite(enabled: Boolean) {
        viewModelScope.launch { libraryRepository.setFavorite(animeId, enabled, System.currentTimeMillis()) }
    }

    fun toggleWatchLater(enabled: Boolean) {
        viewModelScope.launch { libraryRepository.setWatchLater(animeId, enabled, System.currentTimeMillis()) }
    }

    fun toggleSubscription(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled) libraryRepository.subscribeToAnime(animeId, true, System.currentTimeMillis())
            else libraryRepository.removeSubscription(animeId)
        }
    }

    fun loadSubscriptionSources() {
        if (sourcesState.value.isNotEmpty() || sourcesLoadingState.value) return
        viewModelScope.launch {
            sourcesLoadingState.value = true
            try {
                sourcesState.value = kodikRepository.getSources(animeId)
            } catch (_: Exception) {
                sourcesState.value = emptyList()
            } finally {
                sourcesLoadingState.value = false
            }
        }
    }

    fun refreshSocial() {
        if (animeId.isBlank()) return
        viewModelScope.launch {
            socialState.value = socialState.value.copy(
                hasLocalSession = socialRepository.hasSession(),
                ratingLoading = true,
                commentsLoading = true,
                ratingError = null,
                commentError = null
            )
            launch {
                try {
                    val rating = socialRepository.loadRating(animeId)
                    socialState.value = socialState.value.copy(
                        rating = rating,
                        ratingLoading = false,
                        ratingError = null
                    )
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    socialState.value = socialState.value.copy(
                        ratingLoading = false,
                        ratingError = "Не удалось загрузить оценки."
                    )
                }
            }
            launch { loadFirstComments() }
        }
    }

    private suspend fun loadFirstComments() {
        try {
            val page = socialRepository.loadComments(animeId)
            socialState.value = socialState.value.copy(
                comments = page.comments,
                commentsCount = page.totalCount.coerceAtLeast(0),
                commentsHasMore = page.hasMore,
                commentsNextCursor = page.nextCursor,
                commentsLoading = false,
                viewer = page.viewer?.id,
                commentError = null
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            socialState.value = socialState.value.copy(
                commentsLoading = false,
                commentError = "Не удалось загрузить комментарии."
            )
        }
    }

    fun retryComments() {
        if (socialState.value.commentsLoading) return
        viewModelScope.launch {
            socialState.value = socialState.value.copy(commentsLoading = true, commentError = null)
            loadFirstComments()
        }
    }

    fun loadMoreComments() {
        val current = socialState.value
        val cursor = current.commentsNextCursor ?: return
        if (!current.commentsHasMore || current.commentsLoading) return
        viewModelScope.launch {
            socialState.value = socialState.value.copy(commentsLoading = true, commentError = null)
            try {
                val page = socialRepository.loadComments(animeId, cursor)
                val existing = socialState.value.comments.mapTo(mutableSetOf()) { it.id }
                socialState.value = socialState.value.copy(
                    comments = socialState.value.comments + page.comments.filterNot { it.id in existing },
                    commentsCount = page.totalCount.coerceAtLeast(0),
                    commentsHasMore = page.hasMore,
                    commentsNextCursor = page.nextCursor,
                    commentsLoading = false
                )
            } catch (_: Exception) {
                socialState.value = socialState.value.copy(
                    commentsLoading = false,
                    commentError = "Не удалось загрузить следующие комментарии."
                )
            }
        }
    }

    fun loadMoreReplies(rootId: String) {
        val root = socialState.value.comments.firstOrNull { it.id == rootId } ?: return
        val cursor = root.replyCursor ?: return
        if (!root.hasMoreReplies || socialState.value.repliesLoadingFor != null) return
        viewModelScope.launch {
            socialState.value = socialState.value.copy(repliesLoadingFor = rootId, commentError = null)
            try {
                val page: AnimeCommentReplyPageResponse = socialRepository.loadReplyPage(animeId, rootId, cursor)
                updateRoot(rootId) { current ->
                    val known = current.replies.mapTo(mutableSetOf()) { it.id }
                    current.copy(
                        replies = current.replies + page.replies.filterNot { it.id in known },
                        replyCursor = page.nextCursor,
                        hasMoreReplies = page.hasMore,
                        replyCount = maxOf(current.replyCount, current.replies.size + page.replies.size)
                    )
                }
                socialState.value = socialState.value.copy(repliesLoadingFor = null)
            } catch (_: Exception) {
                socialState.value = socialState.value.copy(
                    repliesLoadingFor = null,
                    commentError = "Не удалось загрузить ответы."
                )
            }
        }
    }

    fun saveRating(rating: Int) {
        if (socialState.value.ratingSaving) return
        viewModelScope.launch {
            socialState.value = socialState.value.copy(ratingSaving = true, ratingError = null)
            try {
                val result = socialRepository.saveRating(animeId, rating)
                socialState.value = socialState.value.copy(rating = result, ratingSaving = false)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                socialState.value = socialState.value.copy(
                    ratingSaving = false,
                    ratingError = "Не удалось сохранить оценку. Проверьте вход в аккаунт."
                )
            }
        }
    }

    fun addComment(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || socialState.value.commentsSubmitting) return
        viewModelScope.launch {
            socialState.value = socialState.value.copy(commentsSubmitting = true, commentError = null)
            try {
                socialRepository.createComment(animeId, trimmed)
                val freshPage = socialRepository.loadComments(animeId)
                socialState.value = socialState.value.copy(
                    comments = freshPage.comments,
                    commentsCount = freshPage.totalCount.coerceAtLeast(0),
                    commentsHasMore = freshPage.hasMore,
                    commentsNextCursor = freshPage.nextCursor,
                    commentsSubmitting = false,
                    viewer = freshPage.viewer?.id
                )
            } catch (_: Exception) {
                socialState.value = socialState.value.copy(
                    commentsSubmitting = false,
                    commentError = "Не удалось опубликовать комментарий. Проверьте вход в аккаунт."
                )
            }
        }
    }

    fun replyTo(rootId: String, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || socialState.value.commentsSubmitting) return
        viewModelScope.launch {
            socialState.value = socialState.value.copy(commentsSubmitting = true, commentError = null)
            try {
                socialRepository.reply(animeId, rootId, trimmed)
                val freshPage = socialRepository.loadComments(animeId)
                socialState.value = socialState.value.copy(
                    comments = freshPage.comments,
                    commentsCount = freshPage.totalCount.coerceAtLeast(0),
                    commentsHasMore = freshPage.hasMore,
                    commentsNextCursor = freshPage.nextCursor,
                    commentsSubmitting = false
                )
            } catch (_: Exception) {
                socialState.value = socialState.value.copy(
                    commentsSubmitting = false,
                    commentError = "Не удалось отправить ответ. Проверьте вход в аккаунт."
                )
            }
        }
    }

    fun toggleCommentLike(comment: AnimeCommentDto) {
        if (!socialRepository.hasSession() || socialState.value.commentsSubmitting) return
        viewModelScope.launch {
            socialState.value = socialState.value.copy(commentError = null)
            try {
                val result = socialRepository.setLiked(animeId, comment.id, !comment.liked)
                updateComment(comment.id) { current ->
                    current.copy(
                        liked = result.liked ?: !current.liked,
                        likeCount = result.likeCount ?: current.likeCount
                    )
                }
            } catch (_: Exception) {
                socialState.value = socialState.value.copy(commentError = "Не удалось изменить отметку «Нравится».")
            }
        }
    }

    fun toggleRootLike(comment: AnimeCommentThreadDto) {
        if (!socialRepository.hasSession()) return
        viewModelScope.launch {
            try {
                val result = socialRepository.setLiked(animeId, comment.id, !comment.liked)
                updateRoot(comment.id) { current ->
                    current.copy(
                        liked = result.liked ?: !current.liked,
                        likeCount = result.likeCount ?: current.likeCount
                    )
                }
            } catch (_: Exception) {
                socialState.value = socialState.value.copy(commentError = "Не удалось изменить отметку «Нравится».")
            }
        }
    }

    fun deleteComment(commentId: String) {
        if (!socialRepository.hasSession()) return
        viewModelScope.launch {
            try {
                socialRepository.deleteComment(animeId, commentId)
                val page = socialRepository.loadComments(animeId)
                socialState.value = socialState.value.copy(
                    comments = page.comments,
                    commentsCount = page.totalCount.coerceAtLeast(0),
                    commentsHasMore = page.hasMore,
                    commentsNextCursor = page.nextCursor
                )
            } catch (_: Exception) {
                socialState.value = socialState.value.copy(commentError = "Не удалось удалить комментарий.")
            }
        }
    }

    private fun updateRoot(rootId: String, transform: (AnimeCommentThreadDto) -> AnimeCommentThreadDto) {
        socialState.value = socialState.value.copy(
            comments = socialState.value.comments.map { root -> if (root.id == rootId) transform(root) else root }
        )
    }

    private fun updateComment(commentId: String, transform: (AnimeCommentDto) -> AnimeCommentDto) {
        socialState.value = socialState.value.copy(
            comments = socialState.value.comments.map { root ->
                if (root.id == commentId) {
                    val updated = transform(root.toComment())
                    root.copy(liked = updated.liked, likeCount = updated.likeCount)
                } else {
                    root.copy(replies = root.replies.map { reply -> if (reply.id == commentId) transform(reply) else reply })
                }
            }
        )
    }

    private fun AnimeCommentThreadDto.toComment() = AnimeCommentDto(
        id = id,
        animeId = animeId,
        parentId = parentId,
        threadRootId = threadRootId,
        replyToUserId = replyToUserId,
        replyToName = replyToName,
        body = body,
        likeCount = likeCount,
        createdAt = createdAt,
        deleted = deleted,
        authorId = authorId,
        authorName = authorName,
        avatarUrl = avatarUrl,
        avatarFrame = avatarFrame,
        liked = liked
    )

    fun clearCommentError() {
        socialState.value = socialState.value.copy(commentError = null)
    }

    fun clearRatingError() {
        socialState.value = socialState.value.copy(ratingError = null)
    }


}
