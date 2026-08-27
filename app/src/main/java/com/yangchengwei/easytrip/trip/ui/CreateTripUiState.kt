package com.yangchengwei.easytrip.trip.ui

import com.yangchengwei.easytrip.core.model.TravelMode
import java.time.LocalDate

enum class CreateTimeMode { DRAFT, DATED }

data class CreateTripUiState(
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
) {
    val endDate: LocalDate?
        get() {
            val days = dayCount.toLongOrNull() ?: return null
            return if (timeMode == CreateTimeMode.DATED && days >= 1) startDate?.plusDays(days - 1) else null
        }
}

sealed interface CreateTripAction {
    data object Back : CreateTripAction
    data class NameChanged(val value: String) : CreateTripAction
    data class DayCountChanged(val value: String) : CreateTripAction
    data class TimeModeChanged(val value: CreateTimeMode) : CreateTripAction
    data class StartDateChanged(val value: LocalDate?) : CreateTripAction
    data class TravelModeChanged(val value: TravelMode) : CreateTripAction
    data object Submit : CreateTripAction
}

sealed interface CreateTripEffect {
    data object NavigateBack : CreateTripEffect
    data class OpenWorkspace(val tripId: String) : CreateTripEffect
}
