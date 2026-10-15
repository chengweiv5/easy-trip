package com.yangchengwei.easytrip.assistant

import okhttp3.HttpUrl.Companion.toHttpUrl

data class AssistantTextPolicy(
    val callMillis: Long, val readMillis: Long, val writeMillis: Long,
    val connectMillis: Long, val responseBytes: Long,
)

/** Opt-in for the new protocol only. No change to ordinary or recovered v1 requests. */
object GuideEvidenceBudget {
    const val OUTPUT_TOKENS = 65536
    val transport = AssistantTextPolicy(600_000, 600_000, 60_000, 15_000, 2L * 1024 * 1024)
    fun supports(config: ProviderConfig): Boolean {
        val c = config.validated()
        val url = c.baseUrl.toHttpUrl()
        return url.isHttps && url.host == "api.deepseek.com" && url.port == 443 && c.model == "deepseek-flash"
    }
    fun batchTimeoutMillis(images: Int): Long = maxOf(AssistantImagePolicy.batchTimeoutMillis(images), 1_500_000)
        .coerceAtMost(1_800_000)
}
