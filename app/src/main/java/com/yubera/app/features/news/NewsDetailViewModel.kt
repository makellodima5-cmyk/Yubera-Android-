package com.yubera.app.features.news

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yubera.app.core.news.NewsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NewsDetailUiState(
    val article: com.yubera.app.core.database.entity.NewsArticleEntity? = null,
    val isBookmarked: Boolean = false,
    val isLoadingDetails: Boolean = false,
    val detailError: String? = null
)

@HiltViewModel
class NewsDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: NewsRepository
) : ViewModel() {
    private val articleId = savedStateHandle.get<String>("articleId")?.toLongOrNull() ?: -1L
    private val loading = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    val uiState = combine(
        repository.observeNewsArticle(articleId),
        repository.observeBookmarkedIds(),
        loading,
        error
    ) { article, ids, isLoading, message ->
        NewsDetailUiState(article, articleId in ids, isLoading, message)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NewsDetailUiState())

    init { loadDetails(force = false) }

    fun loadDetails(force: Boolean) {
        if (articleId <= 0L || loading.value) return
        viewModelScope.launch {
            loading.value = true
            error.value = null
            try {
                repository.refreshArticle(articleId, force)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                error.value = "Полный текст пока недоступен. Сохранённая версия остаётся доступна."
            } finally {
                loading.value = false
            }
        }
    }

    fun toggleBookmark() {
        if (articleId <= 0L) return
        viewModelScope.launch { repository.setBookmarked(articleId, !uiState.value.isBookmarked) }
    }
}
