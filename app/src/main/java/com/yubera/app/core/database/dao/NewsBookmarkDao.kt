package com.yubera.app.core.database.dao
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yubera.app.core.database.entity.NewsBookmarkEntity
import kotlinx.coroutines.flow.Flow
@Dao
interface NewsBookmarkDao {
 @Query("SELECT articleId FROM news_bookmarks ORDER BY savedAtEpochMillis DESC") fun observeBookmarkedIds(): Flow<List<Long>>
 @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun save(bookmark: NewsBookmarkEntity)
 @Query("DELETE FROM news_bookmarks WHERE articleId = :articleId") suspend fun remove(articleId: Long)
}
