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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yangchengwei.easytrip.core.model.TransportMode
import com.yangchengwei.easytrip.core.ui.component.EmptyState

@Composable
fun DayItinerarySheet(
    viewModel: DayItineraryViewModel,
    modifier: Modifier = Modifier,
    onScheduleAgain: ((String) -> Unit)? = null,
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    DayItineraryContent(
        state = state,
        modifier = modifier,
        onAction = { action ->
            if (action is DayItineraryAction.ScheduleAgain) {
                onScheduleAgain?.invoke(action.itemId)
            } else {
                viewModel.dispatch(action)
            }
        },
        canScheduleAgain = onScheduleAgain != null,
    )
}

sealed interface DayItineraryAction {
    data object AppendTripDay : DayItineraryAction
    data object AddPlaces : DayItineraryAction
    data class AddPlace(val placeId: String) : DayItineraryAction
    data class PreviewMove(val itemId: String, val target: Int) : DayItineraryAction
    data class CommitMove(val itemId: String, val target: Int) : DayItineraryAction
    data class RequestTiming(val itemId: String) : DayItineraryAction
    data class ScheduleAgain(val itemId: String) : DayItineraryAction
    data class RequestCrossDay(val itemId: String) : DayItineraryAction
    data class RequestDelete(val itemId: String) : DayItineraryAction
    data class RequestMode(val legId: String) : DayItineraryAction
    data class Retry(val legId: String, val expectedVersion: Long) : DayItineraryAction
    data class MoveToDay(val dayId: String) : DayItineraryAction
    data class UpdateArrivalTime(val value: String) : DayItineraryAction
    data class UpdateStayMinutes(val value: String) : DayItineraryAction
    data class UpdateNote(val value: String) : DayItineraryAction
    data object SaveEdit : DayItineraryAction
    data object DismissEditSaveError : DayItineraryAction
    data class SelectMode(val mode: TransportMode) : DayItineraryAction
    data object ClearSelectedModeOverride : DayItineraryAction
    data class UpdateRouteDurationMinutes(val value: String) : DayItineraryAction
    data class UpdateRouteNote(val value: String) : DayItineraryAction
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
    canScheduleAgain: Boolean = false,
) {
    Column(modifier.padding(12.dp)) {
        if (state.items.isNotEmpty() && state.selectedDayId != null) {
            val dayNumber = state.days.firstOrNull { it.id == state.selectedDayId }?.index?.plus(1)
            Text(
                text = if (dayNumber == null) "${state.items.size} 站" else "第 $dayNumber 天 · ${state.items.size} 站",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .testTag("day-itinerary-summary")
                    .semantics { heading() },
            )
        }
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
        val displayItems = currentDisplayItems(state.items, state.previewOrder)
        val visibleLegs = visibleRouteLegs(state.items, state.previewOrder, state.legs)
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
                        verticalPadding = 16.dp,
                        action = {
                            TextButton(
                                { onAction(DayItineraryAction.AddPlaces) },
                                Modifier.testTag("add-places-to-selected-day"),
                            ) { Text("从地点池添加") }
                        },
                    )
                }
            }
            itemsIndexed(displayItems, key = { _, item -> item.id }) { index, item ->
                val id = item.id
                ItineraryItemRow(
                    item = item,
                    index = index,
                    count = displayItems.size,
                    onPreview = { onAction(DayItineraryAction.PreviewMove(id, it)) },
                    onCommit = { onAction(DayItineraryAction.CommitMove(id, it)) },
                    onMenuAction = { action ->
                        onAction(
                            when (action) {
                                ItineraryItemMenuAction.EditTiming -> DayItineraryAction.RequestTiming(id)
                                ItineraryItemMenuAction.ScheduleAgain -> DayItineraryAction.ScheduleAgain(id)
                                ItineraryItemMenuAction.MoveToOtherDay -> DayItineraryAction.RequestCrossDay(id)
                                ItineraryItemMenuAction.Delete -> DayItineraryAction.RequestDelete(id)
                            },
                        )
                    },
                    canScheduleAgain = canScheduleAgain && item.placeId != null,
                )
                val next = displayItems.getOrNull(index + 1)?.id
                visibleLegs.firstOrNull { it.fromItemId == id && it.toItemId == next }?.let { leg ->
                    RouteLegRow(
                        leg = leg,
                        fromPlaceName = displayItems[index].name,
                        toPlaceName = displayItems[index + 1].name,
                        onMode = if (leg.state is RouteLegUiState.Ready) {
                            { onAction(DayItineraryAction.RequestMode(leg.id)) }
                        } else {
                            null
                        },
                        onRetry = if (leg.state is RouteLegUiState.Failed) {
                            { onAction(DayItineraryAction.Retry(leg.id, leg.version)) }
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }
    if (showDialogs) state.editDraft?.let { draft ->
        AlertDialog(
            onDismissRequest = {
                when {
                    draft.isSaving -> Unit
                    draft.saveError != null -> onAction(DayItineraryAction.DismissEditSaveError)
                    else -> onAction(DayItineraryAction.DismissDialogs)
                }
            },
            confirmButton = {},
            text = {
                if (draft.saveError != null) {
                    ItinerarySaveFailureContent(
                        onKeepEditing = { onAction(DayItineraryAction.DismissEditSaveError) },
                        onRetrySave = { onAction(DayItineraryAction.SaveEdit) },
                    )
                } else {
                    EditItineraryItemContent(
                        draft = draft,
                        onArrivalTimeChange = { onAction(DayItineraryAction.UpdateArrivalTime(it)) },
                        onStayMinutesChange = { onAction(DayItineraryAction.UpdateStayMinutes(it)) },
                        onNoteChange = { onAction(DayItineraryAction.UpdateNote(it)) },
                        onScheduleAgain = if (canScheduleAgain && draft.placeId != null) {
                            { onAction(DayItineraryAction.ScheduleAgain(draft.itemId)) }
                        } else {
                            null
                        },
                        onSave = { onAction(DayItineraryAction.SaveEdit) },
                        onCancel = { onAction(DayItineraryAction.DismissDialogs) },
                    )
                }
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
                    Text("仅移除本次安排；收藏仍保留；相邻路线将重新计算。")
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
            confirmButton = {},
            text = {
                EditRouteLegContent(
                    draft = editor,
                    onSelectMode = { onAction(DayItineraryAction.SelectMode(it)) },
                    onClearSelectedModeOverride = { onAction(DayItineraryAction.ClearSelectedModeOverride) },
                    onDurationMinutesChange = { onAction(DayItineraryAction.UpdateRouteDurationMinutes(it)) },
                    onNoteChange = { onAction(DayItineraryAction.UpdateRouteNote(it)) },
                    onSave = { onAction(DayItineraryAction.SaveMode) },
                    onCancel = { onAction(DayItineraryAction.DismissDialogs) },
                )
            },
        )
    }
}
