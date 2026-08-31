package com.yangchengwei.easytrip

import android.content.Context
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import com.yangchengwei.easytrip.amap.TestConsentGate
import com.yangchengwei.easytrip.permission.InMemoryLocationPermissionRequestStore
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.itinerary.data.RoomItineraryRepository
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.amap.PlaceSearchDataSource
import com.yangchengwei.easytrip.place.data.RoomSavedPlaceRepository
import com.yangchengwei.easytrip.route.data.RoomRouteLegRepository
import com.yangchengwei.easytrip.trip.data.RoomTripRepository
import com.yangchengwei.easytrip.trip.domain.CreateTrip
import com.yangchengwei.easytrip.workspace.AmapMapHost
import com.yangchengwei.easytrip.workspace.MapLayer
import com.yangchengwei.easytrip.workspace.MapPoiUi
import com.yangchengwei.easytrip.workspace.MapUiModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference

class V2AcceptanceTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var database: EasyTripDatabase
    private lateinit var observationScope: CoroutineScope
    private var nextId = 0

    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder(compose.activity, EasyTripDatabase::class.java).build()
        observationScope = CoroutineScope(Dispatchers.Default)
    }

    @After fun tearDown() {
        observationScope.cancel()
        database.close()
    }

    @Test fun searchCollectionAndMapFlowUsesProductionNavigationState() {
        val trips = RoomTripRepository(database.tripDao(), idFactory = { "id-${nextId++}" })
        val places = RoomSavedPlaceRepository(database, idFactory = { "id-${nextId++}" })
        val itineraries = RoomItineraryRepository(database, database.itineraryEditingDao(), database.routeLegDao(), itemIdFactory = { "item-${nextId++}" }, legIdFactory = { "leg-${nextId++}" })
        val routes = RoomRouteLegRepository(database.routeLegDao())
        val tripId = runBlocking { trips.createTrip(CreateTrip("集成验收", 2)) }
        val savedPoiIds = places.observeSavedPoiIds(tripId).stateIn(
            observationScope,
            SharingStarted.Eagerly,
            emptySet(),
        )
        val museum = candidate("museum", "博物馆", 39.91, 116.41)
        val park = candidate("park", "公园", 39.92, 116.42)
        val source = object : PlaceSearchDataSource {
            override suspend fun search(keyword: String, city: String?) = listOf(museum, park)
        }
        val consentStore = com.yangchengwei.easytrip.amap.AmapConsentStore(
            persistence = object : com.yangchengwei.easytrip.amap.AmapConsentPersistence {
                override fun readDecision(): Boolean? = null
                override fun writeDecision(accepted: Boolean) = Unit
            },
            reporter = object : com.yangchengwei.easytrip.amap.AmapPrivacyReporter {
                override suspend fun reportShown() = Unit
                override suspend fun reportDecision(accepted: Boolean) = Unit
            },
            registry = com.yangchengwei.easytrip.amap.ConsentRegistry(),
        )
        runBlocking {
            consentStore.reportShown().getOrThrow()
            consentStore.decide(true).getOrThrow()
        }
        val consent = (consentStore.state.value.fact as com.yangchengwei.easytrip.amap.AmapConsentFact.Accepted).token
        val navigationRoutes = java.util.concurrent.CopyOnWriteArrayList<String>()
        val host = AtomicReference<RecordingHost?>()
        fun waitFor(stage: String, condition: () -> Boolean) {
            try {
                compose.waitUntil(10_000, condition)
            } catch (failure: Throwable) {
                throw AssertionError("$stage failed; routes=$navigationRoutes saved=${savedPoiIds.value}", failure)
            }
        }
        fun hasTag(tag: String) = compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

        compose.setContent {
            AppNavigation(
                service = com.yangchengwei.easytrip.trip.domain.TripService(trips),
                repository = trips,
                impacts = com.yangchengwei.easytrip.trip.ui.RoomDeleteImpactProvider(database.deleteImpactDao()),
                dependencies = AppNavigationDependencies(
                    savedPlaceRepository = places,
                    itineraryRepository = itineraries,
                    routeLegRepository = routes,
                    mapPreferences = com.yangchengwei.easytrip.workspace.InMemoryMapPreferences(),
                    locationPermissionRequestStore = InMemoryLocationPermissionRequestStore(),
                    consentStore = consentStore,
                    runtimeSessionFactory = { fact ->
                        com.yangchengwei.easytrip.AmapRuntimeSession(
                            fact.generation,
                            fact.token,
                            source,
                            object : com.yangchengwei.easytrip.route.domain.RouteRefreshCoordinator {
                                override fun start(scope: CoroutineScope) = Unit
                                override suspend fun retry(legId: String) = false
                                override suspend fun overrideMode(legId: String, mode: com.yangchengwei.easytrip.core.model.TransportMode) = false
                            },
                        )
                    },
                    mapConsentToken = consent,
                ),
                navigationObserver = AppNavigationObserver(navigationRoutes::add),
                mapHostFactory = { RecordingHost(it).also(host::set) },
            )
        }

        waitFor("trip list entry") { hasTag("continue-trip-$tripId") }
        compose.onNodeWithTag("continue-trip-$tripId").performClick()
        waitFor("workspace UI") { hasTag("workspace-top-bar") }
        compose.onNodeWithTag("workspace-top-bar").assertIsDisplayed()
        compose.onNodeWithTag("layer-menu").assertHasClickAction()
        compose.onNodeWithTag("section-PLACE_POOL").assertIsSelected()
        compose.onNodeWithTag("section-ITINERARY").assertExists()
        compose.onNodeWithTag("itinerary-scope-rail").assertDoesNotExist()

        compose.onNodeWithTag("workspace-search-launcher").assertHasClickAction().performClick()
        waitFor("place search UI") { hasTag("place-search-field") }
        compose.onNodeWithTag("place-search-field").assertIsDisplayed().performTextInput("博物馆")
        waitFor("place search results") {
            hasTag("place-search-bookmark-touch-museum") && hasTag("place-search-bookmark-touch-park")
        }
        compose.onAllNodesWithText("加入行程").assertCountEquals(0)

        compose.onNodeWithTag("place-search-bookmark-touch-museum").performClick()
        waitFor("museum collection") { "museum" in savedPoiIds.value }
        compose.onNodeWithContentDescription("取消收藏博物馆").assertIsDisplayed()
        compose.onNodeWithTag("place-search-bookmark-touch-park").performClick()
        waitFor("park collection") { "park" in savedPoiIds.value }
        compose.onNodeWithContentDescription("取消收藏公园").assertIsDisplayed()
        compose.onNodeWithTag("place-search-back").performClick()
        waitFor("workspace UI after search return") { hasTag("workspace-top-bar") }
        waitFor("map host initialization") { host.get() != null }
        val mapHost = requireNotNull(host.get())

        compose.runOnIdle { mapHost.emit(MapPoiUi("poi-card", "故宫", "北京市东城区", GeoPoint(39.916, 116.397))) }
        compose.onNodeWithText("故宫").assertIsDisplayed()
        assertFalse("poi-card" in savedPoiIds.value)
        compose.onNodeWithTag("place-card-collection").performClick()
        waitFor("map POI collection") { "poi-card" in savedPoiIds.value }

        compose.onNodeWithTag("section-ITINERARY").performClick()
        compose.onNodeWithTag("section-ITINERARY").assertIsSelected()
        waitFor("itinerary all-empty state") { hasTag("itinerary-all-empty") }
        compose.onNodeWithTag("itinerary-all-empty").assertIsDisplayed()
        compose.onNodeWithTag("itinerary-scope-rail").assertDoesNotExist()
    }

    private fun candidate(id: String, name: String, latitude: Double, longitude: Double) =
        PlaceCandidate(id, name, "地址", GeoPoint(latitude, longitude), "010")

    private class RecordingHost(context: Context) : AmapMapHost {
        override val view = View(context)
        private var poiCallback: (MapPoiUi) -> Unit = {}
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
            poiCallback = onMapPoiClick
        }
        fun emit(poi: MapPoiUi) = poiCallback(poi)
    }
}
