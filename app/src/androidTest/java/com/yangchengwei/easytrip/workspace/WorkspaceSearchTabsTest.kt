package com.yangchengwei.easytrip.workspace

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.itinerary.domain.DayItinerary
import com.yangchengwei.easytrip.itinerary.domain.ItineraryRepository
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.ui.PlaceSearchResults
import com.yangchengwei.easytrip.place.ui.PlaceSearchState
import com.yangchengwei.easytrip.place.ui.SavedPlacesContent
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class WorkspaceSearchTabsTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun searchAndSavedPlacesRenderOnlyInTheirOwnTabs() {
        val candidate = candidate("poi-1")
        val savedPlace = savedPlace()
        val model = model(SavedStateHandle(), MutableStateFlow(listOf(candidate)))
        compose.setContent {
            TripWorkspaceScreen(
                viewModel = model,
                consent = null,
                onBack = {},
                onSettings = {},
                searchContent = {
                    PlaceSearchResults(
                        state = PlaceSearchState(query = "故宫", results = listOf(candidate)),
                        savedPoiIds = emptySet(),
                        onSelect = model::focusSearchResult,
                        onSave = {},
                    )
                },
                placeContent = {
                    SavedPlacesContent(
                        places = listOf(savedPlace),
                        tags = emptyList(),
                        selectedTagIds = emptySet(),
                        onToggleTag = {},
                        onEdit = {},
                        onDelete = {},
                    )
                },
                itineraryContent = { Text("行程内容") },
            )
        }

        compose.onAllNodesWithTag("search-result-poi-1").assertCountEquals(0)
        compose.onNodeWithText("酒店").assertIsDisplayed()
        compose.onNodeWithTag("tab-SEARCH").performClick().assertIsSelected()
        compose.onNodeWithTag("search-result-poi-1").assertIsDisplayed()
        compose.onAllNodesWithText("酒店").assertCountEquals(0)
        compose.onNodeWithTag("tab-PLACES").performClick().assertIsSelected()
        compose.onNodeWithText("酒店").assertIsDisplayed()
        compose.onAllNodesWithTag("search-result-poi-1").assertCountEquals(0)
    }

    @Test fun whitespaceQuerySelectsSearchAndRealResultClickKeepsTab() {
        val candidate = candidate("poi-1")
        val model = model(SavedStateHandle(), MutableStateFlow(listOf(candidate)))
        model.onSearchQueryChanged(" ")
        compose.setContent {
            TripWorkspaceScreen(
                viewModel = model,
                consent = null,
                onBack = {},
                onSettings = {},
                searchContent = {
                    PlaceSearchResults(
                        state = PlaceSearchState(query = "故宫", results = listOf(candidate)),
                        savedPoiIds = emptySet(),
                        onSelect = model::focusSearchResult,
                        onSave = {},
                    )
                },
                placeContent = { Text("已收藏") },
                itineraryContent = { Text("行程") },
            )
        }

        compose.onNodeWithTag("tab-SEARCH").assertIsSelected()
        compose.onNodeWithTag("search-result-poi-1").performClick()
        compose.waitUntil(5_000) { model.state.value.searchSelection?.poiId == "poi-1" }
        assertEquals(WorkspaceTab.SEARCH, model.state.value.tab)
        assertEquals("poi-1", model.state.value.searchSelection?.poiId)
    }

    @Test fun restoredFocusSurvivesInitialEmptyResultsAndClearsAfterObservedResultDisappears() {
        val point = GeoPoint(39.9, 116.4)
        val saved = SavedStateHandle(mapOf(
            "workspace.focusedPoi" to "poi-1",
            "workspace.focusedLatitude" to point.latitude,
            "workspace.focusedLongitude" to point.longitude,
        ))
        val results = MutableStateFlow(emptyList<PlaceCandidate>())
        val model = model(saved, results)
        compose.waitUntil(5_000) { model.state.value.searchSelection?.poiId == "poi-1" }
        assertEquals(point, model.state.value.searchSelection?.point)

        results.value = listOf(candidate("poi-1"))
        compose.waitUntil(5_000) {
            model.state.value.map.markers.any { it.key == "search-poi-1" && it.label == "故宫" }
        }
        results.value = emptyList()
        compose.waitUntil(5_000) { model.state.value.searchSelection == null }
    }

    @Test fun removingFocusedPoiFromResultsClearsFocus() {
        val results = MutableStateFlow(listOf(candidate("poi-1")))
        val model = model(SavedStateHandle(), results)
        model.focusSearchResult(candidate("poi-1"))
        compose.waitUntil(5_000) { model.state.value.searchSelection != null }

        results.value = emptyList()

        compose.waitUntil(5_000) { model.state.value.searchSelection == null }
        assertNull(model.state.value.searchSelection)
    }

    private fun model(saved: SavedStateHandle, results: Flow<List<PlaceCandidate>>) =
        TripWorkspaceViewModel("trip", Trips(), Places(), Itineraries(), Legs(), saved, results)

    private fun candidate(id: String) = PlaceCandidate(id, "故宫", "地址", GeoPoint(39.9, 116.4), null)

    private fun savedPlace() = SavedPlace(
        id = "saved-1",
        tripId = "trip",
        amapPoiId = "hotel-poi",
        name = "酒店",
        address = "酒店地址",
        point = GeoPoint(39.8, 116.3),
        note = "",
        tags = emptyList(),
    )

    private class Trips : TripRepository {
        override fun observeTrip(tripId: String) = flowOf(TripWithDays("trip", "北京", LocalDate.of(2026, 8, 22), TravelMode.FLEXIBLE, listOf(TripDay("day", 0))))
        override fun observeTrips() = flowOf(emptyList<TripSummary>())
        override suspend fun createTrip(command: CreateTrip) = "trip"
        override suspend fun renameTrip(tripId: String, name: String) = Unit
        override suspend fun setStartDate(tripId: String, startDate: LocalDate?) = Unit
        override suspend fun setTravelMode(tripId: String, mode: TravelMode) = Unit
        override suspend fun insertDay(tripId: String, anchorDayId: String?, side: InsertSide) = "day"
        override suspend fun moveDay(tripId: String, dayId: String, targetIndex: Int) = Unit
        override suspend fun deleteDay(dayId: String) = Unit
        override suspend fun deleteTrip(tripId: String) = Unit
    }

    private class Places : SavedPlaceRepository {
        override fun observePlaces(tripId: String, tagIds: Set<String>) = flowOf(emptyList<SavedPlace>())
        override fun observeTags(tripId: String) = flowOf(emptyList<PlaceTag>())
        override fun observeSavedPoiIds(tripId: String) = flowOf(emptySet<String>())
        override suspend fun save(tripId: String, candidate: PlaceCandidate) = SavePlaceResult.Saved("p")
        override suspend fun updateDetails(placeId: String, note: String, tagNames: Set<String>) = Unit
        override suspend fun usageCount(placeId: String) = 0
        override suspend fun deletePlaceAndReferences(placeId: String) = Unit
    }

    private class Itineraries : ItineraryRepository {
        override fun observeDay(dayId: String) = flowOf(DayItinerary("day", "trip", emptyList()))
        override suspend fun addItem(dayId: String, savedPlaceId: String, targetIndex: Int) = "i"
        override suspend fun moveItem(itemId: String, targetDayId: String, targetIndex: Int) = Unit
        override suspend fun deleteItem(itemId: String) = Unit
        override suspend fun updateTiming(itemId: String, arrivalTime: LocalTime?, stayMinutes: Int?) = Unit
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
        override suspend fun overrideMode(legId: String, mode: com.yangchengwei.easytrip.core.model.TransportMode, online: Boolean) = false
        override suspend fun retry(legId: String, online: Boolean) = false
    }
}
