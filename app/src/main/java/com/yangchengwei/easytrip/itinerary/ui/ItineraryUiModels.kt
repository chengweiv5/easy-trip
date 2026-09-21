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
    val note: String? = null,
    val placeId: String? = null,
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
    val durationOverrideSeconds: Int? = null,
    val note: String? = null,
    val selectedModeOverride: TransportMode? = null,
    val version: Long = 0,
) {
    val effectiveDurationSeconds: Int?
        get() = durationOverrideSeconds ?: durationSeconds
    val state: RouteLegUiState
        get() = when (status) {
            RouteStatus.SUCCESS -> RouteLegUiState.Ready(distanceMeters, durationSeconds)
            RouteStatus.PENDING -> RouteLegUiState.Pending
            RouteStatus.CALCULATING -> RouteLegUiState.Calculating
            RouteStatus.WAITING_NETWORK -> RouteLegUiState.WaitingForNetwork
            RouteStatus.FAILED -> RouteLegUiState.Failed(error ?: "路线计算失败")
        }
}

internal fun currentDisplayItems(
    items: List<ItineraryItemUi>,
    previewOrder: List<String>,
): List<ItineraryItemUi> {
    val byId = items.associateBy(ItineraryItemUi::id)
    return if (
        previewOrder.size == items.size &&
        previewOrder.distinct().size == items.size &&
        previewOrder.all(byId::containsKey)
    ) {
        previewOrder.map(byId::getValue)
    } else {
        items
    }
}

internal fun visibleRouteLegs(
    items: List<ItineraryItemUi>,
    previewOrder: List<String>,
    legs: List<RouteLegUi>,
): List<RouteLegUi> {
    val adjacentPairs = currentDisplayItems(items, previewOrder)
        .zipWithNext { from, to -> from.id to to.id }
        .toSet()
    return legs.filter { it.fromItemId to it.toItemId in adjacentPairs }
}

data class WholeTripDayUi(
    val dayId: String,
    val dayNumber: Int,
    val items: List<ItineraryItemUi>,
    val legs: List<RouteLegUi>,
    val collapsedItemCount: Int = 0,
)

internal fun mapWholeTripDays(
    days: List<TripDay>,
    snapshots: List<DayMapSnapshot>,
): List<WholeTripDayUi> {
    val projection = projectWholeTrip(days, snapshots)
    val snapshotsByDay = projection.snapshots.associateBy { it.itinerary.dayId }
    val dayNumbers = days.associate { it.id to it.index + 1 }
    val sourceByDay = snapshots.associateBy { it.itinerary.dayId }
    val mergedNotes = days.sortedBy(TripDay::index).flatMap { day ->
        sourceByDay[day.id]?.itinerary?.items.orEmpty().map { item ->
            Triple(item, dayNumbers.getValue(day.id), projection.representativeItemIds.getValue(item.id))
        }
    }.groupBy { it.third }.mapValues { (_, visits) ->
        if (visits.size <= 1) visits.firstOrNull()?.first?.note
        else visits.mapNotNull { (item, dayNumber, _) ->
            item.note?.trim()?.takeIf(String::isNotEmpty)?.let { "第 $dayNumber 天 · $it" }
        }.joinToString("\n").ifBlank { null }
    }
    return days.sortedBy(TripDay::index).map { day ->
        val snapshot = snapshotsByDay[day.id]
        val items = snapshot?.itinerary?.items.orEmpty()
        WholeTripDayUi(
            dayId = day.id,
            dayNumber = day.index + 1,
            items = items.map { it.toItineraryItemUi().copy(note = mergedNotes[it.id]) },
            legs = snapshot?.legs.orEmpty()
                .map(RouteLegEntity::toRouteLegUi),
            collapsedItemCount = projection.collapsedCounts[day.id] ?: 0,
        )
    }
}

internal fun ItineraryItem.toItineraryItemUi() = ItineraryItemUi(
    id = id,
    name = place.name,
    address = place.address,
    arrivalTime = arrivalTime,
    stayMinutes = stayMinutes,
    note = note,
    placeId = place.id,
)

internal fun RouteLegEntity.toRouteLegUi() = RouteLegUi(
    id = id,
    fromItemId = fromItemId,
    toItemId = toItemId,
    mode = selectedMode ?: recommendedMode,
    status = status,
    distanceMeters = distanceMeters,
    durationSeconds = durationSeconds,
    durationOverrideSeconds = durationOverrideSeconds,
    note = note,
    selectedModeOverride = selectedMode,
    version = version,
    error = errorKind.toRouteErrorSummary() ?: errorCode,
)

internal fun RouteErrorKind?.toRouteErrorSummary() = when (this) {
    RouteErrorKind.TRANSIENT -> "网络异常，请重试"
    RouteErrorKind.NO_ROUTE -> "未找到可用路线"
    RouteErrorKind.UNSUPPORTED_TRANSIT -> "城市信息暂未获取，请重试或更改方式"
    RouteErrorKind.PERMANENT -> "路线规划失败"
    null -> null
}
