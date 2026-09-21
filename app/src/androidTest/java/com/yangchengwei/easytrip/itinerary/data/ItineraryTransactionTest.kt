package com.yangchengwei.easytrip.itinerary.data

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.core.model.RouteStatus
import com.yangchengwei.easytrip.core.model.TimeMode
import com.yangchengwei.easytrip.core.model.TransportMode
import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.place.data.SavedPlaceEntity
import com.yangchengwei.easytrip.trip.data.TripDayEntity
import com.yangchengwei.easytrip.trip.data.TripEntity
import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.async
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ItineraryTransactionTest {
    private lateinit var database: EasyTripDatabase
    private lateinit var repository: RoomItineraryRepository
    private val now = Instant.parse("2026-08-22T00:00:00Z")
    private val itemIds = SequenceIds("item")
    private val legIds = SequenceIds("leg")

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), EasyTripDatabase::class.java).build()
        repository = newRepository(itemIds) { true }
    }

    @After fun tearDown() = database.close()

    @Test fun conditionalCalendarTimingRejectsStaleMovedAndDeletedVisitsAndPreservesNotes() = runTest {
        seedTrip("trip", TravelMode.FLEXIBLE, "day-1", "day-2")
        seedPlace("hotel", "trip", 0.0, 0.0)
        val id = repository.addItem("day-1", "hotel", 0)
        repository.updateDetails(id, null, null, "保留备注")
        val before = com.yangchengwei.easytrip.itinerary.domain.ItineraryTiming(null, null)
        val after = com.yangchengwei.easytrip.itinerary.domain.ItineraryTiming(LocalTime.of(9, 40), 90)
        val change = com.yangchengwei.easytrip.itinerary.domain.ItineraryTimingChange("trip", "day-1", id, before, after)
        assertTrue(repository.compareAndSetTiming(change))
        org.junit.Assert.assertFalse(repository.compareAndSetTiming(change))
        assertEquals("保留备注", database.itineraryEditingDao().item(id)?.note)
        assertTrue(repository.compareAndSetTiming(change.reversed()))
        assertNull(database.itineraryEditingDao().item(id)?.arrivalTime)
        assertNull(database.itineraryEditingDao().item(id)?.stayDurationMinutes)
        repository.moveItem(id, "day-2", 0)
        org.junit.Assert.assertFalse(repository.compareAndSetTiming(change))
        repository.deleteItem(id)
        org.junit.Assert.assertFalse(repository.compareAndSetTiming(change.copy(dayId = "day-2")))
    }

    @Test fun concurrentCalendarWritersOnlyOneCanChangeTheOriginalTiming() = runTest {
        seedTrip("trip", TravelMode.FLEXIBLE, "day")
        seedPlace("hotel", "trip", 0.0, 0.0)
        val id = repository.addItem("day", "hotel", 0)
        val before = com.yangchengwei.easytrip.itinerary.domain.ItineraryTiming(null, null)
        val results = coroutineScope {
            (1..2).map { hour -> async {
                repository.compareAndSetTiming(com.yangchengwei.easytrip.itinerary.domain.ItineraryTimingChange("trip", "day", id, before,
                    com.yangchengwei.easytrip.itinerary.domain.ItineraryTiming(LocalTime.of(hour, 0), 60)))
            } }.awaitAll()
        }
        assertEquals(1, results.count { it })
    }

    @Test fun duplicateHotelOccurrencesUseDistinctItemIdsAndEdges() = runTest {
        seedTrip("trip", TravelMode.FLEXIBLE, "day-1", "day-2", "day-3")
        seedPlace("hotel", "trip", 0.0, 0.0)
        val sameDayIds = List(3) { repository.addItem("day-1", "hotel", it) }
        val otherDayIds = listOf("day-2", "day-3").map { repository.addItem(it, "hotel", 0) }
        assertEquals(5, (sameDayIds + otherDayIds).distinct().size)
        assertEquals(sameDayIds, database.itineraryEditingDao().items("day-1").map { it.id })
        assertEquals(listOf(sameDayIds[0] to sameDayIds[1], sameDayIds[1] to sameDayIds[2]), database.routeLegDao().legs("day-1").map { it.fromItemId to it.toItemId })
        assertEquals(1, database.itineraryEditingDao().items("day-2").size)
        assertEquals(1, database.itineraryEditingDao().items("day-3").size)
    }

    @Test fun crossDayMoveDeletesInvalidLegAndCreatesFreshBridge() = runTest {
        seedTrip("trip", TravelMode.FLEXIBLE, "source", "target")
        seedPlace("a", "trip", 0.0, 0.0); seedPlace("b", "trip", 0.0, 0.005); seedPlace("c", "trip", 0.0, 0.2)
        val a = repository.addItem("source", "a", 0); val b = repository.addItem("source", "b", 1); val c = repository.addItem("source", "c", 2)
        val old = database.routeLegDao().legs("source").first { it.fromItemId == a && it.toItemId == b }
        database.routeLegDao().selectMode(old.id, TransportMode.DRIVE)
        repository.moveItem(b, "target", 0)
        assertEquals(listOf(a, c), database.itineraryEditingDao().items("source").map { it.id })
        assertEquals(listOf(b), database.itineraryEditingDao().items("target").map { it.id })
        val bridge = database.routeLegDao().legs("source").single()
        assertEquals(a to c, bridge.fromItemId to bridge.toItemId); assertNull(bridge.selectedMode)
        assertEquals(TransportMode.TAXI, bridge.recommendedMode); assertEquals(RouteStatus.PENDING, bridge.status); assertEquals(1L, bridge.version)
        assertNull(bridge.distanceMeters); assertNull(bridge.durationSeconds); assertNull(bridge.polyline)
        repository.deleteItem(c)
        assertEquals(0, database.routeLegDao().legs("source").size)
    }

    @Test fun inDayMovePreservesOnlyStillValidSelectedLeg() = runTest {
        seedTrip("trip", TravelMode.FLEXIBLE, "day")
        listOf("a", "b", "c", "d").forEachIndexed { i, id -> seedPlace(id, "trip", 0.0, i * 0.001) }
        val ids = listOf("a", "b", "c", "d").mapIndexed { i, place -> repository.addItem("day", place, i) }
        val untouched = database.routeLegDao().legs("day").first { it.fromItemId == ids[1] && it.toItemId == ids[2] }
        database.routeLegDao().selectMode(untouched.id, TransportMode.DRIVE)
        repository.moveItem(ids[0], "day", 2)
        assertEquals(listOf(ids[1], ids[2], ids[0], ids[3]), database.itineraryEditingDao().items("day").map { it.id })
        val legs = database.routeLegDao().legs("day")
        assertEquals(listOf(ids[1] to ids[2], ids[2] to ids[0], ids[0] to ids[3]), legs.map { it.fromItemId to it.toItemId })
        assertEquals(untouched.id, legs.first().id); assertEquals(TransportMode.DRIVE, legs.first().selectedMode)
    }

    @Test fun offlineNewLegWaitsForNetworkAndSelfDriveUsesDrive() = runTest {
        seedTrip("trip", TravelMode.SELF_DRIVE, "day"); seedPlace("a", "trip", 0.0, 0.0); seedPlace("b", "trip", 0.0, 1.0)
        repository = newRepository(itemIds) { false }
        repository.addItem("day", "a", 0); repository.addItem("day", "b", 1)
        val leg = database.routeLegDao().legs("day").single()
        assertEquals(TransportMode.DRIVE, leg.recommendedMode); assertEquals(RouteStatus.WAITING_NETWORK, leg.status); assertEquals(1L, leg.version)
    }

    @Test fun dayRootObservationEmitsEmptyDayAndItemAndPlaceChanges() = runTest {
        seedTrip("trip", TravelMode.FLEXIBLE, "day"); seedPlace("hotel", "trip", 0.0, 0.0)
        coroutineScope {
            val events = repository.observeDay("day").produceIn(this)
            val empty = events.receive()
            assertEquals("trip", empty.tripId); assertEquals(0, empty.items.size)

            val id = repository.addItem("day", "hotel", 0)
            assertEquals(id, events.receive().items.single().id)
            database.savedPlaceDao().updateNote("hotel", "updated")
            database.savedPlaceDao().insertPlace(SavedPlaceEntity("other", "trip", "other", "Other", "Other", 1.0, 1.0))
            assertEquals("hotel", events.receive().items.single().place.id)
            events.cancel()
        }
    }

    @Test fun deletingObservedDayTerminatesWithMissingDayError() = runTest {
        seedTrip("trip", TravelMode.FLEXIBLE, "day")
        coroutineScope {
            val started = CompletableDeferred<Unit>()
            val failure = async(Dispatchers.Default) {
                runCatching {
                    repository.observeDay("day").collect {
                        started.complete(Unit)
                    }
                }.exceptionOrNull()
            }
            started.await()
            database.tripDao().deleteDayRow("day")
            assertTrue(withContext(Dispatchers.Default.limitedParallelism(1)) { withTimeout(5_000) { failure.await() } } is IllegalArgumentException)
        }
    }

    @Test fun deletingFirstItemRemovesItsInvalidLegAndKeepsSavedPlace() = runTest {
        seedTrip("trip", TravelMode.FLEXIBLE, "day")
        listOf("a", "b", "c").forEachIndexed { index, id -> seedPlace(id, "trip", 0.0, index * 0.001) }
        val ids = listOf("a", "b", "c").mapIndexed { index, place -> repository.addItem("day", place, index) }

        repository.deleteItem(ids.first())

        assertEquals(ids.drop(1), database.itineraryEditingDao().items("day").map { it.id })
        assertEquals(listOf(ids[1] to ids[2]), database.routeLegDao().legs("day").map { it.fromItemId to it.toItemId })
        assertEquals("a", database.savedPlaceDao().place("a")?.id)
    }

    @Test fun deletingMiddleItemCreatesBridgeLegAndKeepsSavedPlace() = runTest {
        seedTrip("trip", TravelMode.FLEXIBLE, "day")
        listOf("a", "b", "c").forEachIndexed { index, id -> seedPlace(id, "trip", 0.0, index * 0.001) }
        val ids = listOf("a", "b", "c").mapIndexed { index, place -> repository.addItem("day", place, index) }

        repository.deleteItem(ids[1])

        assertEquals(listOf(ids[0], ids[2]), database.itineraryEditingDao().items("day").map { it.id })
        assertEquals(listOf(ids[0] to ids[2]), database.routeLegDao().legs("day").map { it.fromItemId to it.toItemId })
        assertEquals("b", database.savedPlaceDao().place("b")?.id)
    }

    @Test fun deletingLastItemRemovesItsInvalidLegAndKeepsSavedPlace() = runTest {
        seedTrip("trip", TravelMode.FLEXIBLE, "day")
        listOf("a", "b", "c").forEachIndexed { index, id -> seedPlace(id, "trip", 0.0, index * 0.001) }
        val ids = listOf("a", "b", "c").mapIndexed { index, place -> repository.addItem("day", place, index) }

        repository.deleteItem(ids.last())

        assertEquals(ids.dropLast(1), database.itineraryEditingDao().items("day").map { it.id })
        assertEquals(listOf(ids[0] to ids[1]), database.routeLegDao().legs("day").map { it.fromItemId to it.toItemId })
        assertEquals("c", database.savedPlaceDao().place("c")?.id)
    }

    @Test fun deleteBridgeLegConflictRollsBackItemsLegsAndSavedPlace() = runTest {
        seedTrip("trip", TravelMode.FLEXIBLE, "day")
        listOf("a", "b", "c", "d").forEachIndexed { index, id -> seedPlace(id, "trip", 0.0, index * 0.001) }
        val ids = listOf("a", "b", "c", "d").mapIndexed { index, place -> repository.addItem("day", place, index) }
        val beforeItems = database.itineraryEditingDao().items("day")
        val beforeLegs = database.routeLegDao().legs("day")
        val conflictingLegId = beforeLegs.first { it.fromItemId == ids[2] && it.toItemId == ids[3] }.id
        repository = RoomItineraryRepository(database, database.itineraryEditingDao(), database.routeLegDao(), Clock.fixed(now, ZoneOffset.UTC), itemIds, { conflictingLegId }, { true })

        assertThrows(SQLiteConstraintException::class.java) { kotlinx.coroutines.runBlocking { repository.deleteItem(ids[1]) } }

        assertEquals(beforeItems, database.itineraryEditingDao().items("day"))
        assertEquals(beforeLegs, database.routeLegDao().legs("day"))
        assertEquals("b", database.savedPlaceDao().place("b")?.id)
    }

    @Test fun timingAndObservationReflectSavedRows() = runTest {
        seedTrip("trip", TravelMode.FLEXIBLE, "day"); seedPlace("hotel", "trip", 0.0, 0.0)
        val id = repository.addItem("day", "hotel", 0); repository.updateTiming(id, LocalTime.of(9, 30), 480)
        val day = repository.observeDay("day").first()
        assertEquals("trip", day.tripId); assertEquals(id, day.items.single().id); assertEquals("hotel", day.items.single().place.id)
        assertEquals(LocalTime.of(9, 30), day.items.single().arrivalTime); assertEquals(480, day.items.single().stayMinutes)
        assertThrows(IllegalArgumentException::class.java) { kotlinx.coroutines.runBlocking { repository.updateTiming(id, null, -1) } }
    }

    @Test fun updateDetailsAtomicallyPersistsTimingAndTrimmedNote() = runTest {
        seedTrip("trip", TravelMode.FLEXIBLE, "day"); seedPlace("hotel", "trip", 0.0, 0.0)
        val id = repository.addItem("day", "hotel", 0)

        repository.updateDetails(id, LocalTime.of(10, 15), 90, "  Late\ncheckout  ")

        val item = database.itineraryEditingDao().item(id)!!
        assertEquals(LocalTime.of(10, 15), item.arrivalTime)
        assertEquals(90, item.stayDurationMinutes)
        assertEquals("Late\ncheckout", item.note)
        assertEquals("Late\ncheckout", repository.observeDay("day").first().items.single().note)
    }

    @Test fun updateDetailsNormalizesBlankNoteToNull() = runTest {
        seedTrip("trip", TravelMode.FLEXIBLE, "day"); seedPlace("hotel", "trip", 0.0, 0.0)
        val id = repository.addItem("day", "hotel", 0)

        repository.updateDetails(id, null, null, "  \n  ")

        assertNull(database.itineraryEditingDao().item(id)!!.note)
    }

    @Test fun crossTripReferencesAreRejectedWithoutChanges() = runTest {
        seedTrip("first", TravelMode.FLEXIBLE, "first-day"); seedTrip("second", TravelMode.FLEXIBLE, "second-day")
        seedPlace("first-place", "first", 0.0, 0.0); seedPlace("second-place", "second", 0.0, 0.0)
        val item = repository.addItem("first-day", "first-place", 0)
        assertThrows(IllegalArgumentException::class.java) { kotlinx.coroutines.runBlocking { repository.addItem("first-day", "second-place", 1) } }
        assertThrows(IllegalArgumentException::class.java) { kotlinx.coroutines.runBlocking { repository.moveItem(item, "second-day", 0) } }
        assertEquals(listOf(item), database.itineraryEditingDao().items("first-day").map { it.id }); assertEquals(0, database.itineraryEditingDao().items("second-day").size)
    }

    @Test fun legInsertConflictWithStillValidLegRollsBackItemsLegsAndSelectedMode() = runTest {
        seedTrip("trip", TravelMode.FLEXIBLE, "day")
        listOf("a", "b", "c", "d").forEachIndexed { index, id -> seedPlace(id, "trip", 0.0, index * 0.001) }
        val ids = listOf("a", "b", "c", "d").mapIndexed { index, place -> repository.addItem("day", place, index) }
        val beforeItems = database.itineraryEditingDao().items("day")
        val surviving = database.routeLegDao().legs("day").first { it.fromItemId == ids[1] && it.toItemId == ids[2] }
        database.routeLegDao().selectMode(surviving.id, TransportMode.DRIVE)
        val beforeLegs = database.routeLegDao().legs("day")
        repository = RoomItineraryRepository(database, database.itineraryEditingDao(), database.routeLegDao(), Clock.fixed(now, ZoneOffset.UTC), itemIds, { surviving.id }, { true })

        assertThrows(SQLiteConstraintException::class.java) { kotlinx.coroutines.runBlocking { repository.moveItem(ids[0], "day", 2) } }

        assertEquals(beforeItems, database.itineraryEditingDao().items("day"))
        assertEquals(beforeLegs, database.routeLegDao().legs("day"))
        assertEquals(listOf(0L, 1_000L, 2_000L, 3_000L), database.itineraryEditingDao().items("day").map { it.position })
        assertEquals(TransportMode.DRIVE, database.routeLegDao().legs("day").first { it.id == surviving.id }.selectedMode)
    }

    @Test fun removePlaceOccurrencesRepairsEveryAffectedDay() = runTest {
        seedTrip("trip", TravelMode.FLEXIBLE, "day-1", "day-2")
        seedPlace("a", "trip", 0.0, 0.0); seedPlace("b", "trip", 0.0, 0.001); seedPlace("c", "trip", 0.0, 0.002)
        val firstDay = listOf("a", "b", "c").mapIndexed { index, place -> repository.addItem("day-1", place, index) }
        val secondDay = listOf("a", "b", "c").mapIndexed { index, place -> repository.addItem("day-2", place, index) }

        repository.removePlaceOccurrences("b")

        assertEquals(listOf(firstDay[0], firstDay[2]), database.itineraryEditingDao().items("day-1").map { it.id })
        assertEquals(listOf(secondDay[0], secondDay[2]), database.itineraryEditingDao().items("day-2").map { it.id })
        assertEquals(listOf(firstDay[0] to firstDay[2]), database.routeLegDao().legs("day-1").map { it.fromItemId to it.toItemId })
        assertEquals(listOf(secondDay[0] to secondDay[2]), database.routeLegDao().legs("day-2").map { it.fromItemId to it.toItemId })
    }

    @Test fun routeLegQueryFollowsCurrentFromItemPosition() = runTest {
        seedTrip("trip", TravelMode.FLEXIBLE, "day")
        listOf("a", "b", "c", "d").forEachIndexed { i, id -> seedPlace(id, "trip", 0.0, i * 0.001) }
        val ids = listOf("a", "b", "c", "d").mapIndexed { i, place -> repository.addItem("day", place, i) }

        repository.moveItem(ids[3], "day", 0)

        assertEquals(listOf(ids[3] to ids[0], ids[0] to ids[1], ids[1] to ids[2]), database.routeLegDao().legs("day").map { it.fromItemId to it.toItemId })
    }

    @Test fun concurrentAddsLeaveCanonicalUniquePositionsAndLegs() = runTest {
        seedTrip("trip", TravelMode.FLEXIBLE, "day"); seedPlace("hotel", "trip", 0.0, 0.0)
        coroutineScope { List(8) { async { repository.addItem("day", "hotel", 0) } }.awaitAll() }
        val items = database.itineraryEditingDao().items("day"); val legs = database.routeLegDao().legs("day")
        assertEquals(List(8) { it * 1_000L }, items.map { it.position }); assertEquals(8, items.map { it.id }.distinct().size)
        assertEquals(7, legs.size); assertEquals(7, legs.map { it.fromItemId to it.toItemId }.distinct().size)
    }

    private fun newRepository(ids: () -> String, online: () -> Boolean) = RoomItineraryRepository(database, database.itineraryEditingDao(), database.routeLegDao(), Clock.fixed(now, ZoneOffset.UTC), ids, legIds, online)
    private suspend fun seedTrip(id: String, mode: TravelMode, vararg days: String) {
        database.tripDao().insertTrip(TripEntity(id, id, TimeMode.DRAFT, null, mode, now, now)); days.forEachIndexed { i, day -> database.tripDao().insertDay(TripDayEntity(day, id, i * 1_000L)) }
    }
    private suspend fun seedPlace(id: String, trip: String, lat: Double, lon: Double) { database.savedPlaceDao().insertPlace(SavedPlaceEntity(id, trip, id, id, id, lat, lon)) }
    private class SequenceIds(private val prefix: String) : () -> String { private val next = AtomicInteger(); override fun invoke() = "$prefix-${next.getAndIncrement()}" }
}
