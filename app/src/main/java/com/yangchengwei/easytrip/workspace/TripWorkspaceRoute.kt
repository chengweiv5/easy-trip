package com.yangchengwei.easytrip.workspace

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yangchengwei.easytrip.amap.AmapConsentToken
import com.yangchengwei.easytrip.core.ui.component.ConfirmationUiModel
import com.yangchengwei.easytrip.itinerary.ui.DayItineraryAction
import com.yangchengwei.easytrip.itinerary.ui.DayItineraryUiState
import com.yangchengwei.easytrip.itinerary.ui.DayItineraryViewModel
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.ui.PlacePoolAction
import com.yangchengwei.easytrip.place.ui.PlacePoolUiState
import com.yangchengwei.easytrip.place.ui.PlacePoolViewModel

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
    placeState: PlacePoolUiState = PlacePoolUiState(),
    onPlaceAction: (PlacePoolAction) -> Unit = {},
    itineraryState: DayItineraryUiState = DayItineraryUiState(),
    onItineraryAction: (DayItineraryAction) -> Unit = {},
    isPoiSaved: Boolean? = null,
    collectionBusyPoiIds: Set<String>? = null,
    collectionError: String? = null,
    onTogglePoiCollection: ((PlaceCandidate) -> Unit)? = null,
    mapHostFactory: (android.content.Context) -> AmapMapHost = { RealAmapMapHost.create(it) },
) {
    val page = viewModel.pageState.collectAsStateWithLifecycle().value
    val places = placeViewModel?.state?.collectAsStateWithLifecycle()?.value ?: placeState
    val itinerary = itineraryViewModel?.state?.collectAsStateWithLifecycle()?.value ?: itineraryState
    val ready = (page as? TripWorkspacePageState.Ready)?.content
    val dispatchPlace: (PlacePoolAction) -> Unit = placeViewModel?.let { it::dispatch } ?: onPlaceAction
    val dispatchItinerary: (DayItineraryAction) -> Unit = itineraryViewModel?.let { it::dispatch } ?: onItineraryAction

    fun dismissPendingDialogs() {
        placeViewModel?.dismissDialogs()
        itineraryViewModel?.dismissDialogs()
    }
    fun closeOverlay() {
        dismissPendingDialogs()
        viewModel.closeOverlay()
    }
    fun leaveOrCloseOverlay() {
        val overlayClosed = viewModel.handleBack()
        dismissPendingDialogs()
        if (!overlayClosed) onBack()
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
                is TripWorkspaceAction.SelectSection -> viewModel.selectSection(action.section)
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
                PlacePoolAction.DismissDialogs -> closeOverlay()
                else -> dispatchPlace(action)
            }
        },
        itineraryState = itinerary,
        onItineraryAction = { action ->
            when (action) {
                is DayItineraryAction.RequestTiming -> {
                    dismissPendingDialogs()
                    dispatchItinerary(action)
                    viewModel.openOverlay(WorkspaceOverlay.EditItineraryItem(stableWorkspaceOverlayId(action.itemId)))
                }
                is DayItineraryAction.RequestCrossDay -> {
                    dismissPendingDialogs()
                    dispatchItinerary(action)
                    viewModel.openOverlay(WorkspaceOverlay.SelectTargetDay(setOf(stableWorkspaceOverlayId(action.itemId))))
                }
                is DayItineraryAction.RequestDelete -> {
                    dismissPendingDialogs()
                    dispatchItinerary(action)
                    viewModel.openOverlay(
                        WorkspaceOverlay.Confirmation(
                            confirmation("删除这次安排？", "该安排将从当前日程中移除。", "确认删除"),
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
    )
}

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
