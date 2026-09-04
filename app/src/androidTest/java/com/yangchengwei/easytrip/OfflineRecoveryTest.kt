package com.yangchengwei.easytrip

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.core.model.RouteStatus
import com.yangchengwei.easytrip.core.model.TimeMode
import com.yangchengwei.easytrip.core.model.TransportMode
import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.core.network.NetworkMonitor
import com.yangchengwei.easytrip.itinerary.data.ItineraryItemEntity
import com.yangchengwei.easytrip.place.data.SavedPlaceEntity
import com.yangchengwei.easytrip.route.data.RoomRouteLegRepository
import com.yangchengwei.easytrip.route.data.RouteLegEntity
import com.yangchengwei.easytrip.route.domain.DefaultRouteRefreshCoordinator
import com.yangchengwei.easytrip.route.domain.PolylineCodec
import com.yangchengwei.easytrip.route.domain.RouteLegWithEndpoints
import com.yangchengwei.easytrip.route.domain.RoutePlanOutcome
import com.yangchengwei.easytrip.route.domain.RoutePlanner
import com.yangchengwei.easytrip.route.domain.RouteResult
import com.yangchengwei.easytrip.trip.data.TripDayEntity
import com.yangchengwei.easytrip.trip.data.TripEntity
import com.yangchengwei.easytrip.permission.InMemoryLocationPermissionRequestStore
import com.yangchengwei.easytrip.permission.LocationPermissionCoordinator
import com.yangchengwei.easytrip.permission.LocationPermissionUiState
import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OfflineRecoveryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "offline-recovery-test.db"
    private lateinit var database: EasyTripDatabase

    @Before fun setUp() {
        context.deleteDatabase(name)
        database = open()
    }

    @After fun tearDown() {
        database.close()
        context.deleteDatabase(name)
    }

    @Test fun persistedRoutesRecoverInterruptedWorkWithoutTouchingSuccess() = runTest {
        val now = Instant.EPOCH
        database.tripDao().insertTrip(TripEntity("trip", "Trip", TimeMode.DRAFT, null, TravelMode.FLEXIBLE, now, now))
        database.tripDao().insertDay(TripDayEntity("day", "trip", 0))
        listOf(
            SavedPlaceEntity("a", "trip", "a", "A", "", 1.0, 2.0),
            SavedPlaceEntity("b", "trip", "b", "B", "", 3.0, 4.0),
            SavedPlaceEntity("c", "trip", "c", "C", "", 5.0, 6.0),
            SavedPlaceEntity("d", "trip", "d", "D", "", 7.0, 8.0),
        ).forEach { database.savedPlaceDao().insertPlace(it) }
        listOf("a", "b", "c", "d").forEachIndexed { index, placeId ->
            database.itineraryEditingDao().insertItem(
                ItineraryItemEntity("i${index + 1}", "day", "trip", placeId, index * 1_000L),
            )
        }
        val cachedPolyline = PolylineCodec.encode(listOf(GeoPoint(3.0, 4.0), GeoPoint(5.0, 6.0)))
        database.routeLegDao().insert(
            RouteLegEntity(
                "waiting", "day", "i1", "i2", TransportMode.WALK,
                status = RouteStatus.WAITING_NETWORK, version = 7, updatedAt = now,
            ),
        )
        database.routeLegDao().insert(
            RouteLegEntity(
                "cached", "day", "i2", "i3", TransportMode.TRANSIT,
                selectedMode = TransportMode.DRIVE,
                status = RouteStatus.SUCCESS,
                distanceMeters = 10,
                durationSeconds = 20,
                polyline = cachedPolyline,
                version = 2,
                updatedAt = now,
                durationOverrideSeconds = 25,
                note = "keep cache",
            ),
        )
        database.routeLegDao().insert(
            RouteLegEntity(
                "interrupted", "day", "i3", "i4", TransportMode.DRIVE,
                status = RouteStatus.CALCULATING, version = 4, updatedAt = now,
            ),
        )
        val cachedBeforeClose = database.routeLegDao().leg("cached")
        database.close()

        database = open()
        val monitor = MutableNetworkMonitor(online = false)
        val planner = GatedRecordingPlanner()
        val repository = RoomRouteLegRepository(database.routeLegDao())
        val coordinator = DefaultRouteRefreshCoordinator(repository, planner, monitor)
        val coordinatorJob = Job()
        try {
            coordinator.start(CoroutineScope(Dispatchers.Default + coordinatorJob))

            awaitRows { rows ->
                rows.firstOrNull { it.id == "interrupted" }?.let {
                    it.status == RouteStatus.WAITING_NETWORK && it.version == 5L
                } == true
            }
            var rows = database.routeLegDao().legs("day").associateBy { it.id }
            assertEquals(RouteStatus.WAITING_NETWORK, rows.getValue("waiting").status)
            assertEquals(7L, rows.getValue("waiting").version)
            assertEquals(cachedBeforeClose, rows.getValue("cached"))
            assertTrue(planner.calls.isEmpty())

            monitor.setOnline(true)
            planner.awaitCallCount(2)
            assertEquals(2, planner.calls.size)
            assertEquals(setOf("waiting", "interrupted"), planner.calls.map(RouteLegWithEndpoints::id).toSet())
            assertFalse(planner.calls.any { it.id == "cached" })
            rows = database.routeLegDao().legs("day").associateBy { it.id }
            assertEquals(RouteStatus.CALCULATING, rows.getValue("waiting").status)
            assertEquals(RouteStatus.CALCULATING, rows.getValue("interrupted").status)
            assertEquals(cachedBeforeClose, rows.getValue("cached"))
            planner.release()
            awaitRows { current -> current.count { it.status == RouteStatus.SUCCESS } == 3 }

            rows = database.routeLegDao().legs("day").associateBy { it.id }
            assertEquals(cachedBeforeClose, rows.getValue("cached"))
            assertEquals(RouteStatus.SUCCESS, rows.getValue("waiting").status)
            assertEquals(7L, rows.getValue("waiting").version)
            assertEquals(RouteStatus.SUCCESS, rows.getValue("interrupted").status)
            assertEquals(5L, rows.getValue("interrupted").version)
        } finally {
            coordinatorJob.cancelAndJoin()
        }
    }

    private suspend fun awaitRows(condition: (List<RouteLegEntity>) -> Boolean) {
        withContext(Dispatchers.Default.limitedParallelism(1)) {
            withTimeout(5_000) {
                database.routeLegDao().observeLegs("day").first(condition)
            }
        }
    }

    private class MutableNetworkMonitor(online: Boolean) : NetworkMonitor {
        private val mutable = MutableStateFlow(online)
        override val isOnline: StateFlow<Boolean> = mutable.asStateFlow()
        fun setOnline(online: Boolean) {
            mutable.value = online
        }
    }

    private class GatedRecordingPlanner : RoutePlanner {
        val calls = CopyOnWriteArrayList<RouteLegWithEndpoints>()
        private val release = CompletableDeferred<Unit>()
        private val callCountChanged = MutableStateFlow(0)

        override suspend fun plan(leg: RouteLegWithEndpoints): RoutePlanOutcome {
            calls += leg
            callCountChanged.value = calls.size
            release.await()
            val result = when (leg.id) {
                "waiting" -> RouteResult(101, 201, listOf(leg.origin, leg.destination))
                "interrupted" -> RouteResult(102, 202, listOf(leg.origin, leg.destination))
                else -> error("Unexpected planned leg: ${leg.id}")
            }
            return RoutePlanOutcome.Success(result)
        }

        suspend fun awaitCallCount(expected: Int) {
            withContext(Dispatchers.Default.limitedParallelism(1)) {
                withTimeout(5_000) { callCountChanged.first { it >= expected } }
            }
        }

        fun release() {
            release.complete(Unit)
        }
    }

    @Test fun onlineRecoveryRequeuesInterruptedWorkWithoutTouchingSuccess() = runTest {
        val now = Instant.EPOCH
        database.tripDao().insertTrip(TripEntity("trip", "Trip", TimeMode.DRAFT, null, TravelMode.FLEXIBLE, now, now))
        database.tripDao().insertDay(TripDayEntity("day", "trip", 0))
        database.savedPlaceDao().insertPlace(SavedPlaceEntity("a", "trip", "a", "A", "", 1.0, 2.0))
        database.savedPlaceDao().insertPlace(SavedPlaceEntity("b", "trip", "b", "B", "", 3.0, 4.0))
        database.itineraryEditingDao().insertItem(ItineraryItemEntity("i1", "day", "trip", "a", 0))
        database.itineraryEditingDao().insertItem(ItineraryItemEntity("i2", "day", "trip", "b", 1000))
        database.itineraryEditingDao().insertItem(ItineraryItemEntity("i3", "day", "trip", "a", 2000))
        database.routeLegDao().insert(RouteLegEntity("interrupted", "day", "i1", "i2", TransportMode.WALK, status = RouteStatus.CALCULATING, version = 4, updatedAt = now))
        database.routeLegDao().insert(RouteLegEntity("cached", "day", "i2", "i3", TransportMode.WALK, status = RouteStatus.SUCCESS, distanceMeters = 10, durationSeconds = 20, polyline = "v1|1.0,2.0;3.0,4.0", version = 2, updatedAt = now))

        val repository = RoomRouteLegRepository(database.routeLegDao())
        assertEquals(1, repository.recoverInterruptedCalculations(online = true))
        val rows = database.routeLegDao().legs("day").associateBy { it.id }
        assertEquals(RouteStatus.PENDING, rows.getValue("interrupted").status)
        assertEquals(5L, rows.getValue("interrupted").version)
        assertEquals(RouteStatus.SUCCESS, rows.getValue("cached").status)
        assertEquals(10, rows.getValue("cached").distanceMeters)
    }

    @Test fun reopeningWorkspaceDoesNotRequestLocationPermission() {
        val coordinator = LocationPermissionCoordinator(InMemoryLocationPermissionRequestStore(true))

        assertEquals(LocationPermissionUiState(), coordinator.uiState.value)
    }

    private fun open() = Room.databaseBuilder(context, EasyTripDatabase::class.java, name).build()
}
