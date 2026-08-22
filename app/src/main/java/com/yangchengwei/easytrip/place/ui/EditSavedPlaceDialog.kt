package com.yangchengwei.easytrip.place.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import com.yangchengwei.easytrip.place.domain.SavedPlace

@Composable fun EditSavedPlaceDialog(place: SavedPlace, onDismiss: () -> Unit, onSave: (String, Set<String>) -> Unit) {
    var note by remember(place.id) { mutableStateOf(place.note) }
    var tags by remember(place.id) { mutableStateOf(place.tags.joinToString(",") { it.name }) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("编辑 ${place.name}") }, text = {
        Column { OutlinedTextField(note, { note = it }, label = { Text("备注") }); OutlinedTextField(tags, { tags = it }, label = { Text("标签（逗号分隔）") }) }
    }, confirmButton = { TextButton(onClick = { onSave(note, tags.split(',').map(String::trim).filter(String::isNotEmpty).toSet()) }) { Text("保存") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
}
