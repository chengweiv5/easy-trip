package com.yangchengwei.easytrip.itinerary.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.yangchengwei.easytrip.core.ui.component.CompactSecondaryButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yangchengwei.easytrip.core.model.TransportMode

@Composable
fun DayItinerarySheet(viewModel: DayItineraryViewModel, modifier: Modifier = Modifier) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    DayItineraryContent(state, modifier, viewModel::dispatch)
}

sealed interface DayItineraryAction {
    data object AddPlaces : DayItineraryAction
    data class AddPlace(val placeId: String) : DayItineraryAction
    data class PreviewMove(val itemId: String, val target: Int) : DayItineraryAction
    data class CommitMove(val itemId: String, val target: Int) : DayItineraryAction
    data class RequestTiming(val itemId: String) : DayItineraryAction
    data class RequestCrossDay(val itemId: String) : DayItineraryAction
    data class RequestDelete(val itemId: String) : DayItineraryAction
    data class RequestMode(val legId: String) : DayItineraryAction
    data class Retry(val legId: String) : DayItineraryAction
    data class MoveToDay(val dayId: String) : DayItineraryAction
    data class SaveTiming(val time: java.time.LocalTime?, val minutes: Int?) : DayItineraryAction
    data class OverrideMode(val mode: TransportMode) : DayItineraryAction
    data object ConfirmDelete : DayItineraryAction
    data object DismissDialogs : DayItineraryAction
}

@Composable
fun DayItineraryContent(
    state: DayItineraryUiState,
    modifier: Modifier = Modifier,
    onAction: (DayItineraryAction) -> Unit,
    showDialogs: Boolean = true,
) {
    Column(modifier.padding(12.dp)) {
        if (state.selectedDayId != null) {
            TextButton(
                { onAction(DayItineraryAction.AddPlaces) },
                Modifier.testTag("add-places-to-selected-day"),
            ) { Text("从地点池添加") }
        }
        if (state.savedPlaces.isNotEmpty()) {
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                state.savedPlaces.forEach { place ->
                    TextButton({ onAction(DayItineraryAction.AddPlace(place.id)) }, Modifier.testTag("add-place-${place.id}")) { Text("添加 ${place.name}") }
                }
            }
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        val byId = state.items.associateBy(ItineraryItemUi::id)
        LazyColumn {
            if (state.items.isEmpty()) {
                item { Text("暂无行程") }
            }
            itemsIndexed(state.previewOrder, key = { _, id -> id }) { index, id ->
                val item = byId[id] ?: return@itemsIndexed
                ItineraryItemRow(
                    item,
                    index,
                    state.previewOrder.size,
                    { onAction(DayItineraryAction.PreviewMove(id, it)) },
                    { onAction(DayItineraryAction.CommitMove(id, it)) },
                    { onAction(DayItineraryAction.RequestTiming(id)) },
                    { onAction(DayItineraryAction.RequestCrossDay(id)) },
                    { onAction(DayItineraryAction.RequestDelete(id)) },
                )
                val next = state.previewOrder.getOrNull(index + 1)
                state.legs.firstOrNull { it.fromItemId == id && it.toItemId == next }?.let { leg ->
                    RouteLegRow(leg, { onAction(DayItineraryAction.RequestMode(leg.id)) }, { onAction(DayItineraryAction.Retry(leg.id)) })
                }
            }
        }
    }
    if (showDialogs) state.timingItemId?.let { id ->
        val item = state.items.firstOrNull { it.id == id }
        EditTimingDialog(item?.arrivalTime, item?.stayMinutes, { onAction(DayItineraryAction.DismissDialogs) }, { time, minutes -> onAction(DayItineraryAction.SaveTiming(time, minutes)) })
    }
    if (showDialogs) state.moveItemId?.let {
        AlertDialog(
            onDismissRequest = { onAction(DayItineraryAction.DismissDialogs) },
            title = { Text("移动到…") },
            text = {
                Column {
                    state.days.filter { it.id != state.selectedDayId }.forEach { day ->
                        TextButton({ onAction(DayItineraryAction.MoveToDay(day.id)) }, Modifier.testTag("move-to-${day.id}")) { Text("Day ${day.index + 1}") }
                    }
                }
            },
            confirmButton = {},
        )
    }
    if (showDialogs) state.deleteItemId?.let {
        AlertDialog(
            onDismissRequest = { onAction(DayItineraryAction.DismissDialogs) },
            title = { Text("删除这次安排？") },
            confirmButton = { TextButton({ onAction(DayItineraryAction.ConfirmDelete) }) { Text("确认删除") } },
            dismissButton = { TextButton({ onAction(DayItineraryAction.DismissDialogs) }) { Text("取消") } },
        )
    }
    if (showDialogs) state.modeLegId?.let {
        AlertDialog(
            onDismissRequest = { onAction(DayItineraryAction.DismissDialogs) },
            title = { Text("选择交通方式") },
            text = {
                Column {
                    TransportMode.entries.forEach { mode ->
                        TextButton({ onAction(DayItineraryAction.OverrideMode(mode)) }, Modifier.testTag("mode-option-${mode.name}")) {
                            Text(when (mode) {
                                TransportMode.WALK -> "步行"
                                TransportMode.TAXI -> "打车"
                                TransportMode.DRIVE -> "驾车"
                                TransportMode.TRANSIT -> "公交"
                            })
                        }
                    }
                }
            },
            confirmButton = {},
        )
    }
}
