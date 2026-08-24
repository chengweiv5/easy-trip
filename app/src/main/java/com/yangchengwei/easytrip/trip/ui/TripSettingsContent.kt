package com.yangchengwei.easytrip.trip.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.core.ui.component.CompactPrimaryButton as Button
import com.yangchengwei.easytrip.core.ui.component.CompactSecondaryButton as TextButton
import com.yangchengwei.easytrip.core.ui.component.SelectablePill
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripSettingsContent(
    state: TripSettingsUiState,
    onBack: () -> Unit,
    onRename: (String) -> Unit,
    onTravelMode: (TravelMode) -> Unit,
    onDateDraft: (LocalDate?, LocalDate?) -> Unit,
    onSubmitDateRange: () -> Unit,
    onCancelDateRange: () -> Unit,
    onConfirmDateRange: () -> Unit,
    onRequestDeleteDay: (DayUi) -> Unit,
    onCancelDeleteDay: () -> Unit,
    onConfirmDeleteDay: () -> Unit,
) {
    var editingName by remember { mutableStateOf(false) }
    var name by remember(state.name) { mutableStateOf(state.name) }
    var startText by remember(state.dateRange.startDate) { mutableStateOf(state.dateRange.startDate?.toString().orEmpty()) }
    var endText by remember(state.dateRange.endDate) { mutableStateOf(state.dateRange.endDate?.toString().orEmpty()) }
    var inputError by remember { mutableStateOf<String?>(null) }
    Scaffold(topBar = {
        CenterAlignedTopAppBar(
            title = { Text(state.name.ifEmpty { "旅行设置" }, Modifier.semantics { contentDescription = "重命名旅行" }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            navigationIcon = { IconButton(onClick = onBack) { Text("返回") } },
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TextButton({ name = state.name; editingName = true }) { Text("修改旅行名称") }
            Text("整体出行日期")
            OutlinedTextField(startText, { startText = it }, label = { Text("开始日期 YYYY-MM-DD") }, modifier = Modifier.testTag("settings-start-date"))
            OutlinedTextField(endText, { endText = it }, label = { Text("结束日期 YYYY-MM-DD") }, modifier = Modifier.testTag("settings-end-date"))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    val start = runCatching { startText.takeIf(String::isNotBlank)?.let(LocalDate::parse) }
                    val end = runCatching { endText.takeIf(String::isNotBlank)?.let(LocalDate::parse) }
                    if (start.isFailure || end.isFailure) {
                        inputError = "请输入 YYYY-MM-DD 格式日期"
                    } else {
                        inputError = null
                        onDateDraft(start.getOrNull(), end.getOrNull())
                        onSubmitDateRange()
                    }
                }) { Text("应用日期范围") }
                TextButton(onClick = { startText = ""; endText = ""; inputError = null; onDateDraft(null, null); onSubmitDateRange() }) { Text("无日期") }
            }
            (inputError ?: state.dateRange.error)?.let { Text(it, color = Color.Red) }
            Text("出行方式")
            Row(Modifier.selectableGroup()) {
                SelectablePill(state.travelMode == TravelMode.FLEXIBLE, { onTravelMode(TravelMode.FLEXIBLE) }, { Text("灵活") }, Modifier.testTag("settings-mode-FLEXIBLE"), role = Role.RadioButton)
                SelectablePill(state.travelMode == TravelMode.SELF_DRIVE, { onTravelMode(TravelMode.SELF_DRIVE) }, { Text("自驾") }, Modifier.testTag("settings-mode-SELF_DRIVE"), role = Role.RadioButton)
            }
            Text("旅行日")
            state.days.forEach { day ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(day.label)
                    TextButton(
                        onClick = { onRequestDeleteDay(day) },
                        modifier = Modifier.testTag("delete-day-${day.id}"),
                        enabled = state.days.size > 1,
                    ) { Text("删除", color = Color.Red) }
                }
            }
        }
    }
    if (editingName) AlertDialog(
        onDismissRequest = { name = state.name; editingName = false },
        title = { Text("重命名旅行") },
        text = { OutlinedTextField(name, { name = it }, label = { Text("旅行名称") }) },
        confirmButton = { TextButton({ onRename(name); editingName = false }, enabled = name.isNotBlank()) { Text("保存名称") } },
        dismissButton = { TextButton({ name = state.name; editingName = false }) { Text("取消") } },
    )
    state.dateRange.confirmation?.let { impact ->
        AlertDialog(
            onDismissRequest = { if (!state.dateRange.submitting) onCancelDateRange() },
            title = { Text("确认修改日期范围？") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("将删除尾部旅行日：${impact.deletedDayIds.joinToString()}。")
                    Text("同时删除 ${impact.deletedItineraryItems} 个行程项和 ${impact.deletedRouteLegs} 个路线段；${impact.retainedSavedPlaces} 个收藏地点会保留。此操作不可撤销，后续旅行日编号将变化。")
                    state.dateRange.error?.let { Text(it, color = Color.Red) }
                }
            },
            confirmButton = {
                TextButton(onConfirmDateRange, enabled = !state.dateRange.submitting) {
                    Text(if (state.dateRange.error == null) "确认修改" else "重新计算并重试", color = Color.Red)
                }
            },
            dismissButton = { TextButton(onCancelDateRange, enabled = !state.dateRange.submitting) { Text("取消") } },
        )
    }
    state.pendingDayDeletion?.let { pending ->
        AlertDialog(
            onDismissRequest = onCancelDeleteDay,
            title = { Text("删除 ${pending.day.label}？") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("将删除 ${pending.impact.itineraryItems} 个行程项和 ${pending.impact.routeLegs} 个路线段；${pending.impact.retainedSavedPlaces} 个收藏地点会保留。此操作不可撤销，后续旅行日日期编号和路线将变化。")
                    state.dayDeleteError?.let { Text(it, color = Color.Red) }
                }
            },
            confirmButton = { TextButton(onConfirmDeleteDay, enabled = !state.dayDeleteInProgress) { Text("确认删除", color = Color.Red) } },
            dismissButton = { TextButton(onCancelDeleteDay, enabled = !state.dayDeleteInProgress) { Text("取消") } },
        )
    }
}
