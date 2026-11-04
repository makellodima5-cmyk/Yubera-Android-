package com.yubera.app.core.news
import com.yubera.app.core.database.entity.NewsArticleEntity
import kotlinx.coroutines.flow.Flow
interface NewsRepository {
 fun observeNews(): Flow<List<NewsArticleEntity>>
 fun observeBookmarkedIds(): Flow<List<Long>>
 fun observeNewsArticle(articleId: Long): Flow<NewsArticleEntity?>
 suspend fun setBookmarked(articleId: Long, bookmarked: Boolean)
 suspend fun refresh()
}
