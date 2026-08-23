package com.yangchengwei.easytrip.workspace

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.BottomSheetScaffold
import com.yangchengwei.easytrip.core.ui.component.CompactPrimaryButton as Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import com.yangchengwei.easytrip.core.ui.component.CompactSecondaryButton as TextButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yangchengwei.easytrip.amap.AmapConsentToken
import com.yangchengwei.easytrip.core.ui.component.SelectablePill
import com.yangchengwei.easytrip.itinerary.ui.WorkspaceItineraryContent
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

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
    onTogglePoiCollection: (com.yangchengwei.easytrip.place.amap.PlaceCandidate) -> Unit = {},
    mapHostFactory: (android.content.Context) -> AmapMapHost = { RealAmapMapHost.create(it) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var mapError by remember { mutableStateOf<String?>(null) }
    var layerMenuExpanded by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = when (state.sheetLevel) {
                WorkspaceSheetLevel.COLLAPSED -> SheetValue.Hidden
                WorkspaceSheetLevel.HALF -> SheetValue.PartiallyExpanded
                WorkspaceSheetLevel.EXPANDED -> SheetValue.Expanded
            },
            skipHiddenState = false,
        ),
    )
    LaunchedEffect(state.sheetLevel) {
        when (state.sheetLevel) {
            WorkspaceSheetLevel.COLLAPSED -> scaffoldState.bottomSheetState.hide()
            WorkspaceSheetLevel.HALF -> scaffoldState.bottomSheetState.partialExpand()
            WorkspaceSheetLevel.EXPANDED -> scaffoldState.bottomSheetState.expand()
        }
    }
    LaunchedEffect(scaffoldState.bottomSheetState) {
        snapshotFlow {
            scaffoldState.bottomSheetState.currentValue to scaffoldState.bottomSheetState.targetValue
        }.distinctUntilChanged().collect { (current, target) ->
            settledWorkspaceSheetLevel(current, target, state.sheetLevel)?.let(viewModel::setSheetLevel)
        }
    }
    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = if (state.sheetLevel == WorkspaceSheetLevel.COLLAPSED) 0.dp else 220.dp,
        sheetDragHandle = null,
        sheetContent = {
            Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp).testTag("workspace-sheet")) {
                if (state.sheetLevel != WorkspaceSheetLevel.COLLAPSED) {
                    WorkspaceSheetHandle(Modifier.testTag("workspace-sheet-handle"))
                }
                when (state.section) {
                    WorkspaceSection.PLACE_POOL -> placeContent()
                    WorkspaceSection.ITINERARY -> WorkspaceItineraryContent(
                        days = state.days,
                        selected = state.itineraryScope,
                        wholeTripDays = state.wholeTripDays,
                        onSelect = viewModel::selectItineraryScope,
                        dayContent = dayItineraryContent,
                    )
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Box(Modifier.weight(1f).fillMaxWidth().testTag("workspace-map")) {
                if (consent == null) {
                    Text("同意高德隐私政策后显示地图", Modifier.padding(16.dp))
                } else {
                    AmapComposeMap(
                        state.map,
                        viewModel::selectMarker,
                        consent,
                        onMapPoiClick = viewModel::selectMapPoi,
                        layer = state.mapLayer,
                        modifier = Modifier.fillMaxSize(),
                        hostFactory = mapHostFactory,
                        onLayerError = { _, retainedLayer ->
                            viewModel.selectMapLayer(retainedLayer)
                            mapError = "地图图层切换失败，已保留当前图层"
                        }
                    )
                }
                Surface(
                    modifier = Modifier
                        .align(androidx.compose.ui.Alignment.TopCenter)
                        .padding(top = 5.dp, start = 10.dp, end = 10.dp)
                        .fillMaxWidth()
                        .height(40.dp)
                        .testTag("workspace-top-bar"),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    shadowElevation = 3.dp,
                ) {
                    Row(
                        Modifier.fillMaxSize().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    ) {
                        TextButton(onBack) { Text("返回") }
                        Text(state.tripName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        TextButton(onPrivacySettings) { Text("地图授权") }
                        TextButton(onSettings) { Text("设置") }
                    }
                }
                Box(Modifier.align(androidx.compose.ui.Alignment.TopEnd).padding(top = 55.dp, end = 10.dp)) {
                    Surface(
                        modifier = Modifier
                            .size(24.dp)
                            .testTag("layer-menu")
                            .semantics { contentDescription = "地图图层" }
                            .clickable { layerMenuExpanded = true },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        shadowElevation = 2.dp,
                    ) {
                        val iconColor = MaterialTheme.colorScheme.primary
                        Canvas(Modifier.fillMaxSize().padding(4.dp)) {
                            val stroke = 1.5.dp.toPx()
                            val w = size.width
                            val h = size.height
                            fun layer(centerY: Float) {
                                val path = androidx.compose.ui.graphics.Path().apply {
                                    moveTo(w / 2f, centerY - h * 0.18f)
                                    lineTo(w, centerY)
                                    lineTo(w / 2f, centerY + h * 0.18f)
                                    lineTo(0f, centerY)
                                    close()
                                }
                                drawPath(path, iconColor, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
                            }
                            layer(h * 0.36f)
                            layer(h * 0.62f)
                        }
                    }
                    DropdownMenu(expanded = layerMenuExpanded, onDismissRequest = { layerMenuExpanded = false }) {
                        MapLayer.entries.forEach { layer ->
                            DropdownMenuItem(
                                text = { Text(if (state.mapLayer == layer) "✓ ${layer.label()}" else layer.label()) },
                                onClick = {
                                    viewModel.selectMapLayer(layer)
                                    layerMenuExpanded = false
                                },
                                modifier = Modifier.testTag("layer-${layer.name}"),
                            )
                        }
                    }
                }
                Box(
                    Modifier
                        .align(androidx.compose.ui.Alignment.BottomCenter)
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(start = 10.dp, end = 10.dp, bottom = 5.dp),
                    contentAlignment = androidx.compose.ui.Alignment.BottomEnd,
                ) {
                    WorkspaceSearchLauncher(
                        onClick = onOpenSearch,
                        modifier = Modifier.fillMaxWidth(0.2f),
                    )
                }
            }
            mapError?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 8.dp)) }
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .testTag("section-controls")
                    .selectableGroup(),
            ) {
                SelectablePill(
                    state.section == WorkspaceSection.PLACE_POOL,
                    { viewModel.selectSection(WorkspaceSection.PLACE_POOL) },
                    { Text("地点池") },
                    Modifier.weight(1f).testTag("section-${WorkspaceSection.PLACE_POOL.name}"),
                    role = Role.Tab,
                )
                SelectablePill(
                    state.section == WorkspaceSection.ITINERARY,
                    { viewModel.selectSection(WorkspaceSection.ITINERARY) },
                    { Text("行程") },
                    Modifier.weight(1f).testTag("section-${WorkspaceSection.ITINERARY.name}"),
                    role = Role.Tab,
                )
            }
            if (state.sheetLevel == WorkspaceSheetLevel.COLLAPSED) {
                Surface(
                    Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 4.dp,
                ) {
                    WorkspaceSheetHandle(
                        Modifier
                            .testTag("workspace-sheet-handle")
                            .pointerInput(scaffoldState.bottomSheetState) {
                                var dragDistance = 0f
                                detectVerticalDragGestures(
                                    onDragStart = { dragDistance = 0f },
                                    onVerticalDrag = { _, amount -> dragDistance += amount },
                                    onDragEnd = {
                                        if (dragDistance < -24f) coroutineScope.launch {
                                            viewModel.setSheetLevel(WorkspaceSheetLevel.HALF)
                                            scaffoldState.bottomSheetState.partialExpand()
                                        }
                                    },
                                )
                            },
                    )
                }
            } else {
                Spacer(Modifier.height(10.dp))
            }
        }
    }
    state.selectedMapPoi?.let { poi ->
        AlertDialog(
            onDismissRequest = viewModel::dismissPlaceCard,
            title = { Text(poi.name) },
            text = {
                Column {
                    Text(poi.address.ifBlank { "地址暂不可用" })
                    collectionError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                val poiId = poi.poiId
                Button(
                    onClick = {
                        if (poiId != null) {
                            onTogglePoiCollection(
                                com.yangchengwei.easytrip.place.amap.PlaceCandidate(
                                    poiId = poiId,
                                    name = poi.name,
                                    address = poi.address,
                                    point = poi.point,
                                    cityCode = null,
                                ),
                            )
                        }
                    },
                    enabled = poiId != null && poiId !in collectionBusyPoiIds,
                    modifier = Modifier.testTag("place-card-collection"),
                ) { Text(if (poiId == null) "无法收藏" else if (isPoiSaved) "取消收藏" else "收藏") }
            },
            dismissButton = { TextButton(viewModel::dismissPlaceCard) { Text("关闭") } },
        )
    }
    state.selectedMarker?.let { marker ->
        val markerPoi = state.selectedMarkerPoi
        AlertDialog(
            onDismissRequest = viewModel::dismissMarker,
            title = { Text(marker.label) },
            text = {
                Column {
                    if (marker.occurrences.isEmpty()) Text("收藏地点")
                    marker.occurrences.forEach { Text("${it.dayLabel} · 第 ${it.order} 项 · ${it.placeName}") }
                    collectionError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                if (markerPoi != null) {
                    Button(
                        onClick = {
                            onTogglePoiCollection(
                                com.yangchengwei.easytrip.place.amap.PlaceCandidate(
                                    markerPoi.poiId!!,
                                    markerPoi.name,
                                    markerPoi.address,
                                    markerPoi.point,
                                    null,
                                ),
                            )
                        },
                        enabled = markerPoi.poiId !in collectionBusyPoiIds,
                        modifier = Modifier.testTag("place-card-collection"),
                    ) { Text("取消收藏") }
                } else {
                    Button(viewModel::dismissMarker) { Text("关闭") }
                }
            },
            dismissButton = markerPoi?.let { { TextButton(viewModel::dismissMarker) { Text("关闭") } } },
        )
    }
}

@Composable
fun WorkspaceSearchLauncher(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(48.dp)
            .testTag("workspace-search-launcher")
            .semantics { contentDescription = "搜索地点" }
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        Surface(
            Modifier.fillMaxWidth().height(40.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            color = Color.White,
            shadowElevation = 2.dp,
        ) {
            val iconColor = MaterialTheme.colorScheme.primary
            Canvas(
                Modifier
                    .fillMaxSize()
                    .padding(10.dp),
            ) {
                val stroke = 2.dp.toPx()
                val radius = size.minDimension * 0.28f
                val center = androidx.compose.ui.geometry.Offset(size.width * 0.43f, size.height * 0.43f)
                drawCircle(iconColor, radius, center, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
                val diagonal = radius * 0.7f
                drawLine(
                    iconColor,
                    center + androidx.compose.ui.geometry.Offset(diagonal, diagonal),
                    center + androidx.compose.ui.geometry.Offset(radius * 1.55f, radius * 1.55f),
                    strokeWidth = stroke,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
internal fun settledWorkspaceSheetLevel(
    current: SheetValue,
    target: SheetValue,
    requested: WorkspaceSheetLevel,
): WorkspaceSheetLevel? {
    if (current != target) return null
    if (requested == WorkspaceSheetLevel.COLLAPSED && current == SheetValue.PartiallyExpanded) return null
    return when (current) {
        SheetValue.Hidden -> WorkspaceSheetLevel.COLLAPSED
        SheetValue.PartiallyExpanded -> WorkspaceSheetLevel.HALF
        SheetValue.Expanded -> WorkspaceSheetLevel.EXPANDED
    }
}

@Composable
private fun WorkspaceSheetHandle(modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxWidth().height(24.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        Surface(
            Modifier.width(32.dp).height(4.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(2.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        ) {}
    }
}

private fun MapScope.label() = when (this) {
    MapScope.PLACE_POOL -> "地点池"
    MapScope.SINGLE_DAY -> "单日"
    MapScope.WHOLE_TRIP -> "全程"
}

private fun MapLayer.label() = when (this) {
    MapLayer.STANDARD -> "标准"
    MapLayer.SATELLITE_ROAD -> "卫星"
}
