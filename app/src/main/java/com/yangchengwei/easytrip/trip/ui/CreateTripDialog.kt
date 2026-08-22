package com.yangchengwei.easytrip.trip.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import com.yangchengwei.easytrip.core.ui.component.CompactPrimaryButton as Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.ui.component.SelectablePill
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import com.yangchengwei.easytrip.core.ui.component.CompactSecondaryButton as TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.yangchengwei.easytrip.core.model.TravelMode
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTripDialog(
    state: TripListUiState,
    viewModel: TripListViewModel,
    initialDateMillis: Long? = null,
) {
    var showPicker by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = viewModel::dismissCreate,
        title = { Text("创建旅行") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(state.createName, viewModel::setCreateName, label = { Text("旅行名称") })
                OutlinedTextField(state.createDays, viewModel::setCreateDays, label = { Text("天数") })
                Row(Modifier.selectableGroup()) {
                    SelectablePill(state.createTimeMode == CreateTimeMode.DRAFT, { viewModel.setCreateTimeMode(CreateTimeMode.DRAFT) }, { Text("无日期") }, Modifier.testTag("create-time-DRAFT"), role = Role.RadioButton)
                    SelectablePill(
                        state.createTimeMode == CreateTimeMode.DATED,
                        {
                            viewModel.setCreateTimeMode(CreateTimeMode.DATED)
                            showPicker = true
                        },
                        { Text(state.createStartDate?.toString() ?: "指定日期") },
                        Modifier.testTag("create-time-DATED"),
                        role = Role.RadioButton,
                    )
                }
                Row(Modifier.selectableGroup()) {
                    SelectablePill(state.createTravelMode == TravelMode.FLEXIBLE, { viewModel.setCreateTravelMode(TravelMode.FLEXIBLE) }, { Text("灵活") }, Modifier.testTag("create-mode-FLEXIBLE"), role = Role.RadioButton)
                    SelectablePill(state.createTravelMode == TravelMode.SELF_DRIVE, { viewModel.setCreateTravelMode(TravelMode.SELF_DRIVE) }, { Text("自驾") }, Modifier.testTag("create-mode-SELF_DRIVE"), role = Role.RadioButton)
                }
            }
        },
        confirmButton = { Button(onClick = viewModel::create, enabled = state.createName.isNotBlank() && (state.createDays.toIntOrNull() ?: 0) >= 1 && (state.createTimeMode == CreateTimeMode.DRAFT || state.createStartDate != null)) { Text("创建") } },
        dismissButton = { Button(onClick = viewModel::dismissCreate) { Text("取消") } },
    )
    if (showPicker) {
        val picker = rememberDatePickerState(
            initialSelectedDateMillis = state.createStartDate
                ?.atStartOfDay(ZoneOffset.UTC)
                ?.toInstant()
                ?.toEpochMilli()
                ?: initialDateMillis,
            initialDisplayedMonthMillis = state.createStartDate
                ?.atStartOfDay(ZoneOffset.UTC)
                ?.toInstant()
                ?.toEpochMilli()
                ?: initialDateMillis,
        )
        val dismissPicker = {
            showPicker = false
            if (state.createStartDate == null) viewModel.setCreateTimeMode(CreateTimeMode.DRAFT)
        }
        DatePickerDialog(onDismissRequest = dismissPicker, confirmButton = {
            TextButton(onClick = {
                picker.selectedDateMillis?.let { viewModel.setCreateStartDate(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                showPicker = false
            }) { Text("确定日期") }
        }) { DatePicker(picker) }
    }
}
