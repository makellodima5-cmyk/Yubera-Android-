package com.yubera.app.features.updates

data class AppUpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val minimumSupportedVersionCode: Int,
    val releaseNotes: String,
    val apkUrl: String,
    val apkSha256: String,
    val fileSizeBytes: Long?,
    val publishedAt: String
)

sealed interface AppUpdateCheckResult {
    data object UpToDate : AppUpdateCheckResult
    data class Available(val update: AppUpdateInfo, val isMandatory: Boolean) : AppUpdateCheckResult
    data class Failed(val reason: String) : AppUpdateCheckResult
}
