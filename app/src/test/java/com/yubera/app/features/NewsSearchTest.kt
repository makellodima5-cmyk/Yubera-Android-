package com.yubera.app.features

import com.yubera.app.core.database.entity.NewsArticleEntity
import com.yubera.app.core.news.NewsCategory
import com.yubera.app.features.news.filterNews
import com.yubera.app.features.news.searchNews
import org.junit.Assert.assertEquals
import org.junit.Test

class NewsSearchTest {
    private val articles = listOf(
        article(1, "Новый сезон аниме", "Продолжение истории", "Фэнтези", "NEW_SEASONS"),
        article(2, "Интервью с режиссёром", "О работе над сериалом", "Авторское кино", "INTERVIEWS"),
        article(3, "Фестиваль анимации", "Программа и участники", null, "EVENTS")
    )

    @Test
    fun blankQueryReturnsAllArticles() {
        assertEquals(listOf(1L, 2L, 3L), articles.searchNews("  ").map { it.id })
    }

    @Test
    fun searchMatchesTitleSummaryAndLinkedTitleCaseInsensitively() {
        assertEquals(listOf(1L), articles.searchNews("НОВЫЙ").map { it.id })
        assertEquals(listOf(2L), articles.searchNews("режиссёром").map { it.id })
        assertEquals(listOf(1L), articles.searchNews("Фэнтези").map { it.id })
    }

    @Test
    fun searchMatchesAuthorName() {
        assertEquals(listOf(2L), articles.searchNews("Автор 2").map { it.id })
    }

    @Test
    fun bookmarksModeShowsOnlySavedArticlesAndKeepsSearchAndCategoryFilters() {
        val result = filterNews(
            feedArticles = articles,
            bookmarkedArticles = listOf(articles[1], articles[2]),
            query = "фестиваль",
            category = NewsCategory.EVENTS,
            bookmarksOnly = true
        )

        assertEquals(listOf(3L), result.map { it.id })
    }

    private fun article(
        id: Long,
        title: String,
        summary: String,
        linkedTitle: String?,
        category: String
    ) = NewsArticleEntity(
        id = id,
        title = title,
        summary = summary,
        createdAt = "2026-11-04",
        commentsCount = 0,
        authorName = if (id == 2L) "Автор 2" else "Автор",
        linkedTitle = linkedTitle,
        category = category,
        sourceUrl = "https://example.com/$id"
    )
}
