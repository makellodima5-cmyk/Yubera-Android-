package com.yubera.app.features.my

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yubera.app.core.catalog.AnimeCatalogItem
import com.yubera.app.core.catalog.CatalogRepository
import com.yubera.app.core.database.entity.AnimeProgressEntity
import com.yubera.app.core.domain.library.PersonalLibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class MyLibraryItem(
    val anime: AnimeCatalogItem,
    val progress: AnimeProgressEntity? = null
)

data class MyUiState(
    val favorites: List<MyLibraryItem> = emptyList(),
    val watchLater: List<MyLibraryItem> = emptyList(),
    val history: List<MyLibraryItem> = emptyList(),
    val subscriptions: List<MyLibraryItem> = emptyList()
)

@HiltViewModel
class MyViewModel @Inject constructor(
    catalogRepository: CatalogRepository,
    libraryRepository: PersonalLibraryRepository
) : ViewModel() {
    val uiState = combine(
        catalogRepository.observeCatalog(),
        libraryRepository.favorites,
        libraryRepository.watchLater,
        libraryRepository.progress,
        libraryRepository.subscriptions
    ) { catalog, favorites, watchLater, progress, subscriptions ->
        val byId = catalog.associateBy(AnimeCatalogItem::id)
        fun items(ids: List<String>) = ids.distinct().mapNotNull { id ->
            byId[id]?.let { anime -> MyLibraryItem(anime) }
        }
        MyUiState(
            favorites = items(favorites.filter { it.isFavorite }.map { it.animeId }),
            watchLater = items(watchLater.filter { it.isInWatchLater }.map { it.animeId }),
            history = progress.mapNotNull { item ->
                byId[item.animeId]?.let { MyLibraryItem(it, item) }
            },
            subscriptions = items(subscriptions.filter { it.notificationsEnabled }.map { it.animeId })
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MyUiState())
}
