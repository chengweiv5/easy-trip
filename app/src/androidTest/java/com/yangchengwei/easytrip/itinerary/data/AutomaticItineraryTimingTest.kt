package com.yangchengwei.easytrip.itinerary.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.core.model.*
import com.yangchengwei.easytrip.place.data.SavedPlaceEntity
import com.yangchengwei.easytrip.trip.data.TripDayEntity
import com.yangchengwei.easytrip.trip.data.TripEntity
import com.yangchengwei.easytrip.route.data.RoomRouteLegRepository
import com.yangchengwei.easytrip.route.domain.RouteResult
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import java.time.Instant
import java.time.LocalTime
import java.util.UUID

class AutomaticItineraryTimingTest {
    private lateinit var db: EasyTripDatabase
    private lateinit var items: RoomItineraryRepository
    private lateinit var routes: RoomRouteLegRepository
    @Before fun setup() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), EasyTripDatabase::class.java).build()
        items = RoomItineraryRepository(db, db.itineraryEditingDao(), db.routeLegDao())
        routes = RoomRouteLegRepository(db.routeLegDao())
        db.tripDao().createTripWithDays(TripEntity("trip", "测试", TimeMode.DRAFT, null, TravelMode.FLEXIBLE, Instant.EPOCH, Instant.EPOCH),
            listOf(TripDayEntity("day", "trip", 0), TripDayEntity("other", "trip", 1)))
        for (id in listOf("a", "b", "c", "d")) db.savedPlaceDao().insertPlace(SavedPlaceEntity(id,"trip",id,id,"地址",30.0,120.0))
    }
    @After fun close() { db.close() }
    @Test fun firstAndAppendedStopsHaveDefaultArrivalAndStay() = runBlocking {
        items.addItem("day", "a", 0); items.addItem("day", "b", 1)
        val day = items.observeDay("day").first()
        assertEquals(listOf(LocalTime.of(8,0), LocalTime.of(9,0)), day.items.map { it.arrivalTime })
        assertEquals(listOf(60,60), day.items.map { it.stayMinutes })
    }
    @Test fun insertionKeepsFullHourAndDoesNotMoveOrShortenNextStop() = runBlocking {
        val first=items.addItem("day","a",0)
        val next=items.addItem("day","b",1)
        items.updateTiming(first,LocalTime.of(8,0),60)
        items.updateTiming(next,LocalTime.of(9,15),30)
        val inserted=items.addItem("day","c",1)
        val day=items.observeDay("day").first()
        assertEquals(listOf(first,inserted,next),day.items.map { it.id })
        assertEquals(LocalTime.of(9,0),day.items[1].arrivalTime)
        assertEquals(60,day.items[1].stayMinutes)
        assertEquals(LocalTime.of(9,15),day.items[2].arrivalTime)
        assertEquals(30,day.items[2].stayMinutes)
    }
    @Test fun routeResultRefinesBatchButAnyManualEditProtectsTheNewStop() = runBlocking {
        val a=items.addItem("day","a",0); val b=items.addItem("day","b",1); val c=items.addItem("day","c",2)
        complete(a,b,900)
        assertEquals(listOf(LocalTime.of(8,0),LocalTime.of(9,15),LocalTime.of(10,15)),items.observeDay("day").first().items.map { it.arrivalTime })
        items.updateDetails(c,LocalTime.of(10,15),60,"已确认")
        complete(b,c,1800)
        assertEquals(LocalTime.of(10,15),items.observeDay("day").first().items.last().arrivalTime)
        assertEquals("已确认",items.observeDay("day").first().items.last().note)
    }
    @Test fun movingAnchorAwayAndBackUsesTheNewRouteAndRejectsTheOldCompletion() = runBlocking {
        val a = items.addItem("day", "a", 0)
        val b = items.addItem("day", "b", 1)
        val old = routes.observeDay("day").first().single()
        assertTrue(routes.claimIfVersionMatches(old.id, old.version))
        items.moveItem(a, "other", 0)
        items.moveItem(a, "day", 0)
        assertFalse(routes.completeIfVersionMatches(old.id, old.version,
            RouteResult(100, 7200, listOf(GeoPoint(30.0, 120.0), GeoPoint(30.0, 120.001)))))
        assertEquals(LocalTime.of(9, 0), day().last().arrivalTime)
        complete(a, b, 3600)
        assertEquals(LocalTime.of(10, 0), day().last().arrivalTime)
    }
    @Test fun insertionAtStartDoesNotRescheduleExistingStops() = runBlocking {
        val a = items.addItem("day", "a", 0)
        items.updateTiming(a, LocalTime.of(7, 30), 20)
        val b = items.addItem("day", "b", 0)
        assertEquals(listOf(b, a), day().map { it.id })
        assertEquals(listOf(LocalTime.of(8, 0), LocalTime.of(7, 30)), day().map { it.arrivalTime })
        assertEquals(listOf(60, 20), day().map { it.stayMinutes })
    }

    @Test fun reversedRouteCompletionStillRefinesTheWholeNewChain() = runBlocking {
        val a = items.addItem("day", "a", 0)
        val b = items.addItem("day", "b", 1)
        val c = items.addItem("day", "c", 2)
        complete(b, c, 1800)
        assertEquals(LocalTime.of(10, 30), day().last().arrivalTime)
        complete(a, b, 900)
        assertEquals(listOf(LocalTime.of(8, 0), LocalTime.of(9, 15), LocalTime.of(10, 45)), day().map { it.arrivalTime })
    }

    @Test fun manualSameValueAndChangeBackRemainProtectedFromLateRoutes() = runBlocking {
        val a = items.addItem("day", "a", 0)
        val b = items.addItem("day", "b", 1)
        val c = items.addItem("day", "c", 2)
        items.updateTiming(b, LocalTime.of(9, 0), 60)
        items.updateTiming(c, LocalTime.of(12, 0), 30)
        items.updateTiming(c, LocalTime.of(10, 0), 60)
        complete(a, b, 1800)
        complete(b, c, 1800)
        assertEquals(listOf(LocalTime.of(8, 0), LocalTime.of(9, 0), LocalTime.of(10, 0)), day().map { it.arrivalTime })
    }

    @Test fun movedStopReestimatesTimingButDeletionStillKeepsItsSuccessorTime() = runBlocking {
        val a = items.addItem("day", "a", 0)
        val b = items.addItem("day", "b", 1)
        val c = items.addItem("day", "c", 2)
        items.moveItem(b, "other", 0)
        assertEquals(LocalTime.of(8, 0), items.observeDay("other").first().items.single().arrivalTime)
        complete(a, c, 7200)
        assertEquals(LocalTime.of(11, 0), day().last().arrivalTime)
        items.deleteItem(a)
        assertEquals(LocalTime.of(11, 0), day().single().arrivalTime)
    }

    @Test fun idempotentRetryDoesNotResetUserTimingOrAddAnotherStop() = runBlocking {
        val result = items.addItemIdempotently("day", "a", 0, "request")
        items.updateTiming(result.itemId, LocalTime.of(14, 0), 25)
        val retry = items.addItemIdempotently("day", "a", 0, "request")
        assertFalse(retry.created)
        assertEquals(result.itemId, retry.itemId)
        assertEquals(LocalTime.of(14, 0), day().single().arrivalTime)
        assertEquals(25, day().single().stayMinutes)
    }

    @Test fun offlineEstimateIsAvailableImmediatelyThenActualTravelRefinesIt() = runBlocking {
        db.savedPlaceDao().insertPlace(SavedPlaceEntity("far", "trip", "far", "步行一公里", "地址", 30.009, 120.0))
        items = RoomItineraryRepository(db, db.itineraryEditingDao(), db.routeLegDao(),
            isOnline = { false }, recommendMode = { _, _, _ -> TransportMode.WALK })
        val a = items.addItem("day", "a", 0)
        val b = items.addItem("day", "far", 1)
        assertEquals(LocalTime.of(9, 18), day().last().arrivalTime)
        assertEquals(RouteStatus.WAITING_NETWORK, routes.observeDay("day").first().single().status)
        complete(a, b, 600)
        assertEquals(LocalTime.of(9, 10), day().last().arrivalTime)
    }

    @Test fun manuallySpecifiedTravelTakesPriorityAndOldVersionCannotChangeTiming() = runBlocking {
        val a = items.addItem("day", "a", 0)
        val b = items.addItem("day", "b", 1)
        val old = routes.observeDay("day").first().single()
        assertTrue(routes.claimIfVersionMatches(old.id, old.version))
        assertTrue(routes.updateDetails(old.id, TransportMode.DRIVE, 1200, "已确认交通", true))
        assertEquals(LocalTime.of(9, 20), day().last().arrivalTime)
        assertFalse(routes.completeIfVersionMatches(old.id, old.version,
            RouteResult(100, 7200, listOf(GeoPoint(30.0, 120.0), GeoPoint(30.0, 120.001)))))
        complete(a, b, 3600)
        assertEquals(LocalTime.of(9, 20), day().last().arrivalTime)
    }

    @Test fun midnightOverflowShowsWarningWithoutWrappingOrChangingNextStop() = runBlocking {
        val a = items.addItem("day", "a", 0)
        items.updateTiming(a, LocalTime.of(23, 0), 60)
        val b = items.addItem("day", "b", 1)
        assertNull(day().last().arrivalTime)
        assertEquals(60, day().last().stayMinutes)
        assertEquals("预计时间超出当天，请调整", day().last().timingWarning)
        complete(a, b, Int.MAX_VALUE)
        assertNull(day().last().arrivalTime)
        items.updateTiming(b, LocalTime.of(23, 30), 20)
        assertNull(day().last().timingWarning)
        assertEquals(LocalTime.of(23, 30), day().last().arrivalTime)
    }

    @Test fun unknownPreviousArrivalKeepsNewStopAdjustableInsteadOfInventingAClockTime() = runBlocking {
        val a = items.addItem("day", "a", 0)
        items.updateTiming(a, null, 30)
        items.addItem("day", "b", 1)
        assertNull(day().last().arrivalTime)
        assertEquals(60, day().last().stayMinutes)
        assertEquals("前站时间待定，请调整", day().last().timingWarning)
    }

    @Test fun stayPastMidnightIsKeptAtOneHourWithAWarning() = runBlocking {
        val a = items.addItem("day", "a", 0)
        items.updateTiming(a, LocalTime.of(22, 30), 60)
        items.addItem("day", "b", 1)
        assertEquals(LocalTime.of(23, 30), day().last().arrivalTime)
        assertEquals(60, day().last().stayMinutes)
        assertEquals("预计时间超出当天，请调整", day().last().timingWarning)
    }

    @Test fun automaticTimingAndManualProtectionSurviveDatabaseReopen() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "automatic-timing-reopen-${UUID.randomUUID()}"
        fun open() = Room.databaseBuilder(context, EasyTripDatabase::class.java, name).build()
        var persistent = open()
        try {
            persistent.tripDao().createTripWithDays(
                TripEntity("trip", "测试", TimeMode.DRAFT, null, TravelMode.FLEXIBLE, Instant.EPOCH, Instant.EPOCH),
                listOf(TripDayEntity("day", "trip", 0)),
            )
            persistent.savedPlaceDao().insertPlace(SavedPlaceEntity("a", "trip", "a", "同一地点", "地址", 30.0, 120.0))
            var repository = RoomItineraryRepository(persistent, persistent.itineraryEditingDao(), persistent.routeLegDao())
            val a = repository.addItem("day", "a", 0)
            val b = repository.addItem("day", "a", 1)
            val c = repository.addItem("day", "a", 2)
            repository.updateTiming(c, LocalTime.of(10, 0), 60)
            persistent.close()
            persistent = open()
            repository = RoomItineraryRepository(persistent, persistent.itineraryEditingDao(), persistent.routeLegDao())
            val routeRepository = RoomRouteLegRepository(persistent.routeLegDao())
            val leg = routeRepository.observeDay("day").first().single { it.fromItemId == a && it.toItemId == b }
            assertTrue(routeRepository.claimIfVersionMatches(leg.id, leg.version))
            assertTrue(routeRepository.completeIfVersionMatches(leg.id, leg.version,
                RouteResult(100, 900, listOf(GeoPoint(30.0, 120.0), GeoPoint(30.0, 120.001)))))
            assertEquals(listOf(LocalTime.of(8, 0), LocalTime.of(9, 15), LocalTime.of(10, 0)),
                repository.observeDay("day").first().items.map { it.arrivalTime })
        } finally {
            persistent.close()
            // This unique test-only database was created by this method.
            context.deleteDatabase(name)
        }
    }

    private suspend fun day() = items.observeDay("day").first().items
    private suspend fun complete(from:String,to:String,seconds:Int) {
        val leg=db.routeLegDao().legs("day").single { it.fromItemId==from && it.toItemId==to }
        assertTrue(routes.claimIfVersionMatches(leg.id,leg.version))
        assertTrue(routes.completeIfVersionMatches(leg.id,leg.version,RouteResult(100,seconds,listOf(GeoPoint(30.0,120.0),GeoPoint(30.0,120.001)))))
    }
}
