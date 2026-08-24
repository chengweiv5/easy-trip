package com.yangchengwei.easytrip.trip.ui

import com.yangchengwei.easytrip.trip.domain.DateRangeChangeImpact
import java.time.LocalDate

data class DateRangeChangeUiState(
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val confirmation: DateRangeChangeImpact? = null,
    val submitting: Boolean = false,
    val error: String? = null,
)
