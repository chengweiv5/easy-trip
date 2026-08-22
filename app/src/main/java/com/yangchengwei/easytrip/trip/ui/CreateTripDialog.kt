package com.yangchengwei.easytrip.trip.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
            Column {
                OutlinedTextField(state.createName, viewModel::setCreateName, label = { Text("旅行名称") })
                OutlinedTextField(state.createDays, viewModel::setCreateDays, label = { Text("天数") })
                Row {
                    FilterChip(state.createTimeMode == CreateTimeMode.DRAFT, { viewModel.setCreateTimeMode(CreateTimeMode.DRAFT) }, { Text("无日期") })
                    FilterChip(state.createTimeMode == CreateTimeMode.DATED, { viewModel.setCreateTimeMode(CreateTimeMode.DATED) }, { Text("指定日期") })
                }
                if (state.createTimeMode == CreateTimeMode.DATED) TextButton(onClick = { showPicker = true }) { Text(state.createStartDate?.toString() ?: "选择起始日期") }
                Row {
                    FilterChip(state.createTravelMode == TravelMode.FLEXIBLE, { viewModel.setCreateTravelMode(TravelMode.FLEXIBLE) }, { Text("灵活") })
                    FilterChip(state.createTravelMode == TravelMode.SELF_DRIVE, { viewModel.setCreateTravelMode(TravelMode.SELF_DRIVE) }, { Text("自驾") })
                }
            }
        },
        confirmButton = { Button(onClick = viewModel::create, enabled = state.createName.isNotBlank() && (state.createDays.toIntOrNull() ?: 0) >= 1 && (state.createTimeMode == CreateTimeMode.DRAFT || state.createStartDate != null)) { Text("创建") } },
        dismissButton = { Button(onClick = viewModel::dismissCreate) { Text("取消") } },
    )
    if (showPicker) {
        val picker = rememberDatePickerState(
            initialSelectedDateMillis = initialDateMillis,
            initialDisplayedMonthMillis = initialDateMillis,
        )
        DatePickerDialog(onDismissRequest = { showPicker = false }, confirmButton = {
            TextButton(onClick = {
                picker.selectedDateMillis?.let { viewModel.setCreateStartDate(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                showPicker = false
            }) { Text("确定日期") }
        }) { DatePicker(picker) }
    }
}
