package com.yangchengwei.easytrip.trip.ui

import com.yangchengwei.easytrip.core.model.TravelMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class CreateTripUiState(
    val name: String = "",
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val travelMode: TravelMode = TravelMode.FLEXIBLE,
    val nameError: String? = null,
    val dateError: String? = null,
    val isSubmitting: Boolean = false,
    val submitError: String? = null,
    val requestId: String? = null,
) {
    private val rawDayCount: Long?
        get() = if (startDate != null && endDate != null && !endDate.isBefore(startDate)) {
            ChronoUnit.DAYS.between(startDate, endDate) + 1
        } else {
            null
        }

    val dayCount: Int?
        get() = rawDayCount?.takeIf { it in 1L..30L }?.toInt()

    val hasDateRangeOverflow: Boolean
        get() = rawDayCount != null && rawDayCount !in 1L..30L
}

sealed interface CreateTripAction {
    data object Back : CreateTripAction
    data class NameChanged(val value: String) : CreateTripAction
    data class DateRangeChanged(val startDate: LocalDate, val endDate: LocalDate) : CreateTripAction
    data class TravelModeChanged(val value: TravelMode) : CreateTripAction
    data object Submit : CreateTripAction
}

sealed interface CreateTripEffect {
    data object NavigateBack : CreateTripEffect
    data class OpenWorkspace(val tripId: String) : CreateTripEffect
}
