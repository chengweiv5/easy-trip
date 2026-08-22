package com.yangchengwei.easytrip.workspace

import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yangchengwei.easytrip.amap.AmapConsentToken
import com.yangchengwei.easytrip.place.ui.PlaceSearchField

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
    placeContent: @Composable () -> Unit,
    itineraryContent: @Composable () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
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
            Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(state.tab == WorkspaceTab.PLACES, { viewModel.selectTab(WorkspaceTab.PLACES) }, { Text("地点池") })
                    FilterChip(state.tab == WorkspaceTab.ITINERARY, { viewModel.selectTab(WorkspaceTab.ITINERARY) }, { Text("每日行程") })
                    TextButton({ viewModel.setSheetLevel(WorkspaceSheetLevel.COLLAPSED) }) { Text("收起") }
                    TextButton({ viewModel.setSheetLevel(WorkspaceSheetLevel.HALF) }) { Text("半屏") }
                    TextButton({ viewModel.setSheetLevel(WorkspaceSheetLevel.EXPANDED) }) { Text("展开") }
                }
                if (state.tab == WorkspaceTab.PLACES) placeContent() else itineraryContent()
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onBack) { Text("返回") }
                Text(state.tripName, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                TextButton(onPrivacySettings) { Text("地图授权") }
                TextButton(onSettings) { Text("旅行设置") }
            }
            PlaceSearchField(searchQuery, onSearchQueryChange)
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MapScope.entries.forEach { scope ->
                    FilterChip(state.mapScope == scope, { viewModel.selectScope(scope) }, { Text(scope.label()) }, Modifier.testTag("scope-${scope.name}"))
                }
            }
            Box(Modifier.fillMaxSize().testTag("workspace-map")) {
                if (consent == null) {
                    Text("同意高德隐私政策后显示地图", Modifier.padding(16.dp))
                } else {
                    AmapComposeMap(state.map, viewModel::selectMarker, consent, Modifier.fillMaxSize())
                }
                if (state.sheetLevel == WorkspaceSheetLevel.COLLAPSED) {
                    Button(
                        { viewModel.setSheetLevel(WorkspaceSheetLevel.HALF) },
                        Modifier.align(androidx.compose.ui.Alignment.BottomCenter).testTag("expand-sheet"),
                    ) { Text("展开抽屉") }
                }
            }
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
