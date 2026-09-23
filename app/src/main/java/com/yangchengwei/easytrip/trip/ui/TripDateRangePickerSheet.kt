package com.yangchengwei.easytrip.trip.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.onClick as semanticOnClick
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.ui.component.EasyTripPrimaryButton
import com.yangchengwei.easytrip.core.ui.component.EasyTripSecondaryButton
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val WeekdayLabels = listOf("一", "二", "三", "四", "五", "六", "日")
private val MonthFormatter = DateTimeFormatter.ofPattern("yyyy年M月", Locale.CHINA)

@Composable
fun TripDateRangePickerModalHost(
    sheetVisible: Boolean,
    background: @Composable () -> Unit,
    sheet: @Composable () -> Unit,
) = Box(Modifier.fillMaxSize()) {
    Box(Modifier.fillMaxSize().then(if (sheetVisible) Modifier.clearAndSetSemantics {} else Modifier)) {
        background()
    }
    if (sheetVisible) sheet()
}

@Composable
fun TripDateRangePickerSheet(
    initialSelection: DateRangeSelection?,
    initialDisplayedMonth: YearMonth? = null,
    onConfirm: (DateRangeSelection) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: WindowInsets = WindowInsets.navigationBars,
    title: String = "选择出行日期",
    confirmLabel: String = "确认",
    fixedDayCount: Int? = null,
) = BoxWithConstraints(modifier.fillMaxSize()) {
    var selection by remember(initialSelection, fixedDayCount) {
        mutableStateOf(initialSelection.fixedToDayCount(fixedDayCount))
    }
    var displayedMonth by remember(initialSelection, initialDisplayedMonth, fixedDayCount) {
        mutableStateOf(initialSelection?.startDate?.let(YearMonth::from) ?: initialDisplayedMonth ?: YearMonth.from(LocalDate.now()))
    }
    var confirmDispatched by remember(initialSelection, fixedDayCount) { mutableStateOf(false) }
    val sheetHeight = tripDateRangePickerSheetHeight(maxHeight)
    val currentSelection = selection.fixedToDayCount(fixedDayCount)
    val dismiss = { if (!confirmDispatched) onDismiss() }

    BackHandler(enabled = !confirmDispatched, onBack = dismiss)
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(confirmDispatched) {
                if (!confirmDispatched) detectTapGestures(onTap = { dismiss() })
            }
            .testTag("trip-date-range-scrim")
            .semantics {
                contentDescription = "关闭日期范围选择"
                if (!confirmDispatched) semanticOnClick(label = "关闭日期范围选择") {
                    dismiss()
                    true
                }
            },
        color = Color.Black.copy(alpha = .35f),
    ) {}
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(sheetHeight)
            .align(Alignment.BottomCenter)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) awaitPointerEvent(PointerEventPass.Initial)
                }
            }
            .testTag("trip-date-range-sheet")
            .semantics {
                paneTitle = title
                isTraversalGroup = true
            },
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 12.dp,
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                text = title,
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Column(
                modifier = Modifier.weight(1f, fill = true).verticalScroll(rememberScrollState()).padding(top = 10.dp)
                    .testTag("trip-date-range-calendar"),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DateRangeSummary(currentSelection)
                MonthNavigation(
                    month = displayedMonth,
                    onPrevious = { if (displayedMonth > YearMonth.from(LocalDate.MIN)) displayedMonth = displayedMonth.minusMonths(1) },
                    onNext = { if (displayedMonth < YearMonth.from(LocalDate.MAX)) displayedMonth = displayedMonth.plusMonths(1) },
                )
                CalendarGrid(
                    month = displayedMonth,
                    selection = currentSelection,
                    onDateClick = { date ->
                        selection = if (fixedDayCount == null) {
                            reduceDateRangeSelection(selection, date)
                        } else {
                            val endDate = runCatching { date.plusDays(fixedDayCount.toLong() - 1L) }.getOrNull()
                            if (endDate == null) DateRangeSelection(startDate = date) else DateRangeSelection(date, endDate)
                        }
                    },
                )
                currentSelection.validationError?.let {
                    Text(
                        text = it,
                        modifier = Modifier.testTag("trip-date-range-error"),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().windowInsetsPadding(bottomInset).padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                EasyTripSecondaryButton(
                    onClick = dismiss,
                    modifier = Modifier.weight(1f).testTag("trip-date-range-cancel"),
                    enabled = !confirmDispatched,
                ) { Text("取消") }
                EasyTripPrimaryButton(
                    onClick = {
                        if (!confirmDispatched) {
                            confirmDispatched = true
                            onConfirm(currentSelection)
                        }
                    },
                    modifier = Modifier.weight(1f).testTag("trip-date-range-confirm"),
                    enabled = currentSelection.isConfirmable && !confirmDispatched,
                ) { Text(confirmLabel) }
            }
        }
    }
}

private fun DateRangeSelection?.fixedToDayCount(fixedDayCount: Int?): DateRangeSelection {
    val selection = this ?: DateRangeSelection()
    val start = selection.startDate ?: return selection
    if (fixedDayCount == null) return selection
    val end = runCatching { start.plusDays(fixedDayCount.toLong() - 1L) }.getOrNull()
    return if (end == null) DateRangeSelection(startDate = start) else DateRangeSelection(start, end)
}

internal fun tripDateRangePickerSheetHeight(availableHeight: Dp): Dp {
    val standardHeight = availableHeight * (660f / 782f)
    return standardHeight.coerceAtLeast(240.dp).coerceAtMost(660.dp).coerceAtMost(availableHeight)
}

@Composable
private fun DateRangeSummary(selection: DateRangeSelection, modifier: Modifier = Modifier) = Surface(
    modifier = modifier.fillMaxWidth().testTag("trip-date-range-summary"),
    color = MaterialTheme.colorScheme.background,
    shape = RoundedCornerShape(EasyTripTheme.sizes.settingsCardCornerRadius),
) {
    Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DateSummaryItem("开始日期", selection.startDate, Modifier.weight(1f))
            DateSummaryItem("结束日期", selection.endDate, Modifier.weight(1f))
        }
        selection.dayCount?.let { days ->
            Text("${selection.startDate} 至 ${selection.endDate} · ${days}天${days - 1}晚", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun DateSummaryItem(label: String, date: LocalDate?, modifier: Modifier = Modifier) = Column(modifier) {
    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(
        date?.toString() ?: "请选择",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun MonthNavigation(month: YearMonth, onPrevious: () -> Unit, onNext: () -> Unit) = Row(
    Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
) {
    DateRangeMonthButton(
        onClick = onPrevious,
        tag = "trip-date-range-previous-month",
        icon = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "上个月") },
    )
    Text(month.format(MonthFormatter), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    DateRangeMonthButton(
        onClick = onNext,
        tag = "trip-date-range-next-month",
        icon = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "下个月") },
    )
}

@Composable
private fun DateRangeMonthButton(onClick: () -> Unit, tag: String, icon: @Composable () -> Unit) = Surface(
    modifier = Modifier.size(40.dp).testTag(tag).clickable(onClick = onClick),
    shape = CircleShape,
    color = MaterialTheme.colorScheme.surfaceVariant,
) { Box(contentAlignment = Alignment.Center) { icon() } }

@Composable
private fun CalendarGrid(
    month: YearMonth,
    selection: DateRangeSelection,
    onDateClick: (LocalDate) -> Unit,
) = Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Row(Modifier.fillMaxWidth()) {
        WeekdayLabels.forEach { weekday ->
            Text(
                weekday,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    calendarMonth(month).days.chunked(DAYS_IN_WEEK).forEach { week ->
        Row(Modifier.fillMaxWidth()) {
            week.forEach { date ->
                DateCell(
                    date = date,
                    inDisplayedMonth = YearMonth.from(date) == month,
                    selectionState = selection.daySelectionState(date),
                    onClick = { onDateClick(date) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun DateCell(
    date: LocalDate,
    inDisplayedMonth: Boolean,
    selectionState: DateSelectionState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedEndpoint = selectionState in setOf(DateSelectionState.START, DateSelectionState.END, DateSelectionState.SINGLE)
    val inRange = selectionState == DateSelectionState.IN_RANGE
    val background = when {
        selectedEndpoint -> MaterialTheme.colorScheme.primary
        inRange -> MaterialTheme.colorScheme.primary.copy(alpha = .16f)
        else -> Color.Transparent
    }
    val contentColor = when {
        selectedEndpoint -> MaterialTheme.colorScheme.onPrimary
        inDisplayedMonth -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .55f)
    }
    val stateLabel = when (selectionState) {
        DateSelectionState.START -> "开始日期"
        DateSelectionState.END -> "结束日期"
        DateSelectionState.SINGLE -> "开始和结束日期"
        DateSelectionState.IN_RANGE -> "范围内日期"
        DateSelectionState.UNSELECTED -> "未选择"
    }
    val isToday = date == LocalDate.now()
    val todayLabel = if (isToday) "，今天" else ""
    val cellShape = if (selectedEndpoint) CircleShape else RoundedCornerShape(0.dp)
    Box(
        modifier = modifier
            .wrapContentHeight()
            .padding(vertical = 2.dp)
            .height(40.dp)
            .background(background, cellShape)
            .then(
                if (isToday) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.tertiary, cellShape)
                } else {
                    Modifier
                },
            )
            .testTag("trip-date-$date")
            .semantics { contentDescription = "$date，$stateLabel$todayLabel" }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(date.dayOfMonth.toString(), color = contentColor, style = MaterialTheme.typography.bodyMedium)
    }
}
