package com.yangchengwei.easytrip.trip.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.core.ui.component.CompactSecondaryButton
import com.yangchengwei.easytrip.core.ui.component.EasyTripPrimaryButton
import com.yangchengwei.easytrip.core.ui.component.SelectablePill
import com.yangchengwei.easytrip.core.ui.theme.EasyTripBorder
import com.yangchengwei.easytrip.core.ui.theme.EasyTripSurfaceSoft
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTripContent(
    state: CreateTripUiState,
    onAction: (CreateTripAction) -> Unit,
    modifier: Modifier = Modifier,
    initialDateMillis: Long? = null,
) {
    var showPicker by remember { mutableStateOf(false) }
    val enabled = !state.isSubmitting
    Scaffold(
        modifier = modifier.fillMaxSize().safeDrawingPadding().imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                state.submitError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                EasyTripPrimaryButton(
                    onClick = { onAction(CreateTripAction.Submit) },
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("create-submit"),
                    enabled = enabled,
                ) { Text(if (state.isSubmitting) "创建中…" else "继续") }
                Text(
                    if (state.nameError != null || state.dayCountError != null || state.dateError != null) "修正标红字段后即可创建旅行" else "地点与日期都可以稍后调整",
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    color = if (state.nameError != null || state.dayCountError != null || state.dateError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
    ) { contentPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
        Row(Modifier.fillMaxWidth().height(58.dp).testTag("create-header"), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(
                modifier = Modifier.size(40.dp).clickable(role = Role.Button) { onAction(CreateTripAction.Back) },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, EasyTripBorder),
            ) { Text("←", modifier = Modifier.padding(10.dp), style = MaterialTheme.typography.titleMedium) }
            Column {
                Text("创建旅行", style = MaterialTheme.typography.headlineMedium)
                Text("先确定基本信息，之后再慢慢规划", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StepNumber("1", true, Modifier.testTag("create-step-1"))
            Text("基本信息", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.width(28.dp).border(0.5.dp, EasyTripBorder))
            StepNumber("2", false)
            Text("开始规划", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
        }
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("旅行名称", style = MaterialTheme.typography.labelLarge)
                OutlinedTextField(
                    value = state.name,
                    onValueChange = { onAction(CreateTripAction.NameChanged(it)) },
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("create-name"),
                    placeholder = { Text("例如：杭州 · 春日慢游") },
                    isError = state.nameError != null,
                    enabled = enabled,
                    singleLine = true,
                )
                Text(
                    state.nameError ?: "一个容易辨认的名称，稍后可以修改",
                    color = if (state.nameError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("旅行天数", style = MaterialTheme.typography.labelLarge)
                OutlinedTextField(
                    value = state.dayCount,
                    onValueChange = { onAction(CreateTripAction.DayCountChanged(it)) },
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("create-day-count"),
                    placeholder = { Text("至少 1 天") },
                    isError = state.dayCountError != null,
                    enabled = enabled,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                state.dayCountError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("出行日期", style = MaterialTheme.typography.labelLarge)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(66.dp)
                        .selectableGroup()
                        .testTag("create-date-control")
                        .then(if (state.dateError != null) Modifier.semantics { error(state.dateError) } else Modifier),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SelectablePill(state.timeMode == CreateTimeMode.DRAFT, { onAction(CreateTripAction.TimeModeChanged(CreateTimeMode.DRAFT)) }, { Text("日期待定") }, Modifier.testTag("create-time-DRAFT"), enabled, Role.RadioButton)
                    SelectablePill(
                        state.timeMode == CreateTimeMode.DATED,
                        { onAction(CreateTripAction.TimeModeChanged(CreateTimeMode.DATED)); showPicker = true },
                        { Text(state.startDate?.toString() ?: "指定日期") },
                        Modifier.testTag("create-time-DATED"),
                        enabled,
                        Role.RadioButton,
                    )
                }
                state.dateError?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("create-date-error")) }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("出行方式", style = MaterialTheme.typography.labelLarge)
                Row(Modifier.fillMaxWidth().height(82.dp).selectableGroup().testTag("create-mode-options"), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SelectablePill(state.travelMode == TravelMode.FLEXIBLE, { onAction(CreateTripAction.TravelModeChanged(TravelMode.FLEXIBLE)) }, { Text("灵活") }, Modifier.weight(1f).testTag("create-mode-FLEXIBLE"), enabled, Role.RadioButton)
                    SelectablePill(state.travelMode == TravelMode.SELF_DRIVE, { onAction(CreateTripAction.TravelModeChanged(TravelMode.SELF_DRIVE)) }, { Text("自驾") }, Modifier.weight(1f).testTag("create-mode-SELF_DRIVE"), enabled, Role.RadioButton)
                }
            }
        }
            Surface(
                modifier = Modifier.fillMaxWidth().height(40.dp).testTag("create-planning-tip"),
                color = EasyTripSurfaceSoft,
                shape = RoundedCornerShape(10.dp),
            ) {
                Box(Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
                    Text("创建后先收藏感兴趣的地点，再从地点池添加到每天的行程。", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
    if (showPicker) {
        val millis = state.startDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli() ?: initialDateMillis
        val picker = rememberDatePickerState(initialSelectedDateMillis = millis, initialDisplayedMonthMillis = millis)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                CompactSecondaryButton(
                    onClick = {
                        onAction(CreateTripAction.StartDateChanged(picker.selectedDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }))
                        showPicker = false
                    },
                    modifier = Modifier.testTag("create-date-confirm"),
                ) { Text("确定日期") }
            },
        ) { DatePicker(picker) }
    }
}

@Composable
private fun StepNumber(value: String, active: Boolean, modifier: Modifier = Modifier) {
    Surface(modifier.size(22.dp), shape = CircleShape, color = if (active) MaterialTheme.colorScheme.primary else EasyTripSurfaceSoft) {
        Box(contentAlignment = Alignment.Center) {
            Text(value, color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        }
    }
}
