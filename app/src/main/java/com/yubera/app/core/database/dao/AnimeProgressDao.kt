package com.yubera.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.yubera.app.core.database.entity.AnimeProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnimeProgressDao {
    @Query("SELECT * FROM anime_progress WHERE accountId = :accountId ORDER BY updatedAtEpochMillis DESC")
    fun observeAll(accountId: String): Flow<List<AnimeProgressEntity>>

    @Query("SELECT * FROM anime_progress WHERE accountId = :accountId")
    suspend fun getAll(accountId: String): List<AnimeProgressEntity>

    @Query("SELECT * FROM anime_progress WHERE accountId = :accountId AND animeId = :animeId LIMIT 1")
    suspend fun getByAnimeId(accountId: String, animeId: String): AnimeProgressEntity?

    @Query("UPDATE anime_progress SET accountId = :accountId WHERE accountId = 'legacy-unassigned'")
    suspend fun claimLegacyRows(accountId: String): Int

    @Upsert
    suspend fun upsert(entity: AnimeProgressEntity)
}
