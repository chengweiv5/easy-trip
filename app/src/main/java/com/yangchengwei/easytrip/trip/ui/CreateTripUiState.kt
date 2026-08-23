package com.yangchengwei.easytrip.trip.ui

import com.yangchengwei.easytrip.core.model.TravelMode
import java.time.LocalDate

data class CreateTripUiState(
    val visible: Boolean = false,
    val name: String = "",
    val dayCount: String = "",
    val timeMode: CreateTimeMode = CreateTimeMode.DRAFT,
    val startDate: LocalDate? = null,
    val travelMode: TravelMode = TravelMode.FLEXIBLE,
    val nameError: String? = null,
    val dayCountError: String? = null,
    val dateError: String? = null,
    val isSubmitting: Boolean = false,
    val submitError: String? = null,
    val requestId: String? = null,
)

sealed interface CreateTripAction {
    data object Dismiss : CreateTripAction
    data class NameChanged(val value: String) : CreateTripAction
    data class DayCountChanged(val value: String) : CreateTripAction
    data class TimeModeChanged(val value: CreateTimeMode) : CreateTripAction
    data class StartDateChanged(val value: LocalDate) : CreateTripAction
    data class TravelModeChanged(val value: TravelMode) : CreateTripAction
    data object Submit : CreateTripAction
}
