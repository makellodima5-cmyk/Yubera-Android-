package com.yubera.app.features.news

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yubera.app.core.database.entity.NewsArticleEntity
import com.yubera.app.core.news.NewsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class NewsDetailUiState(val article: NewsArticleEntity? = null, val isBookmarked: Boolean = false, val loading: Boolean = false, val error: String? = null)
@HiltViewModel
class NewsDetailViewModel @Inject constructor(savedState: SavedStateHandle, private val repository: NewsRepository) : ViewModel() {
 private val id = savedState.get<String>("articleId")?.toLongOrNull() ?: -1L; private val loading = MutableStateFlow(false); private val error = MutableStateFlow<String?>(null)
 val uiState = combine(repository.observeNewsArticle(id), repository.observeBookmarkedIds(), loading, error) { article, ids, busy, message -> NewsDetailUiState(article, id in ids, busy, message) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NewsDetailUiState())
 init { if (id > 0L) viewModelScope.launch { loading.value = true; try { repository.refreshArticle(id) } catch (cancelled: CancellationException) { throw cancelled } catch (_: Exception) { error.value = "Полный текст недоступен. Сохранённая версия остаётся доступна." } finally { loading.value = false } } }
 fun toggleBookmark() { if (id > 0L) viewModelScope.launch { repository.setBookmarked(id, !uiState.value.isBookmarked) } }
}