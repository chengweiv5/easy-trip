package com.yangchengwei.easytrip.place.ui

import androidx.activity.compose.BackHandler

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yangchengwei.easytrip.core.ui.component.CompactSecondaryButton as TextButton

@Composable
fun PlaceSearchRoute(
    viewModel: PlaceSearchViewModel,
    onBack: () -> Unit,
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value

    BackHandler { onBack() }

    LaunchedEffect(state.shouldNavigateBack) {
        if (state.shouldNavigateBack) {
            viewModel.consumeBack()
            onBack()
        }
    }

    PlaceSearchContent(state, viewModel::dispatch)
    state.pendingCollectionRemoval?.let { pending ->
        val busy = pending.candidate.poiId in state.collectionBusyPoiIds
        AlertDialog(
            onDismissRequest = {
                if (!busy) viewModel.dispatch(PlaceSearchAction.DismissRemovalConfirmation)
            },
            title = { Text("取消收藏 ${pending.place.name}？") },
            text = { Text("将同时删除 ${pending.usageCount} 次行程安排及受影响路线。") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.dispatch(PlaceSearchAction.ConfirmRemoval) },
                    enabled = !busy,
                ) {
                    Text("确认取消收藏")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dispatch(PlaceSearchAction.DismissRemovalConfirmation) },
                    enabled = !busy,
                ) {
                    Text("取消")
                }
            },
        )
    }
}
