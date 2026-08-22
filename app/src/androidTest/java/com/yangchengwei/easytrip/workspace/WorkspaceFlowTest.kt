package com.yangchengwei.easytrip.workspace

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.itinerary.domain.DayItinerary
import com.yangchengwei.easytrip.itinerary.domain.ItineraryItem
import com.yangchengwei.easytrip.itinerary.domain.ItineraryPlace
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
import org.junit.Rule
import org.junit.Test

class WorkspaceFlowTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun switchesTabsAndMapScopesAndRestoresSelection() {
        val saved = SavedStateHandle()
        val model = TripWorkspaceViewModel("trip", Trips(), Places(), Itineraries(), Legs(), saved)
        compose.setContent {
            TripWorkspaceScreen(
                viewModel = model,
                consent = null,
                onBack = {},
                onSettings = {},
                placeContent = { Text("地点内容") },
                itineraryContent = { Text("行程内容") },
            )
        }
        compose.onNodeWithTag("workspace-top-bar").assertHeightIsEqualTo(48.dp)
        compose.onNodeWithTag("workspace-search").assertHeightIsEqualTo(48.dp)
        compose.onNodeWithTag("workspace-sheet-handle").assertIsDisplayed()
        assertEquals(0, compose.onAllNodesWithText("收起").fetchSemanticsNodes().size)
        assertEquals(0, compose.onAllNodesWithText("半屏").fetchSemanticsNodes().size)
        assertEquals(0, compose.onAllNodesWithText("展开").fetchSemanticsNodes().size)
        compose.onNodeWithTag("workspace-map").assertIsDisplayed()
        val mapBottom = compose.onNodeWithTag("workspace-map").getUnclippedBoundsInRoot().bottom
        val searchTop = compose.onNodeWithTag("workspace-search").getUnclippedBoundsInRoot().top
        val scopeTop = compose.onNodeWithTag("scope-PLACE_POOL").getUnclippedBoundsInRoot().top
        assert(mapBottom <= searchTop)
        assert(searchTop < scopeTop)
        compose.onNodeWithText("地点内容").assertIsDisplayed()
        compose.onNodeWithText("每日行程").performClick()
        compose.onNodeWithText("行程内容").assertIsDisplayed()
        compose.onNodeWithTag("scope-WHOLE_TRIP").performClick()
        compose.waitUntil(5_000) { model.state.value.mapScope == MapScope.WHOLE_TRIP }
        assertEquals("WHOLE_TRIP", saved.get<String>("workspace.scope"))
        compose.onNodeWithTag("workspace-sheet-handle").performTouchInput { swipeDown() }
        compose.waitUntil(5_000) { model.state.value.sheetLevel == WorkspaceSheetLevel.COLLAPSED }
        android.os.SystemClock.sleep(1_000)
        assertEquals(WorkspaceSheetLevel.COLLAPSED, model.state.value.sheetLevel)
        compose.onNodeWithTag("workspace-sheet-handle").assertIsDisplayed().performTouchInput { swipeUp() }
        compose.waitUntil(5_000) { model.state.value.sheetLevel == WorkspaceSheetLevel.HALF }
    }

    private class Trips : TripRepository {
        override fun observeTrip(tripId: String) = flowOf(TripWithDays("trip", "川西", null, TravelMode.FLEXIBLE, listOf(TripDay("day", 0))))
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
        override fun observePlaces(tripId: String, tagIds: Set<String>) = flowOf(listOf(SavedPlace("p", "trip", "poi", "酒店", "", GeoPoint(1.0, 2.0), "", emptyList())))
        override fun observeTags(tripId: String) = flowOf(emptyList<PlaceTag>())
        override fun observeSavedPoiIds(tripId: String) = flowOf(emptySet<String>())
        override suspend fun save(tripId: String, candidate: PlaceCandidate) = SavePlaceResult.Saved("p")
        override suspend fun updateDetails(placeId: String, note: String, tagNames: Set<String>) = Unit
        override suspend fun usageCount(placeId: String) = 0
        override suspend fun deletePlaceAndReferences(placeId: String) = Unit
    }

    private class Itineraries : ItineraryRepository {
        override fun observeDay(dayId: String) = flowOf(DayItinerary("day", "trip", listOf(ItineraryItem("i", ItineraryPlace("p", "酒店", "", GeoPoint(1.0, 2.0)), null, null))))
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
