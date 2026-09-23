package com.yangchengwei.easytrip.itinerary.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import java.time.LocalDate
import com.yangchengwei.easytrip.core.ui.component.CompactSecondaryButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
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
    onToggleCalendar: (() -> Unit)? = null,
    onDeleteDay: (() -> Unit)? = null,
) {
    val orderColor = androidx.compose.ui.graphics.Color(
        com.yangchengwei.easytrip.workspace.routeColorForDay(
            state.days.firstOrNull { it.id == state.selectedDayId }?.index ?: 0,
        ),
    )
    Column(modifier) {
        if (state.selectedDayId != null) {
            val dayNumber = state.days.firstOrNull { it.id == state.selectedDayId }?.index?.plus(1)
            ItinerarySummaryHeader(
                text = itineraryDaySummary(dayNumber, state.items.size),
                modifier = Modifier.testTag("day-itinerary-summary"),
                date = dayNumber?.let { wholeTripDayDate(it, startDate) },
                trailingInset = 0.dp,
                trailingAction = {
                    ItineraryDayActions(
                        calendarSelected = false,
                        onToggleCalendar = onToggleCalendar,
                        onAdd = { onAction(DayItineraryAction.AddPlaces) },
                        onDelete = onDeleteDay,
                        canDelete = state.days.size > 1,
                    )
                },
            )
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (!state.isDayLoaded) {
            com.yangchengwei.easytrip.core.ui.component.DeferredLoading {
                Text("行程加载中", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp).testTag("day-itinerary-loading"))
            }
        }
        val displayItems = currentDisplayItems(state.items, state.previewOrder)
        val visibleLegs = visibleRouteLegs(state.items, state.previewOrder, state.legs)
        val timelineState = rememberLazyListState()
        val edgeAutoScrollPx = with(LocalDensity.current) { 48.dp.toPx() }
        val handleZonePx = with(LocalDensity.current) { 76.dp.toPx() }
        var dragState by remember(state.selectedDayId) { mutableStateOf<TimelineDragState?>(null) }
        var dragTop by remember { mutableFloatStateOf(0f) }
        var dragHeight by remember { mutableFloatStateOf(0f) }
        val updateTarget by rememberUpdatedState(newValue = {
            val drag = dragState
            if (drag != null) {
                val center = dragTop + dragHeight / 2f
                val targetInfo = timelineState.layoutInfo.visibleItemsInfo
                    .filter { info -> displayItems.any { it.id == info.key } }
                    .minByOrNull { kotlin.math.abs(it.offset + timelineState.layoutInfo.beforeContentPadding + it.size / 2f - center) }
                val target = displayItems.indexOfFirst { it.id == targetInfo?.key }
                if (target >= 0 && target != drag.targetIndex) {
                    dragState = drag.preview(target)
                    onAction(DayItineraryAction.PreviewMove(drag.itemId, target))
                }
            }
        })
        val beginDrag by rememberUpdatedState(newValue = { position: androidx.compose.ui.geometry.Offset ->
            val info = timelineState.layoutInfo.visibleItemsInfo.firstOrNull {
                position.y >= it.offset + timelineState.layoutInfo.beforeContentPadding && position.y <= it.offset + timelineState.layoutInfo.beforeContentPadding + it.size && displayItems.any { item -> item.id == it.key }
            }
            val id = info?.key as? String
            if (id != null) {
                dragState = TimelineDragState.start(id, state.items.indexOfFirst { it.id == id }, displayItems.indexOfFirst { it.id == id })
                dragTop = (info.offset + timelineState.layoutInfo.beforeContentPadding).toFloat()
                dragHeight = info.size.toFloat()
            }
        })
        val endDrag by rememberUpdatedState(newValue = { cancel: Boolean ->
            dragState?.let { drag ->
                if (cancel) onAction(DayItineraryAction.PreviewMove(drag.itemId, drag.originalIndex))
                else onAction(DayItineraryAction.CommitMove(drag.itemId, drag.targetIndex))
            }
            dragState = null
        })
        LaunchedEffect(dragState?.itemId) {
            while (dragState != null) {
                withFrameNanos { }
                val center = dragTop + dragHeight / 2f
                val info = timelineState.layoutInfo
                val scroll = when {
                    center < info.viewportStartOffset + edgeAutoScrollPx -> -12f
                    center > info.viewportEndOffset - edgeAutoScrollPx -> 12f
                    else -> 0f
                }
                if (scroll != 0f) {
                    timelineState.scrollBy(scroll)
                    updateTarget()
                }
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth().pointerInput(state.selectedDayId) {
            detectDragGesturesAfterLongPress(
                onDragStart = { if (it.x >= size.width - handleZonePx) beginDrag(it) },
                onDrag = { change, amount ->
                    if (dragState != null) { change.consume(); dragTop += amount.y; updateTarget() }
                },
                onDragEnd = { endDrag(false) },
                onDragCancel = { endDrag(true) },
            )
        }) {
            LazyColumn(
                state = timelineState,
                userScrollEnabled = dragState == null,
                modifier = Modifier.fillMaxWidth().testTag("day-itinerary-timeline"),
                contentPadding = PaddingValues(top = 6.dp, end = 0.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                if (state.isDayLoaded && state.items.isEmpty() && state.selectedDayId != null) {
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
                displayItems.forEachIndexed { index, item ->
                    item(key = item.id) {
                    val id = item.id
                    ItineraryItemRow(
                        item = item,
                        index = index,
                        count = displayItems.size,
                        orderColor = orderColor,
                        onPreview = { onAction(DayItineraryAction.PreviewMove(id, it)) },
                        onCommit = { onAction(DayItineraryAction.CommitMove(id, it)) },
                        modifier = Modifier.graphicsLayer { alpha = if (dragState?.itemId == id) 0f else 1f },
                        sharedDragEnabled = true,
                        parentHandlesDrag = true,
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
                    }
                    val id = item.id
                    val next = displayItems.getOrNull(index + 1)?.id
                    visibleLegs.firstOrNull { it.fromItemId == id && it.toItemId == next }?.let { leg ->
                        item(key = "route-${leg.id}") { RouteLegRow(
                            leg = leg,
                            fromPlaceName = displayItems[index].name,
                            toPlaceName = displayItems[index + 1].name,
                            showEndpointText = false,
                            onMode = { onAction(DayItineraryAction.RequestMode(leg.id)) },
                            onRetry = if (leg.state is RouteLegUiState.Failed) {
                                { onAction(DayItineraryAction.Retry(leg.id, leg.version)) }
                            } else {
                                null
                            },
                        ) }
                    }
                }
            }
            dragState?.let { drag ->
                displayItems.firstOrNull { it.id == drag.itemId }?.let { item ->
                    androidx.compose.material3.Surface(
                        Modifier.fillMaxWidth().offset { IntOffset(0, dragTop.roundToInt()) }.testTag("drag-overlay-${item.id}"),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 8.dp,
                    ) {
                        ItineraryItemRow(item, drag.targetIndex, displayItems.size, {}, {}, {},
                            sharedDragEnabled = true, parentHandlesDrag = true, orderColor = orderColor)
                    }
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
                        ) { Text(moveTargetDayLabel(day.index, startDate)) }
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
