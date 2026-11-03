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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NewsUiState(
    val articles: List<NewsArticleEntity> = emptyList(),
    val selectedCategory: NewsCategory = NewsCategory.ALL,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class NewsViewModel @Inject constructor(
    private val repository: NewsRepository
) : ViewModel() {
    private val selectedCategory = MutableStateFlow(NewsCategory.ALL)
    private val isRefreshing = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)

    val uiState = combine(
        repository.observeNews(),
        selectedCategory,
        isRefreshing,
        errorMessage
    ) { articles: List<NewsArticleEntity>,
        category: NewsCategory,
        refreshing: Boolean,
        error: String? ->
        NewsUiState(
            articles = if (category == NewsCategory.ALL) articles else articles.filter { it.category == category.name },
            selectedCategory = category,
            isRefreshing = refreshing,
            errorMessage = error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NewsUiState())

    init { refresh() }

    fun selectCategory(category: NewsCategory) {
        selectedCategory.value = category
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
                errorMessage.value = "Не удалось обновить ленту. Сохранённые публикации остались доступны."
            } finally {
                isRefreshing.value = false
            }
        }
    }
}
