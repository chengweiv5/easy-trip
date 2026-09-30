package com.yangchengwei.easytrip.itinerary.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.trip.domain.TripDay
import com.yangchengwei.easytrip.workspace.ItineraryScope
import java.time.LocalDate
import kotlin.math.abs

@Composable
fun ItineraryScopeRail(
    days: List<TripDay>,
    selected: ItineraryScope,
    onSelect: (ItineraryScope) -> Unit,
    onAddDay: () -> Unit = {},
    modifier: Modifier = Modifier,
    startDate: LocalDate? = null,
    onMoveDay: (String, Int) -> Unit = { _, _ -> },
    reorderEnabled: Boolean = true,
) {
    val ordered = days.sortedBy(TripDay::index)
    val list = rememberLazyListState()
    val density = LocalDensity.current
    val step = with(density) { 58.dp.toPx() }
    val edge = with(density) { 40.dp.toPx() }
    val scrollStep = with(density) { 6.dp.toPx() }
    var dragged by remember { mutableStateOf<TripDay?>(null) }
    var dragTop by remember { mutableFloatStateOf(0f) }
    var target by remember { mutableIntStateOf(0) }
    val currentMove by rememberUpdatedState(onMoveDay)
    val updateTarget by rememberUpdatedState(newValue = {
        val center = dragTop + step / 2f
        list.layoutInfo.visibleItemsInfo.filter { info -> ordered.any { it.id == info.key } }
            .minByOrNull { abs(it.offset + it.size / 2f - center) }?.let { info ->
                target = ordered.indexOfFirst { it.id == info.key }
            }
    })
    LaunchedEffect(dragged?.id) {
        while (dragged != null) {
            withFrameNanos { }
            val center = dragTop + step / 2f
            val scroll = when {
                center < list.layoutInfo.viewportStartOffset + edge -> -scrollStep
                center > list.layoutInfo.viewportEndOffset - edge -> scrollStep
                else -> 0f
            }
            if (scroll != 0f) { list.scrollBy(scroll); updateTarget() }
        }
    }
    Box(modifier.width(64.dp).fillMaxHeight().pointerInput(ordered.map { it.id }, reorderEnabled) {
        if (!reorderEnabled || ordered.size < 2) return@pointerInput
        detectDragGesturesAfterLongPress(
            onDragStart = { position ->
                val info = list.layoutInfo.visibleItemsInfo.firstOrNull {
                    position.y >= it.offset && position.y <= it.offset + it.size && ordered.any { day -> day.id == it.key }
                }
                dragged = ordered.firstOrNull { it.id == info?.key }
                if (dragged != null) { dragTop = info!!.offset.toFloat(); target = dragged!!.index }
            },
            onDrag = { change, amount ->
                if (dragged != null) { change.consume(); dragTop += amount.y; updateTarget() }
            },
            onDragEnd = { dragged?.let { if (it.index != target) currentMove(it.id, target) }; dragged = null },
            onDragCancel = { dragged = null },
        )
    }) {
        LazyColumn(
            Modifier.fillMaxSize().testTag("itinerary-scope-rail").selectableGroup(),
            state = list,
            userScrollEnabled = dragged == null,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item(key = "WHOLE_TRIP") {
                ScopeItem("全程", selected == ItineraryScope.WholeTrip, "itinerary-scope-WHOLE_TRIP", { onSelect(ItineraryScope.WholeTrip) })
            }
            items(ordered, key = TripDay::id) { day ->
                val source = dragged?.index
                val shift = when {
                    source == null -> 0f
                    day.index in (source + 1)..target -> -step
                    day.index in target until source -> step
                    else -> 0f
                }
                DayScopeItem(day, selected, startDate, { onSelect(ItineraryScope.Day(day.id)) },
                    Modifier.graphicsLayer { translationY = shift; alpha = if (dragged?.id == day.id) 0f else 1f })
            }
            item(key = "ADD_DAY") {
                ScopeItem("＋ 添加", false, "itinerary-add-day", onAddDay, role = Role.Button)
            }
        }
        dragged?.let { day ->
            DayScopeItem(day, selected, startDate, {},
                Modifier.graphicsLayer { translationY = dragTop; shadowElevation = 4.dp.toPx(); alpha = .94f },
                tag = "dragging-day-${day.id}")
        }
    }
}

@Composable
private fun DayScopeItem(day: TripDay, selected: ItineraryScope, startDate: LocalDate?, onClick: () -> Unit,
    modifier: Modifier = Modifier, tag: String = "itinerary-scope-${day.id}") {
    val date = startDate?.plusDays(day.index.toLong())
    ScopeItem("第 ${day.index + 1} 天", selected == ItineraryScope.Day(day.id), tag, onClick,
        subtitle = date?.let { "${it.monthValue}/${it.dayOfMonth}" }, isDay = true, modifier = modifier)
}

@Composable
private fun ScopeItem(
    label: String, selected: Boolean, tag: String, onClick: () -> Unit,
    subtitle: String? = null, isDay: Boolean = false, role: Role = Role.Tab,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().height(if (isDay) 52.dp else 48.dp)
            .testTag(tag).selectable(selected = selected, role = role, onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 1) }
        }
    }
}
