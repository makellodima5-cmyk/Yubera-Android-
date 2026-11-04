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
    val years: List<Int> = emptyList(),
    val statuses: List<String> = emptyList(),
    val query: String = "",
    val selectedGenre: String? = null,
    val selectedYear: Int? = null,
    val selectedStatus: String? = null,
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
    private val selectedYear = MutableStateFlow<Int?>(null)
    private val selectedStatus = MutableStateFlow<String?>(null)
    private val sortOrder = MutableStateFlow(CatalogSortOrder.CATALOG)
    private val isRefreshing = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)

    val uiState = combine(
        repository.observeCatalog(),
        query,
        selectedGenre,
        selectedYear,
        selectedStatus,
        sortOrder,
        isRefreshing,
        errorMessage
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val catalog = values[0] as List<AnimeCatalogItem>
        val search = values[1] as String
        val genre = values[2] as String?
        val year = values[3] as Int?
        val status = values[4] as String?
        val order = values[5] as CatalogSortOrder
        val refreshing = values[6] as Boolean
        val error = values[7] as String?

        val filtered = catalog.filterForCatalog(search, genre, year, status)
        CatalogUiState(
            items = filtered.sortedForCatalog(order),
            genres = catalog.flatMap { it.genres }.distinct().sorted(),
            years = catalog.map { it.year }.filter { it > 0 }.distinct().sortedDescending(),
            statuses = catalog.map { it.status }.filter { it.isNotBlank() }.distinct().sorted(),
            query = search,
            selectedGenre = genre,
            selectedYear = year,
            selectedStatus = status,
            sortOrder = order,
            isRefreshing = refreshing,
            errorMessage = error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CatalogUiState())

    init { refresh() }

    fun onQueryChange(value: String) { query.value = value }
    fun onGenreSelected(value: String?) { selectedGenre.value = value }
    fun onYearSelected(value: Int?) { selectedYear.value = value }
    fun onStatusSelected(value: String?) { selectedStatus.value = value }
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
