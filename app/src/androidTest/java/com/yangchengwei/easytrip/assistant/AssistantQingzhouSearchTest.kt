package com.yangchengwei.easytrip.assistant

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yangchengwei.easytrip.amap.AmapPrivacyGate
import com.yangchengwei.easytrip.amap.TestConsentGate
import com.yangchengwei.easytrip.place.amap.AmapPlaceDataSource
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.amap.PlaceSearchDataSource
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Emulator-only live AMap check. No model credentials, production DB, collection or itinerary writes. */
@RunWith(AndroidJUnit4::class)
class AssistantQingzhouSearchTest {
    @Test fun initialCityQualifiedQueriesFindAllFourPlacesAndPersistRedactedDiagnostics() = runBlocking {
        assumeTrue("Never run this diagnostic on a physical phone",
            Build.HARDWARE in setOf("ranchu", "goldfish") &&
                (Build.FINGERPRINT.contains("/sdk_") || Build.MODEL.contains("sdk_gphone")))
        val context = ApplicationProvider.getApplicationContext<Context>()
        AmapPrivacyGate.create(context).apply { reportShown(); reportDecision(true) }
        val token = requireNotNull(TestConsentGate().apply { show() }.decide(true))
        val source = AmapPlaceDataSource(context, token)
        val requests = CopyOnWriteArrayList<Pair<String, String?>>()
        val events = CopyOnWriteArrayList<AssistantSearchEvent>()
        val persisted = AndroidAssistantSearchDiagnostics(context)
        val names = listOf("偶园", "昭德古街", "九龙峪景区", "广福寺")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val controller = PlaceAssistantController("emulator-diagnostic-only", scope,
            PlaceIntentParser { _, _ -> names.map { PlaceIntent(it, "", it) } },
            object : PlaceSearchDataSource by source {
                override suspend fun search(keyword: String, city: String?): List<PlaceCandidate> {
                    requests += keyword to city
                    return source.search(keyword, city)
                }
            }, object : PlaceImport {
                override suspend fun receipt(operationId: String): ImportReceipt? = null
                override suspend fun commit(confirmation: ImportConfirmation): ImportReceipt = error("Unexpected write")
            }, resolveDefaultCity = { "青州市" }, diagnostics = AssistantSearchDiagnostics {
                events += it
                persisted.record(it)
            })
        try {
            withContext(Dispatchers.Main) {
                controller.environment(true, true, true)
                controller.editInput(names.joinToString("、"))
                controller.submit()
            }
            val state = withTimeout(90_000) { controller.state.first { !it.busy && it.items.size == 4 } }
            assertEquals(setOf("青州偶园", "青州昭德古街", "青州九龙峪景区", "青州广福寺"), requests.map { it.first }.toSet())
            assertEquals(4, requests.size)
            assertTrue(requests.all { it.second == "青州市" })
            state.items.forEach {
                println("QINGZHOU_LIVE ${it.label} status=${it.status} candidates=${it.candidates.size} city=${it.poi?.cityName} name=${it.poi?.name}")
                assertEquals("Expected first-attempt match for ${it.intent.query}", IntakeStatus.READY, it.status)
                assertTrue(sameCity(it.poi?.cityName, "青州市"))
                val expectedName = mapOf("偶园" to "偶园", "昭德古街" to "昭德",
                    "九龙峪景区" to "九龙峪", "广福寺" to "广福寺").getValue(it.intent.query)
                assertTrue("Must select the intended attraction, not just another POI in the city",
                    it.poi?.name?.contains(expectedName) == true)
            }
            assertTrue(state.selected.isEmpty())
            val file = File(context.externalCacheDir ?: context.cacheDir, "diagnostics/assistant-search.jsonl")
            assertTrue(file.isFile)
            assertTrue(file.length() <= 64 * 1024)
            val session = events.first().session
            val records = file.readLines().map { Json.parseToJsonElement(it).jsonObject }
                .filter { it["session"]?.jsonPrimitive?.content == session }
            assertEquals(8, records.size)
            assertEquals(4, records.count { it["outcome"]?.jsonPrimitive?.content == "READY" })
            val text = records.joinToString()
            (names + listOf("青州", "emulator-diagnostic-only")).forEach { assertFalse(text.contains(it)) }
        } finally {
            withContext(Dispatchers.Main) { controller.cancel() }
            scope.cancel()
        }
    }
}
