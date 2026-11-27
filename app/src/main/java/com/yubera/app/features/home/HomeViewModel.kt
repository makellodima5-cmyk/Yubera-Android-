package com.yubera.app.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yubera.app.core.catalog.AnimeCatalogItem
import com.yubera.app.core.catalog.CatalogRepository
import com.yubera.app.core.database.entity.AnimeProgressEntity
import com.yubera.app.core.domain.library.PersonalLibraryRepository
import com.yubera.app.data.settings.UserSettingsRepository
import java.time.LocalDate
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
    libraryRepository: PersonalLibraryRepository,
    settingsRepository: UserSettingsRepository
) : ViewModel() {
    private val refreshState = MutableStateFlow(false)
    private val errorState = MutableStateFlow<String?>(null)
    private var recommendationWriteInFlight = false

    val uiState = combine(
        catalogRepository.observeCatalog(),
        libraryRepository.progress,
        settingsRepository.settings,
        refreshState,
        errorState
    ) { catalog: List<AnimeCatalogItem>,
        progress: List<AnimeProgressEntity>,
        settings: com.yubera.app.data.settings.UserSettings,
        refreshing: Boolean,
        error: String? ->

        val byId = catalog.associateBy(AnimeCatalogItem::id)
        val now = System.currentTimeMillis()
        val completedIds = progress.mapNotNull { row ->
            val anime = byId[row.animeId] ?: return@mapNotNull null
            val isLastEpisode = anime.episodes > 0 && row.episodeNumber != null && row.episodeNumber >= anime.episodes
            anime.id.takeIf { isLastEpisode && row.durationMillis > 0L && row.positionMillis >= row.durationMillis * 0.90 }
        }.toSet()
        val watchedGenres = progress.filter { it.episodeNumber != null && it.durationMillis > 0L && it.positionMillis >= it.durationMillis * 0.90 }
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

        val hasFreshRecommendationSnapshot = settings.homeRecommendationIds.isNotEmpty() &&
            settings.homeRecommendationsUpdatedAtEpochMillis > 0L &&
            now >= settings.homeRecommendationsUpdatedAtEpochMillis &&
            now - settings.homeRecommendationsUpdatedAtEpochMillis < RECOMMENDATION_REFRESH_MILLIS
        val selectedIds = if (hasFreshRecommendationSnapshot) {
            settings.homeRecommendationIds
        } else {
            computedRecommendations.map { it.id }
        }
        if (!hasFreshRecommendationSnapshot && !recommendationWriteInFlight) {
            recommendationWriteInFlight = true
            viewModelScope.launch {
                try {
                    settingsRepository.saveHomeRecommendations(selectedIds, now)
                } finally {
                    recommendationWriteInFlight = false
                }
            }
        }
        val selectedRecommendations = selectedIds.mapNotNull(byId::get)
        val continueList = progress.asSequence()
            .filter { it.episodeNumber != null && (it.durationMillis <= 0L || it.positionMillis < it.durationMillis * 0.90) }
            .mapNotNull { row -> byId[row.animeId]?.let { ContinueWatchingItem(it, row) } }
            .sortedByDescending { it.progress.updatedAtEpochMillis }
            .toList()
        val popular = catalog.filter { (it.rating ?: 0.0) > 8.5 }
            .sortedWith(compareByDescending<AnimeCatalogItem> { it.rating ?: 0.0 }.thenBy { it.title }).take(20)
        val today = LocalDate.now()
        val earliestReleaseDate = today.minusDays(365)
        val newReleases = catalog.filter { anime ->
            val airedDate = anime.airedOn?.take(10)?.let { raw ->
                runCatching { LocalDate.parse(raw) }.getOrNull()
            }
            airedDate != null && !airedDate.isBefore(earliestReleaseDate) && !airedDate.isAfter(today)
        }.sortedWith(compareByDescending<AnimeCatalogItem> { it.airedOn }.thenByDescending { it.rating ?: 0.0 }).take(20)
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
