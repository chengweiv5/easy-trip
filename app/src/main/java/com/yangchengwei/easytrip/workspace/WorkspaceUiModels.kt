package com.yangchengwei.easytrip.workspace

data class WorkspaceSearchReturn(val recentlyCollectedPoiIds: Set<String>)

data class TripWorkspaceReadyState(
    val tripName: String,
    val days: List<com.yangchengwei.easytrip.trip.domain.TripDay>,
    val section: WorkspaceSection,
    val itineraryScope: ItineraryScope,
    val wholeTripDays: List<com.yangchengwei.easytrip.itinerary.ui.WholeTripDayUi>,
    val sheetLevel: WorkspaceSheetLevel,
    val map: MapUiModel,
    val mapLayer: MapLayer,
    val selectedMarker: MapMarkerUi?,
    val selectedMarkerPoi: MapPoiUi?,
    val selectedMapPoi: MapPoiUi?,
    val overlay: WorkspaceOverlay,
    val dateLabel: String? = null,
)

internal fun TripWorkspaceUiState.toReadyState() = TripWorkspaceReadyState(
    tripName = tripName,
    dateLabel = dateLabel,
    days = days,
    section = section,
    itineraryScope = itineraryScope,
    wholeTripDays = wholeTripDays,
    sheetLevel = sheetLevel,
    map = map,
    mapLayer = mapLayer,
    selectedMarker = selectedMarker,
    selectedMarkerPoi = selectedMarkerPoi,
    selectedMapPoi = selectedMapPoi,
    overlay = overlay,
)

sealed interface TripWorkspacePageState {
    data object Loading : TripWorkspacePageState
    data object NotFound : TripWorkspacePageState
    data class Error(val message: String) : TripWorkspacePageState
    data class Ready(val content: TripWorkspaceReadyState) : TripWorkspacePageState
}

sealed interface WorkspaceMapState {
    data object Loading : WorkspaceMapState
    data object Ready : WorkspaceMapState
    data object ConsentRequired : WorkspaceMapState
    data class Failed(val message: String) : WorkspaceMapState
}

sealed interface TripWorkspaceAction {
    data object Back : TripWorkspaceAction
    data object OpenSettings : TripWorkspaceAction
    data object OpenPrivacySettings : TripWorkspaceAction
    data object OpenSearch : TripWorkspaceAction
    data object Locate : TripWorkspaceAction
    data object Retry : TripWorkspaceAction
    data class SelectSection(val section: WorkspaceSection) : TripWorkspaceAction
    data class SelectItineraryScope(val scope: ItineraryScope) : TripWorkspaceAction
    data class SelectMapLayer(val layer: MapLayer) : TripWorkspaceAction
    data class SetSheetLevel(val level: WorkspaceSheetLevel) : TripWorkspaceAction
    data class OpenOverlay(val overlay: WorkspaceOverlay) : TripWorkspaceAction
    data object CloseOverlay : TripWorkspaceAction
}
