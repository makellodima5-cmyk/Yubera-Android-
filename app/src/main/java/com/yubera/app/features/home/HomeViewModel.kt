package com.yubera.app.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yubera.app.core.catalog.AnimeCatalogItem
import com.yubera.app.core.catalog.CatalogRepository
import com.yubera.app.core.database.entity.AnimeProgressEntity
import com.yubera.app.core.domain.library.PersonalLibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ContinueWatchingItem(val anime: AnimeCatalogItem, val progress: AnimeProgressEntity)

data class HomeUiState(
    val continueWatching: List<ContinueWatchingItem> = emptyList(),
    val cataloguePicks: List<AnimeCatalogItem> = emptyList(),
    val topRatedPicks: List<AnimeCatalogItem> = emptyList(),
    val newPicks: List<AnimeCatalogItem> = emptyList(),
    val announcedPicks: List<AnimeCatalogItem> = emptyList(),
    val ongoingPicks: List<AnimeCatalogItem> = emptyList(),
    val recommendedPicks: List<AnimeCatalogItem> = emptyList(),
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val catalogRepository: CatalogRepository,
    libraryRepository: PersonalLibraryRepository
) : ViewModel() {
    private val refreshState = MutableStateFlow(false)
    private val errorState = MutableStateFlow<String?>(null)

    val uiState = combine(
        catalogRepository.observeCatalog(),
        libraryRepository.progress,
        refreshState,
        errorState
    ) { catalog: List<AnimeCatalogItem>,
        progress: List<AnimeProgressEntity>,
        refreshing: Boolean,
        error: String? ->
        val byId = catalog.associateBy(AnimeCatalogItem::id)
        val newThreshold = java.time.LocalDate.now().minusDays(365).year
        val completed = progress.filter { entry ->
            val anime = byId[entry.animeId]
            anime != null && anime.episodes > 0 && entry.episodeNumber != null &&
                entry.episodeNumber >= anime.episodes &&
                (entry.durationMillis <= 0L || entry.positionMillis >= entry.durationMillis * 9 / 10)
        }.map { it.animeId }.toSet()

        val continueItems = progress
            .filterNot { it.animeId in completed }
            .mapNotNull { entry -> byId[entry.animeId]?.let { ContinueWatchingItem(it, entry) } }
            .sortedByDescending { it.progress.updatedAt }

        val available = catalog.filterNot { it.id in completed }
        val watchedGenres = progress.mapNotNull { byId[it.animeId] }.flatMap { it.genres }.toSet()

        HomeUiState(
            continueWatching = continueItems,
            cataloguePicks = catalog.take(20),
            topRatedPicks = catalog.filter { (it.rating ?: 0.0) > 8.5 }
                .sortedWith(compareByDescending<AnimeCatalogItem> { it.rating }.thenBy { it.title }).take(20),
            newPicks = catalog.filter { it.year > 0 && it.year >= newThreshold }
                .sortedWith(compareByDescending<AnimeCatalogItem> { it.year }.thenByDescending { it.rating ?: 0.0 }).take(20),
            announcedPicks = catalog.filter {
                it.status.contains("анонс", true) || it.status.contains("not yet", true) ||
                    it.status.contains("upcoming", true)
            }.take(20),
            ongoingPicks = catalog.filter {
                it.status.contains("ongoing", true) || it.status.contains("выходит", true) ||
                    it.status.contains("онгоинг", true) || it.status.contains("airing", true)
            }.take(20),
            recommendedPicks = if (watchedGenres.isEmpty()) available.take(5) else
                available.sortedWith(
                    compareByDescending<AnimeCatalogItem> { item -> item.genres.count { it in watchedGenres } }
                        .thenByDescending { it.rating ?: 0.0 }
                ).take(5),
            isRefreshing = refreshing,
            errorMessage = error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init { refresh() }

    fun refresh() {
        if (refreshState.value) return
        viewModelScope.launch {
            refreshState.value = true
            errorState.value = null
            try {
                catalogRepository.refresh()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                errorState.value = "Не удалось обновить каталог. Доступные сохранённые данные остаются на устройстве."
            } finally {
                refreshState.value = false
            }
        }
    }
}
