package com.yangchengwei.easytrip

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.core.model.RouteStatus
import com.yangchengwei.easytrip.core.model.TimeMode
import com.yangchengwei.easytrip.core.model.TransportMode
import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.itinerary.data.ItineraryItemEntity
import com.yangchengwei.easytrip.place.data.SavedPlaceEntity
import com.yangchengwei.easytrip.route.data.RoomRouteLegRepository
import com.yangchengwei.easytrip.route.data.RouteLegEntity
import com.yangchengwei.easytrip.trip.data.TripDayEntity
import com.yangchengwei.easytrip.trip.data.TripEntity
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
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
        database.savedPlaceDao().insertPlace(SavedPlaceEntity("a", "trip", "a", "A", "", 1.0, 2.0))
        database.savedPlaceDao().insertPlace(SavedPlaceEntity("b", "trip", "b", "B", "", 3.0, 4.0))
        database.itineraryEditingDao().insertItem(ItineraryItemEntity("i1", "day", "trip", "a", 0))
        database.itineraryEditingDao().insertItem(ItineraryItemEntity("i2", "day", "trip", "b", 1000))
        database.itineraryEditingDao().insertItem(ItineraryItemEntity("i3", "day", "trip", "a", 2000))
        database.routeLegDao().insert(RouteLegEntity("interrupted", "day", "i1", "i2", TransportMode.WALK, status = RouteStatus.CALCULATING, version = 4, updatedAt = now))
        database.routeLegDao().insert(RouteLegEntity("cached", "day", "i2", "i3", TransportMode.WALK, status = RouteStatus.SUCCESS, distanceMeters = 10, durationSeconds = 20, polyline = "v1|1.0,2.0;3.0,4.0", version = 2, updatedAt = now))
        database.close()

        database = open()
        val repository = RoomRouteLegRepository(database.routeLegDao())
        assertEquals(1, repository.recoverInterruptedCalculations(online = false))
        val rows = database.routeLegDao().legs("day").associateBy { it.id }
        assertEquals(RouteStatus.WAITING_NETWORK, rows.getValue("interrupted").status)
        assertEquals(5L, rows.getValue("interrupted").version)
        assertEquals(RouteStatus.SUCCESS, rows.getValue("cached").status)
        assertEquals(10, rows.getValue("cached").distanceMeters)
    }

    private fun open() = Room.databaseBuilder(context, EasyTripDatabase::class.java, name).build()
}
