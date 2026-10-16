package com.yangchengwei.easytrip.assistant

import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class GuideEntityPromptRegressionTest {
    @Test fun serializedProductionPromptExcludesLodgingWithoutTheOldRetentionInstruction() {
        val system = systemPrompt()
        assertTrue(system.contains("住宿、餐饮、交通、内部细节及背景均剔除"))
        assertFalse(system.contains("已识别住宿保留"))
    }

    @Test fun serializedPromptKeepsDestinationSeparateFromItsNavigationQualifier() {
        val system = systemPrompt()
        assertTrue(system.contains("不能因为带入口或出口而把主体景点一并剔除"))
        assertTrue(system.contains("主体名称必须是本条原文中连续出现的原字"))
        assertTrue(system.contains("路线中已安排游览的起点和终点仍是游览目的地"))
        assertTrue(system.contains("不能仅因箭头、步行或公交标记而判为TRANSPORT"))
        assertFalse(system.contains("徐州"))
        assertFalse(system.contains("青州"))
    }

    private fun systemPrompt(): String {
        val request = GuideEntityProtocol.request(ProviderConfig("https://api.deepseek.com",
            "deepseek-flash", "offline"), listOf(fixtureEvidence("历史馆入口", 0)),
            GuideContext("", "北城", null), emptyList())
        return Json.parseToJsonElement(request).jsonObject.getValue("messages").jsonArray
            .first().jsonObject.getValue("content").jsonPrimitive.content
    }
}
