package com.yangchengwei.easytrip.expense.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yangchengwei.easytrip.core.ui.theme.LocalThemePalette
import com.yangchengwei.easytrip.core.ui.theme.ThemePalette
import com.yangchengwei.easytrip.expense.*
import java.math.BigDecimal
import java.time.YearMonth
import java.util.Locale

internal val expensePageColor: Color
    @Composable get() = if (LocalThemePalette.current == ThemePalette.LAKE) Color(0xFFE5EEF0)
    else lerp(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.primaryContainer, .28f)

internal val expenseBorderColor: Color
    @Composable get() = if (LocalThemePalette.current == ThemePalette.LAKE) Color(0xFFC7D8DC)
    else MaterialTheme.colorScheme.outline

internal enum class ExpenseGlyph { CALENDAR, CATEGORIES, TRIP, RECORDS, INFO, HISTORY }

/** Small vector marks: no emoji/font dependency, and no duplicated accessibility label. */
@Composable
internal fun ExpenseGlyphIcon(glyph: ExpenseGlyph, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier.size(18.dp)) {
        val unit = size.minDimension / 24f
        val stroke = Stroke(1.5f * unit, cap = StrokeCap.Round)
        fun line(x: Float, y: Float, x2: Float, y2: Float) =
            drawLine(color, Offset(x * unit, y * unit), Offset(x2 * unit, y2 * unit), stroke.width, StrokeCap.Round)
        when (glyph) {
            ExpenseGlyph.CALENDAR -> {
                drawRoundRect(color, Offset(4 * unit, 5 * unit), Size(16 * unit, 16 * unit), CornerRadius(2 * unit), style = stroke)
                line(4f, 10f, 20f, 10f); line(8f, 3f, 8f, 7f); line(16f, 3f, 16f, 7f)
                line(8f, 14f, 10f, 14f); line(14f, 14f, 16f, 14f); line(8f, 17f, 10f, 17f)
            }
            ExpenseGlyph.CATEGORIES -> {
                listOf(4f to 4f, 14f to 4f, 4f to 14f, 14f to 14f).forEach { (x, y) ->
                    drawRoundRect(color, Offset(x * unit, y * unit), Size(6 * unit, 6 * unit), CornerRadius(unit), style = stroke)
                }
            }
            ExpenseGlyph.TRIP -> {
                drawRoundRect(color, Offset(4 * unit, 7 * unit), Size(16 * unit, 14 * unit), CornerRadius(2 * unit), style = stroke)
                line(9f, 7f, 9f, 3f); line(9f, 3f, 15f, 3f); line(15f, 3f, 15f, 7f)
                line(8f, 11f, 8f, 17f); line(16f, 11f, 16f, 17f)
            }
            ExpenseGlyph.RECORDS -> {
                drawRoundRect(color, Offset(5 * unit, 3 * unit), Size(14 * unit, 18 * unit), CornerRadius(2 * unit), style = stroke)
                listOf(8f, 12f, 16f).forEach { line(8f, it, 16f, it) }
            }
            ExpenseGlyph.INFO, ExpenseGlyph.HISTORY -> {
                drawCircle(color, 9 * unit, center, style = stroke)
                if (glyph == ExpenseGlyph.INFO) { line(12f, 11f, 12f, 17f); line(12f, 7f, 12f, 7.2f) }
                else { line(12f, 6f, 12f, 12f); line(12f, 12f, 16f, 14f) }
            }
        }
    }
}

@Composable
internal fun ExpenseSection(
    title: String,
    glyph: ExpenseGlyph,
    tag: String,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().testTag(tag),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, expenseBorderColor),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(30.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center) { ExpenseGlyphIcon(glyph) }
                Text(title, Modifier.weight(1f).semantics { heading() }, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                action?.invoke()
            }
            HorizontalDivider(color = expenseBorderColor)
            content()
        }
    }
}

@Composable
internal fun ExpenseSummary(total: ExpenseTotal, period: ExpensePeriod) {
    Surface(color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${periodLabel(period)} · 已记录花费", style = MaterialTheme.typography.labelLarge)
            Text(if (total.records == 0) "—" else expenseMoney(total.cents),
                fontSize = 36.sp, lineHeight = 44.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.testTag("expense-total"))
            Text(if (total.records == 0) "尚无费用记录" else "${total.records}笔费用 · ${total.trips}次有记录旅行",
                style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
internal fun ReviewRow(
    title: String, total: ExpenseTotal, tag: String, detail: String? = null,
    fraction: Float? = null, onClick: () -> Unit,
) {
    val large = LocalDensity.current.fontScale >= 1.5f
    Column(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick)
        .testTag(tag).padding(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val amount = if (total.records == 0) "尚无记录" else expenseMoney(total.cents)
        if (large) {
            Text("$title  ›", fontWeight = FontWeight.Medium)
            Text(amount, fontWeight = FontWeight.SemiBold)
        } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f), fontWeight = FontWeight.Medium)
            Text(amount, fontWeight = FontWeight.SemiBold, color = if (total.records == 0) MaterialTheme.colorScheme.onSurfaceVariant else LocalContentColor.current)
            Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (total.records > 0) Text("${total.records}笔 · ${total.trips}次旅行" +
            (fraction?.let { " · ${String.format(Locale.CHINA, "%.1f%%", it * 100)}" } ?: ""),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (fraction != null) com.yangchengwei.easytrip.core.ui.component.ContinuousProgressBar(
            progress = fraction,
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth(),
        )
        detail?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
internal fun ExpenseMonthChart(scope: ExpenseScope, all: List<ExpenseRecord>, year: Int, onSelect: (YearMonth) -> Unit) {
    val months = (1..12).map { month ->
        YearMonth.of(year, month).let { it to reviewExpenses(scope.copy(period = ExpensePeriod.Month(it)).select(all), ExpensePeriod.All) }
    }
    val maximum = months.maxOf { it.second.cents }.coerceAtLeast(1)
    val columns = if (LocalDensity.current.fontScale >= 1.5f) 3 else 6
    Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        months.chunked(columns).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { (month, total) ->
                    Column(Modifier.weight(1f).clickable(role = Role.Button) { onSelect(month) }
                        .testTag("review-month-${month.monthValue}").padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.height(60.dp).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                            if (total.records > 0) Box(Modifier.width(20.dp)
                                .height((total.cents.toDouble() / maximum * 56).coerceAtLeast(3.0).dp)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(3.dp)))
                        }
                        Text("${month.monthValue}月", style = MaterialTheme.typography.bodySmall)
                        Text(if (total.records == 0) "—" else BigDecimal.valueOf(total.cents, 2).stripTrailingZeros().toPlainString(),
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Text("单位：元 · — 表示尚无记录，点击月份查看", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun ExpenseUndatedNotice(total: ExpenseTotal, includedInTotal: Boolean, onClick: () -> Unit) {
    Surface(Modifier.fillMaxWidth().testTag("expense-undated-notice"), shape = RoundedCornerShape(18.dp),
        color = Color(0xFFFFF6E6), contentColor = Color(0xFF79521E), border = BorderStroke(1.dp, Color(0xFFE5D1AC))) {
        Box(Modifier.padding(horizontal = 16.dp)) {
            ReviewRow("未确定日期", total, "review-undated",
                detail = if (includedInTotal) "包含在全部金额中，尚未归入确定年月" else "单列，未计入任何确定年月", onClick = onClick)
        }
    }
}

internal fun periodLabel(period: ExpensePeriod): String = when (period) {
    ExpensePeriod.All -> "全部"
    ExpensePeriod.Undated -> "未确定日期"
    is ExpensePeriod.Year -> "${period.value}年"
    is ExpensePeriod.Month -> "${period.value.year}年${period.value.monthValue}月"
}
