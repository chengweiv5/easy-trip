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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.yangchengwei.easytrip.assistant.cityfirst.*
import com.yangchengwei.easytrip.assistant.data.*
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import com.yangchengwei.easytrip.place.amap.*
import com.yangchengwei.easytrip.place.data.RoomSavedPlaceRepository
import com.yangchengwei.easytrip.core.model.TravelMode
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

/** Real Room + handoff + ViewModel + UI; models, platform loading and POI service are fixtures. */
class AssistantPartialGuideRecoveryTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun singleCollectionFinishesGuideAndReopensEmptyDespiteFailedImage() = exercise(false)
    @Test fun batchedSingleCollectionFinishesGuideWithoutRetryingFailedImage() = exercise(true)
    @Test fun cancelledNormalizationRestoresSuccessfulSourcesWithoutExtractingAgain() = exercise(true, true)

    private fun exercise(batched: Boolean, cancelNormalization: Boolean = false) {
        val context = compose.activity
        val root = File(context.noBackupFilesDir, "partial-guide-${UUID.randomUUID()}").apply { mkdirs() }
        val config = AssistantConfigStore(object : ContextWrapper(context) { override fun getNoBackupFilesDir() = root })
        config.save(ProviderConfig("https://example.invalid", "fixture", "not-real"))
        val db = Room.inMemoryDatabaseBuilder(context, EasyTripDatabase::class.java).build()
        val creation = RoomGuideCreation(db)
        val repo = RoomSavedPlaceRepository(db)
        val stores = ViewModelStore()
        val imageStore = AndroidAssistantImageStore(context, File(root, "material-images"), false)
        val reads = AtomicInteger()
        val texts = AtomicInteger()
        val pictures = AtomicInteger()
        val searches = AtomicInteger()
        val normalizations = AtomicInteger()
        val scheduler = GuideBatchScheduler()
        val preparation = Mutex()
        val batches = AtomicInteger()
        val batchHttp = OkHttpClient.Builder().addInterceptor { chain ->
            val buffer = Buffer(); chain.request().body!!.writeTo(buffer)
            val request = buffer.readUtf8()
            val number = batches.incrementAndGet()
            if (number == 1) texts.incrementAndGet()
            pictures.incrementAndGet()
            if (number > 1) assertFalse(request.contains("目标text："))
            val arguments = buildJsonObject { putJsonArray("sources") {
                if (number == 1) addJsonObject {
                    put("sourceId", "text"); put("status", "OK")
                    putJsonArray("items") { add(batchedDeviceItem("美术馆")) }
                }
                addJsonObject {
                    put("sourceId", "image_1"); put("status", if (number == 1) "UNREADABLE" else "OK")
                    putJsonArray("items") {
                        if (number > 1) { add(batchedDeviceItem("美术馆", true)); add(batchedDeviceItem("公园", true)) }
                    }
                }
            } }
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(200).message("OK").body(batchedDeviceReply(arguments).toResponseBody()).build()
        }.build()
        val png = java.io.ByteArrayOutputStream().use { stream ->
            Bitmap.createBitmap(40, 40, Bitmap.Config.ARGB_8888).also {
                it.compress(Bitmap.CompressFormat.PNG, 100, stream); it.recycle()
            }
            stream.toByteArray()
        }
        val loader = object : AssistantLinkLoader {
            override suspend fun load(sessionId: String, input: AssistantLinkInput, existing: List<AssistantImage>,
                onProgress: (String) -> Unit): LoadedAssistantLink {
                reads.incrementAndGet()
                return LoadedAssistantLink("北城美术馆", listOf(imageStore.importBytes(sessionId, png, existing)))
            }
            override suspend fun discard(sessionId: String, images: List<AssistantImage>) {}
        }
        val materials = GuideMaterialStore(File(root, "materials"), imageStore, loader) { db.guideCreationDao().referencedMaterials() }
        val results = GuideImportResultStore(File(root, "results"))
        lateinit var assistant: PlaceAssistantViewModel
        var visibleModel by mutableStateOf<PlaceAssistantViewModel?>(null)
        try {
            val command = runBlocking {
                val material = materials.prepare(UUID.randomUUID().toString(), "北城 https://xhslink.cn/o/fixture", emptyList()) {}
                CreateGuideTripCommand.create("北城之旅", null, 1, TravelMode.FLEXIBLE, AssistantCityScope.parse("北城"), material.ref)
                    .also { creation.commit(it) }
            }
            fun createModel(folder: String): PlaceAssistantViewModel {
              val store = AndroidAssistantImageStore(context, File(root, folder))
              return PlaceAssistantViewModel(command.tripId, config, RoomPlaceImport(db), repo,
                images = store,
                guideParserFactory = if (!batched) null else { snapshot ->
                    BatchedGuideMaterialParser(snapshot, GuideBatchFiles(File(root, "batches"), store, preparation),
                        GuideBatchBody(), AssistantTransport(batchHttp), scheduler)
                },
                materialTextParserOverride = PlaceIntentParser { _, _ ->
                    texts.incrementAndGet(); listOf(PlaceIntent("美术馆", "北城", "北城美术馆"))
                },
                imageParserOverride = ImageIntentParser { image, ordinal, _, _, _ ->
                    if (pictures.incrementAndGet() == 1) throw AssistantFailure("图片格式不完整", AssistantFailureCode.PROTOCOL)
                    listOf("美术馆", "公园").map { PlaceIntent(it, "北城", it,
                        sources = listOf(IntakeSource.Image(image.id, ordinal, it))) }
                },
                normalizerOverride = PlaceIntentNormalizer { values, _, _ ->
                    if (normalizations.incrementAndGet() == 1 && cancelNormalization) awaitCancellation()
                    mergeIdenticalPlaceIntents(values)
                },
                guideStore = creation, guideMaterials = materials, guideResults = results).also { model ->
                model.controller.updateSource(object : PlaceSearchDataSource {
                    override suspend fun search(keyword: String, city: String?): List<PlaceCandidate> {
                        searches.incrementAndGet()
                        val name = keyword.removePrefix("北城")
                        return listOf(PlaceCandidate(name, name, "地址", GeoPoint(30.2, 120.1), null, "北城"))
                    }
                })
                model.controller.environment(true, true, true)
              }
            }
            compose.runOnUiThread { assistant = createModel("workspace"); stores.put("assistant", assistant) }
            compose.waitUntil(10_000) { assistant.guide.value != null }
            compose.runOnUiThread { assistant.consumePendingGuide() }
            if (cancelNormalization) {
                compose.waitUntil(10_000) { normalizations.get() == 1 }
                compose.runOnUiThread { assistant.cancelCurrentBatch() }
                compose.waitUntil(10_000) { !assistant.controller.state.value.busy }
                assertTrue(assistant.controller.state.value.items.isEmpty())
                assertTrue(assistant.controller.state.value.normalizationPending)
                compose.waitUntil(10_000) {
                    runBlocking { creation.find(command.tripId)?.resultRef != null }
                }
                compose.runOnUiThread { stores.put("assistant", object : ViewModel() {}) }
                runBlocking { delay(250) }
                compose.runOnUiThread { assistant = createModel("workspace-after-cancel"); stores.put("assistant", assistant) }
                compose.waitUntil(10_000) { assistant.guide.value != null }
                compose.runOnUiThread { assistant.consumePendingGuide() }
                compose.waitUntil(10_000) { assistant.controller.state.value.normalizationPending }
                assertTrue(assistant.controller.state.value.parsedMaterials.any { it.failure == null && it.intents.isNotEmpty() })
                assertEquals(1, batches.get())
                compose.runOnUiThread { assistant.controller.retryNormalization() }
            }
            compose.waitUntil(15_000) { assistant.guide.value?.status == GuideHandoffStatus.IMPORTED && !assistant.controller.state.value.busy }
            assertEquals(1, assistant.controller.state.value.eligible.size)
            assertEquals(1, searches.get())
            compose.runOnUiThread { visibleModel = assistant }
            compose.setContent {
                val model = visibleModel ?: return@setContent
                val state by model.controller.state.collectAsState()
                EasyTripTheme {
                    Box(Modifier.sizeIn(maxWidth = 320.dp, maxHeight = 520.dp).fillMaxSize()) {
                        PlaceAssistantPanel(AssistantWorkspace(model, state, true, null, "北城之旅", {}, {}, {}, {}))
                    }
                }
            }
            compose.onNodeWithTag("assistant-select-A").performScrollTo().assertIsDisplayed().performClick()
            compose.onNodeWithTag("assistant-save").assertIsDisplayed().assertIsEnabled()
            val screenshot = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            File(context.filesDir, "partial-guide-ready.png").outputStream().use {
                screenshot.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            screenshot.recycle()
            compose.onNodeWithTag("assistant-save").performClick()
            compose.waitUntil(10_000) { !assistant.controller.state.value.saving && !assistant.controller.state.value.hasDraft &&
                assistant.guide.value == null }
            assertEquals(GuideHandoffStatus.FINISHED, runBlocking { creation.find(command.tripId) }!!.status)
            assertEquals(1, runBlocking { repo.observePlaces(command.tripId, emptySet()).first().size })
            assertNotNull(runBlocking { RoomPlaceImport(db).guideReceipt(command.operationId) })
            assertNull(runBlocking { creation.find(command.tripId) }!!.resultRef)
            compose.onNodeWithTag("assistant-save").assertDoesNotExist()
            compose.onNodeWithTag("assistant-retry-images").assertDoesNotExist()
            compose.runOnUiThread { stores.put("assistant", object : ViewModel() {}) }
            runBlocking { delay(250) }
            compose.runOnUiThread { assistant = createModel("workspace-restored"); stores.put("assistant", assistant) }
            compose.waitUntil(10_000) { !assistant.controller.state.value.attachmentSelectionBusy }
            compose.runOnUiThread { assistant.consumePendingGuide() }
            compose.waitForIdle()
            assertEquals(1, texts.get()); assertEquals(1, pictures.get()); assertEquals(1, searches.get())
            assertFalse(assistant.controller.state.value.hasDraft)
            assertNull(assistant.guide.value)
            compose.runOnUiThread { visibleModel = assistant }
            compose.onNodeWithTag("assistant-save").assertDoesNotExist()
            assertEquals(1, reads.get())
            assertEquals(1, runBlocking { repo.observePlaces(command.tripId, emptySet()).first().size })
            if (batched) {
                assertEquals(1, batches.get())
                assertTrue(File(root, "batches").listFiles()!!.isEmpty())
            }
        } finally {
            compose.runOnUiThread { stores.clear() }
            runBlocking { delay(250) }
            db.close()
            root.deleteRecursively()
        }
    }
}
