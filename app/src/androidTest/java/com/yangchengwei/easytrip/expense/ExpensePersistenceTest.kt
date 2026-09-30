package com.yangchengwei.easytrip.expense

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.core.model.*
import com.yangchengwei.easytrip.itinerary.data.RoomItineraryRepository
import com.yangchengwei.easytrip.itinerary.domain.ItineraryTiming
import com.yangchengwei.easytrip.itinerary.domain.ItineraryTimingChange
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.data.SavedPlaceEntity
import com.yangchengwei.easytrip.route.data.RoomRouteLegRepository
import com.yangchengwei.easytrip.route.domain.RouteResult
import com.yangchengwei.easytrip.trip.data.RoomTripRepository
import com.yangchengwei.easytrip.trip.domain.CreateTrip
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class ExpensePersistenceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "v170-expense-persistence-test"
    private var db = Room.databaseBuilder(context, EasyTripDatabase::class.java, name).build()
    private var confirmed = false
    private val prompts = mutableListOf<List<RecordedExpense>>()
    private fun items() = RoomItineraryRepository(db, db.itineraryEditingDao(), db.routeLegDao(),
        confirmExpenseRemoval = { entries ->
            // A fresh read here proves the destructive transaction rolled back before asking.
            assertTrue(db.tripDao().observeTrips().first().isNotEmpty())
            prompts += entries
            confirmed
        })
    private fun trips() = RoomTripRepository(db.tripDao(), database = db)
    private fun routes() = RoomRouteLegRepository(db.routeLegDao())
    @After fun close() { db.close(); context.deleteDatabase(name) }
    private suspend fun seed(): Pair<String, List<String>> {
        val id = trips().createTrip(CreateTrip("花费测试", 2, startDate = LocalDate.of(2026, 10, 1)))
        val days = trips().observeTrip(id).first()!!.days.map { it.id }
        listOf("a", "b", "c").forEachIndexed { i, place ->
            db.savedPlaceDao().insertPlace(SavedPlaceEntity(place, id, place, place, place, 30.0 + i, 120.0))
        }
        return id to days
    }

    @Test fun tripSnapshotsNeverCountMovedExpenseTwice() = runBlocking {
        val (trip, days) = seed()
        val repo = items()
        val a = repo.addItem(days[0], "a", 0)
        repo.updateDetailsWithExpense(a, null, null, null, Long.MAX_VALUE)
        val stream = requireNotNull(repo.observeTripDays(trip))
        var observations = 0
        val collector = launch(start = CoroutineStart.UNDISPATCHED) {
            stream.collect { snapshots ->
                assertEquals(Long.MAX_VALUE, expenseSummary(snapshots.flatMap { it.itinerary.items.map { item -> item.expenseCents } + it.legs.map { leg -> leg.expenseCents } }).cents)
                assertEquals(1, snapshots.sumOf { it.itinerary.items.size })
                observations++
            }
        }
        repeat(20) { repo.appendItem(a, days[(it + 1) % 2]); yield() }
        assertTrue(observations > 0)
        collector.cancel()
    }

    @Test fun rejectsOverflowAcrossDaysAndTransportWithoutPersistingInvalidTotals() = runBlocking {
        val (trip, days) = seed()
        val repo = items()
        val a = repo.addItem(days[0], "a", 0)
        val b = repo.addItem(days[1], "b", 0)
        repo.addItem(days[1], "c", 1)
        repo.updateDetailsWithExpense(a, null, null, null, Long.MAX_VALUE - 1)
        val leg = routes().observeDay(days[1]).first().single()
        routes().updateDetailsWithExpense(leg.id, null, null, null, true, 1)
        try { repo.updateDetailsWithExpense(b, null, null, "must rollback", 1); fail("must reject overflow") } catch (_: IllegalArgumentException) { }
        assertNull(repo.observeDay(days[1]).first().items.first().expenseCents)
        assertNull(repo.observeDay(days[1]).first().items.first().note)
        try { routes().updateDetailsWithExpense(leg.id, null, null, null, true, 2); fail("must reject overflow") } catch (_: IllegalArgumentException) { }
        assertEquals(1L, routes().observeDay(days[1]).first().single().expenseCents)
        assertEquals(Long.MAX_VALUE, trips().observeTrips().first().single().expenseCents)
        repo.updateDetailsWithExpense(a, null, null, null, null)
        repo.updateDetailsWithExpense(b, null, null, null, 1)
        assertEquals(2L, trips().observeTrips().first().single().expenseCents)
    }

    @Test fun expenseSurvivesReopenAppendTimingAndDatedDayReorder() = runBlocking {
        val (trip, days) = seed()
        val repo = items()
        val a = repo.addItem(days[0], "a", 0)
        val b = repo.addItem(days[1], "b", 0)
        repo.updateDetailsWithExpense(a, LocalTime.of(8, 0), 60, "门票", 8000)
        repo.updateDetailsWithExpense(b, null, null, null, 0)
        repo.appendItem(a, days[1])
        assertEquals(listOf(b, a), repo.observeDay(days[1]).first().items.map { it.id })
        assertEquals(8000L, repo.observeDay(days[1]).first().items.last().expenseCents)
        repo.updateTiming(a, LocalTime.of(9, 0), 90)
        val leg = routes().observeDay(days[1]).first().single()
        assertTrue(routes().updateDetailsWithExpense(leg.id, null, 1200, "车费", true, 3650))
        assertTrue(routes().claimIfVersionMatches(leg.id, leg.version))
        assertTrue(routes().completeIfVersionMatches(leg.id, leg.version,
            RouteResult(100, 60, listOf(GeoPoint(30.0,120.0), GeoPoint(31.0,120.0)))))
        assertEquals(3650L, routes().observeDay(days[1]).first().single().expenseCents)
        val before = trips().observeTrips().first().single()
        assertEquals(11650L, before.expenseCents)
        assertEquals(3, before.recordedExpenseCount)
        trips().moveDay(trip, days[1], 0)
        assertEquals(listOf(days[1], days[0]), trips().observeTrip(trip).first()!!.days.map { it.id })
        assertEquals(LocalDate.of(2026, 10, 1), trips().observeTrip(trip).first()!!.startDate)
        assertEquals(11650L, trips().observeTrips().first().single().expenseCents)
        db.close()
        db = Room.databaseBuilder(context, EasyTripDatabase::class.java, name).build()
        assertEquals(11650L, trips().observeTrips().first().single().expenseCents)
        val restored = items().observeDay(days[1]).first().items.last()
        assertEquals(LocalTime.of(9, 0), restored.arrivalTime)
        assertEquals(90, restored.stayMinutes)
        assertEquals("门票", restored.note)
        val repeated = items().addItem(days[0], "a", 0)
        assertNull(items().observeDay(days[0]).first().items.single { it.id == repeated }.expenseCents)
    }

    @Test fun cancelledStructuralEditRollsBackAndConfirmedEditDoesNotReassignRouteExpense() = runBlocking {
        val (_, days) = seed()
        val repo = items()
        val a = repo.addItem(days[0], "a", 0)
        val b = repo.addItem(days[0], "b", 1)
        repo.updateDetailsWithExpense(a, null, null, null, 8000)
        val leg = routes().observeDay(days[0]).first().single()
        routes().updateDetailsWithExpense(leg.id, null, null, null, true, 3650)
        try { repo.moveItem(a, days[0], 1); fail("must cancel") } catch (_: IllegalStateException) { }
        assertEquals(listOf(a, b), repo.observeDay(days[0]).first().items.map { it.id })
        assertEquals(3650L, routes().observeDay(days[0]).first().single().expenseCents)
        assertEquals(listOf(RecordedExpense("交通", leg.id, 3650)), prompts.single())
        confirmed = true
        repo.moveItem(a, days[0], 1)
        assertEquals(listOf(b, a), repo.observeDay(days[0]).first().items.map { it.id })
        assertNull(routes().observeDay(days[0]).first().single().expenseCents)
        assertEquals(8000L, repo.observeDay(days[0]).first().items.last().expenseCents)
    }

    @Test fun deletingOccurrenceConfirmsBothItsExpenseAndCascadingRouteExpense() = runBlocking {
        val (_, days) = seed()
        val repo = items()
        val a = repo.addItem(days[0], "a", 0)
        repo.addItem(days[0], "b", 1)
        repo.updateDetailsWithExpense(a, null, null, null, 0)
        val leg = routes().observeDay(days[0]).first().single()
        routes().updateDetailsWithExpense(leg.id, null, null, null, true, 1200)
        try { repo.deleteItem(a); fail("must cancel") } catch (_: IllegalStateException) { }
        assertEquals(2, repo.observeDay(days[0]).first().items.size)
        assertEquals(2, prompts.single().size)
        confirmed = true
        repo.deleteItem(a)
        assertEquals(1, repo.observeDay(days[0]).first().items.size)
        assertTrue(routes().observeDay(days[0]).first().isEmpty())
    }
}
