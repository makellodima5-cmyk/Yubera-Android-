package com.yubera.app.data.news

import com.yubera.app.core.database.dao.NewsBookmarkDao
import com.yubera.app.core.database.dao.NewsDao
import com.yubera.app.core.database.entity.NewsArticleEntity
import com.yubera.app.core.database.entity.NewsBookmarkEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class RemoteNewsRepositoryTest {
    @Test
    fun freshCachedFullArticleDoesNotFetchAgain() = runBlocking {
        val now = System.currentTimeMillis()
        val cached = NewsArticleEntity(
            id = 42L,
            title = "Cached news",
            summary = "Cached summary",
            createdAt = "2026-11-28",
            commentsCount = 4,
            authorName = "Author",
            linkedTitle = "Anime",
            category = "INDUSTRY",
            sourceUrl = "https://shikimori.one/forum/news/42",
            bodyText = "Saved full article text",
            linkedAnimeId = 123L,
            lastFetchedAtEpochMillis = now
        )
        val api = FakeNewsApi()
        val dao = FakeNewsDao(cached)
        val repo = RemoteNewsRepository(api, dao, FakeBookmarkDao())

        repo.refreshArticle(42L)

        assertEquals(0, api.articleCalls)
        assertEquals("Saved full article text", dao.article.value?.bodyText)
    }
}

private class FakeNewsApi : ShikimoriNewsApi {
    var articleCalls = 0
    override suspend fun getNews(forum: String, type: String, page: Int, limit: Int) = emptyList<ShikimoriTopicDto>()
    override suspend fun getTopic(id: Long): ShikimoriTopicDto {
        articleCalls++
        return ShikimoriTopicDto(id = id, topicTitle = "Remote topic")
    }
}

private class FakeNewsDao(initial: NewsArticleEntity?) : NewsDao {
    val article = MutableStateFlow(initial)
    override fun observeFeed(): Flow<List<NewsArticleEntity>> = flowOf(article.value?.let(::listOf).orEmpty())
    override fun observeBookmarked(): Flow<List<NewsArticleEntity>> = flowOf(emptyList())
    override fun observeAll(): Flow<List<NewsArticleEntity>> = article.mapNullableList()
    override suspend fun getById(id: Long): NewsArticleEntity? = article.value?.takeIf { it.id == id }
    override suspend fun getLastFetchTime(): Long? = article.value?.lastFetchedAtEpochMillis
    override suspend fun insertAll(items: List<NewsArticleEntity>) { article.value = items.firstOrNull() }
    override suspend fun upsert(item: NewsArticleEntity) { article.value = item }
    override suspend fun deleteUnbookmarkedOutsideFeed(currentFeedIds: List<Long>) { }
}

private fun MutableStateFlow<NewsArticleEntity?>.mapNullableList(): Flow<List<NewsArticleEntity>> = map { it?.let(::listOf).orEmpty() }

private class FakeBookmarkDao : NewsBookmarkDao {
    override fun observeBookmarkedIds(): Flow<List<Long>> = flowOf(emptyList())
    override suspend fun save(bookmark: NewsBookmarkEntity) { }
    override suspend fun remove(articleId: Long) { }
}
