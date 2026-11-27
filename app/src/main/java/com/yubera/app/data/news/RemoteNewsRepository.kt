package com.yubera.app.data.news
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
class RemoteNewsRepository @Inject constructor(private val api:ShikimoriNewsApi,private val newsDao:NewsDao,private val bookmarkDao:NewsBookmarkDao):NewsRepository{
 override fun observeNews():Flow<List<NewsArticleEntity>>=newsDao.observeFeed()
 override fun observeBookmarkedNews():Flow<List<NewsArticleEntity>>=newsDao.observeBookmarked()
 override fun observeBookmarkedIds():Flow<List<Long>>=bookmarkDao.observeBookmarkedIds()
 override fun observeNewsArticle(articleId:Long):Flow<NewsArticleEntity?>=newsDao.observeAll().map{it.firstOrNull{a->a.id==articleId}}
 override suspend fun setBookmarked(articleId:Long,bookmarked:Boolean)=withContext(Dispatchers.IO){if(bookmarked)bookmarkDao.save(NewsBookmarkEntity(articleId,System.currentTimeMillis()))else bookmarkDao.remove(articleId)}
 override suspend fun refresh()=withContext(Dispatchers.IO){
  val topics=api.getNews(page=1,limit=PAGE_SIZE)
  val rows=topics.filter{it.id>0&&it.topicTitle.isNotBlank()}.distinctBy{it.id}.map{topic->
   val body=topic.body.orEmpty();val summary=plainText(body).take(MAX_SUMMARY_LENGTH)
   NewsArticleEntity(topic.id,topic.topicTitle.trim(),summary,topic.createdAt,topic.commentsCount?.coerceAtLeast(0)?:0,topic.user?.nickname.orEmpty(),topic.linked?.russian?.takeIf(String::isNotBlank)?:topic.linked?.name?.takeIf(String::isNotBlank),NewsCategory.classify(topic.topicTitle,summary).name,"https://shikimori.io/forum/news/\${topic.id}",extractImage(body),extractVideo(body))
  }
  if(rows.isNotEmpty())newsDao.replaceFeed(rows)
 }
 private fun plainText(v:String)=v.replace(Regex("(?is)<script[^>]*>.*?</script>")," ").replace(Regex("(?is)<style[^>]*>.*?</style>")," ").replace(Regex("<[^>]*>")," ").replace(Regex("\\\\[[^\\\\]]*\\\\]")," ").replace("&nbsp;"," ").replace("&amp;","&").replace("&quot;","\\\"").replace("&#39;","'").replace(Regex("\\\\s+")," ").trim()
 private fun extractImage(body:String):String?=listOf(Regex("(?is)<img[^>]+src=[\\\"'](https?://[^\\\"']+)[\\\"']").find(body)?.groupValues?.getOrNull(1),Regex("(?is)!\\\\[[^]]*]\\\\((https?://[^)]+)\\\\)").find(body)?.groupValues?.getOrNull(1)).firstOrNull{it?.startsWith("https://",true)==true&&!it.contains("avatar",true)}
 private fun extractVideo(body:String):String?=Regex("(?i)(https?://(?:www\\\\.)?(?:youtube\\\\.com/watch\\\\?v=[^\\\\s&<>]+|youtu\\\\.be/[^\\\\s?<>]+|youtube\\\\.com/embed/[^\\\\s?<>]+))").find(body)?.groupValues?.getOrNull(1)?.replace("http://","https://")?.trimEnd('.',',',')',']')
 companion object{const val PAGE_SIZE=30;const val MAX_SUMMARY_LENGTH=2400}
}
