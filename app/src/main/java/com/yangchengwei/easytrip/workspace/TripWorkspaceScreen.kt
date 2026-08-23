package com.yangchengwei.easytrip.workspace

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yangchengwei.easytrip.amap.AmapConsentToken
import com.yangchengwei.easytrip.core.ui.component.CompactPrimaryButton
import com.yangchengwei.easytrip.core.ui.component.CompactSecondaryButton
import com.yangchengwei.easytrip.place.amap.PlaceCandidate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripWorkspaceScreen(
    viewModel: TripWorkspaceViewModel,
    consent: AmapConsentToken?,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    onPrivacySettings: () -> Unit = {},
    onOpenSearch: () -> Unit = {},
    placeContent: @Composable () -> Unit,
    dayItineraryContent: @Composable () -> Unit,
    isPoiSaved: Boolean = false,
    collectionBusyPoiIds: Set<String> = emptySet(),
    collectionError: String? = null,
    onTogglePoiCollection: (PlaceCandidate) -> Unit = {},
    mapHostFactory: (android.content.Context) -> AmapMapHost = { RealAmapMapHost.create(it) },
) {
    val pageState by viewModel.pageState.collectAsStateWithLifecycle()
    var mapAttempt by remember { mutableIntStateOf(0) }
    var mapState by remember(consent) { mutableStateOf(if (consent == null) WorkspaceMapState.ConsentRequired else WorkspaceMapState.Loading) }
    var failedAttempt by remember(consent) { mutableStateOf<Int?>(null) }
    LaunchedEffect(consent) {
        mapAttempt++
        failedAttempt = null
        mapState = if (consent == null) WorkspaceMapState.ConsentRequired else WorkspaceMapState.Loading
    }
    val ready = (pageState as? TripWorkspacePageState.Ready)?.content

    TripWorkspaceContent(
        pageState = pageState,
        mapState = mapState,
        onPageRetry = viewModel::retry,
        onMapRetry = {
            mapAttempt++
            failedAttempt = null
            mapState = if (consent == null) WorkspaceMapState.ConsentRequired else WorkspaceMapState.Loading
        },
        onAction = { action ->
            when (action) {
                TripWorkspaceAction.Back -> onBack()
                TripWorkspaceAction.OpenSettings -> onSettings()
                TripWorkspaceAction.OpenPrivacySettings -> onPrivacySettings()
                TripWorkspaceAction.OpenSearch -> onOpenSearch()
                TripWorkspaceAction.Retry -> if (mapState is WorkspaceMapState.Failed) {
                    mapAttempt++
                    failedAttempt = null
                    mapState = if (consent == null) WorkspaceMapState.ConsentRequired else WorkspaceMapState.Loading
                } else viewModel.retry()
                is TripWorkspaceAction.SelectSection -> viewModel.selectSection(action.section)
                is TripWorkspaceAction.SelectItineraryScope -> viewModel.selectItineraryScope(action.scope)
                is TripWorkspaceAction.SelectMapLayer -> viewModel.selectMapLayer(action.layer)
                is TripWorkspaceAction.SetSheetLevel -> viewModel.setSheetLevel(action.level)
            }
        },
        placeContent = placeContent,
        dayItineraryContent = dayItineraryContent,
        mapContent = {
            val token = consent
            if (token != null && ready != null) key(mapAttempt) {
                val attemptId = mapAttempt
                AmapComposeMap(
                    model = ready.map,
                    onMarkerClick = viewModel::selectMarker,
                    consent = token,
                    onMapPoiClick = viewModel::selectMapPoi,
                    layer = ready.mapLayer,
                    modifier = Modifier.fillMaxSize(),
                    hostFactory = mapHostFactory,
                    onLayerError = { _, retainedLayer ->
                        if (attemptId == mapAttempt) {
                            failedAttempt = attemptId
                            viewModel.selectMapLayer(retainedLayer)
                            mapState = WorkspaceMapState.Failed("地图图层切换失败，已保留当前图层")
                        }
                    },
                    onMapError = {
                        if (attemptId == mapAttempt) {
                            failedAttempt = attemptId
                            mapState = WorkspaceMapState.Failed("地图加载失败")
                        }
                    },
                    onMapReady = {
                        if (attemptId == mapAttempt && failedAttempt != attemptId) mapState = WorkspaceMapState.Ready
                    },
                )
            }
        },
    )

    ready?.selectedMapPoi?.let { poi ->
        AlertDialog(
            onDismissRequest = viewModel::dismissPlaceCard,
            title = { Text(poi.name) },
            text = { Column { Text(poi.address.ifBlank { "地址暂不可用" }); collectionError?.let { Text(it, color = MaterialTheme.colorScheme.error) } } },
            confirmButton = {
                val poiId = poi.poiId
                CompactPrimaryButton(
                    onClick = {
                        if (poiId != null) onTogglePoiCollection(PlaceCandidate(poiId, poi.name, poi.address, poi.point, null))
                    },
                    enabled = poiId != null && poiId !in collectionBusyPoiIds,
                    modifier = Modifier.testTag("place-card-collection"),
                ) { Text(if (poiId == null) "无法收藏" else if (isPoiSaved) "取消收藏" else "收藏") }
            },
            dismissButton = { CompactSecondaryButton(viewModel::dismissPlaceCard) { Text("关闭") } },
        )
    }
    ready?.selectedMarker?.let { marker ->
        val markerPoi = ready.selectedMarkerPoi
        AlertDialog(
            onDismissRequest = viewModel::dismissMarker,
            title = { Text(marker.label) },
            text = { Column { if (marker.occurrences.isEmpty()) Text("收藏地点"); marker.occurrences.forEach { Text("${it.dayLabel} · 第 ${it.order} 项 · ${it.placeName}") }; collectionError?.let { Text(it, color = MaterialTheme.colorScheme.error) } } },
            confirmButton = {
                if (markerPoi != null) {
                    CompactPrimaryButton(
                        onClick = { onTogglePoiCollection(PlaceCandidate(requireNotNull(markerPoi.poiId), markerPoi.name, markerPoi.address, markerPoi.point, null)) },
                        enabled = markerPoi.poiId !in collectionBusyPoiIds,
                        modifier = Modifier.testTag("place-card-collection"),
                    ) { Text("取消收藏") }
                } else CompactPrimaryButton(viewModel::dismissMarker) { Text("关闭") }
            },
            dismissButton = markerPoi?.let { { CompactSecondaryButton(viewModel::dismissMarker) { Text("关闭") } } },
        )
    }
}

@Composable
fun WorkspaceSearchLauncher(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.height(48.dp).testTag("workspace-search-launcher").semantics { contentDescription = "搜索地点" }.clickable(role = Role.Button, onClick = onClick),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        Surface(Modifier.fillMaxWidth().height(46.dp), shape = androidx.compose.foundation.shape.RoundedCornerShape(23.dp), color = Color.White, shadowElevation = 2.dp) {
            Canvas(Modifier.fillMaxSize().padding(13.dp)) {
                val color = Color(0xFF2D5E3A)
                val stroke = 2.dp.toPx()
                val radius = size.minDimension * .28f
                val center = androidx.compose.ui.geometry.Offset(size.width * .43f, size.height * .43f)
                drawCircle(color, radius, center, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
                val diagonal = radius * .7f
                drawLine(color, center + androidx.compose.ui.geometry.Offset(diagonal, diagonal), center + androidx.compose.ui.geometry.Offset(radius * 1.55f, radius * 1.55f), strokeWidth = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
internal fun settledWorkspaceSheetLevel(current: SheetValue, target: SheetValue, requested: WorkspaceSheetLevel): WorkspaceSheetLevel? {
    if (current != target) return null
    if (requested == WorkspaceSheetLevel.COLLAPSED && current == SheetValue.PartiallyExpanded) return null
    return when (current) {
        SheetValue.Hidden -> WorkspaceSheetLevel.COLLAPSED
        SheetValue.PartiallyExpanded -> WorkspaceSheetLevel.HALF
        SheetValue.Expanded -> WorkspaceSheetLevel.EXPANDED
    }
}
