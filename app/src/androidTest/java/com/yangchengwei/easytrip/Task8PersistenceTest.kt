package com.yangchengwei.easytrip

import android.content.Context
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yangchengwei.easytrip.amap.SharedPreferencesAmapConsentPersistence
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.core.model.TimeMode
import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.itinerary.data.ItineraryItemEntity
import com.yangchengwei.easytrip.itinerary.data.RoomItineraryRepository
import com.yangchengwei.easytrip.itinerary.domain.ItineraryRepository
import com.yangchengwei.easytrip.itinerary.ui.DayItineraryViewModel
import com.yangchengwei.easytrip.permission.LocationPermissionCoordinator
import com.yangchengwei.easytrip.permission.LocationPermissionPrompt
import com.yangchengwei.easytrip.permission.LocationPermissionSnapshot
import com.yangchengwei.easytrip.permission.SharedPreferencesLocationPermissionRequestStore
import com.yangchengwei.easytrip.permission.WorkspaceEffect
import com.yangchengwei.easytrip.place.data.SavedPlaceEntity
import com.yangchengwei.easytrip.route.data.RoomRouteLegRepository
import com.yangchengwei.easytrip.trip.data.RoomTripRepository
import com.yangchengwei.easytrip.trip.data.TripDayEntity
import com.yangchengwei.easytrip.trip.data.TripEntity
import java.time.Instant
import java.time.LocalTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class Task8PersistenceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val preferencesName = "task8-persistence-${javaClass.name}"
    private val databaseName = "task8-volatile-state.db"
    private val dispatcher = StandardTestDispatcher()
    private lateinit var database: EasyTripDatabase

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE).edit().clear().commit()
        context.deleteDatabase(databaseName)
        database = Room.databaseBuilder(context, EasyTripDatabase::class.java, databaseName).build()
    }

    @After fun tearDown() {
        database.close()
        context.deleteDatabase(databaseName)
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE).edit().clear().commit()
        Dispatchers.resetMain()
    }

    @Test fun amapConsentDecisionPersistsAcrossFreshConcreteWrapperInstances() {
        fun fresh() = SharedPreferencesAmapConsentPersistence(
            context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE),
        )

        assertNull(fresh().readDecision())
        fresh().writeDecision(true)
        assertEquals(true, fresh().readDecision())
        fresh().writeDecision(false)
        assertEquals(false, fresh().readDecision())
    }

    @Test fun locationPermissionHasRequestedPersistsAcrossFreshConcreteStoreInstances() {
        fun fresh() = SharedPreferencesLocationPermissionRequestStore(
            context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE),
        )

        assertFalse(fresh().hasRequested)
        fresh().hasRequested = true
        assertTrue(fresh().hasRequested)
        fresh().hasRequested = false
        assertFalse(fresh().hasRequested)
    }

    @Test fun newLocationCoordinatorRestoresOnlyRequestedFactWithoutPromptGenerationOrEffect() = runTest(dispatcher) {
        val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        val first = LocationPermissionCoordinator(SharedPreferencesLocationPermissionRequestStore(preferences))
        first.attachWorkspace("trip")
        first.onLocateClick(LocationPermissionSnapshot(false, false))
        first.confirmExplanation()
        val generation = (first.effectFlow.first() as WorkspaceEffect.RequestLocationPermission).generation
        first.onPermissionLaunchStarted(generation)
        first.onPermissionResult(generation, LocationPermissionSnapshot(false, false))
        assertEquals(LocationPermissionPrompt.SETTINGS, first.uiState.value.prompt)
        first.requestApplicationSettings()
        assertTrue(first.uiState.value.busy)
        val settingsGeneration = (first.effectFlow.first() as WorkspaceEffect.OpenApplicationSettings).generation
        assertTrue(settingsGeneration > generation)
        first.onSettingsLaunchStarted(settingsGeneration)
        assertEquals(LocationPermissionPrompt.SETTINGS, first.uiState.value.prompt)
        assertTrue(first.uiState.value.busy)

        val second = LocationPermissionCoordinator(
            SharedPreferencesLocationPermissionRequestStore(
                context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE),
            ),
        )
        second.attachWorkspace("trip")
        assertEquals(LocationPermissionPrompt.NONE, second.uiState.value.prompt)
        assertFalse(second.uiState.value.busy)
        assertNull(second.uiState.value.error)
        assertNull(withTimeoutOrNull(1) { second.effectFlow.first() })
        second.onWorkspaceResumed("trip", LocationPermissionSnapshot(true, false))
        assertEquals(LocationPermissionPrompt.NONE, second.uiState.value.prompt)
        assertNull(withTimeoutOrNull(1) { second.effectFlow.first() })

        second.onLocateClick(LocationPermissionSnapshot(false, false))
        assertEquals(LocationPermissionPrompt.SETTINGS, second.uiState.value.prompt)
        assertTrue(second.uiState.value.permanentlyDenied)
    }

    @Test fun newViewModelDropsUncommittedDraftAndSaveErrorThenReopensPersistedRoomValues() = runTest(dispatcher) {
        seedItinerary()
        val trips = RoomTripRepository(database.tripDao())
        val roomItineraries = RoomItineraryRepository(database, database.itineraryEditingDao(), database.routeLegDao())
        val failingItineraries = object : ItineraryRepository by roomItineraries {
            override suspend fun updateDetails(itemId: String, arrivalTime: LocalTime?, stayMinutes: Int?, note: String?) {
                throw IllegalStateException("保存失败")
            }
        }
        val routes = RoomRouteLegRepository(database.routeLegDao())
        val firstStore = ViewModelStore()
        try {
            val first = DayItineraryViewModel(
                "trip", trips, failingItineraries, routes, null, selectedDays = flowOf("day"),
            )
            firstStore.put("day", first)
            first.state.first { state -> state.items.any { it.id == "item" } }
            assertTrue(first.requestTiming("item"))
            first.updateArrivalTime("09:45")
            first.updateStayMinutes("90")
            first.updateNote("未提交草稿")
            first.saveTiming()
            advanceUntilIdle()
            assertEquals("保存失败", first.state.value.editDraft?.saveError)
            assertEquals("09:45", first.state.value.editDraft?.arrivalTimeText)
        } finally {
            firstStore.clear()
            advanceUntilIdle()
        }

        val secondStore = ViewModelStore()
        try {
            val second = DayItineraryViewModel(
                "trip", trips, roomItineraries, routes, null, selectedDays = flowOf("day"),
            )
            secondStore.put("day", second)
            second.state.first { state -> state.items.any { it.id == "item" } }
            assertNull(second.state.value.editDraft)
            val persisted = roomItineraries.observeDay("day").first().items.single()
            assertEquals(LocalTime.of(8, 0), persisted.arrivalTime)
            assertEquals(60, persisted.stayMinutes)
            assertEquals("已提交", persisted.note)
            assertTrue(second.requestTiming("item"))
            assertEquals("08:00", second.state.value.editDraft?.arrivalTimeText)
            assertEquals("60", second.state.value.editDraft?.stayMinutesText)
            assertEquals("已提交", second.state.value.editDraft?.noteText)
        } finally {
            secondStore.clear()
            advanceUntilIdle()
        }
    }

    private suspend fun seedItinerary() {
        val now = Instant.EPOCH
        database.tripDao().insertTrip(TripEntity("trip", "Trip", TimeMode.DRAFT, null, TravelMode.FLEXIBLE, now, now))
        database.tripDao().insertDay(TripDayEntity("day", "trip", 0))
        database.savedPlaceDao().insertPlace(SavedPlaceEntity("place", "trip", "poi", "Place", "", 1.0, 2.0))
        database.itineraryEditingDao().insertItem(
            ItineraryItemEntity(
                id = "item",
                tripDayId = "day",
                tripId = "trip",
                savedPlaceId = "place",
                position = 0,
                arrivalTime = LocalTime.of(8, 0),
                stayDurationMinutes = 60,
                note = "已提交",
            ),
        )
    }
}
