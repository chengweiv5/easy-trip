package com.yangchengwei.easytrip.itinerary.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yangchengwei.easytrip.itinerary.domain.ItineraryTiming
import kotlin.math.roundToInt

internal enum class CalendarHintPosition { Floating, Summary, Wide }

/** Root coordinates keep feedback attached to the finger even while the grid auto-scrolls. */
@Composable
internal fun CalendarResizeHint(
    mode: CalendarDragMode,
    timing: ItineraryTiming,
    pointer: Offset,
    viewport: Rect,
    summary: Rect,
    wideArea: Rect,
    origin: Offset,
    onPosition: (CalendarHintPosition) -> Unit,
) {
    val start = mode == CalendarDragMode.START
    val arrival = timing.arrivalTime ?: return
    val startMinutes = arrival.hour * 60 + arrival.minute
    val duration = timing.stayMinutes ?: 60
    val endMinutes = startMinutes + duration
    fun label(minutes: Int): String = if (minutes <= 1440) calendarTime(minutes)
        else "次日 ${calendarTime(minutes - 1440)}"
    val action = if (start) "调整开始" else "调整结束"
    val value = label(if (start) startMinutes else endMinutes)
    val other = if (start) "结束 ${label(endMinutes)} 不变" else "开始 ${label(startMinutes)} 不变"
    val durationLabel = if (timing.stayMinutes == null) "停留待设 · 预览 $duration 分钟" else "停留 $duration 分钟"
    val subtitle = "$other · $durationLabel"
    val density = LocalDensity.current
    val measure = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold,
        platformStyle = PlatformTextStyle(includeFontPadding = false))
    val detailStyle = TextStyle(fontSize = 10.sp, lineHeight = 13.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false))
    fun px(value: Int) = with(density) { value.dp.toPx() }
    fun timeStyle(floating: Boolean) = labelStyle.copy(fontSize = (if (floating) 21 else 19).sp,
        lineHeight = (if (floating) 24 else 22).sp)
    fun rowWidth(floating: Boolean) = measure.measure(action, labelStyle).size.width +
        measure.measure(value, timeStyle(floating)).size.width + px(16 + 14 + 22)
    fun rowHeight(floating: Boolean) = maxOf(measure.measure(action, labelStyle).size.height,
        measure.measure(value, timeStyle(floating)).size.height, px(16).roundToInt())
    fun detailHeight(width: Float) = measure.measure(subtitle, detailStyle,
        constraints = Constraints(maxWidth = (width - px(22)).roundToInt().coerceAtLeast(1))).size.height
    val floatWidth = maxOf(px(196), rowWidth(true)).coerceAtMost((viewport.width - px(16)).coerceAtLeast(1f))
    val floatHeight = maxOf(px(58), rowHeight(true) + detailHeight(floatWidth) + px(19))
    val floatTop = pointer.y - px(64) - floatHeight
    val fitsWidth = rowWidth(true) <= floatWidth
    var pinned by remember { mutableStateOf(false) }
    val shouldPin = !fitsWidth || floatTop < viewport.top + px(if (pinned) 24 else 8)
    SideEffect { if (pinned != shouldPin) pinned = shouldPin }
    val fixedHeight = maxOf(px(48), rowHeight(false) + detailHeight(summary.width) + px(10))
    val summaryFits = rowWidth(false) <= summary.width && fixedHeight <= summary.height + 1f
    val position = when { !shouldPin -> CalendarHintPosition.Floating
        summaryFits -> CalendarHintPosition.Summary
        else -> CalendarHintPosition.Wide }
    SideEffect { onPosition(position) }
    val floating = position == CalendarHintPosition.Floating
    val wide = position == CalendarHintPosition.Wide
    val area = if (wide) wideArea else summary
    val width = if (floating) floatWidth else area.width.coerceAtLeast(1f)
    // Extremely large text can wrap the main row; never shrink system text to fit.
    val stacked = !floating && rowWidth(false) > width
    val height = when {
        floating -> floatHeight
        !wide -> summary.height
        stacked -> measure.measure(action, labelStyle).size.height +
            measure.measure(value, timeStyle(false)).size.height + px(20)
        else -> rowHeight(false) + px(16)
    }
    val left = if (floating) (pointer.x - width / 2).coerceIn(viewport.left + px(8),
        (viewport.right - px(8) - width).coerceAtLeast(viewport.left + px(8))) else area.left
    val top = if (floating) floatTop else area.top
    val shape = RoundedCornerShape(8.dp)
    Column(Modifier.offset { IntOffset((left - origin.x).roundToInt(), (top - origin.y).roundToInt()) }
        .wrapContentSize(Alignment.TopStart, unbounded = true)
        .requiredSize(with(density) { width.toDp() }, with(density) { height.toDp() })
        .shadow(if (floating) 3.dp else 0.dp, shape)
        .background(MaterialTheme.colorScheme.primary, shape)
        .testTag(if (start) "calendar-resize-start" else "calendar-resize-end")
        .semantics(mergeDescendants = true) { contentDescription = "$action $value，$subtitle"; stateDescription = position.name }
        .padding(horizontal = 11.dp, vertical = if (floating || wide) 8.dp else 5.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        val color = MaterialTheme.colorScheme.onPrimary
        if (stacked) {
            Text("${if (start) "↑" else "↓"} $action", style = labelStyle, color = color)
            Text(value, style = timeStyle(false), color = color)
        } else Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(if (start) "↑" else "↓", Modifier.width(16.dp), color = color, fontSize = 16.sp)
            Spacer(Modifier.width(7.dp))
            Text(action, style = labelStyle, color = color)
            Spacer(Modifier.weight(1f).widthIn(min = 7.dp))
            Text(value, style = timeStyle(floating), color = color)
        }
        if (!wide) {
            if (floating) Spacer(Modifier.height(3.dp))
            Text(subtitle, style = detailStyle, color = color.copy(alpha = .9f))
        }
    }
}
