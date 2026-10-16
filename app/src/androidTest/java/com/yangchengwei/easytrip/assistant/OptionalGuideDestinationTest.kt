package com.yangchengwei.easytrip.assistant

import com.yangchengwei.easytrip.assistant.cityfirst.ModelGuideDestinationInterpreter
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test

/** Actual city protocol/transport; deterministic local responses, no external model. */
class OptionalGuideDestinationTest {
    private val config = ProviderConfig("https://example.invalid", "fixture", "not-real")
    private fun image(n: Int) = AssistantImage(UUID.randomUUID().toString(), n, "image/jpeg", 3, 1, 1)
    private fun response(id: String? = null) = batchedDeviceReply(buildJsonObject {
        putJsonArray("cities") { if (id != null) addJsonObject {
            put("name", "北城"); put("sourceId", id); put("sourceSpan", "北城")
        } }
        put("suggestedName", if (id == null) "" else "北城之旅")
    }, "identify_trip_destination")
    @Test fun firstBadImageDoesNotBlockSecondCityAndTextHasNoImagePayload() = runBlocking {
        val first = image(1); val second = image(2); val requests = AtomicInteger()
        val http = OkHttpClient.Builder().addInterceptor { chain ->
            val body = okio.Buffer(); chain.request().body!!.writeTo(body)
            val content = Json.parseToJsonElement(body.readUtf8()).jsonObject.getValue("messages")
                .jsonArray[1].jsonObject.getValue("content")
            val n = requests.incrementAndGet()
            if (n == 1) assertTrue(content is JsonPrimitive)
            else assertEquals(1, content.jsonArray.count { it.jsonObject["type"]?.jsonPrimitive?.content == "image_url" })
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body(response(if (n == 1) null else second.id).toResponseBody()).build()
        }.build()
        val skipped = mutableListOf<String>()
        val result = ModelGuideDestinationInterpreter({ config }, AssistantTransport(http)).interpret("攻略", images = {
            listOf(first to suspend { throw AssistantFailure("bad image") },
                second to suspend { PreparedImage(second.id, "image/jpeg", byteArrayOf(1, 2, 3)) })
        }, onImageFailure = { id, _ -> skipped += id })
        assertEquals(listOf(first.id), skipped)
        assertEquals("北城", result.cities.single().name)
        assertEquals(2, requests.get())
    }
    @Test fun laterBadImageKeepsPreviouslyIdentifiedCity() = runBlocking {
        val first = image(1); val second = image(2)
        val http = OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body(response(first.id).toResponseBody()).build()
        }.build()
        val failed = mutableListOf<String>()
        val result = ModelGuideDestinationInterpreter({ config }, AssistantTransport(http)).interpret("", images = {
            listOf(first to suspend { PreparedImage(first.id, "image/jpeg", byteArrayOf(1, 2, 3)) },
                second to suspend { throw AssistantFailure("bad image") })
        }, onImageFailure = { id, _ -> failed += id })
        assertEquals("北城", result.cities.single().name)
        assertEquals(listOf(second.id), failed)
    }
    @Test fun authenticationStopsFurtherImagesAndCancellationIsNotASkip() = runBlocking {
        val first = image(1); val second = image(2); var secondPrepared = false
        val http = OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(401).message("Unauthorized")
                .body("{}".toResponseBody()).build()
        }.build()
        val interpreter = ModelGuideDestinationInterpreter({ config }, AssistantTransport(http))
        val result = runCatching { interpreter.interpret("", images = {
            listOf(first to suspend { PreparedImage(first.id, "image/jpeg", byteArrayOf(1)) },
                second to suspend { secondPrepared = true; PreparedImage(second.id, "image/jpeg", byteArrayOf(1)) })
        }) }
        assertEquals(AssistantFailureCode.AUTH, (result.exceptionOrNull() as AssistantFailure).code)
        assertFalse(secondPrepared)
        val entered = CompletableDeferred<Unit>(); var skips = 0
        val job = launch { interpreter.interpret("", images = {
            listOf(first to suspend { entered.complete(Unit); awaitCancellation() })
        }, onImageFailure = { _, _ -> skips++ }) }
        entered.await(); job.cancelAndJoin(); assertEquals(0, skips)
    }
    @Test fun imageStageDeadlineKeepsCityAndSkipsOnlyUnfinishedImage() = runBlocking {
        val first = image(1); val second = image(2)
        val requests = AtomicInteger()
        val http = OkHttpClient.Builder().addInterceptor { chain ->
            requests.incrementAndGet()
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body(response(first.id).toResponseBody()).build()
        }.build()
        val skipped = mutableListOf<Pair<String, OptionalImageSkip>>()
        val start = android.os.SystemClock.elapsedRealtime()
        val result = ModelGuideDestinationInterpreter({ config }, AssistantTransport(http)).interpret("", images = {
            listOf(first to suspend { PreparedImage(first.id, "image/jpeg", byteArrayOf(1)) },
                second to suspend { awaitCancellation() })
        }, onImageFailure = { id, reason -> skipped += id to reason })
        val elapsed = android.os.SystemClock.elapsedRealtime() - start
        assertEquals("北城", result.cities.single().name)
        assertEquals(listOf(second.id to OptionalImageSkip.TIMEOUT), skipped)
        assertEquals(1, requests.get())
        assertTrue("actual image deadline: $elapsed ms", elapsed in 179_000L..210_000L)
    }
}
