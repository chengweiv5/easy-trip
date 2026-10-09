package com.yangchengwei.easytrip.expense

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.expense.data.RoomExpenseRepository
import com.yangchengwei.easytrip.itinerary.data.RoomItineraryRepository
import com.yangchengwei.easytrip.place.data.SavedPlaceEntity
import com.yangchengwei.easytrip.trip.data.RoomTripRepository
import com.yangchengwei.easytrip.trip.domain.CreateTrip
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

class ExpenseMutationConsistencyTest {
    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), EasyTripDatabase::class.java).build()
    private val expenses = RoomExpenseRepository(db)
    private val items = RoomItineraryRepository(db, db.itineraryEditingDao(), db.routeLegDao())
    @After fun close() = db.close()
    private suspend fun seed(): String {
        val trips = RoomTripRepository(db.tripDao(), database = db)
        val id = trips.createTrip(CreateTrip("厦门跨年",3,startDate=LocalDate.of(2025,12,31)))
        val days = trips.observeTrip(id).first()!!.days
        listOf(30000L,45000L,25000L).forEachIndexed { index, cents ->
            val place = "p$index"
            db.savedPlaceDao().insertPlace(SavedPlaceEntity(place,id,place,place,"地址",30.0,120.0))
            val item = items.addItem(days[index].id,place,0)
            items.saveDetailsWithExpenses(item,null,null,null,emptyList(),listOf(PlaceExpenseInput(null,cents,ExpenseCategory.FOOD,null)))
        }
        return id
    }
    @Test fun changedAmountWhileConfirmingRequiresFreshConfirmation() = runBlocking {
        val id = seed()
        var prompts = 0
        val trips = RoomTripRepository(db.tripDao(), database=db, confirmExpensePeriod={ preview ->
            prompts++
            if (prompts == 1) {
                val old = expenses.observeRecords().first().first()
                expenses.updatePlaceExpense(old.sourceId,old,PlaceExpenseInput(old.key.id,31000,old.category,old.note))
            } else assertEquals(31000L, preview.changes.single().cents)
            true
        })
        trips.setStartDate(id,LocalDate.of(2026,1,1))
        assertEquals(2,prompts)
        assertEquals(101000L,reviewExpenses(expenses.observeRecords().first(),ExpensePeriod.Year(2026)).cents)
    }

    @Test fun moveDayAndPlaceAcrossYearRequireConfirmationAndPreserveIds() = runBlocking {
        val id = seed()
        var prompts = 0
        val trips = RoomTripRepository(db.tripDao(),database=db,confirmExpensePeriod={ prompts++; true })
        val days = trips.observeTrip(id).first()!!.days
        val before = expenses.observeRecords().first()
        val moving = RoomItineraryRepository(db,db.itineraryEditingDao(),db.routeLegDao(),confirmExpensePeriod={ prompts++; true })
        moving.appendItem(before.first().sourceId,days[1].id)
        assertEquals(1,prompts)
        trips.moveDay(id,days[1].id,0)
        assertEquals(2,prompts)
        val after = expenses.observeRecords().first()
        assertEquals(before.map { it.key }.toSet(),after.map { it.key }.toSet())
        assertEquals(75000L,reviewExpenses(after,ExpensePeriod.Year(2025)).cents)
        assertEquals(100000L,reviewExpenses(after,ExpensePeriod.All).cents)
    }

    @Test fun zeroAndUndatedChangesAreConfirmedButSameMonthIsNot() = runBlocking {
        val id = seed()
        var prompts = 0
        val trips = RoomTripRepository(db.tripDao(),database=db,confirmExpensePeriod={ prompts++; true })
        trips.setStartDate(id,LocalDate.of(2025,12,30))
        assertEquals(1,prompts)
        trips.setStartDate(id,LocalDate.of(2025,12,29))
        assertEquals(2,prompts)
        trips.setStartDate(id,LocalDate.of(2025,12,28))
        assertEquals(2,prompts)
        trips.setStartDate(id,null)
        assertEquals(3,prompts)
        assertTrue(expenses.observeRecords().first().all { it.date == null })
    }

    @Test fun dateChangeRollsBackBeforeConfirmationAndCancelPreservesEverything() = runBlocking {
        val id = seed()
        val before = expenses.observeRecords().first()
        var preview: ExpensePeriodPreview? = null
        val trips = RoomTripRepository(db.tripDao(),database=db,confirmExpensePeriod={ value ->
            preview = value
            assertEquals(before, expenses.observeRecords().first())
            false
        })
        try { trips.setStartDate(id,LocalDate.of(2026,1,1)); fail("取消不得提交") } catch (_: ExpenseRemovalCancelled) { }
        assertEquals(before,expenses.observeRecords().first())
        assertEquals(30000L,preview!!.changes.single().cents)
        assertEquals(LocalDate.of(2025,12,31),preview!!.changes.single().beforeDate)
        val accepted = RoomTripRepository(db.tripDao(),database=db,confirmExpensePeriod={ true })
        accepted.setStartDate(id,LocalDate.of(2026,1,1))
        val after = expenses.observeRecords().first()
        assertEquals(before.map { it.key },after.map { it.key })
        assertEquals(0L,reviewExpenses(after,ExpensePeriod.Year(2025)).cents)
        assertEquals(100000L,reviewExpenses(after,ExpensePeriod.Year(2026)).cents)
    }
}
