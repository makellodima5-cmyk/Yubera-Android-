package com.yubera.app.core.network

/**
 * Public Yubera origin used by the website and the native Kodik iframe wrapper.
 * It must match the website's SITE_ORIGIN so Kodik receives the permitted
 * Yubera Referer rather than a direct top-level Kodik navigation.
 */
object YuberaEndpoints {
    const val SITE_ORIGIN = "https://yume-website.yumesupport833.workers.dev"
    const val API_BASE_URL = "$SITE_ORIGIN/"
}
