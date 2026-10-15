package com.yangchengwei.easytrip.assistant

import kotlinx.serialization.json.JsonObject

interface GuideWireProtocol {
    fun metadata(config: ProviderConfig, context: GuideContext, sources: List<GuideSource>,
        policy: GuideBatchPolicy, repair: Boolean): JsonObject
    fun decode(response: String, context: GuideContext, sources: List<GuidePreparedSource>,
        binding: GuideBatchBinding?): GuideGroupDecode
}
object LegacyGuideWireProtocol : GuideWireProtocol {
    override fun metadata(config: ProviderConfig, context: GuideContext, sources: List<GuideSource>,
        policy: GuideBatchPolicy, repair: Boolean) = GuideBatchProtocol.metadata(config, context, sources, policy, repair)
    override fun decode(response: String, context: GuideContext, sources: List<GuidePreparedSource>,
        binding: GuideBatchBinding?) = GuideBatchProtocol.decode(response, context, sources.map { it.source })
}
object EvidenceGuideWireProtocol : GuideWireProtocol {
    override fun metadata(config: ProviderConfig, context: GuideContext, sources: List<GuideSource>,
        policy: GuideBatchPolicy, repair: Boolean) = GuideEvidenceProtocol.metadata(config, context, sources, policy, repair)
    override fun decode(response: String, context: GuideContext, sources: List<GuidePreparedSource>,
        binding: GuideBatchBinding?) = GuideEvidenceProtocol.decode(response, context, sources, requireNotNull(binding))
}
