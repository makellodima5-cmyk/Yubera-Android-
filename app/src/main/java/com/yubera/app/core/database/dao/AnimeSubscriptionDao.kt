package com.yubera.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.yubera.app.core.database.entity.AnimeSubscriptionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnimeSubscriptionDao {
    @Query("SELECT * FROM anime_subscriptions WHERE accountId = :accountId AND isSubscribed = 1 ORDER BY updatedAtEpochMillis DESC")
    fun observeAll(accountId: String): Flow<List<AnimeSubscriptionEntity>>

    @Query("SELECT * FROM anime_subscriptions WHERE accountId = :accountId")
    suspend fun getAll(accountId: String): List<AnimeSubscriptionEntity>

    @Query("SELECT * FROM anime_subscriptions WHERE accountId = :accountId AND animeId = :animeId LIMIT 1")
    suspend fun getByAnimeId(accountId: String, animeId: String): AnimeSubscriptionEntity?

    @Query("SELECT * FROM anime_subscriptions WHERE accountId = :accountId AND isSubscribed = 1 AND notificationsEnabled = 1 ORDER BY updatedAtEpochMillis DESC")
    suspend fun getNotificationSubscriptions(accountId: String): List<AnimeSubscriptionEntity>

    @Query("UPDATE anime_subscriptions SET lastKnownEpisodesAired = :episodesAired WHERE accountId = :accountId AND animeId = :animeId")
    suspend fun updateEpisodeWatermark(accountId: String, animeId: String, episodesAired: Int)

    @Query("UPDATE anime_subscriptions SET accountId = :accountId WHERE accountId = 'legacy-unassigned'")
    suspend fun claimLegacyRows(accountId: String): Int

    @Upsert
    suspend fun upsert(entity: AnimeSubscriptionEntity)

    @Query("DELETE FROM anime_subscriptions WHERE accountId = :accountId AND animeId = :animeId")
    suspend fun deleteByAnimeId(accountId: String, animeId: String)
}
