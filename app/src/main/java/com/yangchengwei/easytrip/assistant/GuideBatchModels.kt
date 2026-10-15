package com.yangchengwei.easytrip.assistant

import java.io.File
import okhttp3.HttpUrl.Companion.toHttpUrl

data class GuideBatchPolicy(
    val bodyLimit: Long, val outputTokens: Int,
    val callTimeoutMillis: Long = 180_000, val responseLimit: Long = 2L * 1024 * 1024,
)
object GuideBatchPolicies {
    fun forConfig(config: ProviderConfig): GuideBatchPolicy {
        val current = config.validated()
        val url = current.baseUrl.toHttpUrl()
        val official = url.host == "api.deepseek.com" && url.port == 443 &&
            current.model == "deepseek-flash"
        return GuideBatchPolicy((if (official) 16L else 6L) * 1024 * 1024, if (official) 32768 else 8192)
    }
}
data class GuideSource(val token: String, val localId: String, val ordinal: Int?) {
    init {
        require(localId.isNotBlank())
        require(if (ordinal == null) token == "text" && localId == "text"
            else ordinal in 1..20 && token == "image_$ordinal")
    }
}
data class GuideContext(val text: String, val city: String, val scope: AssistantCityScope?) {
    override fun toString() = "GuideContext(redacted)"
}
data class GuidePreparedSource(val source: GuideSource, val file: File?,
    val byteCount: Long? = file?.length(), val sourceFingerprint: String? = null)
data class GuideGroup(val index: Int, val sources: List<GuidePreparedSource>)
data class GuideGroupPlan(val groups: List<GuideGroup>, val failures: List<ParsedSource>)
data class GuideGroupDecode(val sources: List<ParsedSource>, val repairableSourceIds: Set<String>)
interface GuideMaterialParser {
    suspend fun parse(request: AssistantInputRequest, onSource: (ParsedSource) -> Unit,
        onProgress: (SourceProgress) -> Unit): ParsedInputBatch
}
