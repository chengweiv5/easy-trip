package com.yangchengwei.easytrip.workspace

import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import com.yangchengwei.easytrip.*
import com.yangchengwei.easytrip.amap.*
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.core.model.TransportMode
import com.yangchengwei.easytrip.itinerary.data.RoomItineraryRepository
import com.yangchengwei.easytrip.permission.InMemoryLocationPermissionRequestStore
import com.yangchengwei.easytrip.permission.LocationPermissionSnapshot
import com.yangchengwei.easytrip.place.amap.*
import com.yangchengwei.easytrip.place.data.RoomSavedPlaceRepository
import com.yangchengwei.easytrip.place.domain.PlaceCity
import com.yangchengwei.easytrip.route.data.RoomRouteLegRepository
import com.yangchengwei.easytrip.route.domain.RouteRefreshCoordinator
import com.yangchengwei.easytrip.trip.data.RoomTripRepository
import com.yangchengwei.easytrip.trip.domain.CreateTrip
import com.yangchengwei.easytrip.trip.domain.TripService
import com.yangchengwei.easytrip.trip.ui.RoomDeleteImpactProvider
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

class AppSessionCitySearchTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    /** Real reverse-geocode/POI SDK; only the device fix is supplied deterministically. */
    @Test fun dengfengSearchWithoutLocateWorksAndReenteringSearchReusesStartupLocation() =
        scenario(realSdk = true, initiallyGranted = true)

    @Test fun permissionDeniedIsNotNetworkFailureAndGrantContinuesOriginalSearch() =
        scenario(realSdk = false, initiallyGranted = false)

    private fun scenario(realSdk: Boolean, initiallyGranted: Boolean) {
        val database = Room.inMemoryDatabaseBuilder(compose.activity, EasyTripDatabase::class.java).build()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        try {
            var permission = initiallyGranted
            var permissionRequests = 0
            val fixes = AtomicInteger()
            val queries = CopyOnWriteArrayList<Pair<String, String?>>()
            val completed = CopyOnWriteArrayList<List<PlaceCandidate>>()
            val store = AmapConsentStore(object : AmapConsentPersistence {
                override fun readDecision() = true
                override fun writeDecision(accepted: Boolean) = Unit
            }, AmapPrivacyGate.create(compose.activity), ConsentRegistry())
            val fact = store.state.value.fact as AmapConsentFact.Accepted
            if (realSdk) runBlocking {
                AmapPrivacyGate.create(compose.activity).apply { reportShown(); reportDecision(true) }
            }
            val actual = if (realSdk) AmapPlaceDataSource(compose.activity, fact.token) else null
            val source = object : PlaceSearchDataSource {
                override suspend fun cityAt(point: GeoPoint) =
                    actual?.cityAt(point) ?: PlaceCity("登封市", "410185")
                override suspend fun search(keyword: String, city: String?): List<PlaceCandidate> {
                    queries += keyword to city
                    return (actual?.search(keyword, city) ?: listOf(
                        PlaceCandidate("test-dengfeng", "西施猪蹄", "", GeoPoint(34.454, 113.050), null, "登封市"),
                    )).also {
                        completed += it
                        println("AMAP_RESULT APP_SESSION_SEARCH keyword=$keyword city=$city count=${it.size}")
                    }
                }
            }
            val locationSession = AppLocationSession(scope, { permission }, {
                fixes.incrementAndGet()
                GeoPoint(34.454, 113.050)
            }, source::cityAt)
            val routes = object : RouteRefreshCoordinator {
                override fun start(scope: CoroutineScope) = Unit
                override suspend fun retry(legId: String) = false
                override suspend fun updateDetails(legId: String, selectedModeOverride: TransportMode?, durationOverrideSeconds: Int?, note: String?) = false
            }
            val runtime = AmapRuntimeSession(fact.generation, fact.token, source, routes, locationSession)
            val trips = RoomTripRepository(database.tripDao())
            val tripId = runBlocking { trips.createTrip(CreateTrip("登封搜索回归", 1)) }
            val otherTripId = runBlocking { trips.createTrip(CreateTrip("另一趟旅行", 1)) }
            compose.setContent {
                AppNavigation(
                    service = TripService(trips), repository = trips,
                    impacts = RoomDeleteImpactProvider(database.deleteImpactDao()),
                    dependencies = AppNavigationDependencies(
                        savedPlaceRepository = RoomSavedPlaceRepository(database),
                        itineraryRepository = RoomItineraryRepository(database, database.itineraryEditingDao(), database.routeLegDao()),
                        routeLegRepository = RoomRouteLegRepository(database.routeLegDao()),
                        mapPreferences = InMemoryMapPreferences(),
                        locationPermissionRequestStore = InMemoryLocationPermissionRequestStore(),
                        consentStore = store,
                        runtimeSessionFactory = { runtime },
                        stopRuntimeSession = locationSession::close,
                    ),
                    locationPermissionSnapshot = { LocationPermissionSnapshot(permission, !permission) },
                    onLaunchLocationPermission = { _, callback ->
                        permissionRequests++
                        if (permissionRequests > 1) permission = true
                        callback(LocationPermissionSnapshot(permission, !permission))
                        Result.success(Unit)
                    },
                    mapHostFactory = { context -> object : AmapMapHost {
                        override val view = View(context)
                        override fun canRenderBeforeReady() = true
                        override fun onCreate() = Unit
                        override fun onResume() = Unit
                        override fun onPause() = Unit
                        override fun onDestroy() = Unit
                        override fun showCurrentLocation() = error("Test must never click locate")
                    } },
                )
            }
            fun openTrip(id: String) {
                compose.waitUntil(5_000) { compose.onAllNodesWithTag("other-trips-list").fetchSemanticsNodes().isNotEmpty() }
                val card = hasTestTag("primary-trip-$id") or hasTestTag("other-trip-$id")
                if (compose.onAllNodes(card).fetchSemanticsNodes().isEmpty()) {
                    compose.onNodeWithTag("other-trips-list").performScrollToNode(card)
                }
                compose.onNode(card).performClick()
            }
            if (initiallyGranted) compose.waitUntil(5_000) { fixes.get() == 1 }
            else assertEquals("Startup must not request permissions or locate", 0, fixes.get())
            openTrip(tripId)
            compose.onNodeWithTag("workspace-search-launcher").performClick()
            compose.onNodeWithTag("place-search-field").performTextInput("西施猪蹄")
            if (!initiallyGranted) {
                compose.waitUntil(5_000) { compose.onAllNodesWithText("允许定位以搜索当前城市").fetchSemanticsNodes().isNotEmpty() }
                compose.onNodeWithText("允许定位以搜索当前城市").assertIsDisplayed()
                compose.onNodeWithText("允许定位").performClick()
                compose.waitForIdle()
                compose.onNodeWithText("允许定位以搜索当前城市").assertIsDisplayed()
                assertTrue(queries.isEmpty())
                assertEquals(0, fixes.get())
                compose.onNodeWithText("允许定位").performClick()
            }
            compose.waitUntil(25_000) { completed.isNotEmpty() }
            assertEquals("登封市", queries.first().second)
            assertTrue(completed.first().any { it.name.contains("西施猪蹄") && it.cityName == "登封市" })
            compose.onNodeWithTag("place-search-back").performClick()
            compose.onNodeWithTag("workspace-search-launcher").performClick()
            compose.onNodeWithTag("place-search-field").performTextInput("西施猪蹄")
            compose.waitUntil(25_000) { completed.size >= 2 }
            assertTrue(queries.all { it.second == "登封市" })
            assertEquals("Reopening search must reuse the app fix", 1, fixes.get())
            compose.onNodeWithTag("place-search-back").performClick()
            compose.onNodeWithTag("workspace-back").performClick()
            openTrip(otherTripId)
            compose.onNodeWithTag("workspace-search-launcher").performClick()
            compose.onNodeWithTag("place-search-field").performTextInput("西施猪蹄")
            compose.waitUntil(25_000) { completed.size >= 3 }
            assertEquals("登封市", queries.last().second)
            assertEquals("Changing trips must reuse the same app fix", 1, fixes.get())
        } finally {
            scope.cancel()
            database.close()
        }
    }
}
