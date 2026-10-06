package com.yangchengwei.easytrip.workspace

import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import androidx.room.Room
import com.yangchengwei.easytrip.AppNavigation
import com.yangchengwei.easytrip.AppNavigationDependencies
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import com.yangchengwei.easytrip.itinerary.data.RoomItineraryRepository
import com.yangchengwei.easytrip.itinerary.ui.ItineraryScopeRail
import com.yangchengwei.easytrip.permission.InMemoryLocationPermissionRequestStore
import com.yangchengwei.easytrip.place.data.RoomSavedPlaceRepository
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.domain.SavePlaceResult
import com.yangchengwei.easytrip.route.data.RoomRouteLegRepository
import com.yangchengwei.easytrip.trip.data.RoomTripRepository
import com.yangchengwei.easytrip.trip.domain.CreateTrip
import com.yangchengwei.easytrip.trip.domain.TripDay
import com.yangchengwei.easytrip.trip.domain.TripService
import com.yangchengwei.easytrip.trip.ui.RoomDeleteImpactProvider
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class DefaultItineraryDayTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun ongoingTripOpensTodayAndReenteringReappliesDefault() {
        verifyNavigation(LocalDate.now().minusDays(1), expectedIndex = 1, verifyReentry = true)
    }

    @Test fun futureTripOpensFirstDay() {
        verifyNavigation(LocalDate.now().plusDays(1), expectedIndex = 0)
    }

    @Test fun pastTripOpensFirstDay() {
        verifyNavigation(LocalDate.now().minusDays(3), expectedIndex = 0)
    }

    @Test fun undatedTripOpensFirstDay() {
        verifyNavigation(null, expectedIndex = 0)
    }

    @Test fun defaultSelectedDayIsVisibleWithoutManualScrollingInLongTrip() {
        val days = (1..30).map { TripDay("day-$it", it - 1) }
        compose.setContent {
            EasyTripTheme {
                ItineraryScopeRail(
                    days = days,
                    selected = ItineraryScope.Day("day-20"),
                    onSelect = {},
                    modifier = Modifier.width(64.dp).height(320.dp),
                )
            }
        }
        compose.onNodeWithTag("itinerary-scope-day-20").assertIsDisplayed().assertIsSelected()
        compose.onNodeWithTag("itinerary-scope-rail").performScrollToNode(hasTestTag("itinerary-scope-day-30"))
        compose.onNodeWithTag("itinerary-scope-day-30").assertIsDisplayed()
    }

    @Test fun firstDaySelectionDoesNotHideWholeTripEntry() {
        compose.setContent {
            EasyTripTheme {
                ItineraryScopeRail(
                    days = (1..3).map { TripDay("day-$it", it - 1) },
                    selected = ItineraryScope.Day("day-1"),
                    onSelect = {},
                    modifier = Modifier.width(64.dp).height(320.dp),
                )
            }
        }
        compose.onNodeWithTag("itinerary-scope-WHOLE_TRIP").assertIsDisplayed()
        compose.onNodeWithTag("itinerary-scope-day-1").assertIsDisplayed().assertIsSelected()
    }

    private fun verifyNavigation(startDate: LocalDate?, expectedIndex: Int, verifyReentry: Boolean = false) {
        val database = Room.inMemoryDatabaseBuilder(compose.activity, EasyTripDatabase::class.java).build()
        try {
            val trips = RoomTripRepository(database.tripDao())
            val tripId = runBlocking { trips.createTrip(CreateTrip("默认日期回归", 3, startDate = startDate)) }
            val days = runBlocking { requireNotNull(trips.observeTrip(tripId).first()).days }
            val places = RoomSavedPlaceRepository(database)
            val itineraries = RoomItineraryRepository(database, database.itineraryEditingDao(), database.routeLegDao())
            runBlocking {
                val place = places.save(tripId, PlaceCandidate("test-place", "测试地点", "", GeoPoint(30.0, 120.0), null)) as SavePlaceResult.Saved
                days.forEach { itineraries.addItem(it.id, place.id, 0) }
            }
            compose.setContent {
                EasyTripTheme {
                    AppNavigation(
                        service = TripService(trips),
                        repository = trips,
                        impacts = RoomDeleteImpactProvider(database.deleteImpactDao()),
                        dependencies = AppNavigationDependencies(
                            savedPlaceRepository = places,
                            itineraryRepository = itineraries,
                            routeLegRepository = RoomRouteLegRepository(database.routeLegDao()),
                            mapPreferences = InMemoryMapPreferences(),
                            locationPermissionRequestStore = InMemoryLocationPermissionRequestStore(),
                        ),
                    )
                }
            }
            fun openItinerary() {
                val trip = hasTestTag("primary-trip-$tripId") or hasTestTag("other-trip-$tripId")
                compose.waitUntil(5_000) { compose.onAllNodes(trip).fetchSemanticsNodes().isNotEmpty() }
                compose.onNode(trip).performClick()
                compose.waitUntil(5_000) { compose.onAllNodesWithTag("section-ITINERARY").fetchSemanticsNodes().isNotEmpty() }
                compose.onNodeWithTag("section-ITINERARY").performClick()
                compose.waitUntil(5_000) { compose.onAllNodesWithTag("itinerary-scope-${days[expectedIndex].id}").fetchSemanticsNodes().isNotEmpty() }
                compose.onNodeWithTag("itinerary-scope-${days[expectedIndex].id}").assertIsSelected()
            }
            openItinerary()
            if (verifyReentry) {
                compose.onNodeWithTag("itinerary-scope-${days[2].id}").performClick()
                compose.onNodeWithTag("section-PLACE_POOL").performClick()
                compose.onNodeWithTag("section-ITINERARY").performClick()
                compose.onNodeWithTag("itinerary-scope-${days[2].id}").assertIsSelected()
                compose.onNodeWithTag("workspace-back").performClick()
                openItinerary()
            }
        } finally {
            compose.runOnIdle { compose.activity.setContentView(View(compose.activity)) }
            database.close()
        }
    }
}
