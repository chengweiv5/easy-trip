package com.yangchengwei.easytrip.trip.ui

import androidx.compose.runtime.Immutable
import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.trip.domain.TripSummary
import java.time.format.DateTimeFormatter
import java.util.Locale

sealed interface TripListPageState {
    data object Loading : TripListPageState
    data object Empty : TripListPageState
    data class Content(
        val primaryTrip: TripCardUiModel,
        val otherTrips: List<TripCardUiModel>,
    ) : TripListPageState {
        constructor(trips: List<TripCardUiModel>) : this(trips.first(), trips.drop(1))
        val trips: List<TripCardUiModel> get() = listOf(primaryTrip) + otherTrips
    }
    data class Error(val message: String) : TripListPageState
}

@Immutable
data class TripCardUiModel(
    val id: String,
    val name: String,
    val dayCountLabel: String,
    val dateLabel: String?,
    val travelModeLabel: String,
)

sealed interface TripListAction {
    data object CreateTrip : TripListAction
    data object Retry : TripListAction
    data class OpenTrip(val tripId: String) : TripListAction
    data class OpenSettings(val tripId: String) : TripListAction
    data class RequestDelete(val tripId: String) : TripListAction
}

private val tripDateFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA)

fun TripSummary.toTripCardUiModel(): TripCardUiModel = TripCardUiModel(
    id = id,
    name = name,
    dayCountLabel = "$dayCount 天",
    dateLabel = startDate?.format(tripDateFormatter),
    travelModeLabel = when (travelMode) {
        TravelMode.FLEXIBLE -> "灵活"
        TravelMode.SELF_DRIVE -> "自驾"
    },
)
