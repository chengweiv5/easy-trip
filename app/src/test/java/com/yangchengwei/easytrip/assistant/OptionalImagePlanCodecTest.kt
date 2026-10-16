package com.yangchengwei.easytrip.assistant

import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class OptionalImagePlanCodecTest {
    private val id = "07ff3c98-ade4-4048-87a5-05ac54f8eec6"
    @Test fun skippedSourceSurvivesWithoutAFileAndRetainsItsOriginalOrdinal() {
        val plan = OptionalImagePlan(listOf(OptionalImageAsset(id, 2, 4,
            "https://dimg04.c-ctrip.com/images/a.jpg", OptionalImageStatus.SKIPPED,
            skip = OptionalImageSkip.DOWNLOAD)), expectedCount = 8, hasUnknownOmissions = true)
        assertEquals(plan, OptionalImagePlanCodec.decode(OptionalImagePlanCodec.encode(plan), AssistantLinkPlatform.CTRIP))
        assertThrows(IllegalArgumentException::class.java) { plan.copy(assets = plan.assets + plan.assets) }
        assertThrows(IllegalArgumentException::class.java) { plan.assets.single().copy(status = OptionalImageStatus.AVAILABLE) }
        assertThrows(IllegalArgumentException::class.java) {
            OptionalImagePlanCodec.decode(buildJsonObject { put("version", 77) }, null)
        }
    }
}
