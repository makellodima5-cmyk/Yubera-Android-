package com.yubera.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "news_articles")
data class NewsArticleEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val summary: String,
    val createdAt: String,
    val commentsCount: Int,
    val authorName: String,
    val linkedTitle: String?,
    val category: String,
    val sourceUrl: String
)
