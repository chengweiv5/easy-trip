package com.yangchengwei.easytrip.assistant

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import com.yangchengwei.easytrip.assistant.cityfirst.*
import com.yangchengwei.easytrip.assistant.data.*
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import com.yangchengwei.easytrip.place.amap.*
import com.yangchengwei.easytrip.place.data.RoomSavedPlaceRepository
import com.yangchengwei.easytrip.place.domain.PlaceCategory
import com.yangchengwei.easytrip.trip.data.RoomTripRepository
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** New global UI and production handoff/Room/controller, deterministic external model and map seams. */
class GuideCityFirstUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @Test fun cityThenEmptyTripThenSingleImportAndManualCollection() = exercise(false, 1)
    @Test fun hundredPlacesAtNarrowDoubleFontKeepOneCreationAndOneImport() = exercise(true, 100)
    @Test fun zeroPlacesKeepsTripAndRequiresExplicitRetry() = exercise(false, 1, failFirst = true)
    @Test fun importedSelectionReopensWithoutReparsingOrResearching() = exercise(false, 1, restore = true)
    @Test fun optionalDatesCanBeEditedAndClearedBeforeCreation() = exercise(false, 1, editDates = true)
    @Test fun hundredFirstPlaceKeepsEmptyTripAndGuideWithoutSearching() = exercise(false, 101)
    @Test fun readFailureKeepsOriginalGuideAndDoesNotCreateTrip() = exercise(false, 1, readFailure = true)
    private fun exercise(small: Boolean, count: Int, failFirst: Boolean = false, restore: Boolean = false, editDates: Boolean = false, readFailure: Boolean = false) {
        val context = compose.activity
        val db = Room.inMemoryDatabaseBuilder(context, EasyTripDatabase::class.java).build()
        val trips = RoomTripRepository(db.tripDao(), database = db)
        val places = RoomSavedPlaceRepository(db)
        val config = AssistantConfigStore(context)
        val original = config.read()
        val root = File(context.noBackupFilesDir, "city-first-test-${UUID.randomUUID()}").apply { mkdirs() }
        val stores = ViewModelStore()
        val creation = RoomGuideCreation(db)
        val imageStore = AndroidAssistantImageStore(context, File(root, "images"), false)
        val linkLoads = AtomicInteger()
        val links = object : AssistantLinkLoader {
            override suspend fun read(input: AssistantLinkInput, existing: List<AssistantImage>, onProgress: (String) -> Unit): LoadedAssistantLink {
                linkLoads.incrementAndGet()
                if (readFailure) throw AssistantFailure("笔记图片尚未读取完整，请重试或改用文字／图片，原输入已保留。")
                return LoadedAssistantLink("青州攻略：博物馆", emptyList())
            }
            override suspend fun prepare(sessionId: String, input: AssistantLinkInput, existing: List<AssistantImage>,
                loaded: LoadedAssistantLink, retryIds: Set<String>, onProgress: (String) -> Unit) = loaded
            override suspend fun discard(sessionId: String, images: List<AssistantImage>) = Unit
        }
        val materials = GuideMaterialStore(File(root, "material"), imageStore, links) { db.guideCreationDao().referencedMaterials() }
        val journal = GuideCreationJournal(File(root, "pending.json"))
        val parsed = AtomicInteger()
        val searches = AtomicInteger()
        lateinit var global: GuideCreationViewModel
        lateinit var assistant: PlaceAssistantViewModel
        var created: String? = null
        try {
            config.save(ProviderConfig("https://example.invalid", "fixture", "not-real"))
            compose.runOnUiThread {
                global = GuideCreationViewModel(materials, config, creation, journal,
                    { GuideDestinationResult(listOf(GuideCityEvidence("青州", "text", "青州攻略")), "青州之旅") },
                    { error("Share title must not trigger user date interpretation") })
                stores.put("global", global)
            }
            compose.setContent {
                EasyTripTheme {
                    val density = LocalDensity.current
                    CompositionLocalProvider(LocalDensity provides Density(density.density, if (small) 2f else density.fontScale)) {
                        Box(if (small) Modifier.requiredSize(320.dp, 720.dp) else Modifier) {
                            GuideCreationRoute(global, {}, { created = it })
                        }
                    }
                }
            }
            compose.waitUntil(10_000) { !global.state.value.recovering }
            screenshot("input-${if (small) "large" else "normal"}")
            compose.onNodeWithTag("assistant-input").performTextInput("青州三日游 https://xhslink.cn/o/example")
            compose.onNodeWithTag("guide-identify-destination").performScrollTo().performClick()
            if (readFailure) {
                compose.waitUntil(10_000) { global.state.value.error != null && !global.state.value.busy }
                assertEquals("青州三日游 https://xhslink.cn/o/example", global.state.value.input)
                assertFalse(global.state.value.confirming)
                assertEquals(1, linkLoads.get())
                assertEquals(0, parsed.get())
                assertEquals(0, searches.get())
                assertTrue(runBlocking { trips.observeTrips().first().isEmpty() })
                val error = compose.onNodeWithText(requireNotNull(global.state.value.error))
                error.performScrollTo().assertIsDisplayed()
                val layout = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
                error.performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(layout) }
                assertTrue(layout.isNotEmpty())
                assertTrue(layout.none { it.hasVisualOverflow })
                screenshot("read-failure")
                compose.onNodeWithTag("assistant-input").performScrollTo().assertIsDisplayed()
                compose.onNodeWithTag("guide-identify-destination").performScrollTo().assertIsEnabled()
                return
            }
            compose.waitUntil(10_000) { global.state.value.confirming && !global.state.value.busy }
            assertEquals(0, parsed.get()); assertEquals(0, searches.get())
            assertTrue(runBlocking { trips.observeTrips().first().isEmpty() })
            assertNull(global.state.value.startDate)
            assertEquals(1, global.state.value.dayCount)
            if (editDates) {
                compose.runOnUiThread { global.editDates(java.time.LocalDate.of(2026, 10, 24), 3) }
                compose.onNodeWithTag("trip-draft-clear-date").performScrollTo().performClick()
                assertNull(global.state.value.startDate)
                assertEquals(3, global.state.value.dayCount)
            }
            compose.onNodeWithTag("trip-draft-submit").performScrollTo().assertIsDisplayed()
            screenshot("confirm-${if (small) "large" else "normal"}")
            compose.onNodeWithTag("trip-draft-submit").performClick()
            compose.waitUntil(10_000) { created != null && !File(root, "pending.json").exists() }
            val trip = runBlocking { trips.observeTrips().first().single() }
            assertEquals(0, trip.placeCount)
            assertEquals(listOf("青州"), runBlocking { creation.scope(trip.id) }!!.names)
            val parser = PlaceIntentParser { _, city ->
                val call = parsed.incrementAndGet()
                assertEquals("青州", city)
                if (failFirst && call == 1) emptyList() else (1..count).map { PlaceIntent("公园$it", city, "公园$it") }
            }
            compose.runOnUiThread {
                assistant = PlaceAssistantViewModel(trip.id, config, RoomPlaceImport(db), places,
                    images = AndroidAssistantImageStore(context, File(root, "workspace")),
                    materialTextParserOverride = parser, normalizerOverride = PlaceIntentNormalizer { values, _, _ -> values },
                    guideStore = creation, guideMaterials = materials, guideResults = GuideImportResultStore(File(root, "results")))
                stores.put("assistant", assistant)
                assistant.controller.updateSource(object : PlaceSearchDataSource {
                    override suspend fun search(keyword: String, city: String?): List<PlaceCandidate> {
                        searches.incrementAndGet()
                        assertEquals("青州", city)
                        return listOf(PlaceCandidate(keyword, keyword.removePrefix("青州"), "青州市测试地址", GeoPoint(36.7, 118.4), null, "青州"))
                    }
                })
                assistant.controller.environment(true, true, true)
            }
            compose.waitUntil(10_000) { assistant.guide.value != null }
            compose.runOnUiThread { repeat(3) { assistant.consumePendingGuide() } }
            if (count > AssistantPlacePolicy.MAX_PLACES) {
                compose.waitUntil(10_000) { assistant.guide.value?.status == GuideHandoffStatus.FAILED }
                assertEquals(0, searches.get())
                assertEquals(0, runBlocking { trips.observeTrips().first().single().placeCount })
                assertTrue(assistant.controller.state.value.input.contains("xhslink.cn"))
                assertTrue(assistant.controller.state.value.error.orEmpty().contains("100"))
                compose.runOnUiThread { assistant.consumePendingGuide() }
                compose.waitForIdle()
                assertEquals(1, parsed.get())
                return
            }
            if (failFirst) {
                compose.waitUntil(10_000) { assistant.guide.value?.status == GuideHandoffStatus.FAILED }
                assertEquals(1, runBlocking { trips.observeTrips().first().size })
                compose.runOnUiThread { assistant.consumePendingGuide() }
                compose.waitForIdle()
                assertEquals(1, parsed.get())
                compose.runOnUiThread { assistant.retryGuide() }
            }
            compose.waitUntil(120_000) {
                assistant.guide.value?.status == GuideHandoffStatus.IMPORTED && !assistant.controller.state.value.busy
            }
            assertEquals(count, assistant.controller.state.value.items.count { it.status == IntakeStatus.READY })
            assertEquals(if (failFirst) 2 else 1, parsed.get())
            assertEquals(count, searches.get())
            assertEquals(1, linkLoads.get())
            assertEquals(0, runBlocking { trips.observeTrips().first().single().placeCount })
            if (restore) {
                compose.runOnUiThread { stores.put("assistant", object : androidx.lifecycle.ViewModel() {}) }
                runBlocking { delay(250) }
                compose.runOnUiThread {
                    assistant = PlaceAssistantViewModel(trip.id, config, RoomPlaceImport(db), places,
                        images = AndroidAssistantImageStore(context, File(root, "workspace-restored")),
                        materialTextParserOverride = PlaceIntentParser { _, _ -> error("Restoration must not call model") },
                        guideStore = creation, guideMaterials = materials, guideResults = GuideImportResultStore(File(root, "results")))
                    stores.put("assistant", assistant)
                    assistant.controller.environment(true, true, true)
                }
                compose.waitUntil(10_000) { assistant.guide.value != null }
                compose.runOnUiThread { assistant.consumePendingGuide() }
                compose.waitUntil(10_000) { assistant.controller.state.value.items.size == count }
                assertEquals(count, assistant.controller.state.value.items.count { it.status == IntakeStatus.READY })
            }
            compose.runOnUiThread { assistant.controller.selectAll(); assistant.controller.saveSelected() }
            compose.waitUntil(10_000) { assistant.guide.value == null && assistant.controller.state.value.items.isEmpty() }
            assertEquals(count, runBlocking { trips.observeTrips().first().single().placeCount })
            val saved = runBlocking { RoomSavedPlaceRepository(db).observePlaces(trip.id, emptySet()).first() }
            assertEquals(count, saved.size)
            assertTrue(saved.all { it.category == PlaceCategory.ATTRACTION })
            assertEquals(GuideHandoffStatus.FINISHED, runBlocking { creation.find(trip.id) }!!.status)
            compose.runOnUiThread { assistant.consumePendingGuide() }
            compose.waitForIdle()
            assertEquals(if (failFirst) 2 else 1, parsed.get())
        } finally {
            compose.runOnUiThread { stores.clear() }
            runBlocking { delay(250) }
            if (original != null) config.save(original) else config.clear()
            db.close()
            root.deleteRecursively()
        }
    }

    private fun screenshot(name: String) {
        compose.onNodeWithTag("global-assistant").assertIsDisplayed()
        compose.waitForIdle()
        // Wait for the already-asserted Compose frame to reach the platform screenshot buffer.
        Thread.sleep(200)
        val directory = File(compose.activity.getExternalFilesDir(null), "city-first-ui").apply { mkdirs() }
        InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()?.let {
            File(directory, "$name.png").outputStream().use { output -> it.compress(Bitmap.CompressFormat.PNG, 100, output) }
            it.recycle()
        }
    }
}
