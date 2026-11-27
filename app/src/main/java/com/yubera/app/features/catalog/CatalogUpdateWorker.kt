package com.yubera.app.features.catalog

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.yubera.app.data.catalog.RemoteCatalogRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CatalogUpdateWorkerEntryPoint {
    fun remoteCatalogRepository(): RemoteCatalogRepository
}

class CatalogUpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val entry = EntryPointAccessors.fromApplication(applicationContext, CatalogUpdateWorkerEntryPoint::class.java)
        return try {
            try {
                entry.remoteCatalogRepository().refreshIfStale()
                Result.success()
            } catch (error: Exception) {
                entry.remoteCatalogRepository().markCheckDeferred()
                throw error
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
