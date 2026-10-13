package com.yangchengwei.easytrip.assistant

import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Six bounded requests against externally staged, hash-pinned original screenshots; no POI or writes. */
class AssistantMapBackgroundLiveModelTest {
    private data class Fixture(val ordinal: Int, val sha256: String, val targets: List<Set<String>>)

    @Test fun originalRouteMapsExcludeBackgroundButKeepDestinationVillage(): Unit = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveAssistantMapBackground") == "true")
        require(Build.HARDWARE in setOf("ranchu", "goldfish") &&
            (Build.FINGERPRINT.contains("/sdk_") || Build.MODEL.contains("sdk_gphone")))
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val setupFile = File(context.filesDir, "assistant-map-setup.json")
        val setup = try { Json.parseToJsonElement(setupFile.readText()).jsonObject }
            finally { check(setupFile.delete()) }
        val config = ProviderConfig(setup.getValue("base_url").jsonPrimitive.content,
            setup.getValue("model").jsonPrimitive.content, setup.getValue("api_key").jsonPrimitive.content).validated()
        require(config.baseUrl == "https://api.deepseek.com")
        val fixtures = listOf(
            Fixture(2, "578d3b6b9f0d9893cd1577369eb0f630123b4b014ca75c45a8a4d6d3982d373b", listOf(
                setOf("范公亭公园"), setOf("万年桥"), setOf("青州昭德古街", "昭德古街"),
                setOf("青州古城(南门)", "青州古城南门"))),
            Fixture(3, "dda2d955af2cb6152adbddde9d116a3b3d081d1348dce7fc254b9cd763d08c16", listOf(
                setOf("云门山风景区"), setOf("广福寺"), setOf("青州九龙峪景区"),
                setOf("青州不夜城", "青州不夜城(暂停营业)"))),
            Fixture(4, "b5a9307e2580a2819695e1c4e186be87b2b07eeaf9aa37ea186df28bff29f6e7", listOf(
                setOf("青州博物馆"), setOf("驼山", "驼山风景区"), setOf("龙兴寺"), setOf("青州井塘古村景区"))),
        )
        val staged = File(context.filesDir, "assistant-map-fixtures")
        val root = File(context.noBackupFilesDir, "map-live-${UUID.randomUUID()}")
        val session = UUID.randomUUID().toString()
        val store = AndroidAssistantImageStore(context, root)
        val calls = AtomicInteger()
        val http = mutableListOf<JsonObject>()
        val results = mutableListOf<JsonObject>()
        val client = OkHttpClient.Builder().followRedirects(false).followSslRedirects(false)
            .retryOnConnectionFailure(false).callTimeout(45, TimeUnit.SECONDS).connectTimeout(15, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                check(calls.incrementAndGet() <= 6)
                check(chain.request().url.host == "api.deepseek.com")
                val response = chain.proceed(chain.request())
                val body = runCatching { Json.parseToJsonElement(response.peekBody(256 * 1024).string()).jsonObject }.getOrNull()
                http += buildJsonObject {
                    put("http_status", response.code)
                    body?.get("usage")?.let { put("usage", it) }
                    body?.get("choices")?.jsonArray?.firstOrNull()?.jsonObject?.get("message")?.jsonObject
                        ?.get("tool_calls")?.let { put("tool_calls", it) }
                }
                response
            }.build()
        val parser = ImagePlaceParser({ config }, AssistantTransport(client), 100, travelMaterials = true)
        try {
            val imported = fixtures.map { fixture ->
                val bytes = File(staged, "note-image-0${fixture.ordinal}.jpg").readBytes()
                assertEquals(fixture.sha256, MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) })
                fixture to store.importBytes(session, bytes, emptyList())
            }
            repeat(2) { round ->
                imported.forEach { (fixture, image) ->
                    var names = emptyList<String>()
                    var failure: AssistantFailure? = null
                    try {
                        names = parser.parse(store.prepare(session, image.id), fixture.ordinal, "", "青州") {}.map { it.query }
                    } catch (error: AssistantFailure) { failure = error }
                    val normalizedNames = names.map(::normalizedPlace)
                    val passed = failure == null && names.size == 4 && fixture.targets.all { aliases ->
                        normalizedNames.count { name -> aliases.any { normalizedPlace(it) == name } } == 1
                    }
                    results += buildJsonObject {
                        put("round", round + 1); put("image", fixture.ordinal); put("passed", passed)
                        putJsonArray("names") { names.forEach(::add) }
                        failure?.let { put("failure_code", it.code.name) }
                    }
                }
            }
            assertEquals(6, calls.get())
            assertEquals(6, http.size)
            assertTrue(http.all { it.getValue("http_status").jsonPrimitive.int == 200 })
            assertTrue("Expected exactly four author-marked destinations per map: $results",
                results.all { it.getValue("passed").jsonPrimitive.boolean })
        } finally {
            File(context.filesDir, "assistant-map-result.json").writeText(buildJsonObject {
                put("model", config.model); put("model_calls", calls.get())
                put("scope", "Production image extraction only; no normalization, POI or collection")
                put("requests", JsonArray(http)); put("results", JsonArray(results))
            }.toString())
            store.release(session)
            root.deleteRecursively()
            staged.deleteRecursively()
        }
    }
}
