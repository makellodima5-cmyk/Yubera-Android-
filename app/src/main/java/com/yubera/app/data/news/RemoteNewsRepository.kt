package com.yubera.app.data.news

import com.yubera.app.core.database.dao.NewsDao
import com.yubera.app.core.database.entity.NewsArticleEntity
import com.yubera.app.core.news.NewsCategory
import com.yubera.app.core.news.NewsRepository
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class RemoteNewsRepository @Inject constructor(
    private val api: ShikimoriNewsApi,
    private val newsDao: NewsDao
) : NewsRepository {
    override fun observeNews(): Flow<List<NewsArticleEntity>> = newsDao.observeAll()

    override suspend fun refresh() = withContext(Dispatchers.IO) {
        val topics = api.getNews(page = 1, limit = PAGE_SIZE)
        val articles = topics
            .filter { it.id > 0 && it.topicTitle.isNotBlank() }
            .distinctBy { it.id }
            .map { topic ->
                val summary = plainText(topic.body.orEmpty()).take(MAX_SUMMARY_LENGTH)
                NewsArticleEntity(
                    id = topic.id,
                    title = topic.topicTitle.trim(),
                    summary = summary,
                    createdAt = topic.createdAt,
                    commentsCount = topic.commentsCount?.coerceAtLeast(0) ?: 0,
                    authorName = topic.user?.nickname.orEmpty(),
                    linkedTitle = topic.linked?.russian?.takeIf(String::isNotBlank)
                        ?: topic.linked?.name?.takeIf(String::isNotBlank),
                    category = NewsCategory.classify(topic.topicTitle, summary).name,
                    sourceUrl = "https://shikimori.io/forum/news/${topic.id}"
                )
            }
        if (articles.isNotEmpty()) newsDao.replaceAll(articles)
    }

    private fun plainText(value: String): String = value
        .replace(Regex("<[^>]*>"), " ")
        .replace(Regex("\\[[^\\]]*\\]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    companion object {
        const val PAGE_SIZE = 30
        const val MAX_SUMMARY_LENGTH = 420
    }
}
