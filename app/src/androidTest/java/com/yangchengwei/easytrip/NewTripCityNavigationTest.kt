package com.yangchengwei.easytrip

import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.room.Room
import com.yangchengwei.easytrip.amap.*
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import com.yangchengwei.easytrip.itinerary.data.RoomItineraryRepository
import com.yangchengwei.easytrip.permission.InMemoryLocationPermissionRequestStore
import com.yangchengwei.easytrip.place.data.RoomSavedPlaceRepository
import com.yangchengwei.easytrip.route.data.RoomRouteLegRepository
import com.yangchengwei.easytrip.trip.data.RoomTripRepository
import com.yangchengwei.easytrip.trip.domain.TripService
import com.yangchengwei.easytrip.trip.ui.RoomDeleteImpactProvider
import com.yangchengwei.easytrip.workspace.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList

class NewTripCityNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun creatingAndReopeningEmptyTripBothFitItsCity() {
        val consentStore = AmapConsentStore(
            persistence = object : AmapConsentPersistence {
                override fun readDecision(): Boolean? = null
                override fun writeDecision(accepted: Boolean) = Unit
            },
            reporter = AmapPrivacyGate.create(compose.activity),
            registry = ConsentRegistry(),
        )
        runBlocking {
            consentStore.reportShown().getOrThrow()
            consentStore.decide(true).getOrThrow()
        }
        val token = (consentStore.state.value.fact as AmapConsentFact.Accepted).token
        val db = Room.inMemoryDatabaseBuilder(compose.activity, EasyTripDatabase::class.java).build()
        val trips = RoomTripRepository(db.tripDao())
        val places = RoomSavedPlaceRepository(db)
        val requests = CopyOnWriteArrayList<MapViewportRequest>()
        compose.setContent {
            EasyTripTheme {
                AppNavigation(
                    service = TripService(trips), repository = trips,
                    impacts = RoomDeleteImpactProvider(db.deleteImpactDao()),
                    dependencies = AppNavigationDependencies(
                        savedPlaceRepository = places,
                        itineraryRepository = RoomItineraryRepository(db, db.itineraryEditingDao(), db.routeLegDao()),
                        routeLegRepository = RoomRouteLegRepository(db.routeLegDao()),
                        mapPreferences = InMemoryMapPreferences(),
                        locationPermissionRequestStore = InMemoryLocationPermissionRequestStore(),
                        mapConsentToken = token,
                        consentStore = consentStore,
                        runtimeSessionFactory = { fact ->
                            AmapRuntimeSession(
                                fact.generation, fact.token,
                                object : com.yangchengwei.easytrip.place.amap.PlaceSearchDataSource {
                                    override suspend fun search(keyword: String, city: String?) =
                                        emptyList<com.yangchengwei.easytrip.place.amap.PlaceCandidate>()
                                },
                                object : com.yangchengwei.easytrip.route.domain.RouteRefreshCoordinator {
                                    override fun start(scope: kotlinx.coroutines.CoroutineScope) = Unit
                                    override suspend fun retry(legId: String) = false
                                    override suspend fun updateDetails(
                                        legId: String,
                                        selectedModeOverride: com.yangchengwei.easytrip.core.model.TransportMode?,
                                        durationOverrideSeconds: Int?,
                                        note: String?,
                                    ) = false
                                },
                            )
                        },
                    ),
                    mapHostFactory = { context -> object : AmapMapHost {
                        override val view = View(context)
                        override fun canRenderBeforeReady() = true
                        override fun onCreate() = Unit
                        override fun onResume() = Unit
                        override fun onPause() = Unit
                        override fun onDestroy() = Unit
                        override fun render(model: MapUiModel, layer: MapLayer, onMarkerClick: (String) -> Unit, onLayerError: (Throwable, MapLayer) -> Unit) {
                            model.viewportRequest?.let(requests::add)
                        }
                    } },
                )
            }
        }
        compose.waitUntil(5000) { compose.onAllNodesWithTag("create-trip").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("create-trip").performClick()
        compose.onNodeWithTag("create-name").performTextInput("杭州·苏州五日游")
        compose.onNodeWithTag("create-submit").performClick()
        compose.waitUntil(20_000) { requests.any { it.reason == ViewportReason.INITIAL_CITY } }
        val initial = requests.first { it.reason == ViewportReason.INITIAL_CITY }
        assertEquals(2, initial.points.size)
        assertTrue(initial.points.all { it.latitude in 28.0..32.0 && it.longitude in 118.0..122.0 })
        val trip = runBlocking { trips.observeTrips().first().single() }
        assertTrue(runBlocking { places.observePlaces(trip.id, emptySet()).first() }.isEmpty())
        compose.onNodeWithContentDescription("返回").performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithTag("primary-trip-${trip.id}").fetchSemanticsNodes().isNotEmpty() }
        requests.clear()
        compose.onNodeWithTag("primary-trip-${trip.id}").performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithTag("workspace-search-launcher").fetchSemanticsNodes().isNotEmpty() }
        compose.waitUntil(20_000) { requests.any { it.reason == ViewportReason.INITIAL_CITY } }
        assertEquals(initial.points, requests.first { it.reason == ViewportReason.INITIAL_CITY }.points)
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        db.close()
    }
}
