package com.yubera.app.features.news

import com.yubera.app.core.database.entity.NewsArticleEntity

fun List<NewsArticleEntity>.searchNews(query: String): List<NewsArticleEntity> {
    val normalized = query.trim().lowercase()
    if (normalized.isBlank()) return this
    return filter { article ->
        article.title.lowercase().contains(normalized) ||
            article.summary.lowercase().contains(normalized) ||
            article.linkedTitle?.lowercase()?.contains(normalized) == true ||
            article.authorName.lowercase().contains(normalized)
    }
}
