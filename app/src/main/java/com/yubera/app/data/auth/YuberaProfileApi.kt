package com.yubera.app.data.auth

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface YuberaProfileApi {
    @GET("api/yubera/community?action=me")
    suspend fun getMyProfile(@Header("Authorization") authorization: String): YuberaCommunityResponse

    @POST("api/yubera/community")
    suspend fun updateProfile(
        @Header("Authorization") authorization: String,
        @Body request: YuberaProfileUpdateRequest
    ): YuberaProfileUpdateResponse
}

@Serializable
data class YuberaCommunityResponse(
    val authenticated: Boolean = false,
    val profile: YuberaCommunityProfile? = null,
    val collections: List<YuberaCollectionSummary> = emptyList()
)

@Serializable
data class YuberaCommunityProfile(
    val id: String = "",
    val displayName: String = "",
    val customName: String? = null,
    val username: String? = null,
    val photoUrl: String? = null,
    val avatarUrl: String? = null,
    val avatarDataUrl: String? = null,
    val createdAt: String? = null,
    val bio: String = "",
    val avatarFrame: String? = null,
    val availableSupportFrames: List<String> = emptyList(),
    val availableAdminFrames: List<String> = emptyList(),
    val isAdmin: Boolean = false,
    val visibility: String = "private",
    val isPlus: Boolean = false,
    val publicCollections: Int = 0,
    val publicTitles: Int = 0,
    val collectionLikes: Int = 0,
    val personalCollections: Int = 0,
    val personalTitles: Int = 0,
    val personalCollectionLikes: Int = 0,
    val followers: Int = 0,
    val following: Int = 0,
    val support: YuberaSupportStatus? = null,
    val xp: YuberaXpStatus? = null,
    val stats: YuberaProfileStats? = null
)

@Serializable
data class YuberaProfileStats(
    val favorites: Int = 0,
    val watchlist: Int = 0,
    val history: Int = 0
)

@Serializable
data class YuberaCollectionSummary(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val cover_key: String? = null,
    val is_public: Int = 0,
    val title_count: Int = 0,
    val like_count: Int = 0,
    val liked: Boolean = false
)

@Serializable
data class YuberaXpStatus(
    val total: Int = 0,
    val level: Int = 1,
    val title: String = "",
    val remaining: Int = 0,
    val progress: Float = 0f,
    val nextLevel: Int? = null,
    val nextTitle: String? = null,
    val nextThreshold: Int? = null
)

@Serializable
data class YuberaSupportStatus(
    val level: Int = 0,
    val title: String = "",
    val total: Int = 0
)

@Serializable
data class YuberaProfileUpdateRequest(
    val action: String = "profile.update",
    val bio: String,
    val visibility: String,
    val avatarFrame: String?,
    val customName: String = "",
    val avatarAnimeId: String? = null,
    val avatarDataUrl: String? = null
)

@Serializable
data class YuberaProfileUpdateResponse(
    val ok: Boolean = false,
    val error: String? = null
)
