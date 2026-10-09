package com.yangchengwei.easytrip.itinerary.ui

import com.yangchengwei.easytrip.core.model.TransportMode
import java.time.LocalTime
import com.yangchengwei.easytrip.expense.*

data class ItineraryEditDraft(
    val itemId: String,
    val arrivalTimeText: String,
    val stayMinutesText: String,
    val noteText: String = "",
    val expenseText: String = "",
    val placeId: String? = null,
    val placeName: String = "",
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val generation: Long = 0,
    val originalArrivalTimeText: String = arrivalTimeText,
    val originalStayMinutesText: String = stayMinutesText,
    val originalNoteText: String = noteText,
    val originalExpenses: List<PlaceExpenseInput> = emptyList(),
    val expenses: List<ExpenseDraftRow> = listOf(ExpenseDraftRow("first")),
    val expandedExpenseKey: String? = null,
    val showAllExpenses: Boolean = false,
    val expenseRemoval: ExpenseDraftRemoval? = null,
    val nextExpenseNumber: Int = 1,
    val showDiscardConfirmation: Boolean = false,
    val scheduleAfterDiscard: Boolean = false,
    val sourceMissing: Boolean = false,
) {
    val expenseValidation get() = validateExpenseDraft(expenses, originalExpenses)
    val isDirty: Boolean get() = arrivalTimeText != originalArrivalTimeText ||
        stayMinutesText != originalStayMinutesText || noteText != originalNoteText ||
        when (val validated = expenseValidation) {
            is ExpenseDraftValidation.Invalid -> true
            is ExpenseDraftValidation.Valid -> validated.expenses != originalExpenses
        }
    val arrivalTime: LocalTime?
        get() = arrivalTimeText.takeIf(String::isNotBlank)?.let { runCatching { LocalTime.parse(it) }.getOrNull() }

    val stayMinutes: Int?
        get() = stayMinutesText.takeIf(String::isNotBlank)?.toIntOrNull()

    val isValid: Boolean
        get() {
            val parsedMinutes = stayMinutes
            return !sourceMissing && expenseValidation is ExpenseDraftValidation.Valid && (arrivalTimeText.isBlank() || arrivalTime != null) &&
                (stayMinutesText.isBlank() || parsedMinutes != null && parsedMinutes >= 0)
        }
}

data class CrossDayMoveDraft(
    val itemId: String,
    val targetDayId: String? = null,
    val isMoving: Boolean = false,
    val moveError: String? = null,
    val generation: Long = 0,
)

data class RouteModeEditDraft(
    val legId: String,
    val selectedMode: TransportMode,
    val selectedModeOverride: TransportMode? = null,
    val originalSelectedModeOverride: TransportMode? = null,
    val durationMinutesText: String = "",
    val noteText: String = "",
    val expenseText: String = "",
    val plannedDurationSeconds: Int? = null,
    val originalDurationOverrideSeconds: Int? = null,
    val isDurationEdited: Boolean = false,
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val generation: Long = 0,
    val fromPlaceName: String = "",
    val toPlaceName: String = "",
    val distanceMeters: Int? = null,
) {
    val durationOverrideSeconds: Int?
        get() = if (!isDurationEdited) originalDurationOverrideSeconds else durationMinutesText.takeIf(String::isNotBlank)?.toIntOrNull()?.times(60)

    val isValid: Boolean
        get() = validExpense(expenseText) && (durationMinutesText.isBlank() || durationMinutesText.toIntOrNull() in 1..1_440)
}


data class ItineraryDeleteConfirmation(
    val itemId: String,
    val placeName: String,
    val isDeleting: Boolean = false,
    val deleteError: String? = null,
    val generation: Long = 0,
)
