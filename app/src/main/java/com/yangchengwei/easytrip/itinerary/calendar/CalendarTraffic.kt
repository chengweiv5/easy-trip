package com.yangchengwei.easytrip.itinerary.calendar

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yangchengwei.easytrip.core.model.TransportMode

internal fun CalendarTransfer.modeLabel() = when (mode) {
    TransportMode.WALK -> "步行"
    TransportMode.DRIVE -> "驾车"
    TransportMode.TAXI -> "打车"
    TransportMode.TRANSIT -> "公交"
    null -> "交通"
}

internal fun CalendarTransfer.summary(compact: Boolean = false) =
    "${modeLabel()} · ${minutes?.let { "约$it${if (compact) "分" else "分钟"}" } ?: "耗时待定"}"

internal fun CalendarTransfer.description(): String = buildString {
    append("$fromName → $toName · ${modeLabel()} · ")
    append(minutes?.let { "预计约 $it 分钟" } ?: message ?: "交通用时待计算")
    if (continuation || continues) append("（全程）")
    allocatedMinutes?.let { append(" · 预留 $it 分钟") }
    if (blockStart != null && blockEnd != null) {
        append(" · 本段 ${calendarTime(blockStart)}–${calendarTime(blockEnd)}")
        if (continuation || continues) append(" · 本段预留 ${blockEnd - blockStart} 分钟")
    } else if (start == null) append(" · 时间待设，仅供参考")
    if (conflict) append(" · 时间不足")
}

internal val trafficTextStyle = TextStyle(
    fontSize = 10.sp, lineHeight = 13.sp, platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None, LineHeightStyle.Mode.Fixed),
)

private fun trafficPaint(density: Density) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    textSize = with(density) { 10.sp.toPx() }
}

/** Measure visible glyphs, then place the label in the free space between visits. */
private fun CalendarTransfer.trafficLayout(
    width: Dp, compact: Boolean, density: Density, events: List<CalendarEvent>,
    transfers: List<CalendarTransfer>,
): CalendarTrafficLayout? {
    val label = summary(compact)
    val paint = trafficPaint(density)
    val ink = Rect().also { paint.getTextBounds(label, 0, label.length, it) }
    return with(density) {
        if (paint.measureText(label) > (width - 14.dp).toPx()) return null
        layoutInGap(events, transfers, (ink.height() + 1.dp.toPx()) / CALENDAR_MINUTE_DP.dp.toPx())
    }
}

internal fun trafficInkSize(label: String, density: Density): androidx.compose.ui.geometry.Size {
    val paint = trafficPaint(density)
    val ink = Rect().also { paint.getTextBounds(label, 0, label.length, it) }
    return androidx.compose.ui.geometry.Size(paint.measureText(label), ink.height().toFloat())
}

@Composable
internal fun TrafficLine(label: String, modifier: Modifier = Modifier.fillMaxSize()) {
    val paint = trafficPaint(LocalDensity.current).apply { color = MaterialTheme.colorScheme.primary.toArgb() }
    val ink = Rect().also { paint.getTextBounds(label, 0, label.length, it) }
    Canvas(modifier.semantics { text = AnnotatedString(label) }) {
        drawContext.canvas.nativeCanvas.drawText(label, 7.dp.toPx(), (size.height - ink.height()) / 2 - ink.top, paint)
    }
}

@Composable
internal fun CalendarTraffic(
    dayId: String, transfers: List<CalendarTransfer>, events: List<CalendarEvent>, compact: Boolean,
    modifier: Modifier, enabled: Boolean, onOpen: (CalendarTransfer) -> Unit,
) {
    BoxWithConstraints(modifier) {
        val density = LocalDensity.current
        transfers.forEach { transfer ->
            val layout = transfer.trafficLayout(maxWidth, compact, density, events, transfers) ?: return@forEach
            val shape = RoundedCornerShape(6.dp)
            Box(Modifier.offset(y = (layout.start * CALENDAR_MINUTE_DP).dp).fillMaxWidth()
                .height(((layout.end - layout.start) * CALENDAR_MINUTE_DP).dp)
                .testTag("calendar-traffic-$dayId-${transfer.legId}")
                .semantics(mergeDescendants = true) { contentDescription = transfer.description() }
                .clickable(enabled = enabled) { onOpen(transfer) }) {
                Box(Modifier.fillMaxWidth().height(((layout.intervalEnd - layout.start) * CALENDAR_MINUTE_DP).dp)
                    .testTag("calendar-traffic-interval-$dayId-${transfer.legId}")
                    .clip(shape).background(Color(0xFFEAF5F2))
                    .border(BorderStroke(.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = .4f)), shape))
                TrafficLine(transfer.summary(compact), Modifier
                    .offset(y = ((layout.labelStart - layout.start) * CALENDAR_MINUTE_DP).dp)
                    .fillMaxWidth().height(with(density) { (trafficInkSize(transfer.summary(compact), density).height + 1.dp.toPx()).toDp() }))
            }
        }
    }
}
