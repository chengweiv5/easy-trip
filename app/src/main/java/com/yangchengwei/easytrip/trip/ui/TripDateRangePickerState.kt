package com.yangchengwei.easytrip.trip.ui

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

const val MAX_TRIP_DAYS = 30
const val DAYS_IN_WEEK = 7

data class DateRangeSelection(
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
) {
    private val rawDayCount: Long?
        get() = if (startDate != null && endDate != null) {
            ChronoUnit.DAYS.between(startDate, endDate) + 1
        } else {
            null
        }

    val dayCount: Int?
        get() = rawDayCount?.takeIf { it in 1L..MAX_TRIP_DAYS.toLong() }?.toInt()

    val validationError: String?
        get() = when {
            startDate == null || endDate == null -> "请选择开始和结束日期"
            endDate.isBefore(startDate) -> "结束日期不能早于开始日期"
            rawDayCount !in 1L..MAX_TRIP_DAYS.toLong() -> "旅行最多 $MAX_TRIP_DAYS 天"
            else -> null
        }

    val isConfirmable: Boolean get() = validationError == null
}

data class CalendarMonth(val month: YearMonth, val days: List<LocalDate>)

fun reduceDateRangeSelection(selection: DateRangeSelection, date: LocalDate): DateRangeSelection = when {
    selection.startDate == null || selection.endDate != null -> DateRangeSelection(startDate = date)
    date < selection.startDate -> DateRangeSelection(startDate = date)
    else -> selection.copy(endDate = date)
}

fun calendarMonth(month: YearMonth): CalendarMonth {
    val first = month.atDay(1)
    val last = month.atEndOfMonth()
    val firstMonday = if (first == LocalDate.MIN) first else {
        val offset = (first.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong()
        first.minusDays(offset.coerceAtMost(java.time.temporal.ChronoUnit.DAYS.between(LocalDate.MIN, first)))
    }
    val lastSunday = if (last == LocalDate.MAX) last else {
        val offset = (DayOfWeek.SUNDAY.value - last.dayOfWeek.value).toLong()
        last.plusDays(offset.coerceAtMost(java.time.temporal.ChronoUnit.DAYS.between(last, LocalDate.MAX)))
    }
    val days = buildList {
        var date = firstMonday
        while (true) {
            add(date)
            if (date == lastSunday) break
            date = date.plusDays(1)
        }
    }
    return CalendarMonth(month = month, days = days)
}

fun DateRangeSelection.daySelectionState(date: LocalDate): DateSelectionState = when {
    date == startDate && date == endDate -> DateSelectionState.SINGLE
    date == startDate -> DateSelectionState.START
    date == endDate -> DateSelectionState.END
    startDate != null && endDate != null && date > startDate && date < endDate -> DateSelectionState.IN_RANGE
    else -> DateSelectionState.UNSELECTED
}

enum class DateSelectionState {
    UNSELECTED,
    START,
    END,
    SINGLE,
    IN_RANGE,
}
