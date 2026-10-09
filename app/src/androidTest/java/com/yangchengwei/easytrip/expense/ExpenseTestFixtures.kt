package com.yangchengwei.easytrip.expense

import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.itinerary.domain.ItineraryRepository
import java.time.LocalTime
import kotlinx.coroutines.flow.first

/** Older scenarios still choose one explicit category rather than creating new legacy records. */
internal suspend fun ItineraryRepository.saveSingleExpenseForTest(
    database: EasyTripDatabase, itemId: String, arrival: LocalTime?, stay: Int?, note: String?, cents: Long?,
) {
    val dayId = requireNotNull(database.itineraryEditingDao().item(itemId)).tripDayId
    val current = observeDay(dayId).first().items.single { it.id == itemId }.expenses
    val value = cents?.let {
        PlaceExpenseInput(current.singleOrNull()?.id, it, ExpenseCategory.OTHER, null)
    }
    saveDetailsWithExpenses(itemId, arrival, stay, note, current, listOfNotNull(value))
}
