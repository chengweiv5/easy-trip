package com.yangchengwei.easytrip.workspace

import com.yangchengwei.easytrip.core.ui.component.ConfirmationUiModel

sealed interface WorkspaceOverlay {
    data object None : WorkspaceOverlay
    data class PlaceDetail(val placeId: Long) : WorkspaceOverlay
    data object SelectAddPlaces : WorkspaceOverlay
    data object SelectAddTargetDay : WorkspaceOverlay
    data object AddToItineraryResult : WorkspaceOverlay
    data class SelectMoveTargetDay(val itemId: String) : WorkspaceOverlay
    data object AddTripDay : WorkspaceOverlay
    data class EditItineraryItem(val itemId: String) : WorkspaceOverlay
    data class EditRouteLeg(val legId: String) : WorkspaceOverlay
    data object LayerMenu : WorkspaceOverlay
    data object MoreMenu : WorkspaceOverlay
    data class Confirmation(val model: ConfirmationUiModel) : WorkspaceOverlay
    data class PermissionExplanation(val kind: PermissionKind) : WorkspaceOverlay
    data class Feedback(val model: FeedbackUiModel) : WorkspaceOverlay
}

enum class PermissionKind {
    DEVICE_LOCATION,
    DEVICE_LOCATION_SETTINGS,
}

data class FeedbackUiModel(
    val message: String,
)
