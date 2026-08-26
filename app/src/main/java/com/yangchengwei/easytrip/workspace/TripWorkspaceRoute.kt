package com.yangchengwei.easytrip.workspace

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yangchengwei.easytrip.permission.LocationPermissionCoordinator
import com.yangchengwei.easytrip.permission.LocationPermissionSnapshot
import com.yangchengwei.easytrip.permission.WorkspaceEffect
import com.yangchengwei.easytrip.amap.AmapConsentToken
import com.yangchengwei.easytrip.core.ui.component.ConfirmationUiModel
import com.yangchengwei.easytrip.itinerary.ui.AddToItineraryStep
import com.yangchengwei.easytrip.itinerary.ui.AddToItineraryUiState
import com.yangchengwei.easytrip.itinerary.ui.AddToItineraryViewModel
import com.yangchengwei.easytrip.itinerary.ui.DayItineraryAction
import com.yangchengwei.easytrip.itinerary.ui.DayItineraryUiState
import com.yangchengwei.easytrip.itinerary.ui.DayItineraryViewModel
import com.yangchengwei.easytrip.itinerary.ui.CrossDayMoveDraft
import com.yangchengwei.easytrip.itinerary.ui.ItineraryDeleteConfirmation
import com.yangchengwei.easytrip.itinerary.ui.ItineraryEditDraft
import com.yangchengwei.easytrip.itinerary.ui.RouteModeEditDraft
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.ui.PlacePoolAction
import com.yangchengwei.easytrip.place.ui.PlacePoolUiState
import com.yangchengwei.easytrip.place.ui.PlacePoolViewModel

enum class WorkspaceBackDecision { Ignore, CloseOverlay, LeaveWorkspace }

fun WorkspaceOverlay.isAddToItineraryOverlay(): Boolean =
    this == WorkspaceOverlay.SelectAddPlaces ||
        this == WorkspaceOverlay.SelectAddTargetDay ||
        this == WorkspaceOverlay.AddToItineraryResult

fun canDismissAddOverlay(overlay: WorkspaceOverlay, addToItinerary: AddToItineraryUiState): Boolean =
    !overlay.isAddToItineraryOverlay() || (!addToItinerary.isSubmitting && !addToItinerary.isUndoing)

internal fun addOverlayToPresent(
    workspaceOverlay: WorkspaceOverlay,
    addToItinerary: AddToItineraryUiState,
): WorkspaceOverlay? {
    val desired = when {
        addToItinerary.result is com.yangchengwei.easytrip.itinerary.domain.AddPlacesOutcome.PartialSuccess ||
            addToItinerary.result is com.yangchengwei.easytrip.itinerary.domain.AddPlacesOutcome.TargetDayMissing ->
            WorkspaceOverlay.AddToItineraryResult
        addToItinerary.step == AddToItineraryStep.SELECT_PLACES -> WorkspaceOverlay.SelectAddPlaces
        addToItinerary.step == AddToItineraryStep.SELECT_TARGET_DAY -> WorkspaceOverlay.SelectAddTargetDay
        addToItinerary.step == AddToItineraryStep.COMPLETED -> WorkspaceOverlay.AddToItineraryResult
        else -> null
    }
    return desired?.takeIf {
        workspaceOverlay.isAddToItineraryOverlay() && workspaceOverlay != desired
    }
}

fun canDismissWorkspaceOverlay(
    overlay: WorkspaceOverlay,
    addToItinerary: AddToItineraryUiState,
    itinerary: DayItineraryUiState = DayItineraryUiState(),
    hasPlaceDeleteConfirmation: Boolean = false,
    placeDeletionBusy: Boolean = false,
): Boolean = when {
    overlay == WorkspaceOverlay.AddTripDay && itinerary.isAppendingDay -> false
    overlay is WorkspaceOverlay.EditItineraryItem && itinerary.editDraft?.isSaving == true -> false
    overlay is WorkspaceOverlay.SelectMoveTargetDay && itinerary.crossDayMove?.isMoving == true -> false
    overlay is WorkspaceOverlay.EditRouteLeg && itinerary.modeEditor?.isSaving == true -> false
    overlay is WorkspaceOverlay.Confirmation && placeDeletionBusy -> false
    overlay is WorkspaceOverlay.Confirmation &&
        itinerary.deleteConfirmation?.isDeleting == true &&
        !hasPlaceDeleteConfirmation -> false
    else -> canDismissAddOverlay(overlay, addToItinerary)
}

fun workspaceBackDecision(
    overlay: WorkspaceOverlay,
    addToItinerary: AddToItineraryUiState,
    isAppendingDay: Boolean = false,
    itinerary: DayItineraryUiState = DayItineraryUiState(isAppendingDay = isAppendingDay),
    hasPlaceDeleteConfirmation: Boolean = false,
    placeDeletionBusy: Boolean = false,
): WorkspaceBackDecision = when {
    overlay == WorkspaceOverlay.None -> WorkspaceBackDecision.LeaveWorkspace
    !canDismissWorkspaceOverlay(overlay, addToItinerary, itinerary, hasPlaceDeleteConfirmation, placeDeletionBusy) -> WorkspaceBackDecision.Ignore
    else -> WorkspaceBackDecision.CloseOverlay
}

internal fun itineraryOverlayToPresent(
    editDraft: ItineraryEditDraft?,
    crossDayMove: CrossDayMoveDraft?,
    deleteConfirmation: ItineraryDeleteConfirmation?,
    modeEditor: RouteModeEditDraft?,
): WorkspaceOverlay? = when {
    editDraft != null -> WorkspaceOverlay.EditItineraryItem(editDraft.itemId)
    crossDayMove != null -> WorkspaceOverlay.SelectMoveTargetDay(crossDayMove.itemId)
    deleteConfirmation != null -> WorkspaceOverlay.Confirmation(
        confirmation(
            "移出${deleteConfirmation.placeName}？",
            "仅从当天行程移出，收藏仍保留。",
            "确认移出",
        ),
    )
    modeEditor != null -> WorkspaceOverlay.EditRouteLeg(stableWorkspaceOverlayId(modeEditor.legId))
    else -> null
}

internal fun itineraryOverlayUpdate(
    current: WorkspaceOverlay,
    editDraft: ItineraryEditDraft?,
    crossDayMove: CrossDayMoveDraft?,
    deleteConfirmation: ItineraryDeleteConfirmation?,
    modeEditor: RouteModeEditDraft?,
): WorkspaceOverlay? {
    val desired = itineraryOverlayToPresent(editDraft, crossDayMove, deleteConfirmation, modeEditor)
    return when {
        desired != null && (
            current == WorkspaceOverlay.None ||
                current is WorkspaceOverlay.EditItineraryItem ||
                current is WorkspaceOverlay.SelectMoveTargetDay ||
                current is WorkspaceOverlay.EditRouteLeg ||
                current is WorkspaceOverlay.Confirmation
            ) -> desired
        desired == null && (
            current is WorkspaceOverlay.EditItineraryItem ||
                current is WorkspaceOverlay.SelectMoveTargetDay ||
                current is WorkspaceOverlay.EditRouteLeg
            ) -> null
        else -> current
    }
}

internal sealed interface AppendDayCompletionDecision {
    data object None : AppendDayCompletionDecision
    data class Consume(val token: Long) : AppendDayCompletionDecision
    data class CloseOverlayAndConsume(val token: Long) : AppendDayCompletionDecision
}

internal fun appendDayCompletionDecision(
    overlay: WorkspaceOverlay,
    completionToken: Long?,
): AppendDayCompletionDecision = when {
    completionToken == null -> AppendDayCompletionDecision.None
    overlay == WorkspaceOverlay.AddTripDay -> AppendDayCompletionDecision.CloseOverlayAndConsume(completionToken)
    else -> AppendDayCompletionDecision.Consume(completionToken)
}

@Composable
fun TripWorkspaceRoute(
    viewModel: TripWorkspaceViewModel,
    consent: AmapConsentToken?,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    onPrivacySettings: () -> Unit = {},
    onOpenSearch: () -> Unit = {},
    placeViewModel: PlacePoolViewModel? = null,
    itineraryViewModel: DayItineraryViewModel? = null,
    addToItineraryViewModel: AddToItineraryViewModel? = null,
    placeState: PlacePoolUiState = PlacePoolUiState(),
    onPlaceAction: (PlacePoolAction) -> Unit = {},
    itineraryState: DayItineraryUiState = DayItineraryUiState(),
    onItineraryAction: (DayItineraryAction) -> Unit = {},
    isPoiSaved: Boolean? = null,
    collectionBusyPoiIds: Set<String>? = null,
    collectionError: String? = null,
    onTogglePoiCollection: ((PlaceCandidate) -> Unit)? = null,
    mapHostFactory: (android.content.Context) -> AmapMapHost = { RealAmapMapHost.create(it) },
    searchReturn: WorkspaceSearchReturn? = null,
    onConsumeSearchReturn: () -> Unit = {},
    locationPermissionCoordinator: LocationPermissionCoordinator,
    locationPermissionSnapshot: () -> LocationPermissionSnapshot,
    onWorkspaceEffect: (WorkspaceEffect) -> Unit,
) {
    val page = viewModel.pageState.collectAsStateWithLifecycle().value
    val places = placeViewModel?.state?.collectAsStateWithLifecycle()?.value ?: placeState
    val itinerary = itineraryViewModel?.state?.collectAsStateWithLifecycle()?.value ?: itineraryState
    val addToItinerary = addToItineraryViewModel?.state?.collectAsStateWithLifecycle()?.value ?: AddToItineraryUiState()
    val ready = (page as? TripWorkspacePageState.Ready)?.content
    val dispatchPlace: (PlacePoolAction) -> Unit = placeViewModel?.let { it::dispatch } ?: onPlaceAction
    val dispatchItinerary: (DayItineraryAction) -> Unit = itineraryViewModel?.let { it::dispatch } ?: onItineraryAction
    val permissionExplanation = locationPermissionCoordinator.explanation.collectAsStateWithLifecycle().value
    var locateRequest by remember { mutableIntStateOf(0) }

    LaunchedEffect(locationPermissionCoordinator) {
        locationPermissionCoordinator.effectFlow.collect { effect ->
            if (effect == WorkspaceEffect.ShowCurrentLocation) locateRequest++ else onWorkspaceEffect(effect)
        }
    }
    LaunchedEffect(permissionExplanation, ready?.overlay) {
        val explanation = permissionExplanation
        if (explanation != null && ready?.overlay != WorkspaceOverlay.PermissionExplanation(explanation)) {
            viewModel.openOverlay(WorkspaceOverlay.PermissionExplanation(explanation))
        } else if (explanation == null && ready?.overlay is WorkspaceOverlay.PermissionExplanation) {
            viewModel.closeOverlay()
        }
    }

    fun dismissPendingDialogs() {
        placeViewModel?.dismissDialogs()
        itineraryViewModel?.dismissDialogs()
    }
    fun closeOverlay() {
        val overlay = viewModel.state.value.overlay
        if (!canDismissWorkspaceOverlay(
                overlay,
                addToItinerary,
                itinerary,
                hasPlaceDeleteConfirmation = places.pendingCollectionRemoval != null || places.deleting != null,
                placeDeletionBusy = places.deletionBusy || places.collectionBusyPoiIds.isNotEmpty(),
            )
        ) return
        dismissPendingDialogs()
        if (overlay.isAddToItineraryOverlay()) addToItineraryViewModel?.cancel()
        if (overlay is WorkspaceOverlay.PermissionExplanation) locationPermissionCoordinator.dismissExplanation()
        viewModel.closeOverlay()
    }
    fun leaveOrCloseOverlay() {
        when (
            workspaceBackDecision(
                viewModel.state.value.overlay,
                addToItinerary,
                itinerary = itinerary,
                hasPlaceDeleteConfirmation = places.pendingCollectionRemoval != null || places.deleting != null,
                placeDeletionBusy = places.deletionBusy || places.collectionBusyPoiIds.isNotEmpty(),
            )
        ) {
            WorkspaceBackDecision.Ignore -> Unit
            WorkspaceBackDecision.CloseOverlay -> closeOverlay()
            WorkspaceBackDecision.LeaveWorkspace -> {
                dismissPendingDialogs()
                onBack()
            }
        }
    }

    LaunchedEffect(ready?.days, places.rows) {
        addToItineraryViewModel?.reconcile(
            ready?.days.orEmpty().map { it.id },
            places.rows.mapTo(mutableSetOf()) { it.place.id },
        )
    }
    LaunchedEffect(addToItinerary.step, addToItinerary.result, ready?.overlay) {
        addOverlayToPresent(ready?.overlay ?: WorkspaceOverlay.None, addToItinerary)?.let(viewModel::openOverlay)
    }
    LaunchedEffect(itinerary.appendDayCompletionToken) {
        when (
            val decision = appendDayCompletionDecision(
                ready?.overlay ?: WorkspaceOverlay.None,
                itinerary.appendDayCompletionToken,
            )
        ) {
            AppendDayCompletionDecision.None -> Unit
            is AppendDayCompletionDecision.Consume -> itineraryViewModel?.consumeAppendDayCompletion(decision.token)
            is AppendDayCompletionDecision.CloseOverlayAndConsume -> {
                viewModel.closeOverlay()
                itineraryViewModel?.consumeAppendDayCompletion(decision.token)
            }
        }
    }

    LaunchedEffect(places.pendingCollectionRemoval) {
        places.pendingCollectionRemoval?.let { pending ->
            viewModel.openOverlay(
                WorkspaceOverlay.Confirmation(
                    confirmation(
                        title = "取消收藏 ${pending.place.name}？",
                        message = "将同时删除 ${pending.impact.itineraryItemCount} 次行程安排和 ${pending.impact.routeLegCount} 段路线。",
                        confirmLabel = "确认取消收藏",
                    ),
                ),
            )
        }
    }
    LaunchedEffect(places.editing, ready?.overlay) {
        if (places.editing == null && ready?.overlay is WorkspaceOverlay.PlaceDetail && ready.selectedMapPoi == null && ready.selectedMarker == null) {
            viewModel.closeOverlay()
        }
    }
    LaunchedEffect(places.deleting, places.deletionImpact) {
        places.deleting?.let { place ->
            viewModel.openOverlay(
                WorkspaceOverlay.Confirmation(
                    confirmation(
                        title = "删除 ${place.name}？",
                        message = places.deletionImpact?.let { impact ->
                            "将同时删除 ${impact.itineraryItemCount} 次行程安排和 ${impact.routeLegCount} 段路线。"
                        }.orEmpty(),
                        confirmLabel = "确认删除地点",
                    ),
                ),
            )
        }
    }
    LaunchedEffect(places.pendingCollectionRemoval, places.deleting, ready?.overlay) {
        if (
            places.pendingCollectionRemoval == null &&
            places.deleting == null &&
            itinerary.deleteConfirmation == null &&
            ready?.overlay is WorkspaceOverlay.Confirmation
        ) {
            viewModel.closeOverlay()
        }
    }
    LaunchedEffect(
        itinerary.editDraft,
        itinerary.crossDayMove,
        itinerary.deleteConfirmation,
        itinerary.modeEditor,
        ready?.overlay,
    ) {
        val overlay = ready?.overlay ?: WorkspaceOverlay.None
        when {
            itinerary.editDraft != null ||
                itinerary.crossDayMove != null ||
                itinerary.deleteConfirmation != null ||
                itinerary.modeEditor != null -> {
                val desired = itineraryOverlayUpdate(
                    overlay,
                    itinerary.editDraft,
                    itinerary.crossDayMove,
                    itinerary.deleteConfirmation,
                    itinerary.modeEditor,
                )
                if (desired != null && desired != overlay) viewModel.openOverlay(desired)
            }
            overlay is WorkspaceOverlay.EditItineraryItem ||
                overlay is WorkspaceOverlay.SelectMoveTargetDay ||
                overlay is WorkspaceOverlay.EditRouteLeg -> viewModel.closeOverlay()
        }
    }

    BackHandler(onBack = ::leaveOrCloseOverlay)
    TripWorkspaceScreen(
        pageState = page,
        consent = consent,
        onAction = { action ->
            when (action) {
                TripWorkspaceAction.Back -> leaveOrCloseOverlay()
                TripWorkspaceAction.OpenSettings -> onSettings()
                TripWorkspaceAction.OpenPrivacySettings -> onPrivacySettings()
                TripWorkspaceAction.OpenSearch -> onOpenSearch()
                TripWorkspaceAction.Locate -> locationPermissionCoordinator.onLocateClick(locationPermissionSnapshot())
                TripWorkspaceAction.Retry -> viewModel.retry()
                is TripWorkspaceAction.SelectSection -> {
                    if (shouldConsumeSearchReturn(ready?.section, action.section)) onConsumeSearchReturn()
                    viewModel.selectSection(action.section)
                }
                is TripWorkspaceAction.SelectItineraryScope -> viewModel.selectItineraryScope(action.scope)
                is TripWorkspaceAction.SelectMapLayer -> viewModel.selectMapLayer(action.layer)
                is TripWorkspaceAction.SetSheetLevel -> viewModel.setSheetLevel(action.level)
                is TripWorkspaceAction.OpenOverlay -> {
                    dismissPendingDialogs()
                    viewModel.openOverlay(action.overlay)
                }
                TripWorkspaceAction.CloseOverlay -> closeOverlay()
            }
        },
        onMarkerClick = viewModel::selectMarker,
        onMapPoiClick = viewModel::selectMapPoi,
        onConfirmPermissionExplanation = { locationPermissionCoordinator.confirmExplanation() },
        onDismissPermissionExplanation = { locationPermissionCoordinator.dismissExplanation() },
        placeState = places,
        onPlaceAction = { action ->
            when (action) {
                is PlacePoolAction.Edit -> {
                    dismissPendingDialogs()
                    dispatchPlace(action)
                    viewModel.openOverlay(WorkspaceOverlay.PlaceDetail(stableWorkspaceOverlayId(action.place.id)))
                }
                is PlacePoolAction.Delete -> {
                    dismissPendingDialogs()
                    viewModel.closeOverlay()
                    dispatchPlace(action)
                }
                PlacePoolAction.StartAddToItinerary -> {
                    dismissPendingDialogs()
                    if (addToItineraryViewModel?.startFromPool() == true) {
                        viewModel.openOverlay(WorkspaceOverlay.SelectAddPlaces)
                    }
                }
                is PlacePoolAction.StartAddSingle -> {
                    dismissPendingDialogs()
                    if (ready?.days.isNullOrEmpty()) {
                        viewModel.openOverlay(WorkspaceOverlay.AddTripDay)
                    } else if (addToItineraryViewModel?.startForPlace(action.placeId) == true) {
                        viewModel.openOverlay(WorkspaceOverlay.SelectAddTargetDay)
                    }
                }
                PlacePoolAction.DismissDialogs -> closeOverlay()
                else -> dispatchPlace(action)
            }
        },
        itineraryState = itinerary,
        addToItineraryState = addToItinerary,
        onToggleAddPlace = { addToItineraryViewModel?.togglePlace(it) },
        onContinueAddPlaces = { addToItineraryViewModel?.continueToTargetDay() },
        onSelectAddTargetDay = { addToItineraryViewModel?.selectTargetDay(it) },
        onSubmitAddPlaces = { addToItineraryViewModel?.submit() },
        onUndoAddPlaces = { addToItineraryViewModel?.undo() },
        onRetryPartialAdd = { addToItineraryViewModel?.retryPartial() },
        onItineraryAction = { action ->
            when (action) {
                DayItineraryAction.AppendTripDay -> dispatchItinerary(action)
                DayItineraryAction.AddPlaces -> itinerary.selectedDayId?.let {
                    if (addToItineraryViewModel?.startForDay(it) == true) {
                        viewModel.openOverlay(WorkspaceOverlay.SelectAddPlaces)
                    }
                }
                is DayItineraryAction.RequestTiming -> {
                    dismissPendingDialogs()
                    dispatchItinerary(action)
                }
                is DayItineraryAction.RequestCrossDay -> {
                    dismissPendingDialogs()
                    dispatchItinerary(action)
                }
                is DayItineraryAction.RequestDelete -> {
                    dismissPendingDialogs()
                    dispatchItinerary(action)
                }
                is DayItineraryAction.RequestMode -> {
                    dismissPendingDialogs()
                    dispatchItinerary(action)
                }
                DayItineraryAction.DismissDialogs -> closeOverlay()
                else -> dispatchItinerary(action)
            }
        },
        onCloseOverlay = ::closeOverlay,
        onDismissMapPlace = {
            viewModel.dismissPlaceCard()
            viewModel.dismissMarker()
        },
        isPoiSaved = isPoiSaved ?: (ready?.selectedMapPoi?.poiId in places.savedPoiIds),
        collectionBusyPoiIds = collectionBusyPoiIds ?: places.collectionBusyPoiIds,
        collectionError = collectionError ?: places.collectionError,
        onTogglePoiCollection = onTogglePoiCollection ?: { candidate ->
            viewModel.retainViewportForPlaceCardCollection()
            placeViewModel?.toggleCollection(candidate) ?: onPlaceAction(PlacePoolAction.ToggleCollection(candidate))
        },
        mapHostFactory = mapHostFactory,
        locateRequest = locateRequest,
        searchReturn = searchReturn,
    )
}

internal fun shouldConsumeSearchReturn(current: WorkspaceSection?, selected: WorkspaceSection) = current != selected

private fun confirmation(title: String, message: String, confirmLabel: String) = ConfirmationUiModel(
    title = title,
    message = message,
    deletedItems = emptyList(),
    retainedItems = emptyList(),
    confirmLabel = confirmLabel,
    dismissLabel = "取消",
    destructive = true,
    reversible = false,
)

internal fun stableWorkspaceOverlayId(value: String): Long = value.hashCode().toLong() and 0xffffffffL
