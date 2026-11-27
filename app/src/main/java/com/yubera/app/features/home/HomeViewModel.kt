package com.yubera.app.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yubera.app.core.catalog.AnimeCatalogItem
import com.yubera.app.core.catalog.CatalogRepository
import com.yubera.app.core.database.entity.AnimeProgressEntity
import com.yubera.app.core.domain.library.PersonalLibraryRepository
import com.yubera.app.data.settings.UserSettingsRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
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
    val catalogue: List<AnimeCatalogItem> = emptyList(),
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
        val completedEpisodeIds = progress.filter { row ->
            val anime = byId[row.animeId] ?: return@filter false
            val episode = row.episodeNumber ?: return@filter false
            val aired = anime.episodesAired ?: anime.episodes
            aired > 0 && episode >= aired && row.durationMillis > 0L &&
                row.positionMillis >= (row.durationMillis * 0.90).toLong()
        }.mapTo(mutableSetOf()) { it.animeId }
        val watchedGenres = progress.filter { row ->
            val anime = byId[row.animeId] ?: return@filter false
            val episode = row.episodeNumber ?: return@filter false
            val aired = anime.episodesAired ?: anime.episodes
            aired > 0 && episode <= aired && row.durationMillis > 0L &&
                row.positionMillis >= (row.durationMillis * 0.90).toLong()
        }.flatMap { byId[it.animeId]?.genres.orEmpty() }
            .groupingBy { it.lowercase(Locale.ROOT) }.eachCount()
        val eligible = catalog.filter { it.id !in completedEpisodeIds && it.kodikAvailable != false }
        val recommendationPool = if (watchedGenres.isEmpty()) {
            eligible.sortedWith(compareByDescending<AnimeCatalogItem> { it.rating ?: 0.0 }.thenBy { it.title }).take(20)
        } else {
            eligible.sortedWith(
                compareByDescending<AnimeCatalogItem> { anime ->
                    anime.genres.sumOf { watchedGenres[it.lowercase(Locale.ROOT)] ?: 0 } * 10.0 + (anime.rating ?: 0.0)
                }.thenBy { it.title }
            ).take(20)
        }
        val dailySeed = LocalDate.now().toEpochDay()
        val rotation = if (recommendationPool.isEmpty()) 0 else Math.floorMod(dailySeed, recommendationPool.size.toLong()).toInt()
        val rotatedPool = recommendationPool.drop(rotation) + recommendationPool.take(rotation)
        val computedRecommendations = rotatedPool.take(5)

        val storedRecommendationIds = settings.homeRecommendationIds.filter { id ->
            byId[id] != null && id !in completedEpisodeIds && byId[id]?.kodikAvailable != false
        }
        val hasFreshRecommendationSnapshot = storedRecommendationIds.isNotEmpty() &&
            settings.homeRecommendationsUpdatedAtEpochMillis > 0L &&
            now >= settings.homeRecommendationsUpdatedAtEpochMillis &&
            now - settings.homeRecommendationsUpdatedAtEpochMillis < RECOMMENDATION_REFRESH_MILLIS
        val selectedIds = if (hasFreshRecommendationSnapshot) storedRecommendationIds else computedRecommendations.map { it.id }
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
        val selectedRecommendations = selectedIds.mapNotNull(byId::get).filter { it.id !in completedEpisodeIds && it.kodikAvailable != false }
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
            val release = anime.airedOn?.toReleaseDate()
            release != null && !release.isBefore(earliestReleaseDate) && !release.isAfter(today)
        }.sortedWith(compareByDescending<AnimeCatalogItem> { it.airedOn }.thenByDescending { it.rating ?: 0.0 }).take(20)
        val announcements = catalog.filter { anime ->
            val status = anime.status.lowercase(Locale.ROOT)
            status.contains("анонс") || status.contains("upcoming") || status.contains("not yet aired") ||
                (anime.airedOn?.toReleaseDate()?.isAfter(today) == true)
        }.take(12)
        val ongoing = catalog.filter { anime ->
            val status = anime.status.lowercase(Locale.ROOT)
            status.contains("ongoing") || status.contains("онгоинг") || status.contains("выходит") ||
                (anime.nextEpisodeAt?.isNotBlank() == true && (anime.episodesAired ?: 0) < anime.episodes)
        }.take(20)
        HomeUiState(
            continueWatching = continueList,
            recommended = selectedRecommendations,
            popular = popular,
            newReleases = newReleases,
            announcements = announcements,
            ongoing = ongoing,
            catalogue = catalog,
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
                catalogRepository.refreshIfStale()
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

private fun String.toReleaseDate(): LocalDate? {
    val value = trim()
    runCatching { LocalDate.parse(value.take(10)) }.getOrNull()?.let { return it }
    val formats = listOf(
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss", Locale.ROOT),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX", Locale.ROOT)
    )
    return formats.firstNotNullOfOrNull { formatter ->
        runCatching { LocalDateTime.parse(value, formatter).toLocalDate() }.getOrNull()
    }
}
