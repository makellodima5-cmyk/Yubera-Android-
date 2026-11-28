package com.yubera.app.features.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yubera.app.core.database.entity.NewsArticleEntity
import com.yubera.app.core.news.NewsCategory
import com.yubera.app.core.news.NewsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class NewsUiState(val articles: List<NewsArticleEntity> = emptyList(), val bookmarkedIds: Set<Long> = emptySet(), val query: String = "", val selectedCategory: NewsCategory = NewsCategory.ALL, val showBookmarkedOnly: Boolean = false, val isRefreshing: Boolean = false, val errorMessage: String? = null)
private data class FeedState(val articles: List<NewsArticleEntity>, val query: String, val category: NewsCategory, val savedOnly: Boolean)
@HiltViewModel
class NewsViewModel @Inject constructor(private val repository: NewsRepository) : ViewModel() {
 private val query = MutableStateFlow(""); private val category = MutableStateFlow(NewsCategory.ALL); private val savedOnly = MutableStateFlow(false); private val refreshing = MutableStateFlow(false); private val error = MutableStateFlow<String?>(null); private var loaded = false
 private val feed = combine(repository.observeNews(), repository.observeBookmarkedNews(), query, category, savedOnly) { all, saved, q, c, only -> FeedState(filterNews(all, saved, q, c, only), q, c, only) }
 val uiState = combine(feed, repository.observeBookmarkedIds(), refreshing, error) { f, ids, busy, message -> NewsUiState(f.articles, ids.toSet(), f.query, f.category, f.savedOnly, busy, message) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NewsUiState())
 fun ensureLoaded() { if (!loaded) { loaded = true; refresh() } }
 fun onQueryChange(value: String) { query.value = value }
 fun selectCategory(value: NewsCategory) { category.value = value }
 fun showBookmarkedOnly(value: Boolean) { savedOnly.value = value }
 fun toggleBookmark(id: Long) { viewModelScope.launch { repository.setBookmarked(id, id !in uiState.value.bookmarkedIds) } }
 fun refresh() { if (refreshing.value) return; viewModelScope.launch { refreshing.value = true; error.value = null; try { repository.refresh() } catch (cancelled: CancellationException) { throw cancelled } catch (_: Exception) { error.value = "Не удалось обновить ленту. Сохранённые публикации остались доступны." } finally { refreshing.value = false } } }
}