package com.yubera.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.yubera.app.core.database.entity.NewsArticleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NewsDao {
    @Query("SELECT * FROM news_articles ORDER BY createdAt DESC LIMIT 30")
    fun observeFeed(): Flow<List<NewsArticleEntity>>

    @Query("""
        SELECT news_articles.*
        FROM news_articles
        INNER JOIN news_bookmarks ON news_articles.id = news_bookmarks.articleId
        ORDER BY news_bookmarks.savedAtEpochMillis DESC
    """)
    fun observeBookmarked(): Flow<List<NewsArticleEntity>>

    @Query("SELECT * FROM news_articles ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<NewsArticleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<NewsArticleEntity>)

    @Query("""
        DELETE FROM news_articles
        WHERE id NOT IN (:currentFeedIds)
          AND id NOT IN (SELECT articleId FROM news_bookmarks)
    """)
    suspend fun deleteUnbookmarkedOutsideFeed(currentFeedIds: List<Long>)

    @Transaction
    suspend fun replaceFeed(items: List<NewsArticleEntity>) {
        if (items.isEmpty()) return
        insertAll(items)
        deleteUnbookmarkedOutsideFeed(items.map(NewsArticleEntity::id))
    }
}
