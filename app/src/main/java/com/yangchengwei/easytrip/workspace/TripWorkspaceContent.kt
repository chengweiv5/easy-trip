package com.yangchengwei.easytrip.workspace

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
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
    WorkspaceScaffold(
        sheetLevel = state.sheetLevel,
        searchReturn = searchReturn != null,
        onSheetLevelChange = { onAction(TripWorkspaceAction.SetSheetLevel(it)) },
        modifier = modifier,
        sheetHeader = {
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
        sheetContent = {
            Column(Modifier.fillMaxSize()) {
                when (state.section) {
                    WorkspaceSection.PLACE_POOL -> if (placeContent != null) {
                        Box(Modifier.weight(1f).padding(horizontal = 20.dp)) { placeContent() }
                    } else PlacePoolContent(
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
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                    )
                    WorkspaceSection.ITINERARY -> WorkspaceItineraryContent(
                        days = state.days,
                        selected = state.itineraryScope,
                        wholeTripDays = state.wholeTripDays,
                        onSelect = { onAction(TripWorkspaceAction.SelectItineraryScope(it)) },
                        onAddDay = { onAction(TripWorkspaceAction.OpenOverlay(WorkspaceOverlay.AddTripDay)) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 20.dp),
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
        map = { metrics ->
            Box(Modifier.fillMaxSize().testTag("workspace-map")) {
                if (mapState == WorkspaceMapState.Ready || mapState == WorkspaceMapState.Loading) mapContent()
                if (mapState != WorkspaceMapState.Ready) {
                    WorkspaceMapFallback(
                        mapState,
                        { onAction(TripWorkspaceAction.OpenPrivacySettings) },
                        onMapRetry,
                        Modifier.padding(bottom = metrics.sheetHeight),
                    )
                }
            }
        },
        topOverlay = { metrics ->
            WorkspaceTopBar(
                title = state.tripName,
                dateLabel = state.dateLabel,
                onBack = { onAction(TripWorkspaceAction.Back) },
                onMore = { onAction(TripWorkspaceAction.OpenSettings) },
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            MapControls(
                layer = state.mapLayer,
                overlay = state.overlay,
                onOpenLayerMenu = { onAction(TripWorkspaceAction.OpenOverlay(WorkspaceOverlay.LayerMenu)) },
                onCloseOverlay = { onAction(TripWorkspaceAction.CloseOverlay) },
                onSelectLayer = { onAction(TripWorkspaceAction.SelectMapLayer(it)) },
                onLocate = { onAction(TripWorkspaceAction.Locate) },
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = metrics.overlayBottomInset + 58.dp),
            )
            MapLegend(
                Modifier.align(Alignment.BottomStart).padding(start = 20.dp, bottom = metrics.overlayBottomInset + 58.dp),
            )
            WorkspaceSearchBar(
                { onAction(TripWorkspaceAction.OpenSearch) },
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 20.dp, end = 20.dp, bottom = metrics.overlayBottomInset),
            )
        },
    )
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

internal fun MapLayer.label() = when (this) {
    MapLayer.STANDARD -> "标准"
    MapLayer.SATELLITE_ROAD -> "卫星"
}
