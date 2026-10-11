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
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList

/** Deterministic emulator-only tests. No real provider, map requests or production database. */
class AssistantCityUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun xuzhouTitleFlowsThroughWorkspaceToBothSearchQueries() = exercise(knownCity = true, small = false)
    @Test fun missingCityIsSuppliedOnceForWholeBatch() = exercise(knownCity = false, small = false)
    @Test fun batchCityDialogWorksAtNarrowDoubleFont() = exercise(knownCity = false, small = true)
    @Test fun firstCandidateCanBeChangedWithoutSaving() = exercise(knownCity = true, small = false, alternatives = true)
    @Test fun initialCityAndCandidateSwitchWorkAtNarrowDoubleFont() = exercise(knownCity = true, small = true, alternatives = true)

    private fun exercise(knownCity: Boolean, small: Boolean, alternatives: Boolean = false) {
        val context = compose.activity.applicationContext
        val config = AssistantConfigStore(context)
        val original = config.read()
        val db = Room.inMemoryDatabaseBuilder(context, EasyTripDatabase::class.java).build()
        val stores = ViewModelStore()
        val trips = RoomTripRepository(db.tripDao())
        val places = RoomSavedPlaceRepository(db)
        val title = if (knownCity) "徐州" else "周末出游"
        val trip = runBlocking { trips.createTrip(CreateTrip(title, 1)) }
        val queries = CopyOnWriteArrayList<Pair<String, String?>>()
        val parserCities = CopyOnWriteArrayList<String>()
        val resolvedTitles = CopyOnWriteArrayList<String>()
        val source = object : PlaceSearchDataSource {
            override suspend fun search(keyword: String, city: String?): List<PlaceCandidate> {
                queries += keyword to city
                val first = PlaceCandidate("$keyword-first", if (alternatives) "${keyword}风景区" else keyword, "徐州市测试地址",
                    GeoPoint(34.23, 117.17), null, "徐州市")
                return if (alternatives) listOf(first, first.copy(poiId = "$keyword-second", name = keyword, address = "徐州市第二处地址"))
                    else listOf(first)
            }
        }
        val parser = PlaceIntentParser { input, city ->
            assertEquals("云龙湖、云龙山", input)
            parserCities += city
            listOf("云龙湖", "云龙山").map { PlaceIntent(it, "", it) }
        }
        try {
            config.save(ProviderConfig("https://example.invalid", "test", "not-a-real-key"))
            val token = TestConsentGate().run { show(); requireNotNull(decide(true)) }
            val assistant = PlaceAssistantViewModel(trip, config, RoomPlaceImport(db), places, parser)
            assistant.controller.updateSource(source)
            val workspace = TripWorkspaceViewModel(trip, trips, places,
                RoomItineraryRepository(db, db.itineraryEditingDao(), db.routeLegDao()),
                RoomRouteLegRepository(db.routeLegDao()), SavedStateHandle())
            val pool = PlacePoolViewModel(trip, places, source)
            stores.put("assistant", assistant); stores.put("workspace", workspace); stores.put("pool", pool)
            val permission = LocationPermissionCoordinator(InMemoryLocationPermissionRequestStore())
            compose.setContent {
                EasyTripTheme {
                    val density = LocalDensity.current.density
                    CompositionLocalProvider(LocalDensity provides Density(density, if (small) 2f else 1f)) {
                        Box(if (small) Modifier.requiredSize(320.dp, 568.dp) else Modifier) {
                            TripWorkspaceRoute(workspace, token, onBack = {}, onSettings = {},
                                assistantViewModel = assistant, placeViewModel = pool,
                                resolveAssistantTripCity = {
                                    resolvedTitles += it
                                    if (knownCity) "徐州市" else null
                                },
                                locationPermissionCoordinator = permission,
                                locationPermissionSnapshot = { LocationPermissionSnapshot(false, false) },
                                onWorkspaceEffect = {},
                                mapHostFactory = { ctx -> object : AmapMapHost {
                                    override val view = View(ctx)
                                    override fun canRenderBeforeReady() = true
                                    override fun onCreate() = Unit
                                    override fun onResume() = Unit
                                    override fun onPause() = Unit
                                    override fun onDestroy() = Unit
                                } })
                        }
                    }
                }
            }
            compose.waitUntil(10_000) { assistant.controller.state.value.configured }
            compose.onNodeWithTag("assistant-entry").performClick()
            compose.runOnIdle { workspace.setSheetLevel(WorkspaceSheetLevel.EXPANDED) }
            compose.waitUntil(10_000) { resolvedTitles.isNotEmpty() && !assistant.controller.state.value.resolvingCity }
            compose.onNodeWithText("当前旅行：$title").assertDoesNotExist()
            compose.onNodeWithText(title).assertExists()
            compose.onNodeWithTag("assistant-city").performScrollTo()
            assertEquals(if (knownCity) "徐州市" else "", assistant.controller.state.value.city)
            if (knownCity) compose.onNodeWithTag("assistant-city").assertTextContains("徐州市")
            assertTrue("Opening must not send to the provider", parserCities.isEmpty())
            assertTrue("Opening must not search POIs", queries.isEmpty())
            if (knownCity) {
                assertEquals("徐州市", assistant.controller.state.value.city)
                val openingFolder = File(context.filesDir, "v3.0.2-city-evidence").apply { mkdirs() }
                InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot().let { bitmap ->
                    File(openingFolder, if (small) "initial-city-small.png" else "initial-city.png").outputStream()
                        .use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                }
                compose.onNodeWithTag("assistant-city").performTextReplacement("南京")
                androidx.test.espresso.Espresso.closeSoftKeyboard()
                compose.onNodeWithTag("assistant-collapse").performClick()
                compose.runOnIdle { workspace.setSheetLevel(WorkspaceSheetLevel.HALF) }
                compose.onNodeWithTag("assistant-entry").performClick()
                compose.runOnIdle { workspace.setSheetLevel(WorkspaceSheetLevel.EXPANDED) }
                compose.onNodeWithTag("assistant-city").performScrollTo().assertTextContains("南京")
                compose.onNodeWithTag("assistant-city").performTextReplacement("徐州市")
            }
            compose.onNodeWithTag("assistant-input").performScrollTo().performTextInput("云龙湖、云龙山")
            androidx.test.espresso.Espresso.closeSoftKeyboard()
            compose.onNodeWithTag("assistant-submit").performScrollTo().performClick()
            compose.onNodeWithText("同意并发送本次内容").assertDoesNotExist()
            compose.waitUntil(10_000) { assistant.controller.state.value.items.size == 2 && !assistant.controller.state.value.busy }
            assertTrue(resolvedTitles.all { it == title })
            if (!knownCity) {
                assertTrue(queries.isEmpty())
                compose.onNodeWithTag("assistant-supply-city").performScrollTo().performClick()
                compose.onNodeWithTag("assistant-batch-city").performTextInput("徐州")
                compose.onNodeWithTag("assistant-batch-city-confirm").assertIsDisplayed().performClick()
                compose.waitUntil(10_000) { assistant.controller.state.value.eligible.size == 2 }
            }
            assertEquals(listOf(if (knownCity) "徐州市" else ""), parserCities.toList())
            assertEquals(listOf("云龙湖", "云龙山"), queries.map { it.first })
            assertTrue(queries.all { sameCity(it.second, "徐州") })
            assertEquals(2, assistant.controller.state.value.eligible.size)
            assertTrue(assistant.controller.state.value.selected.isEmpty())
            if (alternatives) {
                val lake = assistant.controller.state.value.items.first()
                assertEquals("云龙湖-first", lake.poi?.poiId)
                compose.onNodeWithText("云龙湖风景区").performScrollTo().performClick()
                if (small) {
                    compose.onNodeWithTag("assistant-change-A").performScrollTo().performClick()
                } else {
                    compose.runOnIdle { workspace.setSheetLevel(WorkspaceSheetLevel.HALF) }
                    compose.onNodeWithTag("assistant-map-card-name").assertIsDisplayed().assertTextContains("云龙湖风景区")
                    compose.onNodeWithTag("assistant-map-category").performScrollTo().performClick()
                    compose.onNodeWithTag("assistant-category-A-food").performScrollTo().performClick()
                    assertEquals(com.yangchengwei.easytrip.place.domain.PlaceCategory.FOOD,
                        assistant.controller.state.value.items.first().category)
                    compose.onNodeWithTag("assistant-map-change").performScrollTo().performClick()
                    compose.onNodeWithTag("assistant-option-A-0").assertExists()
                    compose.runOnIdle { workspace.setSheetLevel(WorkspaceSheetLevel.EXPANDED) }
                }
                compose.onNodeWithTag("assistant-option-A-0").performScrollTo().assertTextContains("已采用", substring = true)
                compose.onNodeWithTag("assistant-option-A-1").performScrollTo().performClick()
                compose.waitUntil(5_000) { assistant.controller.state.value.items.first().poi?.poiId == "云龙湖-second" }
                compose.onNodeWithTag("assistant-option-A-1").assertTextContains("已采用", substring = true)
                assertTrue(assistant.controller.state.value.selected.isEmpty())
            }
            compose.onAllNodesWithText("请补充搜索城市。").assertCountEquals(0)
            assertTrue(runBlocking { places.observePlaces(trip, emptySet()).first() }.isEmpty())
            compose.onNodeWithTag("assistant-item-A").performScrollTo().assertIsDisplayed()
            val folder = File(context.filesDir, "v3.0.2-city-evidence").apply { mkdirs() }
            val name = if (alternatives) if (small) "candidates-small" else "candidates" else if (knownCity) "xuzhou" else if (small) "batch-small" else "batch"
            InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot().let { bitmap ->
                File(folder, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
        } finally {
            compose.runOnIdle { compose.activity.setContentView(View(compose.activity)); stores.clear() }
            db.close()
            if (original != null) config.save(original) else config.clear()
        }
    }
}
