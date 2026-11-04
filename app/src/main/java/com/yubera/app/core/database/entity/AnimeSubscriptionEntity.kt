package com.yubera.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "anime_subscriptions", primaryKeys = ["accountId", "animeId"])
data class AnimeSubscriptionEntity(
    val animeId: String,
    val notificationsEnabled: Boolean,
    val updatedAtEpochMillis: Long,
    val lastKnownEpisodesAired: Int? = null,
    @ColumnInfo(defaultValue = "1") val isSubscribed: Boolean = true,
    @ColumnInfo(defaultValue = "'legacy-unassigned'") val accountId: String = "legacy-unassigned"
)
