package com.yangchengwei.easytrip.workspace

import com.yangchengwei.easytrip.trip.domain.TripDay

private const val WHOLE_TRIP_VALUE = "WHOLE_TRIP"
private const val DAY_PREFIX = "DAY:"

enum class WorkspaceSection { PLACE_POOL, ITINERARY }

sealed interface ItineraryScope {
    data object WholeTrip : ItineraryScope
    data class Day(val dayId: String) : ItineraryScope
}

data class RestoredWorkspaceNavigation(
    val section: WorkspaceSection,
    val itineraryScope: ItineraryScope?,
)

fun WorkspaceSection.toMapScope(itineraryScope: ItineraryScope): MapScope = when (this) {
    WorkspaceSection.PLACE_POOL -> MapScope.PLACE_POOL
    WorkspaceSection.ITINERARY -> when (itineraryScope) {
        ItineraryScope.WholeTrip -> MapScope.WHOLE_TRIP
        is ItineraryScope.Day -> MapScope.SINGLE_DAY
    }
}

fun ItineraryScope.selectedDayId(): String? = when (this) {
    ItineraryScope.WholeTrip -> null
    is ItineraryScope.Day -> dayId
}

internal fun encodeItineraryScope(scope: ItineraryScope): String = when (scope) {
    ItineraryScope.WholeTrip -> WHOLE_TRIP_VALUE
    is ItineraryScope.Day -> "$DAY_PREFIX${scope.dayId}"
}

internal fun decodeItineraryScope(raw: String?): ItineraryScope? = when {
    raw == WHOLE_TRIP_VALUE -> ItineraryScope.WholeTrip
    raw?.startsWith(DAY_PREFIX) == true && raw.removePrefix(DAY_PREFIX).isNotBlank() -> ItineraryScope.Day(raw.removePrefix(DAY_PREFIX))
    else -> null
}

internal fun restoreWorkspaceNavigation(
    sectionRaw: String?,
    itineraryScopeRaw: String?,
    legacyTabRaw: String?,
    legacyScopeRaw: String?,
    legacySelectedDayId: String?,
): RestoredWorkspaceNavigation {
    val legacySection = when {
        legacyScopeRaw == "PLACE_POOL" -> WorkspaceSection.PLACE_POOL
        legacyScopeRaw == "WHOLE_TRIP" || legacyScopeRaw == "SINGLE_DAY" -> WorkspaceSection.ITINERARY
        legacyTabRaw == "ITINERARY" -> WorkspaceSection.ITINERARY
        else -> WorkspaceSection.PLACE_POOL
    }
    val legacyItineraryScope = when {
        legacyScopeRaw == "PLACE_POOL" -> null
        legacyScopeRaw == "WHOLE_TRIP" -> ItineraryScope.WholeTrip
        legacyScopeRaw == "SINGLE_DAY" && !legacySelectedDayId.isNullOrBlank() -> ItineraryScope.Day(legacySelectedDayId)
        legacyTabRaw == "ITINERARY" -> ItineraryScope.WholeTrip
        else -> null
    }
    return RestoredWorkspaceNavigation(
        section = sectionRaw?.let { raw -> WorkspaceSection.entries.firstOrNull { it.name == raw } } ?: legacySection,
        itineraryScope = decodeItineraryScope(itineraryScopeRaw) ?: legacyItineraryScope,
    )
}

internal fun reconcileItineraryScope(
    current: ItineraryScope?,
    previousDays: List<TripDay>,
    currentDays: List<TripDay>,
): ItineraryScope {
    if (current == ItineraryScope.WholeTrip) return current
    if (current is ItineraryScope.Day && currentDays.any { it.id == current.dayId }) return current
    if (currentDays.isEmpty()) return ItineraryScope.WholeTrip
    if (current !is ItineraryScope.Day) return ItineraryScope.Day(currentDays.first().id)

    val previousIndex = previousDays.indexOfFirst { it.id == current.dayId }
    if (previousIndex < 0) return ItineraryScope.Day(currentDays.first().id)
    val replacement = currentDays.getOrNull(previousIndex) ?: currentDays.last()
    return ItineraryScope.Day(replacement.id)
}
