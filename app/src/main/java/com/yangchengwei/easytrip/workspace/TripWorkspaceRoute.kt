package com.yangchengwei.easytrip.workspace

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yangchengwei.easytrip.amap.AmapConsentToken
import com.yangchengwei.easytrip.core.ui.component.ConfirmationUiModel
import com.yangchengwei.easytrip.itinerary.ui.AddToItineraryStep
import com.yangchengwei.easytrip.itinerary.ui.AddToItineraryUiState
import com.yangchengwei.easytrip.itinerary.ui.AddToItineraryViewModel
import com.yangchengwei.easytrip.itinerary.ui.DayItineraryAction
import com.yangchengwei.easytrip.itinerary.ui.DayItineraryUiState
import com.yangchengwei.easytrip.itinerary.ui.DayItineraryViewModel
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
        workspaceOverlay == WorkspaceOverlay.None ||
            workspaceOverlay.isAddToItineraryOverlay() && workspaceOverlay != desired
    }
}

fun workspaceBackDecision(
    overlay: WorkspaceOverlay,
    addToItinerary: AddToItineraryUiState,
    isAppendingDay: Boolean = false,
): WorkspaceBackDecision = when {
    overlay == WorkspaceOverlay.None -> WorkspaceBackDecision.LeaveWorkspace
    overlay == WorkspaceOverlay.AddTripDay && isAppendingDay -> WorkspaceBackDecision.Ignore
    !canDismissAddOverlay(overlay, addToItinerary) -> WorkspaceBackDecision.Ignore
    else -> WorkspaceBackDecision.CloseOverlay
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
) {
    val page = viewModel.pageState.collectAsStateWithLifecycle().value
    val places = placeViewModel?.state?.collectAsStateWithLifecycle()?.value ?: placeState
    val itinerary = itineraryViewModel?.state?.collectAsStateWithLifecycle()?.value ?: itineraryState
    val addToItinerary = addToItineraryViewModel?.state?.collectAsStateWithLifecycle()?.value ?: AddToItineraryUiState()
    val ready = (page as? TripWorkspacePageState.Ready)?.content
    val dispatchPlace: (PlacePoolAction) -> Unit = placeViewModel?.let { it::dispatch } ?: onPlaceAction
    val dispatchItinerary: (DayItineraryAction) -> Unit = itineraryViewModel?.let { it::dispatch } ?: onItineraryAction

    fun dismissPendingDialogs() {
        placeViewModel?.dismissDialogs()
        itineraryViewModel?.dismissDialogs()
    }
    fun closeOverlay() {
        val overlay = ready?.overlay ?: WorkspaceOverlay.None
        if (!canDismissAddOverlay(overlay, addToItinerary) || overlay == WorkspaceOverlay.AddTripDay && itinerary.isAppendingDay) return
        dismissPendingDialogs()
        if (overlay.isAddToItineraryOverlay()) addToItineraryViewModel?.cancel()
        viewModel.closeOverlay()
    }
    fun leaveOrCloseOverlay() {
        when (workspaceBackDecision(ready?.overlay ?: WorkspaceOverlay.None, addToItinerary, itinerary.isAppendingDay)) {
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
                        message = "将同时删除 ${pending.usageCount} 次行程安排及受影响路线。",
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
    LaunchedEffect(places.deleting) {
        places.deleting?.let { place ->
            viewModel.openOverlay(
                WorkspaceOverlay.Confirmation(
                    confirmation(
                        title = "删除 ${place.name}？",
                        message = "将同时删除 ${places.deletionUsageCount} 次行程安排及受影响路线。",
                        confirmLabel = "确认删除地点",
                    ),
                ),
            )
        }
    }
    LaunchedEffect(itinerary.editDraft) {
        if (itinerary.editDraft == null && ready?.overlay is WorkspaceOverlay.EditItineraryItem) viewModel.closeOverlay()
    }
    LaunchedEffect(itinerary.deleteConfirmation) {
        if (itinerary.deleteConfirmation == null && ready?.overlay is WorkspaceOverlay.Confirmation && places.pendingCollectionRemoval == null && places.deleting == null) {
            viewModel.closeOverlay()
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
                    addToItineraryViewModel?.startFromPool()
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
                    addToItineraryViewModel?.startForDay(it)
                }
                is DayItineraryAction.RequestTiming -> {
                    dismissPendingDialogs()
                    dispatchItinerary(action)
                    viewModel.openOverlay(WorkspaceOverlay.EditItineraryItem(action.itemId))
                }
                is DayItineraryAction.RequestCrossDay -> {
                    dismissPendingDialogs()
                    dispatchItinerary(action)
                    viewModel.openOverlay(WorkspaceOverlay.SelectMoveTargetDay(action.itemId))
                }
                is DayItineraryAction.RequestDelete -> {
                    dismissPendingDialogs()
                    dispatchItinerary(action)
                    val placeName = itinerary.items.firstOrNull { it.id == action.itemId }?.name.orEmpty()
                    viewModel.openOverlay(
                        WorkspaceOverlay.Confirmation(
                            confirmation(
                                "移出${placeName.ifBlank { "该地点" }}？",
                                "仅从当天行程移出，收藏仍保留。",
                                "确认移出",
                            ),
                        ),
                    )
                }
                is DayItineraryAction.RequestMode -> {
                    dismissPendingDialogs()
                    dispatchItinerary(action)
                    viewModel.openOverlay(WorkspaceOverlay.EditRouteLeg(stableWorkspaceOverlayId(action.legId)))
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
