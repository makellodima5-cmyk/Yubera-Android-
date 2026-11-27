package com.yubera.app.features.updates

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AppUpdateManager(private val context: Context) {
    fun downloadUpdate(update: AppUpdateInfo) {
        val data = Data.Builder()
            .putString(ApkDownloadWorker.KEY_URL, update.apkUrl)
            .putString(ApkDownloadWorker.KEY_SHA256, update.apkSha256)
            .putLong(ApkDownloadWorker.KEY_EXPECTED_SIZE, update.fileSizeBytes ?: -1L)
            .build()
        val request = OneTimeWorkRequestBuilder<ApkDownloadWorker>()
            .setInputData(data)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .addTag(UPDATE_DOWNLOAD_WORK)
            .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(UPDATE_DOWNLOAD_WORK, ExistingWorkPolicy.REPLACE, request)
    }

    fun downloadState(): Flow<WorkInfo?> =
        WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(UPDATE_DOWNLOAD_WORK)
            .map { infos -> infos.firstOrNull() }

    fun verifiedApkPath(workInfo: WorkInfo): File? {
        if (workInfo.state != WorkInfo.State.SUCCEEDED) return null
        val path = workInfo.outputData.getString(ApkDownloadWorker.KEY_PATH) ?: return null
        return File(path).takeIf { it.isFile && it.length() > 0L }
    }

    companion object { const val UPDATE_DOWNLOAD_WORK = "yubera-apk-update-download" }
}
