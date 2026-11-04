package com.yubera.app.core.database.entity
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "news_bookmarks")
data class NewsBookmarkEntity(@PrimaryKey val articleId: Long, val savedAtEpochMillis: Long)
