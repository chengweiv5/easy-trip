package com.yangchengwei.easytrip.assistant

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class GuideEvidenceBatchTest {
    @get:Rule val temp = TemporaryFolder()
    @Test fun full401EvidenceIsExplicitOverflowRatherThanSuccessfulPrefix() = runBlocking {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val result = buildJsonObject { putJsonArray("sources") {
                (1..5).forEach { i -> addJsonObject {
                    put("sourceId", "image_$i"); put("status", "OK"); putJsonArray("evidence") {
                        repeat(if (i == 5) 1 else 100) { n -> addJsonObject {
                            put("rawQuote", "公园$n"); put("kind", "AUTHOR_LABEL"); put("basis", "AUTHOR_MARK")
                            put("uncertain", false); put("detail", "")
                        } }
                    }
                } }
            } }
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200)
                .message("OK").body(evidenceReply(result.toString()).toResponseBody()).build()
        }.build()
        val parser = BatchedGuideMaterialParser(ProviderConfig("https://example.invalid", "fixture", "not-real"),
            GuideBatchFiles(temp.root, GuideTestImages(), Mutex()), GuideBatchBody(EvidenceGuideWireProtocol),
            AssistantTransport(client), GuideBatchScheduler(), protocol = EvidenceGuideWireProtocol)
        val result = parser.parse(guideRequest(5).copy(text = "", guideVersion = GuideProtocolVersion.EVIDENCE_V2,
            guideBinding = GuideBatchBinding("b", 0, "北市")), {}) {}
        assertTrue(result.overflow)
        assertEquals(401, result.sources.sumOf { it.evidence.size })
        assertTrue(temp.root.listFiles()!!.isEmpty())
    }
    @Test fun sixImagesUseSharedSingleRequestWithFingerprintsAndNoPlaceholderIntents() = runBlocking {
        var calls = 0
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            calls++
            val buffer = Buffer(); chain.request().body!!.writeTo(buffer)
            val body = Json.parseToJsonElement(buffer.readUtf8()).jsonObject
            assertEquals("extract_guide_evidence", body["tools"]!!.jsonArray.single().jsonObject["function"]!!.jsonObject.text("name"))
            val result = buildJsonObject { putJsonArray("sources") {
                (1..6).forEach { i -> addJsonObject {
                    put("sourceId", "image_$i"); put("status", "OK"); putJsonArray("evidence") { addJsonObject {
                        put("rawQuote", "公园$i"); put("kind", "AUTHOR_LABEL"); put("basis", "AUTHOR_MARK")
                        put("uncertain", false); put("detail", "")
                    } }
                } }
            } }
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200)
                .message("OK").body(evidenceReply(result.toString()).toResponseBody()).build()
        }.build()
        val parser = BatchedGuideMaterialParser(ProviderConfig("https://example.invalid", "fixture", "not-real"),
            GuideBatchFiles(temp.root, GuideTestImages(), Mutex()), GuideBatchBody(EvidenceGuideWireProtocol),
            AssistantTransport(client), GuideBatchScheduler(), protocol = EvidenceGuideWireProtocol)
        val result = parser.parse(guideRequest(6).copy(text = "", guideVersion = GuideProtocolVersion.EVIDENCE_V2,
            guideBinding = GuideBatchBinding("b", 0, "北市")), {}) {}
        assertEquals(1, calls)
        assertEquals(6, result.sources.size)
        assertTrue(result.sources.all { it.failure == null && it.intents.isEmpty() && it.evidence.size == 1 })
        assertTrue(result.sources.all { it.evidence.single().sourceFingerprint.length == 64 })
        assertTrue(temp.root.listFiles()!!.isEmpty())
    }
}
