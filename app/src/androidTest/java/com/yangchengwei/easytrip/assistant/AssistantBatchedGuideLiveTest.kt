package com.yangchengwei.easytrip.assistant

import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Explicit opt-in on an isolated emulator. One read, identical images, no trip or collection writes. */
class AssistantBatchedGuideLiveTest {
    @Test fun compareOriginalGuideUsingLegacyAndBatchedExtraction(): Unit = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue(args.getString("liveBatchedGuide") == "true")
        require(Build.HARDWARE in setOf("ranchu", "goldfish") && Build.MODEL.contains("sdk", ignoreCase = true))
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val setupFile = File(context.filesDir, "assistant-batched-live-setup.json")
        val setup = try { Json.parseToJsonElement(setupFile.readText()).jsonObject }
            finally { check(setupFile.delete()) }
        val config = ProviderConfig(setup.getValue("base_url").jsonPrimitive.content,
            setup.getValue("model").jsonPrimitive.content, setup.getValue("api_key").jsonPrimitive.content).validated()
        require(config.baseUrl == "https://api.deepseek.com" && config.model == "deepseek-flash")
        val share = setup.getValue("share").jsonPrimitive.content
        val city = setup.getValue("city").jsonPrimitive.content
        val capability = args.getString("capabilityOnly") == "true"
        val batchedOnly = args.getString("batchedOnly") == "true"
        val replayPrepared = args.getString("replayPrepared") == "true"
        val normalizationOnly = args.getString("normalizationOnly") == "true"
        require(!normalizationOnly || replayPrepared)
        val maxCalls = (args.getString("maxCalls")?.toInt() ?: 30).also { require(it in 1..30) }
        val name = args.getString("sample") ?: "sample"
        require(name.matches(Regex("[a-z0-9-]+")))
        val root = File(context.noBackupFilesDir, "batch-live-${UUID.randomUUID()}")
        val diskStore = AndroidAssistantImageStore(context, root, false)
        val replayImages = mutableMapOf<String, PreparedImage>()
        val store = object : AssistantImageStore by diskStore {
            override suspend fun prepare(sessionId: String, imageId: String): PreparedImage =
                if (replayPrepared) requireNotNull(replayImages[imageId]) else diskStore.prepare(sessionId, imageId)
        }
        val session = UUID.randomUUID().toString()
        val requests = CopyOnWriteArrayList<JsonObject>()
        val totalCalls = AtomicInteger()
        val phase = AtomicReference("read")
        val outputs = linkedMapOf<String, JsonElement>()
        val output = File(context.filesDir, "batch-live-$name.json")
        val client = OkHttpClient.Builder().followRedirects(false).followSslRedirects(false)
            .retryOnConnectionFailure(false).connectTimeout(15, TimeUnit.SECONDS)
            .callTimeout(45, TimeUnit.SECONDS).addInterceptor { chain ->
                val ordinal = totalCalls.incrementAndGet()
                check(ordinal <= minOf(maxCalls, if (capability) 4 else 30)) { "Bounded diagnostic request cap" }
                check(chain.request().url.host == "api.deepseek.com")
                val start = System.nanoTime()
                val response = try {
                    chain.proceed(chain.request())
                } catch (failure: java.io.IOException) {
                    requests += buildJsonObject {
                        put("phase", phase.get()); put("ordinal", ordinal)
                        put("transport_error", failure.javaClass.simpleName)
                        put("body_bytes", chain.request().body!!.contentLength())
                        put("elapsed_ms", (System.nanoTime() - start) / 1_000_000)
                    }
                    throw failure
                }
                val payload = runCatching {
                    Json.parseToJsonElement(response.peekBody(2L * 1024 * 1024).string()).jsonObject
                }.getOrNull()
                requests += buildJsonObject {
                    put("phase", phase.get()); put("ordinal", ordinal); put("status", response.code)
                    put("body_bytes", chain.request().body!!.contentLength())
                    put("elapsed_ms", (System.nanoTime() - start) / 1_000_000)
                    put("usage", payload?.get("usage") ?: JsonNull)
                    put("choices", payload?.get("choices") ?: JsonNull)
                }
                response
            }.build()
        fun places(values: List<PlaceIntent>) = JsonArray(values.map { item -> buildJsonObject {
            put("name", item.query); put("city", item.city); put("span", item.sourceSpan)
            putJsonArray("sources") { item.sources.forEach { source -> addJsonObject {
                when (source) {
                    is IntakeSource.Text -> { put("type", "text"); put("text", source.span) }
                    is IntakeSource.Image -> { put("type", "image"); put("ordinal", source.ordinal)
                        put("text", source.excerpt); put("basis", source.target?.basis?.name)
                        put("detail", source.target?.detail); put("role", source.target?.role?.name); put("parent", source.target?.parentName) }
                }
            } } }
        } })
        try {
            if (normalizationOnly) {
                val fixture = Json.parseToJsonElement(File(context.filesDir,
                    "assistant-batched-live-fixture/fixture.json").readText()).jsonObject
                val raw = fixture.getValue("raw").jsonArray.map { element ->
                    val item = element.jsonObject
                    val sources = item.getValue("sources").jsonArray.map { value ->
                        val source = value.jsonObject
                        val excerpt = source.getValue("text").jsonPrimitive.content
                        if (source.getValue("type").jsonPrimitive.content == "text") IntakeSource.Text(excerpt)
                        else IntakeSource.Image("fixture-${source.getValue("ordinal").jsonPrimitive.int}",
                            source.getValue("ordinal").jsonPrimitive.int, excerpt,
                            ImageTargetEvidence(
                                ImageTargetBasis.valueOf(source.getValue("basis").jsonPrimitive.content),
                                source.getValue("detail").jsonPrimitive.content,
                                ImageEntityRole.valueOf(source.getValue("role").jsonPrimitive.content),
                                source.getValue("parent").jsonPrimitive.content))
                    }
                    PlaceIntent(item.getValue("name").jsonPrimitive.content,
                        item.getValue("city").jsonPrimitive.content, item.getValue("span").jsonPrimitive.content,
                        sources = sources)
                }
                phase.set("normalization_fixture")
                val normalized = ModelPlaceNormalizer({ config }, AssistantTransport(client),
                    cityScope = { AssistantCityScope.parse(city) }).normalize(raw,
                    fixture.getValue("text").jsonPrimitive.content, city)
                outputs["input_mode"] = JsonPrimitive("raw_candidate_fixture")
                outputs["raw"] = places(raw)
                outputs["normalized"] = places(normalized)
                return@runBlocking
            }
            val loaded = if (replayPrepared) {
                // Opt-in diagnostics replay the exact previously sent bytes, never app/user files.
                val fixtureRoot = File(context.filesDir, "assistant-batched-live-fixture")
                val fixture = Json.parseToJsonElement(File(fixtureRoot, "fixture.json").readText()).jsonObject
                val hashes = fixture.getValue("prepared_sha256").jsonArray
                require(hashes.size in 1..20)
                val images = hashes.mapIndexed { index, hash ->
                    val bytes = File(fixtureRoot, "image_${index + 1}.jpg").readBytes()
                    val actual = java.security.MessageDigest.getInstance("SHA-256")
                        .digest(bytes).joinToString("") { "%02x".format(it) }
                    require(actual == hash.jsonPrimitive.content && bytes.size <= AssistantImagePolicy.MAX_PREPARED_BYTES)
                    val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                    require(bounds.outWidth > 0 && bounds.outHeight > 0)
                    val id = UUID.randomUUID().toString()
                    replayImages[id] = PreparedImage(id, "image/jpeg", bytes)
                    AssistantImage(id, index + 1, "image/jpeg", bytes.size.toLong(), bounds.outWidth, bounds.outHeight)
                }
                LoadedAssistantLink(fixture.getValue("text").jsonPrimitive.content, images)
            } else {
                val input = requireNotNull(AssistantLinkInput.parse(share))
                val loader = DefaultAssistantLinkLoader(AndroidAssistantLinkReader(context), store)
                withTimeout(180_000) { loader.load(session, input, emptyList()) {} }
            }
            outputs["input_mode"] = JsonPrimitive(if (replayPrepared) "prepared_fixture" else "live_link")
            val request = AssistantInputRequest(session, loaded.modelText, city,
                if (capability) loaded.images.take(2) else loaded.images,
                travelMaterials = true, cityScope = AssistantCityScope.parse(city))
            outputs["images"] = JsonPrimitive(request.images.size)
            outputs["text"] = JsonPrimitive(loaded.modelText)
            val prepared = GuideBatchFiles(File(root, "measure"), store, Mutex())
            val batch = prepared.prepare(request) {}
            try {
                outputs["prepared_bytes"] = JsonArray(batch.sources.filter { it.file != null }.map { JsonPrimitive(it.byteCount!!) })
                val diagnosticImages = File(context.filesDir, "batch-live-$name-images").apply { mkdirs() }
                outputs["prepared_sha256"] = JsonArray(batch.sources.filter { it.file != null }.map {
                    val image = requireNotNull(it.file)
                    image.copyTo(File(diagnosticImages, "${it.source.token}.jpg"), overwrite = true)
                    JsonPrimitive(java.security.MessageDigest.getInstance("SHA-256")
                        .digest(image.readBytes()).joinToString("") { byte -> "%02x".format(byte) })
                })
                val policy = GuideBatchPolicies.forConfig(config)
                val plan = GuideBatchBody().groups(config, GuideContext(request.text, request.city, request.cityScope), batch.sources, policy)
                outputs["groups"] = JsonPrimitive(plan.groups.size)
                assertTrue(plan.failures.isEmpty())
                assertEquals(1, plan.groups.size)
            } finally { prepared.release(batch) }
            val text = DeepSeekPlaceParser({ config }, client, 20_000, AssistantMaterialPolicy.MAX_PER_SOURCE, true)
            val image = ImagePlaceParser({ config }, AssistantTransport(client),
                AssistantMaterialPolicy.MAX_PER_SOURCE, true, { request.cityScope })
            val normalizer = ModelPlaceNormalizer({ config }, AssistantTransport(client), cityScope = { request.cityScope })
            val failures = mutableListOf<String>()
            for (mode in if (capability || batchedOnly) listOf("batched") else listOf("legacy", "batched")) {
                phase.set(mode)
                val processor = AssistantInputProcessor(text, image, store, normalizer,
                    materialTextParser = text, materialImageParser = image,
                    guideParser = if (mode == "legacy") null else BatchedGuideMaterialParser(config,
                        GuideBatchFiles(File(root, "batches"), store, Mutex()), GuideBatchBody(),
                        AssistantTransport(client), GuideBatchScheduler()))
                val start = System.nanoTime()
                val parsed = processor.parse(request) {}
                val extractionMs = (System.nanoTime() - start) / 1_000_000
                val raw = mergeIdenticalPlaceIntents(parsed.sources.flatMap { it.intents })
                val normalizeStart = System.nanoTime()
                var normalizationError: String? = null
                val normalized = try {
                    if (raw.isEmpty()) emptyList() else processor.normalize(raw, request.text, city)
                } catch (e: CancellationException) { throw e }
                catch (e: AssistantFailure) {
                    normalizationError = e.userMessage
                    failures += "$mode normalization: ${e.userMessage}"
                    emptyList()
                }
                outputs[mode] = buildJsonObject {
                    put("extraction_ms", extractionMs); put("normalization_ms", (System.nanoTime() - normalizeStart) / 1_000_000)
                    put("normalization_error", normalizationError)
                    putJsonArray("sources") { parsed.sources.forEach { source -> addJsonObject {
                        put("id", if (source.sourceId == "text") "text" else
                            "image_${request.images.find { it.id == source.sourceId }?.ordinal}")
                        put("error", source.failure?.userMessage); put("count", source.intents.size)
                    } } }
                    put("raw", places(raw)); put("normalized", places(normalized))
                }
                if (parsed.sources.any { it.failure != null }) failures += "$mode has failed sources"
                if (normalized.isEmpty()) failures += "$mode must recognize places"
            }
            assertTrue(failures.joinToString("; "), failures.isEmpty())
        } finally {
            output.writeText(JsonObject(outputs + ("requests" to JsonArray(requests))).toString())
            store.release(session)
            root.deleteRecursively()
        }
    }
}
