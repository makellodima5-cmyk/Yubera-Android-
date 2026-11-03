package com.yubera.app.features.anime

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yubera.app.core.catalog.AnimeCatalogItem
import com.yubera.app.core.catalog.CatalogRepository
import com.yubera.app.core.database.entity.AnimeLibraryEntity
import com.yubera.app.core.domain.library.PersonalLibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AnimeDetailUiState(
    val anime: AnimeCatalogItem? = null,
    val isFavorite: Boolean = false,
    val isInWatchLater: Boolean = false,
    val isSubscribed: Boolean = false,
    val isLoading: Boolean = true
)

@HiltViewModel
class AnimeDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val catalogRepository: CatalogRepository,
    private val libraryRepository: PersonalLibraryRepository
) : ViewModel() {
    private val animeId: String = savedStateHandle["animeId"].orEmpty()

    val uiState = combine(
        catalogRepository.observeAnime(animeId),
        libraryRepository.favorites,
        libraryRepository.watchLater,
        libraryRepository.subscriptions
    ) { anime, favorites, watchLater, subscriptions ->
        AnimeDetailUiState(
            anime = anime,
            isFavorite = favorites.any { it.animeId == animeId && it.isFavorite },
            isInWatchLater = watchLater.any { it.animeId == animeId && it.isInWatchLater },
            isSubscribed = subscriptions.any { it.animeId == animeId && it.notificationsEnabled },
            isLoading = anime == null
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnimeDetailUiState())

    fun toggleFavorite(enabled: Boolean) {
        viewModelScope.launch {
            libraryRepository.setFavorite(animeId, enabled, System.currentTimeMillis())
        }
    }

    fun toggleWatchLater(enabled: Boolean) {
        viewModelScope.launch {
            libraryRepository.setWatchLater(animeId, enabled, System.currentTimeMillis())
        }
    }

    fun toggleSubscription(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled) {
                libraryRepository.subscribeToAnime(animeId, true, System.currentTimeMillis())
            } else {
                libraryRepository.removeSubscription(animeId)
            }
        }
    }
}
