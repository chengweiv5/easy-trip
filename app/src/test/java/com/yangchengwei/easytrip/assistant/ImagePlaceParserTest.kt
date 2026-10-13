package com.yangchengwei.easytrip.assistant

import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class ImagePlaceParserTest {
    @Test fun structuredAuthorEvidenceSurvivesImageDecodeIntoFinalNormalizationRequest() {
        val args = """{"items":[{"query":"北城美术馆","city":"北城","sourceSpan":"北城美术馆","uncertain":false,"targetBasis":"AUTHOR_MARK","targetDetail":"作者粉色标签与定位标记","entityRole":"PLACE","parentName":""}],"overflow":false}"""
        val decoded = ImageToolProtocol.decode(response(args), "image", 1)
        val request = PlaceNormalizationProtocol.request(ProviderConfig("https://example.test", "model", "secret"),
            decoded, "", "北城")
        val payload = Json.parseToJsonElement(Json.parseToJsonElement(request).jsonObject.getValue("messages")
            .jsonArray[1].jsonObject.getValue("content").jsonPrimitive.content).jsonObject
        val evidence = payload.getValue("candidates").jsonArray.single().jsonObject.getValue("evidence").jsonArray.single().jsonObject
        assertEquals("AUTHOR_MARK", evidence.getValue("targetBasis").jsonPrimitive.content)
        assertEquals("作者粉色标签与定位标记", evidence.getValue("targetDetail").jsonPrimitive.content)
    }
    @Test fun longExtractedArticleIsAcceptedWithoutTruncatingImageContext() = kotlinx.coroutines.runBlocking {
        val context = "青州古城".repeat(1000)
        var requestBody = ""
        val client = okhttp3.OkHttpClient.Builder().addInterceptor { chain ->
            val buffer = okio.Buffer()
            chain.request().body!!.writeTo(buffer)
            requestBody = buffer.readUtf8()
            okhttp3.Response.Builder().request(chain.request()).protocol(okhttp3.Protocol.HTTP_1_1).code(200).message("OK")
                .body(okhttp3.ResponseBody.create(null, response("""{"items":[{"query":"青州古城","city":"青州","sourceSpan":"青州古城","uncertain":false}],"overflow":false}"""))).build()
        }.build()
        val parser = ImagePlaceParser({ ProviderConfig("https://example.test", "vision", "test") }, AssistantTransport(client))
        assertEquals("青州古城", parser.parse(PreparedImage("id", "image/jpeg", byteArrayOf(1)), 1, context, "青州") {}.single().query)
        assertTrue(requestBody.contains(context))
    }
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
            ImageToolProtocol.decode(response("""{"items":[${List(101) { item }.joinToString(",")}],"overflow":false}"""), "id", 1)
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

    @Test fun linkMapExtractionRequiresAuthorIntentRatherThanBaseMapLabels() {
        val config = ProviderConfig("https://example.test", "vision", "private-secret")
        val image = PreparedImage("id", "image/jpeg", byteArrayOf(1))
        val linked = ImageToolProtocol.request(config, image, 2, "", "青州", 100, travelMaterials = true)
        assertTrue(linked.contains("先区分作者叠加内容与底图自带标注"))
        assertTrue(linked.contains("路线经过或贴近一个地名不等于在此停靠"))
        assertTrue(linked.contains("底图自带的绿色景区名"))
        assertTrue(linked.contains("不能仅凭颜色、字号、类别或是否带“村”判断"))
        val manual = ImageToolProtocol.request(config, image, 2, "", "青州")
        assertFalse(manual.contains("先区分作者叠加内容与底图自带标注"))
    }

    @Test fun evidenceCannotContainPartialFieldsUnsupportedTypesOrUnboundedInstructions() {
        val valid = """{"items":[{"query":"北城美术馆","city":"北城","sourceSpan":"北城美术馆","uncertain":false,"targetBasis":"AUTHOR_MARK","targetDetail":"作者标签","entityRole":"PLACE","parentName":""}],"overflow":false}"""
        listOf(
            valid.replace("\"AUTHOR_MARK\"", "\"SYSTEM_AUTHORITY\""),
            valid.replace(",\"parentName\":\"\"", ""),
            valid.replace("\"作者标签\"", "\"${"长".repeat(301)}\""),
            valid.replace("\"PLACE\"", "false"),
            valid.replace("\"北城美术馆\",\"city\"", "\"猜测新地点\",\"city\""),
        ).forEach { assertThrows(AssistantFailure::class.java) { ImageToolProtocol.decode(response(it), "i", 1) } }
    }
    @Test fun travelParserRejectsMissingEvidenceWhileOrdinaryImageParserRemainsCompatible() = kotlinx.coroutines.runBlocking {
        val old = response("""{"items":[{"query":"北城美术馆","city":"北城","sourceSpan":"北城美术馆","uncertain":false}],"overflow":false}""")
        val client = okhttp3.OkHttpClient.Builder().addInterceptor { chain ->
            okhttp3.Response.Builder().request(chain.request()).protocol(okhttp3.Protocol.HTTP_1_1).code(200).message("OK")
                .body(okhttp3.ResponseBody.create(null, old)).build()
        }.build()
        val config = { ProviderConfig("https://example.test", "vision", "test") }
        val image = PreparedImage("i", "image/jpeg", byteArrayOf(1))
        assertEquals(1, ImagePlaceParser(config, AssistantTransport(client)).parse(image, 1, "", "北城") {}.size)
        val failure = runCatching {
            ImagePlaceParser(config, AssistantTransport(client), 100, travelMaterials = true).parse(image, 1, "", "北城") {}
        }.exceptionOrNull()
        assertTrue(failure is AssistantFailure)
        assertEquals(AssistantFailureCode.PROTOCOL, (failure as AssistantFailure).code)
    }
}
