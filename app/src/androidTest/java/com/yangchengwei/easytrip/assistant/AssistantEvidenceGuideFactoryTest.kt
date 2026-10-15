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
import com.yangchengwei.easytrip.assistant.data.RoomPlaceImport
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import com.yangchengwei.easytrip.place.amap.*
import com.yangchengwei.easytrip.place.data.RoomSavedPlaceRepository
import com.yangchengwei.easytrip.trip.data.RoomTripRepository
import com.yangchengwei.easytrip.trip.domain.CreateTrip
import java.io.File
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Production Factory, store, multimodal protocol, transport and normalizer. HTTP/POI are fixtures. */
class AssistantEvidenceGuideFactoryTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun sixImagesUseOneEvidenceExtractionAndOne64kResolution() {
        val context = compose.activity
        val root = File(context.noBackupFilesDir, "batch-factory-${UUID.randomUUID()}").apply { mkdirs() }
        val config = AssistantConfigStore(object : ContextWrapper(context) { override fun getNoBackupFilesDir() = root })
        config.save(ProviderConfig("https://api.deepseek.com", "deepseek-flash", "not-real"))
        val images = AndroidAssistantImageStore(context, File(root, "images"), false)
        val db = Room.inMemoryDatabaseBuilder(context, EasyTripDatabase::class.java).build()
        val vmStore = ViewModelStore()
        val release = CountDownLatch(1)
        val extracts = AtomicInteger()
        val normalizes = AtomicInteger()
        val searches = AtomicInteger()
        val calls = java.util.concurrent.CopyOnWriteArrayList<String>()
        val http = OkHttpClient.Builder().addInterceptor { chain ->
            val buffer = Buffer(); chain.request().body!!.writeTo(buffer)
            val body = Json.parseToJsonElement(buffer.readUtf8()).jsonObject
            val tool = body["tools"]!!.jsonArray.single().jsonObject["function"]!!.jsonObject["name"]!!.jsonPrimitive.content
            calls += tool
            val response = if (tool == "extract_guide_evidence") {
                extracts.incrementAndGet()
                val blocks = body["messages"]!!.jsonArray.flatMap { it.jsonObject["content"] as? JsonArray ?: emptyList() }
                assertEquals(6, blocks.count { it.jsonObject["type"]?.jsonPrimitive?.content == "image_url" })
                check(release.await(20, TimeUnit.SECONDS))
                batchedDeviceReply(buildJsonObject { putJsonArray("sources") {
                    addJsonObject { put("sourceId", "text"); put("status", "OK")
                        putJsonArray("evidence") { addJsonObject { put("rawQuote", "北城美术馆 推荐山间客栈 园内LED舞台"); put("kind", "BODY_EXCERPT"); put("basis", "RECOMMENDED"); put("uncertain", false); put("detail", "") } } }
                    (1..6).forEach { n -> addJsonObject {
                        put("sourceId", "image_$n"); put("status", "NO_PLACES"); putJsonArray("evidence") {}
                    } }
                } }, tool)
            } else {
                assertEquals("resolve_guide_entities", tool); normalizes.incrementAndGet()
                batchedDeviceReply(Json.parseToJsonElement("""{"evidenceResults":[{"e":0,"status":"PARSED","mentions":[{"literal":"美术馆","occurrence":0,"city":"北城","cityEvidenceRefs":[0],"role":"PLACE","parentLiteral":"","partKind":null,"uncertain":false,"classification":{"purpose":"SIGHTSEEING","granularity":"DESTINATION","independentVisit":true,"evidenceQuote":"北城美术馆"}},{"literal":"山间客栈","occurrence":0,"city":"","cityEvidenceRefs":[],"role":"PLACE","parentLiteral":"","partKind":null,"uncertain":false,"classification":{"purpose":"LODGING","granularity":"DESTINATION","independentVisit":false,"evidenceQuote":"推荐山间客栈"}},{"literal":"LED舞台","occurrence":0,"city":"","cityEvidenceRefs":[],"role":"PLACE","parentLiteral":"","partKind":null,"uncertain":false,"classification":{"purpose":"OTHER","granularity":"INTERNAL_DETAIL","independentVisit":false,"evidenceQuote":"园内LED舞台"}}]}],"groups":[{"members":[[0,0]],"relation":"SAME_LITERAL","evidenceRefs":[0],"frozenAnchors":[]},{"members":[[0,1]],"relation":"SAME_LITERAL","evidenceRefs":[0],"frozenAnchors":[]},{"members":[[0,2]],"relation":"SAME_LITERAL","evidenceRefs":[0],"frozenAnchors":[]}]}""").jsonObject, tool)
            }
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(200).message("OK").body(response.toResponseBody()).build()
        }.build()
        try {
            val png = java.io.ByteArrayOutputStream().use { out ->
                Bitmap.createBitmap(40, 40, Bitmap.Config.ARGB_8888).also {
                    it.compress(Bitmap.CompressFormat.PNG, 100, out); it.recycle()
                }; out.toByteArray()
            }
            val loader = object : AssistantLinkLoader {
                override suspend fun load(sessionId: String, input: AssistantLinkInput, existing: List<AssistantImage>,
                    onProgress: (String) -> Unit): LoadedAssistantLink {
                    var added = emptyList<AssistantImage>()
                    repeat(6) { added = added + images.importBytes(sessionId, png, existing + added) }
                    return LoadedAssistantLink("北城美术馆 推荐山间客栈 园内LED舞台", added)
                }
                override suspend fun discard(sessionId: String, images: List<AssistantImage>) {}
            }
            val trip = runBlocking { RoomTripRepository(db.tripDao()).createTrip(CreateTrip("北城之旅", 1)) }
            val scheduler = GuideBatchScheduler()
            val preparation = Mutex()
            lateinit var model: PlaceAssistantViewModel
            compose.runOnUiThread {
                model = PlaceAssistantViewModel.Factory(trip, config, RoomPlaceImport(db), RoomSavedPlaceRepository(db),
                    images, linkLoader = loader, guideEvidenceParserFactory = { snapshot ->
                        BatchedGuideMaterialParser(snapshot, GuideBatchFiles(File(root, "bodies"), images, preparation),
                            GuideBatchBody(EvidenceGuideWireProtocol), AssistantTransport(http), scheduler, protocol = EvidenceGuideWireProtocol)
                    }, evidenceResolverOverride = ModelGuideEvidenceResolver(config::read, AssistantTransport(http)))
                    .create(PlaceAssistantViewModel::class.java)
                vmStore.put("assistant", model)
                model.controller.updateSource(object : PlaceSearchDataSource {
                    override suspend fun search(keyword: String, city: String?): List<PlaceCandidate> {
                        searches.incrementAndGet()
                        return listOf(PlaceCandidate("museum", "美术馆", "地址", GeoPoint(30.2, 120.1), null, "北城"))
                    }
                })
                model.controller.environment(true, true, true)
                model.controller.editCity("北城")
            }
            compose.setContent {
                val state by model.controller.state.collectAsState()
                EasyTripTheme {
                    Box(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                        PlaceAssistantPanel(AssistantWorkspace(model, state, true, null, "北城之旅", {}, {}, {}, {}))
                    }
                }
            }
            compose.runOnIdle { model.controller.editInput("北城 https://xhslink.cn/o/fixture"); model.controller.submit() }
            compose.waitUntil(15_000) { model.controller.state.value.sourceProgress.count { it.stage == SourceStage.UNDERSTANDING } == 7 }
            compose.onAllNodesWithText("已发送，等待模型理解", substring = true).assertCountEquals(7)
            assertEquals(1, extracts.get()); assertEquals(0, normalizes.get())
            release.countDown()
            compose.waitUntil(15_000) { !model.controller.state.value.busy && model.controller.state.value.eligible.size == 1 }
            assertEquals(listOf("extract_guide_evidence", "resolve_guide_entities"), calls.toList())
            assertEquals(1, searches.get())
            assertEquals(listOf("美术馆"), model.controller.state.value.items.map { it.intent.query })
            compose.onNodeWithTag("assistant-select-A").performScrollTo().assertIsDisplayed().performClick()
            compose.onNodeWithTag("assistant-save").assertIsEnabled().assertIsDisplayed()
            assertTrue(File(root, "bodies").listFiles()!!.isEmpty())
        } finally {
            release.countDown()
            compose.runOnUiThread { vmStore.clear() }
            runBlocking { delay(250) }
            db.close(); root.deleteRecursively()
        }
    }
}
