package com.yangchengwei.easytrip.trip.ui

import androidx.compose.runtime.Composable
import com.yangchengwei.easytrip.core.ui.component.ConfirmationDialog
import com.yangchengwei.easytrip.core.ui.component.ConfirmationUiModel

/** Reuses the settings deletion preview and transaction from the workspace entry. */
@Composable
internal fun WorkspaceDayDeletionDialog(
    state: TripSettingsUiState,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
) {
    val pending = state.pendingDayDeletion
    val day = pending?.day ?: state.dayDeletionRetry ?: return
    val loading = pending == null && state.dayDeleteError == null
    ConfirmationDialog(
        model = ConfirmationUiModel(
            title = "删除 ${day.label}？",
            message = "此操作不可撤销，后续旅行日日期编号和路线将变化。收藏地点会保留。",
            deletedItems = pending?.impact?.let { listOf("${it.itineraryItems} 个行程项", "${it.routeLegs} 个路线段") }.orEmpty(),
            retainedItems = pending?.impact?.let { listOf("${it.retainedSavedPlaces} 个收藏地点") }.orEmpty(),
            confirmLabel = if (pending == null) "重新检查" else "确认删除",
            dismissLabel = "取消",
            destructive = true,
            reversible = false,
        ),
        onConfirm = if (pending == null) onRetry else onConfirm,
        onDismiss = onCancel,
        busy = loading || state.dayDeleteInProgress,
        dismissible = !state.dayDeleteInProgress,
        dismissEnabled = !state.dayDeleteInProgress,
        errorMessage = state.dayDeleteError,
    )
}
