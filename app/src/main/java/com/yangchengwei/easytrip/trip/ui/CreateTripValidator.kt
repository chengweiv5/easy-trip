package com.yangchengwei.easytrip.trip.ui

import com.yangchengwei.easytrip.trip.domain.CreateTrip

data class ValidCreateTrip(
    val command: CreateTrip,
    val startDate: java.time.LocalDate?,
)

data class CreateTripValidation(
    val valid: ValidCreateTrip? = null,
    val nameError: String? = null,
    val dayCountError: String? = null,
    val dateError: String? = null,
)

fun validateCreateTrip(state: CreateTripUiState): CreateTripValidation {
    val nameError = if (state.name.isBlank()) "请输入旅行名称" else null
    val days = state.dayCount.toIntOrNull()
    val dayCountError = if (days == null || days < 1) "请输入至少 1 天" else null
    val dateError = if (state.timeMode == CreateTimeMode.DATED && state.startDate == null) "请选择开始日期" else null
    if (nameError != null || dayCountError != null || dateError != null) {
        return CreateTripValidation(nameError = nameError, dayCountError = dayCountError, dateError = dateError)
    }
    return CreateTripValidation(
        valid = ValidCreateTrip(
            CreateTrip(
                state.name.trim(),
                days!!,
                state.travelMode,
                if (state.timeMode == CreateTimeMode.DATED) state.startDate else null,
                state.requestId,
            ),
            if (state.timeMode == CreateTimeMode.DATED) state.startDate else null,
        ),
    )
}
