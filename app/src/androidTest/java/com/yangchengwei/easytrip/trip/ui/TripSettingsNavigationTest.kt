package com.yangchengwei.easytrip.trip.ui

import android.content.Context
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.NoActivityResumedException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.yangchengwei.easytrip.AppNavigation
import com.yangchengwei.easytrip.AppNavigationDependencies
import com.yangchengwei.easytrip.AppNavigationObserver
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.itinerary.data.RoomItineraryRepository
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.permission.InMemoryLocationPermissionRequestStore
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.data.RoomSavedPlaceRepository
import com.yangchengwei.easytrip.place.domain.SavePlaceResult
import com.yangchengwei.easytrip.route.data.RoomRouteLegRepository
import com.yangchengwei.easytrip.trip.data.RoomTripRepository
import com.yangchengwei.easytrip.trip.domain.CreateTrip
import com.yangchengwei.easytrip.trip.domain.DateRangeApply
import com.yangchengwei.easytrip.trip.domain.DateRangeDeletionCounts
import com.yangchengwei.easytrip.trip.domain.DayDeletion
import com.yangchengwei.easytrip.trip.domain.InsertSide
import com.yangchengwei.easytrip.trip.domain.TripDay
import com.yangchengwei.easytrip.trip.domain.TripRepository
import com.yangchengwei.easytrip.trip.domain.TripService
import com.yangchengwei.easytrip.trip.domain.TripSummary
import com.yangchengwei.easytrip.trip.domain.TripWithDays
import com.yangchengwei.easytrip.workspace.AmapMapHost
import com.yangchengwei.easytrip.workspace.InMemoryMapPreferences
import com.yangchengwei.easytrip.workspace.MapLayer
import com.yangchengwei.easytrip.workspace.MapPoiUi
import com.yangchengwei.easytrip.workspace.MapUiModel
import java.time.LocalDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class TripSettingsNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private lateinit var database: EasyTripDatabase
    private lateinit var repository: RoomTripRepository

    @Before fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, EasyTripDatabase::class.java).build()
        var nextId = 0
        repository = RoomTripRepository(database.tripDao(), idFactory = { "settings-nav-${nextId++}" })
    }

    @After fun tearDown() = database.close()

    @Test fun workspaceMoreOpensCurrentTripSettingsAndBackReturnsToSameWorkspace() {
        val tripId = createTrip()
        val trip = checkNotNull(runBlocking { repository.observeTrip(tripId).first() })
        val places = RoomSavedPlaceRepository(database, idFactory = { "saved-place" })
        val itineraries = RoomItineraryRepository(database, database.itineraryEditingDao(), database.routeLegDao())
        val savedPlaceId = runBlocking {
            (places.save(
                tripId,
                PlaceCandidate("room-poi", "Room 博物馆", "真实地址", GeoPoint(30.2, 120.1), null),
            ) as SavePlaceResult.Saved).id
        }
        runBlocking { itineraries.addItem(trip.days.first().id, savedPlaceId, 0) }
        val routes = mutableListOf<String>()
        setNavigation(routes, places, itineraries)

        compose.onNodeWithTag("continue-trip-$tripId").performClick()
        compose.onNodeWithTag("section-ITINERARY").performClick().assertIsSelected()
        compose.onNodeWithText("Room 博物馆").assertIsDisplayed()
        compose.onNodeWithTag("workspace-sheet-handle").performTouchInput { swipeUp() }
        val expandedSheetTop = compose.onNodeWithTag("workspace-sheet").getUnclippedBoundsInRoot().top
        compose.onNodeWithTag("workspace-more").performClick()
        compose.onNodeWithTag("settings-date-row").assertIsDisplayed()
        compose.runOnIdle { assertEquals("trips/$tripId/settings", routes.last()) }

        compose.onNodeWithTag("settings-back").performClick()
        compose.onNodeWithTag("workspace-top-bar").assertIsDisplayed()
        compose.onNodeWithTag("section-ITINERARY").assertIsSelected()
        compose.onNodeWithText("Room 博物馆").assertIsDisplayed()
        assertEquals(expandedSheetTop, compose.onNodeWithTag("workspace-sheet").getUnclippedBoundsInRoot().top)
        assertNotNull(runBlocking { repository.observeTrip(tripId).first() })
        assertEquals("Room 博物馆", runBlocking { itineraries.observeDay(trip.days.first().id).first() }.items.single().place.name)
    }

    @Test fun growthApplyingAndAwaitingRoomConsumeSystemBackUntilCompletion() {
        val repository = BackLockRepository()
        val viewModel = TripSettingsViewModel(
            androidx.lifecycle.SavedStateHandle(mapOf("tripId" to "trip")),
            TripService(repository),
            repository,
            object : DeleteImpactProvider {
                override suspend fun trip(tripId: String) = TripDeleteImpact(0, 0, 0, 0, 0)
                override suspend fun day(dayId: String) = DayDeleteImpact(0, 0, 0)
            },
            com.yangchengwei.easytrip.trip.domain.TripDateRangeService(repository),
        )
        var settings by mutableStateOf(true)
        compose.setContent {
            BackHandler(enabled = settings) { settings = false }
            if (settings) {
                TripSettingsRoute(viewModel, onBack = { settings = false })
            } else {
                Text("workspace", Modifier.testTag("test-workspace"))
            }
        }
        compose.waitUntil { viewModel.state.value.days.size == 3 }
        compose.runOnIdle {
            viewModel.updateDateEndDraft(LocalDate.parse("2026-10-04"))
            viewModel.requestDateRangeChange()
        }
        compose.waitUntil { viewModel.state.value.dateRange.phase is DateRangeChangePhase.Applying }

        pressBack()
        compose.onNodeWithTag("settings-date-row").assertIsNotEnabled()
        compose.onNodeWithText("修改出行日期").assertDoesNotExist()

        repository.applyBlock.complete(Unit)
        compose.waitUntil { viewModel.state.value.dateRange.phase is DateRangeChangePhase.AwaitingRoom }
        pressBack()
        compose.onNodeWithTag("settings-date-row").assertIsNotEnabled()
        compose.onNodeWithText("修改出行日期").assertDoesNotExist()

        repository.emitDayCount(4)
        compose.waitUntil { !viewModel.state.value.dateRange.submitting }
        pressBack()
        compose.onNodeWithTag("test-workspace").assertIsDisplayed()
    }

    @Test fun deletedTripWhileSettingsOpenReturnsToTripList() {
        val tripId = createTrip()
        val routes = mutableListOf<String>()
        setNavigation(routes)
        compose.onNodeWithTag("continue-trip-$tripId").performClick()
        compose.onNodeWithTag("workspace-more").performClick()
        compose.onNodeWithTag("settings-date-row").assertIsDisplayed()

        runBlocking { repository.deleteTrip(tripId) }

        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("empty-trips").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("empty-trips").assertIsDisplayed()
        val routesAfterDeletion = routes.toList()
        assertThrows(NoActivityResumedException::class.java) { pressBack() }
        assertEquals(routesAfterDeletion, routes)
    }

    private fun createTrip() = runBlocking {
        repository.createTrip(
            CreateTrip(
                name = "真实设置导航",
                dayCount = 3,
                startDate = LocalDate.parse("2026-10-01"),
            ),
        )
    }

    private fun setNavigation(
        routes: MutableList<String>,
        places: RoomSavedPlaceRepository = RoomSavedPlaceRepository(database),
        itineraries: RoomItineraryRepository = RoomItineraryRepository(
            database,
            database.itineraryEditingDao(),
            database.routeLegDao(),
        ),
    ) {
        compose.setContent {
            AppNavigation(
                service = TripService(repository),
                repository = repository,
                impacts = RoomDeleteImpactProvider(database.deleteImpactDao()),
                dependencies = AppNavigationDependencies(
                    savedPlaceRepository = places,
                    itineraryRepository = itineraries,
                    routeLegRepository = RoomRouteLegRepository(database.routeLegDao()),
                    mapPreferences = InMemoryMapPreferences(),
                    locationPermissionRequestStore = InMemoryLocationPermissionRequestStore(),
                ),
                navigationObserver = AppNavigationObserver(routes::add),
                mapHostFactory = ::TestMapHost,
            )
        }
        val tripId = runBlocking { repository.observeTrips().first().single().id }
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("continue-trip-$tripId").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private class BackLockRepository : TripRepository {
        private val trip = MutableStateFlow<TripWithDays?>(
            TripWithDays(
                id = "trip",
                name = "Back lock",
                startDate = LocalDate.parse("2026-10-01"),
                travelMode = com.yangchengwei.easytrip.core.model.TravelMode.FLEXIBLE,
                days = List(3) { TripDay("day-${it + 1}", it) },
            ),
        )
        val applyBlock = CompletableDeferred<Unit>()

        fun emitDayCount(dayCount: Int) {
            val current = checkNotNull(trip.value)
            trip.value = current.copy(
                days = current.days + List(dayCount - current.days.size) {
                    TripDay("added-${it + 1}", current.days.size + it)
                },
            )
        }

        override fun observeTrips(): Flow<List<TripSummary>> = MutableStateFlow(emptyList())
        override fun observeTrip(tripId: String): Flow<TripWithDays?> = trip
        override suspend fun createTrip(command: CreateTrip) = "trip"
        override suspend fun renameTrip(tripId: String, name: String) = Unit
        override suspend fun setStartDate(tripId: String, startDate: LocalDate?) = Unit
        override suspend fun dateRangeDeletionCounts(tripId: String, dayIds: List<String>) =
            DateRangeDeletionCounts(0, 0, 0)
        override suspend fun applyDateRange(command: DateRangeApply) {
            applyBlock.await()
        }
        override suspend fun setTravelMode(
            tripId: String,
            mode: com.yangchengwei.easytrip.core.model.TravelMode,
        ) = Unit
        override suspend fun insertDay(tripId: String, anchorDayId: String?, side: InsertSide) = "day"
        override suspend fun moveDay(tripId: String, dayId: String, targetIndex: Int) = Unit
        override suspend fun deleteDay(command: DayDeletion) = Unit
        override suspend fun deleteTrip(tripId: String) = Unit
    }

    private class TestMapHost(context: Context) : AmapMapHost {
        override val view = View(context)
        override fun canRenderBeforeReady() = true
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
        ) = Unit
    }
}
