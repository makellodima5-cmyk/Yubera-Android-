package com.yubera.app.features.anime

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yubera.app.core.catalog.AnimeCatalogItem
import com.yubera.app.core.catalog.CatalogRepository
import com.yubera.app.core.database.entity.AnimeLibraryEntity
import com.yubera.app.core.database.entity.AnimeSubscriptionEntity
import com.yubera.app.core.domain.library.PersonalLibraryRepository
import com.yubera.app.core.player.KodikRepository
import com.yubera.app.core.player.KodikSource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AnimeDetailUiState(
    val anime: AnimeCatalogItem? = null,
    val isFavorite: Boolean = false,
    val isInWatchLater: Boolean = false,
    val isSubscribed: Boolean = false,
    val isLoading: Boolean = true,
    val localRating: Int? = null,
    val localComments: List<LocalTitleComment> = emptyList(),
    val availableSources: List<KodikSource> = emptyList(),
    val sourcesLoading: Boolean = false
)

@HiltViewModel
class AnimeDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val catalogRepository: CatalogRepository,
    private val libraryRepository: PersonalLibraryRepository,
    private val titleLocalRepository: TitleLocalRepository,
    private val kodikRepository: KodikRepository
) : ViewModel() {
    private val animeId: String = savedStateHandle.get<String>("animeId").orEmpty()
    private val sourcesState = MutableStateFlow<List<KodikSource>>(emptyList())
    private val sourcesLoadingState = MutableStateFlow(false)

    val uiState = combine(
        catalogRepository.observeAnime(animeId),
        libraryRepository.favorites,
        libraryRepository.watchLater,
        libraryRepository.subscriptions,
        titleLocalRepository.observeRating(animeId),
        titleLocalRepository.observeComments(animeId),
        combine(sourcesState, sourcesLoadingState) { sources, loading -> sources to loading }
    ) { anime: AnimeCatalogItem?,
        favorites: List<AnimeLibraryEntity>,
        watchLater: List<AnimeLibraryEntity>,
        subscriptions: List<AnimeSubscriptionEntity>,
        rating: Int?,
        comments: List<LocalTitleComment>,
        sourcePair: Pair<List<KodikSource>, Boolean> ->
        AnimeDetailUiState(
            anime = anime,
            isFavorite = favorites.any { it.animeId == animeId && it.isFavorite },
            isInWatchLater = watchLater.any { it.animeId == animeId && it.isInWatchLater },
            isSubscribed = subscriptions.any { it.animeId == animeId },
            isLoading = false,
            localRating = rating,
            localComments = comments,
            availableSources = sourcePair.first,
            sourcesLoading = sourcePair.second
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnimeDetailUiState())

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
            } finally {
                sourcesLoadingState.value = false
            }
        }
    }

    fun saveRating(rating: Int) {
        viewModelScope.launch { titleLocalRepository.saveRating(animeId, rating) }
    }

    fun addComment(text: String) {
        viewModelScope.launch { titleLocalRepository.addComment(animeId, text) }
    }

    fun toggleCommentLike(commentId: String) {
        viewModelScope.launch { titleLocalRepository.toggleLike(animeId, commentId) }
    }

    fun deleteComment(commentId: String) {
        viewModelScope.launch { titleLocalRepository.deleteComment(animeId, commentId) }
    }
}
