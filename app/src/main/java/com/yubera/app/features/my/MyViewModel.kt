package com.yubera.app.features.my
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yubera.app.core.catalog.AnimeCatalogItem
import com.yubera.app.core.catalog.CatalogRepository
import com.yubera.app.core.database.dao.AnimeEpisodeNotificationDao
import com.yubera.app.core.database.entity.AnimeLibraryEntity
import com.yubera.app.core.database.entity.AnimeProgressEntity
import com.yubera.app.core.database.entity.AnimeSubscriptionEntity
import com.yubera.app.core.domain.library.PersonalLibraryRepository
import com.yubera.app.core.session.ActiveAccountStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MyLibraryItem(val anime: AnimeCatalogItem, val progress: AnimeProgressEntity? = null)
data class MyUiState(
    val favorites: List<MyLibraryItem> = emptyList(),
    val watchLater: List<MyLibraryItem> = emptyList(),
    val history: List<MyLibraryItem> = emptyList(),
    val subscriptions: List<MyLibraryItem> = emptyList(),
    val unreadNotificationCount: Int = 0
)

@HiltViewModel
class MyViewModel @Inject constructor(
    catalogRepository: CatalogRepository,
    private val libraryRepository: PersonalLibraryRepository,
    private val notificationDao: AnimeEpisodeNotificationDao,
    private val activeAccountStore: ActiveAccountStore
) : ViewModel() {
    private val unread = activeAccountStore.activeAccountId.flatMapLatest { accountId ->
        if (accountId.isNullOrBlank()) flowOf(0) else notificationDao.observeUnreadDeliveredCount(accountId)
    }

    private val libraryState = combine(
        catalogRepository.observeCatalog(),
        libraryRepository.favorites,
        libraryRepository.watchLater,
        libraryRepository.progress,
        libraryRepository.subscriptions
    ) { catalog, favorites, watch, progress, subs ->
        val byId = catalog.associateBy(AnimeCatalogItem::id)
        fun mapped(ids: List<String>) = ids.distinct().mapNotNull { byId[it]?.let { anime -> MyLibraryItem(anime) } }
        MyUiState(
            favorites = mapped(favorites.filter { it.isFavorite }.sortedByDescending { it.favoriteUpdatedAtEpochMillis }.map { it.animeId }),
            watchLater = mapped(watch.filter { it.isInWatchLater }.sortedByDescending { it.watchLaterUpdatedAtEpochMillis }.map { it.animeId }),
            history = progress.sortedByDescending { it.updatedAtEpochMillis }.mapNotNull { row -> byId[row.animeId]?.let { MyLibraryItem(it, row) } },
            subscriptions = mapped(subs.filter { it.isSubscribed }.sortedByDescending { it.updatedAtEpochMillis }.map { it.animeId })
        )
    }

    val uiState = combine(libraryState, unread) { library, unreadCount ->
        library.copy(unreadNotificationCount = unreadCount)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MyUiState())

    fun removeFavorite(id: String) {
        viewModelScope.launch { libraryRepository.setFavorite(id, false, System.currentTimeMillis()) }
    }
}
