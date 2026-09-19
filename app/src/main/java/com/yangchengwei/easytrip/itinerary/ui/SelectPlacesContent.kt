package com.yangchengwei.easytrip.itinerary.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.ui.theme.EasyTripBackground
import com.yangchengwei.easytrip.core.ui.theme.EasyTripPlaceSurface
import com.yangchengwei.easytrip.core.ui.theme.EasyTripScheduled
import com.yangchengwei.easytrip.place.ui.SavedPlaceRowUi
import com.yangchengwei.easytrip.place.ui.PlaceCityFilterBar
import com.yangchengwei.easytrip.place.ui.PlaceCityGroupHeading
import com.yangchengwei.easytrip.place.ui.placeCityGroups
import com.yangchengwei.easytrip.place.ui.filterPlaceCityGroups
import androidx.compose.foundation.lazy.rememberLazyListState
import com.yangchengwei.easytrip.workspace.WorkspaceCloseIcon
import com.yangchengwei.easytrip.workspace.WorkspaceSearchIcon

@Composable
fun SelectPlacesContent(
    rows: List<SavedPlaceRowUi>,
    state: AddToItineraryUiState,
    onTogglePlace: (String) -> Unit,
    onContinue: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    targetDayLabel: String? = null,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedCityKey by rememberSaveable { mutableStateOf<String?>(null) }
    val cities = remember(rows) { placeCityGroups(rows) }
    val activeCity = selectedCityKey?.takeIf { key -> cities.any { it.key == key } }
    LaunchedEffect(activeCity) { selectedCityKey = activeCity }
    val visibleGroups = remember(rows, activeCity, query) { filterPlaceCityGroups(rows, activeCity, query) }
    val listState = rememberLazyListState()
    LaunchedEffect(activeCity, query) { listState.scrollToItem(0) }
    val selectionOrder = remember(state.selectedPlaceIds) {
        state.selectedPlaceIds.mapIndexed { index, id -> id to index + 1 }.toMap()
    }
    val busy = state.isSubmitting || state.isUndoing
    val focusManager = LocalFocusManager.current
    Column(
        modifier.fillMaxWidth().padding(top = 4.dp).testTag("select-places-content"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("从地点池添加", style = MaterialTheme.typography.titleLarge)
                Text(
                    if (targetDayLabel != null) "$targetDayLabel · 已安排地点可重复添加" else "按选择顺序加入 · 已安排地点可重复添加",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(
                Modifier.size(44.dp).testTag("select-places-close")
                    .semantics { contentDescription = "取消添加地点" }
                    .clickable(enabled = !busy, role = Role.Button, onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Surface(Modifier.size(32.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                    Box(contentAlignment = Alignment.Center) { WorkspaceCloseIcon(Modifier.size(16.dp)) }
                }
            }
        }
        if (rows.isNotEmpty()) PlaceCityFilterBar(cities, activeCity, { selectedCityKey = it }, Modifier.fillMaxWidth(), enabled = !busy)
        PlacePickerSearch(query, { query = it }, enabled = !busy, cityName = cities.firstOrNull { it.key == activeCity }?.name)
        if (visibleGroups.isEmpty()) {
            Column(
                Modifier.weight(1f).fillMaxWidth().testTag("select-places-empty"),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(if (rows.isEmpty()) "还没有收藏地点" else "没有找到匹配地点", style = MaterialTheme.typography.labelLarge)
                Text(
                    if (rows.isEmpty()) "先在地图中搜索并收藏喜欢的地点" else if (activeCity != null) "换个关键词，或切换到全部城市" else "换个名称、地址或城市试试",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("select-places-list"), state = listState, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                visibleGroups.forEach { group ->
                    if (activeCity == null && cities.size > 1) item(key = "city-heading-${group.key}") { PlaceCityGroupHeading(group) }
                    items(group.rows, key = { it.id }) { row ->
                        SelectablePlaceRow(row, selectionOrder[row.id], !busy) { onTogglePlace(row.id) }
                    }
                }
            }
        }
        state.errorMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
        PlacePickerFooter(
            count = state.selectedPlaceIds.size,
            label = when {
                state.isSubmitting -> "正在加入…"
                state.isUndoing -> "处理中…"
                state.editingTarget is AddToItineraryEditingTarget.ForDay -> targetDayLabel?.let { "加入$it" } ?: "加入当天行程"
                else -> "下一步"
            },
            enabled = state.canContinue && !busy && !state.hasStaleFixedDay,
            onContinue = { focusManager.clearFocus(); onContinue() },
        )
    }
}

@Composable
private fun PlacePickerSearch(query: String, onQueryChange: (String) -> Unit, enabled: Boolean, cityName: String?) {
    val focusManager = LocalFocusManager.current
    Surface(shape = RoundedCornerShape(8.dp), color = EasyTripBackground) {
        Row(Modifier.fillMaxWidth().heightIn(min = 36.dp).padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WorkspaceSearchIcon(Modifier.size(16.dp))
            BasicTextField(
                value = query, onValueChange = onQueryChange, enabled = enabled, singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier.weight(1f).testTag("select-places-search").semantics { contentDescription = "搜索已收藏的地点" },
                decorationBox = { input ->
                    Box {
                        if (query.isEmpty()) Text(cityName?.let { "搜索${it}收藏的地点" } ?: "搜索已收藏的地点", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        input()
                    }
                },
            )
            if (query.isNotEmpty()) Box(
                Modifier.size(24.dp).testTag("select-places-clear-search").semantics { contentDescription = "清空搜索" }
                    .clickable(enabled = enabled, role = Role.Button) { onQueryChange("") },
                contentAlignment = Alignment.Center,
            ) { WorkspaceCloseIcon(Modifier.size(14.dp)) }
        }
    }
}

@Composable
private fun SelectablePlaceRow(row: SavedPlaceRowUi, selection: Int?, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("select-place-${row.id}")
            .semantics { selected = selection != null; stateDescription = selection?.let { "第 $it 个选择" } ?: "未选择" }
            .clickable(enabled = enabled, role = Role.Checkbox, onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (selection != null) EasyTripPlaceSurface else MaterialTheme.colorScheme.surface,
        border = if (selection != null) BorderStroke(1.dp, Color(0xFFAFBEA8)) else null,
    ) {
        Row(Modifier.heightIn(min = 58.dp).padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(
                modifier = Modifier.defaultMinSize(minWidth = 22.dp, minHeight = 22.dp),
                shape = CircleShape,
                color = if (selection != null) MaterialTheme.colorScheme.primary else Color.Transparent,
                border = if (selection == null) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (selection != null) Text(selection.toString(), Modifier.padding(horizontal = 5.dp, vertical = 2.dp).testTag("select-place-order-${row.id}"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(row.name, style = MaterialTheme.typography.labelMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (row.address.isNotBlank()) Text(row.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (row.scheduled && LocalDensity.current.fontScale > 1.3f) Text("已排 ${row.itineraryOccurrenceCount} 次 · 可重复添加", style = MaterialTheme.typography.bodySmall, color = EasyTripScheduled)
            }
            if (row.scheduled && LocalDensity.current.fontScale <= 1.3f) Surface(
                modifier = Modifier.semantics { contentDescription = "已安排 ${row.itineraryOccurrenceCount} 次，可重复添加" },
                shape = RoundedCornerShape(4.dp), color = Color(0xFFF6EDE2),
            ) {
                Text("已排 ${row.itineraryOccurrenceCount} 次", Modifier.padding(horizontal = 6.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall, color = EasyTripScheduled)
            }
        }
    }
}

@Composable
private fun PlacePickerFooter(count: Int, label: String, enabled: Boolean, onContinue: () -> Unit) {
    val stacked = LocalDensity.current.fontScale > 1.3f
    val countText: @Composable () -> Unit = {
        Text(if (count == 0) "请选择地点" else "已选 $count 个地点", style = MaterialTheme.typography.labelMedium, color = if (count == 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary)
    }
    val button: @Composable (Modifier) -> Unit = { modifier ->
        Surface(
            modifier.heightIn(min = 44.dp).testTag("select-places-continue").clickable(enabled = enabled, role = Role.Button, onClick = onContinue),
            shape = RoundedCornerShape(10.dp), color = if (enabled) MaterialTheme.colorScheme.primary else Color(0xFFE2E6DF),
        ) {
            Box(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
                Text(label, style = MaterialTheme.typography.labelLarge, color = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        if (stacked) Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { countText(); button(Modifier.fillMaxWidth()) }
        else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) { countText(); button(Modifier.weight(1f)) }
    }
}
