package com.yubera.app.features.updates

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.yubera.app.data.catalog.CatalogApi
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.URI
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppUpdateRepository @Inject constructor(
    private val catalogApi: CatalogApi,
    @ApplicationContext private val context: Context
) {
    suspend fun check(): AppUpdateCheckResult {
        val currentCode = runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= 28) info.longVersionCode.coerceAtMost(Int.MAX_VALUE.toLong()).toInt() else @Suppress("DEPRECATION") info.versionCode
        }.getOrElse { return AppUpdateCheckResult.Failed("Не удалось определить текущую версию приложения.") }

        return try {
            val manifest = catalogApi.getVersionManifest()
            require(manifest.schemaVersion == SUPPORTED_SCHEMA_VERSION)
            require(manifest.versionCode > 0)
            require(manifest.versionName.isNotBlank())
            require(manifest.minimumSupportedVersionCode > 0 && manifest.minimumSupportedVersionCode <= manifest.versionCode)
            require(manifest.apkSha256.matches(Regex("(?i)[0-9a-f]{64}")))
            require(manifest.fileSizeBytes == null || manifest.fileSizeBytes in 1..MAX_APK_BYTES)
            require(isAllowedApkUrl(manifest.apkUrl))
            if (manifest.versionCode <= currentCode) {
                AppUpdateCheckResult.UpToDate
            } else {
                AppUpdateCheckResult.Available(
                    AppUpdateInfo(
                        versionCode = manifest.versionCode,
                        versionName = manifest.versionName,
                        minimumSupportedVersionCode = manifest.minimumSupportedVersionCode,
                        releaseNotes = manifest.releaseNotes,
                        apkUrl = manifest.apkUrl,
                        apkSha256 = manifest.apkSha256.lowercase(Locale.ROOT),
                        fileSizeBytes = manifest.fileSizeBytes?.takeIf { it > 0 },
                        publishedAt = manifest.publishedAt
                    ),
                    isMandatory = currentCode < manifest.minimumSupportedVersionCode
                )
            }
        } catch (_: Exception) {
            AppUpdateCheckResult.Failed("Не удалось проверить наличие обновлений.")
        }
    }

    private fun isAllowedApkUrl(value: String): Boolean {
        val uri = runCatching { URI(value) }.getOrNull() ?: return false
        if (!uri.scheme.equals("https", ignoreCase = true) || uri.rawUserInfo != null || uri.port !in listOf(-1, 443)) return false
        val host = uri.host?.lowercase(Locale.ROOT)?.removeSuffix(".") ?: return false
        return host == UPDATE_HOST || host == "github.com" && uri.path.orEmpty().contains("/releases/download/")
    }

    companion object {
        const val SUPPORTED_SCHEMA_VERSION = 1
        const val UPDATE_HOST = "yume-website.yumesupport833.workers.dev"
        const val MAX_APK_BYTES = 250L * 1024L * 1024L
    }
}
