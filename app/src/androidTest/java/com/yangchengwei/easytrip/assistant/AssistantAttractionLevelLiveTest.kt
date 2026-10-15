package com.yangchengwei.easytrip.assistant

import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Explicit opt-in only: production public reader and model pipeline, no POI or trip writes. */
class AssistantAttractionLevelLiveTest {
    @Test fun guideProducesIndependentDestinationsUsingConfiguredModel() = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue(args.getString("liveAttractionLevel") == "true")
        // Test-only, explicit retry opt-in. Production never automatically retries
        // transport failures; exercise its existing failed-source retry interface.
        val retryRounds = args.getString("liveSourceRetryRounds")?.toInt() ?: 0
        require(retryRounds in 0..2)
        check(Build.HARDWARE in setOf("ranchu", "goldfish"))
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val setupFile = File(context.filesDir, "attraction-live-setup.json")
        val setup = try { Json.parseToJsonElement(setupFile.readText()).jsonObject }
            finally { check(setupFile.delete()) }
        val config = ProviderConfig(setup.getValue("base_url").jsonPrimitive.content,
            setup.getValue("model").jsonPrimitive.content, setup.getValue("api_key").jsonPrimitive.content).validated()
        check(config.baseUrl == "https://api.deepseek.com" && config.model == "deepseek-flash")
        val sample = setup.getValue("sample").jsonPrimitive.content
        check(sample in setOf("xuzhou", "qingzhou-xhs", "qingzhou-ctrip"))
        val share = setup.getValue("share").jsonPrimitive.content
        val city = if (sample == "xuzhou") "徐州市" else "青州市"
        val root = File(context.noBackupFilesDir, "attraction-live-${UUID.randomUUID()}")
        val session = UUID.randomUUID().toString()
        val store = AndroidAssistantImageStore(context, root)
        val requests = mutableListOf<JsonObject>()
        val calls = AtomicInteger()
        val client = OkHttpClient.Builder().followRedirects(false).followSslRedirects(false)
            .retryOnConnectionFailure(false).callTimeout(90, TimeUnit.SECONDS).connectTimeout(15, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                check(calls.incrementAndGet() <= 28) { "Live diagnostic request budget exceeded" }
                val started = System.nanoTime()
                try {
                    val response = chain.proceed(chain.request())
                    val payload = runCatching {
                        Json.parseToJsonElement(response.peekBody(256 * 1024).string()).jsonObject
                    }.getOrNull()
                    requests += buildJsonObject {
                        put("http_status", response.code); put("elapsed_ms", (System.nanoTime() - started) / 1_000_000)
                        payload?.get("usage")?.let { put("usage", it) }
                        payload?.get("choices")?.let { put("choices", it) }
                    }
                    response
                } catch (e: Exception) {
                    requests += buildJsonObject { put("transport_error", e.javaClass.simpleName) }
                    throw e
                }
            }.build()
        var material: LoadedAssistantLink? = null
        var parsed: ParsedInputBatch? = null
        var normalized = emptyList<PlaceIntent>()
        val retriedSources = mutableListOf<List<String>>()
        var error: String? = null
        try {
            material = DefaultAssistantLinkLoader(AndroidAssistantLinkReader(context), store)
                .load(session, requireNotNull(AssistantLinkInput.parse(share)), emptyList()) {}
            val text = DeepSeekPlaceParser({ config }, client, 20_000, AssistantMaterialPolicy.MAX_PER_SOURCE, true)
            val image = ImagePlaceParser({ config }, AssistantTransport(client), AssistantMaterialPolicy.MAX_PER_SOURCE, true)
            val processor = AssistantInputProcessor(text, image, store,
                ModelPlaceNormalizer({ config }, AssistantTransport(client)), text, image)
            parsed = processor.parse(AssistantInputRequest(session, material.modelText, city,
                material.images, travelMaterials = true)) {}
            repeat(retryRounds) {
                val failedIds = requireNotNull(parsed).sources.filter { it.failure != null }.map { it.sourceId }.toSet()
                if (failedIds.isNotEmpty()) {
                    retriedSources += failedIds.toList()
                    delay(3_000)
                    val retried = processor.parse(AssistantInputRequest(session, material.modelText, city,
                        material.images, retrySourceIds = failedIds, travelMaterials = true)) {}
                    val replacements = retried.sources.associateBy { it.sourceId }
                    parsed = ParsedInputBatch(requireNotNull(parsed).sources.map { replacements[it.sourceId] ?: it },
                        retried.overflow || requireNotNull(parsed).sources.filter { it.sourceId !in failedIds }
                            .any { it.failure?.code == AssistantFailureCode.OVERFLOW })
                }
            }
            val completed = requireNotNull(parsed)
            // Like production's partial-success path, normalize successful
            // material first. Full-sample acceptance still requires no failures.
            normalized = processor.normalize(mergeIdenticalPlaceIntents(completed.sources.flatMap { it.intents }),
                material.modelText, city)
            assertFalse(completed.overflow)
            assertTrue("All sources must finish: ${completed.sources.mapNotNull { it.failure?.userMessage }}",
                completed.sources.all { it.failure == null })
            assertTrue(normalized.isNotEmpty())
            assertTrue("Clear independent destinations should not all require manual review: " +
                normalized.filter { it.needsReview }.map { it.query },
                normalized.none { it.needsReview })
            if (sample == "xuzhou") {
                val excluded = setOf("LED互动舞台", "双层舞台", "北门外广场", "北門外广场", "粉色打卡墙",
                    "六合巷", "六合巷小酒馆", "云龙让茶", "徐州博物馆入口", "徐州博物馆出口", "云龙山北门",
                    "徐州站", "徐州东站", "丰储街", "丰储街蜗牛捞汁小海鲜")
                assertTrue("Internal stops/dining retained: ${normalized.map { it.query }}",
                    normalized.none { it.query in excluded || it.query.endsWith("停车场") })
                listOf("龟山汉墓", "宝莲寺", "徐州博物馆", "乾隆行宫", "汉文化景区").forEach { expected ->
                    assertTrue("Independent attraction lost or joined: $expected", normalized.any { it.query == expected })
                }
                assertTrue("Food culture museum is not dining", normalized.any { it.query.contains("饮食文化博物馆") })
            } else {
                listOf("井塘", "青州博物馆", "青州古城", "青州美术馆").forEach { expected ->
                    assertTrue("Independent attraction lost: $expected", normalized.any { it.query.contains(expected) })
                }
                assertTrue(normalized.none { it.query in setOf("南门", "青州古城(南门)", "青州古城南门") })
                assertFalse("A lodging-area shorthand must not duplicate the park",
                    normalized.any { it.query == "范公亭" } && normalized.any { it.query == "范公亭公园" })
                if (sample == "qingzhou-xhs") {
                    listOf("古州往事客栈", "建国璞隐酒店", "桔子酒店").forEach { lodging ->
                        assertFalse("Recommended lodging must be excluded: $lodging",
                            normalized.any { it.query == lodging })
                    }
                }
            }
        } catch (e: Throwable) {
            error = (e as? AssistantFailure)?.userMessage ?: "${e.javaClass.simpleName}: ${e.message}"
            throw e
        } finally {
            File(context.filesDir, "attraction-live-$sample.json").writeText(buildJsonObject {
                put("sample", sample); put("model", config.model); put("city", city)
                put("image_count", material?.images?.size ?: 0); put("request_count", calls.get())
                put("retry_rounds_allowed", retryRounds)
                putJsonArray("retried_source_ids") {
                    retriedSources.forEach { ids -> add(JsonArray(ids.map(::JsonPrimitive))) }
                }
                put("error", error?.let(::JsonPrimitive) ?: JsonNull)
                put("material_text", material?.modelText.orEmpty())
                put("requests", JsonArray(requests))
                putJsonArray("sources") {
                    parsed?.sources?.forEach { source -> addJsonObject {
                        put("id", source.sourceId); put("error", source.failure?.userMessage?.let(::JsonPrimitive) ?: JsonNull)
                        putJsonArray("names") { source.intents.forEach { add(it.query) } }
                    } }
                }
                putJsonArray("normalized") { normalized.forEach { addJsonObject {
                    put("name", it.query); put("city", it.city); put("needs_review", it.needsReview)
                    putJsonArray("evidence") { it.sources.forEach { source -> add(source.toString()) } }
                } } }
                put("poi_calls", 0); put("trip_writes", 0)
            }.toString())
            store.release(session)
            check(root.deleteRecursively() || !root.exists())
        }
    }
}
