package com.yubera.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "anime_progress")
data class AnimeProgressEntity(
    @PrimaryKey val animeId: String,
    val seasonNumber: Int?,
    val episodeNumber: Int?,
    val positionMillis: Long,
    val durationMillis: Long,
    val updatedAtEpochMillis: Long
)
