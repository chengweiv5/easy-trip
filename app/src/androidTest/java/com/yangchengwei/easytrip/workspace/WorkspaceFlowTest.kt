package com.yangchengwei.easytrip.workspace

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
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
import androidx.test.espresso.Espresso.pressBack
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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class WorkspaceFlowTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun routeBackClosesOverlayBeforeLeavingAndTopBackUsesSamePriority() {
        val model = TripWorkspaceViewModel("trip", Trips(), Places(), Itineraries(), Legs(), SavedStateHandle())
        var backCount = 0
        compose.setContent {
            TripWorkspaceRoute(
                viewModel = model,
                consent = null,
                onBack = { backCount++ },
                onSettings = {},
                placeState = com.yangchengwei.easytrip.place.ui.PlacePoolUiState(),
                onPlaceAction = {},
                itineraryState = com.yangchengwei.easytrip.itinerary.ui.DayItineraryUiState(),
                onItineraryAction = {},
            )
        }
        compose.waitUntil(5_000) { model.pageState.value is TripWorkspacePageState.Ready }

        compose.onNodeWithTag("layer-menu").performClick()
        compose.waitUntil { model.state.value.overlay == WorkspaceOverlay.LayerMenu }
        pressBack()
        compose.waitUntil { model.state.value.overlay == WorkspaceOverlay.None }
        assertEquals(0, backCount)
        pressBack()
        compose.waitUntil { backCount == 1 }

        compose.onNodeWithTag("layer-menu").performClick()
        compose.onNodeWithText("返回").performClick()
        compose.waitUntil { model.state.value.overlay == WorkspaceOverlay.None }
        assertEquals(1, backCount)
        compose.onNodeWithText("返回").performClick()
        compose.waitUntil { backCount == 2 }
    }

    @Test fun mapDetailBackThenPlaceEditRendersNewTarget() {
        val workspace = TripWorkspaceViewModel("trip", Trips(), Places(), Itineraries(), Legs(), SavedStateHandle())
        val placeModel = com.yangchengwei.easytrip.place.ui.PlacePoolViewModel("trip", EditingPlaces(), null)
        compose.setContent {
            TripWorkspaceRoute(
                viewModel = workspace,
                consent = null,
                onBack = {},
                onSettings = {},
                placeViewModel = placeModel,
            )
        }
        compose.waitUntil(5_000) { workspace.pageState.value is TripWorkspacePageState.Ready }

        compose.runOnIdle {
            workspace.selectMapPoi(MapPoiUi("old-poi", "旧地图地点", "旧地址", GeoPoint(39.9, 116.4)))
        }
        compose.onNodeWithText("旧地图地点").assertIsDisplayed()
        pressBack()
        compose.waitUntil { workspace.state.value.selectedMapPoi == null }

        compose.onNodeWithText("编辑").performClick()
        compose.onNodeWithText("编辑 新编辑地点").assertIsDisplayed()
        compose.onNodeWithText("旧地图地点").assertDoesNotExist()
    }

    @Test fun deleteConfirmationWaitsForReadyTargetAndConfirmsOnce() {
        val workspace = TripWorkspaceViewModel("trip", Trips(), Places(), Itineraries(), Legs(), SavedStateHandle())
        val repository = DelayedDeletePlaces()
        val placeModel = com.yangchengwei.easytrip.place.ui.PlacePoolViewModel("trip", repository, null)
        compose.setContent {
            TripWorkspaceRoute(
                viewModel = workspace,
                consent = null,
                onBack = {},
                onSettings = {},
                placeViewModel = placeModel,
            )
        }
        compose.waitUntil(5_000) { placeModel.state.value.search.savedPlaces.isNotEmpty() }
        compose.onNodeWithTag("delete-place-saved").performClick()
        compose.onNodeWithTag("confirmation-confirm").assertDoesNotExist()

        compose.runOnIdle { repository.usage.complete(2) }
        compose.waitUntil(5_000) { placeModel.state.value.deleting != null }
        compose.onNodeWithTag("confirmation-confirm").assertIsDisplayed().performClick()
        compose.waitUntil(5_000) { repository.deleteCalls == 1 }
        assertEquals(1, repository.deleteCalls)
        compose.onNodeWithTag("confirmation-confirm").assertDoesNotExist()
    }

    @Test fun mapPoiClickOpensCardBeforeCollectionAction() {
        val saved = SavedStateHandle()
        val model = TripWorkspaceViewModel("trip", Trips(), Places(), Itineraries(), Legs(), saved)
        val poi = MapPoiUi("poi-card", "故宫", "北京市东城区", GeoPoint(39.916, 116.397))
        lateinit var host: PoiHost
        var toggles = 0
        compose.setContent {
            TripWorkspaceScreen(
                viewModel = model,
                consent = consentToken(),
                onBack = {},
                onSettings = {},
                isPoiSaved = false,
                onTogglePoiCollection = { toggles++ },
                placeContent = { Text("地点内容") },
                dayItineraryContent = { Text("行程内容") },
                mapHostFactory = { PoiHost(it).also { created -> host = created } },
            )
        }
        compose.waitUntil(5_000) { runCatching { host }.isSuccess }

        compose.runOnIdle { host.emit(poi) }

        compose.onNodeWithText("故宫").assertIsDisplayed()
        compose.onNodeWithText("北京市东城区").assertIsDisplayed()
        val viewportBeforeClose = model.state.value.map.viewportRequest
        compose.onNodeWithText("关闭").performClick()
        compose.waitForIdle()
        assertEquals(viewportBeforeClose, model.state.value.map.viewportRequest)
        assertEquals(0, toggles)
    }

    @Test fun mapPoiWithoutAddressShowsUnavailableMessageAndCanBeCollected() {
        val model = TripWorkspaceViewModel("trip", Trips(), Places(), Itineraries(), Legs(), SavedStateHandle())
        lateinit var host: PoiHost
        var collected: PlaceCandidate? = null
        compose.setContent {
            TripWorkspaceScreen(
                viewModel = model,
                consent = consentToken(),
                onBack = {},
                onSettings = {},
                isPoiSaved = false,
                onTogglePoiCollection = { collected = it },
                placeContent = { Text("地点内容") },
                dayItineraryContent = { Text("行程内容") },
                mapHostFactory = { PoiHost(it).also { created -> host = created } },
            )
        }
        compose.waitUntil(5_000) { runCatching { host }.isSuccess }
        compose.runOnIdle { host.emit(MapPoiUi("poi-empty-address", "故宫", "", GeoPoint(39.9, 116.4))) }

        compose.onNodeWithText("地址暂不可用").assertIsDisplayed()
        compose.onNodeWithTag("place-card-collection").performClick()
        assertEquals("", collected?.address)
    }

    @Test fun mapPoiWithoutStableIdCannotBeCollected() {
        val model = TripWorkspaceViewModel("trip", Trips(), Places(), Itineraries(), Legs(), SavedStateHandle())
        lateinit var host: PoiHost
        compose.setContent {
            TripWorkspaceScreen(
                viewModel = model,
                consent = consentToken(),
                onBack = {},
                onSettings = {},
                isPoiSaved = false,
                onTogglePoiCollection = {},
                placeContent = { Text("地点内容") },
                dayItineraryContent = { Text("行程内容") },
                mapHostFactory = { PoiHost(it).also { created -> host = created } },
            )
        }
        compose.waitUntil(5_000) { runCatching { host }.isSuccess }
        compose.runOnIdle { host.emit(MapPoiUi(null, "无编号地点", "地址未知", GeoPoint(39.9, 116.4))) }

        compose.onNodeWithText("无法收藏").assertIsNotEnabled()
    }

    @Test fun bottomNavigationKeepsItineraryAndMapScopeSynchronized() {
        val model = TripWorkspaceViewModel("trip", Trips(), Places(), Itineraries(), Legs(), SavedStateHandle())
        compose.setContent {
            TripWorkspaceScreen(
                viewModel = model,
                consent = null,
                onBack = {},
                onSettings = {},
                placeContent = { Text("地点内容") },
                dayItineraryContent = { Text("可编辑日行程") },
            )
        }
        compose.waitUntil(5_000) { model.state.value.map.viewportRequest != null }
        compose.onNodeWithTag("workspace-top-bar").assertHeightIsEqualTo(52.dp)
        compose.onNodeWithTag("workspace-search-launcher").assertHeightIsEqualTo(48.dp)
        compose.onNodeWithTag("workspace-sheet-handle").assertIsDisplayed()
        assertEquals(0, compose.onAllNodesWithText("收起").fetchSemanticsNodes().size)
        assertEquals(0, compose.onAllNodesWithText("半屏").fetchSemanticsNodes().size)
        assertEquals(0, compose.onAllNodesWithText("展开").fetchSemanticsNodes().size)
        compose.onNodeWithTag("workspace-map").assertIsDisplayed()
        val mapBottom = compose.onNodeWithTag("workspace-map").getUnclippedBoundsInRoot().bottom
        val searchTop = compose.onNodeWithTag("workspace-search-launcher").getUnclippedBoundsInRoot().top
        val navigationTop = compose.onNodeWithTag("section-controls").getUnclippedBoundsInRoot().top
        assert(searchTop < mapBottom)
        assert(mapBottom <= navigationTop)
        compose.onNodeWithTag("section-PLACE_POOL").assertExists()
        compose.onNodeWithTag("section-ITINERARY").assertExists()
        compose.onNodeWithTag("scope-PLACE_POOL").assertDoesNotExist()
        compose.onNodeWithTag("scope-SINGLE_DAY").assertDoesNotExist()
        compose.onNodeWithTag("scope-WHOLE_TRIP").assertDoesNotExist()
        compose.onNodeWithTag("tab-PLACES").assertDoesNotExist()
        compose.onNodeWithTag("tab-ITINERARY").assertDoesNotExist()
        compose.onNodeWithText("地点内容").assertIsDisplayed()

        val placeRequestId = model.state.value.map.viewportRequest?.id
        compose.onNodeWithTag("section-ITINERARY").performClick()
        compose.waitUntil(5_000) { model.state.value.mapScope == MapScope.SINGLE_DAY }
        compose.onNodeWithTag("itinerary-scope-rail").assertIsDisplayed()
        compose.onNodeWithText("第一天").assertIsDisplayed()
        compose.onNodeWithText("可编辑日行程").assertIsDisplayed()
        assertEquals(ItineraryScope.Day("day-1"), model.state.value.itineraryScope)
        assertEquals(placeRequestId?.plus(1), model.state.value.map.viewportRequest?.id)

        val dayOneRequestId = model.state.value.map.viewportRequest?.id
        compose.onNodeWithTag("itinerary-scope-WHOLE_TRIP").performClick()
        compose.waitUntil(5_000) { model.state.value.mapScope == MapScope.WHOLE_TRIP }
        compose.onNodeWithText("可编辑日行程").assertDoesNotExist()
        compose.onNodeWithTag("whole-trip-day-day-1").assertExists()
        compose.onNodeWithTag("whole-trip-day-day-2").assertExists()
        assertEquals(dayOneRequestId?.plus(1), model.state.value.map.viewportRequest?.id)

        val wholeTripRequestId = model.state.value.map.viewportRequest?.id
        compose.onNodeWithTag("itinerary-scope-day-2").performClick()
        compose.waitUntil(5_000) { model.state.value.selectedDayId == "day-2" }
        compose.onNodeWithText("可编辑日行程").assertIsDisplayed()
        assertEquals(MapScope.SINGLE_DAY, model.state.value.mapScope)
        assertEquals(wholeTripRequestId?.plus(1), model.state.value.map.viewportRequest?.id)

        compose.onNodeWithTag("section-PLACE_POOL").performClick()
        compose.waitUntil(5_000) { model.state.value.section == WorkspaceSection.PLACE_POOL }
        val returnedPlaceRequestId = model.state.value.map.viewportRequest?.id
        compose.onNodeWithTag("section-ITINERARY").performClick()
        compose.waitUntil(5_000) { model.state.value.section == WorkspaceSection.ITINERARY }
        assertEquals(ItineraryScope.Day("day-2"), model.state.value.itineraryScope)
        assertEquals(returnedPlaceRequestId?.plus(1), model.state.value.map.viewportRequest?.id)

        compose.onNodeWithTag("workspace-sheet-handle").performTouchInput { swipeDown() }
        compose.waitUntil(5_000) { model.state.value.sheetLevel == WorkspaceSheetLevel.COLLAPSED }
        compose.onNodeWithTag("workspace-sheet-handle").assertIsDisplayed().performTouchInput { swipeUp() }
        compose.waitUntil(5_000) { model.state.value.sheetLevel == WorkspaceSheetLevel.HALF }
    }

    private class EditingPlaces : SavedPlaceRepository {
        private val place = SavedPlace("saved", "trip", "saved-poi", "新编辑地点", "新地址", GeoPoint(39.8, 116.3), "", emptyList())
        override fun observePlaces(tripId: String, tagIds: Set<String>) = flowOf(listOf(place))
        override fun observeTags(tripId: String) = flowOf(emptyList<PlaceTag>())
        override fun observeSavedPoiIds(tripId: String) = flowOf(setOf("saved-poi"))
        override suspend fun save(tripId: String, candidate: PlaceCandidate) = SavePlaceResult.Saved("saved")
        override suspend fun updateDetails(placeId: String, note: String, tagNames: Set<String>) = Unit
        override suspend fun usageCount(placeId: String) = 0
        override suspend fun deletePlaceAndReferences(placeId: String) = Unit
    }

    private class DelayedDeletePlaces : SavedPlaceRepository {
        val usage = CompletableDeferred<Int>()
        var deleteCalls = 0
        private val place = SavedPlace("saved", "trip", "poi", "待删除地点", "地址", GeoPoint(39.9, 116.4), "", emptyList())
        override fun observePlaces(tripId: String, tagIds: Set<String>) = flowOf(listOf(place))
        override fun observeTags(tripId: String) = flowOf(emptyList<PlaceTag>())
        override fun observeSavedPoiIds(tripId: String) = flowOf(setOf("poi"))
        override suspend fun save(tripId: String, candidate: PlaceCandidate) = SavePlaceResult.Saved("saved")
        override suspend fun updateDetails(placeId: String, note: String, tagNames: Set<String>) = Unit
        override suspend fun usageCount(placeId: String) = usage.await()
        override suspend fun deletePlaceAndReferences(placeId: String) { deleteCalls++ }
    }

    private fun consentToken(): com.yangchengwei.easytrip.amap.AmapConsentToken {
        val gate = com.yangchengwei.easytrip.amap.AmapPrivacyGate.create(compose.activity)
        gate.reportPrivacyShown()
        return requireNotNull(gate.reportUserDecision(true))
    }

    private class PoiHost(context: android.content.Context) : AmapMapHost {
        override val view = android.view.View(context)
        private var callback: (MapPoiUi) -> Unit = {}
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
            callback = onMapPoiClick
        }
        fun emit(poi: MapPoiUi) = callback(poi)
    }

    private class Trips : TripRepository {
        override fun observeTrip(tripId: String) = flowOf(
            TripWithDays(
                "trip",
                "川西",
                null,
                TravelMode.FLEXIBLE,
                listOf(TripDay("day-1", 0), TripDay("day-2", 1)),
            ),
        )
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
        override fun observeDay(dayId: String) = flowOf(
            DayItinerary(
                dayId,
                "trip",
                listOf(
                    ItineraryItem(
                        "i-$dayId",
                        ItineraryPlace("p-$dayId", if (dayId == "day-1") "酒店" else "景点", "", GeoPoint(1.0, if (dayId == "day-1") 2.0 else 3.0)),
                        null,
                        null,
                    ),
                ),
            ),
        )
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
