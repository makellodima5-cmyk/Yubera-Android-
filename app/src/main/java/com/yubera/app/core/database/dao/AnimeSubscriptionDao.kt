package com.yubera.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.yubera.app.core.database.entity.AnimeSubscriptionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnimeSubscriptionDao {
    @Query("SELECT * FROM anime_subscriptions ORDER BY updatedAtEpochMillis DESC")
    fun observeAll(): Flow<List<AnimeSubscriptionEntity>>

    @Query("SELECT * FROM anime_subscriptions WHERE animeId = :animeId LIMIT 1")
    suspend fun getByAnimeId(animeId: String): AnimeSubscriptionEntity?

    @Upsert
    suspend fun upsert(entity: AnimeSubscriptionEntity)

    @Query("DELETE FROM anime_subscriptions WHERE animeId = :animeId")
    suspend fun deleteByAnimeId(animeId: String)
}
