package com.yubera.app.features.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yubera.app.core.catalog.AnimeCatalogItem
import com.yubera.app.core.catalog.CatalogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CatalogUiState(
    val items: List<AnimeCatalogItem> = emptyList(),
    val genres: List<String> = emptyList(),
    val query: String = "",
    val selectedGenre: String? = null,
    val sortOrder: CatalogSortOrder = CatalogSortOrder.CATALOG,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class CatalogViewModel @Inject constructor(
    private val repository: CatalogRepository
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val selectedGenre = MutableStateFlow<String?>(null)
    private val sortOrder = MutableStateFlow(CatalogSortOrder.CATALOG)
    private val isRefreshing = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)

    val uiState = combine(
        repository.observeCatalog(),
        query,
        selectedGenre,
        sortOrder,
        isRefreshing,
        errorMessage
    ) { catalog, search, genre, order, refreshing, error ->
        val normalizedQuery = search.trim().lowercase()
        val filtered = catalog.filter { anime ->
            val matchesQuery = normalizedQuery.isBlank() ||
                anime.title.lowercase().contains(normalizedQuery) ||
                (anime.englishTitle?.lowercase()?.contains(normalizedQuery) == true) ||
                anime.originalTitle.lowercase().contains(normalizedQuery)
            val matchesGenre = genre == null || genre in anime.genres
            matchesQuery && matchesGenre
        }
        CatalogUiState(
            items = filtered.sortedForCatalog(order),
            genres = catalog.flatMap { it.genres }.distinct().sorted(),
            query = search,
            selectedGenre = genre,
            sortOrder = order,
            isRefreshing = refreshing,
            errorMessage = error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CatalogUiState())

    init {
        refresh()
    }

    fun onQueryChange(value: String) { query.value = value }
    fun onGenreSelected(value: String?) { selectedGenre.value = value }
    fun onSortOrderSelected(value: CatalogSortOrder) { sortOrder.value = value }

    fun refresh() {
        if (isRefreshing.value) return
        viewModelScope.launch {
            isRefreshing.value = true
            errorMessage.value = null
            try {
                repository.refresh()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                errorMessage.value = "Не удалось обновить каталог. Проверьте подключение к интернету."
            } finally {
                isRefreshing.value = false
            }
        }
    }
}
