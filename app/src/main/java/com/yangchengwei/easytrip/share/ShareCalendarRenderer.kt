package com.yangchengwei.easytrip.share

import android.graphics.Color
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Text measurement and drawing use identical fonts so no label is clipped to fit a time slot. */
internal interface ShareCalendarDrawing {
    fun text(value: String, x: Float, y: Float, width: Float, size: Float, color: Int, bold: Boolean = false, draw: Boolean = true): Float
    fun rect(x: Float, y: Float, width: Float, height: Float, color: Int)
    fun outline(x: Float, y: Float, width: Float, height: Float, color: Int, dashed: Boolean = false)
    fun checkpoint(bottom: Float)
}

internal class ShareCalendarPainter(private val canvas: ShareCalendarDrawing) {
    private val ink = Color.rgb(32, 52, 59)
    private val muted = Color.rgb(83, 103, 109)
    private val line = Color.rgb(204, 221, 224)
    private val soft = Color.rgb(245, 248, 248)
    private val traffic = Color.rgb(64, 87, 95)
    private val trafficBg = Color.rgb(228, 235, 237)
    private val warning = Color.rgb(138, 66, 30)
    private val pxPerMinute = .8f
    private data class Detail(val title: String, val body: String, val color: Int)
    private data class VisitGroup(val visits: MutableList<ShareCalendarVisit>, var end: Int)

    private fun text(value: String, x: Float, y: Float, width: Float, size: Float = 10f, color: Int = ink, bold: Boolean = false, draw: Boolean = true) =
        canvas.text(value, x, y, width, size, color, bold, draw)
    private fun tint(color: Int) = Color.rgb(
        (Color.red(color) * .09 + 255 * .91).toInt(),
        (Color.green(color) * .09 + 255 * .91).toInt(),
        (Color.blue(color) * .09 + 255 * .91).toInt(),
    )
    private fun visitLabel(v: ShareCalendarVisit) = "${if (v.continuation) "前日 " else ""}${v.stop.number}"
    private fun visitDetail(v: ShareCalendarVisit) = "第 ${v.source.index + 1} 天 · ${v.stop.number} ${v.stop.name}\n${shareVisitTime(v.stop)}" +
        (if (v.continuation) " · 前日延续" else "") + v.rangeNote.takeIf { it.isNotEmpty() }?.let { " · $it" }.orEmpty()

    fun draw(calendar: ShareCalendar, top: Float): Float {
        var y = top + 24
        y += text(if (calendar.singleDay) "当日日历" else "全程日历", 20f, y, 220f, 16f, bold = true) + 5
        y += text("彩色表示停留，浅灰表示交通；按同一时间轴查看", 20f, y, 350f, 10f, muted) + 16
        for ((groupIndex, days) in calendar.groups.withIndex()) {
            canvas.checkpoint(y)
            val width = (350f - 35 - (days.size - 1) * 3) / days.size
            fun column(i: Int) = 55f + i * (width + 3)
            val headerHeight = days.maxOf { d ->
                text("第 ${d.day.index + 1} 天", 0f, 0f, width - 8, 11f, d.day.color, true, false) +
                    text(dateLabel(d.day), 0f, 0f, width - 8, 9f, muted, draw = false) + 16
            }
            text("时间", 20f, y + 15, 30f, 9f, muted)
            days.forEachIndexed { i, d ->
                val x = column(i)
                canvas.rect(x, y, width, headerHeight, soft)
                canvas.rect(x, y, width, 3f, d.day.color)
                val h = text("第 ${d.day.index + 1} 天", x + 4, y + 9, width - 8, 11f, d.day.color, true)
                text(dateLabel(d.day), x + 4, y + 11 + h, width - 8, 9f, muted)
            }
            y += headerHeight + 10
            if (days.any { it.pending.isNotEmpty() }) {
                text("待安排", 20f, y + 4, 30f, 9f, muted)
                var bottom = y
                days.forEachIndexed { i, d ->
                    val x = column(i)
                    var row = y
                    if (d.pending.isEmpty()) text("—", x + 4, row + 4, width - 8, 10f, muted)
                    d.pending.forEach { stop ->
                        val value = "${stop.number} ${stop.name}\n${shareVisitTime(stop)}"
                        val height = text(value, x + 5, row + 5, width - 10, draw = false) + 10
                        canvas.checkpoint(row + height)
                        canvas.rect(x, row, width, height, soft)
                        canvas.outline(x, row, width, height, line, true)
                        text(value, x + 5, row + 5, width - 10)
                        row += height + 5
                    }
                    bottom = maxOf(bottom, row)
                }
                y = bottom + 14
            }
            val visitDetails = mutableListOf<Detail>()
            val trafficDetails = mutableListOf<Detail>()
            val gridTop = y + 7
            val gridHeight = (calendar.end - calendar.start) * pxPerMinute
            if (calendar.hasTimeline) {
                canvas.checkpoint(gridTop + gridHeight + 12)
                for (minute in calendar.start..calendar.end step 60) {
                    val row = gridTop + (minute - calendar.start) * pxPerMinute
                    text(if (minute == 1440) "24:00" else shareClock(minute.toLong()), 20f, row - 5, 31f, 9f, muted)
                    canvas.rect(55f, row, 315f, .6f, line)
                }
            }
            days.forEachIndexed { i, day ->
                val x = column(i)
                fun row(minute: Int) = gridTop + (minute - calendar.start) * pxPerMinute
                val descriptions = mutableSetOf<String>()
                fun transferDetail(t: ShareCalendarTransfer, reason: String = "") {
                    if (!descriptions.add(t.key)) return
                    val timing = when {
                        t.departure == null -> "出发时间待定"
                        t.arrival == null -> "${shareClock(t.departure)} 出发 · 用时待定"
                        else -> "${shareClock(t.departure)}—${shareClock(t.arrival)}"
                    }
                    val source = if (t.continuation) " · 前日延续（源第 ${t.source.index + 1} 天）" else ""
                    val title = "第 ${day.day.index + 1} 天$source · ${t.from.number}→${t.to.number} ${t.mode} ${t.minutes?.let { "约 $it 分钟" } ?: "· 用时待定"}"
                    val notes = listOf(if (t.conflict) "交通可能来不及" else reason, t.rangeNote).filter { it.isNotBlank() }
                    trafficDetails += Detail(title, "${t.from.name} → ${t.to.name}\n$timing" + if (notes.isEmpty()) "" else " · ${notes.joinToString(" · ")}", if (t.conflict) warning else traffic)
                }
                for (t in day.transfers) {
                    if (t.start == null || t.end == null) { transferDetail(t); continue }
                    val overlaps = day.visits.any { v -> v.start < t.end && t.start < v.end }
                    if (t.conflict || overlaps) { transferDetail(t, "时段与安排重叠"); continue }
                    val height = (t.end - t.start) * pxPerMinute
                    val label = "${t.mode} ${t.minutes}分"
                    val textHeight = text(label, 0f, 0f, width - 12, 8.5f, traffic, draw = false)
                    val next = (day.visits.filter { it.start >= t.end }.map { it.start } +
                        day.transfers.filter { it !== t }.mapNotNull { it.start?.takeIf { start -> start >= t.end } } + calendar.end).min()
                    val canLabelBelow = height < textHeight + 4 && (next - t.end) * pxPerMinute >= textHeight + 3
                    canvas.rect(x + 2, row(t.start), width - 4, maxOf(height, 1f), trafficBg)
                    canvas.rect(x + 2, row(t.start), 2f, maxOf(height, 1f), Color.rgb(112, 135, 142))
                    when {
                        height >= textHeight + 4 -> text(label, x + 6, row(t.start) + (height - textHeight) / 2, width - 12, 8.5f, traffic)
                        canLabelBelow -> text(label, x + 6, row(t.end) + 1, width - 12, 8.5f, traffic)
                        else -> transferDetail(t, "刻度较短，信息在此展开")
                    }
                    if (t.continuation || t.rangeNote.isNotEmpty()) transferDetail(t)
                }
                val groups = mutableListOf<VisitGroup>()
                day.visits.forEach { v ->
                    val previous = groups.lastOrNull()
                    if (previous != null && v.start < previous.end) {
                        previous.visits += v; previous.end = maxOf(previous.end, v.end)
                    } else groups += VisitGroup(mutableListOf(v), v.end)
                }
                for (group in groups) {
                    val visits = group.visits
                    val first = visits.first()
                    val height = (group.end - first.start) * pxPerMinute
                    val blockTop = row(first.start)
                    val realConflict = visits.any { a -> a.stop.stayMinutes != null && a.end > a.start && visits.any { b ->
                        a !== b && b.stop.stayMinutes != null && b.end > b.start && a.start < b.end && b.start < a.end
                    } }
                    if (visits.size > 1) {
                        val description = if (realConflict) "时间重叠" else "占位接近"
                        val color = if (realConflict) warning else muted
                        val label = "${visits.joinToString(" / ", transform = ::visitLabel)}\n${visits.size} 项$description"
                        canvas.rect(x + 2, blockTop, width - 4, maxOf(height, 1f), if (realConflict) Color.rgb(255, 243, 233) else soft)
                        canvas.outline(x + 2, blockTop, width - 4, maxOf(height, 1f), color, !realConflict)
                        if (text(label, 0f, 0f, width - 14, 9f, color, draw = false) + 8 <= height)
                            text(label, x + 7, blockTop + 4, width - 14, 9f, color)
                        visitDetails += Detail("第 ${day.day.index + 1} 天 · $description", visits.joinToString("\n", transform = ::visitDetail), color)
                        continue
                    }
                    val label = "${visitLabel(first)} ${first.stop.name}"
                    val timing = shareVisitTime(first.stop)
                    val labelHeight = text(label, 0f, 0f, width - 14, 10f, bold = true, draw = false)
                    val timingHeight = text(timing, 0f, 0f, width - 14, 8.5f, muted, draw = false)
                    val compact = first.stop.stayMinutes == null || first.continuation || first.rangeNote.isNotBlank() || labelHeight + timingHeight + 12 > height
                    canvas.rect(x + 2, blockTop, width - 4, maxOf(1f, height), if (first.stop.stayMinutes == null) soft else tint(first.source.color))
                    if (first.stop.stayMinutes == null) canvas.outline(x + 2, blockTop, width - 4, maxOf(1f, height), muted, true)
                    else canvas.rect(x + 2, blockTop, 3f, maxOf(1f, height), first.source.color)
                    if (!compact) {
                        text(label, x + 7, blockTop + 4, width - 14, 10f, bold = true)
                        text(timing, x + 7, blockTop + labelHeight + 7, width - 14, 8.5f, muted)
                    } else {
                        val number = visitLabel(first)
                        if (text(number, 0f, 0f, width - 14, 9f, draw = false) + 4 <= height)
                            text(number, x + 7, blockTop + 2, width - 14, 9f)
                        visitDetails += Detail("第 ${day.day.index + 1} 天${if (first.continuation) " · 前日延续" else ""}", visitDetail(first), first.source.color)
                    }
                }
                if (day.day.stops.isEmpty() && day.visits.isEmpty() && day.transfers.isEmpty())
                    text("暂无安排", x + 5, if (calendar.hasTimeline) gridTop + 14 else y, width - 10, 10f, muted)
            }
            y = if (calendar.hasTimeline) gridTop + gridHeight + 15 else y + 20
            val groupRange = if (days.size == 1) "第 ${days.first().day.index + 1} 天"
                else "第 ${days.first().day.index + 1}—${days.last().day.index + 1} 天"
            y += text(groupRange +
                (if (calendar.groups.size > 1) " · ${groupIndex + 1} / ${calendar.groups.size} 组" else ""), 240f, y, 130f, 9f, muted) + 12
            y = details("交通补充", trafficDetails, y)
            y = details("时间补充", visitDetails, y)
            y += 12
        }
        canvas.rect(20f, y, 3f, 10f, Color.rgb(8, 111, 118))
        text("停留时段", 27f, y, 85f, 9f, muted)
        canvas.rect(135f, y, 10f, 10f, trafficBg)
        text("交通用时参考", 150f, y, 100f, 9f, muted)
        y += 19
        y += text("数字对应当天清单 · 空白不代表可自由安排", 20f, y, 350f, 9f, muted) + 20
        canvas.rect(20f, y, 350f, 1f, line)
        canvas.checkpoint(y)
        return y
    }

    private fun details(title: String, rows: List<Detail>, top: Float): Float {
        if (rows.isEmpty()) return top
        var y = top + 8
        y += text(title, 30f, y, 330f, 11f, traffic, true) + 8
        for (row in rows) {
            val titleHeight = text(row.title, 0f, 0f, 326f, 10f, row.color, true, false)
            val bodyHeight = text(row.body, 0f, 0f, 326f, 10f, row.color, draw = false)
            val height = titleHeight + bodyHeight + 17
            canvas.checkpoint(y + height)
            canvas.rect(20f, y, 350f, height, soft)
            text(row.title, 32f, y + 6, 326f, 10f, row.color, true)
            text(row.body, 32f, y + 10 + titleHeight, 326f, 10f, row.color)
            y += height + 4
        }
        return y + 10
    }
    private fun dateLabel(day: ShareDay) = day.date?.format(DateTimeFormatter.ofPattern("M月d日 · EEE", Locale.CHINA)) ?: "日期待定"
}
