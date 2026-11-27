package com.yubera.app.features.updates

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException

@EntryPoint
@InstallIn(SingletonComponent::class)
interface UpdateCheckWorkerEntryPoint {
    fun appUpdateRepository(): AppUpdateRepository
}

class UpdateCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val entry = EntryPointAccessors.fromApplication(applicationContext, UpdateCheckWorkerEntryPoint::class.java)
        return try {
            when (entry.appUpdateRepository().check()) {
                AppUpdateCheckResult.UpToDate,
                is AppUpdateCheckResult.Available -> Result.success()
                is AppUpdateCheckResult.Failed -> Result.retry()
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
