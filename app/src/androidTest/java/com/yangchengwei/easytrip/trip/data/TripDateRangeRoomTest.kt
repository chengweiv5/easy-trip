package com.yangchengwei.easytrip.trip.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.data.RoomSavedPlaceRepository
import com.yangchengwei.easytrip.trip.domain.CreateTrip
import com.yangchengwei.easytrip.trip.domain.DateRangeApply
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TripDateRangeRoomTest {
    private lateinit var database: EasyTripDatabase

    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            EasyTripDatabase::class.java,
        ).build()
    }

    @After fun tearDown() = database.close()

    @Test fun rangeApplyPreservesPrefixIdsAndSavedPlacesWhileAppendingAndShrinkingTail() = runTest {
        var id = 0
        val repository = RoomTripRepository(database.tripDao(), idFactory = { "id-${id++}" }, database = database)
        val tripId = repository.createTrip(CreateTrip("Trip", 3))
        val original = repository.observeTrip(tripId).first()!!.days.map { it.id }
        val places = RoomSavedPlaceRepository(database)
        places.save(tripId, PlaceCandidate("poi", "Place", "", GeoPoint(1.0, 2.0), null))

        repository.applyDateRange(DateRangeApply(tripId, LocalDate.parse("2026-10-01"), 5))
        val grown = repository.observeTrip(tripId).first()!!
        assertEquals(original, grown.days.take(3).map { it.id })
        assertEquals(5, grown.days.size)

        repository.applyDateRange(DateRangeApply(tripId, LocalDate.parse("2026-11-01"), 2))
        val shrunk = repository.observeTrip(tripId).first()!!
        assertEquals(original.take(2), shrunk.days.map { it.id })
        assertEquals(LocalDate.parse("2026-11-01"), shrunk.startDate)
        assertEquals(1, database.deleteImpactDao().places(tripId))
    }

    @Test fun dayDeleteRejectsChangedImpactAndLeavesDayUntouched() = runTest {
        var id = 0
        val repository = RoomTripRepository(database.tripDao(), idFactory = { "id-${id++}" }, database = database)
        val tripId = repository.createTrip(CreateTrip("Trip", 2))
        val dayId = repository.observeTrip(tripId).first()!!.days.first().id

        try {
            repository.deleteDay(com.yangchengwei.easytrip.trip.domain.DayDeletion(dayId, 1, 0))
            fail("Expected changed impact")
        } catch (_: IllegalArgumentException) {
        }

        assertEquals(2, repository.observeTrip(tripId).first()!!.days.size)
    }

    @Test fun failedGrowthRollsBackDateAndAllAppendedDays() = runTest {
        var id = 0
        val repository = RoomTripRepository(
            database.tripDao(),
            idFactory = {
                id++
                if (id == 4) error("id failure") else "id-$id"
            },
            database = database,
        )
        val tripId = repository.createTrip(CreateTrip("Trip", 2))
        val before = repository.observeTrip(tripId).first()!!

        try {
            repository.applyDateRange(DateRangeApply(tripId, LocalDate.parse("2026-10-01"), 4))
            fail("Expected id failure")
        } catch (_: IllegalStateException) {
        }

        assertEquals(before, repository.observeTrip(tripId).first())
    }
}
