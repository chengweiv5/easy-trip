package com.yangchengwei.easytrip.trip.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.core.ui.component.CompactPrimaryButton
import com.yangchengwei.easytrip.core.ui.component.CompactSecondaryButton
import com.yangchengwei.easytrip.core.ui.component.SelectablePill
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTripDialog(
    state: CreateTripUiState,
    onAction: (CreateTripAction) -> Unit,
    initialDateMillis: Long? = null,
) {
    var showPicker by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = { if (!state.isSubmitting) onAction(CreateTripAction.Dismiss) },
        title = { Text("创建旅行") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    state.name,
                    { onAction(CreateTripAction.NameChanged(it)) },
                    label = { Text("旅行名称") },
                    isError = state.nameError != null,
                    supportingText = state.nameError?.let { { Text(it) } },
                    enabled = !state.isSubmitting && state.requestId == null,
                )
                OutlinedTextField(
                    state.dayCount,
                    { onAction(CreateTripAction.DayCountChanged(it)) },
                    label = { Text("天数") },
                    isError = state.dayCountError != null,
                    supportingText = state.dayCountError?.let { { Text(it) } },
                    enabled = !state.isSubmitting && state.requestId == null,
                )
                Row(
                    Modifier
                        .selectableGroup()
                        .then(
                            if (state.dateError != null) {
                                Modifier
                                    .testTag("create-date-control")
                                    .semantics { error(state.dateError) }
                                    .border(1.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(10.dp))
                            } else {
                                Modifier.testTag("create-date-control")
                            },
                        ),
                ) {
                    SelectablePill(state.timeMode == CreateTimeMode.DRAFT, { onAction(CreateTripAction.TimeModeChanged(CreateTimeMode.DRAFT)) }, { Text("无日期") }, Modifier.testTag("create-time-DRAFT"), enabled = !state.isSubmitting && state.requestId == null, role = Role.RadioButton)
                    SelectablePill(
                        state.timeMode == CreateTimeMode.DATED,
                        {
                            if (state.timeMode != CreateTimeMode.DATED) {
                                onAction(CreateTripAction.TimeModeChanged(CreateTimeMode.DATED))
                            }
                            showPicker = true
                        },
                        { Text(state.startDate?.toString() ?: "指定日期") },
                        Modifier.testTag("create-time-DATED"),
                        enabled = !state.isSubmitting && state.requestId == null,
                        role = Role.RadioButton,
                    )
                }
                state.dateError?.let { message ->
                    Text(
                        message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .testTag("create-date-error")
                            .semantics { error(message) },
                    )
                }
                Row(Modifier.selectableGroup()) {
                    SelectablePill(state.travelMode == TravelMode.FLEXIBLE, { onAction(CreateTripAction.TravelModeChanged(TravelMode.FLEXIBLE)) }, { Text("灵活") }, Modifier.testTag("create-mode-FLEXIBLE"), enabled = !state.isSubmitting && state.requestId == null, role = Role.RadioButton)
                    SelectablePill(state.travelMode == TravelMode.SELF_DRIVE, { onAction(CreateTripAction.TravelModeChanged(TravelMode.SELF_DRIVE)) }, { Text("自驾") }, Modifier.testTag("create-mode-SELF_DRIVE"), enabled = !state.isSubmitting && state.requestId == null, role = Role.RadioButton)
                }
                state.submitError?.let { Text(it) }
            }
        },
        confirmButton = {
            CompactPrimaryButton(
                onClick = { onAction(CreateTripAction.Submit) },
                enabled = !state.isSubmitting,
                modifier = Modifier.testTag("create-submit"),
            ) { Text(if (state.isSubmitting) "创建中…" else "创建") }
        },
        dismissButton = { CompactSecondaryButton(onClick = { onAction(CreateTripAction.Dismiss) }, enabled = !state.isSubmitting) { Text("取消") } },
    )
    if (showPicker) {
        val picker = rememberDatePickerState(
            initialSelectedDateMillis = state.startDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli() ?: initialDateMillis,
            initialDisplayedMonthMillis = state.startDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli() ?: initialDateMillis,
        )
        DatePickerDialog(
            onDismissRequest = {
                showPicker = false
                if (state.startDate == null) onAction(CreateTripAction.TimeModeChanged(CreateTimeMode.DRAFT))
            },
            confirmButton = {
                CompactSecondaryButton(
                    onClick = {
                        picker.selectedDateMillis?.let { onAction(CreateTripAction.StartDateChanged(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())) }
                        showPicker = false
                    },
                    modifier = Modifier.testTag("create-date-confirm"),
                ) { Text("确定日期") }
            },
        ) { DatePicker(picker) }
    }
}
