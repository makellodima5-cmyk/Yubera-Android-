package com.yubera.app.core.database.entity
import androidx.room.Entity
@Entity(tableName = "anime_episode_notifications", primaryKeys = ["animeId", "episodeNumber"])
data class AnimeEpisodeNotificationEntity(
 val animeId: String,
 val episodeNumber: Int,
 val createdAtEpochMillis: Long,
 val deliveredAtEpochMillis: Long? = null
)
