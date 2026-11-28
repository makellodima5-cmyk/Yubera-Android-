package com.yubera.app.features.news

import com.yubera.app.core.database.entity.NewsArticleEntity
import com.yubera.app.core.news.NewsCategory

fun List<NewsArticleEntity>.searchNews(query: String): List<NewsArticleEntity> {
    val q = query.trim().lowercase()
    if (q.isBlank()) return this
    return filter { article ->
        article.title.lowercase().contains(q) ||
            article.summary.lowercase().contains(q) ||
            article.bodyText.orEmpty().lowercase().contains(q) ||
            article.linkedTitle?.lowercase()?.contains(q) == true ||
            article.authorName.lowercase().contains(q) ||
            article.category.lowercase().contains(q)
    }
}

fun filterNews(
    feed: List<NewsArticleEntity>,
    saved: List<NewsArticleEntity>,
    query: String,
    category: NewsCategory,
    bookmarksOnly: Boolean
): List<NewsArticleEntity> {
    val source = if (bookmarksOnly) saved else feed
    return (if (category == NewsCategory.ALL) source else source.filter { it.category == category.name })
        .searchNews(query)
}
