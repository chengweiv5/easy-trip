package com.yangchengwei.easytrip.assistant

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.*
import org.junit.Test

class GuideEvidenceResolverTest {
    @Test fun partialResultUsesOneTextOnly64kRequestAndDedicatedTransportBudget() = runBlocking {
        var calls = 0
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            calls++
            assertEquals(600_000, chain.readTimeoutMillis())
            assertEquals(60_000, chain.writeTimeoutMillis())
            assertEquals(15_000, chain.connectTimeoutMillis())
            assertEquals(600_000, chain.call().timeout().timeoutNanos() / 1_000_000)
            val buffer = Buffer(); chain.request().body!!.writeTo(buffer)
            val body = buffer.readUtf8()
            assertFalse(body.contains("image_url"))
            val json = Json.parseToJsonElement(body).jsonObject
            assertEquals(65536, json.getValue("max_tokens").jsonPrimitive.int)
            assertEquals(0, json.getValue("temperature").jsonPrimitive.int)
            assertEquals("disabled", json.getValue("thinking").jsonObject.text("type"))
            val reply = entityReply("""{"evidenceResults":[
              {"e":0,"status":"PARSED","mentions":[${classifiedMentionWire("公园")}]},
              {"e":1,"status":"REVIEW"}],"groups":[
              {"members":[[0,0]],"relation":"SAME_LITERAL","evidenceRefs":[0],"frozenAnchors":[]}]}""")
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200)
                .message("OK").body(reply.toResponseBody()).build()
        }.build()
        val resolver = ModelGuideEvidenceResolver(
            { ProviderConfig("https://api.deepseek.com", "deepseek-flash", "not-real") }, AssistantTransport(client))
        val r = resolver.resolve(listOf(fixtureEvidence("公园", 0), fixtureEvidence("北湖（北山南门）", 1)),
            GuideContext("", "北市", null), emptyList())
        assertEquals(1, calls)
        assertEquals(listOf("公园"), r.places.map { it.intent.query })
        assertEquals(setOf("e1"), r.pending.flatMap { it.evidenceIds }.toSet())
        assertEquals(0, resolver.resolve(emptyList(), GuideContext("", "", null), emptyList()).places.size)
        assertEquals(1, calls)
    }

    @Test fun productionResolverCannotBypassClassificationAndKeepsIndependentSuccess() = runBlocking {
        val quotes = listOf("历史博物馆", "推荐山间客栈", "内部舞台", "未知场馆")
        val args = """{"evidenceResults":[
          {"e":0,"status":"PARSED","mentions":[${classifiedMentionWire(quotes[0])}]},
          {"e":1,"status":"PARSED","mentions":[${classifiedMentionWire("山间客栈", "LODGING", quote = quotes[1])}]},
          {"e":2,"status":"PARSED","mentions":[${classifiedMentionWire(quotes[2], granularity = "INTERNAL_DETAIL")}]},
          {"e":3,"status":"PARSED","mentions":[${mentionWire(quotes[3])}]}],
          "groups":[${quotes.indices.joinToString(",") {
            """{"members":[[$it,0]],"relation":"SAME_LITERAL","evidenceRefs":[$it],"frozenAnchors":[]}"""
          }}]}"""
        var calls = 0
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            calls++
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200)
                .message("OK").body(entityReply(args).toResponseBody()).build()
        }.build()
        val resolution = ModelGuideEvidenceResolver(
            { ProviderConfig("https://api.deepseek.com", "deepseek-flash", "fixture") }, AssistantTransport(client))
            .resolve(quotes.mapIndexed { index, quote -> fixtureEvidence(quote, index) },
                GuideContext("", "北市", null), emptyList())
        assertEquals(listOf("历史博物馆"), resolution.places.map { it.intent.query })
        assertEquals(setOf("e1", "e2"), resolution.excludedEvidenceIds)
        assertEquals(setOf("e3"), resolution.pending.flatMap { it.evidenceIds }.toSet())
        assertEquals(1, calls)
    }
}
