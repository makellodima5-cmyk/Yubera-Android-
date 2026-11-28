package com.yubera.app.core.network

import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Serializes outbound Shikimori API requests to a conservative 90 requests/minute.
 * The same singleton interceptor is shared by all Shikimori Retrofit services.
 */
class ShikimoriRateLimitInterceptor(
    private val userAgent: String,
    private val minIntervalNanos: Long = TimeUnit.MINUTES.toNanos(1) / 90L
) : Interceptor {
    private val lock = Any()
    private var nextRequestAtNanos = 0L

    override fun intercept(chain: Interceptor.Chain): Response {
        waitForRequestSlot()

        val request = chain.request()
            .newBuilder()
            .header("User-Agent", userAgent)
            .build()
        val response = chain.proceed(request)

        if (response.code == 429) {
            val retryAfterSeconds = response.header("Retry-After")
                ?.toLongOrNull()
                ?.coerceIn(1L, MAX_RETRY_AFTER_SECONDS)
            if (retryAfterSeconds != null) {
                val retryAt = System.nanoTime() + TimeUnit.SECONDS.toNanos(retryAfterSeconds)
                synchronized(lock) {
                    nextRequestAtNanos = maxOf(nextRequestAtNanos, retryAt)
                }
            }
        }
        return response
    }

    private fun waitForRequestSlot() {
        val waitNanos = synchronized(lock) {
            val now = System.nanoTime()
            val slot = maxOf(now, nextRequestAtNanos)
            nextRequestAtNanos = slot + minIntervalNanos
            slot - now
        }
        if (waitNanos <= 0L) return

        try {
            TimeUnit.NANOSECONDS.sleep(waitNanos)
        } catch (interrupted: InterruptedException) {
            Thread.currentThread().interrupt()
            throw IOException("Interrupted while waiting for the Shikimori API rate limit", interrupted)
        }
    }

    private companion object {
        const val MAX_RETRY_AFTER_SECONDS = 300L
    }
}
