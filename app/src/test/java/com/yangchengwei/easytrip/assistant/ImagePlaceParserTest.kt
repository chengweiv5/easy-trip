package com.yangchengwei.easytrip.assistant

import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class ImagePlaceParserTest {
    private fun response(args: String, tool: String = "search_place_batch") = buildJsonObject {
        putJsonArray("choices") { addJsonObject {
            put("finish_reason", "tool_calls")
            putJsonObject("message") { putJsonArray("tool_calls") { addJsonObject {
                put("type", "function")
                putJsonObject("function") { put("name", tool); put("arguments", args) }
            } } }
        } }
    }.toString()

    @Test fun sourceComesFromLocalImageIdentityNotModelAuthority() {
        val result = ImageToolProtocol.decode(response("""{"items":[{"query":"灵隐寺","city":"杭州","sourceSpan":"杭州 灵隐寺","uncertain":true}],"overflow":false}"""), "local-image", 2)
        assertEquals("灵隐寺", result.single().query)
        assertEquals(listOf(IntakeSource.Image("local-image", 2, "杭州 灵隐寺")), result.single().sources)
        assertTrue(result.single().needsReview)
    }
    @Test fun modelCannotReturnWritesCoordinatesOrUngroundedNames() {
        val valid = """{"items":[{"query":"灵隐寺","city":"杭州","sourceSpan":"杭州 灵隐寺","uncertain":false}],"overflow":false}"""
        listOf(response(valid, "save_places"), response(valid.replace("\"uncertain\":false", "\"uncertain\":false,\"lat\":30")),
            response(valid.replace("\"query\":\"灵隐寺\"", "\"query\":\"故宫\"")),
            response(valid.replace("\"uncertain\":false", "\"uncertain\":\"false\"")),
            response(valid.replace("\"overflow\":false", "\"overflow\":true"))).forEach {
            assertThrows(AssistantFailure::class.java) { ImageToolProtocol.decode(it, "id", 1) }
        }
    }
    @Test fun oversizedToolItemsAreOverflowEvenWhenModelFlagIsFalse() {
        val item = """{"query":"灵隐寺","city":"杭州","sourceSpan":"杭州 灵隐寺","uncertain":false}"""
        val failure = assertThrows(AssistantFailure::class.java) {
            ImageToolProtocol.decode(response("""{"items":[${List(21) { item }.joinToString(",")}],"overflow":false}"""), "id", 1)
        }
        assertEquals(AssistantFailureCode.OVERFLOW, failure.code)
    }
    @Test fun requestContainsOneImageAndNeverEmbedsApiKey() {
        val raw = ImageToolProtocol.request(ProviderConfig("https://example.test", "vision", "private-secret"),
            PreparedImage("id", "image/jpeg", byteArrayOf(1,2,3)), 1, "仅作说明", "杭州")
        val request = Json.parseToJsonElement(raw).jsonObject
        assertNull(request["thinking"])
        val content = request["messages"]!!.jsonArray[1].jsonObject["content"]!!.jsonArray
        assertEquals(2, content.size)
        assertEquals("data:image/jpeg;base64,AQID", content[1].jsonObject["image_url"]!!.jsonObject["url"]!!.jsonPrimitive.content)
        assertFalse(raw.contains("private-secret"))
        assertFalse(raw.contains("save_places"))
    }
}
