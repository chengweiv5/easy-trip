package com.yangchengwei.easytrip.itinerary.ui

import com.yangchengwei.easytrip.core.model.RouteStatus
import com.yangchengwei.easytrip.core.model.TransportMode
import com.yangchengwei.easytrip.itinerary.domain.ItineraryItem
import com.yangchengwei.easytrip.route.data.RouteLegEntity
import com.yangchengwei.easytrip.route.domain.RouteErrorKind
import com.yangchengwei.easytrip.trip.domain.TripDay
import com.yangchengwei.easytrip.workspace.DayMapSnapshot
import java.time.LocalTime

data class ItineraryItemUi(
    val id: String,
    val name: String,
    val address: String,
    val arrivalTime: LocalTime?,
    val stayMinutes: Int?,
)

sealed interface RouteLegUiState {
    data class Ready(
        val distanceMeters: Int?,
        val durationSeconds: Int?,
    ) : RouteLegUiState

    data object Pending : RouteLegUiState
    data object Calculating : RouteLegUiState
    data object WaitingForNetwork : RouteLegUiState
    data class Failed(val message: String) : RouteLegUiState
}

data class RouteLegUi(
    val id: String,
    val fromItemId: String,
    val toItemId: String,
    val mode: TransportMode,
    val status: RouteStatus,
    val distanceMeters: Int?,
    val durationSeconds: Int?,
    val error: String?,
) {
    val state: RouteLegUiState
        get() = when (status) {
            RouteStatus.SUCCESS -> RouteLegUiState.Ready(distanceMeters, durationSeconds)
            RouteStatus.PENDING -> RouteLegUiState.Pending
            RouteStatus.CALCULATING -> RouteLegUiState.Calculating
            RouteStatus.WAITING_NETWORK -> RouteLegUiState.WaitingForNetwork
            RouteStatus.FAILED -> RouteLegUiState.Failed(error ?: "路线计算失败")
        }
}

data class WholeTripDayUi(
    val dayId: String,
    val dayNumber: Int,
    val items: List<ItineraryItemUi>,
    val legs: List<RouteLegUi>,
)

internal fun mapWholeTripDays(
    days: List<TripDay>,
    snapshots: List<DayMapSnapshot>,
): List<WholeTripDayUi> {
    val snapshotsByDay = snapshots.associateBy { it.itinerary.dayId }
    return days.sortedBy(TripDay::index).map { day ->
        val snapshot = snapshotsByDay[day.id]
        val items = snapshot?.itinerary?.items.orEmpty()
        val adjacentPairs = items.zipWithNext { from, to -> from.id to to.id }.toSet()
        WholeTripDayUi(
            dayId = day.id,
            dayNumber = day.index + 1,
            items = items.map(ItineraryItem::toItineraryItemUi),
            legs = snapshot?.legs.orEmpty()
                .filter { it.fromItemId to it.toItemId in adjacentPairs }
                .map(RouteLegEntity::toRouteLegUi),
        )
    }
}

internal fun ItineraryItem.toItineraryItemUi() = ItineraryItemUi(
    id = id,
    name = place.name,
    address = place.address,
    arrivalTime = arrivalTime,
    stayMinutes = stayMinutes,
)

internal fun RouteLegEntity.toRouteLegUi() = RouteLegUi(
    id = id,
    fromItemId = fromItemId,
    toItemId = toItemId,
    mode = selectedMode ?: recommendedMode,
    status = status,
    distanceMeters = distanceMeters,
    durationSeconds = durationSeconds,
    error = errorKind.toRouteErrorSummary() ?: errorCode,
)

internal fun RouteErrorKind?.toRouteErrorSummary() = when (this) {
    RouteErrorKind.TRANSIENT -> "网络异常，请重试"
    RouteErrorKind.NO_ROUTE -> "未找到可用路线"
    RouteErrorKind.UNSUPPORTED_TRANSIT -> "当前地点不支持公交规划"
    RouteErrorKind.PERMANENT -> "路线规划失败"
    null -> null
}
