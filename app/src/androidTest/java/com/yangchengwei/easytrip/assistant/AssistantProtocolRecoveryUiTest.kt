package com.yangchengwei.easytrip.assistant

import android.content.ContextWrapper
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Real parser/controller/UI/Room; only external HTTP, source platform and POI service are fixtures. */
class AssistantProtocolRecoveryUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun defaultPlainTextParserRecordsTransportFailureWithoutRemoteService() {
        val context = compose.activity
        val folder = File(context.noBackupFilesDir, "protocol-text-${UUID.randomUUID()}").apply { mkdirs() }
        val config = AssistantConfigStore(object : ContextWrapper(context) { override fun getNoBackupFilesDir() = folder })
        // A closed loopback port exercises the default parser without contacting a model service.
        val port = java.net.ServerSocket(0, 1, java.net.InetAddress.getLoopbackAddress()).use { it.localPort }
        config.save(ProviderConfig("https://127.0.0.1:$port", "fixture", "not-real"))
        val db = Room.inMemoryDatabaseBuilder(context, EasyTripDatabase::class.java).build()
        val store = ViewModelStore()
        val repo = RoomSavedPlaceRepository(db)
        val trip = runBlocking { RoomTripRepository(db.tripDao()).createTrip(CreateTrip("北城协议验证", 1)) }
        val diagnostics = AssistantProtocolLogFile(File(folder, "diagnostics"))
        try {
            val model = PlaceAssistantViewModel.Factory(trip, config, RoomPlaceImport(db), repo,
                protocolDiagnostics = diagnostics).create(PlaceAssistantViewModel::class.java)
            store.put("assistant", model)
            compose.runOnIdle {
                model.controller.environment(true, true, true)
                model.controller.editCity("北城")
                model.controller.editInput("北城美术馆")
                model.controller.submit()
            }
            compose.waitUntil(15_000) { !model.controller.state.value.busy && model.controller.state.value.error != null }
            val log = File(folder, "diagnostics/assistant-protocol.jsonl")
            assertTrue("Default plain-text path must record its failure", log.isFile)
            val events = log.readLines().map { Json.parseToJsonElement(it).jsonObject }
            assertEquals(1, events.size)
            assertEquals("TEXT", events.single().getValue("stage").jsonPrimitive.content)
            assertEquals("FAILED", events.single().getValue("outcome").jsonPrimitive.content)
            assertTrue(runBlocking { repo.observePlaces(trip, emptySet()).first() }.isEmpty())
        } finally {
            compose.runOnIdle { store.clear() }
            db.close()
            folder.deleteRecursively()
        }
    }

    @Test fun plainTextRepairRecordsBothAttemptsWithoutAutomaticallySaving() {
        val context = compose.activity
        val folder = File(context.noBackupFilesDir, "protocol-text-repair-${UUID.randomUUID()}").apply { mkdirs() }
        val config = AssistantConfigStore(object : ContextWrapper(context) { override fun getNoBackupFilesDir() = folder })
        val provider = ProviderConfig("https://example.invalid", "fixture", "not-real")
        config.save(provider)
        val db = Room.inMemoryDatabaseBuilder(context, EasyTripDatabase::class.java).build()
        val store = ViewModelStore()
        val repo = RoomSavedPlaceRepository(db)
        val trip = runBlocking { RoomTripRepository(db.tripDao()).createTrip(CreateTrip("北城协议验证", 1)) }
        val diagnostics = AssistantProtocolLogFile(File(folder, "diagnostics"))
        val calls = AtomicInteger()
        val searches = AtomicInteger()
        val http = OkHttpClient.Builder().addInterceptor { chain ->
            val args = if (calls.incrementAndGet() == 1) """{"items":[]}"""
                else """{"items":[{"query":"美术馆","city":"北城","sourceSpan":"北城美术馆","confidence":0.9}],"overflow":false}"""
            val quoted = JsonPrimitive(args).toString()
            val output = """{"choices":[{"finish_reason":"stop","message":{"tool_calls":[{"type":"function","function":{"name":"search_place_batch","arguments":$quoted}}]}}]}"""
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body(output.toResponseBody()).build()
        }.build()
        try {
            val model = PlaceAssistantViewModel(trip, config, RoomPlaceImport(db), repo,
                parser = DeepSeekPlaceParser(config::read, http, diagnostics = diagnostics),
                protocolDiagnostics = diagnostics)
            store.put("assistant", model)
            model.controller.updateSource(object : PlaceSearchDataSource {
                override suspend fun search(keyword: String, city: String?): List<PlaceCandidate> {
                    searches.incrementAndGet()
                    return listOf(PlaceCandidate("museum", "美术馆", "北城", GeoPoint(30.2, 120.1), null, "北城"))
                }
            })
            compose.runOnIdle {
                model.controller.environment(true, true, true)
                model.controller.editCity("北城")
                model.controller.editInput("北城美术馆")
                model.controller.submit()
            }
            compose.waitUntil(15_000) { !model.controller.state.value.busy && model.controller.state.value.items.size == 1 }
            assertEquals(2, calls.get())
            assertEquals(1, searches.get())
            assertNull(model.controller.state.value.error)
            val events = File(folder, "diagnostics/assistant-protocol.jsonl").readLines()
                .map { Json.parseToJsonElement(it).jsonObject }
            assertEquals(listOf("REPAIRING", "SUCCESS"), events.map { it.getValue("outcome").jsonPrimitive.content })
            assertTrue(events.all { it.getValue("stage").jsonPrimitive.content == "TEXT" })
            assertTrue(runBlocking { repo.observePlaces(trip, emptySet()).first() }.isEmpty())
        } finally {
            compose.runOnIdle { store.clear() }
            db.close()
            folder.deleteRecursively()
        }
    }

    @Test fun partialProtocolFailureRetainsCompletedSourcesAndRetriesOnlyFailedImage() {
        val context = compose.activity
        val folder = File(context.noBackupFilesDir, "protocol-ui-${UUID.randomUUID()}").apply { mkdirs() }
        val config = AssistantConfigStore(object : ContextWrapper(context) { override fun getNoBackupFilesDir() = folder })
        val provider = ProviderConfig("https://example.invalid", "fixture", "not-real")
        config.save(provider)
        val images = AndroidAssistantImageStore(context, File(folder, "images"))
        val db = Room.inMemoryDatabaseBuilder(context, EasyTripDatabase::class.java).build()
        val store = ViewModelStore()
        val repo = RoomSavedPlaceRepository(db)
        val trip = runBlocking { RoomTripRepository(db.tripDao()).createTrip(CreateTrip("北城协议验证", 1)) }
        val counts = List(4) { AtomicInteger() } // text, image1, image2, normalization
        val searches = AtomicInteger()
        val noteReads = AtomicInteger()
        val diagnostics = AssistantProtocolLogFile(File(folder, "diagnostics"))
        fun response(args: String, tool: String = "search_place_batch") =
            """{"choices":[{"finish_reason":"tool_calls","message":{"tool_calls":[{"type":"function","function":{"name":"$tool","arguments":${JsonPrimitive(args)}}}]}}]}"""
        val http = OkHttpClient.Builder().addInterceptor { chain ->
            val buffer = okio.Buffer()
            chain.request().body!!.writeTo(buffer)
            val request = Json.parseToJsonElement(buffer.readUtf8()).jsonObject
            val tool = request["tools"]!!.jsonArray.single().jsonObject["function"]!!.jsonObject["name"]!!.jsonPrimitive.content
            val user = request["messages"]!!.jsonArray[1].jsonObject["content"]!!
            val output = when {
                tool == "normalize_place_candidates" -> {
                    counts[3].incrementAndGet()
                    response("""{"groups":[{"canonical":0,"members":[0,1]}],"discarded":[]}""", tool)
                }
                user is JsonPrimitive -> {
                    counts[0].incrementAndGet()
                    response("""{"items":[{"query":"美术馆","city":"北城","sourceSpan":"美术馆"}],"overflow":false}""")
                }
                user.jsonArray[0].jsonObject["text"]!!.jsonPrimitive.content.startsWith("图 2\n") -> {
                    if (counts[2].incrementAndGet() <= 2) response("""{"items":[]""".dropLast(1) + "}")
                    else response("""{"items":[],"overflow":false}""")
                }
                else -> {
                    counts[1].incrementAndGet()
                    response("""{"items":[{"query":"北城美术馆","city":"北城","sourceSpan":"北城美术馆","uncertain":false,"targetBasis":"author_mark","targetDetail":"作者标记","entityRole":"place","parentName":null,"confidence":0.9}],"overflow":false}""")
                }
            }
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body(output.toResponseBody()).build()
        }.build()
        try {
            val png = java.io.ByteArrayOutputStream().use { stream ->
                Bitmap.createBitmap(40, 40, Bitmap.Config.ARGB_8888).also {
                    it.compress(Bitmap.CompressFormat.PNG, 100, stream); it.recycle()
                }
                stream.toByteArray()
            }
            val model = PlaceAssistantViewModel(trip, config, RoomPlaceImport(db), repo, images = images,
                materialTextParserOverride = DeepSeekPlaceParser({ provider }, http, travelMaterials = true),
                imageParserOverride = ImagePlaceParser({ provider }, AssistantTransport(http, diagnostics), travelMaterials = true),
                normalizerOverride = ModelPlaceNormalizer({ provider }, AssistantTransport(http, diagnostics)),
                linkLoader = object : AssistantLinkLoader {
                    override suspend fun load(sessionId: String, input: AssistantLinkInput, existing: List<AssistantImage>, onProgress: (String) -> Unit): LoadedAssistantLink {
                        noteReads.incrementAndGet()
                        val first = images.importBytes(sessionId, png, existing)
                        val second = images.importBytes(sessionId, png, existing + first)
                        return LoadedAssistantLink("北城美术馆", listOf(first, second))
                    }
                    override suspend fun discard(sessionId: String, images: List<AssistantImage>) {}
                })
            store.put("assistant", model)
            model.controller.updateSource(object : PlaceSearchDataSource {
                override suspend fun search(keyword: String, city: String?): List<PlaceCandidate> {
                    searches.incrementAndGet()
                    return listOf(PlaceCandidate("museum", "美术馆", "北城", GeoPoint(30.2, 120.1), null, "北城"))
                }
            })
            compose.setContent {
                val state by model.controller.state.collectAsState()
                val density = LocalDensity.current.density
                EasyTripTheme {
                    CompositionLocalProvider(LocalDensity provides Density(density, 2f)) {
                        Box(Modifier.sizeIn(maxWidth = 320.dp, maxHeight = 520.dp).fillMaxSize()) {
                            PlaceAssistantPanel(AssistantWorkspace(model, state, true, null, "北城协议验证", {}, {}, {}, {}))
                        }
                    }
                }
            }
            compose.runOnIdle {
                model.controller.environment(true, true, true)
                model.controller.editCity("北城")
                model.controller.editInput("北城 https://xhslink.cn/o/fixture")
                model.controller.submit()
            }
            compose.waitUntil(15_000) { !model.controller.state.value.busy && model.controller.state.value.items.size == 1 }
            assertEquals(listOf(1, 1, 2, 1), counts.map { it.get() })
            assertEquals(1, searches.get())
            assertEquals(IntakeStatus.READY, model.controller.state.value.items.single().status)
            compose.onNodeWithTag("assistant-partial-notice").performScrollTo().assertIsDisplayed()
            val error = "图 2 · 图片理解：返回格式不完整；未查询地点，可重试失败素材。"
            compose.onNodeWithText(error).performScrollTo().assertIsDisplayed()
            val screenshot = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            File(context.filesDir, "protocol-error-ui.png").outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG, 100, it) }
            screenshot.recycle()
            compose.onNodeWithTag("assistant-retry-images").performScrollTo().assertIsEnabled().performClick()
            compose.waitUntil(15_000) { !model.controller.state.value.busy && model.controller.state.value.items.size == 1 }
            assertEquals(listOf(1, 1, 3, 2), counts.map { it.get() })
            assertEquals(1, noteReads.get())
            assertEquals(1, searches.get())
            assertTrue(runBlocking { repo.observePlaces(trip, emptySet()).first() }.isEmpty())
            assertNull(model.controller.state.value.error)
            assertTrue(File(folder, "diagnostics/assistant-protocol.jsonl").readText().contains("INVALID_ARGUMENTS"))
        } finally {
            compose.runOnIdle { store.clear() }
            db.close()
            folder.deleteRecursively()
        }
    }
}
