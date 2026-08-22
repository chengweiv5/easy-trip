package com.yangchengwei.easytrip.trip.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.core.ui.component.CompactPrimaryButton as Button
import com.yangchengwei.easytrip.core.ui.component.CompactSecondaryButton as TextButton
import com.yangchengwei.easytrip.core.ui.component.SelectablePill
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripSettingsScreen(viewModel: TripSettingsViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showPicker by remember { mutableStateOf(false) }
    var editingName by remember { mutableStateOf(false) }
    var name by remember(state.name) { mutableStateOf(state.name) }
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        state.name.ifEmpty { "旅行设置" },
                        Modifier
                            .clickable { editingName = true }
                            .semantics { contentDescription = "重命名旅行" },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = { IconButton(onClick = onBack) { Text("返回") } },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("出行日期")
                Button(onClick = { showPicker = true }) { Text(state.startDate?.toString() ?: "选择起始日期") }
                Button(onClick = { viewModel.setStartDate(null) }) { Text("无日期") }
            }
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("出行方式")
                Row(Modifier.selectableGroup()) {
                    SelectablePill(state.travelMode == TravelMode.FLEXIBLE, { viewModel.setTravelMode(TravelMode.FLEXIBLE) }, { Text("灵活") }, Modifier.testTag("settings-mode-FLEXIBLE"), role = Role.RadioButton)
                    SelectablePill(state.travelMode == TravelMode.SELF_DRIVE, { viewModel.setTravelMode(TravelMode.SELF_DRIVE) }, { Text("自驾") }, Modifier.testTag("settings-mode-SELF_DRIVE"), role = Role.RadioButton)
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
}
