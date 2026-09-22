package com.yangchengwei.easytrip.itinerary.calendar

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.yangchengwei.easytrip.itinerary.domain.ItineraryTiming
import com.yangchengwei.easytrip.itinerary.domain.ItineraryTimingChange
import com.yangchengwei.easytrip.itinerary.ui.ItineraryItemUi
import com.yangchengwei.easytrip.itinerary.ui.ItinerarySummaryHeader
import com.yangchengwei.easytrip.itinerary.ui.WholeTripDayUi
import com.yangchengwei.easytrip.itinerary.ui.wholeTripDayDate
import com.yangchengwei.easytrip.workspace.ItineraryScope
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private data class CalendarDraft(
    val dayId: String, val item: ItineraryItemUi, val mode: CalendarDragMode,
    val pointerStart: Offset, val pointer: Offset, val grabOffset: Float,
    val scrollStart: Int, val timing: ItineraryTiming?, val keyboard: Boolean = false,
    val keyboardDelta: Float = 0f,
)
private fun ItineraryItemUi.timing() = ItineraryTiming(arrivalTime, stayMinutes)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarContent(
    rawDays: List<WholeTripDayUi>,
    selected: ItineraryScope,
    saveState: CalendarSaveState,
    focusItemId: String?,
    onToggle: () -> Unit,
    onFocus: (String, String?) -> Unit,
    onEdit: (String, String) -> Unit,
    onAdd: () -> Unit,
    onSave: (ItineraryTimingChange) -> Unit,
    onUndo: () -> Unit,
    onRetry: () -> Unit,
    onDismissMessage: () -> Unit,
    onBusy: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    startDate: LocalDate? = null,
    onRoute: (String) -> Unit = {},
) {
    val days = remember(rawDays) { projectCalendarDays(rawDays) }
    val single = (selected as? ItineraryScope.Day)?.dayId
    var page by rememberSaveable { mutableIntStateOf(0) }
    var detail by remember { mutableStateOf<Pair<String, String>?>(null) }
    var expandedGroup by remember(selected) { mutableStateOf<Int?>(null) }
    var pendingGroup by remember { mutableStateOf<Pair<String, String>?>(null) }
    var localMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(single, pendingGroup) {
        pendingGroup?.takeIf { it.first == single }?.let { (_, itemId) ->
            expandedGroup = days.firstOrNull { it.dayId == single }?.events?.firstOrNull { it.item.id == itemId }?.group
            pendingGroup = null
        }
    }
    var draft by remember { mutableStateOf<CalendarDraft?>(null) }
    LaunchedEffect(selected) { draft = null; detail = null }
    val scrollPositions = rememberSaveable(saver = mapSaver(
        save = { values: MutableMap<String, Int> -> values.toMap() },
        restore = { values -> values.mapValues { it.value as Int }.toMutableMap() },
    )) { mutableMapOf<String, Int>() }
    val scopeKey = single ?: "whole"
    val scroll = rememberScrollState()
    val density = LocalDensity.current
    val pxPerMinute = with(density) { CALENDAR_HOUR_DP.dp.toPx() } / 60f
    var viewport by remember { mutableStateOf(Rect.Zero) }
    var stableViewportTop by remember { mutableStateOf<Float?>(null) }
    var viewportScope by remember { mutableStateOf(scopeKey) }
    LaunchedEffect(viewport, scopeKey) {
        if (viewport.height > 0f) {
            val previous = stableViewportTop
            if (previous != null && viewportScope == scopeKey && draft == null) scroll.scrollBy(viewport.top - previous)
            stableViewportTop = viewport.top
            viewportScope = scopeKey
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    val busy = draft != null || saveState.saving
    LaunchedEffect(busy) { onBusy(busy) }
    DisposableEffect(Unit) { onDispose { onBusy(false) } }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) draft = null }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(rawDays) {
        draft?.let { active ->
            val now = rawDays.firstOrNull { it.dayId == active.dayId }?.items?.firstOrNull { it.id == active.item.id }
            if (now == null || now.timing() != active.item.timing()) draft = null
        }
    }
    LaunchedEffect(scopeKey, days.isEmpty()) {
        val visible = if (single == null) days else days.filter { it.dayId == single }
        val first = visible.flatMap { it.events }.minOfOrNull { it.start }
        val initial = first?.let { ((it / 60 - 1) * 60).coerceAtLeast(0) } ?: 480
        scroll.scrollTo(scrollPositions[scopeKey] ?: (initial * pxPerMinute).roundToInt())
    }
    LaunchedEffect(scopeKey, scroll) { snapshotFlow { scroll.value }.collect { scrollPositions[scopeKey] = it } }
    LaunchedEffect(focusItemId, single) {
        if (single != null && focusItemId != null) {
            val event = days.firstOrNull { it.dayId == single }?.events?.firstOrNull { it.item.id == focusItemId }
            if (event != null) scroll.scrollTo(((event.start - 30).coerceAtLeast(0) * pxPerMinute).roundToInt())
        }
    }
    fun candidate(active: CalendarDraft): ItineraryTiming? {
        val delta = if (active.keyboard) active.keyboardDelta else if (active.mode == CalendarDragMode.PLACE) {
            (active.pointer.y - viewport.top + scroll.value - active.grabOffset) / pxPerMinute
        } else (active.pointer.y - active.pointerStart.y + scroll.value - active.scrollStart) / pxPerMinute
        return candidateTiming(active.item.timing(), active.mode, delta)
    }
    fun move(pointer: Offset) { draft?.let { old -> val next = old.copy(pointer = pointer); draft = next.copy(timing = candidate(next)) } }
    fun finish(cancel: Boolean) {
        val active = draft ?: return
        val timing = active.timing
        val validDrop = active.keyboard || viewport.contains(active.pointer) && active.pointer.x >= viewport.left + with(density) { 36.dp.toPx() }
        if (!cancel && validDrop && timing != null && timing != active.item.timing()) {
            onSave(ItineraryTimingChange("", active.dayId, active.item.id, active.item.timing(), timing))
        }
        draft = null
    }
    val edgeSize = with(density) { 32.dp.toPx() }
    val direction = draft?.takeIf { !it.keyboard && it.pointer.x >= viewport.left && it.pointer.x <= viewport.right }?.let {
        when { it.pointer.y < viewport.top + edgeSize && it.pointer.y >= viewport.top -> -1
            it.pointer.y > viewport.bottom - edgeSize && it.pointer.y <= viewport.bottom -> 1
            else -> 0 }
    } ?: 0
    LaunchedEffect(direction, draft?.item?.id) {
        if (direction != 0) {
            delay(300)
            while (draft != null) {
                scroll.scrollBy(direction * with(density) { 8.dp.toPx() })
                draft?.let { move(it.pointer) }
                delay(32)
            }
        }
    }
    BackHandler(enabled = draft != null || detail != null || saveState.saving) { if (draft != null) draft = null else if (!saveState.saving) detail = null }
    val open: (String, String) -> Unit = { dayId, itemId ->
        if (!busy) {
            if (single == null || single != dayId) onFocus(dayId, itemId)
            else detail = dayId to itemId
        }
    }
    fun start(dayId: String, item: ItineraryItemUi, mode: CalendarDragMode, point: Offset, grab: Float) {
        if (saveState.saving || single != dayId) return
        val active = CalendarDraft(dayId, item, mode, point, point, grab, scroll.value, item.timing())
        draft = active.copy(timing = candidate(active))
    }
    fun step(dayId: String, item: ItineraryItemUi, mode: CalendarDragMode, delta: Int) {
        if (busy || single != dayId) return
        val next = candidateTiming(item.timing(), mode, delta.toFloat()) ?: return
        if (next != item.timing()) onSave(ItineraryTimingChange("", dayId, item.id, item.timing(), next))
    }
    fun keyStart(dayId: String, item: ItineraryItemUi) {
        if (saveState.saving || single != dayId) return
        val pending = item.arrivalTime == null
        val active = CalendarDraft(dayId, item, if (pending) CalendarDragMode.PLACE else CalendarDragMode.MOVE,
            Offset.Zero, Offset.Zero, 0f, scroll.value, item.timing(), true,
            if (pending) (scroll.value / pxPerMinute / 30).roundToInt() * 30f else 0f)
        draft = active.copy(timing = candidate(active))
    }
    fun keyStep(delta: Int) { draft?.let { val next = it.copy(keyboardDelta = it.keyboardDelta + delta); draft = next.copy(timing = candidate(next)) } }
    val selectedDay = days.firstOrNull { it.dayId == single }
    Column(modifier.fillMaxSize().testTag("calendar-content")) {
        ItinerarySummaryHeader(
            text = if (single == null) "全程 · ${days.size} 天" else "第 ${selectedDay?.number ?: 1} 天 · ${selectedDay?.sourceItems?.size ?: 0} 站",
            date = selectedDay?.let { wholeTripDayDate(it.number, startDate) },
            trailingAction = {
                Row {
                    CalendarToggle(true, onToggle, !busy)
                    if (single != null) IconButton(onAdd, enabled = !busy, modifier = Modifier.size(48.dp).testTag("add-places-to-selected-day")) { Icon(Icons.Rounded.Add, "从地点池添加地点", Modifier.size(18.dp)) }
                }
            },
        )
        localMessage?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
        if (saveState.message != null) {
            Row(Modifier.fillMaxWidth().testTag("calendar-save-message"), verticalAlignment = Alignment.CenterVertically) {
                Text(saveState.message, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
                if (saveState.undo != null) TextButton(onUndo, enabled = !busy) { Text("撤销") }
                if (saveState.retry != null) TextButton(onRetry, enabled = !busy) { Text("重试") }
                TextButton(onDismissMessage, enabled = !busy, contentPadding = PaddingValues(4.dp)) { Text("关闭") }
            }
        }
        if (saveState.saving) LinearProgressIndicator(Modifier.fillMaxWidth().testTag("calendar-saving"))
        BoxWithConstraints(Modifier.weight(1f)) {
            val hasTraffic = days.any { day -> day.transfers.any { it.blockStart != null } }
            val minTwoColumnWidth = if (hasTraffic) 360.dp else 250.dp
            val columns = if (maxWidth < minTwoColumnWidth || density.fontScale > 1.2f) 1 else 2
            val lastPage = (days.size - columns).coerceAtLeast(0)
            val currentPage = page.coerceIn(0, lastPage)
            val shown = if (single != null) listOfNotNull(selectedDay) else days.drop(currentPage).take(columns)
            Column(Modifier.fillMaxSize()) {
                if (single == null) {
                    Row(Modifier.fillMaxWidth().testTag("calendar-date-pager"), verticalAlignment = Alignment.CenterVertically) {
                        TextButton({ page = (currentPage - columns).coerceAtLeast(0) }, enabled = currentPage > 0 && !busy, contentPadding = PaddingValues.Zero, modifier = Modifier.width(48.dp)) { Text("‹") }
                        Text("${shown.firstOrNull()?.number ?: 0}–${shown.lastOrNull()?.number ?: 0} / ${days.size} 天", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                        TextButton({ page = (currentPage + columns).coerceAtMost(lastPage) }, enabled = currentPage < lastPage && !busy, contentPadding = PaddingValues.Zero, modifier = Modifier.width(48.dp)) { Text("›") }
                    }
                    Row(Modifier.fillMaxWidth().padding(start = 36.dp)) {
                        shown.forEach { day ->
                            TextButton({ onFocus(day.dayId, null) }, enabled = !busy, modifier = Modifier.weight(1f).testTag("calendar-date-${day.dayId}"), contentPadding = PaddingValues(2.dp)) {
                                Text("第 ${day.number} 天\n${wholeTripDayDate(day.number, startDate) ?: "日期待定"}", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
                if (shown.any { it.pending.isNotEmpty() }) {
                    Text("待安排 · ${shown.sumOf { it.pending.size }}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(vertical = 4.dp))
                    if (single != null) {
                        LazyRow(Modifier.fillMaxWidth().height(56.dp).testTag("calendar-pending"), horizontalArrangement = Arrangement.spacedBy(6.dp), userScrollEnabled = !busy) {
                            items(selectedDay?.pending.orEmpty(), key = { it.id }) { item ->
                                val editable = !saveState.saving && (item.stayMinutes == null || item.stayMinutes in 1..1440)
                                CalendarCard(item.name, "到达待设 · ${item.stayMinutes?.let { "停留 $it 分钟" } ?: "停留待设"}",
                                    Modifier.width(190.dp).height(52.dp).testTag("calendar-pending-${item.id}"), dashed = true, pending = true, editable = editable,
                                    onClick = { open(single, item.id) },
                                    onStart = { mode, point, grab -> start(single, item, mode, point, grab) }, onMove = { move(it) }, onEnd = { finish(it) },
                                    onKeyboardStart = { keyStart(single, item) }, onKeyboardStep = { keyStep(it) }, onKeyboardEnd = { finish(it) })
                            }
                        }
                    } else Row(Modifier.fillMaxWidth().padding(start = 36.dp)) {
                        shown.forEach { day ->
                            Column(Modifier.weight(1f).heightIn(max = 112.dp).verticalScroll(rememberScrollState())) {
                                day.pending.forEach { item -> CalendarCard(item.name, "到达待设", Modifier.fillMaxWidth().height(52.dp).padding(2.dp).testTag("calendar-pending-${item.id}"), dashed = true, onClick = { open(day.dayId, item.id) }) }
                            }
                        }
                    }
                }
                val active = draft
                val saving = saveState.active
                val savingItem = rawDays.firstOrNull { it.dayId == saving?.dayId }?.items?.firstOrNull { it.id == saving?.itemId }
                val visibleTiming = active?.timing ?: saving?.after
                val visibleItem = active?.item ?: savingItem
                val previewDays = if (visibleTiming == null || visibleItem == null) shown else {
                    val updated = rawDays.map { day -> day.copy(items = day.items.map { item ->
                        if (item.id == visibleItem.id) item.copy(arrivalTime = visibleTiming.arrivalTime, stayMinutes = visibleTiming.stayMinutes) else item
                    }) }
                    val projected = projectCalendarDays(updated).associateBy { it.dayId }
                    shown.map { it.copy(transfers = projected[it.dayId]?.transfers.orEmpty()) }
                }
                val dragLabel = when (active?.mode) {
                    CalendarDragMode.START -> "调整开始 · "
                    CalendarDragMode.END -> "调整结束 · "
                    else -> ""
                }
                Text(visibleTiming?.let { "$dragLabel${it.arrivalTime} → ${it.stayMinutes?.let { stay -> calendarEndLabel(requireNotNull(visibleItem).copy(arrivalTime = it.arrivalTime, stayMinutes = stay)) }} · ${it.stayMinutes} 分钟" } ?: if (active == null) "" else "拖入当天时间轴设置时间",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.height(24.dp).testTag("calendar-draft-label"))
                Box(Modifier.weight(1f).fillMaxWidth().onGloballyPositioned { viewport = it.boundsInRoot() }.testTag("calendar-viewport")) {
                    val swipe = if (single == null) Modifier.pointerInput(currentPage, columns, lastPage) {
                        var distance = 0f
                        detectHorizontalDragGestures(onDragStart = { distance = 0f }, onDragEnd = {
                            if (distance < -40.dp.toPx()) page = (currentPage + columns).coerceAtMost(lastPage)
                            if (distance > 40.dp.toPx()) page = (currentPage - columns).coerceAtLeast(0)
                        }) { change, amount -> change.consume(); distance += amount }
                    } else Modifier
                    key(selected) {
                    CalendarGrid(previewDays, single, expandedGroup, focusItemId,
                        draftItem = visibleItem, draftDayId = active?.dayId ?: saving?.dayId, draftTiming = visibleTiming,
                        draftMode = active?.mode,
                        modifier = Modifier.fillMaxSize().then(swipe).verticalScroll(scroll, enabled = draft == null),
                        editable = !saveState.saving,
                        onOpen = open, onExpand = { expandedGroup = if (expandedGroup == it) null else it },
                        onOpenGroup = { dayId, itemId -> pendingGroup = dayId to itemId; onFocus(dayId, itemId) },
                        onStart = { d, i, m, p, g -> start(d, i, m, p, g) }, onMove = { move(it) }, onEnd = { finish(it) }, onStep = { d, i, m, n -> step(d, i, m, n) },
                        onKeyboardStart = { d, i -> keyStart(d, i) }, onKeyboardStep = { keyStep(it) }, onKeyboardEnd = { finish(it) },
                        onTraffic = { transfer ->
                            if (!busy) {
                                if (single == transfer.sourceDayId) onRoute(transfer.legId)
                                else onFocus(transfer.sourceDayId, transfer.fromId)
                            }
                        })
                    }
                }
                // Short events retain their truthful grid height; a separate 48dp target opens precise editing.
                val compactEvents = shown.flatMap { it.events }.filter { it.point || !it.placeholder && (it.end - it.start) * CALENDAR_MINUTE_DP < 48 }
                if (compactEvents.isNotEmpty()) LazyRow(Modifier.height(48.dp).fillMaxWidth()) {
                    items(compactEvents, key = { it.key }) { event ->
                        CalendarCard(event.item.name, "${calendarTime(event.start)} · ${if(event.point) "停留0分钟" else "停留${event.item.stayMinutes}分钟"}",
                            Modifier.width(170.dp).height(48.dp).testTag("calendar-short-${event.item.id}"),
                            editable = event.point && single == event.sourceDayId && !saveState.saving,
                            onClick = { open(event.sourceDayId, event.item.id) },
                            onStart = { mode, point, grab -> start(event.sourceDayId, event.item, mode, point, grab) }, onMove = { move(it) }, onEnd = { finish(it) },
                            onStep = { mode, delta -> step(event.sourceDayId, event.item, mode, delta) },
                            onKeyboardStart = { keyStart(event.sourceDayId, event.item) }, onKeyboardStep = { keyStep(it) }, onKeyboardEnd = { finish(it) })
                    }
                }
            }
        }
    }
    expandedGroup?.let { group ->
        val members = selectedDay?.events?.filter { it.group == group }.orEmpty()
        if (members.isNotEmpty()) ModalBottomSheet(onDismissRequest = { expandedGroup = null }) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)) {
                Text("${members.size} 项交叠日程", style = MaterialTheme.typography.titleMedium)
                members.forEach { event ->
                    TextButton({ expandedGroup = null; open(event.sourceDayId, event.item.id) }, Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text("${event.order}. ${event.item.name} · ${calendarTime(event.start)}–${calendarTime(event.end)}")
                    }
                }
            }
        }
    }
    detail?.let { (dayId, itemId) ->
        val day = days.firstOrNull { it.dayId == dayId }
        val item = day?.sourceItems?.firstOrNull { it.id == itemId }
        if (item == null) LaunchedEffect(detail, rawDays) {
            val target = days.firstOrNull { candidate -> candidate.sourceItems.any { it.id == itemId } }
            detail = null
            if (target != null) onFocus(target.dayId, itemId) else localMessage = "该日程已删除"
        }
        else CalendarDetail(day, item, startDate,
            outgoing = days.flatMap { it.transfers }.filter { it.sourceDayId == dayId && it.fromId == itemId }
                .groupBy { it.sourceDayId to it.legId }.values
                .map { fragments -> fragments.first().copy(conflict = fragments.any { it.conflict }) },
            onDismiss = { detail = null }, onEdit = { detail = null; onEdit(dayId, itemId) },
            onRoute = { legId -> detail = null; onRoute(legId) })
    }
}
