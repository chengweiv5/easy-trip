package com.yangchengwei.easytrip.assistant

import androidx.compose.runtime.staticCompositionLocalOf
import com.yangchengwei.easytrip.workspace.*

enum class AssistantDetail { CATEGORY, CANDIDATES }

data class AssistantWorkspace(
    val model: PlaceAssistantViewModel,
    val state: PlaceAssistantState,
    val open: Boolean,
    val focused: String?,
    val tripName: String,
    val onSettings: () -> Unit,
    val onConsent: () -> Unit,
    val onOpen: () -> Unit,
    val onViewPool: () -> Unit,
    val detail: AssistantDetail? = null,
) {
    val controller get() = model.controller
}
val LocalAssistantWorkspace = staticCompositionLocalOf<AssistantWorkspace?> { null }

fun assistantMarkers(state: PlaceAssistantState, focused: String?): List<MapMarkerUi> =
    state.items.flatMap { item ->
        val options = if (item.id == focused && item.poi == null) item.candidates else listOfNotNull(item.poi)
        if (item.status in setOf(IntakeStatus.ALREADY_SAVED, IntakeStatus.DUPLICATE)) emptyList()
        else options.mapIndexedNotNull { index, poi ->
            poi.point?.let { point ->
                MapMarkerUi("assistant:${item.id}:${poi.poiId}", point, poi.name, emptyList(), MapMarkerKind.UNSAVED_SEARCH,
                    badgeText = item.label + if (item.poi == null) (index + 1).toString() else "",
                    isFocused = item.id == focused)
            }
        }
    }

/** Latest user-owned request wins; retaining a draft never permanently owns the camera. */
class AssistantViewportOwner {
    private var workspaceId: Long? = null
    private var assistantId: Long? = null
    private var assistantOwnsCamera = false
    private var currentRequest: MapViewportRequest? = null

    fun resolve(
        workspace: MapViewportRequest?,
        assistant: MapViewportRequest?,
        assistantVisible: Boolean = true,
    ): MapViewportRequest? {
        if (workspace?.id != workspaceId) {
            assistantOwnsCamera = false
            currentRequest = workspace
        }
        if (assistant?.id != assistantId) {
            when {
                assistant != null && assistantVisible -> {
                    assistantOwnsCamera = true
                    currentRequest = assistant
                }
                // Retiring a draft must not replay an older workspace request.
                assistantOwnsCamera -> currentRequest = null
            }
        }
        // Observe hidden results too: reopening a retained draft is not a new camera intent.
        workspaceId = workspace?.id
        assistantId = assistant?.id
        return currentRequest
    }
}

fun assistantViewportPoints(state: PlaceAssistantState, focused: String?): List<com.yangchengwei.easytrip.core.model.GeoPoint> {
    val item = state.items.find { it.id == focused }
    return if (item != null) {
        (item.poi?.let { listOf(it) } ?: item.candidates).mapNotNull { it.point }.distinct()
    } else assistantMarkers(state, null).map { it.point }.distinct()
}
