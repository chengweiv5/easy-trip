package com.yangchengwei.easytrip.assistant

import java.io.File
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class GuideBatchTransportTest {
    @get:Rule val temporary = TemporaryFolder()
    private val config = ProviderConfig("https://example.invalid", "fixture", "not-real")
    private val policy = GuideBatchPolicy(6L * 1024 * 1024, 8192)
    @Test fun fileBodyStreamsExactLengthAndUsesIndependentTimeouts() = runBlocking {
        val file = File(temporary.root, "body").apply { writeText("""{"中文":"正文"}""") }
        var seen = ""
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            assertEquals(180_000, chain.readTimeoutMillis())
            assertEquals(180_000, chain.writeTimeoutMillis())
            assertEquals(15_000, chain.connectTimeoutMillis())
            val body = chain.request().body!!
            val buffer = Buffer(); body.writeTo(buffer)
            assertEquals(body.contentLength(), buffer.size)
            seen = buffer.readUtf8()
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(200).message("OK").body("{}".toResponseBody()).build()
        }.build()
        assertEquals("{}", AssistantTransport(client).postFile(config, file, policy))
        assertEquals(file.readText(), seen)
        assertEquals(10_000, client.readTimeoutMillis)
    }

    @Test fun guideHasTwoMiBResponseLimitWhileLegacyStillHas256KiBAndSixMiBBody() = runBlocking {
        var calls = 0
        var responseBytes = 2 * 1024 * 1024
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            calls++
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(200).message("OK").body(" ".repeat(responseBytes).toResponseBody()).build()
        }.build()
        val transport = AssistantTransport(client)
        val file = File(temporary.root, "body").apply { writeText("{}") }
        assertEquals(responseBytes, transport.postFile(config, file, policy).length)
        responseBytes++
        assertEquals(AssistantFailureCode.PROTOCOL,
            (runCatching { transport.postFile(config, file, policy) }.exceptionOrNull() as AssistantFailure).code)
        responseBytes = 256 * 1024
        assertEquals(responseBytes, transport.post(config, "{}").length)
        responseBytes++
        assertTrue(runCatching { transport.post(config, "{}") }.exceptionOrNull() is AssistantFailure)
        val count = calls
        assertTrue(runCatching { transport.post(config, " ".repeat(6 * 1024 * 1024 + 1)) }.isFailure)
        assertTrue(runCatching { transport.postFile(config, file, policy.copy(bodyLimit = 1)) }.isFailure)
        assertEquals(count, calls)
    }

    @Test fun networkAuthLimitAndRedirectNeverRepeat() = runBlocking {
        val file = File(temporary.root, "body").apply { writeText("{}") }
        for (status in listOf(0, 401, 429, 302)) {
            var calls = 0
            val client = OkHttpClient.Builder().addInterceptor { chain ->
                calls++
                if (status == 0) throw java.io.IOException("fixture network")
                Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                    .code(status).message("fixture").header("Location", "https://other.invalid")
                    .body("{}".toResponseBody()).build()
            }.build()
            val failure = runCatching { AssistantTransport(client).postFile(config, file, policy) }.exceptionOrNull()
            assertTrue(failure is AssistantFailure)
            assertEquals(1, calls)
        }
    }
}
