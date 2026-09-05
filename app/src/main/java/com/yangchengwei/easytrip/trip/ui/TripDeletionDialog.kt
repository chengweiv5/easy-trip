package com.yangchengwei.easytrip.trip.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.yangchengwei.easytrip.core.ui.component.ConfirmationDialog
import com.yangchengwei.easytrip.core.ui.component.EasyTripPrimaryButton
import com.yangchengwei.easytrip.core.ui.component.EasyTripSecondaryButton

@Composable
fun TripDeletionDialog(
    deletion: TripDeletionUiState,
    onCancel: () -> Unit,
    onRetryImpact: () -> Unit,
    onConfirm: () -> Unit,
    onRetrySync: () -> Unit,
) {
    when (deletion) {
        TripDeletionUiState.Idle -> Unit
        is TripDeletionUiState.LoadingImpact -> DeletionImpactDialog(
            tripName = deletion.tripName,
            message = "正在查询删除影响…",
            onDismiss = onCancel,
        )
        is TripDeletionUiState.ImpactFailure -> DeletionImpactDialog(
            tripName = deletion.tripName,
            message = "未删除旅行\n${deletion.message}",
            onDismiss = onCancel,
            onRetry = onRetryImpact,
        )
        is TripDeletionUiState.Ready -> ConfirmationDialog(
            model = deletion.confirmation,
            onConfirm = if (deletion.confirmationSyncFailed) onRetrySync else onConfirm,
            onDismiss = onCancel,
            busy = deletion.isDeleting,
            dismissible = !deletion.isDeleting && !deletion.confirmationSyncFailed,
            dismissEnabled = !deletion.isDeleting && !deletion.confirmationSyncFailed,
            showDismissAction = !deletion.confirmationSyncFailed,
            errorMessage = deletion.errorMessage,
            confirmLabel = if (deletion.confirmationSyncFailed) "重新同步" else deletion.confirmation.confirmLabel,
        )
    }
}

@Composable
private fun DeletionImpactDialog(
    tripName: String,
    message: String,
    onDismiss: () -> Unit,
    onRetry: (() -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = { if (onRetry != null) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = onRetry != null,
            dismissOnClickOutside = onRetry != null,
        ),
        title = { Text("删除$tripName？") },
        text = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onRetry == null) CircularProgressIndicator(Modifier.testTag("trip-delete-impact-loading"))
                Column {
                    message.lineSequence().forEach { Text(it) }
                }
            }
        },
        confirmButton = {
            onRetry?.let {
                EasyTripPrimaryButton(onClick = it, modifier = Modifier.testTag("trip-delete-impact-retry")) {
                    Text("重试")
                }
            }
        },
        dismissButton = {
            EasyTripSecondaryButton(
                onDismiss,
                enabled = onRetry != null,
                modifier = Modifier.testTag("trip-delete-impact-cancel"),
            ) { Text("取消") }
        },
    )
}
