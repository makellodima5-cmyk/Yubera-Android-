package com.yubera.app.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yubera.app.core.catalog.AnimeCatalogItem
import com.yubera.app.core.catalog.CatalogRepository
import com.yubera.app.core.database.entity.AnimeProgressEntity
import com.yubera.app.core.database.dao.AnimeEpisodeNotificationDao
import com.yubera.app.core.session.ActiveAccountStore
import com.yubera.app.core.domain.library.PersonalLibraryRepository
import com.yubera.app.core.domain.library.EpisodeCompletionPolicy
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
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
    val errorMessage: String? = null,
    val unreadNotificationCount: Int = 0
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val catalogRepository: CatalogRepository,
    libraryRepository: PersonalLibraryRepository,
    settingsRepository: UserSettingsRepository,
    private val notificationDao: AnimeEpisodeNotificationDao,
    private val activeAccountStore: ActiveAccountStore
) : ViewModel() {
    private val refreshState = MutableStateFlow(false)
    private val errorState = MutableStateFlow<String?>(null)
    private var recommendationWriteInFlight = false

    private val unreadNotifications = activeAccountStore.activeAccountId.flatMapLatest { accountId ->
        if (accountId.isNullOrBlank()) flowOf(0) else notificationDao.observeUnreadDeliveredCount(accountId)
    }

    val uiState = combine(
        catalogRepository.observeCatalog(),
        libraryRepository.progress,
        settingsRepository.settings,
        combine(refreshState, errorState) { refreshing, error -> refreshing to error },
        unreadNotifications
    ) { catalog: List<AnimeCatalogItem>,
        progress: List<AnimeProgressEntity>,
        settings: com.yubera.app.data.settings.UserSettings,
        refreshAndError: Pair<Boolean, String?>,
        unreadCount: Int ->
        val (refreshing, error) = refreshAndError

        val byId = catalog.associateBy(AnimeCatalogItem::id)
        val now = System.currentTimeMillis()
        val fullyCompletedAnimeIds = HomeRecommendationPolicy.fullyCompletedAnimeIds(progress, catalog)
        val computedRecommendations = HomeRecommendationPolicy.selectRecommendations(
            catalog = catalog,
            progress = progress,
            dailySeed = LocalDate.now().toEpochDay()
        )

        val cachedRecommendationIds = settings.homeRecommendationIds
        val storedRecommendationIds = cachedRecommendationIds.filter { id ->
            byId[id] != null && id !in fullyCompletedAnimeIds && byId[id]?.kodikAvailable != false
        }
        val snapshotInvalidatedByCompletedTitle = storedRecommendationIds.size != cachedRecommendationIds.size
        val hasFreshRecommendationSnapshot = !snapshotInvalidatedByCompletedTitle && storedRecommendationIds.isNotEmpty() &&
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
        val selectedRecommendations = selectedIds.mapNotNull(byId::get).filter { it.id !in fullyCompletedAnimeIds && it.kodikAvailable != false }
        val continueList = progress.asSequence()
            .mapNotNull { row ->
                byId[row.animeId]
                    ?.takeIf { anime -> isContinueWatchingEligible(anime, row) }
                    ?.let { anime -> ContinueWatchingItem(anime, row) }
            }
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
            errorMessage = error,
            unreadNotificationCount = unreadCount
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init { refresh() }

    fun markNotificationsRead() {
        val accountId = activeAccountStore.currentAccountId() ?: return
        viewModelScope.launch { notificationDao.markAllDeliveredAsRead(accountId, System.currentTimeMillis()) }
    }

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


internal fun isContinueWatchingEligible(anime: AnimeCatalogItem, progress: AnimeProgressEntity): Boolean {
    if (progress.episodeNumber == null) return false
    val status = anime.status.lowercase(Locale.ROOT)
    val ongoing = listOf("ongoing", "airing", "онгоинг", "выходит", "анонс", "upcoming", "planned", "not yet aired")
        .any(status::contains)
    if (!ongoing && anime.episodes > 0 && (progress.completedEpisodeNumber ?: 0) >= anime.episodes) return false
    return progress.durationMillis <= 0L || progress.positionMillis < progress.durationMillis * 0.90
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
