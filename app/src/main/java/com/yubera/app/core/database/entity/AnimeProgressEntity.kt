package com.yubera.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "anime_progress", primaryKeys = ["accountId", "animeId"])
data class AnimeProgressEntity(
    val animeId: String,
    val seasonNumber: Int?,
    val episodeNumber: Int?,
    val positionMillis: Long,
    val durationMillis: Long,
    val updatedAtEpochMillis: Long,
    @ColumnInfo(defaultValue = "'legacy-unassigned'") val accountId: String = "legacy-unassigned"
)
