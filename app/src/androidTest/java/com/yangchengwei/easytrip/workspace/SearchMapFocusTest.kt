package com.yangchengwei.easytrip.workspace

import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.yangchengwei.easytrip.amap.TestConsentGate
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.itinerary.domain.DayItinerary
import com.yangchengwei.easytrip.itinerary.domain.ItineraryRepository
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.domain.PlaceTag
import com.yangchengwei.easytrip.place.domain.SavePlaceResult
import com.yangchengwei.easytrip.place.domain.SavedPlace
import com.yangchengwei.easytrip.place.domain.SavedPlaceRepository
import com.yangchengwei.easytrip.route.domain.RouteLegRepository
import com.yangchengwei.easytrip.route.domain.RouteLegWithEndpoints
import com.yangchengwei.easytrip.route.domain.RoutePlanOutcome
import com.yangchengwei.easytrip.route.domain.RouteResult
import com.yangchengwei.easytrip.trip.domain.CreateTrip
import com.yangchengwei.easytrip.trip.domain.InsertSide
import com.yangchengwei.easytrip.trip.domain.TripDay
import com.yangchengwei.easytrip.trip.domain.TripRepository
import com.yangchengwei.easytrip.trip.domain.TripSummary
import com.yangchengwei.easytrip.trip.domain.TripWithDays
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

class SearchMapFocusTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun independentUnsavedSelectionRetainsCardDataAfterSearchResultsClear() {
        val candidate = PlaceCandidate("poi-unsaved", "故宫", "", GeoPoint(39.9, 116.4), null)
        val savedState = SavedStateHandle()
        val model = TripWorkspaceViewModel("trip", Trips(), Places(), Itineraries(), Legs(), savedState)

        compose.runOnIdle { model.focusSearchResult(candidate) }
        compose.waitUntil(5_000) {
            model.state.value.map.markers.any { it.key == "search-${candidate.poiId}" }
        }
        assertEquals(null, model.state.value.selectedMapPoi)
        compose.runOnIdle { model.selectMarker("search-${candidate.poiId}") }
        compose.waitUntil(5_000) { model.state.value.selectedMapPoi?.poiId == candidate.poiId }

        assertEquals(candidate.name, model.state.value.selectedMapPoi?.name)
        assertEquals(candidate.address, model.state.value.selectedMapPoi?.address)
        assertEquals(candidate.point, model.state.value.selectedMapPoi?.point)
        assertEquals(candidate.name, savedState.get<String>("workspace.focusedName"))
        assertEquals(candidate.address, savedState.get<String>("workspace.focusedAddress"))
    }

    @Test fun savedResultClickFlowsThroughWorkspaceMapperAndMovesCameraOnlyOnce() {
        val candidate = PlaceCandidate("poi-saved", "故宫", "地址", GeoPoint(39.9, 116.4), null)
        val saved = SavedPlace("saved", "trip", candidate.poiId, "已收藏故宫", "地址", requireNotNull(candidate.point), "", emptyList())
        val model = TripWorkspaceViewModel(
            "trip", Trips(), SavedPlaces(saved), Itineraries(), Legs(),
            SavedStateHandle(mapOf("workspace.tab" to WorkspaceTab.PLACES.name)),
            flowOf(listOf(candidate)),
        )
        val unrelated = mutableStateOf(0)
        lateinit var host: RecordingHost
        val gate = TestConsentGate()
        gate.show()
        val token = requireNotNull(gate.decide(true))

        compose.setContent {
            unrelated.value
            val workspaceState by model.state.collectAsState()
            TripWorkspaceScreen(
                viewModel = model,
                consent = token,
                onBack = {},
                onSettings = {},
                placeContent = { Text("地点") },
                dayItineraryContent = { Text("行程") },
                mapHostFactory = { RecordingHost(it).also { created -> host = created } },
            )
        }

        compose.waitUntil(5_000) { model.state.value.section == WorkspaceSection.PLACE_POOL }
        compose.waitUntil(5_000) { host.lastModel != null }
        val viewportCallsBeforeFocus = host.viewportCalls
        compose.runOnIdle { model.focusSearchResult(candidate) }
        compose.waitUntil(5_000) { host.lastModel?.viewportRequest?.reason == ViewportReason.SEARCH_FOCUS }

        compose.onNodeWithTag("section-PLACE_POOL").assertIsSelected()
        assertEquals(WorkspaceSection.PLACE_POOL, model.state.value.section)
        assertEquals(MapLayer.STANDARD, host.lastLayer)
        val focusedMarker = host.lastModel?.markers?.singleOrNull { it.point == candidate.point }
        assertNotNull(focusedMarker)
        assertEquals("place-saved", focusedMarker?.key)
        assertEquals(MapMarkerKind.SAVED_PLACE_POOL, focusedMarker?.kind)
        assertEquals(true, focusedMarker?.isFocused)
        assertEquals(1, host.lastModel?.markers?.count { it.point == candidate.point })
        assertEquals(listOf(candidate.point), host.lastModel?.viewportRequest?.points)
        assertEquals(viewportCallsBeforeFocus + 1, host.viewportCalls)

        compose.runOnIdle { unrelated.value++ }
        compose.waitForIdle()
        assertEquals(viewportCallsBeforeFocus + 1, host.viewportCalls)
    }

    @Test fun searchFocusPersistsWhenBaseScopeHasNoVisiblePoints() {
        val candidate = PlaceCandidate("poi-empty", "故宫", "地址", GeoPoint(39.9, 116.4), null)
        val model = TripWorkspaceViewModel(
            "trip", Trips(), Places(), Itineraries(), Legs(),
            SavedStateHandle(mapOf("workspace.tab" to WorkspaceTab.PLACES.name)),
            flowOf(listOf(candidate)),
        )
        lateinit var host: RecordingHost
        val gate = TestConsentGate()
        gate.show()
        val token = requireNotNull(gate.decide(true))

        compose.setContent {
            val workspaceState by model.state.collectAsState()
            TripWorkspaceScreen(
                viewModel = model,
                consent = token,
                onBack = {},
                onSettings = {},
                placeContent = { Text("地点") },
                dayItineraryContent = { Text("行程") },
                mapHostFactory = { RecordingHost(it).also { created -> host = created } },
            )
        }
        compose.runOnIdle { model.focusSearchResult(candidate) }
        compose.waitUntil(5_000) { model.state.value.map.viewportRequest?.reason == ViewportReason.SEARCH_FOCUS }
        compose.waitForIdle()

        assertEquals(listOf(candidate.point), model.state.value.map.viewportRequest?.points)
        assertEquals(ViewportReason.SEARCH_FOCUS, host.lastModel?.viewportRequest?.reason)
        assertEquals(1, host.viewportCalls)
    }

    @Test fun repeatedSelectionSheetChangesAndListRenderingKeepViewportRequestId() {
        val point = GeoPoint(39.9, 116.4)
        val saved = SavedPlace("saved", "trip", "poi", "故宫", "地址", point, "", emptyList())
        val model = TripWorkspaceViewModel("trip", Trips(), SavedPlaces(saved), Itineraries(), Legs(), SavedStateHandle())
        val listRender = mutableStateOf(0)
        compose.setContent {
            listRender.value
            TripWorkspaceScreen(
                viewModel = model,
                consent = null,
                onBack = {},
                onSettings = {},
                placeContent = { Text("地点") },
                dayItineraryContent = { Text("行程") },
            )
        }
        compose.waitUntil(5_000) { model.state.value.map.viewportRequest != null }
        val initial = model.state.value.map.viewportRequest

        compose.runOnIdle {
            model.selectSection(WorkspaceSection.PLACE_POOL)
            model.setSheetLevel(WorkspaceSheetLevel.EXPANDED)
            listRender.value++
        }
        compose.waitUntil(5_000) { model.state.value.sheetLevel == WorkspaceSheetLevel.EXPANDED }
        compose.waitForIdle()

        assertEquals(initial, model.state.value.map.viewportRequest)
    }

    @Test fun emptyScopeClearsRequestSoReplacementHostDoesNotReplayInitialViewport() {
        val point = GeoPoint(39.9, 116.4)
        val saved = SavedPlace("saved", "trip", "poi", "故宫", "地址", point, "", emptyList())
        val model = TripWorkspaceViewModel("trip", Trips(), SavedPlaces(saved), Itineraries(), Legs(), SavedStateHandle())
        compose.waitUntil(5_000) { model.state.value.map.viewportRequest?.reason == ViewportReason.INITIAL }
        val gate = TestConsentGate()
        gate.show()
        val token = requireNotNull(gate.decide(true))
        val owner = mutableStateOf<TestOwner>(TestOwner())
        val hosts = mutableListOf<RecordingHost>()

        compose.setContent {
            val workspaceState by model.state.collectAsState()
            CompositionLocalProvider(LocalLifecycleOwner provides owner.value) {
                AmapComposeMap(workspaceState.map, {}, token, hostFactory = { RecordingHost(it).also(hosts::add) })
            }
        }
        compose.waitUntil(5_000) { hosts.firstOrNull()?.viewportCalls == 1 }

        compose.runOnIdle {
            model.selectItineraryScope(ItineraryScope.Day("day"))
            model.selectSection(WorkspaceSection.ITINERARY)
        }
        compose.waitUntil(5_000) { model.state.value.map.viewportRequest == null }
        compose.runOnIdle { owner.value = TestOwner() }
        compose.waitUntil(5_000) { hosts.size == 2 }

        assertEquals(1, hosts[0].viewportCalls)
        assertEquals(0, hosts[1].viewportCalls)
    }

    private class RecordingHost(context: android.content.Context) : AmapMapHost {
        override val view = View(context)
        override fun canRenderBeforeReady() = true
        var lastModel: MapUiModel? = null
        var lastLayer: MapLayer? = null
        var viewportCalls = 0
        private var lastViewportId: Long? = null
        override fun onCreate() = Unit
        override fun onResume() = Unit
        override fun onPause() = Unit
        override fun onDestroy() = Unit
        override fun render(model: MapUiModel, layer: MapLayer, onMarkerClick: (String) -> Unit, onLayerError: (Throwable, MapLayer) -> Unit) {
            lastModel = model
            lastLayer = layer
            model.viewportRequest?.let {
                if (it.id != lastViewportId) {
                    lastViewportId = it.id
                    viewportCalls++
                }
            }
        }
    }

    private class TestOwner : LifecycleOwner {
        private val registry = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
        override val lifecycle: Lifecycle = registry
    }

    private class SavedPlaces(private val place: SavedPlace) : SavedPlaceRepository {
        override fun observePlaces(tripId: String, tagIds: Set<String>) = flowOf(listOf(place))
        override fun observeTags(tripId: String) = flowOf(emptyList<PlaceTag>())
        override fun observeSavedPoiIds(tripId: String) = flowOf(setOf(place.amapPoiId))
        override suspend fun save(tripId: String, candidate: PlaceCandidate) = SavePlaceResult.AlreadySaved(place.id)
        override suspend fun updateDetails(placeId: String, note: String, tagNames: Set<String>) = Unit
        override suspend fun usageCount(placeId: String) = 0
        override suspend fun deletionImpact(placeId: String) = com.yangchengwei.easytrip.place.domain.PlaceDeletionImpact(usageCount(placeId), 0)
        override suspend fun deletePlaceAndReferences(placeId: String) = Unit
    }

    private class Trips : TripRepository {
        override fun observeTrip(tripId: String) = flowOf(TripWithDays("trip", "北京", LocalDate.of(2026, 8, 22), TravelMode.FLEXIBLE, listOf(TripDay("day", 0))))
        override fun observeTrips() = flowOf(emptyList<TripSummary>())
        override suspend fun createTrip(command: CreateTrip) = "trip"
        override suspend fun setHasTraveled(tripId: String, hasTraveled: Boolean) = Unit
        override suspend fun renameTrip(tripId: String, name: String) = Unit
        override suspend fun setStartDate(tripId: String, startDate: LocalDate?) = Unit
        override suspend fun dateRangeDeletionCounts(tripId: String, dayIds: List<String>) = com.yangchengwei.easytrip.trip.domain.DateRangeDeletionCounts(0, 0, 0)
        override suspend fun applyDateRange(command: com.yangchengwei.easytrip.trip.domain.DateRangeApply) = Unit
        override suspend fun setTravelMode(tripId: String, mode: TravelMode) = Unit
        override suspend fun insertDay(tripId: String, anchorDayId: String?, side: InsertSide) = "day"
        override suspend fun moveDay(tripId: String, dayId: String, targetIndex: Int) = Unit
        override suspend fun deleteDay(command: com.yangchengwei.easytrip.trip.domain.DayDeletion) = Unit
        override suspend fun deleteTrip(tripId: String) = Unit
    }

    private class Places : SavedPlaceRepository {
        override fun observePlaces(tripId: String, tagIds: Set<String>) = flowOf(emptyList<SavedPlace>())
        override fun observeTags(tripId: String) = flowOf(emptyList<PlaceTag>())
        override fun observeSavedPoiIds(tripId: String) = flowOf(emptySet<String>())
        override suspend fun save(tripId: String, candidate: PlaceCandidate) = SavePlaceResult.Saved("p")
        override suspend fun updateDetails(placeId: String, note: String, tagNames: Set<String>) = Unit
        override suspend fun usageCount(placeId: String) = 0
        override suspend fun deletionImpact(placeId: String) = com.yangchengwei.easytrip.place.domain.PlaceDeletionImpact(usageCount(placeId), 0)
        override suspend fun deletePlaceAndReferences(placeId: String) = Unit
    }

    private class Itineraries : ItineraryRepository {
        override fun observeDay(dayId: String) = flowOf(DayItinerary("day", "trip", emptyList()))
        override suspend fun addItem(dayId: String, savedPlaceId: String, targetIndex: Int) = "i"
        override suspend fun moveItem(itemId: String, targetDayId: String, targetIndex: Int) = Unit
        override suspend fun deleteItem(itemId: String) = Unit
        override suspend fun updateTiming(itemId: String, arrivalTime: LocalTime?, stayMinutes: Int?) = Unit
        override suspend fun updateDetails(itemId: String, arrivalTime: LocalTime?, stayMinutes: Int?, note: String?) = error("Fake itinerary details are not modeled")
        override suspend fun removePlaceOccurrences(placeId: String) = Unit
    }

    private class Legs : RouteLegRepository {
        override fun observeDay(dayId: String) = flowOf(emptyList<com.yangchengwei.easytrip.route.data.RouteLegEntity>())
        override fun observePending(): Flow<List<RouteLegWithEndpoints>> = flowOf(emptyList())
        override suspend fun get(legId: String) = null
        override suspend fun requeueTransientFailures() = 0
        override suspend fun recoverInterruptedCalculations(online: Boolean) = 0
        override suspend fun repairCorruptPolyline(legId: String, version: Long) = false
        override suspend fun claimIfVersionMatches(legId: String, version: Long) = false
        override suspend fun waitForNetworkIfVersionMatches(legId: String, version: Long) = false
        override suspend fun releaseClaimIfVersionMatches(legId: String, version: Long, online: Boolean) = false
        override suspend fun completeIfVersionMatches(legId: String, version: Long, result: RouteResult) = false
        override suspend fun failIfVersionMatches(legId: String, version: Long, failure: RoutePlanOutcome.Failure) = false
        override suspend fun updateDetails(legId: String, selectedModeOverride: com.yangchengwei.easytrip.core.model.TransportMode?, durationOverrideSeconds: Int?, note: String?, online: Boolean): Boolean = error("Fake route details are not modeled")
        override suspend fun retry(legId: String, online: Boolean) = false
    }
}
