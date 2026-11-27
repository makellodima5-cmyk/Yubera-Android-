package com.yubera.app.features.updates

import android.content.Context
import android.content.Intent
import androidx.work.WorkInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class AppUpdateCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: AppUpdateRepository
) {
    private val manager by lazy { AppUpdateManager(context) }
    private val installer by lazy { ApkInstaller(context) }

    suspend fun check(): AppUpdateCheckResult = repository.check()

    fun download(update: AppUpdateInfo) = manager.downloadUpdate(update)

    fun observeDownload(): Flow<WorkInfo?> = manager.downloadState()

    fun createUserConfirmedInstallIntent(workInfo: WorkInfo, update: AppUpdateInfo): Intent? {
        val apk = manager.verifiedApkPath(workInfo) ?: return null
        return installer.createInstallIntent(apk, update.apkSha256)
    }
}
