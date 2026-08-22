package com.yangchengwei.easytrip.trip.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.core.ui.component.CompactPrimaryButton as Button
import com.yangchengwei.easytrip.core.ui.component.CompactSecondaryButton as TextButton
import com.yangchengwei.easytrip.core.ui.component.SelectablePill
import java.time.Instant
import java.time.ZoneOffset
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripSettingsScreen(viewModel: TripSettingsViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showPicker by remember { mutableStateOf(false) }
    var editingName by remember { mutableStateOf(false) }
    var name by remember(state.name) { mutableStateOf(state.name) }
    Scaffold(topBar = { TopAppBar(title = { Text(state.name.ifEmpty { "旅行设置" }) }, navigationIcon = { IconButton(onClick = onBack) { Text("返回") } }) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(onClick = { editingName = true }) { Text("重命名旅行") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { showPicker = true }) { Text(state.startDate?.toString() ?: "选择起始日期") }
                Button(onClick = { viewModel.setStartDate(null) }) { Text("无日期") }
            }
            Row(
                Modifier.selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SelectablePill(state.travelMode == TravelMode.FLEXIBLE, { viewModel.setTravelMode(TravelMode.FLEXIBLE) }, { Text("灵活") }, Modifier.testTag("settings-mode-FLEXIBLE"), role = Role.RadioButton)
                SelectablePill(state.travelMode == TravelMode.SELF_DRIVE, { viewModel.setTravelMode(TravelMode.SELF_DRIVE) }, { Text("自驾") }, Modifier.testTag("settings-mode-SELF_DRIVE"), role = Role.RadioButton)
            }
            Button(onClick = viewModel::appendDay) { Text("末尾追加旅行日") }
            LazyColumn {
                itemsIndexed(state.days, key = { _, day -> day.id }) { index, day ->
                    DayRow(day, index, state.days.lastIndex, viewModel)
                }
            }
        }
    }
    if (editingName) {
        AlertDialog(
            onDismissRequest = { editingName = false },
            title = { Text("重命名旅行") },
            text = { OutlinedTextField(name, { name = it }, label = { Text("旅行名称") }, modifier = Modifier.testTag("rename-input")) },
            confirmButton = { TextButton({ viewModel.rename(name); editingName = false }, enabled = name.isNotBlank()) { Text("保存名称") } },
            dismissButton = { TextButton({ editingName = false }) { Text("取消") } },
        )
    }
    if (showPicker) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = state.startDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli())
        DatePickerDialog(onDismissRequest = { showPicker = false }, confirmButton = {
            TextButton(onClick = {
                picker.selectedDateMillis?.let { viewModel.setStartDate(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                showPicker = false
            }) { Text("确定日期") }
        }) { DatePicker(picker) }
    }
    state.pendingDayDeletion?.let { pending ->
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = { Text("删除旅行日？") },
            text = { Text("行程项 ${pending.impact.itineraryItems}，路线段 ${pending.impact.routeLegs}") },
            confirmButton = { Button(onClick = viewModel::confirmDelete) { Text("确认删除旅行日") } },
            dismissButton = { Button(onClick = viewModel::cancelDelete) { Text("取消删除旅行日") } },
        )
    }
}

@Composable
private fun DayRow(day: DayUi, index: Int, lastIndex: Int, viewModel: TripSettingsViewModel) {
    var expanded by remember { mutableStateOf(false) }
    var dragY by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    val rowHeight = with(LocalDensity.current) { 64.dp.toPx() }
    Row(
        Modifier
            .testTag("day-row-${day.id}")
            .fillMaxWidth()
            .background(if (dragging) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction("上移") { if (index > 0) viewModel.move(day.id, index - 1); index > 0 },
                    CustomAccessibilityAction("下移") { if (index < lastIndex) viewModel.move(day.id, index + 1); index < lastIndex },
                )
            }
            .pointerInput(day.id, index, lastIndex) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { dragging = true; dragY = 0f },
                    onDragCancel = { dragging = false; dragY = 0f },
                    onDragEnd = {
                        val target = (index + (dragY / rowHeight).roundToInt()).coerceIn(0, lastIndex)
                        if (target != index) viewModel.move(day.id, target)
                        dragging = false
                        dragY = 0f
                    },
                ) { change, amount -> change.consume(); dragY += amount.y }
            }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("拖动 ${day.label}", Modifier.width(180.dp))
        TextButton(onClick = { expanded = true }) { Text("操作 ${day.label}") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("前插") }, onClick = { expanded = false; viewModel.insertBefore(day.id) })
            DropdownMenuItem(text = { Text("后插") }, onClick = { expanded = false; viewModel.insertAfter(day.id) })
            DropdownMenuItem(text = { Text("删除") }, onClick = { expanded = false; viewModel.requestDelete(day) })
        }
    }
}
