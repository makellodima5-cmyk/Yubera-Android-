package com.yubera.app.features.news
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yubera.app.core.database.entity.NewsArticleEntity
import com.yubera.app.core.news.NewsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
data class NewsDetailUiState(val article: NewsArticleEntity? = null, val isBookmarked: Boolean = false)
@HiltViewModel
class NewsDetailViewModel @Inject constructor(savedStateHandle: SavedStateHandle, private val repository: NewsRepository) : ViewModel() {
 private val articleId = savedStateHandle.get<String>("articleId")?.toLongOrNull() ?: -1L
 val uiState = combine(repository.observeNewsArticle(articleId), repository.observeBookmarkedIds()) { article, ids -> NewsDetailUiState(article, articleId in ids) }
  .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NewsDetailUiState())
 fun toggleBookmark() { viewModelScope.launch { repository.setBookmarked(articleId, !uiState.value.isBookmarked) } }
}
