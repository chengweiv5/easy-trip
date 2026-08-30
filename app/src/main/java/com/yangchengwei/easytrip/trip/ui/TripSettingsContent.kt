package com.yangchengwei.easytrip.trip.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.core.ui.component.ConfirmationDialog
import com.yangchengwei.easytrip.core.ui.component.ConfirmationUiModel
import com.yangchengwei.easytrip.core.ui.component.EasyTripDangerButton
import com.yangchengwei.easytrip.core.ui.component.EasyTripDialogSurface
import com.yangchengwei.easytrip.core.ui.component.EasyTripPrimaryButton
import com.yangchengwei.easytrip.core.ui.component.EasyTripSecondaryButton
import com.yangchengwei.easytrip.core.ui.theme.EasyTripBackground
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import java.time.LocalDate

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
    var editingDates by remember { mutableStateOf(false) }
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
    val dateImpact = when (datePhase) {
        is DateRangeChangePhase.AwaitingConfirmation -> datePhase.impact
        is DateRangeChangePhase.Applying -> datePhase.impact.takeIf { it.deletedDayIds.isNotEmpty() }
        is DateRangeChangePhase.AwaitingRoom -> datePhase.impact.takeIf { it.deletedDayIds.isNotEmpty() }
        else -> null
    }

    Column(
        Modifier.fillMaxSize()
            .background(EasyTripBackground)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = EasyTripTheme.spacing.settingsGrid, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(EasyTripTheme.spacing.settingsSectionGap),
    ) {
        SettingsHeader(state.name, onBack, !screenBusy)
        if (state.observationError != null) {
            Text(state.observationError, color = MaterialTheme.colorScheme.error)
            EasyTripSecondaryButton(onRetryTripObservation) { Text("重试") }
        } else {
            SettingsSection("基本信息") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(EasyTripTheme.sizes.settingsCardCornerRadius),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        SettingsRow(
                            icon = { Icon(Icons.Rounded.Edit, null) },
                            label = "修改旅行名称",
                            value = state.name.ifEmpty { "未命名旅行" },
                            enabled = !settingsWriteLocked,
                            onClick = { name = state.name; editingName = true },
                            modifier = Modifier.testTag("settings-rename"),
                            grouped = true,
                        )
                        if (hasDates) {
                            SettingsRow(
                                icon = { Icon(Icons.Rounded.DateRange, null) },
                                label = "整体出行日期",
                                value = "${state.dateRange.startDate} — ${state.dateRange.endDate}",
                                enabled = !settingsWriteLocked,
                                onClick = { editingDates = true },
                                modifier = Modifier.testTag("settings-date-row"),
                                grouped = true,
                            )
                        } else {
                            SettingsRow(
                                icon = { Icon(Icons.Rounded.DateRange, null) },
                                label = "出行日期",
                                value = "未设置日期",
                                enabled = false,
                                onClick = {},
                                modifier = Modifier.testTag("settings-start-date"),
                                grouped = true,
                            )
                        }
                    }
                }
            }
            inputError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            state.dateRange.error?.takeIf { datePhase !is DateRangeChangePhase.SyncFailed }?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (dateBusy && dateImpact == null) Text("正在保存日期范围…", Modifier.testTag("settings-date-progress"))
            if (datePhase is DateRangeChangePhase.SyncFailed) {
                Text(state.dateRange.error.orEmpty(), color = MaterialTheme.colorScheme.error)
                EasyTripSecondaryButton(onRetryDateRangeSync, modifier = Modifier.testTag("settings-date-sync-retry")) { Text("重新同步") }
            }
            SettingsSection("出行方式") {
                Row(
                    Modifier.fillMaxWidth().height(EasyTripTheme.sizes.settingsModeCardHeight).selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(EasyTripTheme.spacing.settingsCardGap),
                ) {
                    com.yangchengwei.easytrip.trip.ui.TravelModeCard(TravelMode.FLEXIBLE, state.travelMode == TravelMode.FLEXIBLE, { onTravelMode(TravelMode.FLEXIBLE) }, Modifier.weight(1f).testTag("settings-mode-FLEXIBLE"), !settingsWriteLocked)
                    com.yangchengwei.easytrip.trip.ui.TravelModeCard(TravelMode.SELF_DRIVE, state.travelMode == TravelMode.SELF_DRIVE, { onTravelMode(TravelMode.SELF_DRIVE) }, Modifier.weight(1f).testTag("settings-mode-SELF_DRIVE"), !settingsWriteLocked)
                }
            }
            SettingsSection("旅行日") {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    state.days.forEachIndexed { index, day ->
                        Surface(color = MaterialTheme.colorScheme.surface, shape = if (index == 0 || index == state.days.lastIndex) RoundedCornerShape(EasyTripTheme.sizes.settingsCardCornerRadius) else RoundedCornerShape(0.dp)) {
                            Row(Modifier.fillMaxWidth().height(EasyTripTheme.sizes.settingsDayRowHeight).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(Modifier.size(26.dp), shape = RoundedCornerShape(7.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = .12f)) {
                                    androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) { Text("${index + 1}", style = MaterialTheme.typography.labelMedium) }
                                }
                                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                                    Text("第 ${index + 1} 天", fontWeight = FontWeight.SemiBold)
                                    Text(day.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                androidx.compose.foundation.layout.Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("delete-day-${day.id}")
                                        .clickable(enabled = state.days.size > 1 && !settingsWriteLocked) { onRequestDeleteDay(day) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Rounded.Delete, "删除", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(30.dp))
                                }
                            }
                        }
                    }
                }
                if (state.pendingDayDeletion == null && state.dayDeletionRetry != null && state.dayDeleteError != null) {
                    Text(state.dayDeleteError, color = MaterialTheme.colorScheme.error)
                    EasyTripSecondaryButton(onRetryDeleteDay, enabled = !settingsWriteLocked) { Text("重试检查") }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("危险操作", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge)
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(EasyTripTheme.sizes.settingsCardCornerRadius), color = MaterialTheme.colorScheme.errorContainer, border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = .25f))) {
                    Text("删除旅行请返回旅行列表操作", Modifier.padding(14.dp), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }

    if (editingName && dateImpact == null) RenameDialog(name, { name = it }, { onRename(name); editingName = false }, { name = state.name; editingName = false }, !settingsWriteLocked)
    if (editingDates && hasDates && dateImpact == null) DateEditorDialog(
        startDate = state.dateRange.startDate!!,
        endText = endText,
        onEndTextChange = { value ->
            endText = value
            val parsed = runCatching { value.takeIf(String::isNotBlank)?.let(LocalDate::parse) }
            inputError = if (parsed.isFailure) "请输入 YYYY-MM-DD 格式日期" else null
            if (parsed.isSuccess) onDateEndDraft(parsed.getOrNull())
        },
        error = inputError ?: state.dateRange.error,
        onApply = {
            if (runCatching { LocalDate.parse(endText) }.isFailure) inputError = "请输入 YYYY-MM-DD 格式日期"
            else { inputError = null; editingDates = false; onSubmitDateRange() }
        },
        onDismiss = { editingDates = false },
        enabled = !settingsWriteLocked,
    )
    dateImpact?.let { impact ->
        editingDates = false
        ConfirmationDialog(
            model = ConfirmationUiModel("确认修改日期范围？", "缩短日期会删除超出范围的旅行内容。", listOf("${impact.deletedDayIds.size} 个尾部旅行日", "${impact.deletedItineraryItems} 个行程项", "${impact.deletedRouteLegs} 个路线段"), listOf("${impact.retainedSavedPlaces} 个收藏地点"), "确认修改", "取消", true, false),
            onConfirm = onConfirmDateRange, onDismiss = onCancelDateRange, busy = dateBusy, errorMessage = state.dateRange.error,
            deletedItemTags = listOf("settings-date-impact-days", "settings-date-impact-items", "settings-date-impact-legs"), retainedItemTags = listOf("settings-date-impact-saved-places"),
        )
    }
    state.pendingDayDeletion?.takeUnless { dateMutationLocked }?.let { pending ->
        ConfirmationDialog(
            model = ConfirmationUiModel("删除 ${pending.day.label}？", "将删除 ${pending.impact.itineraryItems} 个行程项和 ${pending.impact.routeLegs} 个路线段；${pending.impact.retainedSavedPlaces} 个收藏地点会保留。此操作不可撤销，后续旅行日日期编号和路线将变化。", emptyList(), emptyList(), "确认删除", "取消", true, false),
            onConfirm = onConfirmDeleteDay, onDismiss = onCancelDeleteDay, busy = state.dayDeleteInProgress, errorMessage = state.dayDeleteError,
        )
    }
}

@Composable
private fun SettingsHeader(name: String, onBack: () -> Unit, enabled: Boolean) = Row(
    Modifier.fillMaxWidth().height(EasyTripTheme.sizes.settingsHeaderHeight).testTag("settings-header"),
    verticalAlignment = Alignment.CenterVertically,
) {
    Surface(
        Modifier.size(40.dp).testTag("settings-back").clickable(enabled = enabled, onClick = onBack),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) { androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回") } }
    Column(Modifier.padding(start = 12.dp).weight(1f)) {
        Text("旅行设置", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(name.ifEmpty { "未命名旅行" }, Modifier.semantics { contentDescription = "重命名旅行" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    Surface(
        modifier = Modifier
            .height(EasyTripTheme.sizes.buttonHeight)
            .testTag("settings-done")
            .clickable(enabled = enabled, onClick = onBack),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("完成", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) = Column(
    verticalArrangement = Arrangement.spacedBy(EasyTripTheme.spacing.settingsCardGap),
) { Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold); content() }

@Composable
private fun SettingsRow(
    icon: @Composable () -> Unit,
    label: String,
    value: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    grouped: Boolean = false,
) = Surface(
    modifier.fillMaxWidth().height(EasyTripTheme.sizes.settingsRowHeight).clickable(enabled = enabled, onClick = onClick),
    shape = if (grouped) RoundedCornerShape(0.dp) else RoundedCornerShape(EasyTripTheme.sizes.settingsCardCornerRadius),
    color = MaterialTheme.colorScheme.surface,
) { Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) { Surface(Modifier.size(28.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = .12f), contentColor = MaterialTheme.colorScheme.primary) { androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) { icon() } }; Column(Modifier.padding(start = 12.dp).weight(1f)) { Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold) }; Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) } }

@Composable
private fun RenameDialog(name: String, onNameChange: (String) -> Unit, onSave: () -> Unit, onDismiss: () -> Unit, enabled: Boolean) = EasyTripDialogSurface(
    onDismiss,
    dismissible = enabled,
    width = EasyTripTheme.sizes.dialogWidth,
) { Column(Modifier.padding(EasyTripTheme.spacing.dialogContentVertical), verticalArrangement = Arrangement.spacedBy(EasyTripTheme.spacing.dialogSectionGap)) { Text("重命名旅行", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); OutlinedTextField(name, onNameChange, label = { Text("旅行名称") }, enabled = enabled, modifier = Modifier.fillMaxWidth()); DialogActions(onDismiss, onSave, "保存名称", enabled && name.isNotBlank(), false) } }

@Composable
private fun DateEditorDialog(startDate: LocalDate, endText: String, onEndTextChange: (String) -> Unit, error: String?, onApply: () -> Unit, onDismiss: () -> Unit, enabled: Boolean) = EasyTripDialogSurface(
    onDismiss,
    dismissible = enabled,
    width = EasyTripTheme.sizes.dialogWidth,
) {
    Column(
        Modifier.padding(EasyTripTheme.spacing.dialogContentHorizontal),
        verticalArrangement = Arrangement.spacedBy(EasyTripTheme.spacing.settingsCardGap),
    ) {
        Text("修改出行日期", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("旅行日将根据日期范围自动连续生成，不能单独修改某一天。", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        DateEditorRow("开始日期", startDate.toString(), Modifier.testTag("settings-start-date"))
        EditableDateEditorRow("结束日期", endText, onEndTextChange, enabled, Modifier.testTag("settings-end-date"))
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        DialogActions(onDismiss, onApply, "确认修改", enabled, false, Modifier.testTag("settings-apply-date-range"))
    }
}

@Composable
private fun DateEditorRow(label: String, value: String, modifier: Modifier = Modifier) = Surface(
    modifier.fillMaxWidth().height(EasyTripTheme.sizes.dialogFieldHeight),
    shape = RoundedCornerShape(EasyTripTheme.sizes.settingsCardCornerRadius),
    color = EasyTripBackground,
) {
    Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.DateRange, null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.padding(start = 10.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun EditableDateEditorRow(label: String, value: String, onValueChange: (String) -> Unit, enabled: Boolean, modifier: Modifier = Modifier) = Surface(
    Modifier.fillMaxWidth().height(EasyTripTheme.sizes.dialogFieldHeight),
    shape = RoundedCornerShape(EasyTripTheme.sizes.settingsCardCornerRadius),
    color = EasyTripBackground,
) {
    Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.DateRange, null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.padding(start = 10.dp).weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                modifier = modifier.fillMaxWidth().semantics { contentDescription = "结束日期" },
            )
        }
    }
}

@Composable private fun DialogActions(onDismiss: () -> Unit, onConfirm: () -> Unit, confirmLabel: String, enabled: Boolean, destructive: Boolean, modifier: Modifier = Modifier) = Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { EasyTripSecondaryButton(onDismiss, Modifier.weight(1f), enabled) { Text("取消") }; if (destructive) EasyTripDangerButton(onConfirm, Modifier.weight(1f), enabled) { Text(confirmLabel) } else EasyTripPrimaryButton(onConfirm, Modifier.weight(1f).then(modifier), enabled) { Text(confirmLabel) } }
