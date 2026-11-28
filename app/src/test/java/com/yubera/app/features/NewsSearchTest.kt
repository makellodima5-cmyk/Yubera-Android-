package com.yubera.app.features

import com.yubera.app.core.database.entity.NewsArticleEntity
import com.yubera.app.core.news.NewsCategory
import com.yubera.app.features.news.chooseFeaturedNews
import com.yubera.app.features.news.filterNews
import com.yubera.app.features.news.searchNews
import java.time.LocalDate
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
    fun featuredSelectionPrefersFreshSubstantiveStoryWithMedia() {
        val date = LocalDate.now().toString()
        val plain = article(4, "Короткая новость", "", null, "ALL").copy(createdAt = date)
        val rich = article(5, "Студия анонсировала новый проект", "Подробности о новом аниме-проекте и участниках производства.", null, "ANNOUNCEMENTS")
            .copy(
                createdAt = date,
                imageUrl = "https://cdn.example.com/news.jpg",
                bodyText = "Подробный текст публикации о новом аниме-проекте. ".repeat(8)
            )

        assertEquals(5L, chooseFeaturedNews(listOf(plain, rich))?.id)
    }

    @Test
    fun staleArticleWithoutSupportingContentIsNotForcedAsFeatured() {
        val stale = article(6, "Коротко", "", null, "ALL").copy(
            createdAt = LocalDate.now().minusDays(180).toString(),
            imageUrl = null,
            videoUrl = null,
            bodyText = null
        )
        assertEquals(null, chooseFeaturedNews(listOf(stale)))
    }

    @Test
    fun bookmarksModeShowsOnlySavedArticlesAndKeepsSearchAndCategoryFilters() {
        val result = filterNews(
            feed = articles,
            saved = listOf(articles[1], articles[2]),
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
