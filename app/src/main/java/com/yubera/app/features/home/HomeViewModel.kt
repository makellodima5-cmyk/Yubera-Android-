package com.yubera.app.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yubera.app.core.catalog.AnimeCatalogItem
import com.yubera.app.core.catalog.CatalogRepository
import com.yubera.app.core.database.entity.AnimeProgressEntity
import com.yubera.app.core.domain.library.PersonalLibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
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
    val recommended: List<AnimeCatalogItem> = emptyList(),
    val popular: List<AnimeCatalogItem> = emptyList(),
    val newReleases: List<AnimeCatalogItem> = emptyList(),
    val announcements: List<AnimeCatalogItem> = emptyList(),
    val ongoing: List<AnimeCatalogItem> = emptyList(),
    val randomPick: AnimeCatalogItem? = null,
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
    private val recommendationIds = MutableStateFlow<Set<String>>(emptySet())
    private var recommendationsUpdatedAt = 0L

    val uiState = combine(
        catalogRepository.observeCatalog(),
        libraryRepository.progress,
        libraryRepository.favorites,
        libraryRepository.watchLater,
        libraryRepository.subscriptions,
        recommendationIds,
        refreshState,
        errorState
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val catalog = values[0] as List<AnimeCatalogItem>
        @Suppress("UNCHECKED_CAST")
        val progress = values[1] as List<AnimeProgressEntity>
        @Suppress("UNCHECKED_CAST")
        val favorites = values[2] as List<com.yubera.app.core.database.entity.AnimeLibraryEntity>
        @Suppress("UNCHECKED_CAST")
        val watchLater = values[3] as List<com.yubera.app.core.database.entity.AnimeLibraryEntity>
        @Suppress("UNCHECKED_CAST")
        val subscriptions = values[4] as List<com.yubera.app.core.database.entity.AnimeSubscriptionEntity>
        @Suppress("UNCHECKED_CAST")
        val stableRecommendations = values[5] as Set<String>
        val refreshing = values[6] as Boolean
        val error = values[7] as String?

        val byId = catalog.associateBy(AnimeCatalogItem::id)
        val now = System.currentTimeMillis()
        val completedIds = progress.filter { row ->
            row.durationMillis > 0L && row.positionMillis >= row.durationMillis * 0.90
        }.mapTo(mutableSetOf()) { it.animeId }
        val viewedIds = progress.filter { it.episodeNumber != null }.mapTo(mutableSetOf()) { it.animeId }
        val watchedGenres = progress.filter { it.episodeNumber != null }
            .flatMap { byId[it.animeId]?.genres.orEmpty() }
            .groupingBy { it.lowercase() }.eachCount()
        val eligible = catalog.filter { it.id !in completedIds }
        val computedRecommendations = if (watchedGenres.isEmpty()) {
            eligible.sortedWith(compareByDescending<AnimeCatalogItem> { it.rating ?: 0.0 }.thenBy { it.title }).take(5)
        } else {
            eligible.sortedWith(
                compareByDescending<AnimeCatalogItem> { anime ->
                    anime.genres.sumOf { watchedGenres[it.lowercase()] ?: 0 } * 10.0 + (anime.rating ?: 0.0)
                }.thenBy { it.title }
            ).take(5)
        }

        if (stableRecommendations.isEmpty() || now - recommendationsUpdatedAt >= RECOMMENDATION_REFRESH_MILLIS) {
            recommendationsUpdatedAt = now
            recommendationIds.value = computedRecommendations.mapTo(linkedSetOf()) { it.id }
        }
        val selectedIds = if (now - recommendationsUpdatedAt < RECOMMENDATION_REFRESH_MILLIS) {
            recommendationIds.value.ifEmpty { computedRecommendations.mapTo(linkedSetOf()) { it.id } }
        } else {
            computedRecommendations.mapTo(linkedSetOf()) { it.id }
        }
        val selectedRecommendations = selectedIds.mapNotNull(byId::get)
        val continueList = progress.asSequence()
            .filter { it.episodeNumber != null && (it.durationMillis <= 0L || it.positionMillis < it.durationMillis * 0.90) }
            .mapNotNull { row -> byId[row.animeId]?.let { ContinueWatchingItem(it, row) } }
            .sortedByDescending { it.progress.updatedAtEpochMillis }
            .toList()
        val popular = catalog.filter { (it.rating ?: 0.0) > 8.5 }
            .sortedWith(compareByDescending<AnimeCatalogItem> { it.rating ?: 0.0 }.thenBy { it.title }).take(20)
        val yearAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -365) }.get(Calendar.YEAR)
        val newReleases = catalog.filter { it.year >= yearAgo && it.year <= Calendar.getInstance().get(Calendar.YEAR) }
            .sortedWith(compareByDescending<AnimeCatalogItem> { it.year }.thenByDescending { it.rating ?: 0.0 }).take(20)
        val announcements = catalog.filter { it.status.contains("анонс", true) || it.status.contains("upcoming", true) }
            .take(12)
        val ongoing = catalog.filter { it.status.contains("ongoing", true) || it.status.contains("онгоинг", true) || it.status.contains("выходит", true) }
            .take(20)
        val randomPool = catalog.filter { it.kodikAvailable != false }
        HomeUiState(
            continueWatching = continueList,
            recommended = selectedRecommendations,
            popular = popular,
            newReleases = newReleases,
            announcements = announcements,
            ongoing = ongoing,
            randomPick = randomPool.randomOrNull(),
            isRefreshing = refreshing,
            errorMessage = error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            refreshState.value = true
            errorState.value = null
            try {
                catalogRepository.refresh()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                errorState.value = "Не удалось обновить каталог. Сохранённые данные доступны офлайн."
            } finally {
                refreshState.value = false
            }
        }
    }

    private companion object {
        const val RECOMMENDATION_REFRESH_MILLIS = 24L * 60L * 60L * 1_000L
    }
}
