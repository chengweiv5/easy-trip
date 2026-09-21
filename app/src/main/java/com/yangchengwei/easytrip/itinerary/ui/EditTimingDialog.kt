package com.yangchengwei.easytrip.itinerary.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import com.yangchengwei.easytrip.core.ui.component.CompactSecondaryButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import java.time.LocalTime

@Composable
fun EditTimingDialog(initialTime: LocalTime?, initialMinutes: Int?, onDismiss: () -> Unit, onSave: (LocalTime?, Int?) -> Unit) {
    var timeText by remember(initialTime) { mutableStateOf(initialTime?.toString().orEmpty()) }
    var minutesText by remember(initialMinutes) { mutableStateOf(initialMinutes?.toString().orEmpty()) }
    val time = timeText.takeIf(String::isNotBlank)?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
    val minutes = minutesText.takeIf(String::isNotBlank)?.toIntOrNull()
    val valid = (timeText.isBlank() || time != null) && (minutesText.isBlank() || minutes != null && minutes >= 0)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("到达与停留") },
        text = {
            Column {
                OutlinedTextField(
                    timeText,
                    { timeText = it },
                    label = { Text("到达时间（HH:mm）") },
                    isError = timeText.isNotBlank() && time == null,
                    singleLine = true,
                    modifier = Modifier.testTag("arrival-time-input"),
                )
                OutlinedTextField(
                    minutesText,
                    { minutesText = it.filter(Char::isDigit) },
                    label = { Text("停留分钟") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.testTag("stay-minutes-input"),
                )
            }
        },
        confirmButton = { TextButton({ onSave(time, minutes) }, enabled = valid) { Text("保存") } },
        dismissButton = { TextButton(onDismiss) { Text("取消") } },
    )
}
