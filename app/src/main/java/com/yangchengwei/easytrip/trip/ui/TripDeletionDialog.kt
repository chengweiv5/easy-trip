package com.yangchengwei.easytrip.trip.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
    val currentDeletion by rememberUpdatedState(deletion)
    val currentOnCancel by rememberUpdatedState(onCancel)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        fun cancelPendingImpact() {
            if (currentDeletion is TripDeletionUiState.LoadingImpact) currentOnCancel()
        }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) cancelPendingImpact()
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            cancelPendingImpact()
        }
    }
    when (deletion) {
        TripDeletionUiState.Idle, is TripDeletionUiState.LoadingImpact -> Unit
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
    onRetry: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("删除$tripName？") },
        text = {
            Column {
                message.lineSequence().forEach { Text(it) }
            }
        },
        confirmButton = {
            EasyTripPrimaryButton(onClick = onRetry, modifier = Modifier.testTag("trip-delete-impact-retry")) {
                Text("重试")
            }
        },
        dismissButton = {
            EasyTripSecondaryButton(
                onDismiss,
                modifier = Modifier.testTag("trip-delete-impact-cancel"),
            ) { Text("取消") }
        },
    )
}
