package com.yangchengwei.easytrip.workspace

import android.view.View
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material3.Text
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.Modifier
import androidx.test.platform.app.InstrumentationRegistry
import androidx.room.Room
import com.yangchengwei.easytrip.*
import com.yangchengwei.easytrip.amap.*
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import com.yangchengwei.easytrip.itinerary.data.RoomItineraryRepository
import com.yangchengwei.easytrip.itinerary.ui.DayItineraryUiState
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.amap.PlaceSearchDataSource
import com.yangchengwei.easytrip.place.amap.AppLocationSession
import com.yangchengwei.easytrip.place.data.RoomSavedPlaceRepository
import com.yangchengwei.easytrip.place.domain.PlaceCity
import com.yangchengwei.easytrip.place.ui.PlacePoolUiState
import com.yangchengwei.easytrip.place.ui.SavedPlaceRowUi
import com.yangchengwei.easytrip.place.domain.SavedPlace
import com.yangchengwei.easytrip.route.data.RoomRouteLegRepository
import com.yangchengwei.easytrip.route.domain.RouteRefreshCoordinator
import com.yangchengwei.easytrip.core.model.TransportMode
import com.yangchengwei.easytrip.trip.data.RoomTripRepository
import com.yangchengwei.easytrip.trip.domain.CreateTrip
import com.yangchengwei.easytrip.trip.domain.TripService
import com.yangchengwei.easytrip.trip.ui.RoomDeleteImpactProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

class SearchResultsMapFlowTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val candidates = listOf(
        PlaceCandidate("a", "断桥残雪", "北山街", GeoPoint(30.258, 120.149), "0571"),
        PlaceCandidate("b", "雷峰塔", "南山路", GeoPoint(30.230, 120.148), "0571"),
        PlaceCandidate("c", "无坐标结果", "", null, null),
    )

    @Test fun submitListAndClearUseRealNavigationWithoutSavingSearchResults() {
        val database = Room.inMemoryDatabaseBuilder(compose.activity, EasyTripDatabase::class.java).build()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        try {
            val trips = RoomTripRepository(database.tripDao())
            val places = RoomSavedPlaceRepository(database)
            val tripId = runBlocking { trips.createTrip(CreateTrip("搜索地图回归", 1)) }
            val requests = CopyOnWriteArrayList<String>()
            val models = CopyOnWriteArrayList<MapUiModel>()
            val source = object : PlaceSearchDataSource {
                override suspend fun cityAt(point: GeoPoint) = PlaceCity("杭州市", "330100")
                override suspend fun search(keyword: String, city: String?): List<PlaceCandidate> {
                    requests += keyword
                    return candidates
                }
            }
            val store = AmapConsentStore(object : AmapConsentPersistence {
                override fun readDecision() = true
                override fun writeDecision(accepted: Boolean) = Unit
            }, AmapPrivacyGate.create(compose.activity), ConsentRegistry())
            val locationSession = AppLocationSession(scope, { true }, { GeoPoint(30.258, 120.149) }, source::cityAt)
            compose.setContent {
                AppNavigation(
                    service = TripService(trips), repository = trips,
                    impacts = RoomDeleteImpactProvider(database.deleteImpactDao()),
                    dependencies = AppNavigationDependencies(
                        savedPlaceRepository = places,
                        itineraryRepository = RoomItineraryRepository(database, database.itineraryEditingDao(), database.routeLegDao()),
                        routeLegRepository = RoomRouteLegRepository(database.routeLegDao()),
                        mapPreferences = InMemoryMapPreferences(),
                        locationPermissionRequestStore = com.yangchengwei.easytrip.permission.InMemoryLocationPermissionRequestStore(),
                        consentStore = store,
                        runtimeSessionFactory = { fact ->
                            AmapRuntimeSession(fact.generation, fact.token, source, object : RouteRefreshCoordinator {
                                override fun start(scope: CoroutineScope) = Unit
                                override suspend fun retry(legId: String) = false
                                override suspend fun updateDetails(
                                    legId: String,
                                    selectedModeOverride: TransportMode?,
                                    durationOverrideSeconds: Int?,
                                    note: String?,
                                ) = false
                            }, locationSession)
                        },
                        stopRuntimeSession = locationSession::close,
                    ),
                    mapHostFactory = { context -> object : AmapMapHost {
                        override val view = View(context)
                        override fun canRenderBeforeReady() = true
                        override fun onCreate() = Unit
                        override fun onResume() = Unit
                        override fun onPause() = Unit
                        override fun onDestroy() = Unit
                        override fun render(model: MapUiModel, layer: MapLayer, onMarkerClick: (String) -> Unit,
                            onMapPoiClick: (MapPoiUi) -> Unit, onLayerError: (Throwable, MapLayer) -> Unit) {
                            models += model
                        }
                    } },
                )
            }
            compose.waitUntil(5_000) {
                compose.onAllNodes(hasTestTag("primary-trip-$tripId") or hasTestTag("other-trip-$tripId")).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNode(hasTestTag("primary-trip-$tripId") or hasTestTag("other-trip-$tripId")).performClick()
            compose.onNodeWithTag("section-ITINERARY").performClick()
            compose.waitUntil(5_000) {
                compose.onAllNodesWithTag("workspace-search-launcher").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("workspace-search-launcher").performClick()
            compose.onNodeWithTag("place-search-field").performTextInput("西湖")
            compose.waitUntil(5_000) { compose.onAllNodesWithText("断桥残雪").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("place-search-submit").performClick()
            compose.onNodeWithTag("workspace-search-summary").assertIsDisplayed()
            compose.onNodeWithTag("workspace-collapsed-content").assertExists()
            compose.onNodeWithTag("section-ITINERARY").assertIsSelected()
            compose.onNodeWithText("3 个结果 · 2 个可定位").assertIsDisplayed()
            compose.waitUntil(5_000) { models.any { it.markers.any { marker -> marker.kind == MapMarkerKind.SEARCH_RESULT } } }
            assertEquals(listOf("断桥残雪", "雷峰塔"), models.last().markers.map { it.label })
            assertEquals(2, models.last().viewportRequest?.points?.size)

            compose.onNodeWithTag("workspace-search-results-list").performClick()
            compose.onNodeWithTag("place-search-field").assertTextEquals("西湖")
            compose.onNodeWithText("断桥残雪").assertIsDisplayed()
            assertEquals("List reopening should use the exact result snapshot", 1, requests.size)
            compose.onNodeWithTag("place-search-submit").performClick()
            compose.onNodeWithTag("workspace-search-results-clear").performClick()
            compose.onNodeWithTag("workspace-search-summary").assertDoesNotExist()
            compose.onNodeWithTag("section-ITINERARY").assertIsSelected()
            assertTrue(runBlocking { places.observePlaces(tripId, emptySet()).first() }.isEmpty())
        } finally {
            compose.runOnIdle { compose.activity.setContentView(View(compose.activity)) }
            scope.cancel()
            database.close()
        }
    }

    @Test fun expandingDrawerCoversSummaryWithoutMovingMapOrChangingLayout() {
        var level by mutableStateOf(WorkspaceSheetLevel.COLLAPSED)
        val layouts = mutableListOf<WorkspaceMapLayout>()
        compose.setContent { EasyTripTheme {
            TripWorkspaceContent(
                pageState = TripWorkspacePageState.Ready(
                    TripWorkspaceUiState(tripName = "杭州", sheetLevel = level,
                        searchResults = WorkspaceSearchResults("西湖", candidates)).toReadyState(),
                ),
                mapState = WorkspaceMapState.Ready,
                onAction = { if (it is TripWorkspaceAction.SetSheetLevel) level = it.level },
                placeState = PlacePoolUiState(), onPlaceAction = {},
                itineraryState = DayItineraryUiState(), onItineraryAction = {},
                mapContent = { layout -> androidx.compose.runtime.SideEffect { layouts += layout }; Text("固定地图") },
            )
        } }
        val mapBounds = compose.onNodeWithTag("workspace-map").getUnclippedBoundsInRoot()
        val summaryBounds = compose.onNodeWithTag("workspace-search-summary").getUnclippedBoundsInRoot()
        val layout = layouts.last()
        for (target in listOf(WorkspaceSheetLevel.HALF, WorkspaceSheetLevel.EXPANDED, WorkspaceSheetLevel.COLLAPSED)) {
            compose.runOnIdle { level = target }
            assertEquals(mapBounds, compose.onNodeWithTag("workspace-map").getUnclippedBoundsInRoot())
            assertEquals(summaryBounds, compose.onNodeWithTag("workspace-search-summary").getUnclippedBoundsInRoot())
            assertEquals(layout, layouts.last())
            if (target != WorkspaceSheetLevel.COLLAPSED) {
                val sheet = compose.onNodeWithTag("workspace-sheet").getUnclippedBoundsInRoot()
                assertTrue(sheet.top < summaryBounds.top && sheet.bottom >= summaryBounds.bottom)
            }
        }
    }

    @Test fun realMapCameraStaysFixedWhenDrawerCoversSearchResults() {
        com.amap.api.maps.MapsInitializer.updatePrivacyShow(compose.activity, true, true)
        com.amap.api.maps.MapsInitializer.updatePrivacyAgree(compose.activity, true)
        val gate = TestConsentGate().also { it.show() }
        val token = requireNotNull(gate.decide(true))
        val loaded = AtomicBoolean(false)
        val cameras = CopyOnWriteArrayList<MapCameraState>()
        var level by mutableStateOf(WorkspaceSheetLevel.COLLAPSED)
        val results = WorkspaceSearchResults("西湖", candidates)
        val markers = results.mappedPlaces.map {
            MapMarkerUi("result-${it.poiId}", requireNotNull(it.point), it.name, emptyList(), MapMarkerKind.SEARCH_RESULT)
        }
        val model = MapUiModel(markers = markers, viewportRequest = MapViewportRequest(
            1, ViewportReason.SEARCH_RESULTS, markers.map { it.point },
        ))
        val places = results.mappedPlaces.map {
            SavedPlaceRowUi(SavedPlace(it.poiId, "visual", it.poiId, it.name, it.address, requireNotNull(it.point), "", emptyList()), 0, false)
        }
        compose.setContent { EasyTripTheme {
            TripWorkspaceContent(
                pageState = TripWorkspacePageState.Ready(TripWorkspaceUiState(
                    tripName = "杭州 · 春日慢游", sheetLevel = level, map = model, searchResults = results,
                ).toReadyState()),
                mapState = WorkspaceMapState.Ready,
                onAction = { if (it is TripWorkspaceAction.SetSheetLevel) level = it.level },
                placeState = PlacePoolUiState(rows = places, savedPoiIds = places.map { it.place.amapPoiId }.toSet()), onPlaceAction = {},
                itineraryState = DayItineraryUiState(), onItineraryAction = {},
                mapContent = { layout -> AmapComposeMap(
                    model = model.copy(viewportRequest = model.viewportRequest?.copy(safeInsets = layout.fitInsets)),
                    consent = token, onMarkerClick = {}, onMapReady = { loaded.set(true) },
                    onCameraChanged = cameras::add,
                    visibleInsets = layout.visibleInsets, modifier = Modifier.fillMaxSize(),
                ) },
            )
        } }
        compose.waitUntil(30_000) { loaded.get() && cameras.isNotEmpty() }
        android.os.SystemClock.sleep(1_500)
        val camera = cameras.last()
        val bounds = compose.onNodeWithTag("workspace-search-summary").getUnclippedBoundsInRoot()
        for (target in listOf(WorkspaceSheetLevel.COLLAPSED, WorkspaceSheetLevel.HALF, WorkspaceSheetLevel.EXPANDED)) {
            compose.runOnIdle { level = target }
            compose.waitForIdle()
            android.os.SystemClock.sleep(750)
            assertEquals(camera, cameras.last())
            assertEquals(bounds, compose.onNodeWithTag("workspace-search-summary").getUnclippedBoundsInRoot())
            val bitmap = checkNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
            java.io.File(compose.activity.getExternalFilesDir(null), "search-map-${target.name.lowercase()}.png")
                .outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
