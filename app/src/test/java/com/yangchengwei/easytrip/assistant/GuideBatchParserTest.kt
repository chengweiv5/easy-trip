package com.yangchengwei.easytrip.assistant

import java.io.File
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class GuideBatchParserTest {
    @get:Rule val temporary = TemporaryFolder()
    private val config = ProviderConfig("https://example.invalid", "fixture", "not-real")
    private fun client(reply: (String) -> String) = OkHttpClient.Builder().addInterceptor { chain ->
        val body = Buffer(); chain.request().body!!.writeTo(body)
        Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
            .code(200).message("OK").body(reply(body.readUtf8()).toResponseBody()).build()
    }.build()
    private fun parser(http: OkHttpClient) = BatchedGuideMaterialParser(config,
        GuideBatchFiles(temporary.root, GuideTestImages(), Mutex()), GuideBatchBody(),
        AssistantTransport(http), GuideBatchScheduler())
    private fun receipt(ids: List<String>) = guideReply(buildJsonObject {
        putJsonArray("sources") { ids.forEach { id -> addJsonObject {
            put("sourceId", id); put("status", "NO_PLACES"); putJsonArray("items") {}
        } } }
    }.toString())

    @Test fun fourHundredCandidatesAcrossGroupsAreCompleteButFifthGroupOverflows() = runBlocking {
        for (count in listOf(4, 5)) {
            val calls = java.util.concurrent.atomic.AtomicInteger()
            val http = client { request ->
                calls.incrementAndGet()
                val token = Regex("目标(image_\\d+)：").find(request)!!.groupValues[1]
                guideReply(buildJsonObject { putJsonArray("sources") { addJsonObject {
                    put("sourceId", token); put("status", "OK")
                    putJsonArray("items") { repeat(100) { index -> addJsonObject {
                        val name = "美术馆${token}_$index"
                        put("query", name); put("city", "北城"); put("sourceSpan", name); put("uncertain", false)
                        put("targetBasis", "AUTHOR_MARK"); put("targetDetail", "编号"); put("entityRole", "PLACE"); put("parentName", "")
                    } } }
                } } }.toString())
            }
            val parser = BatchedGuideMaterialParser(config,
                GuideBatchFiles(temporary.root, GuideTestImages {
                    PreparedImage(it, "image/jpeg", ByteArray(3 * 1024 * 1024))
                }, Mutex()), GuideBatchBody(), AssistantTransport(http), GuideBatchScheduler())
            val result = parser.parse(guideRequest(count).copy(text = ""), {}) {}
            assertEquals(count, calls.get())
            assertEquals(count * 100, result.sources.sumOf { it.intents.size })
            assertTrue(result.sources.all { it.failure == null })
            assertEquals(count > 4, result.overflow)
            assertTrue(temporary.root.listFiles()!!.isEmpty())
        }
    }

    @Test fun cancelledInflightResponseCannotPublishAndOwnedFilesAreReleased() = runBlocking {
        val entered = java.util.concurrent.CountDownLatch(1)
        val release = java.util.concurrent.CountDownLatch(1)
        val events = mutableListOf<ParsedSource>()
        val http = client { entered.countDown(); check(release.await(10, java.util.concurrent.TimeUnit.SECONDS))
            receipt(listOf("text", "image_1")) }
        val job = launch { parser(http).parse(guideRequest(1), events::add) {} }
        try {
            withContext(Dispatchers.IO) { check(entered.await(10, java.util.concurrent.TimeUnit.SECONDS)) }
            job.cancel()
            release.countDown()
            withTimeout(10_000) { job.join() }
            assertTrue(events.isEmpty())
            assertTrue(temporary.root.listFiles()!!.isEmpty())
        } finally { release.countDown(); job.cancelAndJoin() }
    }

    @Test fun twentySmallImagesAndTextUseOneCallWithOrderedReceiptsAndCleanup() = runBlocking {
        var calls = 0
        val http = client { body ->
            calls++
            val blocks = Json.parseToJsonElement(body).jsonObject["messages"]!!.jsonArray.flatMap {
                it.jsonObject["content"] as? JsonArray ?: emptyList()
            }
            assertEquals(20, blocks.count { it.jsonObject["type"]?.jsonPrimitive?.content == "image_url" })
            receipt(listOf("text") + (1..20).map { "image_$it" })
        }
        val events = mutableListOf<ParsedSource>()
        val result = parser(http).parse(guideRequest(20), events::add) {}
        assertEquals(1, calls)
        assertEquals(listOf("text") + guideImages(20).map { it.id }, result.sources.map { it.sourceId })
        assertEquals(21, events.size)
        assertTrue(result.sources.all { it.failure == null })
        assertTrue(temporary.root.listFiles()!!.isEmpty())
    }

    @Test fun repairResendsOnlyMissingSourceAndSuccessfulReceiptsArePublishedOnce() = runBlocking {
        val requests = mutableListOf<String>()
        val events = mutableListOf<ParsedSource>()
        val http = client { request ->
            requests += request
            if (requests.size == 1) receipt(listOf("text", "image_1"))
            else {
                assertEquals(listOf("text", "local-1"), events.map { it.sourceId })
                receipt(listOf("image_2"))
            }
        }
        val result = parser(http).parse(guideRequest(), events::add) {}
        assertEquals(2, requests.size)
        val targets = Json.parseToJsonElement(requests.last()).jsonObject["tools"]!!.jsonArray.single()
            .jsonObject["function"]!!.jsonObject["parameters"]!!.jsonObject["properties"]!!.jsonObject
            .getValue("sources").jsonObject["items"]!!.jsonObject["properties"]!!.jsonObject
            .getValue("sourceId").jsonObject["enum"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertEquals(listOf("image_2"), targets)
        assertTrue(result.sources.all { it.failure == null })
        assertEquals(3, events.size)
    }

    @Test fun repairNetworkFailureKeepsSuccessfulTextAndDoesNotTryAThirdTime() = runBlocking {
        var calls = 0
        val http = client {
            calls++
            if (calls == 1) receipt(listOf("text"))
            else throw java.io.IOException("fixture")
        }
        val result = parser(http).parse(guideRequest(1), {}) {}
        assertEquals(2, calls)
        assertNull(result.sources.first().failure)
        assertEquals(AssistantFailureCode.NETWORK, result.sources.last().failure?.code)
    }

    @Test fun cleanupUnableToDeleteUnknownChildDoesNotRevokeCompletedReceipts() = runBlocking {
        val http = client { receipt(listOf("text", "image_1")) }
        val result = parser(http).parse(guideRequest(1), {
            val workspace = temporary.root.listFiles()!!.single()
            File(workspace, "unknown-directory").mkdir()
        }) {}
        assertTrue(result.sources.all { it.failure == null })
        assertTrue(File(temporary.root.listFiles()!!.single(), ".guide-owner").isFile)
    }

    @Test fun separateParsersShareTwoSlotsAndAtMostTwoLiveBodyFiles() = runBlocking {
        val scheduler = GuideBatchScheduler()
        val entered = java.util.concurrent.CountDownLatch(2)
        val release = java.util.concurrent.CountDownLatch(1)
        val calls = java.util.concurrent.atomic.AtomicInteger()
        val http = client {
            calls.incrementAndGet(); entered.countDown()
            check(release.await(10, java.util.concurrent.TimeUnit.SECONDS))
            receipt(listOf("text", "image_1"))
        }
        val jobs = (1..3).map {
            val parser = BatchedGuideMaterialParser(config,
                GuideBatchFiles(temporary.root, GuideTestImages(), Mutex()), GuideBatchBody(),
                AssistantTransport(http), scheduler)
            async { parser.parse(guideRequest(1).copy(sessionId = "session-$it"), {}) {} }
        }
        try {
            withContext(Dispatchers.IO) { check(entered.await(10, java.util.concurrent.TimeUnit.SECONDS)) }
            assertEquals(2, calls.get())
            assertEquals(2, temporary.root.walkTopDown().count { it.name.startsWith("body-") && it.extension == "json" })
            release.countDown(); jobs.awaitAll()
            assertEquals(3, calls.get())
            assertTrue(temporary.root.listFiles()!!.isEmpty())
        } finally { release.countDown(); jobs.forEach { it.cancelAndJoin() } }
    }

    @Test fun truncatedOrInvalidEnvelopeDoesNotSalvageOrRepairItsGroup() = runBlocking {
        for (response in listOf(receipt(listOf("text", "image_1")).replace("tool_calls\",\"message", "length\",\"message"),
            "{", guideReply("""{"sources":[]}""", "length"))) {
            var calls = 0
            val http = client { calls++; response }
            val result = parser(http).parse(guideRequest(1), {}) {}
            assertEquals(1, calls)
            assertTrue(result.sources.all { it.failure != null })
        }
    }

    @Test fun authStopsQueuedGroupButPreservesSecondInflightSuccessAndSendEvidence() = rejectedGroup(false)
    @Test fun capabilityCacheWriteFailureDoesNotCancelInflightSuccessOrLoseRejection() = rejectedGroup(true)

    private fun rejectedGroup(capabilityWriteFails: Boolean) = runBlocking {
        val bothSent = java.util.concurrent.CountDownLatch(2)
        val authReceived = java.util.concurrent.CountDownLatch(1)
        val calls = java.util.concurrent.atomic.AtomicInteger()
        val http = OkHttpClient.Builder().addInterceptor { chain ->
            calls.incrementAndGet()
            val out = Buffer(); chain.request().body!!.writeTo(out)
            bothSent.countDown()
            check(bothSent.await(10, java.util.concurrent.TimeUnit.SECONDS))
            val request = out.readUtf8()
            val id = Regex("目标(image_\\d+)：").find(request)!!.groupValues[1]
            if (id != "image_1") check(authReceived.await(10, java.util.concurrent.TimeUnit.SECONDS))
            val ids = if (request.contains("目标text：")) listOf("text", id) else listOf(id)
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(if (id == "image_1") { if (capabilityWriteFails) 400 else 401 } else 200).message("fixture")
                .body((if (id == "image_1") {
                    if (capabilityWriteFails) """{"error":{"code":"unsupported_image"}}""" else "{}"
                } else receipt(ids)).toResponseBody()).build()
        }.build()
        val parser = BatchedGuideMaterialParser(config, GuideBatchFiles(temporary.root,
            GuideTestImages { PreparedImage(it, "image/jpeg", ByteArray(3 * 1024 * 1024)) }, Mutex()),
            GuideBatchBody(), AssistantTransport(http), GuideBatchScheduler(),
            onUnsupportedImage = { if (capabilityWriteFails) throw java.io.IOException("cache unavailable") })
        val result = withTimeout(20_000) { parser.parse(guideRequest(3), {
            if (it.failure?.code in setOf(AssistantFailureCode.AUTH, AssistantFailureCode.UNSUPPORTED_IMAGE)) authReceived.countDown()
        }) {} }
        assertEquals(2, calls.get())
        assertEquals(listOf("text", "local-1", "local-2", "local-3"), result.sources.map { it.sourceId })
        assertEquals(1, result.sources.count { it.failure == null })
        assertEquals(SendEvidence.NOT_DISPATCHED, result.sources.last().failure?.sendEvidence)
        assertTrue(result.sources.any { it.failure?.sendEvidence == SendEvidence.RESPONSE_RECEIVED })
        assertTrue(temporary.root.listFiles()!!.isEmpty())
    }
}
