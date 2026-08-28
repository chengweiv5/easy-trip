package com.yangchengwei.easytrip.trip.ui

import com.yangchengwei.easytrip.trip.domain.CreateTrip
import com.yangchengwei.easytrip.trip.domain.MAX_TRIP_DAYS
import com.yangchengwei.easytrip.trip.domain.isTripDateRangeRepresentable

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

fun createTripCommand(state: CreateTripUiState): CreateTrip? {
    val days = state.dayCount.toIntOrNull() ?: return null
    if (
        state.name.isBlank() ||
        days !in 1..MAX_TRIP_DAYS ||
        state.timeMode == CreateTimeMode.DATED &&
        (state.startDate == null || !isTripDateRangeRepresentable(state.startDate, days))
    ) return null
    return CreateTrip(
        state.name.trim(),
        days,
        state.travelMode,
        if (state.timeMode == CreateTimeMode.DATED) state.startDate else null,
    )
}

fun validateCreateTrip(state: CreateTripUiState): CreateTripValidation {
    val nameError = if (state.name.isBlank()) "请输入旅行名称" else null
    val days = state.dayCount.toIntOrNull()
    val dayCountError = when {
        days == null || days < 1 -> "请输入至少 1 天"
        days > MAX_TRIP_DAYS -> "旅行最多 30 天"
        else -> null
    }
    val dateError = when {
        state.timeMode != CreateTimeMode.DATED -> null
        state.startDate == null -> "请选择开始日期"
        days != null && days >= 1 && !isTripDateRangeRepresentable(state.startDate, days) -> "日期范围超出支持范围"
        else -> null
    }
    if (nameError != null || dayCountError != null || dateError != null) {
        return CreateTripValidation(nameError = nameError, dayCountError = dayCountError, dateError = dateError)
    }
    return CreateTripValidation(
        valid = ValidCreateTrip(
            createTripCommand(state)!!.copy(requestId = state.requestId),
            if (state.timeMode == CreateTimeMode.DATED) state.startDate else null,
        ),
    )
}
