package com.yubera.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.yubera.app.core.database.entity.AnimeSubscriptionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnimeSubscriptionDao {
    @Query("SELECT * FROM anime_subscriptions WHERE isSubscribed = 1 ORDER BY updatedAtEpochMillis DESC")
    fun observeAll(): Flow<List<AnimeSubscriptionEntity>>

    @Query("SELECT * FROM anime_subscriptions")
    suspend fun getAll(): List<AnimeSubscriptionEntity>

    @Query("SELECT * FROM anime_subscriptions WHERE animeId = :animeId LIMIT 1")
    suspend fun getByAnimeId(animeId: String): AnimeSubscriptionEntity?

    @Query("SELECT * FROM anime_subscriptions WHERE isSubscribed = 1 AND notificationsEnabled = 1 ORDER BY updatedAtEpochMillis DESC")
    suspend fun getNotificationSubscriptions(): List<AnimeSubscriptionEntity>

    @Query("UPDATE anime_subscriptions SET lastKnownEpisodesAired = :episodesAired WHERE animeId = :animeId")
    suspend fun updateEpisodeWatermark(animeId: String, episodesAired: Int)

    @Upsert
    suspend fun upsert(entity: AnimeSubscriptionEntity)

    @Query("DELETE FROM anime_subscriptions WHERE animeId = :animeId")
    suspend fun deleteByAnimeId(animeId: String)
}
