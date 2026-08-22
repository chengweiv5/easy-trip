package com.yangchengwei.easytrip.workspace

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yangchengwei.easytrip.amap.AmapConsentToken
import com.yangchengwei.easytrip.core.ui.component.SelectablePill
import com.yangchengwei.easytrip.place.ui.PlaceSearchField
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripWorkspaceScreen(
    viewModel: TripWorkspaceViewModel,
    consent: AmapConsentToken?,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    onPrivacySettings: () -> Unit = {},
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    searchContent: @Composable () -> Unit = {},
    placeContent: @Composable () -> Unit,
    itineraryContent: @Composable () -> Unit,
    mapHostFactory: (android.content.Context) -> AmapMapHost = { RealAmapMapHost.create(it) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var mapError by remember { mutableStateOf<String?>(null) }
    var layerMenuExpanded by remember { mutableStateOf(false) }
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
        snapshotFlow { scaffoldState.bottomSheetState.currentValue }.distinctUntilChanged().collect { value ->
            when (value) {
                SheetValue.Expanded -> viewModel.setSheetLevel(WorkspaceSheetLevel.EXPANDED)
                SheetValue.PartiallyExpanded -> viewModel.setSheetLevel(WorkspaceSheetLevel.HALF)
                SheetValue.Hidden -> viewModel.setSheetLevel(WorkspaceSheetLevel.COLLAPSED)
            }
        }
    }
    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = if (state.sheetLevel == WorkspaceSheetLevel.COLLAPSED) 56.dp else 220.dp,
        sheetContent = {
            Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp).testTag("workspace-sheet")) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SelectablePill(state.tab == WorkspaceTab.SEARCH, { viewModel.selectTab(WorkspaceTab.SEARCH) }, { Text("搜索") }, modifier = Modifier.testTag("tab-SEARCH"), role = Role.Tab)
                    SelectablePill(state.tab == WorkspaceTab.PLACES, { viewModel.selectTab(WorkspaceTab.PLACES) }, { Text("地点池") }, modifier = Modifier.testTag("tab-PLACES"), role = Role.Tab)
                    SelectablePill(state.tab == WorkspaceTab.ITINERARY, { viewModel.selectTab(WorkspaceTab.ITINERARY) }, { Text("每日行程") }, modifier = Modifier.testTag("tab-ITINERARY"), role = Role.Tab)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton({ viewModel.setSheetLevel(WorkspaceSheetLevel.COLLAPSED) }) { Text("收起") }
                    TextButton({ viewModel.setSheetLevel(WorkspaceSheetLevel.HALF) }) { Text("半屏") }
                    TextButton({ viewModel.setSheetLevel(WorkspaceSheetLevel.EXPANDED) }) { Text("展开") }
                }
                when (state.tab) {
                    WorkspaceTab.SEARCH -> searchContent()
                    WorkspaceTab.PLACES -> placeContent()
                    WorkspaceTab.ITINERARY -> itineraryContent()
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 8.dp).testTag("workspace-top-bar"),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                TextButton(onBack) { Text("返回") }
                Text(state.tripName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onPrivacySettings) { Text("地图授权") }
                TextButton(onSettings) { Text("设置") }
            }
            Box(Modifier.weight(1f).fillMaxWidth().testTag("workspace-map")) {
                if (consent == null) {
                    Text("同意高德隐私政策后显示地图", Modifier.padding(16.dp))
                } else {
                    AmapComposeMap(
                        state.map,
                        viewModel::selectMarker,
                        consent,
                        layer = state.mapLayer,
                        modifier = Modifier.fillMaxSize(),
                        hostFactory = mapHostFactory,
                        onLayerError = { _, retainedLayer ->
                            viewModel.selectMapLayer(retainedLayer)
                            mapError = "地图图层切换失败，已保留当前图层"
                        }
                    )
                }
                Box(Modifier.align(androidx.compose.ui.Alignment.TopEnd).padding(8.dp)) {
                    TextButton(
                        onClick = { layerMenuExpanded = true },
                        modifier = Modifier.testTag("layer-menu").semantics { contentDescription = "地图图层" },
                    ) {
                        val iconColor = MaterialTheme.colorScheme.primary
                        Canvas(Modifier.size(20.dp)) {
                            val stroke = size.minDimension / 10f
                            drawRect(iconColor, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
                            drawLine(iconColor, start = androidx.compose.ui.geometry.Offset(0f, size.height / 3f), end = androidx.compose.ui.geometry.Offset(size.width, size.height / 3f), strokeWidth = stroke)
                            drawLine(iconColor, start = androidx.compose.ui.geometry.Offset(0f, size.height * 2f / 3f), end = androidx.compose.ui.geometry.Offset(size.width, size.height * 2f / 3f), strokeWidth = stroke)
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
                if (state.sheetLevel == WorkspaceSheetLevel.COLLAPSED) {
                    Button(
                        { viewModel.setSheetLevel(WorkspaceSheetLevel.HALF) },
                        Modifier.align(androidx.compose.ui.Alignment.BottomCenter).testTag("expand-sheet"),
                    ) { Text("展开抽屉") }
                }
            }
            mapError?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 8.dp)) }
            PlaceSearchField(searchQuery, onSearchQueryChange)
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp).testTag("scope-controls"),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MapScope.entries.forEach { scope ->
                    SelectablePill(state.mapScope == scope, { viewModel.selectScope(scope) }, { Text(scope.label()) }, Modifier.testTag("scope-${scope.name}"))
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
    state.selectedMarker?.let { marker ->
        AlertDialog(
            onDismissRequest = viewModel::dismissMarker,
            title = { Text(marker.label) },
            text = {
                Column {
                    if (marker.occurrences.isEmpty()) Text("收藏地点")
                    marker.occurrences.forEach { Text("${it.dayLabel} · 第 ${it.order} 项 · ${it.placeName}") }
                }
            },
            confirmButton = { Button(viewModel::dismissMarker) { Text("关闭") } },
        )
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
