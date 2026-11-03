package com.yubera.app.core.news

import com.yubera.app.core.database.entity.NewsArticleEntity
import kotlinx.coroutines.flow.Flow

interface NewsRepository {
    fun observeNews(): Flow<List<NewsArticleEntity>>
    suspend fun refresh()
}
