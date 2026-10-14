package com.yangchengwei.easytrip.assistant

import com.yangchengwei.easytrip.assistant.cityfirst.GuideDestinationProtocol
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class GuideDestinationProtocolTest {
    private fun result(city: String, source: String = "text", span: String = "青州周末") =
        """{"cities":[{"name":"$city","sourceId":"$source","sourceSpan":"$span"}],"suggestedName":"周末之旅"}"""
    @Test fun onlyCitiesWithActualSourceEvidenceAreAccepted() {
        assertEquals("青州", GuideDestinationProtocol.decodeArguments(result("青州"), "青州周末", emptySet()).cities.single().name)
        assertTrue(runCatching { GuideDestinationProtocol.decodeArguments(result("济南", span = "济南游"), "青州周末", emptySet()) }.isFailure)
        assertTrue(runCatching { GuideDestinationProtocol.decodeArguments(result("青州", "invented"), "青州周末", emptySet()) }.isFailure)
    }
    @Test fun cityStageCannotReturnPlacesOrActionsAndSystemDoesNotEmbedTripData() {
        assertTrue(runCatching {
            GuideDestinationProtocol.decodeArguments("""{"cities":[],"suggestedName":"","places":["景点"]}""", "", emptySet())
        }.isFailure)
        val config = ProviderConfig("https://example.invalid", "fixture", "not-real")
        val a = Json.parseToJsonElement(GuideDestinationProtocol.request(config, "青州周末")).jsonObject
        val b = Json.parseToJsonElement(GuideDestinationProtocol.request(config, "南京周末")).jsonObject
        assertEquals(a["messages"]!!.jsonArray.first(), b["messages"]!!.jsonArray.first())
        assertFalse(a["messages"]!!.jsonArray.first().toString().contains("青州"))
        assertEquals("identify_trip_destination", a["tool_choice"]!!.jsonObject["function"]!!.jsonObject["name"]!!.jsonPrimitive.content)
    }
    @Test fun shareTitleAndAuthorDateAreNotUserTravelDates() {
        assertEquals("", tripUserIntent("青州三日游 2026-10-12 https://xhslink.cn/o/example"))
        assertEquals("我下周去玩三天", tripUserIntent("我下周去玩三天\n青州三日游 https://xhslink.cn/o/example"))
    }
    @Test fun forcedDestinationToolUsesDeepSeekNonThinkingMode() {
        val request = Json.parseToJsonElement(GuideDestinationProtocol.request(
            ProviderConfig("https://api.deepseek.com", "deepseek-flash", "fixture"), "青州攻略")).jsonObject
        assertEquals("disabled", request["thinking"]?.jsonObject?.get("type")?.jsonPrimitive?.content)
    }
}
