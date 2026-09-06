package com.yangchengwei.easytrip.trip.ui

import com.yangchengwei.easytrip.trip.domain.DateRangeChangeImpact
import java.time.LocalDate

data class DateRangeChangeUiState(
    val startDate: LocalDate? = null,
    val baselineEndDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val isDirty: Boolean = false,
    val phase: DateRangeChangePhase = DateRangeChangePhase.Idle,
    val error: String? = null,
    val baselineStartDate: LocalDate? = startDate,
    val draftStartDate: LocalDate? = startDate,
    val draftEndDate: LocalDate? = endDate,
) {
    val baseline: DateRangeSelection
        get() = DateRangeSelection(baselineStartDate, baselineEndDate)

    val draft: DateRangeSelection
        get() = DateRangeSelection(draftStartDate, draftEndDate)
    val confirmation: DateRangeChangeImpact?
        get() = when (val value = phase) {
            is DateRangeChangePhase.AwaitingConfirmation -> value.impact
            is DateRangeChangePhase.Applying -> value.impact
            is DateRangeChangePhase.AwaitingRoom -> value.impact
            is DateRangeChangePhase.SyncFailed -> value.impact
            else -> null
        }

    val submitting: Boolean
        get() = phase is DateRangeChangePhase.Applying || phase is DateRangeChangePhase.AwaitingRoom
}
