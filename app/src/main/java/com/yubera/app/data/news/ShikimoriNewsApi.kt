package com.yubera.app.data.news

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

interface ShikimoriNewsApi {
    @GET("api/topics")
    suspend fun getNews(
        @Query("forum") forum: String = "news",
        @Query("type") type: String = "Topics::NewsTopic",
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 30
    ): List<ShikimoriTopicDto>
}

@Serializable
data class ShikimoriTopicDto(
    val id: Long,
    @SerialName("topic_title") val topicTitle: String,
    val body: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("comments_count") val commentsCount: Int? = null,
    val user: ShikimoriUserDto? = null,
    val linked: ShikimoriLinkedDto? = null
)

@Serializable
data class ShikimoriUserDto(
    val nickname: String? = null
)

@Serializable
data class ShikimoriLinkedDto(
    val name: String? = null,
    val russian: String? = null
)
