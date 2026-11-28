package com.yubera.app.data.news

import android.text.Html
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

class RemoteNewsRepository @Inject constructor(private val api: ShikimoriNewsApi, private val newsDao: NewsDao, private val bookmarkDao: NewsBookmarkDao) : NewsRepository {
 override fun observeNews(): Flow<List<NewsArticleEntity>> = newsDao.observeFeed()
 override fun observeBookmarkedNews(): Flow<List<NewsArticleEntity>> = newsDao.observeBookmarked()
 override fun observeBookmarkedIds(): Flow<List<Long>> = bookmarkDao.observeBookmarkedIds()
 override fun observeNewsArticle(articleId: Long): Flow<NewsArticleEntity?> = newsDao.observeAll().map { it.firstOrNull { row -> row.id == articleId } }
 override suspend fun setBookmarked(articleId: Long, bookmarked: Boolean) = withContext(Dispatchers.IO) { if (bookmarked) bookmarkDao.save(NewsBookmarkEntity(articleId, System.currentTimeMillis())) else bookmarkDao.remove(articleId) }
 override suspend fun refresh(force: Boolean) = withContext(Dispatchers.IO) {
  val last = newsDao.getLastFetchTime() ?: 0L; val now = System.currentTimeMillis(); if (!force && now - last < FEED_TTL) return@withContext
  val found = linkedMapOf<Long, ShikimoriTopicDto>()
  for (page in 1..MAX_PAGES) { val items = api.getNews(page = page, limit = PAGE_SIZE); items.filter { it.id > 0 && it.topicTitle.isNotBlank() }.forEach { found[it.id] = it }; if (items.size < PAGE_SIZE) break }
  if (found.isEmpty()) return@withContext
  val rows = found.values.map { topic ->
   val text = cleanText(topic.body.orEmpty()); val previous = newsDao.getById(topic.id)
   NewsArticleEntity(topic.id, topic.topicTitle.trim(), text.take(MAX_SUMMARY), topic.createdAt, topic.commentsCount?.coerceAtLeast(0) ?: 0, topic.user?.nickname.orEmpty(), topic.linked?.russian?.takeIf(String::isNotBlank) ?: topic.linked?.name, NewsCategory.classify(topic.topicTitle, text).name, sourceUrl(topic.id), imageFrom(topic.body.orEmpty()) ?: previous?.imageUrl, videoFrom(topic.body.orEmpty()) ?: previous?.videoUrl, previous?.bodyText ?: text.take(MAX_BODY).ifBlank { null }, topic.linkedId ?: topic.linked?.id ?: previous?.linkedAnimeId, now)
  }
  newsDao.replaceFeed(rows)
 }
 override suspend fun refreshArticle(articleId: Long) = withContext(Dispatchers.IO) {
  val remote = api.getTopic(articleId); val cached = newsDao.getById(articleId); val text = cleanText(remote.body.orEmpty()).take(MAX_BODY); val raw = remote.body.orEmpty(); val now = System.currentTimeMillis()
  newsDao.upsert(NewsArticleEntity(remote.id, remote.topicTitle.ifBlank { cached?.title.orEmpty() }, text.take(MAX_SUMMARY).ifBlank { cached?.summary.orEmpty() }, remote.createdAt.ifBlank { cached?.createdAt.orEmpty() }, remote.commentsCount?.coerceAtLeast(0) ?: cached?.commentsCount ?: 0, remote.user?.nickname?.takeIf(String::isNotBlank) ?: cached?.authorName.orEmpty(), remote.linked?.russian?.takeIf(String::isNotBlank) ?: remote.linked?.name ?: cached?.linkedTitle, NewsCategory.classify(remote.topicTitle, text).name, sourceUrl(remote.id), imageFrom(raw) ?: cached?.imageUrl, videoFrom(raw) ?: cached?.videoUrl, text.ifBlank { cached?.bodyText }, remote.linkedId ?: remote.linked?.id ?: cached?.linkedAnimeId, now))
 }
 private fun sourceUrl(id: Long) = "https://shikimori.one/forum/news/" + id
 private fun cleanText(raw: String): String { val safe = raw.replace(Regex("(?is)<script[^>]*>.*?</script>"), " ").replace(Regex("(?is)<style[^>]*>.*?</style>"), " "); return Html.fromHtml(safe, Html.FROM_HTML_MODE_COMPACT).toString().replace('\u00a0', ' ').replace(Regex("\\s+"), " ").trim() }
 private fun imageFrom(raw: String): String? = Regex("""(?is)<img[^>]+src=["'](https://[^"']+)["']""").find(raw)?.groupValues?.getOrNull(1)?.takeIf { it.length <= 2048 && !it.contains("avatar", true) }
 private fun videoFrom(raw: String): String? = Regex("""(?i)(https?://(?:www\\.)?(?:youtube\\.com/watch\\?v=[^\\s&<>]+|youtu\\.be/[^\\s?<>]+|youtube\\.com/embed/[^\\s?<>]+))""").find(raw)?.groupValues?.getOrNull(1)?.replace("http://", "https://")?.trimEnd('.', ',', ')', ']')
 companion object { const val PAGE_SIZE = 30; const val MAX_PAGES = 3; const val MAX_SUMMARY = 2400; const val MAX_BODY = 40000; const val FEED_TTL = 30 * 60 * 1000L }
}