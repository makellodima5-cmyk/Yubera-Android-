package com.yubera.app.features.updates

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

class ApkDownloadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val url = inputData.getString(KEY_URL) ?: return Result.failure()
        val checksum = inputData.getString(KEY_SHA256)?.lowercase() ?: return Result.failure()
        if (!url.startsWith("https://") || !checksum.matches(Regex("[0-9a-f]{64}"))) return Result.failure()
        val expectedSize = inputData.getLong(KEY_EXPECTED_SIZE, -1L)
        var currentUrl = URL(url)
        for (redirectCount in 0..MAX_REDIRECTS) {
            val connection = currentUrl.openConnection() as? HttpURLConnection ?: return Result.failure()
            try {
                connection.connectTimeout = 15_000
                connection.readTimeout = 30_000
                connection.instanceFollowRedirects = false
                connection.setRequestProperty("Accept", "application/vnd.android.package-archive")
                connection.setRequestProperty("Accept-Encoding", "identity")
                connection.connect()
                if (connection.responseCode in REDIRECT_CODES) {
                    val location = connection.getHeaderField("Location") ?: return Result.failure()
                    if (redirectCount == MAX_REDIRECTS) return Result.failure()
                    val next = URL(currentUrl, location)
                    if (!next.protocol.equals("https", true) || next.host.lowercase() !in DOWNLOAD_HOSTS) return Result.failure()
                    currentUrl = next
                    continue
                }
                if (connection.responseCode !in 200..299) return Result.retry()
                if (currentUrl.host.lowercase() !in DOWNLOAD_HOSTS) return Result.failure()
                val contentLength = connection.contentLengthLong
                if (expectedSize > 0 && contentLength > 0 && contentLength != expectedSize) return Result.failure()

                val file = File(applicationContext.cacheDir, "updates/yubera-update.apk")
                file.parentFile?.mkdirs()
                val digest = MessageDigest.getInstance("SHA-256")
                var total = 0L
                connection.inputStream.use { input ->
                    FileOutputStream(file).use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            if (isStopped) return Result.retry()
                            val count = input.read(buffer)
                            if (count < 0) break
                            total += count
                            if (total > MAX_APK_BYTES || expectedSize > 0 && total > expectedSize) return Result.failure()
                            digest.update(buffer, 0, count)
                            output.write(buffer, 0, count)
                        }
                        output.fd.sync()
                    }
                }
                val actual = digest.digest().joinToString("") { "%02x".format(it) }
                if (actual != checksum || expectedSize > 0 && total != expectedSize || !isZipArchive(file)) {
                    file.delete()
                    return Result.failure()
                }
                return Result.success(androidx.work.workDataOf(KEY_PATH to file.absolutePath))
            } finally {
                connection.disconnect()
            }
        }
        return Result.failure()
    }

    private fun isZipArchive(file: File): Boolean = runCatching {
        file.inputStream().use { input ->
            val signature = ByteArray(4)
            input.read(signature) == 4 && signature[0] == 0x50.toByte() &&
                signature[1] == 0x4b.toByte() && signature[2] == 0x03.toByte() &&
                signature[3] == 0x04.toByte()
        }
    }.getOrDefault(false)

    companion object {
        const val KEY_URL = "apk_url"
        const val KEY_SHA256 = "apk_sha256"
        const val KEY_EXPECTED_SIZE = "expected_size"
        const val KEY_PATH = "apk_path"
        const val MAX_APK_BYTES = 250L * 1024L * 1024L
        const val MAX_REDIRECTS = 3
        val REDIRECT_CODES = setOf(301, 302, 303, 307, 308)
        val DOWNLOAD_HOSTS = setOf(
            "yume-website.yumesupport833.workers.dev",
            "objects.githubusercontent.com",
            "release-assets.githubusercontent.com",
            "github.com"
        )
    }
}
