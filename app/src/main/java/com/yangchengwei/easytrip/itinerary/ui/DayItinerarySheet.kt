package com.yangchengwei.easytrip.itinerary.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import java.time.LocalDate
import com.yangchengwei.easytrip.core.ui.component.CompactSecondaryButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
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

internal data class TimelineMove(
    val itemId: String,
    val targetIndex: Int,
)

internal data class TimelineDragState(
    val itemId: String,
    val originalIndex: Int,
    val targetIndex: Int,
    val isCancelled: Boolean = false,
) {
    fun preview(targetIndex: Int): TimelineDragState = copy(targetIndex = targetIndex)

    fun cancel(): TimelineDragState = copy(isCancelled = true)

    fun commitMove(): TimelineMove? =
        if (isCancelled) null else TimelineMove(itemId, targetIndex)

    companion object {
        fun start(itemId: String, originalIndex: Int, initialTargetIndex: Int): TimelineDragState =
            TimelineDragState(itemId, originalIndex, initialTargetIndex)
    }
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
    startDate: LocalDate? = null,
    showDialogs: Boolean = true,
    canScheduleAgain: Boolean = false,
) {
    Column(modifier) {
        if (state.selectedDayId != null) {
            val dayNumber = state.days.firstOrNull { it.id == state.selectedDayId }?.index?.plus(1)
            ItinerarySummaryHeader(
                text = itineraryDaySummary(dayNumber, state.items.size),
                modifier = Modifier.testTag("day-itinerary-summary"),
                date = dayNumber?.let { wholeTripDayDate(it, startDate) },
                trailingInset = 0.dp,
                trailingAction = {
                    IconButton(
                        onClick = { onAction(DayItineraryAction.AddPlaces) },
                        modifier = Modifier.size(28.dp).testTag("add-places-to-selected-day"),
                    ) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = "从地点池添加地点",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                },
            )
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        val displayItems = currentDisplayItems(state.items, state.previewOrder)
        val visibleLegs = visibleRouteLegs(state.items, state.previewOrder, state.legs)
        val timelineState = rememberLazyListState()
        val dragScope = rememberCoroutineScope()
        val edgeAutoScrollPx = with(LocalDensity.current) { 56.dp.toPx() }
        var dragState by remember { mutableStateOf<TimelineDragState?>(null) }
        var dragTranslationY by remember { mutableFloatStateOf(0f) }
        LazyColumn(
            state = timelineState,
            modifier = Modifier.testTag("day-itinerary-timeline"),
            contentPadding = PaddingValues(top = 6.dp, end = 0.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            if (state.items.isEmpty() && state.selectedDayId != null) {
                val dayNumber = state.days.firstOrNull { it.id == state.selectedDayId }?.index?.plus(1)
                item {
                    EmptyState(
                        title = if (dayNumber == null) "暂无行程" else "第 $dayNumber 天 · 暂无行程",
                        message = "从地点池添加地点，开始安排这一天",
                        emptyIllustration = com.yangchengwei.easytrip.core.ui.component.EmptyIllustration.Itinerary,
                        verticalPadding = 16.dp,
                        action = null,
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
                    modifier = Modifier.animateItem(),
                    isDragging = dragState?.itemId == id,
                    dragTranslationY = if (dragState?.itemId == id) dragTranslationY else 0f,
                    sharedDragEnabled = true,
                    onDragStart = {
                        dragState = TimelineDragState.start(
                            itemId = id,
                            originalIndex = state.items.indexOfFirst { it.id == id },
                            initialTargetIndex = index,
                        )
                        dragTranslationY = 0f
                    },
                    onDragDelta = { delta ->
                        if (dragState?.itemId == id) {
                            dragTranslationY += delta
                            val draggedInfo = timelineState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == id }
                            if (draggedInfo != null) {
                                val draggedCenter = draggedInfo.offset + dragTranslationY + draggedInfo.size / 2f
                                val targetInfo = timelineState.layoutInfo.visibleItemsInfo
                                    .filter { it.key in displayItems.map(ItineraryItemUi::id) }
                                    .minByOrNull { kotlin.math.abs((it.offset + it.size / 2f) - draggedCenter) }
                                val targetId = targetInfo?.key as? String
                                val target = displayItems.indexOfFirst { it.id == targetId }
                                if (target >= 0 && target != dragState?.targetIndex) {
                                    dragState = dragState?.preview(target)
                                    dragTranslationY = 0f
                                    onAction(DayItineraryAction.PreviewMove(id, target))
                                }
                                val viewportStart = timelineState.layoutInfo.viewportStartOffset
                                val viewportEnd = timelineState.layoutInfo.viewportEndOffset
                                val scrollDelta = when {
                                    draggedCenter < viewportStart + edgeAutoScrollPx -> -20f
                                    draggedCenter > viewportEnd - edgeAutoScrollPx -> 20f
                                    else -> 0f
                                }
                                if (scrollDelta != 0f) dragScope.launch { timelineState.scrollBy(scrollDelta) }
                            }
                        }
                    },
                    onDragEnd = {
                        dragState?.takeIf { it.itemId == id }?.commitMove()?.let { move ->
                            onAction(DayItineraryAction.CommitMove(move.itemId, move.targetIndex))
                        }
                        dragState = null
                        dragTranslationY = 0f
                    },
                    onDragCancel = {
                        dragState?.takeIf { it.itemId == id }?.let { drag ->
                            onAction(DayItineraryAction.PreviewMove(id, drag.originalIndex))
                        }
                        dragState = null
                        dragTranslationY = 0f
                    },
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
                        showEndpointText = false,
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
