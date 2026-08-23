package com.yangchengwei.easytrip

import android.content.Context
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import com.yangchengwei.easytrip.amap.AmapPrivacyGate
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.itinerary.data.RoomItineraryRepository
import com.yangchengwei.easytrip.itinerary.ui.DayItinerarySheet
import com.yangchengwei.easytrip.itinerary.ui.DayItineraryViewModel
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.amap.PlaceSearchDataSource
import com.yangchengwei.easytrip.place.data.RoomSavedPlaceRepository
import com.yangchengwei.easytrip.place.ui.PlacePoolSheet
import com.yangchengwei.easytrip.place.ui.PlacePoolViewModel
import com.yangchengwei.easytrip.place.ui.PlaceSearchScreen
import com.yangchengwei.easytrip.route.data.RoomRouteLegRepository
import com.yangchengwei.easytrip.trip.data.RoomTripRepository
import com.yangchengwei.easytrip.trip.domain.CreateTrip
import com.yangchengwei.easytrip.workspace.AmapMapHost
import com.yangchengwei.easytrip.workspace.MapLayer
import com.yangchengwei.easytrip.workspace.MapMarkerKind
import com.yangchengwei.easytrip.workspace.MapPoiUi
import com.yangchengwei.easytrip.workspace.ItineraryScope
import com.yangchengwei.easytrip.workspace.MapScope
import com.yangchengwei.easytrip.workspace.MapUiModel
import com.yangchengwei.easytrip.workspace.TripWorkspaceScreen
import com.yangchengwei.easytrip.workspace.TripWorkspaceViewModel
import com.yangchengwei.easytrip.workspace.ViewportReason
import com.yangchengwei.easytrip.workspace.WorkspaceSection
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class V2AcceptanceTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var database: EasyTripDatabase
    private var nextId = 0

    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder(compose.activity, EasyTripDatabase::class.java).build()
    }

    @After fun tearDown() = database.close()

    @Test fun searchCollectionMapAndRestorationFlow() {
        val trips = RoomTripRepository(database.tripDao(), idFactory = { "id-${nextId++}" })
        val places = RoomSavedPlaceRepository(database, idFactory = { "id-${nextId++}" })
        val itineraries = RoomItineraryRepository(database, database.itineraryEditingDao(), database.routeLegDao(), itemIdFactory = { "item-${nextId++}" }, legIdFactory = { "leg-${nextId++}" })
        val routes = RoomRouteLegRepository(database.routeLegDao())
        val tripId = runBlocking { trips.createTrip(CreateTrip("集成验收", 2)) }
        val museum = candidate("museum", "博物馆", 39.91, 116.41)
        val park = candidate("park", "公园", 39.92, 116.42)
        val source = object : PlaceSearchDataSource {
            override suspend fun search(keyword: String, city: String?) = listOf(museum, park)
        }
        val placeModel = PlacePoolViewModel(tripId, places, source)
        val savedState = SavedStateHandle(mapOf("workspace.tab" to "SEARCH"))
        val workspace = TripWorkspaceViewModel(tripId, trips, places, itineraries, routes, savedState)
        val itinerary = DayItineraryViewModel(tripId, trips, itineraries, routes, null, places.observePlaces(tripId, emptySet()), workspace.selectedDayId)
        val gate = AmapPrivacyGate.create(compose.activity).apply { reportPrivacyShown() }
        val token = requireNotNull(gate.reportUserDecision(true))
        lateinit var host: RecordingHost
        var searching by mutableStateOf(false)

        compose.setContent {
            if (searching) {
                val placeState by placeModel.state.collectAsState()
                PlaceSearchScreen(
                    state = placeState,
                    onQueryChange = placeModel::setQuery,
                    onBack = { placeModel.clearSearch(); searching = false },
                    onSelect = {
                        placeModel.clearSearch()
                        workspace.focusSearchResult(it)
                        searching = false
                    },
                    onToggleCollection = placeModel::toggleCollection,
                )
            } else {
                val placeState by placeModel.state.collectAsState()
                val workspaceState by workspace.state.collectAsState()
                TripWorkspaceScreen(
                    viewModel = workspace,
                    consent = token,
                    onBack = {},
                    onSettings = {},
                    onOpenSearch = { searching = true },
                    placeContent = { PlacePoolSheet(placeModel, showSearch = false) },
                    dayItineraryContent = { DayItinerarySheet(itinerary) },
                    isPoiSaved = workspaceState.selectedMapPoi?.poiId in placeState.savedPoiIds,
                    collectionBusyPoiIds = placeState.collectionBusyPoiIds,
                    onTogglePoiCollection = placeModel::toggleCollection,
                    mapHostFactory = { RecordingHost(it).also { created -> host = created } },
                )
            }
        }

        compose.onNodeWithTag("workspace-top-bar").assertIsDisplayed()
        compose.onNodeWithTag("layer-menu").assertHasClickAction()
        compose.onNodeWithTag("section-PLACE_POOL").assertIsSelected()
        compose.onNodeWithTag("section-ITINERARY").assertExists()
        compose.onNodeWithTag("itinerary-scope-rail").assertDoesNotExist()
        compose.onNodeWithContentDescription("搜索地点").assertHasClickAction().performClick()
        compose.onNodeWithTag("workspace-search").performTextInput("博物馆")
        compose.waitUntil(5_000) { placeModel.state.value.search.results.size == 2 }

        compose.onNodeWithTag("save-result-museum").performClick()
        compose.waitUntil(5_000) { "museum" in placeModel.state.value.savedPoiIds }
        compose.onNodeWithTag("save-result-museum").performClick()
        compose.waitUntil(5_000) { "museum" !in placeModel.state.value.savedPoiIds }
        compose.onNodeWithText("确认取消收藏").assertDoesNotExist()

        compose.onNodeWithTag("search-result-park").performClick()
        compose.waitUntil(5_000) { !searching }
        assertEquals("", placeModel.state.value.search.query)
        compose.waitUntil(5_000) { host.lastModel?.viewportRequest?.reason == ViewportReason.SEARCH_FOCUS }
        val focused = host.lastModel?.markers?.single { it.point == park.point }
        assertEquals(MapMarkerKind.UNSAVED_SEARCH, focused?.kind)
        assertTrue(focused?.isFocused == true)
        val focusCalls = host.viewportCalls
        compose.onNodeWithTag("section-PLACE_POOL").performClick()
        compose.waitForIdle()
        assertEquals(focusCalls, host.viewportCalls)

        compose.runOnIdle { host.emit(MapPoiUi("poi-card", "故宫", "北京市东城区", GeoPoint(39.916, 116.397))) }
        compose.onNodeWithText("故宫").assertIsDisplayed()
        assertFalse(runBlocking { places.observeSavedPoiIds(tripId).first() }.contains("poi-card"))
        compose.onNodeWithTag("place-card-collection").performClick()
        compose.waitUntil(5_000) { "poi-card" in placeModel.state.value.savedPoiIds }

        compose.onNodeWithTag("section-ITINERARY").performClick()
        compose.onNodeWithTag("itinerary-scope-WHOLE_TRIP").performClick()
        compose.waitUntil(5_000) { workspace.state.value.itineraryScope == ItineraryScope.WholeTrip }
        compose.waitUntil(5_000) { workspace.state.value.mapScope == MapScope.WHOLE_TRIP }
        val restored = TripWorkspaceViewModel(tripId, trips, places, itineraries, routes, savedState)
        val legacy = TripWorkspaceViewModel(
            tripId,
            trips,
            places,
            itineraries,
            routes,
            SavedStateHandle(mapOf("workspace.tab" to "SEARCH")),
        )
        compose.waitUntil(5_000) { restored.state.value.tripName.isNotEmpty() && legacy.state.value.tripName.isNotEmpty() }
        assertEquals(WorkspaceSection.ITINERARY, restored.state.value.section)
        assertEquals(ItineraryScope.WholeTrip, restored.state.value.itineraryScope)
        assertEquals(MapScope.WHOLE_TRIP, restored.state.value.mapScope)
        assertEquals(WorkspaceSection.PLACE_POOL, legacy.state.value.section)
    }

    private fun candidate(id: String, name: String, latitude: Double, longitude: Double) =
        PlaceCandidate(id, name, "地址", GeoPoint(latitude, longitude), "010")

    private class RecordingHost(context: Context) : AmapMapHost {
        override val view = View(context)
        var lastModel: MapUiModel? = null
        var viewportCalls = 0
        private var viewportId: Long? = null
        private var poiCallback: (MapPoiUi) -> Unit = {}
        override fun onCreate() = Unit
        override fun onResume() = Unit
        override fun onPause() = Unit
        override fun onDestroy() = Unit
        override fun render(
            model: MapUiModel,
            layer: MapLayer,
            onMarkerClick: (String) -> Unit,
            onMapPoiClick: (MapPoiUi) -> Unit,
            onLayerError: (Throwable, MapLayer) -> Unit,
        ) {
            lastModel = model
            poiCallback = onMapPoiClick
            model.viewportRequest?.takeIf { it.id != viewportId }?.let {
                viewportId = it.id
                viewportCalls++
            }
        }
        fun emit(poi: MapPoiUi) = poiCallback(poi)
    }
}
