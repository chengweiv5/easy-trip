package com.yangchengwei.easytrip.trip.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.yangchengwei.easytrip.core.ui.component.SelectablePill
import com.yangchengwei.easytrip.core.ui.theme.EasyTripSurfaceSoft
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CreateTripFormFields(
    state: CreateTripUiState,
    onAction: (CreateTripAction) -> Unit,
    initialDateMillis: Long?,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }
    val enabled = !state.isSubmitting

    Column(modifier, verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Column(
            Modifier.testTag("create-name-container"),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("旅行名称", style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(
                value = state.name,
                onValueChange = { onAction(CreateTripAction.NameChanged(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("create-name")
                    .errorSemantics(state.nameError),
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
        Column(
            Modifier.testTag("create-day-count-container"),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("旅行天数", style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(
                value = state.dayCount,
                onValueChange = { onAction(CreateTripAction.DayCountChanged(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("create-day-count")
                    .errorSemantics(state.dayCountError),
                placeholder = { Text("至少 1 天") },
                isError = state.dayCountError != null,
                enabled = enabled,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            state.dayCountError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
        Column(
            Modifier.testTag("create-date-control-container"),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("出行日期", style = MaterialTheme.typography.labelLarge)
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(66.dp)
                    .selectableGroup()
                    .testTag("create-date-control")
                    .errorSemantics(state.dateError),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SelectablePill(
                    selected = state.timeMode == CreateTimeMode.DRAFT,
                    onClick = { onAction(CreateTripAction.TimeModeChanged(CreateTimeMode.DRAFT)) },
                    label = { Text("日期待定") },
                    modifier = Modifier.testTag("create-time-DRAFT"),
                    enabled = enabled,
                    role = Role.RadioButton,
                )
                SelectablePill(
                    selected = state.timeMode == CreateTimeMode.DATED,
                    onClick = {
                        onAction(CreateTripAction.TimeModeChanged(CreateTimeMode.DATED))
                        showPicker = true
                    },
                    label = {
                        val startDate = state.startDate
                        if (startDate == null) {
                            Text("指定日期")
                        } else {
                            Column {
                                Text(startDate.toString())
                                state.endDate?.let { Text(it.toString(), style = MaterialTheme.typography.bodySmall) }
                            }
                        }
                    },
                    modifier = Modifier.testTag("create-time-DATED"),
                    enabled = enabled,
                    role = Role.RadioButton,
                )
            }
            state.dateError?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("create-date-error"),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("出行方式", style = MaterialTheme.typography.labelLarge)
            Row(
                Modifier.fillMaxWidth().height(82.dp).selectableGroup().testTag("create-mode-options"),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SelectablePill(
                    state.travelMode == TravelMode.FLEXIBLE,
                    { onAction(CreateTripAction.TravelModeChanged(TravelMode.FLEXIBLE)) },
                    { Text("灵活") },
                    Modifier.weight(1f).testTag("create-mode-FLEXIBLE"),
                    enabled,
                    Role.RadioButton,
                )
                SelectablePill(
                    state.travelMode == TravelMode.SELF_DRIVE,
                    { onAction(CreateTripAction.TravelModeChanged(TravelMode.SELF_DRIVE)) },
                    { Text("自驾") },
                    Modifier.weight(1f).testTag("create-mode-SELF_DRIVE"),
                    enabled,
                    Role.RadioButton,
                )
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth().testTag("create-planning-tip"),
            color = EasyTripSurfaceSoft,
            shape = RoundedCornerShape(10.dp),
        ) {
            Box(Modifier.padding(horizontal = 12.dp, vertical = 11.dp), contentAlignment = Alignment.CenterStart) {
                Text(
                    "创建后先收藏感兴趣的地点，再从地点池添加到每天的行程。",
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }

    if (showPicker && enabled) {
        val millis = state.startDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli() ?: initialDateMillis
        val picker = rememberDatePickerState(initialSelectedDateMillis = millis, initialDisplayedMonthMillis = millis)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                CompactSecondaryButton(
                    onClick = {
                        picker.selectedDateMillis?.let {
                            onAction(CreateTripAction.StartDateChanged(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()))
                        }
                        showPicker = false
                    },
                    modifier = Modifier.testTag("create-date-confirm"),
                ) { Text("确定日期") }
            },
        ) {
            DatePicker(
                state = picker,
                modifier = Modifier.testTag("create-date-picker"),
                colors = DatePickerDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    headlineContentColor = MaterialTheme.colorScheme.onSurface,
                    weekdayContentColor = MaterialTheme.colorScheme.secondary,
                    subheadContentColor = MaterialTheme.colorScheme.secondary,
                    navigationContentColor = MaterialTheme.colorScheme.primary,
                    yearContentColor = MaterialTheme.colorScheme.onSurface,
                    currentYearContentColor = MaterialTheme.colorScheme.primary,
                    selectedYearContentColor = MaterialTheme.colorScheme.onPrimary,
                    selectedYearContainerColor = MaterialTheme.colorScheme.primary,
                    dayContentColor = MaterialTheme.colorScheme.onSurface,
                    selectedDayContentColor = MaterialTheme.colorScheme.onPrimary,
                    selectedDayContainerColor = MaterialTheme.colorScheme.primary,
                    todayContentColor = MaterialTheme.colorScheme.primary,
                    todayDateBorderColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
    }
}

private fun Modifier.errorSemantics(message: String?): Modifier =
    if (message == null) this else semantics { error(message) }
