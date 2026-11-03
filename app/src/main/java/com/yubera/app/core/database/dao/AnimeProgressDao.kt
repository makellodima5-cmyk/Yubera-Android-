package com.yubera.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.yubera.app.core.database.entity.AnimeProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnimeProgressDao {
    @Query("SELECT * FROM anime_progress ORDER BY updatedAtEpochMillis DESC")
    fun observeAll(): Flow<List<AnimeProgressEntity>>

    @Query("SELECT * FROM anime_progress WHERE animeId = :animeId LIMIT 1")
    suspend fun getByAnimeId(animeId: String): AnimeProgressEntity?

    @Upsert
    suspend fun upsert(entity: AnimeProgressEntity)
}
