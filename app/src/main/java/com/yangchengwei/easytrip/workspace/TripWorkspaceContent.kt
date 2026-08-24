package com.yangchengwei.easytrip.workspace

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.ui.component.CompactSecondaryButton
import com.yangchengwei.easytrip.core.ui.component.SelectablePill
import com.yangchengwei.easytrip.itinerary.ui.DayItineraryAction
import com.yangchengwei.easytrip.itinerary.ui.DayItineraryContent
import com.yangchengwei.easytrip.itinerary.ui.DayItineraryUiState
import com.yangchengwei.easytrip.itinerary.ui.WorkspaceItineraryContent
import com.yangchengwei.easytrip.place.ui.PlacePoolAction
import com.yangchengwei.easytrip.place.ui.PlacePoolContent
import com.yangchengwei.easytrip.place.ui.PlacePoolUiState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripWorkspaceContent(
    pageState: TripWorkspacePageState,
    mapState: WorkspaceMapState,
    onAction: (TripWorkspaceAction) -> Unit,
    onPageRetry: () -> Unit = { onAction(TripWorkspaceAction.Retry) },
    onMapRetry: () -> Unit = { onAction(TripWorkspaceAction.Retry) },
    placeState: PlacePoolUiState,
    onPlaceAction: (PlacePoolAction) -> Unit,
    itineraryState: DayItineraryUiState,
    onItineraryAction: (DayItineraryAction) -> Unit,
    mapContent: @Composable BoxScope.() -> Unit,
    placeContent: (@Composable () -> Unit)? = null,
    dayItineraryContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    searchReturn: WorkspaceSearchReturn? = null,
) {
    when (pageState) {
        TripWorkspacePageState.Loading -> WorkspacePageMessage("旅行加载中")
        TripWorkspacePageState.NotFound -> WorkspacePageMessage("旅行不存在", "返回旅行列表") { onAction(TripWorkspaceAction.Back) }
        is TripWorkspacePageState.Error -> WorkspacePageMessage(pageState.message, "重试", onPageRetry)
        is TripWorkspacePageState.Ready -> WorkspaceReadyContent(
            pageState.content,
            mapState,
            onAction,
            onMapRetry,
            placeState,
            onPlaceAction,
            itineraryState,
            onItineraryAction,
            mapContent,
            placeContent,
            dayItineraryContent,
            modifier,
            searchReturn,
        )
    }
}

@Composable
private fun WorkspacePageMessage(message: String, action: String? = null, onAction: () -> Unit = {}) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(message)
        action?.let { CompactSecondaryButton(onAction) { Text(it) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkspaceReadyContent(
    state: TripWorkspaceReadyState,
    mapState: WorkspaceMapState,
    onAction: (TripWorkspaceAction) -> Unit,
    onMapRetry: () -> Unit,
    placeState: PlacePoolUiState,
    onPlaceAction: (PlacePoolAction) -> Unit,
    itineraryState: DayItineraryUiState,
    onItineraryAction: (DayItineraryAction) -> Unit,
    mapContent: @Composable BoxScope.() -> Unit,
    placeContent: (@Composable () -> Unit)?,
    dayItineraryContent: (@Composable () -> Unit)?,
    modifier: Modifier,
    searchReturn: WorkspaceSearchReturn?,
) {
    WorkspaceBottomSheet(
        value = state.sheetLevel,
        onValueChange = { onAction(TripWorkspaceAction.SetSheetLevel(it)) },
        modifier = modifier.windowInsetsPadding(WindowInsets.safeDrawing).imePadding(),
        searchReturn = searchReturn != null,
        header = {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                WorkspaceSheetHandle()
                if (state.sheetLevel != WorkspaceSheetLevel.COLLAPSED) {
                    WorkspaceTabs(
                        selected = state.section,
                        onSelect = { onAction(TripWorkspaceAction.SelectSection(it)) },
                    )
                }
            }
        },
        content = {
            Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                when (state.section) {
                    WorkspaceSection.PLACE_POOL -> if (placeContent != null) placeContent() else PlacePoolContent(
                        state = placeState.copy(
                            rows = placeState.rows.map { row ->
                                row.copy(recentlyCollected = row.recentlyCollected || row.place.amapPoiId in searchReturn?.recentlyCollectedPoiIds.orEmpty())
                            },
                        ),
                        modifier = Modifier.weight(1f),
                        showSearch = false,
                        onAction = onPlaceAction,
                        onSearch = { onAction(TripWorkspaceAction.OpenSearch) },
                        showDialogs = false,
                    )
                    WorkspaceSection.ITINERARY -> WorkspaceItineraryContent(
                        days = state.days,
                        selected = state.itineraryScope,
                        wholeTripDays = state.wholeTripDays,
                        onSelect = { onAction(TripWorkspaceAction.SelectItineraryScope(it)) },
                        onAddDay = { onAction(TripWorkspaceAction.OpenOverlay(WorkspaceOverlay.AddTripDay)) },
                        modifier = Modifier.weight(1f),
                        dayContent = {
                            if (dayItineraryContent != null) dayItineraryContent() else DayItineraryContent(
                                state = itineraryState,
                                onAction = onItineraryAction,
                                showDialogs = false,
                            )
                        },
                    )
                }
            }
        },
        background = { sheetHeight ->
            Box(Modifier.fillMaxSize().testTag("workspace-map")) {
                if (mapState == WorkspaceMapState.Ready || mapState == WorkspaceMapState.Loading) mapContent()
                if (mapState != WorkspaceMapState.Ready) {
                    WorkspaceMapFallback(
                        mapState,
                        { onAction(TripWorkspaceAction.OpenPrivacySettings) },
                        onMapRetry,
                        Modifier.padding(bottom = sheetHeight),
                    )
                }
                WorkspaceTopBar(state.tripName, onAction)
                MapControls(
                    layer = state.mapLayer,
                    overlay = state.overlay,
                    onOpenLayerMenu = { onAction(TripWorkspaceAction.OpenOverlay(WorkspaceOverlay.LayerMenu)) },
                    onCloseOverlay = { onAction(TripWorkspaceAction.CloseOverlay) },
                    onSelectLayer = { onAction(TripWorkspaceAction.SelectMapLayer(it)) },
                    onLocate = { onAction(TripWorkspaceAction.Locate) },
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 62.dp, end = 20.dp),
                )
                MapLegend(Modifier.align(Alignment.BottomStart).padding(start = 20.dp, bottom = 82.dp))
                SearchSurface(
                    { onAction(TripWorkspaceAction.OpenSearch) },
                    Modifier.align(Alignment.BottomCenter).padding(horizontal = 20.dp, vertical = 20.dp),
                )
            }
        },
    )
}

@Composable
private fun WorkspaceTopBar(tripName: String, onAction: (TripWorkspaceAction) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 20.dp).testTag("workspace-top-bar"),
        shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 3.dp,
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CompactSecondaryButton({ onAction(TripWorkspaceAction.Back) }) { Text("返回") }
            Text(tripName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            CompactSecondaryButton({ onAction(TripWorkspaceAction.OpenSettings) }) { Text("设置") }
        }
    }
}

@Composable internal fun LayerIcon() {
    Canvas(Modifier.fillMaxSize().padding(12.dp)) {
        val stroke = 1.5.dp.toPx()
        fun layer(centerY: Float) {
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(size.width / 2f, centerY - size.height * .18f); lineTo(size.width, centerY); lineTo(size.width / 2f, centerY + size.height * .18f); lineTo(0f, centerY); close()
            }
            drawPath(path, Color(0xFF2D5E3A), style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
        }
        layer(size.height * .36f); layer(size.height * .62f)
    }
}

@Composable
internal fun WorkspaceSheetHandle(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(24.dp), contentAlignment = Alignment.Center) {
        Surface(Modifier.fillMaxWidth(.1f).height(4.dp), shape = RoundedCornerShape(2.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .4f)) {}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private fun WorkspaceSheetLevel.toSheetValue() = when (this) {
    WorkspaceSheetLevel.COLLAPSED -> SheetValue.Hidden
    WorkspaceSheetLevel.HALF -> SheetValue.PartiallyExpanded
    WorkspaceSheetLevel.EXPANDED -> SheetValue.Expanded
}

internal fun MapLayer.label() = when (this) {
    MapLayer.STANDARD -> "标准"
    MapLayer.SATELLITE_ROAD -> "卫星"
}
