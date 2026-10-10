package com.yangchengwei.easytrip.assistant

import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.domain.PlaceCategory
import okhttp3.HttpUrl.Companion.toHttpUrl

class AssistantFailure(val userMessage: String) : Exception(userMessage)

class ProviderConfig(val baseUrl: String, val model: String, val apiKey: String) {
    val chatUrl: String get() = baseUrl.trimEnd('/') + "/chat/completions"
    fun validated(): ProviderConfig {
        val url = baseUrl.trim().toHttpUrl()
        require(url.isHttps && url.username.isEmpty() && url.password.isEmpty() &&
            url.query == null && url.fragment == null) { "请输入不含账号、参数或片段的 HTTPS Base URL" }
        require(!url.encodedPath.trimEnd('/').endsWith("/chat/completions")) { "请填 Base URL，不要填写完整聊天接口" }
        require(model.isNotBlank() && model.length <= 100 && model.none(Char::isISOControl)) { "请填写有效模型名" }
        require(apiKey.isNotBlank() && apiKey.length <= 4096 && apiKey.none(Char::isISOControl)) { "请填写有效 API Key" }
        return ProviderConfig(url.toString().trimEnd('/'), model.trim(), apiKey.trim())
    }
    override fun toString() = "ProviderConfig(credentials=redacted)"
}

data class PlaceIntent(val query: String, val city: String, val sourceSpan: String, val usesDefaultCity: Boolean = false)
fun interface PlaceIntentParser {
    suspend fun parse(input: String, defaultCity: String): List<PlaceIntent>
}

enum class IntakeStatus { WAITING, QUERYING, NEED_CITY, AMBIGUOUS, NOT_FOUND, FAILED, READY, ALREADY_SAVED, DUPLICATE, CANCELLED }
data class IntakeItem(
    val id: String,
    val label: String,
    val intent: PlaceIntent,
    val generation: Int = 0,
    val status: IntakeStatus = IntakeStatus.WAITING,
    val candidates: List<PlaceCandidate> = emptyList(),
    val poi: PlaceCandidate? = null,
    val category: PlaceCategory = PlaceCategory.OTHER,
    val detail: String? = null,
    val cityConflict: Boolean = false,
)

data class PlaceAssistantState(
    val input: String = "",
    val city: String = "",
    val items: List<IntakeItem> = emptyList(),
    val selected: Set<String> = emptySet(),
    val busy: Boolean = false,
    val saving: Boolean = false,
    val mapReady: Boolean = false,
    val configured: Boolean = false,
    val consent: Boolean = false,
    val sendConsent: Boolean = false,
    val confirmation: ImportConfirmation? = null,
    val receipt: ImportReceipt? = null,
    val error: String? = null,
) {
    val eligible get() = items.filter { it.status == IntakeStatus.READY }
    val hasDraft get() = input.isNotBlank() || items.isNotEmpty()
}
