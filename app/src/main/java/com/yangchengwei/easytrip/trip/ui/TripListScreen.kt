package com.yangchengwei.easytrip.trip.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.yangchengwei.easytrip.core.ui.component.ConfirmationDialog
import com.yangchengwei.easytrip.core.ui.component.EasyTripPrimaryButton
import com.yangchengwei.easytrip.core.ui.component.EasyTripSecondaryButton

@Composable
fun TripListScreen(
    viewModel: TripListViewModel,
    onCreateTrip: () -> Unit,
    onWorkspace: (String) -> Unit,
    onSettings: (String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(viewModel, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.navigation.collect {
                when (it) {
                    is TripListNavigation.OpenWorkspace -> onWorkspace(it.tripId)
                    is TripListNavigation.OpenSettings -> onSettings(it.tripId)
                }
            }
        }
    }
    TripListContent(
        state = state,
        onAction = { action ->
            if (action == TripListAction.CreateTrip) onCreateTrip() else viewModel.onAction(action)
        },
    )
    when (val deletion = state.deletion) {
        TripDeletionUiState.Idle -> Unit
        is TripDeletionUiState.LoadingImpact -> DeletionImpactDialog(
            tripName = deletion.tripName,
            message = "正在查询删除影响…",
            onDismiss = viewModel::cancelDelete,
        )
        is TripDeletionUiState.ImpactFailure -> DeletionImpactDialog(
            tripName = deletion.tripName,
            message = "未删除旅行\n${deletion.message}",
            onDismiss = viewModel::cancelDelete,
            onRetry = { viewModel.onAction(TripListAction.RetryDeleteImpact) },
        )
        is TripDeletionUiState.Ready -> ConfirmationDialog(
            model = deletion.confirmation,
            onConfirm = {
                viewModel.onAction(
                    if (deletion.confirmationSyncFailed) {
                        TripListAction.RetryDeletionSync
                    } else {
                        TripListAction.ConfirmDelete
                    },
                )
            },
            onDismiss = viewModel::cancelDelete,
            busy = deletion.isDeleting,
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
            androidx.compose.foundation.layout.Row(
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                if (onRetry == null) CircularProgressIndicator(Modifier.testTag("trip-delete-impact-loading"))
                androidx.compose.foundation.layout.Column {
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
            EasyTripSecondaryButton(onDismiss, enabled = onRetry != null) { Text("取消") }
        },
    )
}
