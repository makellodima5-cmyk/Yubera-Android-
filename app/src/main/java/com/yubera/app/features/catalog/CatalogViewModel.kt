package com.yubera.app.features.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yubera.app.core.catalog.AnimeCatalogItem
import com.yubera.app.data.catalog.RemoteCatalogRepository
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
    val kinds: List<String> = emptyList(),
    val ageRatings: List<String> = emptyList(),
    val ratingMin: Double = 0.0,
    val ratingMax: Double = 10.0,
    val query: String = "",
    val filters: CatalogFilters = CatalogFilters(),
    val draftFilters: CatalogFilters = CatalogFilters(),
    val sortOrder: CatalogSortOrder = CatalogSortOrder.CATALOG,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class CatalogViewModel @Inject constructor(private val repository: RemoteCatalogRepository) : ViewModel() {
    private val query = MutableStateFlow("")
    private val appliedFilters = MutableStateFlow(CatalogFilters())
    private val draftFilters = MutableStateFlow(CatalogFilters())
    private val sortOrder = MutableStateFlow(CatalogSortOrder.CATALOG)
    private val isRefreshing = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)

    val uiState = combine(repository.observeCatalog(), query, appliedFilters, draftFilters, sortOrder, isRefreshing, errorMessage) { values ->
        @Suppress("UNCHECKED_CAST")
        val catalog=values[0] as List<AnimeCatalogItem>
        val q=values[1] as String
        val filters=values[2] as CatalogFilters
        val draft=values[3] as CatalogFilters
        val order=values[4] as CatalogSortOrder
        val refreshing=values[5] as Boolean
        val error=values[6] as String?
        CatalogUiState(
            items=if(q.isBlank()) catalog.filterForCatalog(q,filters).sortedForCatalog(order) else catalog.filterForCatalog(q,filters).sortedWith(compareBy<AnimeCatalogItem>{searchPriority(it,q).coerceAtMost(2)}.thenByDescending{it.rating ?: 0.0}),
            genres=catalog.flatMap{it.genres}.distinct().sorted(),
            years=catalog.map{it.year}.filter{it>0}.distinct().sortedDescending(),
            statuses=catalog.map{it.status}.filter{it.isNotBlank()}.distinct().sorted(),
            kinds=catalog.mapNotNull{it.kind}.filter{it.isNotBlank()}.distinct().sorted(),
            ageRatings=catalog.mapNotNull{it.ageRating}.filter{it.isNotBlank()}.distinct().sorted(),
            ratingMin=catalog.mapNotNull{it.rating}.minOrNull() ?: 0.0,
            ratingMax=catalog.mapNotNull{it.rating}.maxOrNull() ?: 10.0,
            query=q, filters=filters, draftFilters=draft, sortOrder=order,
            isRefreshing=refreshing,errorMessage=error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CatalogUiState())

    init { refreshIfStale() }
    fun onQueryChange(value:String) { query.value=value }
    fun resetCatalog() { query.value=""; appliedFilters.value=CatalogFilters(); draftFilters.value=CatalogFilters(); sortOrder.value=CatalogSortOrder.CATALOG }
    fun onGenreSelected(value:String?) { draftFilters.value=draftFilters.value.copy(genres=value?.let(::setOf) ?: emptySet()); applyFilters() }
    fun onYearSelected(value:Int?) { draftFilters.value=draftFilters.value.copy(yearFrom=value,yearTo=value); applyFilters() }
    fun onStatusSelected(value:String?) { draftFilters.value=draftFilters.value.copy(status=value); applyFilters() }
    fun onSortOrderSelected(value:CatalogSortOrder) { sortOrder.value=value }
    fun updateDraft(value:CatalogFilters) { draftFilters.value=value }
    fun resetDraft() { draftFilters.value=CatalogFilters() }\n    fun resetFilters() { appliedFilters.value=CatalogFilters(); draftFilters.value=CatalogFilters() }\n    fun resetFilters() { appliedFilters.value=CatalogFilters(); draftFilters.value=CatalogFilters() }
    fun applyFilters() { appliedFilters.value=draftFilters.value }
    fun refresh()=refreshWith(true)
    private fun refreshIfStale()=refreshWith(false)
    private fun refreshWith(force:Boolean) {
        if(isRefreshing.value)return
        viewModelScope.launch {
            isRefreshing.value=true; errorMessage.value=null
            try { if(force) repository.refresh() else repository.refreshIfStale() }
            catch(cancelled:CancellationException){throw cancelled}
            catch(_:Exception){errorMessage.value="Не удалось обновить каталог. Проверьте подключение к интернету."}
            finally{isRefreshing.value=false}
        }
    }
}
