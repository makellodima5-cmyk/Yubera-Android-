package com.yubera.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "catalog_anime")
data class CatalogAnimeEntity(
    @PrimaryKey val animeId: String,
    val title: String,
    val originalTitle: String,
    val year: Int,
    val status: String,
    val genresJson: String,
    val payloadJson: String,
    val position: Int
)
