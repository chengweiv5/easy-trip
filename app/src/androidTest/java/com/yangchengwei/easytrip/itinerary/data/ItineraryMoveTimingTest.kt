package com.yangchengwei.easytrip.itinerary.data

import android.content.Context
import androidx.room.Room
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.core.model.*
import com.yangchengwei.easytrip.place.data.SavedPlaceEntity
import com.yangchengwei.easytrip.route.data.RoomRouteLegRepository
import com.yangchengwei.easytrip.route.domain.RouteResult
import com.yangchengwei.easytrip.trip.data.TripDayEntity
import com.yangchengwei.easytrip.trip.data.RoomTripRepository
import com.yangchengwei.easytrip.itinerary.ui.DayItineraryViewModel
import com.yangchengwei.easytrip.trip.data.TripEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.*
import org.junit.Assert.*
import java.time.Instant
import java.time.LocalTime
import java.util.UUID

class ItineraryMoveTimingTest {
    private lateinit var db: EasyTripDatabase
    private lateinit var items: RoomItineraryRepository
    private lateinit var routes: RoomRouteLegRepository

    @Before fun setup() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), EasyTripDatabase::class.java).build()
        items = RoomItineraryRepository(db, db.itineraryEditingDao(), db.routeLegDao())
        routes = RoomRouteLegRepository(db.routeLegDao())
        db.tripDao().createTripWithDays(
            TripEntity("trip", "移动时间", TimeMode.DRAFT, null, TravelMode.FLEXIBLE, Instant.EPOCH, Instant.EPOCH),
            listOf(TripDayEntity("source", "trip", 0), TripDayEntity("target", "trip", 1)),
        )
        for (id in listOf("a", "b", "c", "d")) {
            db.savedPlaceDao().insertPlace(SavedPlaceEntity(id, "trip", id, id, "地址", 30.0, 120.0))
        }
    }

    @After fun close() { db.close() }

    @Test fun appendToAnotherDayReestimatesArrivalAndStayFromItsLastStop() = runBlocking {
        val moved = items.addItem("source", "a", 0)
        items.updateDetailsWithExpense(moved, LocalTime.of(7, 0), 15, "门票备注", 3500)
        val last = items.addItem("target", "b", 0)
        items.updateTiming(last, LocalTime.of(14, 0), 90)

        items.appendItem(moved, "target")

        val day = day("target")
        assertEquals(listOf(last, moved), day.map { it.id })
        assertEquals(LocalTime.of(15, 30), day.last().arrivalTime)
        assertEquals(60, day.last().stayMinutes)
        assertEquals("门票备注", day.last().note)
        assertEquals(3500L, day.last().expenseCents)
        assertTrue(day("source").isEmpty())
        complete("target", last, moved, 1200)
        assertEquals(LocalTime.of(15, 50), day("target").last().arrivalTime)
        assertEquals(LocalTime.of(14, 0), day("target").first().arrivalTime)
    }

    @Test fun sameDayFirstToLastReestimatesFromTheEarliestChangedPosition() = runBlocking {
        val a = items.addItem("source", "a", 0)
        val b = items.addItem("source", "b", 1)
        val c = items.addItem("source", "c", 2)
        items.updateTiming(a, LocalTime.of(7, 0), 15)
        items.updateTiming(b, LocalTime.of(15, 0), 90)
        items.updateTiming(c, LocalTime.of(18, 0), 30)

        items.moveItem(a, "source", 2)

        assertEquals(listOf(b, c, a), day("source").map { it.id })
        assertEquals(listOf(LocalTime.of(8, 0), LocalTime.of(9, 30), LocalTime.of(10, 0)),
            day("source").map { it.arrivalTime })
        assertEquals(listOf(90, 30, 60), day("source").map { it.stayMinutes })
        // Downstream result arrives first; the still-pending chain must refine again.
        complete("source", c, a, 900)
        complete("source", b, c, 1800)
        assertEquals(listOf(LocalTime.of(8, 0), LocalTime.of(10, 0), LocalTime.of(10, 45)),
            day("source").map { it.arrivalTime })
    }

    @Test fun sameDayLastToFirstStartsAtEightAndReestimatesAllSuccessors() = runBlocking {
        val a = stop("source", "a", 0, 11, 0, 45)
        val b = stop("source", "b", 1, 18, 0, 90)
        val c = stop("source", "c", 2, 20, 0, 20)
        items.moveItem(c, "source", 0)
        assertEquals(listOf(c, a, b), day("source").map { it.id })
        assertEquals(listOf(LocalTime.of(8, 0), LocalTime.of(9, 0), LocalTime.of(9, 45)),
            day("source").map { it.arrivalTime })
        assertEquals(listOf(60, 45, 90), day("source").map { it.stayMinutes })
        complete("source", a, b, 1200)
        complete("source", c, a, 600)
        assertEquals(listOf(LocalTime.of(8, 0), LocalTime.of(9, 10), LocalTime.of(10, 15)),
            day("source").map { it.arrivalTime })
    }

    @Test fun middleMoveKeepsUnaffectedPrefixAndReestimatesTheSuffix() = runBlocking {
        val a = stop("source", "a", 0, 11, 15, 45)
        val b = stop("source", "b", 1, 15, 0, 20)
        val c = stop("source", "c", 2, 16, 0, 30)
        val d = stop("source", "d", 3, 18, 0, 90)
        val prefix = day("source").first()
        items.moveItem(b, "source", 2)
        assertEquals(listOf(a, c, b, d), day("source").map { it.id })
        assertEquals(prefix, day("source").first())
        assertEquals(listOf(LocalTime.of(11, 15), LocalTime.of(12, 0), LocalTime.of(12, 30), LocalTime.of(13, 30)),
            day("source").map { it.arrivalTime })
        assertEquals(listOf(45, 30, 60, 90), day("source").map { it.stayMinutes })
    }

    @Test fun crossDayMoveReestimatesTheSourceGapAndStartsAnEmptyTargetAtEight() = runBlocking {
        val a = stop("source", "a", 0, 10, 0, 30)
        val b = stop("source", "b", 1, 14, 0, 20)
        val c = stop("source", "c", 2, 17, 0, 40)
        val d = stop("source", "d", 3, 20, 0, 90)
        items.appendItem(b, "target")
        assertEquals(LocalTime.of(8, 0), day("target").single().arrivalTime)
        assertEquals(60, day("target").single().stayMinutes)
        assertEquals(listOf(a, c, d), day("source").map { it.id })
        assertEquals(listOf(LocalTime.of(10, 0), LocalTime.of(10, 30), LocalTime.of(11, 10)),
            day("source").map { it.arrivalTime })
        complete("source", c, d, 600)
        complete("source", a, c, 900)
        assertEquals(listOf(LocalTime.of(10, 0), LocalTime.of(10, 45), LocalTime.of(11, 35)),
            day("source").map { it.arrivalTime })
    }

    @Test fun movingIntoTheMiddleOfAnotherDayReestimatesItsExistingSuccessor() = runBlocking {
        val a = stop("source", "a", 0, 7, 0, 15)
        val b = stop("target", "b", 0, 14, 0, 30)
        val c = stop("target", "c", 1, 18, 0, 25)
        items.moveItem(a, "target", 1)
        assertEquals(listOf(b, a, c), day("target").map { it.id })
        assertEquals(listOf(LocalTime.of(14, 0), LocalTime.of(14, 30), LocalTime.of(15, 30)),
            day("target").map { it.arrivalTime })
        assertEquals(listOf(30, 60, 25), day("target").map { it.stayMinutes })
    }

    @Test fun manualSameValueAndChangeBackAfterMovingWinOverLateRoutes() = runBlocking {
        val a = items.addItem("source", "a", 0)
        val b = items.addItem("source", "b", 1)
        val c = items.addItem("source", "c", 2)
        items.moveItem(c, "source", 1)
        items.updateDetailsWithExpense(c, LocalTime.of(9, 0), 60, "手动确认", 1234)
        items.updateTiming(b, LocalTime.of(12, 0), 30)
        items.updateTiming(b, LocalTime.of(10, 0), 60)
        val before = day("source")
        complete("source", c, b, 900)
        complete("source", a, c, 1800)
        assertEquals(before, day("source"))
    }

    @Test fun movingToTheSamePositionDoesNotResetTimingOrReplaceRoutes() = runBlocking {
        val a = items.addItem("source", "a", 0)
        val b = items.addItem("source", "b", 1)
        items.updateTiming(a, LocalTime.of(11, 0), 25)
        val before = day("source")
        val beforeRoutes = routes.observeDay("source").first()
        items.moveItem(a, "source", 0)
        items.appendItem(b, "source")
        assertEquals(before, day("source"))
        assertEquals(beforeRoutes, routes.observeDay("source").first())
        complete("source", a, b, 600)
        assertEquals(LocalTime.of(11, 0), day("source").first().arrivalTime)
        assertEquals(LocalTime.of(11, 35), day("source").last().arrivalTime)
    }

    @Test fun retainedManualTransportDurationStillControlsTheRescheduledSuffix() = runBlocking {
        val a = stop("source", "a", 0, 7, 0, 15)
        val b = stop("source", "b", 1, 15, 0, 90)
        val c = stop("source", "c", 2, 18, 0, 30)
        val bc = routes.observeDay("source").first().single { it.fromItemId == b && it.toItemId == c }
        assertTrue(routes.updateDetails(bc.id, TransportMode.DRIVE, 1200, "交通备注", true))
        val kept = routes.observeDay("source").first().single { it.id == bc.id }
        items.moveItem(a, "source", 2)
        assertEquals(kept, routes.observeDay("source").first().single { it.id == bc.id })
        assertEquals(listOf(LocalTime.of(8, 0), LocalTime.of(9, 50), LocalTime.of(10, 20)),
            day("source").map { it.arrivalTime })
        complete("source", b, c, 7200)
        assertEquals(LocalTime.of(9, 50), day("source")[1].arrivalTime)
    }

    @Test fun midnightOverflowAfterMoveDoesNotWrapIntoTheMorning() = runBlocking {
        val a = stop("source", "a", 0, 7, 0, 15)
        val b = stop("target", "b", 0, 23, 0, 60)
        items.appendItem(a, "target")
        assertNull(day("target").last().arrivalTime)
        assertEquals(60, day("target").last().stayMinutes)
        assertEquals("预计时间超出当天，请调整", day("target").last().timingWarning)
        complete("target", b, a, 1800)
        assertNull(day("target").last().arrivalTime)
    }

    @Test fun missingPredecessorTimeAfterMoveRemainsAdjustable() = runBlocking {
        val a = stop("source", "a", 0, 7, 0, 15)
        val b = items.addItem("target", "b", 0)
        items.updateTiming(b, null, 30)
        items.appendItem(a, "target")
        assertNull(day("target").last().arrivalTime)
        assertEquals(60, day("target").last().stayMinutes)
        assertEquals("前站时间待定，请调整", day("target").last().timingWarning)
        complete("target", b, a, 1800)
        assertNull(day("target").last().arrivalTime)
    }

    @Test fun offlineMoveShowsAnImmediateEstimateBeforeTheRouteIsAvailable() = runBlocking {
        db.savedPlaceDao().insertPlace(SavedPlaceEntity("far", "trip", "far", "一公里外", "地址", 30.009, 120.0))
        items = RoomItineraryRepository(db, db.itineraryEditingDao(), db.routeLegDao(),
            isOnline = { false }, recommendMode = { _, _, _ -> TransportMode.WALK })
        val a = stop("source", "far", 0, 7, 0, 15)
        val b = stop("target", "b", 0, 14, 0, 30)
        items.appendItem(a, "target")
        assertEquals(LocalTime.of(14, 48), day("target").last().arrivalTime)
        assertEquals(RouteStatus.WAITING_NETWORK, routes.observeDay("target").first().single().status)
        complete("target", b, a, 600)
        assertEquals(LocalTime.of(14, 40), day("target").last().arrivalTime)
    }

    @Test fun movedPendingTimingAndLaterManualEditSurviveDatabaseReopen() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "move-timing-${UUID.randomUUID()}.db"
        fun open() = Room.databaseBuilder(context, EasyTripDatabase::class.java, name).build()
        var persistent = open()
        try {
            persistent.tripDao().createTripWithDays(
                TripEntity("trip", "重开", TimeMode.DRAFT, null, TravelMode.FLEXIBLE, Instant.EPOCH, Instant.EPOCH),
                listOf(TripDayEntity("source", "trip", 0)),
            )
            persistent.savedPlaceDao().insertPlace(SavedPlaceEntity("a", "trip", "a", "同坐标", "地址", 30.0, 120.0))
            var repository = RoomItineraryRepository(persistent, persistent.itineraryEditingDao(), persistent.routeLegDao())
            val a = repository.addItem("source", "a", 0)
            val b = repository.addItem("source", "a", 1)
            val c = repository.addItem("source", "a", 2)
            repository.moveItem(c, "source", 0)
            repository.updateTiming(b, LocalTime.of(10, 0), 60)
            persistent.close()
            persistent = open()
            repository = RoomItineraryRepository(persistent, persistent.itineraryEditingDao(), persistent.routeLegDao())
            val routeRepository = RoomRouteLegRepository(persistent.routeLegDao())
            for ((from, to) in listOf(a to b, c to a)) {
                val leg = routeRepository.observeDay("source").first().single { it.fromItemId == from && it.toItemId == to }
                assertTrue(routeRepository.claimIfVersionMatches(leg.id, leg.version))
                assertTrue(routeRepository.completeIfVersionMatches(leg.id, leg.version,
                    RouteResult(100, 1800, listOf(GeoPoint(30.0, 120.0), GeoPoint(30.0, 120.001)))))
            }
            assertEquals(listOf(c, a, b), repository.observeDay("source").first().items.map { it.id })
            assertEquals(listOf(LocalTime.of(8, 0), LocalTime.of(9, 30), LocalTime.of(10, 0)),
                repository.observeDay("source").first().items.map { it.arrivalTime })
        } finally {
            persistent.close()
            context.deleteDatabase(name) // Only this test's uniquely named temporary database.
        }
    }

    @Test fun dragCommitPublishesTheNewOrderAndTimesToTheDayScreenTogether() = runBlocking {
        val a = stop("source", "a", 0, 7, 0, 15)
        val b = stop("source", "b", 1, 15, 0, 90)
        val c = stop("source", "c", 2, 18, 0, 30)
        val store = ViewModelStore()
        try {
            val model = withContext(Dispatchers.Main) {
                DayItineraryViewModel("trip", RoomTripRepository(db.tripDao(), database = db), items, routes, null)
                    .also { store.put("day", it) }
            }
            withTimeout(5_000) { model.state.first { it.selectedDayId == "source" && it.items.size == 3 } }
            withContext(Dispatchers.Main) {
                model.previewMove(a, 2)
                model.commitMove(a, 2)
            }
            val state = withTimeout(5_000) { model.state.first { it.items.map { item -> item.id } == listOf(b, c, a) } }
            assertEquals(listOf(LocalTime.of(8, 0), LocalTime.of(9, 30), LocalTime.of(10, 0)), state.items.map { it.arrivalTime })
            assertEquals(listOf(b, c, a), state.previewOrder)
            assertNull(state.error)
        } finally {
            withContext(Dispatchers.Main) { store.clear() }
        }
    }

    @Test fun moveToDayActionAppendsWithNewTimingVisibleWhenTheTargetIsSelected() = runBlocking {
        val a = stop("source", "a", 0, 7, 0, 15)
        val b = stop("target", "b", 0, 14, 0, 90)
        val store = ViewModelStore()
        try {
            val model = withContext(Dispatchers.Main) {
                DayItineraryViewModel("trip", RoomTripRepository(db.tripDao(), database = db), items, routes, null)
                    .also { store.put("day", it) }
            }
            withTimeout(5_000) { model.state.first { it.selectedDayId == "source" && it.items.size == 1 } }
            withContext(Dispatchers.Main) {
                model.requestCrossDay(a)
                model.moveToDay("target")
            }
            withTimeout(5_000) { model.state.first { it.items.isEmpty() && it.crossDayMove == null } }
            withContext(Dispatchers.Main) { model.selectDay("target") }
            val state = withTimeout(5_000) { model.state.first { it.selectedDayId == "target" && it.items.size == 2 } }
            assertEquals(listOf(b, a), state.items.map { it.id })
            assertEquals(listOf(LocalTime.of(14, 0), LocalTime.of(15, 30)), state.items.map { it.arrivalTime })
            assertEquals(listOf(90, 60), state.items.map { it.stayMinutes })
            assertNull(state.error)
        } finally {
            withContext(Dispatchers.Main) { store.clear() }
        }
    }

    private suspend fun stop(dayId: String, place: String, index: Int, hour: Int, minute: Int, stay: Int): String {
        val id = items.addItem(dayId, place, index)
        items.updateTiming(id, LocalTime.of(hour, minute), stay)
        return id
    }

    private suspend fun day(id: String) = items.observeDay(id).first().items

    private suspend fun complete(dayId: String, from: String, to: String, seconds: Int) {
        val leg = routes.observeDay(dayId).first().single { it.fromItemId == from && it.toItemId == to }
        assertTrue(routes.claimIfVersionMatches(leg.id, leg.version))
        assertTrue(routes.completeIfVersionMatches(leg.id, leg.version,
            RouteResult(100, seconds, listOf(GeoPoint(30.0, 120.0), GeoPoint(30.0, 120.001)))))
    }
}
