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

data class NewsUiState(val articles: List<NewsArticleEntity> = emptyList(), val bookmarkedIds: Set<Long> = emptySet(), val query: String = "", val selectedCategory: NewsCategory = NewsCategory.ALL, val isRefreshing: Boolean = false, val errorMessage: String? = null)

@HiltViewModel
class NewsViewModel @Inject constructor(private val repository: NewsRepository) : ViewModel() {
 private val query = MutableStateFlow("")
 private val selectedCategory = MutableStateFlow(NewsCategory.ALL)
 private val isRefreshing = MutableStateFlow(false)
 private val errorMessage = MutableStateFlow<String?>(null)
 val uiState = combine(repository.observeNews(), repository.observeBookmarkedIds(), query, selectedCategory, isRefreshing, errorMessage) { articles, savedIds, search, category, refreshing, error ->
  val categorized = if (category == NewsCategory.ALL) articles else articles.filter { it.category == category.name }
  NewsUiState(categorized.searchNews(search), savedIds.toSet(), search, category, refreshing, error)
 }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NewsUiState())
 init { refresh() }
 fun onQueryChange(value: String) { query.value = value }
 fun selectCategory(category: NewsCategory) { selectedCategory.value = category }
 fun toggleBookmark(id: Long) { viewModelScope.launch { repository.setBookmarked(id, id !in uiState.value.bookmarkedIds) } }
 fun refresh() {
  if (isRefreshing.value) return
  viewModelScope.launch {
   isRefreshing.value = true; errorMessage.value = null
   try { repository.refresh() }
   catch (cancelled: CancellationException) { throw cancelled }
   catch (_: Exception) { errorMessage.value = "Не удалось обновить ленту. Сохранённые публикации остались доступны." }
   finally { isRefreshing.value = false }
  }
 }
}
