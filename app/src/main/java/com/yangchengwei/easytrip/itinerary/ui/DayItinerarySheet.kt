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
    Column(modifier.padding(12.dp)) {
        if (state.savedPlaces.isNotEmpty()) {
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                state.savedPlaces.forEach { place ->
                    TextButton({ viewModel.addPlace(place.id) }, Modifier.testTag("add-place-${place.id}")) { Text("添加 ${place.name}") }
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
                    { viewModel.previewMove(id, it) },
                    { viewModel.commitMove(id, it) },
                    { viewModel.requestTiming(id) },
                    { viewModel.requestCrossDay(id) },
                    { viewModel.requestDelete(id) },
                )
                val next = state.previewOrder.getOrNull(index + 1)
                state.legs.firstOrNull { it.fromItemId == id && it.toItemId == next }?.let { leg ->
                    RouteLegRow(leg, { viewModel.requestMode(leg.id) }, { viewModel.retry(leg.id) })
                }
            }
        }
    }
    state.timingItemId?.let { id ->
        val item = state.items.firstOrNull { it.id == id }
        EditTimingDialog(item?.arrivalTime, item?.stayMinutes, viewModel::dismissDialogs, viewModel::saveTiming)
    }
    state.moveItemId?.let {
        AlertDialog(
            onDismissRequest = viewModel::dismissDialogs,
            title = { Text("移动到…") },
            text = {
                Column {
                    state.days.filter { it.id != state.selectedDayId }.forEach { day ->
                        TextButton({ viewModel.moveToDay(day.id) }, Modifier.testTag("move-to-${day.id}")) { Text("Day ${day.index + 1}") }
                    }
                }
            },
            confirmButton = {},
        )
    }
    state.deleteItemId?.let {
        AlertDialog(
            onDismissRequest = viewModel::dismissDialogs,
            title = { Text("删除这次安排？") },
            confirmButton = { TextButton(viewModel::confirmDelete) { Text("确认删除") } },
            dismissButton = { TextButton(viewModel::dismissDialogs) { Text("取消") } },
        )
    }
    state.modeLegId?.let {
        AlertDialog(
            onDismissRequest = viewModel::dismissDialogs,
            title = { Text("选择交通方式") },
            text = {
                Column {
                    TransportMode.entries.forEach { mode ->
                        TextButton({ viewModel.overrideMode(mode) }, Modifier.testTag("mode-option-${mode.name}")) {
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
