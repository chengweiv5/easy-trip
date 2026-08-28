package com.yangchengwei.easytrip.trip.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.itinerary.data.RoomItineraryRepository
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.data.RoomSavedPlaceRepository
import com.yangchengwei.easytrip.place.domain.SavePlaceResult
import com.yangchengwei.easytrip.trip.domain.CreateTrip
import com.yangchengwei.easytrip.trip.domain.DateRangeApply
import com.yangchengwei.easytrip.trip.domain.DateRangeSnapshotChangedException
import com.yangchengwei.easytrip.trip.domain.DayDeletion
import com.yangchengwei.easytrip.trip.domain.InsertSide
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TripDateRangeRoomTest {
    private lateinit var database: EasyTripDatabase
    private var nextId = 0

    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            EasyTripDatabase::class.java,
        ).build()
    }

    @After fun tearDown() = database.close()

    @Test fun shrinkRejectsWhenDayIdsChangedAfterPreviewWithoutPartialWrite() = runTest {
        val repository = repository()
        val tripId = repository.createTrip(CreateTrip("Trip", 3, startDate = START))
        val baseline = repository.observeTrip(tripId).first()!!.days.map { it.id }
        val command = shrinkCommand(tripId, baseline)
        repository.insertDay(tripId, baseline.first(), InsertSide.AFTER)
        val beforeApply = snapshot(tripId)

        assertThrows(DateRangeSnapshotChangedException::class.java) {
            kotlinx.coroutines.runBlocking { repository.applyDateRange(command) }
        }

        assertEquals(beforeApply, snapshot(tripId))
    }

    @Test fun shrinkRejectsWhenItemCountChangedAfterPreviewWithoutPartialWrite() = runTest {
        val repository = repository()
        val tripId = repository.createTrip(CreateTrip("Trip", 3, startDate = START))
        val days = repository.observeTrip(tripId).first()!!.days.map { it.id }
        val content = content(tripId, days.last())
        val command = shrinkCommand(tripId, days, expectedItems = 2, expectedLegs = 1)
        content.itineraries.addItem(days.last(), content.placeIds.first(), 2)
        val beforeApply = snapshot(tripId)

        assertThrows(DateRangeSnapshotChangedException::class.java) {
            kotlinx.coroutines.runBlocking { repository.applyDateRange(command) }
        }

        assertEquals(beforeApply, snapshot(tripId))
    }

    @Test fun shrinkRejectsWhenLegCountChangedAfterPreviewWithoutPartialWrite() = runTest {
        val repository = repository()
        val tripId = repository.createTrip(CreateTrip("Trip", 3, startDate = START))
        val days = repository.observeTrip(tripId).first()!!.days.map { it.id }
        val content = content(tripId, days.last())
        val command = shrinkCommand(tripId, days, expectedItems = 2, expectedLegs = 1)
        val leg = database.routeLegDao().legs(days.last()).single()
        database.routeLegDao().deleteEdge(days.last(), leg.fromItemId, leg.toItemId)
        val beforeApply = snapshot(tripId)

        assertThrows(DateRangeSnapshotChangedException::class.java) {
            kotlinx.coroutines.runBlocking { repository.applyDateRange(command) }
        }

        assertEquals(beforeApply, snapshot(tripId))
        assertEquals(content.placeIds.toSet(), savedPlaceIds(tripId))
    }

    @Test fun applyRejectsMoreThanThirtyDaysWithoutWriting() = runTest {
        val repository = repository()
        val tripId = repository.createTrip(CreateTrip("Trip", 3, startDate = START))
        val original = repository.observeTrip(tripId).first()!!.days.map { it.id }
        val beforeApply = snapshot(tripId)

        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking {
                repository.applyDateRange(growthCommand(tripId, original, dayCount = 31))
            }
        }

        assertEquals(beforeApply, snapshot(tripId))
    }

    @Test fun growthAppendsTailKeepsOriginalIdsAndContinuousPositions() = runTest {
        val repository = repository()
        val tripId = repository.createTrip(CreateTrip("Trip", 3, startDate = START))
        val original = repository.observeTrip(tripId).first()!!.days.map { it.id }

        repository.applyDateRange(growthCommand(tripId, original, dayCount = 5))

        val grown = repository.observeTrip(tripId).first()!!
        assertEquals(original, grown.days.take(original.size).map { it.id })
        assertEquals(listOf(0, 1, 2, 3, 4), grown.days.map { it.index })
        assertEquals(List(5) { it * TripDao.POSITION_STEP }, database.tripDao().dayPositions(tripId))
        assertEquals(true, grown.days.drop(original.size).all { it.id.startsWith("id-") })
    }

    @Test fun failedGrowthRollsBackStartDateAndAllAppendedDays() = runTest {
        var calls = 0
        val repository = RoomTripRepository(
            database.tripDao(),
            idFactory = {
                calls++
                if (calls == 6) error("id failure") else "id-${calls - 1}"
            },
            database = database,
        )
        val tripId = repository.createTrip(CreateTrip("Trip", 3, startDate = START))
        val beforeApply = snapshot(tripId)
        val original = beforeApply.trip.days.map { it.id }

        assertThrows(IllegalStateException::class.java) {
            kotlinx.coroutines.runBlocking {
                repository.applyDateRange(growthCommand(tripId, original, dayCount = 5))
            }
        }

        val afterFailure = snapshot(tripId)
        assertEquals(beforeApply.trip.startDate, afterFailure.trip.startDate)
        assertEquals(beforeApply.trip.days.map { it.id }, afterFailure.trip.days.map { it.id })
        assertEquals(beforeApply.trip.days.map { it.index }, afterFailure.trip.days.map { it.index })
        assertEquals(beforeApply, afterFailure)
        assertEquals(List(3) { it * TripDao.POSITION_STEP }, database.tripDao().dayPositions(tripId))
    }

    @Test fun dayDeleteRejectsWhenItemCountChangedAfterPreviewAndLeavesContentUntouched() = runTest {
        val repository = repository()
        val tripId = repository.createTrip(CreateTrip("Trip", 3, startDate = START))
        val targetDay = repository.observeTrip(tripId).first()!!.days[1].id
        val targetContent = content(tripId, targetDay)
        val command = DayDeletion(targetDay, expectedItineraryItems = 2, expectedRouteLegs = 1)
        targetContent.itineraries.addItem(targetDay, targetContent.placeIds.first(), 2)
        val beforeDelete = snapshot(tripId)

        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { repository.deleteDay(command) }
        }

        assertEquals(beforeDelete, snapshot(tripId))
        assertEquals(targetContent.placeIds.toSet(), savedPlaceIds(tripId))
    }

    @Test fun dayDeleteRejectsWhenLegCountChangedAfterPreviewAndLeavesContentUntouched() = runTest {
        val repository = repository()
        val tripId = repository.createTrip(CreateTrip("Trip", 3, startDate = START))
        val targetDay = repository.observeTrip(tripId).first()!!.days[1].id
        val targetContent = content(tripId, targetDay)
        val command = DayDeletion(targetDay, expectedItineraryItems = 2, expectedRouteLegs = 1)
        val leg = database.routeLegDao().legs(targetDay).single()
        database.routeLegDao().deleteEdge(targetDay, leg.fromItemId, leg.toItemId)
        val beforeDelete = snapshot(tripId)

        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { repository.deleteDay(command) }
        }

        assertEquals(beforeDelete, snapshot(tripId))
        assertEquals(targetContent.placeIds.toSet(), savedPlaceIds(tripId))
    }

    @Test fun shrinkRejectsWhenStartDateChangedAfterPreviewWithoutPartialWrite() = runTest {
        val repository = repository()
        val tripId = repository.createTrip(CreateTrip("Trip", 3, startDate = START))
        val days = repository.observeTrip(tripId).first()!!.days.map { it.id }
        val command = shrinkCommand(tripId, days)
        repository.setStartDate(tripId, START.plusDays(1))
        val beforeApply = snapshot(tripId)

        assertThrows(DateRangeSnapshotChangedException::class.java) {
            kotlinx.coroutines.runBlocking { repository.applyDateRange(command) }
        }

        assertEquals(beforeApply, snapshot(tripId))
    }

    @Test fun shrinkCascadesItemsAndLegsKeepsSavedPlacesAndRenumbersDays() = runTest {
        val repository = repository()
        val tripId = repository.createTrip(CreateTrip("Trip", 3, startDate = START))
        val originalDays = repository.observeTrip(tripId).first()!!.days.map { it.id }
        val retained = content(tripId, originalDays.first())
        val deleted = content(tripId, originalDays.last())
        val savedPlacesBefore = database.tripDao().savedPlaceCount(tripId)

        repository.applyDateRange(shrinkCommand(tripId, originalDays, expectedItems = 2, expectedLegs = 1))

        val trip = repository.observeTrip(tripId).first()!!
        assertEquals(originalDays.take(2), trip.days.map { it.id })
        assertEquals(listOf(0, 1), trip.days.map { it.index })
        assertEquals(true, trip.days.all { it.id.startsWith("id-") })
        assertEquals(2, database.itineraryEditingDao().items(originalDays.first()).size)
        assertEquals(1, database.routeLegDao().legs(originalDays.first()).size)
        assertEquals(0, database.itineraryEditingDao().items(originalDays.last()).size)
        assertEquals(0, database.routeLegDao().legs(originalDays.last()).size)
        assertEquals(savedPlacesBefore, database.tripDao().savedPlaceCount(tripId))
        assertEquals(retained.placeIds.toSet() + deleted.placeIds.toSet(), database.savedPlaceDao().observePlaces(tripId).first().map { it.id }.toSet())
    }

    @Test fun dayDeleteCascadesItemsAndLegsKeepsSavedPlacesAndRenumbersDays() = runTest {
        val repository = repository()
        val tripId = repository.createTrip(CreateTrip("Trip", 3, startDate = START))
        val originalDays = repository.observeTrip(tripId).first()!!.days.map { it.id }
        content(tripId, originalDays.first())
        content(tripId, originalDays[1])
        val savedPlacesBefore = savedPlaceIds(tripId)

        repository.deleteDay(DayDeletion(originalDays[1], expectedItineraryItems = 2, expectedRouteLegs = 1))

        val trip = repository.observeTrip(tripId).first()!!
        assertEquals(listOf(originalDays[0], originalDays[2]), trip.days.map { it.id })
        assertEquals(listOf(0, 1), trip.days.map { it.index })
        assertEquals(true, trip.days.all { it.id.startsWith("id-") })
        assertEquals(2, database.itineraryEditingDao().items(originalDays.first()).size)
        assertEquals(1, database.routeLegDao().legs(originalDays.first()).size)
        assertEquals(0, database.itineraryEditingDao().items(originalDays[1]).size)
        assertEquals(0, database.routeLegDao().legs(originalDays[1]).size)
        assertEquals(savedPlacesBefore, savedPlaceIds(tripId))
    }

    private fun repository() = RoomTripRepository(
        database.tripDao(),
        idFactory = { "id-${nextId++}" },
        database = database,
    )

    private fun shrinkCommand(
        tripId: String,
        dayIds: List<String>,
        expectedItems: Int = 0,
        expectedLegs: Int = 0,
    ) = DateRangeApply(
        tripId = tripId,
        expectedStartDate = START,
        startDate = START,
        dayCount = 2,
        expectedDayIds = dayIds,
        expectedDeletedDayIds = listOf(dayIds.last()),
        expectedDeletedItineraryItems = expectedItems,
        expectedDeletedRouteLegs = expectedLegs,
    )

    private fun growthCommand(tripId: String, dayIds: List<String>, dayCount: Int) = DateRangeApply(
        tripId = tripId,
        expectedStartDate = START,
        startDate = START,
        dayCount = dayCount,
        expectedDayIds = dayIds,
        expectedDeletedDayIds = emptyList(),
        expectedDeletedItineraryItems = 0,
        expectedDeletedRouteLegs = 0,
    )

    private suspend fun savedPlaceIds(tripId: String) =
        database.savedPlaceDao().observePlaces(tripId).first().map { it.id }.toSet()

    private suspend fun content(tripId: String, dayId: String): Content {
        val places = RoomSavedPlaceRepository(database, idFactory = { "place-${nextId++}" })
        val itineraries = RoomItineraryRepository(
            database,
            database.itineraryEditingDao(),
            database.routeLegDao(),
            itemIdFactory = { "item-${nextId++}" },
            legIdFactory = { "leg-${nextId++}" },
        )
        val placeIds = listOf("a", "b").map { suffix ->
            (places.save(tripId, PlaceCandidate("$dayId-$suffix", suffix, "", GeoPoint(1.0, nextId.toDouble()), null)) as SavePlaceResult.Saved).id
        }
        placeIds.forEachIndexed { index, placeId -> itineraries.addItem(dayId, placeId, index) }
        return Content(placeIds, itineraries)
    }

    private suspend fun snapshot(tripId: String): Snapshot {
        val trip = repository().observeTrip(tripId).first()!!
        val dayIds = trip.days.map { it.id }
        return Snapshot(
            trip,
            dayIds.associateWith { database.itineraryEditingDao().items(it) },
            dayIds.associateWith { database.routeLegDao().legs(it) },
            database.tripDao().savedPlaceCount(tripId),
        )
    }

    private data class Content(val placeIds: List<String>, val itineraries: RoomItineraryRepository)
    private data class Snapshot(
        val trip: com.yangchengwei.easytrip.trip.domain.TripWithDays,
        val items: Map<String, List<com.yangchengwei.easytrip.itinerary.data.ItineraryItemEntity>>,
        val legs: Map<String, List<com.yangchengwei.easytrip.route.data.RouteLegEntity>>,
        val savedPlaces: Int,
    )

    companion object {
        private val START = LocalDate.parse("2026-10-01")
    }
}
