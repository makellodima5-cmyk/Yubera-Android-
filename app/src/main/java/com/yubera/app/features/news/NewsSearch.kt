package com.yubera.app.features.news

import com.yubera.app.core.database.entity.NewsArticleEntity
import com.yubera.app.core.news.NewsCategory
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

fun List<NewsArticleEntity>.searchNews(query: String): List<NewsArticleEntity> {
    val q = query.trim().lowercase()
    if (q.isBlank()) return this
    return filter {
        it.title.lowercase().contains(q) ||
            it.summary.lowercase().contains(q) ||
            it.bodyText.orEmpty().lowercase().contains(q) ||
            it.linkedTitle?.lowercase()?.contains(q) == true ||
            it.authorName.lowercase().contains(q) ||
            it.category.lowercase().contains(q)
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

/**
 * Selects a featured item only when local metadata provides enough evidence:
 * freshness, a substantive title/description, and useful media/category data.
 * Returns null when a normal first row is safer than inventing a "main" story.
 */
fun chooseFeaturedNews(articles: List<NewsArticleEntity>): NewsArticleEntity? {
    val best = articles.maxWithOrNull(
        compareBy<NewsArticleEntity> { featuredNewsScore(it) }
            .thenBy { publishedAtMillis(it.createdAt) ?: Long.MIN_VALUE }
    ) ?: return null
    return best.takeIf { featuredNewsScore(it) >= FEATURED_SCORE_THRESHOLD }
}

private fun featuredNewsScore(article: NewsArticleEntity): Int {
    val publishedAt = publishedAtMillis(article.createdAt)
    val ageDays = publishedAt?.let { ((System.currentTimeMillis() - it) / DAY_MILLIS).coerceAtLeast(0L) }
    val freshness = when {
        ageDays == null -> 0
        ageDays <= 1L -> 50
        ageDays <= 3L -> 42
        ageDays <= 7L -> 32
        ageDays <= 30L -> 18
        ageDays <= 90L -> 6
        else -> 0
    }
    val titleLength = article.title.trim().length
    val titleScore = when {
        titleLength >= 18 -> 10
        titleLength >= 10 -> 4
        else -> 0
    }
    val summaryLength = article.summary.trim().length
    val summaryScore = when {
        summaryLength >= 80 -> 10
        summaryLength >= 30 -> 5
        else -> 0
    }
    val bodyScore = if (article.bodyText.orEmpty().length >= 200) 8 else 0
    val imageScore = if (!article.imageUrl.isNullOrBlank()) 20 else 0
    val videoScore = if (!article.videoUrl.isNullOrBlank()) 5 else 0
    val categoryScore = if (article.category != NewsCategory.ALL.name) 8 else 0
    return freshness + titleScore + summaryScore + bodyScore + imageScore + videoScore + categoryScore
}

private fun publishedAtMillis(value: String): Long? {
    val normalized = value.trim()
    if (normalized.isEmpty()) return null
    return runCatching { Instant.parse(normalized).toEpochMilli() }.getOrNull()
        ?: runCatching {
            LocalDate.parse(normalized.take(10)).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        }.getOrNull()
}

private const val DAY_MILLIS = 24L * 60L * 60L * 1_000L
private const val FEATURED_SCORE_THRESHOLD = 55
