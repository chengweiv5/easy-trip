package com.yangchengwei.easytrip.trip.ui

import androidx.compose.runtime.Immutable
import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.core.ui.component.ConfirmationUiModel
import com.yangchengwei.easytrip.trip.domain.TripSummary
import java.time.format.DateTimeFormatter
import java.util.Locale

fun TripDeleteImpact.toConfirmationUiModel(tripName: String): ConfirmationUiModel = ConfirmationUiModel(
    title = "删除$tripName？",
    message = "此操作将永久删除旅行及其中的所有内容，无法撤销。",
    deletedItems = listOf(
        "$days 个旅行日",
        "$places 个收藏地点",
        "$tags 个标签",
        "$itineraryItems 个行程项",
        "$routeLegs 个路线段",
    ),
    retainedItems = listOf("其他旅行及其内容"),
    confirmLabel = "确认删除旅行",
    dismissLabel = "取消",
    destructive = true,
    reversible = false,
)

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

sealed interface TripDeletionUiState {
    data object Idle : TripDeletionUiState
    data class LoadingImpact(val tripId: String, val tripName: String) : TripDeletionUiState
    data class ImpactFailure(
        val tripId: String,
        val tripName: String,
        val message: String,
    ) : TripDeletionUiState
    data class Ready(
        val tripId: String,
        val tripName: String,
        val confirmation: ConfirmationUiModel,
        val isDeleting: Boolean = false,
        val errorMessage: String? = null,
        val confirmationSyncFailed: Boolean = false,
    ) : TripDeletionUiState
}

sealed interface TripListAction {
    data object CreateTrip : TripListAction
    data object Retry : TripListAction
    data class OpenTrip(val tripId: String) : TripListAction
    data class OpenSettings(val tripId: String) : TripListAction
    data class RequestDelete(val tripId: String) : TripListAction
    data object RetryDeleteImpact : TripListAction
    data object ConfirmDelete : TripListAction
    data object RetryDeletionSync : TripListAction
    data object CancelDelete : TripListAction
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
