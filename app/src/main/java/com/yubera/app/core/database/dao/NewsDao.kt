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
    @Query("SELECT * FROM news_articles ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<NewsArticleEntity>>

    @Query("DELETE FROM news_articles")
    suspend fun clear()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<NewsArticleEntity>)

    @Transaction
    suspend fun replaceAll(items: List<NewsArticleEntity>) {
        clear()
        if (items.isNotEmpty()) insertAll(items)
    }
}
