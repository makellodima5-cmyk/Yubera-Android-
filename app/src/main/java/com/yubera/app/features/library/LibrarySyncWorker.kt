package com.yubera.app.features.library

import android.content.Context
import androidx.room.withTransaction
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.yubera.app.core.database.YuberaDatabase
import com.yubera.app.core.database.dao.AnimeLibraryDao
import com.yubera.app.core.database.dao.AnimeProgressDao
import com.yubera.app.core.database.dao.AnimeSubscriptionDao
import com.yubera.app.core.database.entity.AnimeLibraryEntity
import com.yubera.app.core.database.entity.AnimeProgressEntity
import com.yubera.app.core.database.entity.AnimeSubscriptionEntity
import com.yubera.app.core.session.ActiveAccountStore
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
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

@EntryPoint
@InstallIn(SingletonComponent::class)
interface LibrarySyncWorkerEntryPoint {
    fun database(): YuberaDatabase
    fun telegramLibrarySyncApi(): TelegramLibrarySyncApi
    fun secureTokenStore(): SecureTokenStore
    fun activeAccountStore(): ActiveAccountStore
}

class LibrarySyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val entry = EntryPointAccessors.fromApplication(applicationContext, LibrarySyncWorkerEntryPoint::class.java)
        val tokenStore = entry.secureTokenStore()
        val activeAccountStore = entry.activeAccountStore()
        val token = tokenStore.getAccessToken() ?: return@withContext Result.success()
        val accountId = tokenStore.getAccountId()
            ?.takeIf { it.isNotBlank() && it != ActiveAccountStore.LEGACY_UNASSIGNED_ACCOUNT_ID }
            ?: return@withContext Result.success()
        if (!sessionMatches(tokenStore, activeAccountStore, token, accountId)) return@withContext Result.success()

        val authorization = "Bearer $token"
        val database = entry.database()
        val api = entry.telegramLibrarySyncApi()
        val sourceId = deviceSourceId()

        try {
            val initial = api.getLibrary(authorization)
            if (!initial.authenticated) {
                clearSessionIfCurrent(tokenStore, activeAccountStore, token, accountId)
                return@withContext Result.success()
            }
            if (!sessionMatches(tokenStore, activeAccountStore, token, accountId)) return@withContext Result.success()

            val remote = initial.items.associateBy { key(it.itemType, it.animeId) }
            val localSnapshot = database.withTransaction {
                Triple(
                    database.animeLibraryDao().getAll(accountId),
                    database.animeProgressDao().getAll(accountId),
                    database.animeSubscriptionDao().getAll(accountId)
                )
            }
            val localItems = buildLocalItems(localSnapshot.first, localSnapshot.second, localSnapshot.third, sourceId)
            val outbound = localItems.filter { local ->
                val server = remote[key(local.itemType, local.animeId)]
                if (server == null) true else LibrarySyncPolicy.compareVersions(
                    local.updatedAtEpochMillis, local.sourceId,
                    server.updatedAtEpochMillis, server.sourceId
                ) > 0
            }.sortedBy { item ->
                when {
                    item.deleted -> 0
                    (item.itemType == TYPE_FAVORITE || item.itemType == TYPE_WATCHLIST) && item.payload.saved == false -> 0
                    else -> 1
                }
            }

            val canonicalItems = linkedMapOf<String, TelegramLibrarySyncItem>()
            initial.items.forEach { canonicalItems[key(it.itemType, it.animeId)] = it }
            outbound.chunked(MAX_BATCH_SIZE).forEach { batch ->
                if (!sessionMatches(tokenStore, activeAccountStore, token, accountId)) {
                    return@withContext Result.success()
                }
                val response = api.syncLibrary(authorization, TelegramLibrarySyncRequest(batch))
                if (!response.authenticated) {
                    clearSessionIfCurrent(tokenStore, activeAccountStore, token, accountId)
                    return@withContext Result.success()
                }
                if (!sessionMatches(tokenStore, activeAccountStore, token, accountId)) {
                    return@withContext Result.success()
                }
                response.items.forEach { item -> canonicalItems[key(item.itemType, item.animeId)] = item }
            }

            // Merge each server acknowledgement without overwriting a newer local edit.
            database.withTransaction {
                val libraryDao = database.animeLibraryDao()
                val progressDao = database.animeProgressDao()
                val subscriptionDao = database.animeSubscriptionDao()
                canonicalItems.values.forEach { item ->
                    val currentVersion = when (item.itemType) {
                        TYPE_FAVORITE -> libraryDao.getByAnimeId(accountId, item.animeId)
                            ?.favoriteUpdatedAtEpochMillis ?: 0L
                        TYPE_WATCHLIST -> libraryDao.getByAnimeId(accountId, item.animeId)
                            ?.watchLaterUpdatedAtEpochMillis ?: 0L
                        TYPE_PROGRESS -> progressDao.getByAnimeId(accountId, item.animeId)
                            ?.updatedAtEpochMillis ?: 0L
                        TYPE_SUBSCRIPTION -> subscriptionDao.getByAnimeId(accountId, item.animeId)
                            ?.updatedAtEpochMillis ?: 0L
                        else -> 0L
                    }
                    if (LibrarySyncPolicy.compareVersions(
                            item.updatedAtEpochMillis, item.sourceId, currentVersion, sourceId
                        ) > 0
                    ) {
                        applyRemote(accountId, item, libraryDao, progressDao, subscriptionDao)
                    }
                }
            }
            if (!sessionMatches(tokenStore, activeAccountStore, token, accountId)) {
                return@withContext Result.success()
            }
            Result.success()
        } catch (error: HttpException) {
            if (error.code() == 401) {
                clearSessionIfCurrent(tokenStore, activeAccountStore, token, accountId)
                Result.success()
            } else {
                Result.retry()
            }
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private fun sessionMatches(
        tokenStore: SecureTokenStore,
        activeAccountStore: ActiveAccountStore,
        token: String,
        accountId: String
    ): Boolean =
        tokenStore.getAccessToken() == token &&
            tokenStore.getAccountId() == accountId &&
            activeAccountStore.currentAccountId() == accountId

    private suspend fun clearSessionIfCurrent(
        tokenStore: SecureTokenStore,
        activeAccountStore: ActiveAccountStore,
        token: String,
        accountId: String
    ) {
        if (sessionMatches(tokenStore, activeAccountStore, token, accountId)) {
            activeAccountStore.clearActiveAccount()
        }
    }

    private fun buildLocalItems(
        library: Collection<AnimeLibraryEntity>,
        progress: Collection<AnimeProgressEntity>,
        subscriptions: Collection<AnimeSubscriptionEntity>,
        sourceId: String
    ): List<TelegramLibrarySyncItem> = buildList {
        library.forEach { row ->
            add(TelegramLibrarySyncItem(TYPE_FAVORITE, row.animeId, TelegramLibrarySyncPayload(saved = row.isFavorite), row.favoriteUpdatedAtEpochMillis, false, sourceId))
            add(TelegramLibrarySyncItem(TYPE_WATCHLIST, row.animeId, TelegramLibrarySyncPayload(saved = row.isInWatchLater), row.watchLaterUpdatedAtEpochMillis, false, sourceId))
        }
        progress.forEach { row ->
            add(TelegramLibrarySyncItem(
                TYPE_PROGRESS, row.animeId,
                TelegramLibrarySyncPayload(
                    seasonNumber = row.seasonNumber,
                    episodeNumber = row.episodeNumber,
                    positionMillis = row.positionMillis,
                    durationMillis = row.durationMillis
                ),
                row.updatedAtEpochMillis, false, sourceId
            ))
        }
        subscriptions.forEach { row ->
            add(TelegramLibrarySyncItem(
                TYPE_SUBSCRIPTION, row.animeId,
                TelegramLibrarySyncPayload(notificationsEnabled = row.notificationsEnabled),
                row.updatedAtEpochMillis, !row.isSubscribed, sourceId
            ))
        }
    }

    private suspend fun applyRemote(
        accountId: String,
        item: TelegramLibrarySyncItem,
        libraryDao: AnimeLibraryDao,
        progressDao: AnimeProgressDao,
        subscriptionDao: AnimeSubscriptionDao
    ) {
        when (item.itemType) {
            TYPE_FAVORITE -> {
                val current = libraryDao.getByAnimeId(accountId, item.animeId)
                libraryDao.upsert(AnimeLibraryEntity(
                    animeId = item.animeId,
                    isFavorite = !item.deleted && (item.payload.saved ?: false),
                    isInWatchLater = current?.isInWatchLater ?: false,
                    updatedAtEpochMillis = maxOf(current?.updatedAtEpochMillis ?: 0L, item.updatedAtEpochMillis),
                    favoriteUpdatedAtEpochMillis = item.updatedAtEpochMillis,
                    watchLaterUpdatedAtEpochMillis = current?.watchLaterUpdatedAtEpochMillis ?: 0L,
                    accountId = accountId
                ))
            }
            TYPE_WATCHLIST -> {
                val current = libraryDao.getByAnimeId(accountId, item.animeId)
                libraryDao.upsert(AnimeLibraryEntity(
                    animeId = item.animeId,
                    isFavorite = current?.isFavorite ?: false,
                    isInWatchLater = !item.deleted && (item.payload.saved ?: false),
                    updatedAtEpochMillis = maxOf(current?.updatedAtEpochMillis ?: 0L, item.updatedAtEpochMillis),
                    favoriteUpdatedAtEpochMillis = current?.favoriteUpdatedAtEpochMillis ?: 0L,
                    watchLaterUpdatedAtEpochMillis = item.updatedAtEpochMillis,
                    accountId = accountId
                ))
            }
            TYPE_PROGRESS -> {
                if (item.deleted) {
                    progressDao.deleteByAnimeId(accountId, item.animeId)
                } else {
                    progressDao.upsert(AnimeProgressEntity(
                        animeId = item.animeId,
                        seasonNumber = item.payload.seasonNumber,
                        episodeNumber = item.payload.episodeNumber,
                        positionMillis = item.payload.positionMillis ?: 0L,
                        durationMillis = item.payload.durationMillis ?: 0L,
                        updatedAtEpochMillis = item.updatedAtEpochMillis,
                        accountId = accountId
                    ))
                }
            }
            TYPE_SUBSCRIPTION -> {
                val current = subscriptionDao.getByAnimeId(accountId, item.animeId)
                subscriptionDao.upsert(AnimeSubscriptionEntity(
                    animeId = item.animeId,
                    notificationsEnabled = item.payload.notificationsEnabled ?: true,
                    updatedAtEpochMillis = item.updatedAtEpochMillis,
                    lastKnownEpisodesAired = current?.lastKnownEpisodesAired,
                    isSubscribed = !item.deleted,
                    accountId = accountId
                ))
            }
        }
    }

    private fun deviceSourceId(): String {
        // The sync tie-breaker only needs to be stable within this installation.
        // Do not persist raw device identifiers or rely on them being available.
        val deviceIdentity = applicationContext.getSharedPreferences("yubera_sync", Context.MODE_PRIVATE)
            .let { preferences ->
                preferences.getString("source_install_id", null) ?: UUID.randomUUID().toString().also { id ->
                    preferences.edit().putString("source_install_id", id).apply()
                }
            }
        val digest = MessageDigest.getInstance("SHA-256").digest(deviceIdentity.toByteArray())
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
