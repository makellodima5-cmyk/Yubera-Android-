package com.yubera.app.features.anime

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class LocalTitleComment(
    val id: String,
    val author: String,
    val text: String,
    val createdAtEpochMillis: Long,
    val likes: Int = 0,
    val likedByMe: Boolean = false
)

@Singleton
class TitleLocalRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun observeRating(animeId: String): Flow<Int?> = dataStore.data.map { it[ratingKey(animeId)] }

    fun observeComments(animeId: String): Flow<List<LocalTitleComment>> = dataStore.data.map { prefs ->
        runCatching { json.decodeFromString<List<LocalTitleComment>>(prefs[commentsKey(animeId)].orEmpty()) }
            .getOrDefault(emptyList())
    }

    suspend fun saveRating(animeId: String, rating: Int) {
        require(rating in 1..10)
        dataStore.edit { it[ratingKey(animeId)] = rating }
    }

    suspend fun addComment(animeId: String, text: String) {
        val trimmed = text.trim().take(2000)
        if (trimmed.isBlank()) return
        dataStore.edit { prefs ->
            val existing = runCatching {
                json.decodeFromString<List<LocalTitleComment>>(prefs[commentsKey(animeId)].orEmpty())
            }.getOrDefault(emptyList())
            val comment = LocalTitleComment(
                id = java.util.UUID.randomUUID().toString(),
                author = "Вы",
                text = trimmed,
                createdAtEpochMillis = System.currentTimeMillis()
            )
            prefs[commentsKey(animeId)] = json.encodeToString(existing + comment)
        }
    }

    suspend fun toggleLike(animeId: String, commentId: String) {
        dataStore.edit { prefs ->
            val existing = runCatching {
                json.decodeFromString<List<LocalTitleComment>>(prefs[commentsKey(animeId)].orEmpty())
            }.getOrDefault(emptyList())
            val changed = existing.map { comment ->
                if (comment.id == commentId) {
                    comment.copy(
                        likedByMe = !comment.likedByMe,
                        likes = (comment.likes + if (comment.likedByMe) -1 else 1).coerceAtLeast(0)
                    )
                } else comment
            }
            prefs[commentsKey(animeId)] = json.encodeToString(changed)
        }
    }

    suspend fun deleteComment(animeId: String, commentId: String) {
        dataStore.edit { prefs ->
            val existing = runCatching {
                json.decodeFromString<List<LocalTitleComment>>(prefs[commentsKey(animeId)].orEmpty())
            }.getOrDefault(emptyList())
            prefs[commentsKey(animeId)] = json.encodeToString(existing.filterNot { it.id == commentId })
        }
    }

    private fun ratingKey(animeId: String) = intPreferencesKey("title_local_rating_$animeId")
    private fun commentsKey(animeId: String) = stringPreferencesKey("title_local_comments_$animeId")
}
