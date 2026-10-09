package com.yangchengwei.easytrip.expense.data

import androidx.room.withTransaction
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.expense.ExpenseCategory
import com.yangchengwei.easytrip.expense.ExpenseKey
import com.yangchengwei.easytrip.expense.ExpenseRecord
import com.yangchengwei.easytrip.expense.ExpenseRepository
import com.yangchengwei.easytrip.expense.ExpenseSourceKind
import com.yangchengwei.easytrip.expense.expenseDate
import com.yangchengwei.easytrip.expense.ExpenseDraftRow
import com.yangchengwei.easytrip.expense.ExpenseDraftValidation
import com.yangchengwei.easytrip.expense.PlaceExpenseInput
import com.yangchengwei.easytrip.expense.expenseInput
import com.yangchengwei.easytrip.expense.requireSummableExpense
import com.yangchengwei.easytrip.expense.validateExpenseDraft
import com.yangchengwei.easytrip.itinerary.data.TripDayReadSnapshot
import java.time.Clock
import kotlinx.coroutines.flow.map

class RoomExpenseRepository(
    private val database: EasyTripDatabase,
    private val clock: Clock = Clock.systemUTC(),
) : ExpenseRepository {
    override fun observeRecords() = database.expenseDao().observeSnapshots().map(::expenseRecords)

    override suspend fun updatePlaceExpense(itemId: String, expected: ExpenseRecord, value: PlaceExpenseInput) =
        database.withTransaction {
            verifyExpected(itemId, expected)
            require(value.id == expected.key.id) { "费用ID不匹配" }
            val originals = database.itineraryEditingDao().placeExpenses(itemId).map { it.input() }
            val validation = validateExpenseDraft(listOf(
                ExpenseDraftRow("edit", value.id, expenseInput(value.cents), value.category, value.note.orEmpty()),
            ), originals)
            require(validation is ExpenseDraftValidation.Valid) {
                (validation as ExpenseDraftValidation.Invalid).message
            }
            val normalized = validation.expenses.single()
            requireSummableExpense(
                database.itineraryEditingDao().otherExpenses(expected.tripId, itemId) +
                    originals.filterNot { it.id == expected.key.id }.map { it.cents },
                normalized.cents,
            )
            check(database.expenseDao().update(expected.key.id, itemId, normalized.cents,
                normalized.category?.storageKey, normalized.note) == 1)
            check(database.tripDao().touch(expected.tripId, clock.instant()) == 1)
        }

    override suspend fun deletePlaceExpense(itemId: String, expected: ExpenseRecord) = database.withTransaction {
        verifyExpected(itemId, expected)
        check(database.expenseDao().delete(expected.key.id, itemId) == 1)
        check(database.tripDao().touch(expected.tripId, clock.instant()) == 1)
    }

    private suspend fun verifyExpected(itemId: String, expected: ExpenseRecord) {
        require(expected.key.kind == ExpenseSourceKind.PLACE && expected.sourceId == itemId) { "费用来源不匹配" }
        val current = expenseRecords(database.expenseDao().tripSnapshot(expected.tripId))
            .singleOrNull { it.key == expected.key }
        check(current == expected) { "费用记录或来源已变更，请重新读取" }
    }
}

internal fun expenseRecords(trips: List<ExpenseTripReadSnapshot>): List<ExpenseRecord> = trips.flatMap { trip ->
    trip.days.sortedWith(compareBy<TripDayReadSnapshot> { it.day.position }
        .thenBy { it.day.id }).flatMapIndexed { index, snapshot ->
        val date = expenseDate(trip.trip.startDate, index)
        val items = snapshot.items.associateBy { it.id }
        val places = snapshot.places.associateBy { it.id }
        fun name(itemId: String) = places.getValue(items.getValue(itemId).savedPlaceId).name
        snapshot.expenses.sortedWith(compareBy<PlaceExpenseEntity> { it.itineraryItemId }
            .thenBy { it.position }.thenBy { it.id }).map { expense ->
            ExpenseRecord(
                ExpenseKey(ExpenseSourceKind.PLACE, expense.id), trip.trip.id, snapshot.day.id,
                expense.itineraryItemId, name(expense.itineraryItemId), date, expense.cents,
                ExpenseCategory.fromStorageKey(expense.category), expense.note, trip.trip.name, index + 1,
            )
        } + snapshot.legs.sortedBy { it.id }.mapNotNull { leg ->
            leg.expenseCents?.let { cents ->
                ExpenseRecord(
                    ExpenseKey(ExpenseSourceKind.ROUTE, leg.id), trip.trip.id, snapshot.day.id,
                    leg.id, "${name(leg.fromItemId)} → ${name(leg.toItemId)}", date, cents,
                    ExpenseCategory.TRANSPORT, leg.note, trip.trip.name, index + 1,
                )
            }
        }
    }
}
