package com.yubera.app.features.library

import android.content.Context
import android.provider.Settings
import androidx.room.withTransaction
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.yubera.app.core.database.YuberaDatabase
import com.yubera.app.core.database.entity.AnimeLibraryEntity
import com.yubera.app.core.database.entity.AnimeProgressEntity
import com.yubera.app.core.database.entity.AnimeSubscriptionEntity
import com.yubera.app.core.session.SecureTokenStore
import com.yubera.app.data.library.TelegramLibrarySyncApi
import com.yubera.app.data.library.TelegramLibrarySyncItem
import com.yubera.app.data.library.TelegramLibrarySyncPayload
import com.yubera.app.data.library.TelegramLibrarySyncRequest
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

@EntryPoint
@InstallIn(SingletonComponent::class)
interface LibrarySyncWorkerEntryPoint {
    fun database(): YuberaDatabase
    fun telegramLibrarySyncApi(): TelegramLibrarySyncApi
    fun secureTokenStore(): SecureTokenStore
}

class LibrarySyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val entry = EntryPointAccessors.fromApplication(applicationContext, LibrarySyncWorkerEntryPoint::class.java)
        val tokenStore = entry.secureTokenStore()
        val token = tokenStore.getAccessToken() ?: return@withContext Result.success()
        val authorization = "Bearer $token"
        val database = entry.database()
        val api = entry.telegramLibrarySyncApi()
        val sourceId = deviceSourceId()

        try {
            val initial = api.getLibrary(authorization)
            if (!initial.authenticated) {
                tokenStore.clearAccessToken()
                return@withContext Result.success()
            }

            val remote = initial.items.associateBy { key(it.itemType, it.animeId) }
            val outbound = database.withTransaction {
                val libraryDao = database.animeLibraryDao()
                val progressDao = database.animeProgressDao()
                val subscriptionDao = database.animeSubscriptionDao()

                val localLibrary = libraryDao.getAll().associateBy { it.animeId }
                val localProgress = progressDao.getAll().associateBy { it.animeId }
                val localSubscriptions = subscriptionDao.getAll().associateBy { it.animeId }

                initial.items.forEach { item ->
                    val localTimestamp = localTimestamp(item.itemType, item.animeId, localLibrary, localProgress, localSubscriptions)
                    if (LibrarySyncPolicy.compareVersions(
                            item.updatedAtEpochMillis, item.sourceId, localTimestamp, sourceId
                        ) > 0
                    ) {
                        applyRemote(item, libraryDao, progressDao, subscriptionDao)
                    }
                }

                buildLocalItems(
                    libraryDao.getAll(),
                    progressDao.getAll(),
                    subscriptionDao.getAll(),
                    sourceId
                ).filter { local ->
                    val server = remote[key(local.itemType, local.animeId)]
                    server == null || LibrarySyncPolicy.compareVersions(
                        local.updatedAtEpochMillis, local.sourceId,
                        server.updatedAtEpochMillis, server.sourceId
                    ) > 0
                }
            }

            var canonicalItems = initial.items
            outbound.chunked(MAX_BATCH_SIZE).forEach { batch ->
                val response = api.syncLibrary(authorization, TelegramLibrarySyncRequest(batch))
                if (!response.authenticated) {
                    tokenStore.clearAccessToken()
                    return@withContext Result.success()
                }
                canonicalItems = response.items
            }

            database.withTransaction {
                val libraryDao = database.animeLibraryDao()
                val progressDao = database.animeProgressDao()
                val subscriptionDao = database.animeSubscriptionDao()
                val library = libraryDao.getAll().associateBy { it.animeId }
                val progress = progressDao.getAll().associateBy { it.animeId }
                val subscriptions = subscriptionDao.getAll().associateBy { it.animeId }
                canonicalItems.forEach { item ->
                    val localTimestamp = localTimestamp(item.itemType, item.animeId, library, progress, subscriptions)
                    if (LibrarySyncPolicy.compareVersions(
                            item.updatedAtEpochMillis, item.sourceId, localTimestamp, sourceId
                        ) > 0
                    ) {
                        applyRemote(item, libraryDao, progressDao, subscriptionDao)
                    }
                }
            }
            Result.success()
        } catch (error: HttpException) {
            if (error.code() == 401) {
                tokenStore.clearAccessToken()
                Result.success()
            } else {
                Result.retry()
            }
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private fun buildLocalItems(
        library: Collection<AnimeLibraryEntity>,
        progress: Collection<AnimeProgressEntity>,
        subscriptions: Collection<AnimeSubscriptionEntity>,
        sourceId: String
    ): List<TelegramLibrarySyncItem> = buildList {
        library.forEach { row ->
            add(TelegramLibrarySyncItem(
                itemType = TYPE_FAVORITE,
                animeId = row.animeId,
                payload = TelegramLibrarySyncPayload(saved = row.isFavorite),
                updatedAtEpochMillis = row.favoriteUpdatedAtEpochMillis,
                deleted = false,
                sourceId = sourceId
            ))
            add(TelegramLibrarySyncItem(
                itemType = TYPE_WATCHLIST,
                animeId = row.animeId,
                payload = TelegramLibrarySyncPayload(saved = row.isInWatchLater),
                updatedAtEpochMillis = row.watchLaterUpdatedAtEpochMillis,
                deleted = false,
                sourceId = sourceId
            ))
        }
        progress.forEach { row ->
            add(TelegramLibrarySyncItem(
                itemType = TYPE_PROGRESS,
                animeId = row.animeId,
                payload = TelegramLibrarySyncPayload(
                    seasonNumber = row.seasonNumber,
                    episodeNumber = row.episodeNumber,
                    positionMillis = row.positionMillis,
                    durationMillis = row.durationMillis
                ),
                updatedAtEpochMillis = row.updatedAtEpochMillis,
                deleted = false,
                sourceId = sourceId
            ))
        }
        subscriptions.forEach { row ->
            add(TelegramLibrarySyncItem(
                itemType = TYPE_SUBSCRIPTION,
                animeId = row.animeId,
                payload = TelegramLibrarySyncPayload(notificationsEnabled = row.notificationsEnabled),
                updatedAtEpochMillis = row.updatedAtEpochMillis,
                deleted = !row.isSubscribed,
                sourceId = sourceId
            ))
        }
    }

    private suspend fun localTimestamp(
        type: String,
        animeId: String,
        library: Map<String, AnimeLibraryEntity>,
        progress: Map<String, AnimeProgressEntity>,
        subscriptions: Map<String, AnimeSubscriptionEntity>
    ): Long = when (type) {
        TYPE_FAVORITE -> library[animeId]?.favoriteUpdatedAtEpochMillis ?: 0L
        TYPE_WATCHLIST -> library[animeId]?.watchLaterUpdatedAtEpochMillis ?: 0L
        TYPE_PROGRESS -> progress[animeId]?.updatedAtEpochMillis ?: 0L
        TYPE_SUBSCRIPTION -> subscriptions[animeId]?.updatedAtEpochMillis ?: 0L
        else -> 0L
    }

    private suspend fun applyRemote(
        item: TelegramLibrarySyncItem,
        libraryDao: com.yubera.app.core.database.dao.AnimeLibraryDao,
        progressDao: com.yubera.app.core.database.dao.AnimeProgressDao,
        subscriptionDao: com.yubera.app.core.database.dao.AnimeSubscriptionDao
    ) {
        when (item.itemType) {
            TYPE_FAVORITE -> {
                val current = libraryDao.getByAnimeId(item.animeId)
                libraryDao.upsert(AnimeLibraryEntity(
                    animeId = item.animeId,
                    isFavorite = item.payload.saved ?: false,
                    isInWatchLater = current?.isInWatchLater ?: false,
                    updatedAtEpochMillis = maxOf(current?.updatedAtEpochMillis ?: 0L, item.updatedAtEpochMillis),
                    favoriteUpdatedAtEpochMillis = item.updatedAtEpochMillis,
                    watchLaterUpdatedAtEpochMillis = current?.watchLaterUpdatedAtEpochMillis ?: 0L
                ))
            }
            TYPE_WATCHLIST -> {
                val current = libraryDao.getByAnimeId(item.animeId)
                libraryDao.upsert(AnimeLibraryEntity(
                    animeId = item.animeId,
                    isFavorite = current?.isFavorite ?: false,
                    isInWatchLater = item.payload.saved ?: false,
                    updatedAtEpochMillis = maxOf(current?.updatedAtEpochMillis ?: 0L, item.updatedAtEpochMillis),
                    favoriteUpdatedAtEpochMillis = current?.favoriteUpdatedAtEpochMillis ?: 0L,
                    watchLaterUpdatedAtEpochMillis = item.updatedAtEpochMillis
                ))
            }
            TYPE_PROGRESS -> progressDao.upsert(AnimeProgressEntity(
                animeId = item.animeId,
                seasonNumber = item.payload.seasonNumber,
                episodeNumber = item.payload.episodeNumber,
                positionMillis = item.payload.positionMillis ?: 0L,
                durationMillis = item.payload.durationMillis ?: 0L,
                updatedAtEpochMillis = item.updatedAtEpochMillis
            ))
            TYPE_SUBSCRIPTION -> {
                val current = subscriptionDao.getByAnimeId(item.animeId)
                subscriptionDao.upsert(AnimeSubscriptionEntity(
                    animeId = item.animeId,
                    notificationsEnabled = item.payload.notificationsEnabled ?: true,
                    updatedAtEpochMillis = item.updatedAtEpochMillis,
                    lastKnownEpisodesAired = current?.lastKnownEpisodesAired,
                    isSubscribed = !item.deleted
                ))
            }
        }
    }

    private fun deviceSourceId(): String {
        val androidId = Settings.Secure.getString(applicationContext.contentResolver, Settings.Secure.ANDROID_ID).orEmpty()
        val digest = MessageDigest.getInstance("SHA-256").digest(androidId.toByteArray())
        val suffix = digest.take(10).joinToString("") { byte -> (byte.toInt() and 0xff).toString(16).padStart(2, '0') }
        return "android-$suffix"
    }

    private fun key(type: String, animeId: String) = "$type:$animeId"

    private companion object {
        const val MAX_BATCH_SIZE = 500
        const val TYPE_FAVORITE = "favorite"
        const val TYPE_WATCHLIST = "watchlist"
        const val TYPE_PROGRESS = "progress"
        const val TYPE_SUBSCRIPTION = "subscription"
    }
}
