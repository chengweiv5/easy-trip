package com.yangchengwei.easytrip.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
@Composable
fun ConfirmationDialog(
    model: ConfirmationUiModel,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmEnabled: Boolean = true,
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(model.title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(model.message)
                ConfirmationSection("将删除", model.deletedItems, MaterialTheme.colorScheme.error)
                ConfirmationSection("将保留", model.retainedItems, MaterialTheme.colorScheme.onSurface)
            }
        },
        confirmButton = {
            if (model.destructive) {
                EasyTripDangerButton(onClick = onConfirm, enabled = confirmEnabled) {
                    Text(model.confirmLabel)
                }
            } else {
                EasyTripPrimaryButton(onClick = onConfirm, enabled = confirmEnabled) {
                    Text(model.confirmLabel)
                }
            }
        },
        dismissButton = {
            EasyTripSecondaryButton(onClick = onDismiss, enabled = confirmEnabled) {
                Text(model.dismissLabel)
            }
        },
    )
}

@Composable
private fun ConfirmationSection(
    title: String,
    items: List<String>,
    color: androidx.compose.ui.graphics.Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = color)
        items.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
    }
}
