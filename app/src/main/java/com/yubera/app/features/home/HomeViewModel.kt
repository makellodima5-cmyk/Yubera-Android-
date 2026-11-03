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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ContinueWatchingItem(
    val anime: AnimeCatalogItem,
    val progress: AnimeProgressEntity
)

data class HomeUiState(
    val continueWatching: List<ContinueWatchingItem> = emptyList(),
    val cataloguePicks: List<AnimeCatalogItem> = emptyList(),
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    catalogRepository: CatalogRepository,
    libraryRepository: PersonalLibraryRepository
) : ViewModel() {
    private val refreshState = kotlinx.coroutines.flow.MutableStateFlow(false)
    private val errorState = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    val uiState = combine(
        catalogRepository.observeCatalog(),
        libraryRepository.progress,
        refreshState,
        errorState
    ) { catalog, progress, refreshing, error ->
        val byId = catalog.associateBy(AnimeCatalogItem::id)
        HomeUiState(
            continueWatching = progress.mapNotNull { item ->
                byId[item.animeId]?.let { ContinueWatchingItem(it, item) }
            },
            cataloguePicks = catalog.take(12),
            isRefreshing = refreshing,
            errorMessage = error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        refresh(catalogRepository)
    }

    private fun refresh(repository: CatalogRepository) {
        viewModelScope.launch {
            refreshState.value = true
            errorState.value = null
            try {
                repository.refresh()
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
