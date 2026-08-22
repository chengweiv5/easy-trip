package com.yangchengwei.easytrip

import android.content.Context
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
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
import com.yangchengwei.easytrip.place.domain.SavePlaceResult
import com.yangchengwei.easytrip.place.ui.PlacePoolViewModel
import com.yangchengwei.easytrip.place.ui.PlaceSearchResults
import com.yangchengwei.easytrip.place.ui.SavedPlacesContent
import com.yangchengwei.easytrip.route.data.RoomRouteLegRepository
import com.yangchengwei.easytrip.trip.data.RoomTripRepository
import com.yangchengwei.easytrip.trip.domain.CreateTrip
import com.yangchengwei.easytrip.workspace.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
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

    @Test fun searchFocusSaveLayersScopesAndRepeatedItineraryFlow() {
        val trips = RoomTripRepository(database.tripDao(), idFactory = { "id-${nextId++}" })
        val places = RoomSavedPlaceRepository(database, idFactory = { "id-${nextId++}" })
        val itineraries = RoomItineraryRepository(database, database.itineraryEditingDao(), database.routeLegDao(), itemIdFactory = { "item-${nextId++}" }, legIdFactory = { "leg-${nextId++}" })
        val routes = RoomRouteLegRepository(database.routeLegDao())
        val tripId = runBlocking { trips.createTrip(CreateTrip("V2", 2)) }
        val hotel = runBlocking { (places.save(tripId, candidate("hotel", "酒店", 39.90, 116.40)) as SavePlaceResult.Saved).id }
        val museumCandidate = candidate("museum", "博物馆", 39.91, 116.41)
        val source = object : PlaceSearchDataSource { override suspend fun search(keyword: String, city: String?) = listOf(museumCandidate) }
        val placeModel = PlacePoolViewModel(tripId, places, source)
        val savedState = SavedStateHandle()
        val preferences = InMemoryMapPreferences()
        val workspace = TripWorkspaceViewModel(tripId, trips, places, itineraries, routes, savedState, placeModel.state.map { it.search.results }, preferences)
        val itinerary = DayItineraryViewModel(tripId, trips, itineraries, routes, null, places.observePlaces(tripId, emptySet()), workspace.selectedDayId)
        val gate = AmapPrivacyGate.create(compose.activity).apply { reportPrivacyShown() }
        val token = requireNotNull(gate.reportUserDecision(true))
        lateinit var host: RecordingHost

        compose.setContent {
            val placeState = placeModel.state.collectAsState().value
            val workspaceState = workspace.state.collectAsState().value
            TripWorkspaceScreen(
                viewModel = workspace, consent = token, onBack = {}, onSettings = {},
                searchQuery = placeState.search.query,
                onSearchQueryChange = { placeModel.setQuery(it); workspace.onSearchQueryChanged(it) },
                searchContent = { PlaceSearchResults(placeState.search, placeState.savedPoiIds, workspace::focusSearchResult, placeModel::save, selectedPoiId = workspaceState.searchSelection?.poiId) },
                placeContent = { SavedPlacesContent(placeState.search.savedPlaces, placeState.tags, placeState.selectedTagIds, placeModel::toggleTag, placeModel::edit, placeModel::requestDelete) },
                itineraryContent = { DayItinerarySheet(itinerary, onSelectDay = workspace::selectDay) },
                mapHostFactory = { RecordingHost(it).also { created -> host = created } },
            )
        }

        compose.onNodeWithTag("workspace-search").performTextInput("博物馆")
        compose.waitUntil(5_000) { placeModel.state.value.search.results.isNotEmpty() }
        compose.onNodeWithTag("tab-SEARCH").assertIsSelected()
        compose.onNodeWithTag("search-result-museum").performClick()
        compose.waitUntil(5_000) { host.lastModel?.highlightedMarkerKey == "search-museum" }
        assertEquals(ViewportReason.SEARCH_FOCUS, host.lastModel?.viewportRequest?.reason)
        compose.onNodeWithTag("save-result-museum").performClick()
        compose.waitUntil(5_000) { placeModel.state.value.savedPoiIds.contains("museum") }
        compose.onNodeWithTag("tab-PLACES").performClick()
        compose.onNodeWithTag("saved-place-${runBlocking { places.observePlaces(tripId, emptySet()).first().first { it.amapPoiId == "museum" }.id }}").assertIsDisplayed()
        assertEquals(1, host.lastModel?.markers?.count { it.point == museumCandidate.point })

        compose.onNodeWithTag("layer-menu").performClick()
        compose.onNodeWithTag("layer-SATELLITE_ROAD").performClick()
        compose.waitUntil(5_000) { host.lastLayer == MapLayer.SATELLITE_ROAD }
        MapScope.entries.forEach { scope -> compose.onNodeWithTag("scope-${scope.name}").performClick() }
        compose.waitUntil(5_000) { workspace.state.value.mapScope == MapScope.WHOLE_TRIP }

        compose.onNodeWithTag("tab-ITINERARY").performClick()
        compose.waitUntil(5_000) { itinerary.state.value.savedPlaces.size == 2 }
        compose.onNodeWithTag("add-place-$hotel").performClick()
        compose.waitUntil(5_000) { itinerary.state.value.items.size == 1 }
        compose.onNodeWithTag("add-place-$hotel").performClick()
        compose.waitUntil(5_000) { itinerary.state.value.items.size == 2 }
        assertEquals(2, itinerary.state.value.items.map { it.name }.count { it == "酒店" })
        assertTrue(itinerary.state.value.legs.size == 1)

        workspace.setSheetLevel(WorkspaceSheetLevel.EXPANDED)
        val restored = TripWorkspaceViewModel(tripId, trips, places, itineraries, routes, savedState, mapPreferences = preferences)
        compose.waitUntil(5_000) { restored.state.value.tripName.isNotEmpty() }
        assertEquals(WorkspaceTab.ITINERARY, restored.state.value.tab)
        assertEquals(MapScope.WHOLE_TRIP, restored.state.value.mapScope)
        assertEquals(WorkspaceSheetLevel.EXPANDED, restored.state.value.sheetLevel)
        assertEquals(MapLayer.SATELLITE_ROAD, restored.state.value.mapLayer)
    }

    private fun candidate(id: String, name: String, latitude: Double, longitude: Double) = PlaceCandidate(id, name, "地址", GeoPoint(latitude, longitude), "010")

    private class RecordingHost(context: Context) : AmapMapHost {
        override val view = View(context)
        var lastModel: MapUiModel? = null
        var lastLayer: MapLayer? = null
        override fun onCreate() = Unit
        override fun onResume() = Unit
        override fun onPause() = Unit
        override fun onDestroy() = Unit
        override fun render(model: MapUiModel, layer: MapLayer, onMarkerClick: (String) -> Unit, onLayerError: (Throwable, MapLayer) -> Unit) { lastModel = model; lastLayer = layer }
    }
}
