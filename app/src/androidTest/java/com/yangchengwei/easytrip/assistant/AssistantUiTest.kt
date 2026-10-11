package com.yangchengwei.easytrip.assistant

import android.graphics.Bitmap
import android.view.View
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
import androidx.compose.ui.unit.height
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.amap.api.maps.MapView
import com.yangchengwei.easytrip.amap.*
import com.yangchengwei.easytrip.assistant.data.RoomPlaceImport
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import com.yangchengwei.easytrip.itinerary.data.RoomItineraryRepository
import com.yangchengwei.easytrip.permission.*
import com.yangchengwei.easytrip.place.amap.*
import com.yangchengwei.easytrip.place.data.RoomSavedPlaceRepository
import com.yangchengwei.easytrip.place.ui.PlacePoolViewModel
import com.yangchengwei.easytrip.route.data.RoomRouteLegRepository
import com.yangchengwei.easytrip.trip.data.RoomTripRepository
import com.yangchengwei.easytrip.trip.domain.CreateTrip
import com.yangchengwei.easytrip.workspace.*
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

/** Dedicated emulator tests; never opens the production trip database. */
class AssistantUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun markingAndSelectionDoNotSaveUntilFinalConfirmation() = exercise(real = false, small = false)
    @Test fun narrowDoubleFontCanReachFinalConfirmation() = exercise(real = false, small = true)
    @Test fun realProviderToRealAmapAndNativeMarkers() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("runV3Real") == "true")
        exercise(real = true, small = false)
    }

    private fun exercise(real: Boolean, small: Boolean) {
        val context = compose.activity.applicationContext
        val folder = File(context.filesDir, "v3-implementation-evidence").apply { mkdirs() }
        val config = AssistantConfigStore(context)
        val original = config.read()
        val secret = File(context.filesDir, "v3-provider-test.json")
        val modelCalls = AtomicInteger()
        val poiCalls = AtomicInteger()
        val started = System.currentTimeMillis()
        val prefix = if (real) "real" else if (small) "small" else "ui"
        val db = Room.inMemoryDatabaseBuilder(context, EasyTripDatabase::class.java).build()
        val stores = ViewModelStore()
        val tripRepository = RoomTripRepository(db.tripDao())
        val places = RoomSavedPlaceRepository(db)
        val trip = runBlocking { tripRepository.createTrip(CreateTrip("杭州 · 助手验证", 1)) }
        val hosts = CopyOnWriteArrayList<RealAmapMapHost>()
        val models = CopyOnWriteArrayList<MapUiModel>()
        fun saved() = runBlocking { places.observePlaces(trip, emptySet()).first() }
        fun image(suffix: String) {
            android.os.SystemClock.sleep(2500)
            val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            File(folder, "$prefix-$suffix.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        fun assertAssistantGutters(tag: String = "assistant-panel") {
            val sheet = compose.onNodeWithTag("workspace-sheet").getUnclippedBoundsInRoot()
            val content = compose.onNodeWithTag(tag).getUnclippedBoundsInRoot()
            assertEquals("$tag left gutter", 16f, (content.left - sheet.left).value, .5f)
            assertEquals("$tag right gutter", 16f, (sheet.right - content.right).value, .5f)
        }
        try {
            val provider = if (real) JSONObject(secret.readText()).let {
                ProviderConfig(it.getString("base_url"), it.getString("model"), it.getString("api_key"))
            } else ProviderConfig("https://example.invalid", "test-model", "test-only-not-a-real-key")
            config.save(provider)
            val parser: PlaceIntentParser = if (real) DeepSeekPlaceParser(config::read) else PlaceIntentParser { _, _ ->
                listOf("灵隐寺", "河坊街", "雷峰塔", "失败项").map { PlaceIntent(it, "杭州", it) }
            }
            val countedParser = PlaceIntentParser { input, city ->
                check(modelCalls.incrementAndGet() == 1) { "Test model-call budget exceeded" }
                parser.parse(input, city)
            }
            runBlocking { AmapPrivacyGate.create(context).apply { reportShown(); reportDecision(true) } }
            val token = TestConsentGate().run { show(); requireNotNull(decide(true)) }
            val realSource = if (real) AmapPlaceDataSource(context, token) else null
            val source = object : PlaceSearchDataSource {
                override suspend fun search(keyword: String, city: String?): List<PlaceCandidate> {
                    poiCalls.incrementAndGet()
                    if (real) return requireNotNull(realSource).search(keyword, city)
                    if (keyword == "失败项") error("simulated source failure")
                    if (keyword == "雷峰塔") return listOf(
                        PlaceCandidate("tower1", "雷峰塔景区", "南山路", GeoPoint(30.23,120.15), null, "杭州"),
                        PlaceCandidate("tower2", "雷峰塔塔体", "景区内", GeoPoint(30.231,120.151), null, "杭州"))
                    return listOf(PlaceCandidate(keyword, keyword, "杭州市真实地点测试地址", GeoPoint(30.24 + poiCalls.get() * .008,120.14), null, "杭州"))
                }
            }
            val importer = RoomPlaceImport(db)
            val assistant = PlaceAssistantViewModel(trip, config, importer, places, countedParser)
            assistant.controller.updateSource(source)
            val workspace = TripWorkspaceViewModel(trip, tripRepository, places,
                RoomItineraryRepository(db, db.itineraryEditingDao(), db.routeLegDao()),
                RoomRouteLegRepository(db.routeLegDao()), SavedStateHandle())
            val pool = PlacePoolViewModel(trip, places, source)
            stores.put("assistant", assistant); stores.put("workspace", workspace); stores.put("pool", pool)
            val permission = LocationPermissionCoordinator(InMemoryLocationPermissionRequestStore())
            compose.setContent { EasyTripTheme {
                val density = LocalDensity.current.density
                CompositionLocalProvider(LocalDensity provides Density(density, if (small) 2f else 1f)) {
                    Box(if (small) Modifier.requiredSize(320.dp, 568.dp) else Modifier) {
                        TripWorkspaceRoute(workspace, token, onBack = {}, onSettings = {},
                            assistantViewModel = assistant, placeViewModel = pool,
                            locationPermissionCoordinator = permission,
                            locationPermissionSnapshot = { LocationPermissionSnapshot(false, false) }, onWorkspaceEffect = {},
                            mapHostFactory = { ctx ->
                                val host = RealAmapMapHost(ctx).also(hosts::add)
                                object : AmapMapHost by host {
                                    override fun render(model: MapUiModel, layer: MapLayer, onMarkerClick: (String) -> Unit,
                                        onMapPoiClick: (MapPoiUi) -> Unit, onLayerError: (Throwable, MapLayer) -> Unit) {
                                        host.render(model, layer, onMarkerClick, onMapPoiClick, onLayerError)
                                        models += model
                                    }
                                }
                            })
                    }
                }
            } }
            compose.waitUntil(25_000) { assistant.controller.state.value.mapReady }
            val entry = compose.onNodeWithTag("assistant-entry").getUnclippedBoundsInRoot()
            val search = compose.onNodeWithTag("workspace-search-launcher").getUnclippedBoundsInRoot()
            assertTrue("assistant must stay left of search: $entry / $search", entry.right <= search.left)
            compose.onAllNodesWithTag("map-legend").assertCountEquals(0)
            compose.onNodeWithTag("assistant-entry-surface", useUnmergedTree = true)
                .assertWidthIsEqualTo(124.dp).assertHeightIsAtLeast(32.dp)
            compose.onNodeWithTag("assistant-entry").assertHeightIsAtLeast(48.dp)
            val assistantSurface = compose.onNodeWithTag("assistant-entry-surface", useUnmergedTree = true).getUnclippedBoundsInRoot()
            val searchSurface = compose.onNodeWithTag("workspace-search-launcher-surface", useUnmergedTree = true).getUnclippedBoundsInRoot()
            assertEquals(assistantSurface.top, searchSurface.top)
            assertEquals(assistantSurface.height, searchSurface.height)
            if (!small) assertEquals(32.dp, assistantSurface.height)
            val root = compose.onNodeWithTag("workspace-screen-root").getUnclippedBoundsInRoot()
            assertEquals("search stays 12dp from the right edge", 12f, (root.right - search.right).value, 1f)
            image("map-entry")
            listOf("PLACE_POOL", "ITINERARY").forEach { section ->
                compose.onNodeWithTag("section-$section").performClick()
                compose.onNodeWithTag("assistant-entry").performClick()
                assertAssistantGutters()
                compose.onNodeWithTag("assistant-city").performScrollTo()
                assertAssistantGutters("assistant-city")
                val title = compose.onNodeWithText("✦ 助手").getUnclippedBoundsInRoot()
                val sheet = compose.onNodeWithTag("workspace-sheet").getUnclippedBoundsInRoot()
                assertEquals("header keeps a single inset", 16f, (title.left - sheet.left).value, .5f)
                compose.onNodeWithTag("assistant-collapse").performClick()
            }
            compose.onNodeWithTag("assistant-entry").performClick()
            assertAssistantGutters()
            image("input-gutters")
            compose.onNodeWithTag("assistant-city").performScrollTo().performTextInput("杭州")
            val text = if (real) "把杭州的灵隐寺、河坊街、雷峰塔标记出来" else "杭州的灵隐寺、河坊街、雷峰塔、失败项"
            compose.onNodeWithTag("assistant-input").performScrollTo().performTextInput(text)
            androidx.test.espresso.Espresso.closeSoftKeyboard()
            compose.onNodeWithTag("assistant-submit").performScrollTo().performClick()
            compose.onNodeWithText("同意并发送本次内容").performClick()
            compose.waitUntil(100_000) { !assistant.controller.state.value.busy && assistant.controller.state.value.items.isNotEmpty() }
            assertEquals(1, modelCalls.get())
            assertTrue(saved().isEmpty())
            assertAssistantGutters()
            assertAssistantGutters("assistant-item-${assistant.controller.state.value.items.first().label}")
            if (real) {
                // Where the provider returns several real POIs, make an explicit test-user choice.
                assistant.controller.state.value.items.filter { it.status == IntakeStatus.AMBIGUOUS }.forEach { item ->
                    val option = item.candidates.indexOfFirst { it.name.contains(item.intent.query) && sameCity(it.cityName, "杭州") }
                    assertTrue("Expected a matching real POI for ${item.label}", option >= 0)
                    compose.runOnIdle { assistant.focus(item.id) }
                    compose.onNodeWithTag("assistant-option-${item.label}-$option").performScrollTo().performClick()
                }
                assertEquals(3, assistant.controller.state.value.eligible.size)
            } else {
                assertEquals(2, assistant.controller.state.value.eligible.size)
                assertEquals(1, assistant.controller.state.value.items.count { it.status == IntakeStatus.FAILED })
                assertEquals(1, assistant.controller.state.value.items.count { it.status == IntakeStatus.AMBIGUOUS })
            }
            compose.onNodeWithTag("assistant-select-all").performScrollTo().performClick()
            assertTrue(saved().isEmpty())
            val count = assistant.controller.state.value.selected.size
            compose.onNodeWithTag("assistant-review").performScrollTo().performClick()
            assertTrue(saved().isEmpty())
            compose.onNodeWithTag("assistant-confirm").performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(48.dp)
            assertAssistantGutters("assistant-confirm")
            image("confirmation-zero-writes")
            compose.onNodeWithTag("assistant-confirm").performClick()
            compose.waitUntil(15_000) { assistant.controller.state.value.receipt != null }
            assertEquals(count, saved().size)
            assertEquals(count, assistant.controller.state.value.receipt!!.addedCount)
            assertEquals(assistant.controller.state.value.receipt, runBlocking { importer.latestReceipt(trip) })
            compose.onNodeWithTag("assistant-receipt").performScrollTo().assertIsDisplayed()
            assertAssistantGutters()
            image("itinerary-receipt-gutters")
            compose.onNodeWithTag("assistant-collapse").performClick()
            compose.onNodeWithTag("section-PLACE_POOL").performClick()
            compose.onNodeWithTag("assistant-entry").performClick()
            assertAssistantGutters()
            compose.waitUntil(15_000) { models.lastOrNull()?.markers?.count { it.kind == MapMarkerKind.SAVED_PLACE_POOL } == count }
            if (real) {
                compose.waitUntil(15_000) {
                    var nativeCount = 0
                    compose.runOnUiThread { hosts.filter { it.view.isAttachedToWindow }.forEach { host ->
                        nativeCount += (host.view as MapView).map.mapScreenMarkers.orEmpty().count { it.`object`?.toString()?.startsWith("place-") == true }
                    } }
                    nativeCount == count
                }
            }
            compose.onNodeWithTag("assistant-receipt").performScrollTo().assertIsDisplayed()
            assertAssistantGutters()
            image("receipt")
            File(folder, "$prefix-result.json").writeText(JSONObject().put("passed", true).put("modelCalls", modelCalls.get())
                .put("poiCalls", poiCalls.get()).put("saved", count).put("preConfirmationWrites", 0)
                .put("elapsedMs", System.currentTimeMillis() - started).put("realProvider", real).put("smallDoubleFont", small)
                .put("places", JSONArray().also { array -> saved().forEach { place -> array.put(JSONObject()
                    .put("poiId", place.amapPoiId).put("name", place.name).put("address", place.address)
                    .put("city", place.cityName).put("latitude", place.point.latitude).put("longitude", place.point.longitude)
                    .put("category", place.category.name)) } }).toString(2))
        } finally {
            compose.runOnIdle { compose.activity.setContentView(View(compose.activity)); stores.clear() }
            db.close()
            if (original != null) config.save(original) else config.clear()
            if (real) secret.delete()
        }
    }
}
