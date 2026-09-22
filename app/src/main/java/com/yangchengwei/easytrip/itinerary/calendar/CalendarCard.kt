package com.yangchengwei.easytrip.itinerary.calendar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min

@Composable
fun CalendarToggle(selected: Boolean, onClick: () -> Unit, enabled: Boolean = true) {
    IconButton(onClick, enabled = enabled, modifier = Modifier.size(48.dp).testTag("calendar-toggle").semantics {
        contentDescription = "日历视图"
        this.selected = selected
    }) {
        val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        Canvas(Modifier.size(18.dp)) {
            val w = size.width; val h = size.height; val line = 1.5.dp.toPx()
            drawRoundRect(color, Offset(w * .1f, h * .17f), androidx.compose.ui.geometry.Size(w * .8f, h * .75f), CornerRadius(2.dp.toPx()), style = Stroke(line))
            drawLine(color, Offset(w * .1f, h * .39f), Offset(w * .9f, h * .39f), line)
            listOf(.32f, .68f).forEach { x -> drawLine(color, Offset(w*x, 0f), Offset(w*x, h*.26f), line) }
            listOf(.32f, .55f, .75f).forEach { x -> drawCircle(color, w*.035f, Offset(w*x,h*.59f)) }
        }
    }
}

/** Coordinates always use the root, so scrolling/reflow never changes the finger's anchor. */
@Composable
internal fun CalendarCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    dashed: Boolean = false,
    conflict: Boolean = false,
    editable: Boolean = false,
    edges: Boolean = false,
    pending: Boolean = false,
    selected: Boolean = false,
    compact: Boolean = false,
    accent: Color? = null,
    incoming: CalendarTransfer? = null,
    narrowTraffic: Boolean = false,
    incomingTag: String = "calendar-incoming",
    contentTopPadding: androidx.compose.ui.unit.Dp? = null,
    onClick: () -> Unit,
    onStart: (CalendarDragMode, Offset, Float) -> Unit = { _, _, _ -> },
    onMove: (Offset) -> Unit = {},
    onEnd: (Boolean) -> Unit = {},
    onStep: (CalendarDragMode, Int) -> Unit = { _, _ -> },
    onKeyboardStart: () -> Unit = {},
    onKeyboardStep: (Int) -> Unit = {},
    onKeyboardEnd: (Boolean) -> Unit = {},
) {
    var origin by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val latestStart by rememberUpdatedState(onStart)
    val latestMove by rememberUpdatedState(onMove)
    val latestEnd by rememberUpdatedState(onEnd)
    val latestClick by rememberUpdatedState(onClick)
    val border = when { conflict -> Color(0xFFBA5B37); dashed -> Color(0xFF758B90); else -> (accent ?: MaterialTheme.colorScheme.primary).copy(alpha = .7f) }
    val surface = if (dashed) Color(0xFFF3F6F6) else (accent ?: MaterialTheme.colorScheme.primary).copy(alpha = if (selected) .16f else .09f)
    var keyboardActive by remember { mutableStateOf(false) }
    Box(modifier.onGloballyPositioned { origin = it.positionInRoot() }
        .clip(RoundedCornerShape(6.dp)).background(surface)
        .semantics(mergeDescendants = true) {
            contentDescription = "$title，$subtitle${if (conflict) "，时间重叠" else ""}${incoming?.let { "，${it.description()}" }.orEmpty()}"
            onClick("查看日程详情") { latestClick(); true }
            if (editable) customActions = buildList {
                add(CustomAccessibilityAction("精确编辑时间") { latestClick(); true })
                if (!pending) {
                    add(CustomAccessibilityAction("后移30分钟") { onStep(CalendarDragMode.MOVE, 30); true })
                    add(CustomAccessibilityAction("前移30分钟") { onStep(CalendarDragMode.MOVE, -30); true })
                    if (edges) {
                        add(CustomAccessibilityAction("开始提前30分钟") { onStep(CalendarDragMode.START, -30); true })
                        add(CustomAccessibilityAction("开始延后30分钟") { onStep(CalendarDragMode.START, 30); true })
                        add(CustomAccessibilityAction("结束提前30分钟") { onStep(CalendarDragMode.END, -30); true })
                        add(CustomAccessibilityAction("结束延后30分钟") { onStep(CalendarDragMode.END, 30); true })
                    }
                }
            }
        }
        .onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) false else when (event.key) {
                Key.Spacebar -> { if (editable) { keyboardActive = true; onKeyboardStart() } else latestClick(); true }
                Key.DirectionUp -> if (keyboardActive) { onKeyboardStep(-30); true } else false
                Key.DirectionDown -> if (keyboardActive) { onKeyboardStep(30); true } else false
                Key.Enter -> { if (keyboardActive) { onKeyboardEnd(false); keyboardActive = false } else latestClick(); true }
                Key.Escape -> if (keyboardActive) { onKeyboardEnd(true); keyboardActive = false; true } else false
                else -> false
            }
        }.focusable()
        .pointerInput(editable, edges, pending) {
            val slop = with(density) { 8.dp.toPx() }
            val edgeMax = with(density) { 12.dp.toPx() }
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val edge = min(edgeMax, size.height * .25f)
                val mode = when {
                    !editable -> null
                    pending -> CalendarDragMode.PLACE
                    edges && down.position.y <= edge -> CalendarDragMode.START
                    edges && down.position.y >= size.height - edge -> CalendarDragMode.END
                    else -> CalendarDragMode.MOVE
                }
                var active = false
                var complete = false
                var last = down.position
                try {
                    // Edges follow the same long-press gate as body/pending cards. Until then,
                    // leave pointer events unconsumed so a swipe belongs to the calendar scroll.
                    var abandoned = false
                    val ended = withTimeoutOrNull(500L) {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            last = change.position
                            if (event.changes.count { it.pressed } > 1 || change.isConsumed || (last-down.position).getDistance() > slop) {
                                abandoned = true; break
                            }
                            if (!change.pressed) { latestClick(); complete = true; break }
                        }
                        true
                    }
                    if (ended == null && mode != null && !abandoned) {
                        active = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        latestStart(mode, origin + down.position, down.position.y)
                    } else return@awaitEachGesture
                    while (active) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (event.changes.count { it.pressed } > 1 || change.isConsumed) break
                        last = change.position
                        latestMove(origin + last)
                        change.consume()
                        if (!change.pressed) { latestEnd(false); complete = true; break }
                    }
                } finally {
                    if (active && !complete) latestEnd(true)
                }
            }
        }) {
        Canvas(Modifier.matchParentSize()) {
            drawRoundRect(border, cornerRadius = CornerRadius(6.dp.toPx()), style = Stroke(
                width = if (selected || conflict) 1.5.dp.toPx() else 1.dp.toPx(),
                pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 3.dp.toPx())) else null,
            ))
        }
        CalendarCardText(title, subtitle, conflict, compact, incoming, narrowTraffic, incomingTag, contentTopPadding)

    }
}

/** Shared by static visits and the moving preview to keep line visibility identical. */
@Composable
internal fun CalendarCardText(
    title: String, subtitle: String, conflict: Boolean, compact: Boolean,
    incoming: CalendarTransfer?, narrowTraffic: Boolean, incomingTag: String,
    contentTopPadding: androidx.compose.ui.unit.Dp? = null,
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val titleStyle = TextStyle(fontSize = 12.sp, lineHeight = 15.sp, platformStyle = PlatformTextStyle(includeFontPadding = false))
        val subtitleStyle = trafficTextStyle
        val incomingText = incoming?.summary(narrowTraffic)
        val trafficSize = incomingText?.let { trafficInkSize(it, density) }
        val titleHeight = textMeasurer.measure(title, titleStyle, maxLines = 1, softWrap = false).size.height
        val subtitleHeight = textMeasurer.measure(subtitle, subtitleStyle, maxLines = 1, softWrap = false).size.height
        val showIncoming = !compact && trafficSize != null && with(density) {
            trafficSize.width <= (maxWidth - 14.dp).toPx() &&
                titleHeight + subtitleHeight + trafficSize.height + (5.dp + (contentTopPadding ?: 2.dp) - 2.dp).toPx() <= maxHeight.toPx()
        }
        Column(Modifier.fillMaxWidth().padding(start = 7.dp, end = 7.dp,
            top = contentTopPadding ?: if (compact || showIncoming) 2.dp else 6.dp,
            bottom = if (compact || showIncoming) 2.dp else 6.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, style = titleStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!compact) Text(subtitle, color = if (conflict) Color(0xFF9F4527) else MaterialTheme.colorScheme.onSurfaceVariant,
                style = subtitleStyle, maxLines = if (showIncoming) 1 else 2, overflow = TextOverflow.Ellipsis)
            if (showIncoming) {
                HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = .2f))
                TrafficLine(requireNotNull(incomingText), Modifier.offset(x = (-7).dp).fillMaxWidth()
                    .height(with(density) { requireNotNull(trafficSize).height.toDp() }).testTag(incomingTag))
            }
        }
    }
}
