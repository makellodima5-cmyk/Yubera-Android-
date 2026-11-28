package com.yubera.app.features.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yubera.app.core.database.entity.NewsArticleEntity
import com.yubera.app.core.news.NewsCategory
import com.yubera.app.core.news.NewsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NewsUiState(
    val articles: List<NewsArticleEntity> = emptyList(),
    val bookmarkedIds: Set<Long> = emptySet(),
    val query: String = "",
    val selectedCategory: NewsCategory = NewsCategory.ALL,
    val showBookmarkedOnly: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)

private data class NewsContentState(
    val articles: List<NewsArticleEntity>,
    val query: String,
    val category: NewsCategory,
    val showBookmarkedOnly: Boolean
)

@HiltViewModel
class NewsViewModel @Inject constructor(
    private val repository: NewsRepository
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val selectedCategory = MutableStateFlow(NewsCategory.ALL)
    private val showBookmarkedOnly = MutableStateFlow(false)
    private val isRefreshing = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)

    private val content = combine(
        repository.observeNews(),
        repository.observeBookmarkedNews(),
        query,
        selectedCategory,
        showBookmarkedOnly
    ) { feed, saved, search, category, bookmarksOnly ->
        NewsContentState(
            articles = filterNews(feed, saved, search, category, bookmarksOnly),
            query = search,
            category = category,
            showBookmarkedOnly = bookmarksOnly
        )
    }

    val uiState: StateFlow<NewsUiState> = combine(
        content,
        repository.observeBookmarkedIds(),
        isRefreshing,
        errorMessage
    ) { current, savedIds, refreshing, error ->
        NewsUiState(
            articles = current.articles,
            bookmarkedIds = savedIds.toSet(),
            query = current.query,
            selectedCategory = current.category,
            showBookmarkedOnly = current.showBookmarkedOnly,
            isRefreshing = refreshing,
            errorMessage = error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NewsUiState())

    init {
        refresh()
    }

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun selectCategory(category: NewsCategory) {
        selectedCategory.value = category
    }

    fun showBookmarkedOnly(show: Boolean) {
        showBookmarkedOnly.value = show
    }

    fun toggleBookmark(id: Long) {
        viewModelScope.launch {
            repository.setBookmarked(id, id !in uiState.value.bookmarkedIds)
        }
    }

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
                errorMessage.value =
                    "Не удалось обновить ленту. Сохранённые публикации остались доступны."
            } finally {
                isRefreshing.value = false
            }
        }
    }
}
