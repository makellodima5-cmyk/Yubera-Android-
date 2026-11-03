package com.yubera.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "anime_subscriptions")
data class AnimeSubscriptionEntity(
    @PrimaryKey val animeId: String,
    val notificationsEnabled: Boolean,
    val updatedAtEpochMillis: Long,
    val lastKnownEpisodesAired: Int? = null,
    val isSubscribed: Boolean = true
)
