package com.yangchengwei.easytrip.trip.ui

import com.yangchengwei.easytrip.trip.domain.CreateTrip
import com.yangchengwei.easytrip.trip.domain.MAX_TRIP_DAYS
import com.yangchengwei.easytrip.trip.domain.isTripDateRangeRepresentable

data class ValidCreateTrip(
    val command: CreateTrip,
    val startDate: java.time.LocalDate,
)

data class CreateTripValidation(
    val valid: ValidCreateTrip? = null,
    val nameError: String? = null,
    val dateError: String? = null,
)

fun createTripCommand(state: CreateTripUiState): CreateTrip? {
    val startDate = state.startDate ?: return null
    val endDate = state.endDate ?: return null
    val days = state.dayCount ?: return null
    if (
        state.name.isBlank() ||
        endDate.isBefore(startDate) ||
        days !in 1..MAX_TRIP_DAYS ||
        !isTripDateRangeRepresentable(startDate, days)
    ) return null
    return CreateTrip(state.name.trim(), days, state.travelMode, startDate)
}

fun validateCreateTrip(state: CreateTripUiState): CreateTripValidation {
    val nameError = if (state.name.isBlank()) "请输入旅行名称" else null
    val selection = DateRangeSelection(state.startDate, state.endDate)
    val dayCount = state.dayCount
    val dateError = when {
        selection.validationError != null -> selection.validationError
        dayCount != null && !isTripDateRangeRepresentable(state.startDate, dayCount) -> "日期范围超出支持范围"
        else -> null
    }
    if (nameError != null || dateError != null) {
        return CreateTripValidation(nameError = nameError, dateError = dateError)
    }
    return CreateTripValidation(
        valid = ValidCreateTrip(createTripCommand(state)!!.copy(requestId = state.requestId), state.startDate!!),
    )
}
