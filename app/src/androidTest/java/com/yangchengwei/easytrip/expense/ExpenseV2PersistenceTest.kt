package com.yangchengwei.easytrip.expense

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.expense.data.RoomExpenseRepository
import com.yangchengwei.easytrip.itinerary.data.RoomItineraryRepository
import com.yangchengwei.easytrip.place.data.SavedPlaceEntity
import com.yangchengwei.easytrip.trip.data.RoomTripRepository
import com.yangchengwei.easytrip.trip.domain.CreateTrip
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class ExpenseV2PersistenceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val db = Room.inMemoryDatabaseBuilder(context, EasyTripDatabase::class.java).build()
    private val items = RoomItineraryRepository(db, db.itineraryEditingDao(), db.routeLegDao(), confirmExpensePeriod = { true })
    private val trips = RoomTripRepository(db.tripDao(), database = db, confirmExpensePeriod = { true })
    private val expenses = RoomExpenseRepository(db)
    @After fun close() { db.close() }

    private suspend fun seed(): Pair<String, List<String>> {
        val trip = trips.createTrip(CreateTrip("跨年旅行", 2, startDate = LocalDate.of(2025, 12, 31)))
        val days = trips.observeTrip(trip).first()!!.days.map { it.id }
        listOf("hotel", "park").forEach {
            db.savedPlaceDao().insertPlace(SavedPlaceEntity(it, trip, it, it, "地址", 30.0, 120.0))
        }
        return trip to days
    }

    @Test fun savesThreeExpensesAndPlaceDetailsTogetherWithoutCopyingToRepeatedVisit() = runBlocking {
        val (trip, days) = seed()
        val item = items.addItem(days[1], "hotel", 0)
        val values = listOf(
            PlaceExpenseInput(null, 60000, ExpenseCategory.LODGING, "住宿"),
            PlaceExpenseInput(null, 12000, ExpenseCategory.FOOD, "晚餐"),
            PlaceExpenseInput(null, 4000, ExpenseCategory.FOOD, "早餐"),
        )
        items.saveDetailsWithExpenses(item, LocalTime.NOON, 60, " 地点备注 ", emptyList(), values)
        val records = expenses.observeRecords().first()
        assertEquals(listOf(60000L, 12000L, 4000L), records.map { it.cents })
        assertEquals(3, records.map { it.key }.toSet().size)
        assertTrue(records.all { it.date == LocalDate.of(2026, 1, 1) })
        val saved = items.observeDay(days[1]).first().items.single()
        assertEquals(LocalTime.NOON, saved.arrivalTime)
        assertEquals("地点备注", saved.note)
        assertEquals(76000L, saved.expenseCents)
        assertEquals(3, saved.expenses.size)
        assertEquals(3, trips.observeTrips().first().single { it.id == trip }.recordedExpenseCount)
        items.addItem(days[0], "hotel", 0)
        assertTrue(items.observeDay(days[0]).first().items.single().expenses.isEmpty())
        assertEquals(records, expenses.observeRecords().first())
    }

    @Test fun independentlyEditsAndDeletesOnlyTheRequestedExpenseAndRejectsStaleSnapshots() = runBlocking {
        val (_, days) = seed()
        val item = items.addItem(days[0], "hotel", 0)
        items.saveDetailsWithExpenses(item, LocalTime.NOON, 60, "原备注", emptyList(), listOf(
            PlaceExpenseInput(null, 12000, ExpenseCategory.FOOD, "晚餐"),
            PlaceExpenseInput(null, 4000, ExpenseCategory.FOOD, "早餐"),
        ))
        val before = expenses.observeRecords().first()
        val dinner = before.single { it.note == "晚餐" }
        expenses.updatePlaceExpense(item, dinner, PlaceExpenseInput(dinner.key.id, 12500, ExpenseCategory.FOOD, "两人晚餐"))
        val updated = expenses.observeRecords().first()
        assertEquals(12500L, updated.single { it.key == dinner.key }.cents)
        assertEquals(before.single { it.note == "早餐" }, updated.single { it.note == "早餐" })
        rejects { expenses.deletePlaceExpense(item, dinner) }
        assertEquals(updated, expenses.observeRecords().first())
        expenses.deletePlaceExpense(item, updated.single { it.key == dinner.key })
        assertEquals(listOf(before.single { it.note == "早餐" }), expenses.observeRecords().first())
        val place = items.observeDay(days[0]).first().items.single()
        assertEquals(LocalTime.NOON, place.arrivalTime)
        assertEquals(60, place.stayMinutes)
        assertEquals("原备注", place.note)
    }

    private suspend fun rejects(action: suspend () -> Unit) {
        try { action(); fail("必须拒绝无效或过期保存") }
        catch (_: IllegalArgumentException) { }
        catch (_: IllegalStateException) { }
    }

    @Test fun rejectsForeignIdsStaleEditsRepeatedSubmissionAndOverflowWithoutChangingPlace() = runBlocking {
        val (_, days) = seed()
        val hotel = items.addItem(days[0], "hotel", 0)
        val park = items.addItem(days[1], "park", 0)
        val first = listOf(PlaceExpenseInput(null, 100, ExpenseCategory.FOOD, null))
        items.saveDetailsWithExpenses(hotel, LocalTime.NOON, 60, "原备注", emptyList(), first)
        val original = items.observeDay(days[0]).first().items.single()
        val record = expenses.observeRecords().first().single()
        rejects { items.saveDetailsWithExpenses(hotel, LocalTime.MIDNIGHT, 10, "重复", emptyList(), first) }
        rejects { items.saveDetailsWithExpenses(park, LocalTime.MIDNIGHT, 10, "跨地点", emptyList(), original.expenses) }
        rejects { expenses.updatePlaceExpense(park, record, original.expenses.single()) }
        assertTrue(items.observeDay(days[1]).first().items.single().expenses.isEmpty())
        assertEquals(original, items.observeDay(days[0]).first().items.single())
        items.saveDetailsWithExpenses(park, null, null, null, emptyList(),
            listOf(PlaceExpenseInput(null, Long.MAX_VALUE - 100, ExpenseCategory.OTHER, null)))
        rejects {
            items.saveDetailsWithExpenses(hotel, LocalTime.MIDNIGHT, 10, "溢出", original.expenses,
                listOf(original.expenses.single().copy(cents = 101)))
        }
        assertEquals(original, items.observeDay(days[0]).first().items.single())
        expenses.updatePlaceExpense(hotel, record, original.expenses.single().copy(cents = 99))
        rejects { items.saveDetailsWithExpenses(hotel, LocalTime.MIDNIGHT, 10, "过期", original.expenses, original.expenses) }
        val updated = items.observeDay(days[0]).first().items.single()
        assertEquals(99L, updated.expenseCents)
        assertEquals(LocalTime.NOON, updated.arrivalTime)
        assertEquals("原备注", updated.note)
        rejects { items.updateDetailsWithExpense(park, null, null, null, Long.MAX_VALUE) }
    }

    @Test fun databaseInsertFailureRollsBackTimingNotesAndAllExpenseRows() = runBlocking {
        val (_, days) = seed()
        val hotel = items.addItem(days[0], "hotel", 0)
        val park = items.addItem(days[1], "park", 0)
        items.saveDetailsWithExpenses(hotel, LocalTime.NOON, 60, "原备注", emptyList(),
            listOf(PlaceExpenseInput(null, 60000, ExpenseCategory.LODGING, null)))
        items.saveDetailsWithExpenses(park, null, null, null, emptyList(),
            listOf(PlaceExpenseInput(null, 0, ExpenseCategory.ATTRACTION, null)))
        val before = items.observeDay(days[0]).first().items.single()
        val records = expenses.observeRecords().first()
        val occupiedId = records.single { it.sourceId == park }.key.id
        val collision = RoomItineraryRepository(db, db.itineraryEditingDao(), db.routeLegDao(),
            expenseIdFactory = { occupiedId })
        try {
            collision.saveDetailsWithExpenses(hotel, LocalTime.MIDNIGHT, 10, "不应保存", before.expenses,
                before.expenses + PlaceExpenseInput(null, 12000, ExpenseCategory.FOOD, null))
            fail("数据库冲突必须回滚")
        } catch (_: android.database.sqlite.SQLiteConstraintException) { }
        assertEquals(before, items.observeDay(days[0]).first().items.single())
        assertEquals(records, expenses.observeRecords().first())
    }

    @Test fun zeroAndRepeatedCategoriesRequireExactDeletionConsentAndDeletedSourcesRejectWrites() = runBlocking {
        val (trip, days) = seed()
        val hotel = items.addItem(days[0], "hotel", 0)
        val park = items.addItem(days[1], "park", 0)
        items.saveDetailsWithExpenses(hotel, LocalTime.NOON, 60, null, emptyList(), listOf(
            PlaceExpenseInput(null, 0, ExpenseCategory.FOOD, "赠送早餐"),
            PlaceExpenseInput(null, 4000, ExpenseCategory.FOOD, "晚餐"),
        ))
        val before = expenses.observeRecords().first()
        val original = items.observeDay(days[0]).first().items.single()
        rejects { items.updateDetailsWithExpense(hotel, null, null, null, 8000) }
        try { items.deleteItem(hotel); fail("需要确认每一笔费用") }
        catch (required: ExpenseRemovalRequired) {
            assertEquals(before.map { RecordedExpense("地点", it.key.id, it.cents) }.toSet(), required.entries.toSet())
        }
        assertEquals(before, expenses.observeRecords().first())
        val confirmed = RoomItineraryRepository(db, db.itineraryEditingDao(), db.routeLegDao(),
            confirmExpenseRemoval = { true })
        confirmed.deleteItem(hotel)
        assertTrue(expenses.observeRecords().first().isEmpty())
        assertEquals(park, items.observeDay(days[1]).first().items.single().id)
        assertEquals(0, trips.observeTrips().first().single { it.id == trip }.recordedExpenseCount)
        rejects { expenses.updatePlaceExpense(hotel, before.first(), original.expenses.first()) }
        rejects { items.saveDetailsWithExpenses(hotel, null, null, null, original.expenses, original.expenses) }
    }

    @Test fun movingAndChangingDatesOnlyReassignRecordsWithoutChangingIdsOrAmounts() = runBlocking {
        val (trip, days) = seed()
        val hotel = items.addItem(days[0], "hotel", 0)
        items.saveDetailsWithExpenses(hotel, null, null, null, emptyList(),
            listOf(PlaceExpenseInput(null, 60000, ExpenseCategory.LODGING, null)))
        val before = expenses.observeRecords().first().single()
        items.appendItem(hotel, days[1])
        val moved = expenses.observeRecords().first().single()
        assertEquals(before.copy(dayId = days[1], date = LocalDate.of(2026, 1, 1), dayNumber = 2), moved)
        trips.setStartDate(trip, LocalDate.of(2026, 1, 1))
        val dated = expenses.observeRecords().first().single()
        assertEquals(moved.copy(date = LocalDate.of(2026, 1, 2)), dated)
        trips.setStartDate(trip, null)
        assertEquals(dated.copy(date = null), expenses.observeRecords().first().single())
        assertEquals(60000L, trips.observeTrips().first().single().expenseCents)
    }

    @Test fun compatibilityApiCannotCreateUnclassifiedExpenses() = runBlocking {
        val (_, days) = seed()
        val hotel = items.addItem(days[0], "hotel", 0)
        val before = items.observeDay(days[0]).first().items.single()
        rejects { items.updateDetailsWithExpense(hotel, LocalTime.NOON, 60, "不应保存", 60000) }
        assertEquals(before, items.observeDay(days[0]).first().items.single())
        assertTrue(expenses.observeRecords().first().isEmpty())
    }
}
