package com.yangchengwei.easytrip.assistant

import android.content.Context
import android.os.Build
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.yangchengwei.easytrip.amap.AmapPrivacyGate
import com.yangchengwei.easytrip.amap.AmapServiceException
import com.yangchengwei.easytrip.amap.TestConsentGate
import com.yangchengwei.easytrip.place.amap.AmapPlaceDataSource
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.amap.PlaceSearchDataSource
import com.amap.api.services.core.AMapException
import com.amap.api.services.poisearch.PoiSearch
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Opt-in, emulator-only regression using the production controller and SDK without extra pacing.
 * Run with -e pacingMode production and -e pacingRun <unique label>.
 * Historical pre-fix comparison modes are archived with the diagnosis, not runnable in this build.
 */
@RunWith(AndroidJUnit4::class)
class AssistantSearchPacingDiagnosticTest {
    /** Deliberately separate from controller regression: same SDK query, bounded 8-request burst. */
    @Test fun inspectSdkBurstErrorMetadata() = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        val mode = args.getString("sdkMode").orEmpty()
        assumeTrue("Explicit SDK diagnostic mode required", mode in setOf("burst", "paced"))
        assumeTrue("Never run on a physical phone", Build.HARDWARE in setOf("ranchu", "goldfish"))
        val run = args.getString("pacingRun").orEmpty()
        require(run.matches(Regex("[a-zA-Z0-9_-]{1,50}")))
        val context = ApplicationProvider.getApplicationContext<Context>()
        AmapPrivacyGate.create(context).apply { reportShown(); reportDecision(true) }
        val count = args.getString("sdkCount")?.toInt() ?: 8
        require(count in 1..8)
        val names = listOf("青州古城", "青州博物馆", "范公亭公园", "偶园",
            "昭德古街", "黄花溪", "九龙峪景区", "广福寺").let {
            (if (args.getString("pacingReverse") == "true") it.reversed() else it).take(count)
        }
        val safeTypes = setOf("CUQPS_HAS_EXCEEDED_THE_LIMIT", "CKQPS_HAS_EXCEEDED_THE_LIMIT",
            "CQPS_HAS_EXCEEDED_THE_LIMIT", "QPS_HAS_EXCEEDED_THE_LIMIT", "SERVER_IS_BUSY",
            "ACCESS_TOO_FREQUENT", "GATEWAY_TIMEOUT", "AMAP_CLIENT_ERRORCODE_MISSSING")
        val start = SystemClock.elapsedRealtime()
        val records = names.mapIndexed { index, name ->
            async(Dispatchers.IO) {
                if (mode == "paced") delay(index * 700L)
                val began = SystemClock.elapsedRealtime()
                var count = 0
                var code = 1000
                var type = ""
                var message = ""
                try {
                    val query = PoiSearch.Query(assistantSearchKeyword(name, "青州市"), "", "青州市").apply {
                        pageSize = 20; pageNum = 1; cityLimit = false
                    }
                    count = PoiSearch(context, query).searchPOI().pois.orEmpty().size
                } catch (e: AMapException) {
                    code = e.errorCode
                    type = e.errorType?.takeIf { it in safeTypes } ?: "OTHER"
                    message = e.errorMessage?.takeIf { it in safeTypes } ?: "OTHER"
                }
                buildJsonObject {
                    put("fixture", name); put("startMs", began - start)
                    put("elapsedMs", SystemClock.elapsedRealtime() - began)
                    put("resultCount", count); put("errorCode", code)
                    put("errorType", type); put("safeErrorMessage", message)
                }
            }
        }.awaitAll()
        val report = buildJsonObject { put("mode", mode); put("run", run); put("requests", JsonArray(records)) }
        val file = File(context.externalCacheDir ?: context.cacheDir, "diagnostics/pacing/$run.json")
        file.parentFile!!.mkdirs(); file.writeText(report.toString())
        println("PACING_DIAGNOSTIC $report")
        assertTrue("All $count first-attempt SDK calls should succeed",
            records.all { it.getValue("errorCode").jsonPrimitive.int == 1000 &&
                it.getValue("resultCount").jsonPrimitive.int > 0 })
    }

    @Test fun compareInitialEightPlaceSearches() = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        val mode = args.getString("pacingMode").orEmpty()
        assumeTrue("Explicit production regression mode required", mode == "production")
        assumeTrue("Never run this diagnostic on a physical phone",
            Build.HARDWARE in setOf("ranchu", "goldfish") &&
                (Build.FINGERPRINT.contains("/sdk_") || Build.MODEL.contains("sdk_gphone")))
        val run = args.getString("pacingRun").orEmpty()
        require(run.matches(Regex("[a-zA-Z0-9_-]{1,50}")))
        val reverse = args.getString("pacingReverse") == "true"
        val context = ApplicationProvider.getApplicationContext<Context>()
        AmapPrivacyGate.create(context).apply { reportShown(); reportDecision(true) }
        val token = requireNotNull(TestConsentGate().apply { show() }.decide(true))
        val source = AmapPlaceDataSource(context, token)
        val names = listOf("青州古城", "青州博物馆", "范公亭公园", "偶园",
            "昭德古街", "黄花溪", "九龙峪景区", "广福寺").let { if (reverse) it.reversed() else it }
        val requests = CopyOnWriteArrayList<JsonObject>()
        val events = CopyOnWriteArrayList<AssistantSearchEvent>()
        val began = SystemClock.elapsedRealtime()
        val wrapped = object : PlaceSearchDataSource {
            override suspend fun search(keyword: String, city: String?): List<PlaceCandidate> {
                val start = SystemClock.elapsedRealtime()
                var errorCode: Int? = null
                var count = 0
                try {
                    return source.search(keyword, city).also { count = it.size }
                } catch (e: AmapServiceException) {
                    errorCode = e.errorCode
                    throw e
                } finally {
                    requests += buildJsonObject {
                        put("mode", mode); put("run", run)
                        // Public fixture names only, never user input or credentials.
                        put("fixture", keyword); put("startMs", start - began)
                        put("elapsedMs", SystemClock.elapsedRealtime() - start)
                        put("resultCount", count)
                        errorCode?.let { put("errorCode", it) }
                    }
                }
            }
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val controller = PlaceAssistantController("emulator-pacing-diagnostic", scope,
            PlaceIntentParser { _, _ -> names.map { PlaceIntent(it, "", it) } }, wrapped,
            object : PlaceImport {
                override suspend fun receipt(operationId: String): ImportReceipt? = null
                override suspend fun commit(confirmation: ImportConfirmation): ImportReceipt =
                    error("Diagnostic must never write places")
            }, resolveDefaultCity = { "青州市" }, diagnostics = AssistantSearchDiagnostics { events += it })
        try {
            withContext(Dispatchers.Main) {
                controller.environment(true, true, true)
                controller.editInput(names.joinToString("、"))
                controller.submit()
            }
            val state = withTimeout(90_000) {
                controller.state.first { !it.busy && it.items.size == names.size }
            }
            val report = buildJsonObject {
                put("mode", mode); put("run", run); put("reverse", reverse)
                put("requestCount", requests.size)
                put("requests", JsonArray(requests.sortedBy { it.getValue("startMs").jsonPrimitive.long }))
                put("events", JsonArray(events.map { Json.parseToJsonElement(it.toJsonLine()) }))
                put("items", JsonArray(state.items.map {
                    buildJsonObject {
                        put("fixture", it.intent.query); put("label", it.label); put("status", it.status.name)
                        put("name", it.poi?.name.orEmpty()); put("city", it.poi?.cityName.orEmpty())
                    }
                }))
            }
            val file = File(context.externalCacheDir ?: context.cacheDir, "diagnostics/pacing/$run.json")
            file.parentFile!!.mkdirs()
            file.writeText(report.toString())
            println("PACING_DIAGNOSTIC $report")
            assertEquals(names.size, requests.size)
            val actualStarts = requests.map { it.getValue("startMs").jsonPrimitive.long }.sorted()
            assertTrue("Production must pace actual SDK starts without a diagnostic wrapper",
                actualStarts.zipWithNext().all { (a, b) -> b - a >= 700 })
            assertTrue(state.selected.isEmpty())
            val bad = state.items.filter {
                it.status != IntakeStatus.READY || !sameCity(it.poi?.cityName, "青州市") ||
                    it.poi?.name?.contains(it.intent.query.removePrefix("青州").removeSuffix("景区")) != true
            }
            assertTrue("Initial searches must resolve the intended places: " +
                bad.joinToString { "${it.intent.query}=${it.status}/${it.poi?.name}" }, bad.isEmpty())
        } finally {
            withContext(Dispatchers.Main) { controller.cancel() }
            scope.cancel()
        }
    }
}
