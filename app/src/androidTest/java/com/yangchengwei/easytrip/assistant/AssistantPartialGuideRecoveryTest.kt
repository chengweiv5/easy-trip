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
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Real Room + handoff + ViewModel + UI; models, platform loading and POI service are fixtures. */
class AssistantPartialGuideRecoveryTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun partialCollectionReopensAndRetriesWithoutFinishingGuideOrDuplicatingSavedPlaces() {
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
            fun createModel(folder: String) = PlaceAssistantViewModel(command.tripId, config, RoomPlaceImport(db), repo,
                images = AndroidAssistantImageStore(context, File(root, folder)),
                materialTextParserOverride = PlaceIntentParser { _, _ ->
                    texts.incrementAndGet(); listOf(PlaceIntent("美术馆", "北城", "北城美术馆"))
                },
                imageParserOverride = ImageIntentParser { image, ordinal, _, _, _ ->
                    if (pictures.incrementAndGet() == 1) throw AssistantFailure("图片格式不完整", AssistantFailureCode.PROTOCOL)
                    listOf("美术馆", "公园").map { PlaceIntent(it, "北城", it,
                        sources = listOf(IntakeSource.Image(image.id, ordinal, it))) }
                },
                normalizerOverride = PlaceIntentNormalizer { values, _, _ -> mergeIdenticalPlaceIntents(values) },
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
            compose.runOnUiThread { assistant = createModel("workspace"); stores.put("assistant", assistant) }
            compose.waitUntil(10_000) { assistant.guide.value != null }
            compose.runOnUiThread { assistant.consumePendingGuide() }
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
            compose.waitUntil(10_000) { !assistant.controller.state.value.saving && assistant.controller.state.value.items.single().status == IntakeStatus.ALREADY_SAVED }
            assertEquals(GuideHandoffStatus.IMPORTED, runBlocking { creation.find(command.tripId) }!!.status)
            assertEquals(1, runBlocking { repo.observePlaces(command.tripId, emptySet()).first().size })
            assertNull(runBlocking { RoomPlaceImport(db).guideReceipt(command.operationId) })
            compose.waitUntil(10_000) {
                runBlocking {
                    val handoff = creation.find(command.tripId)!!
                    handoff.resultRef?.let { results.read(it).items.firstOrNull()?.status == IntakeStatus.ALREADY_SAVED } == true
                }
            }
            compose.runOnUiThread { stores.put("assistant", object : ViewModel() {}) }
            runBlocking { delay(250) }
            compose.runOnUiThread { assistant = createModel("workspace-restored"); stores.put("assistant", assistant) }
            compose.waitUntil(10_000) { assistant.guide.value != null }
            compose.runOnUiThread { assistant.consumePendingGuide() }
            compose.waitUntil(10_000) { assistant.controller.state.value.items.size == 1 }
            assertEquals(1, texts.get()); assertEquals(1, pictures.get()); assertEquals(1, searches.get())
            assertEquals(1, assistant.controller.state.value.unfinishedSources.size)
            compose.runOnUiThread { visibleModel = assistant }
            compose.onNodeWithTag("assistant-retry-images").performScrollTo().assertIsEnabled().performClick()
            compose.waitUntil(15_000) { !assistant.controller.state.value.busy && assistant.controller.state.value.items.size == 2 }
            assertEquals(1, texts.get()); assertEquals(2, pictures.get()); assertEquals(2, searches.get()); assertEquals(1, reads.get())
            assertEquals(1, assistant.controller.state.value.eligible.size)
            compose.onNodeWithTag("assistant-select-B").performScrollTo().assertIsDisplayed().performClick()
            compose.onNodeWithTag("assistant-save").assertIsDisplayed().assertIsEnabled().performClick()
            compose.waitUntil(10_000) { assistant.guide.value == null && !assistant.controller.state.value.hasDraft }
            assertEquals(2, runBlocking { repo.observePlaces(command.tripId, emptySet()).first().size })
            assertEquals(GuideHandoffStatus.FINISHED, runBlocking { creation.find(command.tripId) }!!.status)
            compose.runOnUiThread { assistant.consumePendingGuide() }
            compose.waitForIdle()
            assertEquals(2, pictures.get())
        } finally {
            compose.runOnUiThread { stores.clear() }
            runBlocking { delay(250) }
            db.close()
            root.deleteRecursively()
        }
    }
}
