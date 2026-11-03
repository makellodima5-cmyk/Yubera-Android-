package com.yubera.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "anime_library")
data class AnimeLibraryEntity(
    @PrimaryKey val animeId: String,
    val isFavorite: Boolean = false,
    val isInWatchLater: Boolean = false,
    val updatedAtEpochMillis: Long,
    @ColumnInfo(defaultValue = "0") val favoriteUpdatedAtEpochMillis: Long = updatedAtEpochMillis,
    @ColumnInfo(defaultValue = "0") val watchLaterUpdatedAtEpochMillis: Long = updatedAtEpochMillis
)
