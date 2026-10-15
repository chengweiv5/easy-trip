package com.yangchengwei.easytrip.assistant

import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import com.yangchengwei.easytrip.amap.AmapPrivacyGate
import com.yangchengwei.easytrip.amap.TestConsentGate
import com.yangchengwei.easytrip.place.amap.AmapPlaceDataSource
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.amap.PlaceSearchDataSource
import java.io.File
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Explicit opt-in, emulator only. Production reader/model/POI; never writes a user's trip. */
class AssistantLinkLiveModelTest {
    @Test fun originalShareUsesRealModelAndPoiOrExplicitlyBlocksPlaceOverflow(): Unit = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue(args.getString("liveAssistantModel") == "true")
        require(Build.HARDWARE in setOf("ranchu", "goldfish") &&
            (Build.FINGERPRINT.contains("/sdk_") || Build.MODEL.contains("sdk_gphone"))) {
            "Live model diagnostic is restricted to an isolated emulator"
        }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val setupFile = File(context.filesDir, "assistant-live-setup.json")
        val setup = try { Json.parseToJsonElement(setupFile.readText()).jsonObject }
            finally { check(setupFile.delete()) }
        val config = ProviderConfig(
            setup.getValue("base_url").jsonPrimitive.content,
            setup.getValue("model").jsonPrimitive.content,
            setup.getValue("api_key").jsonPrimitive.content,
        ).validated()
        require(config.baseUrl == "https://api.deepseek.com")
        val share = setup.getValue("share").jsonPrimitive.content
        val input = requireNotNull(AssistantLinkInput.parse(share))
        val session = UUID.randomUUID().toString()
        val root = File(context.noBackupFilesDir, "link-live-model-$session")
        val store = AndroidAssistantImageStore(context, root)
        val modelCalls = AtomicInteger()
        val writes = AtomicInteger()
        val requests = CopyOnWriteArrayList<JsonObject>()
        val searches = CopyOnWriteArrayList<JsonObject>()
        val client = OkHttpClient.Builder()
            .followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false)
            .callTimeout(45, TimeUnit.SECONDS).connectTimeout(15, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val ordinal = modelCalls.incrementAndGet()
                check(ordinal <= 16) { "Exceeded eight stages with at most one format repair each" }
                check(chain.request().url.host == "api.deepseek.com")
                val started = System.nanoTime()
                try {
                    val response = chain.proceed(chain.request())
                    val payload = runCatching {
                        Json.parseToJsonElement(response.peekBody(256 * 1024).string()).jsonObject
                    }.getOrNull()
                    requests += buildJsonObject {
                        put("ordinal", ordinal)
                        put("http_status", response.code)
                        put("elapsed_ms", (System.nanoTime() - started) / 1_000_000)
                        payload?.get("usage")?.let { put("usage", it) }
                        // Only tool results, never credentials, request images or server error bodies.
                        payload?.get("choices")?.jsonArray?.firstOrNull()?.jsonObject?.let {
                            put("finish_reason", it["finish_reason"] ?: JsonNull)
                            it["message"]?.jsonObject?.get("tool_calls")?.let { calls -> put("tool_calls", calls) }
                        }
                    }
                    response
                } catch (error: Exception) {
                    requests += buildJsonObject {
                        put("ordinal", ordinal)
                        put("transport_error", error.javaClass.simpleName)
                        put("elapsed_ms", (System.nanoTime() - started) / 1_000_000)
                    }
                    throw error
                }
            }.build()
        val textParser = DeepSeekPlaceParser({ config }, client, maxInputLength = 20_000,
            maxPlaces = AssistantMaterialPolicy.MAX_PER_SOURCE, travelMaterials = true)
        val imageParser = ImagePlaceParser({ config }, AssistantTransport(client),
            maxPlaces = AssistantMaterialPolicy.MAX_PER_SOURCE, travelMaterials = true)
        var normalizedPlaces = emptyList<PlaceIntent>()
        var rawCandidates = emptyList<PlaceIntent>()
        val normalizer = PlaceIntentNormalizer { candidates, material, city ->
            rawCandidates = candidates
            ModelPlaceNormalizer({ config }, AssistantTransport(client)).normalize(candidates, material, city)
                .also { normalizedPlaces = it }
        }
        val reader = AndroidAssistantLinkReader(context) {
            android.util.Log.i("AssistantLiveModel", "${input.platform}: $it")
        }
        val loader = DefaultAssistantLinkLoader(reader, store)
        AmapPrivacyGate.create(context).apply { reportShown(); reportDecision(true) }
        val consent = requireNotNull(TestConsentGate().apply { show() }.decide(true))
        val amap = AmapPlaceDataSource(context, consent)
        val source = object : PlaceSearchDataSource by amap {
            override suspend fun search(keyword: String, city: String?): List<PlaceCandidate> {
                val result = amap.search(keyword, city)
                searches += buildJsonObject {
                    put("query", keyword); put("city", city); put("candidates", result.size)
                }
                return result
            }
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val controller = PlaceAssistantController(
            "isolated-live-no-writes", scope, textParser, source,
            object : PlaceImport {
                override suspend fun receipt(operationId: String): ImportReceipt? = null
                override suspend fun commit(confirmation: ImportConfirmation): ImportReceipt {
                    writes.incrementAndGet()
                    error("Live diagnostic must never collect places")
                }
            },
            resolveDefaultCity = { "青州市" },
            processor = { AssistantInputProcessor(textParser, imageParser, store, normalizer = normalizer) },
            sessionId = session, linkLoader = loader,
        )
        val output = File(context.filesDir, "assistant-live-${input.platform}.json")
        fun record(state: PlaceAssistantState) {
            output.writeText(buildJsonObject {
                put("platform", input.platform.name)
                put("model", config.model)
                put("model_calls", modelCalls.get())
                put("image_count", state.images.size)
                put("writes", writes.get())
                put("error", state.error)
                put("input_preserved", state.input == share)
                put("busy", state.busy)
                put("requests", JsonArray(requests))
                putJsonArray("raw_candidates") {
                    rawCandidates.forEach { place -> addJsonObject {
                        put("name", place.query); put("city", place.city); put("span", place.sourceSpan)
                        putJsonArray("image_evidence") {
                            place.sources.filterIsInstance<IntakeSource.Image>().forEach { source -> addJsonObject {
                                put("ordinal", source.ordinal); put("excerpt", source.excerpt)
                                source.target?.let {
                                    put("targetBasis", it.basis.name); put("targetDetail", it.detail)
                                    put("entityRole", it.role.name); put("parentName", it.parentName)
                                }
                            } }
                        }
                    } }
                }
                putJsonArray("normalized_places") {
                    normalizedPlaces.forEach { addJsonObject { put("name", it.query); put("city", it.city); put("sources", it.sources.size) } }
                }
                put("searches", JsonArray(searches))
                putJsonArray("sources") {
                    state.sourceProgress.forEach {
                        addJsonObject { put("stage", it.stage.name); put("error", it.error) }
                    }
                }
                putJsonArray("items") {
                    state.items.forEach {
                        addJsonObject {
                            put("query", it.intent.query); put("city", it.intent.city)
                            put("source_span", it.intent.sourceSpan); put("sources", it.sources.size)
                            put("status", it.status.name); put("detail", it.detail)
                            put("poi_name", it.poi?.name); put("poi_city", it.poi?.cityName)
                            put("poi_id", it.poi?.poiId)
                            put("latitude", it.poi?.point?.latitude)
                            put("longitude", it.poi?.point?.longitude)
                        }
                    }
                }
            }.toString())
        }
        try {
            withContext(Dispatchers.Main) {
                controller.environment(true, true, true)
                controller.editInput(share)
                controller.submit()
                assertTrue(controller.state.value.busy)
            }
            val state = withTimeout(720_000) { controller.state.first { !it.busy } }
            record(state)
            assertEquals(share, state.input)
            assertEquals(0, writes.get())
            assertEquals(6, state.images.size)
            assertTrue(modelCalls.get() in 8..16)
            assertEquals(modelCalls.get(), requests.size)
            assertTrue("All real model requests must succeed",
                requests.all { it["http_status"]?.jsonPrimitive?.int == 200 })
            assertTrue("Independent destinations must not all require review",
                normalizedPlaces.none { it.needsReview })
            assertTrue("Recommended lodging is not a sightseeing destination",
                normalizedPlaces.none { it.query in setOf("古州往事客栈", "建国璞隐酒店", "桔子酒店") })
            assertTrue(normalizedPlaces.none { it.query == "青州糗糕" })
            val excludedDining = setOf("王家煎包", "王志勇早餐", "齐记土鸡米线", "马杰炸鸡",
                "玲珑小吃", "回香斋", "建军烧烤", "古州学伟牛鲜苼", "古州学伟牛鲜笙",
                "丁妈妈糗糕", "隆盛糕点", "老槐树煎包", "清真老槐树煎包",
                "东关酱牛肉", "东关清真酱牛肉", "老北京铜锅涮")
            assertTrue("Dining venues must not remain after normalization",
                normalizedPlaces.none { it.query in excludedDining })
            assertEquals(1, normalizedPlaces.count { it.query.contains("九龙峪") })
            assertEquals(1, normalizedPlaces.count { it.query.contains("博物馆") })
            assertTrue("Unique generic names should be merged before POI lookup",
                normalizedPlaces.none { it.query in setOf("博物馆", "古城") })
            assertTrue("Author-marked art museum must survive", normalizedPlaces.any { it.query.contains("青州美术馆") })
            assertTrue("Author-marked flower market must survive", normalizedPlaces.any { it.query.contains("青州花卉市场") })
            // A historic gate may remain when introduced as an independent visit;
            // a lodging/entry reference alone no longer forces it into the result.
            if (input.platform == AssistantLinkPlatform.XIAOHONGSHU) {
                assertTrue(normalizedPlaces.any { it.query == "青州古城" })
                assertTrue("An ordinary entry inside the ancient-city visit is not a separate itinerary stop",
                    normalizedPlaces.none { it.query in setOf("南门", "青州古城(南门)", "青州古城南门") })
                val backgrounds = setOf("角楼村", "壮汉庙村", "薄板台村", "西建德村", "西河村", "窦楼村", "闫刘村",
                    "赤涧村", "石河村", "西赵村", "白家店村", "西刘井村", "西上院村", "凤凰山西路")
                assertTrue(rawCandidates.none { it.query in backgrounds })
            }
            if (state.error?.contains("每批最多 ${AssistantPlacePolicy.MAX_PLACES} 个") == true) {
                assertTrue(state.items.isEmpty())
                assertTrue(searches.isEmpty())
                assertEquals(7, state.sourceProgress.size)
                android.util.Log.i("AssistantLiveModel", "PASS ${input.platform} explicit place overflow; no POI or writes")
            } else {
                assertNull(state.error)
                assertEquals(7, state.sourceProgress.size)
                assertTrue("All material sources must complete: ${state.sourceProgress}",
                    state.sourceProgress.all { it.stage == SourceStage.COMPLETE })
                assertTrue(state.items.isNotEmpty())
                assertTrue(state.items.any { it.status == IntakeStatus.READY })
                assertTrue(state.items.none { it.status in setOf(IntakeStatus.FAILED, IntakeStatus.QUERYING, IntakeStatus.WAITING) })
                state.items.filter { it.status == IntakeStatus.READY }.forEach {
                    assertNotNull(it.poi?.point)
                    assertTrue(sameCity(it.poi?.cityName, "青州市"))
                }
                assertTrue("Known attraction must be recognized and matched",
                    state.items.any { it.intent.query.contains("井塘") && it.poi?.name?.contains("井塘") == true })
                android.util.Log.i("AssistantLiveModel", "PASS ${input.platform} items=${state.items.size} ready=${state.eligible.size} no writes")
            }
        } finally {
            record(controller.state.value)
            withContext(Dispatchers.Main) { controller.cancel() }
            scope.cancel()
            store.release(session)
            root.deleteRecursively()
        }
    }
}
