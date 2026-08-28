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
import com.yangchengwei.easytrip.core.ui.component.ConfirmationDialog
import com.yangchengwei.easytrip.core.ui.component.ConfirmationUiModel
import com.yangchengwei.easytrip.core.ui.component.SelectablePill
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripSettingsContent(
    state: TripSettingsUiState,
    onBack: () -> Unit,
    onRename: (String) -> Unit,
    onTravelMode: (TravelMode) -> Unit,
    onDateEndDraft: (LocalDate?) -> Unit,
    onSubmitDateRange: () -> Unit,
    onCancelDateRange: () -> Unit,
    onConfirmDateRange: () -> Unit,
    onRetryDateRangeSync: () -> Unit,
    onRequestDeleteDay: (DayUi) -> Unit,
    onRetryDeleteDay: () -> Unit,
    onCancelDeleteDay: () -> Unit,
    onConfirmDeleteDay: () -> Unit,
    onRetryTripObservation: () -> Unit = {},
) {
    var editingName by remember { mutableStateOf(false) }
    var name by remember(state.name) { mutableStateOf(state.name) }
    var endText by remember(state.dateRange.endDate) { mutableStateOf(state.dateRange.endDate?.toString().orEmpty()) }
    var inputError by remember { mutableStateOf<String?>(null) }
    val datePhase = state.dateRange.phase
    val dateBusy = datePhase is DateRangeChangePhase.Applying || datePhase is DateRangeChangePhase.AwaitingRoom
    val dateMutationLocked = datePhase is DateRangeChangePhase.Previewing ||
        datePhase is DateRangeChangePhase.AwaitingConfirmation || dateBusy ||
        datePhase is DateRangeChangePhase.SyncFailed
    val screenBusy = dateBusy || state.dayDeleteInProgress
    val settingsWriteLocked = dateMutationLocked || state.dayDeleteInProgress
    val hasDates = state.dateRange.startDate != null

    Scaffold(topBar = {
        CenterAlignedTopAppBar(
            title = { Text(state.name.ifEmpty { "旅行设置" }, Modifier.semantics { contentDescription = "重命名旅行" }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            navigationIcon = {
                IconButton(
                    onClick = onBack,
                    enabled = !screenBusy,
                    modifier = Modifier.testTag("settings-back"),
                ) { Text("返回") }
            },
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (state.observationError != null) {
                Text(state.observationError, color = Color.Red)
                TextButton(onRetryTripObservation) { Text("重试") }
            } else {
            TextButton({ name = state.name; editingName = true }, enabled = !settingsWriteLocked) { Text("修改旅行名称") }
            Text("整体出行日期")
            Text(
                state.dateRange.startDate?.toString() ?: "未设置日期",
                modifier = Modifier.fillMaxWidth().testTag("settings-start-date"),
            )
            if (hasDates) {
                OutlinedTextField(
                    value = endText,
                    onValueChange = { value ->
                        endText = value
                        val parsed = runCatching { value.takeIf(String::isNotBlank)?.let(LocalDate::parse) }
                        inputError = if (parsed.isFailure) "请输入 YYYY-MM-DD 格式日期" else null
                        if (parsed.isSuccess) onDateEndDraft(parsed.getOrNull())
                    },
                    label = { Text("结束日期 YYYY-MM-DD") },
                    modifier = Modifier.testTag("settings-end-date"),
                    enabled = !dateMutationLocked && !state.dayDeleteInProgress,
                )
                Button(
                    onClick = {
                        if (runCatching { LocalDate.parse(endText) }.isFailure) {
                            inputError = "请输入 YYYY-MM-DD 格式日期"
                        } else {
                            inputError = null
                            onSubmitDateRange()
                        }
                    },
                    enabled = !dateMutationLocked && !state.dayDeleteInProgress && inputError == null,
                    modifier = Modifier.testTag("settings-apply-date-range"),
                ) { Text("应用日期范围") }
            }
            (inputError ?: state.dateRange.error)?.let { Text(it, color = Color.Red) }
            if (dateBusy && state.dateRange.confirmation?.deletedDayIds.isNullOrEmpty()) {
                Text("正在保存日期范围…", modifier = Modifier.testTag("settings-date-progress"))
            }
            if (datePhase is DateRangeChangePhase.SyncFailed) {
                TextButton(
                    onClick = onRetryDateRangeSync,
                    modifier = Modifier.testTag("settings-date-sync-retry"),
                ) { Text("重新同步") }
            }
            Text("出行方式")
            Row(Modifier.selectableGroup()) {
                SelectablePill(state.travelMode == TravelMode.FLEXIBLE, { onTravelMode(TravelMode.FLEXIBLE) }, { Text("灵活") }, Modifier.testTag("settings-mode-FLEXIBLE"), enabled = !settingsWriteLocked, role = Role.RadioButton)
                SelectablePill(state.travelMode == TravelMode.SELF_DRIVE, { onTravelMode(TravelMode.SELF_DRIVE) }, { Text("自驾") }, Modifier.testTag("settings-mode-SELF_DRIVE"), enabled = !settingsWriteLocked, role = Role.RadioButton)
            }
            Text("旅行日")
            state.days.forEach { day ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(day.label)
                    TextButton(
                        onClick = { onRequestDeleteDay(day) },
                        modifier = Modifier.testTag("delete-day-${day.id}"),
                        enabled = state.days.size > 1 && !settingsWriteLocked,
                    ) { Text("删除", color = Color.Red) }
                }
            }
            if (state.pendingDayDeletion == null && state.dayDeletionRetry != null && state.dayDeleteError != null) {
                Text(state.dayDeleteError, color = Color.Red)
                TextButton(onRetryDeleteDay, enabled = !settingsWriteLocked) { Text("重试检查") }
            }
            }
        }
    }
    if (editingName) AlertDialog(
        onDismissRequest = { if (!settingsWriteLocked) { name = state.name; editingName = false } },
        title = { Text("重命名旅行") },
        text = { OutlinedTextField(name, { name = it }, label = { Text("旅行名称") }, enabled = !settingsWriteLocked) },
        confirmButton = { TextButton({ onRename(name); editingName = false }, enabled = name.isNotBlank() && !settingsWriteLocked) { Text("保存名称") } },
        dismissButton = { TextButton({ name = state.name; editingName = false }, enabled = !settingsWriteLocked) { Text("取消") } },
    )
    val dateImpact = when (datePhase) {
        is DateRangeChangePhase.AwaitingConfirmation -> datePhase.impact
        is DateRangeChangePhase.Applying -> datePhase.impact.takeIf { it.deletedDayIds.isNotEmpty() }
        is DateRangeChangePhase.AwaitingRoom -> datePhase.impact.takeIf { it.deletedDayIds.isNotEmpty() }
        else -> null
    }
    dateImpact?.let { impact ->
        ConfirmationDialog(
            model = ConfirmationUiModel(
                title = "确认修改日期范围？",
                message = "缩短日期会删除超出范围的旅行内容。",
                deletedItems = listOf(
                    "${impact.deletedDayIds.size} 个尾部旅行日",
                    "${impact.deletedItineraryItems} 个行程项",
                    "${impact.deletedRouteLegs} 个路线段",
                ),
                retainedItems = listOf("${impact.retainedSavedPlaces} 个收藏地点"),
                confirmLabel = "确认修改",
                dismissLabel = "取消",
                destructive = true,
                reversible = false,
            ),
            onConfirm = onConfirmDateRange,
            onDismiss = onCancelDateRange,
            busy = dateBusy,
            errorMessage = state.dateRange.error,
            deletedItemTags = listOf(
                "settings-date-impact-days",
                "settings-date-impact-items",
                "settings-date-impact-legs",
            ),
            retainedItemTags = listOf("settings-date-impact-saved-places"),
        )
    }
    state.pendingDayDeletion?.takeUnless { dateMutationLocked }?.let { pending ->
        AlertDialog(
            onDismissRequest = { if (!state.dayDeleteInProgress) onCancelDeleteDay() },
            title = { Text("删除 ${pending.day.label}？") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("将删除 ${pending.impact.itineraryItems} 个行程项和 ${pending.impact.routeLegs} 个路线段；${pending.impact.retainedSavedPlaces} 个收藏地点会保留。此操作不可撤销，后续旅行日日期编号和路线将变化。")
                    state.dayDeleteError?.let { Text(it, color = Color.Red) }
                }
            },
            confirmButton = { TextButton(onConfirmDeleteDay, enabled = !state.dayDeleteInProgress) { Text("确认删除", color = Color.Red) } },
            dismissButton = { TextButton(onCancelDeleteDay, enabled = !state.dayDeleteInProgress) { Text("取消") } },
            properties = androidx.compose.ui.window.DialogProperties(
                dismissOnBackPress = !state.dayDeleteInProgress,
                dismissOnClickOutside = !state.dayDeleteInProgress,
            ),
        )
    }
}
