package com.yangchengwei.easytrip.itinerary.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import com.yangchengwei.easytrip.core.ui.component.EmptyState

@Composable
fun DayItinerarySheet(viewModel: DayItineraryViewModel, modifier: Modifier = Modifier) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    DayItineraryContent(state, modifier, viewModel::dispatch)
}

sealed interface DayItineraryAction {
    data object AppendTripDay : DayItineraryAction
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
    data class UpdateArrivalTime(val value: String) : DayItineraryAction
    data class UpdateStayMinutes(val value: String) : DayItineraryAction
    data object SaveEdit : DayItineraryAction
    data class SelectMode(val mode: TransportMode) : DayItineraryAction
    data object SaveMode : DayItineraryAction
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
        if (state.items.isNotEmpty() && state.selectedDayId != null) {
            TextButton(
                { onAction(DayItineraryAction.AddPlaces) },
                Modifier.testTag("add-places-to-selected-day"),
            ) { Text("从地点池添加") }
        }
        if (state.items.isNotEmpty() && state.savedPlaces.isNotEmpty()) {
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                state.savedPlaces.forEach { place ->
                    TextButton({ onAction(DayItineraryAction.AddPlace(place.id)) }, Modifier.testTag("add-place-${place.id}")) { Text("添加 ${place.name}") }
                }
            }
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        val byId = state.items.associateBy(ItineraryItemUi::id)
        LazyColumn(
            modifier = Modifier.testTag("day-itinerary-timeline"),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            if (state.items.isEmpty() && state.selectedDayId != null) {
                val dayNumber = state.days.firstOrNull { it.id == state.selectedDayId }?.index?.plus(1)
                item {
                    EmptyState(
                        title = if (dayNumber == null) "暂无行程" else "第${dayNumber}天 · 暂无行程",
                        message = "从地点池添加地点，开始安排这一天",
                        emptyIllustration = com.yangchengwei.easytrip.core.ui.component.EmptyIllustration.Itinerary,
                        action = {
                            TextButton(
                                { onAction(DayItineraryAction.AddPlaces) },
                                Modifier.testTag("add-places-to-selected-day"),
                            ) { Text("从地点池添加") }
                        },
                    )
                }
            }
            itemsIndexed(state.previewOrder, key = { _, id -> id }) { index, id ->
                val item = byId[id] ?: return@itemsIndexed
                ItineraryItemRow(
                    item,
                    index,
                    state.previewOrder.size,
                    { onAction(DayItineraryAction.PreviewMove(id, it)) },
                    { onAction(DayItineraryAction.CommitMove(id, it)) },
                    { action ->
                        onAction(
                            when (action) {
                                ItineraryItemMenuAction.EditTiming -> DayItineraryAction.RequestTiming(id)
                                ItineraryItemMenuAction.MoveToOtherDay -> DayItineraryAction.RequestCrossDay(id)
                                ItineraryItemMenuAction.Delete -> DayItineraryAction.RequestDelete(id)
                            },
                        )
                    },
                )
                val next = state.previewOrder.getOrNull(index + 1)
                state.legs.firstOrNull { it.fromItemId == id && it.toItemId == next }?.let { leg ->
                    RouteLegRow(leg, { onAction(DayItineraryAction.RequestMode(leg.id)) }, { onAction(DayItineraryAction.Retry(leg.id)) })
                }
            }
        }
    }
    if (showDialogs) state.editDraft?.let { draft ->
        AlertDialog(
            onDismissRequest = { if (!draft.isSaving) onAction(DayItineraryAction.DismissDialogs) },
            confirmButton = {},
            text = {
                EditItineraryItemContent(
                    draft = draft,
                    onArrivalTimeChange = { onAction(DayItineraryAction.UpdateArrivalTime(it)) },
                    onStayMinutesChange = { onAction(DayItineraryAction.UpdateStayMinutes(it)) },
                    onSave = { onAction(DayItineraryAction.SaveEdit) },
                    onCancel = { onAction(DayItineraryAction.DismissDialogs) },
                )
            },
        )
    }
    if (showDialogs) state.crossDayMove?.let { move ->
        AlertDialog(
            onDismissRequest = { if (!move.isMoving) onAction(DayItineraryAction.DismissDialogs) },
            title = { Text("移动到…") },
            text = {
                Column {
                    move.moveError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    state.days.filter { it.id != state.selectedDayId }.forEach { day ->
                        TextButton(
                            { onAction(DayItineraryAction.MoveToDay(day.id)) },
                            Modifier.testTag("move-to-${day.id}"),
                            enabled = !move.isMoving,
                        ) { Text("Day ${day.index + 1}") }
                    }
                }
            },
            confirmButton = {},
        )
    }
    if (showDialogs) state.deleteConfirmation?.let { confirmation ->
        AlertDialog(
            onDismissRequest = { if (!confirmation.isDeleting) onAction(DayItineraryAction.DismissDialogs) },
            title = { Text("移出${confirmation.placeName}？") },
            text = {
                Column {
                    Text("仅从当天行程移出，收藏仍保留；相邻路线将重新计算。")
                    confirmation.deleteError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(
                    { onAction(DayItineraryAction.ConfirmDelete) },
                    enabled = !confirmation.isDeleting,
                ) { Text(if (confirmation.isDeleting) "移出中…" else "确认移出") }
            },
            dismissButton = {
                TextButton(
                    { onAction(DayItineraryAction.DismissDialogs) },
                    enabled = !confirmation.isDeleting,
                ) { Text("取消") }
            },
        )
    }
    if (showDialogs) state.modeEditor?.let { editor ->
        AlertDialog(
            onDismissRequest = { if (!editor.isSaving) onAction(DayItineraryAction.DismissDialogs) },
            title = { Text("选择交通方式") },
            text = {
                Column {
                    editor.saveError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    TransportMode.entries.forEach { mode ->
                        TextButton(
                            { onAction(DayItineraryAction.SelectMode(mode)) },
                            Modifier.testTag("mode-option-${mode.name}"),
                            enabled = !editor.isSaving,
                        ) {
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
            confirmButton = {
                TextButton(
                    { onAction(DayItineraryAction.SaveMode) },
                    enabled = !editor.isSaving,
                ) { Text(if (editor.isSaving) "保存中…" else "保存") }
            },
        )
    }
}
