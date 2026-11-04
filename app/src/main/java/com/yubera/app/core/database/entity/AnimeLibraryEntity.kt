package com.yubera.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "anime_library", primaryKeys = ["accountId", "animeId"])
data class AnimeLibraryEntity(
    val animeId: String,
    val isFavorite: Boolean = false,
    val isInWatchLater: Boolean = false,
    val updatedAtEpochMillis: Long,
    @ColumnInfo(defaultValue = "0") val favoriteUpdatedAtEpochMillis: Long = updatedAtEpochMillis,
    @ColumnInfo(defaultValue = "0") val watchLaterUpdatedAtEpochMillis: Long = updatedAtEpochMillis,
    @ColumnInfo(defaultValue = "'legacy-unassigned'") val accountId: String = "legacy-unassigned"
)
