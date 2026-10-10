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

class ExistingEmptyTripCityNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private data class CityCase(
        val title: String,
        val latitudes: ClosedFloatingPointRange<Double>,
        val longitudes: ClosedFloatingPointRange<Double>,
    )
    private val zibo = CityCase("淄博", 35.0..38.0, 116.0..120.0)
    private val xuzhou = CityCase("徐州", 32.0..36.0, 115.0..120.0)
    private val anyang = CityCase("安阳", 34.0..38.0, 112.0..116.0)

    @Test fun ziboOpenedFromTripListFitsItsCity() = openExistingTrips(listOf(zibo))
    @Test fun xuzhouOpenedFromTripListFitsItsCity() = openExistingTrips(listOf(xuzhou))
    @Test fun anyangOpenedFromTripListFitsItsCity() = openExistingTrips(listOf(anyang))

    @Test fun switchingAndReopeningThreeEmptyTripsMovesRealMapToEachCity() =
        openExistingTrips(listOf(zibo, xuzhou, anyang, zibo), realMap = true)

    @Test fun existingTripWithSavedPlaceKeepsSavedPlaceViewport() =
        openExistingTrips(listOf(zibo), withSavedPlace = true)

    private fun openExistingTrips(
        visits: List<CityCase>,
        realMap: Boolean = false,
        withSavedPlace: Boolean = false,
    ) {
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
        val savedPoint = com.yangchengwei.easytrip.core.model.GeoPoint(39.916, 116.397)
        val tripIds = runBlocking {
            visits.distinctBy { it.title }.associate { city ->
                val id = trips.createTrip(com.yangchengwei.easytrip.trip.domain.CreateTrip(
                    name = city.title, startDate = null, dayCount = 1,
                ))
                if (withSavedPlace) places.save(id, com.yangchengwei.easytrip.place.amap.PlaceCandidate(
                    "saved-beijing", "故宫", "", savedPoint, "010", "北京市", "110000", 1,
                ))
                city.title to id
            }
        }
        var activeHost: AmapMapHost? = null
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
                    mapHostFactory = { context ->
                        val delegate = if (realMap) RealAmapMapHost.create(context) else object : AmapMapHost {
                            override val view = View(context)
                            override fun canRenderBeforeReady() = true
                            override fun onCreate() = Unit
                            override fun onResume() = Unit
                            override fun onPause() = Unit
                            override fun onDestroy() = Unit
                        }
                        object : AmapMapHost by delegate {
                            override fun render(
                                model: MapUiModel, layer: MapLayer,
                                onMarkerClick: (String) -> Unit, onMapPoiClick: (MapPoiUi) -> Unit,
                                onLayerError: (Throwable, MapLayer) -> Unit,
                            ) {
                                delegate.render(model, layer, onMarkerClick, onMapPoiClick, onLayerError)
                                model.viewportRequest?.let(requests::add)
                            }
                        }.also { activeHost = it }
                    },
                )
            }
        }
        visits.forEach { city ->
            val tripId = requireNotNull(tripIds[city.title])
            compose.waitUntil(5000) { compose.onAllNodesWithTag("other-trips-list").fetchSemanticsNodes().isNotEmpty() }
            val card = hasTestTag("primary-trip-$tripId") or hasTestTag("other-trip-$tripId")
            compose.onNodeWithTag("other-trips-list").performScrollToNode(card)
            compose.runOnIdle { requests.clear(); activeHost = null }
            compose.onNode(card).performClick()
            compose.waitUntil(5000) { compose.onAllNodesWithTag("workspace-search-launcher").fetchSemanticsNodes().isNotEmpty() }
            val expectedReason = if (withSavedPlace) ViewportReason.INITIAL else ViewportReason.INITIAL_CITY
            compose.waitUntil(20_000) { requests.any { it.reason == expectedReason } }
            val initial = requests.first { it.reason == expectedReason }
            if (withSavedPlace) {
                assertEquals(listOf(savedPoint), initial.points)
                assertTrue(requests.none { it.reason == ViewportReason.INITIAL_CITY })
            } else {
                assertEquals(2, initial.points.size)
                assertTrue("${city.title} must fit its city, not the default map", initial.points.all {
                    it.latitude in city.latitudes && it.longitude in city.longitudes
                })
                assertTrue(runBlocking { places.observePlaces(tripId, emptySet()).first() }.isEmpty())
                if (realMap) {
                    var camera: MapCameraState? = null
                    compose.waitUntil(10_000) {
                        compose.runOnUiThread { camera = activeHost?.cameraSnapshot() }
                        camera?.let { it.target.latitude in city.latitudes && it.target.longitude in city.longitudes && it.zoom in 5f..13f } == true
                    }
                    println("AMAP_RESULT EXISTING_EMPTY_TRIP title=${city.title} camera=$camera bounds=${initial.points}")
                }
            }
            compose.onNodeWithContentDescription("返回").performClick()
        }
        compose.waitForIdle()
        db.close()
    }
}
