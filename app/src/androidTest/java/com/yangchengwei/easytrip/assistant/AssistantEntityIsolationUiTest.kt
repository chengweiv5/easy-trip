package com.yangchengwei.easytrip.assistant

import android.content.ContextWrapper
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.yangchengwei.easytrip.assistant.data.RoomPlaceImport
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.amap.PlaceSearchDataSource
import com.yangchengwei.easytrip.place.data.RoomSavedPlaceRepository
import com.yangchengwei.easytrip.trip.data.RoomTripRepository
import com.yangchengwei.easytrip.trip.domain.CreateTrip
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Real production controller/resolver, isolated Room, HTTP and POI fixtures only. */
class AssistantEntityIsolationUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun validDestinationCanBeSavedAndRetriedWhileMalformedExitRemainsPending() {
        val context = compose.activity
        val root = File(context.noBackupFilesDir, "entity-isolation-${UUID.randomUUID()}").apply { mkdirs() }
        val config = AssistantConfigStore(object : ContextWrapper(context) {
            override fun getNoBackupFilesDir() = root
        }).apply { save(ProviderConfig("https://api.deepseek.com", "deepseek-flash", "fixture-only")) }
        val images = AndroidAssistantImageStore(context, File(root, "images"), false)
        val db = Room.inMemoryDatabaseBuilder(context, EasyTripDatabase::class.java).build()
        val places = RoomSavedPlaceRepository(db)
        val vmStore = ViewModelStore()
        val extracts = AtomicInteger()
        val resolutions = AtomicInteger()
        val searches = AtomicInteger()
        val destination = "徐州饮食文化博物馆"
        val quote = "徐博物馆出口→$destination"
        val http = OkHttpClient.Builder().addInterceptor { chain ->
            val buffer = Buffer(); chain.request().body!!.writeTo(buffer)
            val request = Json.parseToJsonElement(buffer.readUtf8()).jsonObject
            val tool = request.getValue("tools").jsonArray.single().jsonObject
                .getValue("function").jsonObject.getValue("name").jsonPrimitive.content
            val response = when (tool) {
                "extract_guide_evidence" -> {
                    extracts.incrementAndGet()
                    buildJsonObject { putJsonArray("sources") {
                        add(source("text", quote, "BODY_EXCERPT"))
                        add(source("image_1", destination, "AUTHOR_LABEL"))
                    } }
                }
                "resolve_guide_entities" -> {
                    resolutions.incrementAndGet()
                    buildJsonObject {
                        putJsonArray("evidenceResults") {
                            addJsonObject {
                                put("e", 0); put("status", "PARSED")
                                putJsonArray("mentions") {
                                    add(mention("徐博物馆出口", quote, exit = true))
                                    add(mention(destination, quote))
                                }
                            }
                            addJsonObject {
                                put("e", 1); put("status", "PARSED")
                                put("mentions", JsonArray(listOf(mention(destination, destination))))
                            }
                        }
                        putJsonArray("groups") { addJsonObject {
                            put("members", Json.parseToJsonElement("[[0,1],[1,0]]"))
                            put("relation", "SAME_LITERAL")
                            put("evidenceRefs", Json.parseToJsonElement("[0,1]"))
                            putJsonArray("frozenAnchors") {}
                        } }
                    }
                }
                else -> error("Unexpected model operation: $tool")
            }
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200)
                .message("fixture").body(batchedDeviceReply(response, tool).toResponseBody()).build()
        }.build()
        try {
            val png = java.io.ByteArrayOutputStream().use { out ->
                Bitmap.createBitmap(40, 40, Bitmap.Config.ARGB_8888).also {
                    it.compress(Bitmap.CompressFormat.PNG, 100, out); it.recycle()
                }
                out.toByteArray()
            }
            val loader = object : AssistantLinkLoader {
                override suspend fun load(sessionId: String, input: AssistantLinkInput, existing: List<AssistantImage>,
                    onProgress: (String) -> Unit) =
                    LoadedAssistantLink(quote, listOf(images.importBytes(sessionId, png, existing)))
                override suspend fun discard(sessionId: String, images: List<AssistantImage>) {}
            }
            val trip = runBlocking { RoomTripRepository(db.tripDao()).createTrip(CreateTrip("徐州之旅", 1)) }
            val preparation = Mutex()
            val scheduler = GuideBatchScheduler()
            lateinit var model: PlaceAssistantViewModel
            compose.runOnUiThread {
                model = PlaceAssistantViewModel.Factory(trip, config, RoomPlaceImport(db), places, images,
                    linkLoader = loader, guideEvidenceParserFactory = { snapshot ->
                        BatchedGuideMaterialParser(snapshot, GuideBatchFiles(File(root, "bodies"), images, preparation),
                            GuideBatchBody(EvidenceGuideWireProtocol), AssistantTransport(http), scheduler,
                            protocol = EvidenceGuideWireProtocol)
                    }, evidenceResolverOverride = ModelGuideEvidenceResolver(config::read, AssistantTransport(http)))
                    .create(PlaceAssistantViewModel::class.java)
                vmStore.put("assistant", model)
                model.controller.updateSource(object : PlaceSearchDataSource {
                    override suspend fun search(keyword: String, city: String?): List<PlaceCandidate> {
                        assertEquals(destination, keyword)
                        searches.incrementAndGet()
                        return listOf(PlaceCandidate("museum-fixture", destination, "测试地址",
                            GeoPoint(34.26, 117.18), null, "徐州市"))
                    }
                })
                model.controller.environment(true, true, true)
                model.controller.editCity("徐州市")
            }
            compose.setContent {
                val state by model.controller.state.collectAsState()
                EasyTripTheme {
                    // Match WorkspaceScaffold's system-bar inset contract.
                    Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(horizontal = 16.dp)) {
                        PlaceAssistantPanel(AssistantWorkspace(model, state, true, null, "徐州之旅",
                            {}, {}, {}, {}))
                    }
                }
            }
            compose.runOnIdle {
                model.controller.editInput("徐州 https://xhslink.cn/o/fixture")
                model.controller.submit()
            }
            compose.waitUntil(15_000) {
                !model.controller.state.value.busy && model.controller.state.value.eligible.size == 1
            }
            assertEquals(listOf(destination), model.controller.state.value.items.map { it.intent.query })
            assertTrue(model.controller.state.value.normalizationPending)
            assertEquals(1, searches.get())
            compose.onNodeWithTag("assistant-select-A").performScrollTo().performClick()
            compose.onNodeWithTag("assistant-save").assertIsEnabled()
            InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()?.let { bitmap ->
                File(context.filesDir, "entity-isolation-ui.png").outputStream().use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
                bitmap.recycle()
            }
            compose.onNodeWithTag("assistant-save").performClick()
            compose.waitUntil(15_000) {
                !model.controller.state.value.saving &&
                    model.controller.state.value.items.singleOrNull()?.status == IntakeStatus.ALREADY_SAVED
            }
            assertEquals(1, runBlocking { places.observePlaces(trip, emptySet()).first() }.size)
            compose.runOnIdle { model.controller.retryNormalization() }
            compose.waitUntil(15_000) { !model.controller.state.value.busy && resolutions.get() == 2 }
            assertEquals(1, model.controller.state.value.items.size)
            assertEquals(IntakeStatus.ALREADY_SAVED, model.controller.state.value.items.single().status)
            assertTrue(model.controller.state.value.normalizationPending)
            assertEquals(1, extracts.get())
            assertEquals(1, searches.get())
            assertEquals(1, runBlocking { places.observePlaces(trip, emptySet()).first() }.size)
        } finally {
            compose.runOnUiThread { vmStore.clear() }
            runBlocking { delay(250) }
            db.close()
            root.deleteRecursively()
        }
    }

    private fun source(id: String, quote: String, kind: String) = buildJsonObject {
        put("sourceId", id); put("status", "OK")
        putJsonArray("evidence") { addJsonObject {
            put("rawQuote", quote); put("kind", kind); put("basis", "RECOMMENDED")
            put("uncertain", false); put("detail", "")
        } }
    }

    private fun mention(literal: String, quote: String, exit: Boolean = false) = buildJsonObject {
        put("literal", literal); put("occurrence", 0); put("city", "")
        putJsonArray("cityEvidenceRefs") {}
        put("role", if (exit) "DISTINCT_PART" else "PLACE")
        put("parentLiteral", if (exit) "徐州博物馆" else "")
        put("partKind", if (exit) JsonPrimitive("EXIT") else JsonNull); put("uncertain", false)
        putJsonObject("classification") {
            put("purpose", if (exit) "TRANSPORT" else "SIGHTSEEING")
            put("granularity", if (exit) "INTERNAL_DETAIL" else "DESTINATION")
            put("independentVisit", !exit); put("evidenceQuote", quote)
        }
    }
}
