package com.yangchengwei.easytrip.trip.ui

import com.yangchengwei.easytrip.trip.domain.DateRangeChangeImpact
import java.time.LocalDate

data class DateRangeChangeRequest(
    val generation: Long,
    val tripId: String,
    val baselineStartDate: LocalDate?,
    val baselineDayIds: List<String>,
    val targetStartDate: LocalDate,
    val targetEndDate: LocalDate,
)

sealed interface DateRangeChangePhase {
    data object Idle : DateRangeChangePhase
    data class Previewing(val request: DateRangeChangeRequest) : DateRangeChangePhase
    data class AwaitingConfirmation(
        val request: DateRangeChangeRequest,
        val impact: DateRangeChangeImpact,
    ) : DateRangeChangePhase
    data class Applying(
        val request: DateRangeChangeRequest,
        val impact: DateRangeChangeImpact,
    ) : DateRangeChangePhase
    data class AwaitingRoom(
        val request: DateRangeChangeRequest,
        val impact: DateRangeChangeImpact,
    ) : DateRangeChangePhase
    data class SyncFailed(
        val request: DateRangeChangeRequest,
        val impact: DateRangeChangeImpact,
    ) : DateRangeChangePhase
}
