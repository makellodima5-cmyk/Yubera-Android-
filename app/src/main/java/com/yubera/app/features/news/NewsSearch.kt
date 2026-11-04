package com.yubera.app.features.news

import com.yubera.app.core.database.entity.NewsArticleEntity
import com.yubera.app.core.news.NewsCategory

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

fun filterNews(
    feedArticles: List<NewsArticleEntity>,
    bookmarkedArticles: List<NewsArticleEntity>,
    query: String,
    category: NewsCategory,
    bookmarksOnly: Boolean
): List<NewsArticleEntity> {
    val source = if (bookmarksOnly) bookmarkedArticles else feedArticles
    val categorized = if (category == NewsCategory.ALL) {
        source
    } else {
        source.filter { it.category == category.name }
    }
    return categorized.searchNews(query)
}
