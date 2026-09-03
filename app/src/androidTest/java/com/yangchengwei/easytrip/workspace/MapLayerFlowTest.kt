package com.yangchengwei.easytrip.workspace

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import androidx.lifecycle.SavedStateHandle
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MapLayerFlowTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun mapLegendWrapsContentAndHasTextLabels() {
        compose.setContent { EasyTripTheme { MapLegend() } }

        compose.onNodeWithTag("map-legend").assertIsDisplayed()
        compose.onNodeWithText("已排入").assertIsDisplayed()
        compose.onNodeWithText("仅收藏").assertIsDisplayed()
        compose.onNodeWithTag("legend-scheduled-shape").assertIsDisplayed()
        compose.onNodeWithTag("legend-saved-shape").assertIsDisplayed()
    }

    @Test fun searchSurfaceIsFullWidthAndOpaque() {
        compose.setContent { EasyTripTheme { WorkspaceSearchBar(onClick = {}) } }

        compose.onNodeWithTag("workspace-search-surface").assertHeightIsAtLeast(46.dp)
        compose.onNodeWithText("搜索餐厅、景点或地址").assertIsDisplayed()
    }

    @Test fun selectedLayerOptionHasExplicitPrimaryBorder() {
        compose.setContent {
            EasyTripTheme {
                MapLayerMenu(
                    layer = MapLayer.STANDARD,
                    onClose = {},
                    onSelectLayer = {},
                )
            }
        }

        compose.onNodeWithTag("layer-selected-border-STANDARD", useUnmergedTree = true).assertIsDisplayed()
        compose.onAllNodesWithTag("layer-selected-border-SATELLITE", useUnmergedTree = true).assertCountEquals(0)
        compose.onAllNodesWithTag("layer-selected-border-SATELLITE_ROAD", useUnmergedTree = true).assertCountEquals(0)
    }

    @Test fun layerLauncherActiveStateFollowsMenuVisibilityInsteadOfSelectedLayer() {
        var active by mutableStateOf(false)
        compose.setContent {
            EasyTripTheme {
                MapControls(
                    active = active,
                    onOpenLayerMenu = { active = true },
                )
            }
        }

        compose.onNodeWithTag("layer-menu").assertIsNotSelected().performClick()
        compose.onNodeWithTag("layer-menu").assertIsSelected()
    }

    @Test fun layerMenuUsesExclusiveWorkspaceOverlay() {
        var overlay by mutableStateOf<WorkspaceOverlay>(WorkspaceOverlay.None)
        compose.setContent {
            EasyTripTheme {
                MapControls(
                    active = overlay == WorkspaceOverlay.LayerMenu,
                    onOpenLayerMenu = { overlay = WorkspaceOverlay.LayerMenu },
                )
                if (overlay == WorkspaceOverlay.LayerMenu) {
                    MapLayerMenu(
                        layer = MapLayer.STANDARD,
                        onClose = { overlay = WorkspaceOverlay.None },
                        onSelectLayer = {},
                    )
                }
            }
        }

        compose.onNodeWithTag("layer-menu").performClick()
        compose.onNodeWithTag("layer-menu-panel").assertIsDisplayed()
        compose.onNodeWithTag("layer-STANDARD").assertIsDisplayed()
        compose.runOnIdle { overlay = WorkspaceOverlay.PlaceDetail(1) }
        compose.onNodeWithTag("layer-menu-panel").assertDoesNotExist()
    }

    @Test fun layerControlsAreExclusiveAndSelectionSurvivesTripSwitch() {
        val preferences = FakeMapPreferences()
        val first = model("trip-1", preferences)
        var current by mutableStateOf(first)
        compose.setContent {
            TripWorkspaceScreen(
                viewModel = current,
                consent = null,
                onBack = {},
                onSettings = {},
                placeContent = { Text("地点") },
                dayItineraryContent = { Text("行程") },
            )
        }

        compose.onNodeWithTag("layer-menu").performClick()
        compose.onNodeWithTag("layer-STANDARD").assertIsDisplayed()
        compose.onNodeWithTag("layer-SATELLITE").assertIsDisplayed()
        compose.onNodeWithText("卫星地图").assertIsDisplayed()
        compose.onNodeWithTag("layer-SATELLITE_ROAD").assertIsDisplayed().performClick()
        compose.waitUntil(5_000) { first.state.value.mapLayer == MapLayer.SATELLITE_ROAD }
        assertEquals(0, compose.onAllNodesWithTag("layer-STANDARD").fetchSemanticsNodes().size)

        compose.onNodeWithTag("layer-menu").performClick()
        compose.onNodeWithText("卫星路网").assertIsDisplayed()
        compose.onNodeWithTag("layer-SATELLITE_ROAD").assertIsSelected().performClick()

        val second = model("trip-2", preferences)
        compose.runOnIdle { current = second }
        compose.waitUntil(5_000) { second.state.value.mapLayer == MapLayer.SATELLITE_ROAD }
        compose.onNodeWithTag("layer-menu").performClick()
        compose.onNodeWithText("卫星路网").assertIsDisplayed()
        compose.onNodeWithTag("layer-SATELLITE_ROAD").assertIsSelected()
        assertEquals(MapLayer.SATELLITE_ROAD, preferences.layer.value)
    }

    private fun hasRole(role: Role) = SemanticsMatcher.expectValue(SemanticsProperties.Role, role)

    private fun model(id: String, preferences: MapPreferences) = TripWorkspaceViewModel(
        id, Trips(), Places(), Itineraries(), Legs(), SavedStateHandle(), mapPreferences = preferences,
    )

    private class FakeMapPreferences : MapPreferences {
        override val layer = MutableStateFlow(MapLayer.STANDARD)
        override fun setLayer(layer: MapLayer) { this.layer.value = layer }
    }

    private class Trips : TripRepository {
        override fun observeTrip(tripId: String) = flowOf(TripWithDays("trip", "北京", LocalDate.of(2026, 8, 22), TravelMode.FLEXIBLE, listOf(TripDay("day", 0))))
        override fun observeTrips() = flowOf(emptyList<TripSummary>())
        override suspend fun createTrip(command: CreateTrip) = "trip"
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
