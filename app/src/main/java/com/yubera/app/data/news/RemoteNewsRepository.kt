package com.yubera.app.data.news

import android.text.Html
import android.text.Spanned
import com.yubera.app.core.database.dao.NewsBookmarkDao
import com.yubera.app.core.database.dao.NewsDao
import com.yubera.app.core.database.entity.NewsArticleEntity
import com.yubera.app.core.database.entity.NewsBookmarkEntity
import com.yubera.app.core.news.NewsCategory
import com.yubera.app.core.news.NewsRepository
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class RemoteNewsRepository @Inject constructor(
    private val api: ShikimoriNewsApi,
    private val newsDao: NewsDao,
    private val bookmarkDao: NewsBookmarkDao
) : NewsRepository {
    override fun observeNews(): Flow<List<NewsArticleEntity>> = newsDao.observeFeed()
    override fun observeBookmarkedNews(): Flow<List<NewsArticleEntity>> = newsDao.observeBookmarked()
    override fun observeBookmarkedIds(): Flow<List<Long>> = bookmarkDao.observeBookmarkedIds()
    override fun observeNewsArticle(articleId: Long): Flow<NewsArticleEntity?> =
        newsDao.observeAll().map { rows -> rows.firstOrNull { it.id == articleId } }

    override suspend fun setBookmarked(articleId: Long, bookmarked: Boolean) = withContext(Dispatchers.IO) {
        if (bookmarked) bookmarkDao.save(NewsBookmarkEntity(articleId, System.currentTimeMillis()))
        else bookmarkDao.remove(articleId)
    }

    override suspend fun refresh() = withContext(Dispatchers.IO) {
        val lastFetch = newsDao.getLastFetchTime() ?: 0L
        val now = System.currentTimeMillis()
        if (now - lastFetch < FEED_CACHE_TTL_MILLIS) return@withContext
        val collected = linkedMapOf<Long, ShikimoriTopicDto>()
        for (page in 1..MAX_PAGES) {
            val topics = api.getNews(page = page, limit = PAGE_SIZE)
            topics.filter { it.id > 0 && it.topicTitle.isNotBlank() }.forEach { collected[it.id] = it }
            if (topics.size < PAGE_SIZE) break
        }
        if (collected.isEmpty()) return@withContext
        val rows = collected.values.map { topic ->
            val bodyText = topic.body?.let(::toPlainText).orEmpty()
            NewsArticleEntity(
                id = topic.id,
                title = topic.topicTitle.trim(),
                summary = bodyText.take(MAX_SUMMARY_LENGTH),
                createdAt = topic.createdAt,
                commentsCount = topic.commentsCount?.coerceAtLeast(0) ?: 0,
                authorName = topic.user?.nickname.orEmpty(),
                linkedTitle = topic.linked?.russian?.takeIf(String::isNotBlank) ?: topic.linked?.name?.takeIf(String::isNotBlank),
                category = NewsCategory.classify(topic.topicTitle, bodyText).name,
                sourceUrl = "https://shikimori.one/forum/news/${topic.id}",
                imageUrl = topic.body?.let(::extractImage),
                videoUrl = topic.body?.let(::extractVideo),
                bodyText = bodyText.take(MAX_BODY_LENGTH).ifBlank { null },
                linkedAnimeId = topic.linkedId ?: topic.linked?.id,
                lastFetchedAtEpochMillis = now
            )
        }
        newsDao.replaceFeed(rows)
    }

    override suspend fun refreshArticle(articleId: Long, force: Boolean) = withContext(Dispatchers.IO) {
        val cached = newsDao.getById(articleId)
        val now = System.currentTimeMillis()
        val bodyFresh = !cached?.bodyText.isNullOrBlank() &&
            cached?.lastFetchedAtEpochMillis?.let { now - it < ARTICLE_CACHE_TTL_MILLIS } == true
        if (!force && bodyFresh) return@withContext
        val remote = api.getTopic(articleId)
        val rawBody = remote.body.orEmpty()
        val bodyText = toPlainText(rawBody).take(MAX_BODY_LENGTH)
        val updated = NewsArticleEntity(
            id = remote.id,
            title = remote.topicTitle.ifBlank { cached?.title.orEmpty() },
            summary = bodyText.take(MAX_SUMMARY_LENGTH).ifBlank { cached?.summary.orEmpty() },
            createdAt = remote.createdAt.ifBlank { cached?.createdAt.orEmpty() },
            commentsCount = remote.commentsCount?.coerceAtLeast(0) ?: cached?.commentsCount ?: 0,
            authorName = remote.user?.nickname?.takeIf(String::isNotBlank) ?: cached?.authorName.orEmpty(),
            linkedTitle = remote.linked?.russian?.takeIf(String::isNotBlank) ?: remote.linked?.name?.takeIf(String::isNotBlank) ?: cached?.linkedTitle,
            category = NewsCategory.classify(remote.topicTitle, bodyText).name,
            sourceUrl = "https://shikimori.one/forum/news/${remote.id}",
            imageUrl = extractImage(rawBody) ?: cached?.imageUrl,
            videoUrl = extractVideo(rawBody) ?: cached?.videoUrl,
            bodyText = bodyText.ifBlank { cached?.bodyText },
            linkedAnimeId = remote.linkedId ?: remote.linked?.id ?: cached?.linkedAnimeId,
            lastFetchedAtEpochMillis = now
        )
        newsDao.upsert(updated)
    }

    private fun toPlainText(html: String): String {
        val safeMarkup = html.replace(Regex("(?is)<script[^>]*>.*?</script>"), " ")
            .replace(Regex("(?is)<style[^>]*>.*?</style>"), " ")
        val parsed: Spanned = Html.fromHtml(safeMarkup, Html.FROM_HTML_MODE_COMPACT)
        return parsed.toString().replace('\u00a0', ' ')
            .replace(Regex("\\n{3,}"), "\\n\\n")
            .replace(Regex("[ \\t]+"), " ").trim()
    }

    private fun extractImage(body: String): String? {
        val html = Regex("""(?is)<img[^>]+src=["'](https://[^"']+)["']""").find(body)?.groupValues?.getOrNull(1)
        val markdown = Regex("""(?is)!\\[[^]]*\\]\\((https://[^)]+)\\)""").find(body)?.groupValues?.getOrNull(1)
        return listOf(html, markdown).firstOrNull { it != null && !it.contains("avatar", true) && it.length <= MAX_URL_LENGTH }
    }

    private fun extractVideo(body: String): String? =
        Regex("""(?i)(https?://(?:www\\.)?(?:youtube\\.com/watch\\?v=[^\\s&<>]+|youtu\\.be/[^\\s?<>]+|youtube\\.com/embed/[^\\s?<>]+))""")
            .find(body)?.groupValues?.getOrNull(1)?.replace("http://", "https://")
            ?.trimEnd('.', ',', ')', ']')?.takeIf { it.length <= MAX_URL_LENGTH }

    companion object {
        const val PAGE_SIZE = 30
        const val MAX_PAGES = 3
        const val MAX_SUMMARY_LENGTH = 2_400
        const val MAX_BODY_LENGTH = 40_000
        const val MAX_URL_LENGTH = 2_048
        const val FEED_CACHE_TTL_MILLIS = 30 * 60 * 1_000L
        const val ARTICLE_CACHE_TTL_MILLIS = 24 * 60 * 60 * 1_000L
    }
}