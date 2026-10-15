package com.yangchengwei.easytrip.assistant

import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import com.yangchengwei.easytrip.assistant.cityfirst.GuideEvidenceCheckpointCodec
import java.io.File
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Explicit opt-in, prepared fixtures only, isolated emulator, no trip, POI or collection writes. */
class GuideEvidenceLiveTest {
    @Test fun runPreparedComparison(): Unit = runBlocking {
        val arguments = InstrumentationRegistry.getArguments()
        assumeTrue(arguments.getString("liveEvidence") == "true")
        require(Build.HARDWARE in setOf("ranchu", "goldfish") && Build.MODEL.contains("sdk", true))
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val setupFile = File(context.filesDir, "evidence-live-setup.json")
        val setup = try { Json.parseToJsonElement(setupFile.readText()).jsonObject }
            finally { check(setupFile.delete()) }
        val config = ProviderConfig(setup.getValue("base_url").jsonPrimitive.content,
            setup.getValue("model").jsonPrimitive.content, setup.getValue("api_key").jsonPrimitive.content).validated()
        require(GuideEvidenceBudget.supports(config))
        val sample = requireNotNull(arguments.getString("sample")).also { require(it.matches(Regex("[a-z0-9-]+"))) }
        val mode = requireNotNull(arguments.getString("mode")).also { require(it in setOf("legacy", "evidence")) }
        val round = requireNotNull(arguments.getString("round")).toInt().also { require(it in 1..3) }
        val fixtureRoot = File(context.filesDir, "evidence-fixtures/$sample")
        val fixture = Json.parseToJsonElement(File(fixtureRoot, "fixture.json").readText()).jsonObject
        val hashes = fixture.getValue("prepared_sha256").jsonArray.map { it.jsonPrimitive.content }
        val prepared = mutableMapOf<String, PreparedImage>()
        val images = hashes.mapIndexed { index, expected ->
            val bytes = File(fixtureRoot, "image_${index + 1}.jpg").readBytes()
            require(sha(bytes) == expected && bytes.size <= AssistantImagePolicy.MAX_PREPARED_BYTES)
            val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            require(bounds.outWidth > 0 && bounds.outHeight > 0)
            val id = UUID.randomUUID().toString()
            prepared[id] = PreparedImage(id, "image/jpeg", bytes)
            AssistantImage(id, index + 1, "image/jpeg", bytes.size.toLong(), bounds.outWidth, bounds.outHeight)
        }
        val root = File(context.noBackupFilesDir, "evidence-live-${UUID.randomUUID()}").apply { mkdirs() }
        val disk = AndroidAssistantImageStore(context, root, false)
        val store = object : AssistantImageStore by disk {
            override suspend fun prepare(sessionId: String, imageId: String) = requireNotNull(prepared[imageId])
        }
        val requests = mutableListOf<JsonObject>()
        val ledger = File(context.filesDir, "evidence-live-ledger.txt")
        val budget = DiagnosticCallBudget(ledger, 24)
        var phase = "extraction"
        val output = File(context.filesDir, "evidence-live-$sample-$mode-r$round.json")
        val fields = linkedMapOf<String, JsonElement>("sample" to JsonPrimitive(sample), "mode" to JsonPrimitive(mode),
            "prepared_sha256" to JsonArray(hashes.map(::JsonPrimitive)))
        val client = OkHttpClient.Builder().followRedirects(false).followSslRedirects(false)
            .retryOnConnectionFailure(false).addInterceptor { chain ->
                val ordinal = budget.reserve()
                val start = System.nanoTime()
                try {
                    val response = chain.proceed(chain.request())
                    val raw = response.peekBody(2L * 1024 * 1024).string()
                    requests += buildJsonObject {
                        put("ordinal", ordinal); put("phase", phase); put("status", response.code)
                        put("elapsed_ms", (System.nanoTime() - start) / 1_000_000)
                        put("request_bytes", chain.request().body!!.contentLength())
                        put("response", runCatching { Json.parseToJsonElement(raw) }.getOrElse { JsonPrimitive(raw) })
                    }
                    response
                } catch (e: Exception) {
                    requests += buildJsonObject { put("ordinal", ordinal); put("phase", phase)
                        put("error", e.javaClass.simpleName); put("elapsed_ms", (System.nanoTime() - start) / 1_000_000) }
                    throw e
                }
            }.build()
        val text = fixture.getValue("text").jsonPrimitive.content
        val city = setup.getValue("city").jsonPrimitive.content
        val binding = GuideBatchBinding(UUID.randomUUID().toString(), 0, city)
        try {
            val wire = if (mode == "evidence") EvidenceGuideWireProtocol else LegacyGuideWireProtocol
            val request = AssistantInputRequest(UUID.randomUUID().toString(), text, city, images,
                travelMaterials = true, cityScope = AssistantCityScope.parse(city),
                guideVersion = if (mode == "evidence") GuideProtocolVersion.EVIDENCE_V2 else GuideProtocolVersion.LEGACY_V1,
                guideBinding = if (mode == "evidence") binding else null)
            val parser = BatchedGuideMaterialParser(config, GuideBatchFiles(File(root, "body"), store, Mutex()),
                GuideBatchBody(wire), AssistantTransport(client), GuideBatchScheduler(), protocol = wire)
            val parsed = parser.parse(request, {}) {}
            fields["sources"] = JsonArray(parsed.sources.map { s -> buildJsonObject {
                put("id", s.sourceId); put("error", s.failure?.userMessage)
                put("count", if (mode == "evidence") s.evidence.size else s.intents.size)
            } })
            phase = "resolution"
            val places = if (mode == "evidence") {
                val result = ModelGuideEvidenceResolver({ config }, AssistantTransport(client)).resolve(
                    parsed.sources.flatMap { it.evidence }, GuideContext(text, city, request.cityScope), emptyList())
                fields["checkpoint"] = GuideEvidenceCheckpointCodec.encode(GuideEvidenceCheckpoint(binding,
                    parsed.sources, result, emptyList()))
                result.places.map { it.intent }
            } else ModelPlaceNormalizer({ config }, AssistantTransport(client), cityScope = { request.cityScope })
                .normalize(mergeIdenticalPlaceIntents(parsed.sources.flatMap { it.intents }), text, city)
            fields["places"] = JsonArray(places.map { p -> buildJsonObject {
                put("name", p.query); put("city", p.city); put("review", p.needsReview)
            } })
            assertFalse("no valid result", places.isEmpty())
        } catch (e: Exception) {
            fields["failure"] = JsonPrimitive((e as? AssistantFailure)?.userMessage ?: e.javaClass.simpleName)
            throw e
        } finally {
            fields["requests"] = JsonArray(requests)
            fields["used_total"] = JsonPrimitive(budget.used())
            output.writeText(JsonObject(fields).toString())
            root.deleteRecursively()
        }
    }
    private fun sha(bytes: ByteArray) = java.security.MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it) }
}

internal class DiagnosticCallBudget(private val ledger: File, private val cap: Int) {
    @Synchronized fun used(): Int = ledger.readText().trim().toInt().also { require(it in 0..cap) }
    @Synchronized fun reserve(): Int {
        val next = used() + 1
        check(next <= cap) { "diagnostic HTTP budget exhausted" }
        java.io.RandomAccessFile(ledger, "rw").use { file ->
            file.setLength(0); file.writeBytes(next.toString()); file.fd.sync()
        }
        return next
    }
}

class GuideEvidenceDiagnosticBudgetTest {
    @Test fun exhaustedBudgetCannotDispatchAndFailedCallsAreNotRefunded() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "budget-test-${UUID.randomUUID()}")
        try {
            file.writeText("0")
            val budget = DiagnosticCallBudget(file, 2)
            assertEquals(1, budget.reserve())
            // Simulated IOException / invalid JSON still consumed the reserved dispatch.
            assertEquals(2, DiagnosticCallBudget(file, 2).reserve())
            assertTrue(runCatching { budget.reserve() }.isFailure)
            assertEquals("2", file.readText())
        } finally { file.delete() }
    }
}
