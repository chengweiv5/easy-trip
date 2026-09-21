package com.yangchengwei.easytrip.workspace

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yangchengwei.easytrip.amap.AmapConsentFact
import com.yangchengwei.easytrip.amap.AmapConsentToken
import com.yangchengwei.easytrip.core.model.TransportMode
import com.yangchengwei.easytrip.permission.LocationPermissionUiState
import com.yangchengwei.easytrip.core.ui.component.CompactPrimaryButton
import com.yangchengwei.easytrip.core.ui.component.CompactSecondaryButton
import com.yangchengwei.easytrip.core.ui.component.ConfirmationDialog
import com.yangchengwei.easytrip.itinerary.ui.AddToItineraryResultContent
import com.yangchengwei.easytrip.itinerary.ui.AddToItineraryUiState
import com.yangchengwei.easytrip.itinerary.ui.AddTripDayContent
import com.yangchengwei.easytrip.itinerary.ui.DayItineraryAction
import com.yangchengwei.easytrip.itinerary.ui.SelectPlacesContent
import com.yangchengwei.easytrip.itinerary.ui.SelectTargetDayContent
import com.yangchengwei.easytrip.itinerary.ui.DayItineraryUiState
import com.yangchengwei.easytrip.itinerary.ui.EditItineraryItemContent
import com.yangchengwei.easytrip.itinerary.ui.ItinerarySaveFailureContent
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.ui.PlaceDetailPanelAction
import com.yangchengwei.easytrip.place.ui.WorkspacePlaceDetailSheet
import com.yangchengwei.easytrip.place.ui.PlacePoolAction
import com.yangchengwei.easytrip.place.ui.PlacePoolUiState
import com.yangchengwei.easytrip.permission.LocationPermissionSettingsContent
import com.yangchengwei.easytrip.permission.PermissionExplanationContent

internal class MapLocateRequestBaselineTracker(private val initialRequest: Int) {
    private var hasMounted = false
    private var activeAttemptId: Any? = null
    private var consumedRequest = initialRequest
    private var failedConsumedRequest = initialRequest
    private var forwardedAttemptId: Any? = null
    private var forwardedBaseline: Int? = null

    fun baselineForMount(attemptId: Any, currentRequest: Int): Int {
        if (activeAttemptId == attemptId) return consumedRequest
        val baseline = if (forwardedAttemptId == attemptId) {
            forwardedAttemptId = null
            forwardedBaseline.also { forwardedBaseline = null } ?: currentRequest
        } else if (hasMounted) {
            currentRequest
        } else {
            initialRequest
        }
        hasMounted = true
        activeAttemptId = attemptId
        consumedRequest = baseline
        return baseline
    }

    fun recordConsumedRequest(attemptId: Any, request: Int) {
        if (activeAttemptId == attemptId) consumedRequest = maxOf(consumedRequest, request)
    }

    fun finishFailedAttempt(attemptId: Any, currentRequest: Int): Int? {
        if (activeAttemptId != attemptId) return null
        failedConsumedRequest = consumedRequest
        activeAttemptId = null
        clearForwardedAttempt(attemptId)
        return currentRequest.takeIf { it > failedConsumedRequest }
    }

    fun pendingRequestWhileFailed(currentRequest: Int): Int? =
        currentRequest.takeIf { it > failedConsumedRequest }

    fun forwardPendingRequest(attemptId: Any, request: Int): Boolean {
        if (forwardedAttemptId == attemptId) return false
        forwardedAttemptId = attemptId
        forwardedBaseline = request - 1
        return true
    }

    fun finishReadyAttempt(attemptId: Any) {
        clearForwardedAttempt(attemptId)
    }

    private fun clearForwardedAttempt(attemptId: Any) {
        if (forwardedAttemptId == attemptId) {
            forwardedAttemptId = null
            forwardedBaseline = null
        }
    }
}

internal enum class WorkspaceRootLayer { Content, Overlay }

internal fun workspaceRootLayers(hasReadyOverlay: Boolean): List<WorkspaceRootLayer> =
    if (hasReadyOverlay) {
        listOf(WorkspaceRootLayer.Content, WorkspaceRootLayer.Overlay)
    } else {
        listOf(WorkspaceRootLayer.Content)
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripWorkspaceScreen(
    pageState: TripWorkspacePageState,
    consent: AmapConsentToken?,
    consentFact: AmapConsentFact? = consent?.let { AmapConsentFact.Accepted(0, it) },
    locationPermissionUiState: LocationPermissionUiState = LocationPermissionUiState(),
    onOpenLocationSettings: () -> Unit = {},
    onAction: (TripWorkspaceAction) -> Unit,
    onMarkerClick: (String) -> Unit,
    onMapPoiClick: (MapPoiUi) -> Unit,
    placeState: PlacePoolUiState,
    onPlaceAction: (PlacePoolAction) -> Unit,
    itineraryState: DayItineraryUiState,
    addToItineraryState: AddToItineraryUiState = AddToItineraryUiState(),
    onToggleAddPlace: (String) -> Unit = {},
    onContinueAddPlaces: () -> Unit = {},
    onToggleAddTargetDay: (String) -> Unit = {},
    onGoToItineraryAddDay: () -> Unit = {},
    onSelectAddTargetDay: (String) -> Unit = {},
    onSubmitAddPlaces: () -> Unit = {},
    onUndoAddPlaces: () -> Unit = {},
    onRetryPartialAdd: () -> Unit = {},
    onReselectAddTargetDays: () -> Unit = {},
    onViewAddResult: (List<String>) -> Unit = {},
    onViewPlacePoolAfterUndo: () -> Unit = {},
    onItineraryAction: (DayItineraryAction) -> Unit,
    placeContent: (@Composable () -> Unit)? = null,
    dayItineraryContent: (@Composable () -> Unit)? = null,
    onCloseOverlay: () -> Unit,
    onDismissMapPlace: () -> Unit,
    isPoiSaved: Boolean = false,
    collectionBusyPoiIds: Set<String> = emptySet(),
    collectionError: String? = null,
    onTogglePoiCollection: (PlaceCandidate) -> Unit = {},
    onConfirmPermissionExplanation: () -> Unit = {},
    onDismissPermissionExplanation: () -> Unit = {},
    onDismissLocationSettings: () -> Unit = {},
    mapHostFactory: (android.content.Context) -> AmapMapHost = { RealAmapMapHost.create(it) },
    mapReadyTimeoutMillis: Long = DEFAULT_MAP_READY_TIMEOUT_MILLIS,
    locateRequest: Int = 0,
    searchReturn: WorkspaceSearchReturn? = null,
) {
    var mapAttempt by remember { mutableIntStateOf(0) }
    var zoomInRequest by remember { mutableIntStateOf(0) }
    var zoomOutRequest by remember { mutableIntStateOf(0) }
    var resetNorthRequest by remember { mutableIntStateOf(0) }
    var mapBearing by remember(consent, mapAttempt) { mutableFloatStateOf(0f) }
    var mapHostState: MapHostState by remember(consent) { mutableStateOf(MapHostState.Loading) }
    var failedAttempt by remember(consent) { mutableStateOf<Int?>(null) }
    val locateBaselineTracker = remember { MapLocateRequestBaselineTracker(locateRequest) }
    var pendingLocateRequest by remember(consent) { mutableStateOf<Int?>(null) }
    var layerFailureMessage by remember(consent) { mutableStateOf<String?>(null) }
    val ready = (pageState as? TripWorkspacePageState.Ready)?.content
    LaunchedEffect(consent) {
        pendingLocateRequest = null
        failedAttempt = null
        mapHostState = MapHostState.Loading
    }
    LaunchedEffect(locateRequest, mapHostState) {
        if (mapHostState is MapHostState.Failed) {
            pendingLocateRequest = locateBaselineTracker.pendingRequestWhileFailed(locateRequest)
        }
    }
    val mapState = resolveWorkspaceMapState(
        consentFact = consentFact,
        mapHostState = mapHostState,
    )

    Box(Modifier.fillMaxSize().testTag("workspace-screen-root")) {
        workspaceRootLayers(hasReadyOverlay = ready != null).forEach { layer ->
            when (layer) {
                WorkspaceRootLayer.Content -> TripWorkspaceContent(
                    modifier = if (ready?.overlay is WorkspaceOverlay.EditItineraryItem || ready?.overlay is WorkspaceOverlay.EditRouteLeg || ready?.overlay == WorkspaceOverlay.SelectAddPlaces || ready?.overlay == WorkspaceOverlay.SelectAddTargetDay) Modifier.clearAndSetSemantics {} else Modifier,
                    pageState = pageState,
                    mapState = mapState,
                    onMapRetry = {
                        if (mapHostState is MapHostState.Failed) {
                            val replacementAttempt = mapAttempt + 1
                            val requestToForward = pendingLocateRequest
                                ?: locateBaselineTracker.pendingRequestWhileFailed(locateRequest)
                            requestToForward?.let { request ->
                                locateBaselineTracker.forwardPendingRequest(consent to replacementAttempt, request)
                            }
                            pendingLocateRequest = null
                            mapAttempt = replacementAttempt
                            failedAttempt = null
                            layerFailureMessage = null
                            mapHostState = MapHostState.Loading
                        }
                    },
                    onAction = { action ->
                        when (action) {
                            TripWorkspaceAction.ZoomIn -> {
                                onAction(TripWorkspaceAction.MapGesture)
                                zoomInRequest++
                            }
                            TripWorkspaceAction.ZoomOut -> {
                                onAction(TripWorkspaceAction.MapGesture)
                                zoomOutRequest++
                            }
                            TripWorkspaceAction.ResetNorth -> {
                                onAction(TripWorkspaceAction.MapGesture)
                                resetNorthRequest++
                            }
                            is TripWorkspaceAction.SelectMapLayer -> {
                                layerFailureMessage = null
                                onAction(action)
                            }
                            else -> onAction(action)
                        }
                    },
                    placeState = placeState,
                    onPlaceAction = onPlaceAction,
                    itineraryState = itineraryState,
                    onItineraryAction = onItineraryAction,
                    placeContent = placeContent,
                    dayItineraryContent = dayItineraryContent,
                    searchReturn = searchReturn,
                    layerFailureMessage = layerFailureMessage,
                    onLayerFailureMessageDismissed = { layerFailureMessage = null },
                    mapBearing = mapBearing,
                    mapContent = { mapLayout ->
                        val token = consent
                        if (token != null && ready != null) key(mapAttempt, mapReadyTimeoutMillis) {
                            val attemptId = mapAttempt
                            val locateAttemptId = consent to attemptId
                            val initialLocateRequest = locateBaselineTracker.baselineForMount(
                                attemptId = locateAttemptId,
                                currentRequest = locateRequest,
                            )
                            AmapComposeMap(
                                model = ready.map.copy(
                                    viewportRequest = ready.map.viewportRequest?.copy(safeInsets = mapLayout.fitInsets),
                                ),
                                visibleInsets = mapLayout.visibleInsets,
                                onMarkerClick = onMarkerClick,
                                consent = token,
                                onMapPoiClick = onMapPoiClick,
                                layer = ready.mapLayer,
                                locateRequest = locateRequest,
                                initialLocateRequest = initialLocateRequest,
                                modifier = Modifier.fillMaxSize(),
                                hostFactory = mapHostFactory,
                                onLayerError = { _, retainedLayer ->
                                    if (attemptId == mapAttempt) {
                                        onAction(TripWorkspaceAction.SelectMapLayer(retainedLayer))
                                        layerFailureMessage = "图层切换失败，已保留当前图层"
                                    }
                                },
                                onLocateRequestConsumed = { request ->
                                    locateBaselineTracker.recordConsumedRequest(locateAttemptId, request)
                                },
                                onMapError = {
                                    if (attemptId == mapAttempt) {
                                        failedAttempt = attemptId
                                        pendingLocateRequest = locateBaselineTracker.finishFailedAttempt(
                                            attemptId = locateAttemptId,
                                            currentRequest = locateRequest,
                                        )
                                        mapHostState = MapHostState.Failed("地图加载失败")
                                    }
                                },
                                onMapReady = {
                                    if (attemptId == mapAttempt && failedAttempt != attemptId) {
                                        locateBaselineTracker.finishReadyAttempt(locateAttemptId)
                                        mapHostState = MapHostState.Ready
                                    }
                                },
                                onUserGesture = { onAction(TripWorkspaceAction.MapGesture) },
                                zoomInRequest = zoomInRequest,
                                zoomOutRequest = zoomOutRequest,
                                resetNorthRequest = resetNorthRequest,
                                onBearingChanged = { mapBearing = it },
                                readyTimeoutMillis = mapReadyTimeoutMillis,
                            )
                        }
                    },
                )
                WorkspaceRootLayer.Overlay -> ready?.let {
                    WorkspaceOverlayContent(
                        state = it,
                        placeState = placeState,
                        onPlaceAction = onPlaceAction,
                        itineraryState = itineraryState,
                        addToItineraryState = addToItineraryState,
                        onToggleAddPlace = onToggleAddPlace,
                        onContinueAddPlaces = onContinueAddPlaces,
                        onToggleAddTargetDay = onToggleAddTargetDay,
                        onGoToItineraryAddDay = onGoToItineraryAddDay,
                        onSelectAddTargetDay = onSelectAddTargetDay,
                        onSubmitAddPlaces = onSubmitAddPlaces,
                        onUndoAddPlaces = onUndoAddPlaces,
                        onRetryPartialAdd = onRetryPartialAdd,
                        onReselectAddTargetDays = onReselectAddTargetDays,
                        onViewAddResult = onViewAddResult,
                        onViewPlacePoolAfterUndo = onViewPlacePoolAfterUndo,
                        onItineraryAction = onItineraryAction,
                        onClose = onCloseOverlay,
                        onDismissMapPlace = onDismissMapPlace,
                        isPoiSaved = isPoiSaved,
                        collectionBusyPoiIds = collectionBusyPoiIds,
                        collectionError = collectionError,
                        onTogglePoiCollection = onTogglePoiCollection,
                        locationPermissionUiState = locationPermissionUiState,
                        onConfirmPermissionExplanation = onConfirmPermissionExplanation,
                        onDismissPermissionExplanation = onDismissPermissionExplanation,
                        onDismissLocationSettings = onDismissLocationSettings,
                        onOpenLocationSettings = onOpenLocationSettings,
                    )
                }
            }
        }
    }
}

@Composable
fun TripWorkspaceScreen(
    viewModel: TripWorkspaceViewModel,
    consent: AmapConsentToken?,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    onShareItinerary: () -> Unit = {},
    onPrivacySettings: () -> Unit = {},
    onOpenSearch: () -> Unit = {},
    placeContent: @Composable () -> Unit,
    dayItineraryContent: @Composable () -> Unit,
    isPoiSaved: Boolean = false,
    collectionBusyPoiIds: Set<String> = emptySet(),
    collectionError: String? = null,
    onTogglePoiCollection: (PlaceCandidate) -> Unit = {},
    mapHostFactory: (android.content.Context) -> AmapMapHost = { RealAmapMapHost.create(it) },
    mapReadyTimeoutMillis: Long = DEFAULT_MAP_READY_TIMEOUT_MILLIS,
) {
    val pageState by viewModel.pageState.collectAsStateWithLifecycle()
    TripWorkspaceScreen(
        pageState = pageState,
        consent = consent,
        onAction = { action ->
            when (action) {
                TripWorkspaceAction.ToggleCalendar -> viewModel.toggleCalendar()
                is TripWorkspaceAction.FocusCalendar -> viewModel.focusCalendar(action.dayId, action.itemId)
                is TripWorkspaceAction.SaveCalendar -> viewModel.saveCalendar(action.change)
                TripWorkspaceAction.UndoCalendar -> viewModel.undoCalendar()
                TripWorkspaceAction.RetryCalendar -> viewModel.retryCalendar()
                TripWorkspaceAction.DismissCalendarMessage -> viewModel.dismissCalendarMessage()
                TripWorkspaceAction.Back -> if (!viewModel.handleBack()) onBack()
                TripWorkspaceAction.LeaveWorkspace -> onBack()
                TripWorkspaceAction.ShareItinerary -> onShareItinerary()
                TripWorkspaceAction.OpenSettings -> onSettings()
                TripWorkspaceAction.OpenPrivacySettings -> onPrivacySettings()
                TripWorkspaceAction.OpenSearch -> onOpenSearch()
                TripWorkspaceAction.ZoomIn,
                TripWorkspaceAction.ZoomOut,
                TripWorkspaceAction.ResetNorth,
                TripWorkspaceAction.Locate -> Unit
                TripWorkspaceAction.MapGesture -> viewModel.onMapGesture()
                TripWorkspaceAction.Retry -> viewModel.retry()
                is TripWorkspaceAction.SelectSection -> viewModel.selectSection(action.section)
                is TripWorkspaceAction.SelectItineraryScope -> viewModel.selectItineraryScope(action.scope)
                is TripWorkspaceAction.SelectMapLayer -> viewModel.selectMapLayer(action.layer)
                is TripWorkspaceAction.SetSheetLevel -> viewModel.setSheetLevel(action.level)
                is TripWorkspaceAction.OpenOverlay -> viewModel.openOverlay(action.overlay)
                TripWorkspaceAction.CloseOverlay -> viewModel.closeOverlay()
            }
        },
        onMarkerClick = viewModel::selectMarker,
        onMapPoiClick = viewModel::selectMapPoi,
        placeState = PlacePoolUiState(),
        onPlaceAction = {},
        itineraryState = DayItineraryUiState(),
        onItineraryAction = {},
        placeContent = placeContent,
        dayItineraryContent = dayItineraryContent,
        onCloseOverlay = viewModel::closeOverlay,
        onDismissMapPlace = {
            viewModel.dismissPlaceCard()
            viewModel.dismissMarker()
        },
        isPoiSaved = isPoiSaved,
        collectionBusyPoiIds = collectionBusyPoiIds,
        collectionError = collectionError,
        onTogglePoiCollection = onTogglePoiCollection,
        mapHostFactory = mapHostFactory,
        mapReadyTimeoutMillis = mapReadyTimeoutMillis,
    )
}

@Composable
private fun WorkspaceOverlayContent(
    state: TripWorkspaceReadyState,
    placeState: PlacePoolUiState,
    onPlaceAction: (PlacePoolAction) -> Unit,
    itineraryState: DayItineraryUiState,
    addToItineraryState: AddToItineraryUiState,
    onToggleAddPlace: (String) -> Unit,
    onContinueAddPlaces: () -> Unit,
    onToggleAddTargetDay: (String) -> Unit,
    onGoToItineraryAddDay: () -> Unit,
    onSelectAddTargetDay: (String) -> Unit,
    onSubmitAddPlaces: () -> Unit,
    onUndoAddPlaces: () -> Unit,
    onRetryPartialAdd: () -> Unit,
    onReselectAddTargetDays: () -> Unit,
    onViewAddResult: (List<String>) -> Unit,
    onViewPlacePoolAfterUndo: () -> Unit,
    onItineraryAction: (DayItineraryAction) -> Unit,
    onClose: () -> Unit,
    onDismissMapPlace: () -> Unit,
    isPoiSaved: Boolean,
    collectionBusyPoiIds: Set<String>,
    collectionError: String?,
    onTogglePoiCollection: (PlaceCandidate) -> Unit,
    locationPermissionUiState: LocationPermissionUiState,
    onConfirmPermissionExplanation: () -> Unit,
    onDismissPermissionExplanation: () -> Unit,
    onDismissLocationSettings: () -> Unit,
    onOpenLocationSettings: () -> Unit,
) {
    when (val overlay = state.overlay) {
        WorkspaceOverlay.None, WorkspaceOverlay.LayerMenu, WorkspaceOverlay.MoreMenu -> Unit
        is WorkspaceOverlay.PlaceDetail -> {
            when {
                state.selectedMapPoi != null -> MapPoiDialog(state.selectedMapPoi, isPoiSaved, collectionBusyPoiIds, collectionError, onTogglePoiCollection, onDismissMapPlace)
                placeState.selectedDetailPlace != null || placeState.editing != null -> {
                    val place = placeState.editing ?: placeState.selectedDetailPlace
                    if (place != null) {
                        WorkspacePlaceDetailSheet(
                            state = placeState,
                            schedule = state.schedulesByPlaceId[place.id]
                                ?: PlaceScheduleSummaryUi(isKnown = false),
                            onAction = { action ->
                                val draft = placeState.detailDraft
                                when (action) {
                                    PlaceDetailPanelAction.Dismiss,
                                    PlaceDetailPanelAction.CancelEdit -> if (!placeState.detailSaving) {
                                        onPlaceAction(
                                            if (placeState.editing != null) PlacePoolAction.DismissDialogs
                                            else PlacePoolAction.DismissDetail,
                                        )
                                        onClose()
                                    }
                                    PlaceDetailPanelAction.StartEdit -> onPlaceAction(PlacePoolAction.Edit(place))
                                    PlaceDetailPanelAction.StartAddToItinerary -> onPlaceAction(PlacePoolAction.StartAddSingle(place.id))
                                    PlaceDetailPanelAction.Delete -> onPlaceAction(PlacePoolAction.Delete(place))
                                    is PlaceDetailPanelAction.NoteChanged -> draft?.let {
                                        onPlaceAction(PlacePoolAction.UpdateDraft(action.value, it.tags))
                                    }
                                    is PlaceDetailPanelAction.NewTagInputChanged ->
                                        onPlaceAction(PlacePoolAction.UpdateNewTagInput(action.value))
                                    PlaceDetailPanelAction.AddTag -> onPlaceAction(PlacePoolAction.AddTag)
                                    is PlaceDetailPanelAction.AddPresetTag -> draft?.let {
                                        onPlaceAction(PlacePoolAction.UpdateDraft(it.note, it.tags + action.name))
                                    }
                                    is PlaceDetailPanelAction.RemoveTag -> onPlaceAction(PlacePoolAction.RemoveTag(action.name))
                                    PlaceDetailPanelAction.SaveEdit -> draft?.let {
                                        onPlaceAction(PlacePoolAction.UpdateDetails(it.note, it.tags))
                                    }
                                    PlaceDetailPanelAction.ToggleCollection -> Unit
                                }
                            },
                        )
                    }
                }
                state.selectedMarker != null -> MarkerDialog(state.selectedMarker, state.selectedMarkerPoi, collectionBusyPoiIds, collectionError, onTogglePoiCollection, onDismissMapPlace)
            }
        }
        is WorkspaceOverlay.EditItineraryItem -> itineraryState.editDraft?.let { draft ->
            WorkspaceEditorSheet(
                onDismissRequest = {
                    when {
                        draft.isSaving -> Unit
                        draft.saveError != null -> onItineraryAction(DayItineraryAction.DismissEditSaveError)
                        else -> {
                            onItineraryAction(DayItineraryAction.DismissDialogs)
                            onClose()
                        }
                    }
                },
                confirmButton = {},
                text = {
                    if (draft.saveError != null) {
                        ItinerarySaveFailureContent(
                            onKeepEditing = { onItineraryAction(DayItineraryAction.DismissEditSaveError) },
                            onRetrySave = { onItineraryAction(DayItineraryAction.SaveEdit) },
                        )
                    } else {
                        EditItineraryItemContent(
                            draft = draft,
                            onArrivalTimeChange = { onItineraryAction(DayItineraryAction.UpdateArrivalTime(it)) },
                            onStayMinutesChange = { onItineraryAction(DayItineraryAction.UpdateStayMinutes(it)) },
                            onNoteChange = { onItineraryAction(DayItineraryAction.UpdateNote(it)) },
                            onScheduleAgain = draft.placeId?.let { { onItineraryAction(DayItineraryAction.ScheduleAgain(draft.itemId)) } },
                            onSave = { onItineraryAction(DayItineraryAction.SaveEdit) },
                            onCancel = { onItineraryAction(DayItineraryAction.DismissDialogs); onClose() },
                        )
                    }
                },
            )
        }
        is WorkspaceOverlay.SelectMoveTargetDay -> itineraryState.crossDayMove?.let { move ->
            AlertDialog(
                onDismissRequest = {
                    if (!move.isMoving) {
                        onItineraryAction(DayItineraryAction.DismissDialogs)
                        onClose()
                    }
                },
                title = { Text("移动到…") },
                text = {
                    Column {
                        move.moveError?.let { Text(it) }
                        itineraryState.days.filter { it.id != itineraryState.selectedDayId }.forEach { day ->
                            CompactSecondaryButton(
                                { onItineraryAction(DayItineraryAction.MoveToDay(day.id)) },
                                enabled = !move.isMoving,
                            ) { Text(com.yangchengwei.easytrip.itinerary.ui.moveTargetDayLabel(day.index, state.startDate)) }
                        }
                    }
                },
                confirmButton = {},
            )
        }
        WorkspaceOverlay.SelectAddPlaces -> WorkspaceEditorSheet(
            onDismissRequest = onClose,
            confirmButton = {},
            text = {
                SelectPlacesContent(
                    rows = placeState.allRows ?: placeState.rows,
                    state = addToItineraryState,
                    onTogglePlace = onToggleAddPlace,
                    onContinue = onContinueAddPlaces,
                    onClose = onClose,
                    targetDayLabel = (addToItineraryState.editingTarget as? com.yangchengwei.easytrip.itinerary.ui.AddToItineraryEditingTarget.ForDay)
                        ?.let { target -> state.days.firstOrNull { it.id == target.dayId } }
                        ?.let { "第 ${it.index + 1} 天" },
                    modifier = Modifier.height(548.dp),
                )
            },
        )
        WorkspaceOverlay.AddToItineraryResult -> if (!addToItineraryState.hasCompletedSuccessfully) AlertDialog(
            onDismissRequest = { if (!addToItineraryState.isSubmitting && !addToItineraryState.isUndoing) onClose() },
            confirmButton = {},
            text = {
                AddToItineraryResultContent(
                    state = addToItineraryState,
                    days = state.days,
                    placeNameForId = { placeId -> (placeState.allRows ?: placeState.rows).firstOrNull { it.place.id == placeId }?.place?.name },
                    onUndo = onUndoAddPlaces,
                    onRetryFailed = onRetryPartialAdd,
                    onReselectDates = onReselectAddTargetDays,
                    onViewResult = onViewAddResult,
                    onViewPlacePool = onViewPlacePoolAfterUndo,
                    onClose = onClose,
                )
            },
        )
        WorkspaceOverlay.SelectAddTargetDay -> WorkspaceEditorSheet(
            onDismissRequest = { if (!addToItineraryState.isSubmitting) onClose() },
            confirmButton = {},
            text = {
                SelectTargetDayContent(
                    days = state.days,
                    startDate = state.startDate,
                    schedule = addToItineraryState.editingTarget
                        ?.let { target ->
                            (target as? com.yangchengwei.easytrip.itinerary.ui.AddToItineraryEditingTarget.ForPlace)
                                ?.let { state.schedulesByPlaceId[it.placeId] }
                        },
                    selectedPlaceName = addToItineraryState.editingTarget
                        ?.let { target ->
                            (target as? com.yangchengwei.easytrip.itinerary.ui.AddToItineraryEditingTarget.ForPlace)
                                ?.let { selectedTarget ->
                                    placeState.rows.firstOrNull { it.place.id == selectedTarget.placeId }?.place?.name
                                }
                        },
                    state = addToItineraryState,
                    onSelectDay = onSelectAddTargetDay,
                    onToggleDay = onToggleAddTargetDay,
                    onSubmit = onSubmitAddPlaces,
                    onGoToItineraryAddDay = onGoToItineraryAddDay,
                    onClose = onClose,
                    modifier = Modifier.height(550.dp),
                )
            },
        )
        is WorkspaceOverlay.EditRouteLeg -> itineraryState.modeEditor?.let { editor ->
            WorkspaceEditorSheet(
                onDismissRequest = {
                    if (!editor.isSaving) {
                        onItineraryAction(DayItineraryAction.DismissDialogs)
                        onClose()
                    }
                },
                confirmButton = {},
                text = {
                    com.yangchengwei.easytrip.itinerary.ui.EditRouteLegContent(
                        draft = editor,
                        onSelectMode = { onItineraryAction(DayItineraryAction.SelectMode(it)) },
                        onClearSelectedModeOverride = { onItineraryAction(DayItineraryAction.ClearSelectedModeOverride) },
                        onDurationMinutesChange = { onItineraryAction(DayItineraryAction.UpdateRouteDurationMinutes(it)) },
                        onNoteChange = { onItineraryAction(DayItineraryAction.UpdateRouteNote(it)) },
                        onSave = { onItineraryAction(DayItineraryAction.SaveMode) },
                        onCancel = { onItineraryAction(DayItineraryAction.DismissDialogs); onClose() },
                    )
                },
            )
        }
        is WorkspaceOverlay.Confirmation -> {
            val itineraryDelete = itineraryState.deleteConfirmation
            if (itineraryDelete != null) {
                AlertDialog(
                    onDismissRequest = {
                        if (!itineraryDelete.isDeleting) {
                            onItineraryAction(DayItineraryAction.DismissDialogs)
                            onClose()
                        }
                    },
                    title = { Text("移出${itineraryDelete.placeName}？") },
                    text = {
                        Column {
                            Text("仅移除本次安排；收藏仍保留；相邻路线将重新计算。")
                            itineraryDelete.deleteError?.let { Text(it) }
                        }
                    },
                    confirmButton = {
                        CompactPrimaryButton(
                            onClick = { onItineraryAction(DayItineraryAction.ConfirmDelete) },
                            enabled = !itineraryDelete.isDeleting,
                        ) { Text(if (itineraryDelete.isDeleting) "移出中…" else "确认移出") }
                    },
                    dismissButton = {
                        CompactSecondaryButton(
                            onClick = {
                                onItineraryAction(DayItineraryAction.DismissDialogs)
                                onClose()
                            },
                            enabled = !itineraryDelete.isDeleting,
                        ) { Text("取消") }
                    },
                )
            } else {
                val placeBusy = placeState.deletionBusy || placeState.collectionBusyPoiIds.isNotEmpty()
                ConfirmationDialog(
                    model = overlay.model,
                    onConfirm = {
                        when {
                            placeState.pendingCollectionRemoval != null -> onPlaceAction(PlacePoolAction.ConfirmCollectionRemoval)
                            placeState.deleting != null -> onPlaceAction(PlacePoolAction.ConfirmDelete)
                        }
                    },
                    onDismiss = {
                        if (!placeBusy) {
                            onPlaceAction(PlacePoolAction.DismissDialogs)
                            onItineraryAction(DayItineraryAction.DismissDialogs)
                            onClose()
                        }
                    },
                    busy = placeBusy,
                    errorMessage = placeState.deletionError,
                )
            }
        }
        is WorkspaceOverlay.Feedback -> AlertDialog(onDismissRequest = onClose, title = { Text(overlay.model.message) }, confirmButton = { CompactSecondaryButton(onClose) { Text("关闭") } })
        WorkspaceOverlay.AddTripDay -> AlertDialog(
            onDismissRequest = { if (!itineraryState.isAppendingDay) onClose() },
            title = { Text("添加旅行日") },
            text = {
                AddTripDayContent(
                    isAppending = itineraryState.isAppendingDay,
                    error = itineraryState.appendDayError,
                    onConfirm = { onItineraryAction(DayItineraryAction.AppendTripDay) },
                    onClose = onClose,
                )
            },
            confirmButton = {},
        )
        is WorkspaceOverlay.PermissionExplanation -> when (overlay.kind) {
            PermissionKind.DEVICE_LOCATION -> PermissionExplanationContent(
                onConfirm = onConfirmPermissionExplanation,
                onDismiss = onDismissPermissionExplanation,
                busy = locationPermissionUiState.busy,
                error = locationPermissionUiState.error,
            )
            PermissionKind.DEVICE_LOCATION_SETTINGS -> LocationPermissionSettingsContent(
                onOpenSettings = onOpenLocationSettings,
                onDismiss = onDismissLocationSettings,
                busy = locationPermissionUiState.busy,
                error = locationPermissionUiState.error,
            )
        }
    }
}

@Composable
private fun MapPoiDialog(
    poi: MapPoiUi,
    isPoiSaved: Boolean,
    collectionBusyPoiIds: Set<String>,
    collectionError: String?,
    onTogglePoiCollection: (PlaceCandidate) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(poi.name) },
        text = { Column { Text(poi.address.ifBlank { "地址暂不可用" }); collectionError?.let { Text(it, color = MaterialTheme.colorScheme.error) } } },
        confirmButton = {
            val poiId = poi.poiId
            CompactPrimaryButton(
                onClick = { if (poiId != null) onTogglePoiCollection(PlaceCandidate(poiId, poi.name, poi.address, poi.point, null)) },
                enabled = poiId != null && poiId !in collectionBusyPoiIds,
                modifier = Modifier.testTag("place-card-collection"),
            ) { Text(if (poiId == null) "无法收藏" else if (isPoiSaved) "取消收藏" else "收藏") }
        },
        dismissButton = { CompactSecondaryButton(onDismiss) { Text("关闭") } },
    )
}

@Composable
private fun MarkerDialog(
    marker: MapMarkerUi,
    markerPoi: MapPoiUi?,
    collectionBusyPoiIds: Set<String>,
    collectionError: String?,
    onTogglePoiCollection: (PlaceCandidate) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(marker.label) },
        text = { Column { if (marker.occurrences.isEmpty()) Text("收藏地点"); marker.occurrences.forEach { Text("${it.dayLabel} · 第 ${it.order} 项 · ${it.placeName}") }; collectionError?.let { Text(it, color = MaterialTheme.colorScheme.error) } } },
        confirmButton = {
            if (markerPoi != null) CompactPrimaryButton(
                onClick = { onTogglePoiCollection(PlaceCandidate(requireNotNull(markerPoi.poiId), markerPoi.name, markerPoi.address, markerPoi.point, null)) },
                enabled = markerPoi.poiId !in collectionBusyPoiIds,
                modifier = Modifier.testTag("place-card-collection"),
            ) { Text("取消收藏") } else CompactPrimaryButton(onDismiss) { Text("关闭") }
        },
        dismissButton = markerPoi?.let { { CompactSecondaryButton(onDismiss) { Text("关闭") } } },
    )
}

private fun TransportMode.label() = when (this) {
    TransportMode.WALK -> "步行"
    TransportMode.TAXI -> "打车"
    TransportMode.DRIVE -> "驾车"
    TransportMode.TRANSIT -> "公交"
}
