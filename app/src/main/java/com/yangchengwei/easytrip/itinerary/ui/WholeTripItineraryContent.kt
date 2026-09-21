package com.yangchengwei.easytrip.itinerary.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.ui.component.CompactSecondaryButton as TextButton
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.yangchengwei.easytrip.core.ui.component.EmptyState
import com.yangchengwei.easytrip.workspace.routeColorForDay
import com.yangchengwei.easytrip.workspace.routePalette

@Composable
fun WholeTripItineraryContent(
    days: List<WholeTripDayUi>,
    onAddDay: () -> Unit = {},
    modifier: Modifier = Modifier,
    startDate: LocalDate? = null,
) {
    val orderedDays = days.sortedBy { it.dayNumber }
    val orderOffsets = orderedDays.associate { day -> day.dayId to orderedDays.takeWhile { it.dayId != day.dayId }.sumOf { it.items.size } }
    val totalStops = days.sumOf { it.items.size }
    val itemsById = orderedDays.flatMap { it.items }.associateBy { it.id }
    Column(modifier.fillMaxSize()) {
        if (days.isNotEmpty()) {
            ItinerarySummaryHeader(
                text = "全程 · ${days.size} 天 · $totalStops 站",
                modifier = Modifier.testTag("whole-trip-summary"),
            )
        }
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().testTag("whole-trip-timeline"),
            contentPadding = PaddingValues.Zero,
        ) {
            if (days.isEmpty()) {
                item {
                    EmptyState(
                        title = "暂无旅行日",
                        message = "新增旅行日，开始规划行程",
                        emptyIllustration = com.yangchengwei.easytrip.core.ui.component.EmptyIllustration.Itinerary,
                        verticalPadding = 16.dp,
                        action = {
                            TextButton(onAddDay, Modifier.testTag("whole-trip-add-day")) {
                                Text("新增旅行日")
                            }
                        },
                    )
                }
            }
            orderedDays.forEach { day ->
                item(key = "heading-${day.dayId}") {
                    ItinerarySummaryHeader(
                        text = itineraryDaySummary(day.dayNumber, day.items.size),
                        date = wholeTripDayDate(day.dayNumber, startDate),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = if (day.dayNumber == days.first().dayNumber) 0.dp else 14.dp)
                            .testTag("whole-trip-day-${day.dayId}")
                            .testTag("whole-trip-day-summary-${day.dayId}"),
                    )
                }
                if (day.items.isEmpty()) {
                    item(key = "empty-${day.dayId}") {
                        Text(if (day.collapsedItemCount > 0) "连续重复地点已合并" else "暂无行程", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    day.items.forEachIndexed { index, itineraryItem ->
                        if (index == 0) day.legs.firstOrNull { it.toItemId == itineraryItem.id && day.items.none { item -> item.id == it.fromItemId } }?.let { leg ->
                            item(key = "${day.dayId}-leading-leg-${leg.id}") {
                                RouteLegContent(
                                    leg = leg,
                                    modifier = Modifier.fillMaxWidth().testTag("leg-${leg.id}"),
                                    fromPlaceName = itemsById[leg.fromItemId]?.name.orEmpty(),
                                    toPlaceName = itineraryItem.name,
                                    showEndpointText = true,
                                )
                            }
                        }
                        item(key = "${day.dayId}-item-${itineraryItem.id}") {
                            Column(Modifier.fillMaxWidth()) {
                                ItineraryPlaceRow(
                                    item = itineraryItem,
                                    displayOrder = orderOffsets.getValue(day.dayId) + index + 1,
                                    orderColor = Color(routeColorForDay((day.dayNumber - 1).coerceAtLeast(0))),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                        val nextItemId = day.items.getOrNull(index + 1)?.id
                        day.legs.firstOrNull {
                            it.fromItemId == itineraryItem.id && it.toItemId == nextItemId
                        }?.let { leg ->
                            item(key = "${day.dayId}-leg-${leg.id}") {
                                Column(Modifier.fillMaxWidth()) {
                                    RouteLegContent(
                                        leg = leg,
                                        modifier = Modifier.fillMaxWidth().testTag("leg-${leg.id}"),
                                        fromPlaceName = itineraryItem.name,
                                        toPlaceName = day.items[index + 1].name,
                                        showEndpointText = false,
                                        connectorInset = 0.dp,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (days.isNotEmpty()) {
                item(key = "whole-trip-bottom-spacer") {
                    Spacer(Modifier.height(24.dp).testTag("whole-trip-bottom-spacer"))
                }
            }
        }
    }
}

internal fun wholeTripDayColorIndex(dayNumber: Int): Int = (dayNumber - 1).coerceAtLeast(0) % routePalette().size

private val WholeTripDateFormatter = DateTimeFormatter.ofPattern("M 月 d 日")

internal fun wholeTripDayHeading(dayNumber: Int, startDate: LocalDate?): String =
    listOfNotNull(wholeTripDayDate(dayNumber, startDate), dayHeading(dayNumber)).joinToString(" · ")

internal fun wholeTripDayDate(dayNumber: Int, startDate: LocalDate?): String? =
    startDate?.plusDays((dayNumber - 1).coerceAtLeast(0).toLong())?.format(WholeTripDateFormatter)

internal fun dayHeading(dayNumber: Int): String = when (dayNumber) {
    1 -> "第一天"
    2 -> "第二天"
    3 -> "第三天"
    4 -> "第四天"
    5 -> "第五天"
    6 -> "第六天"
    7 -> "第七天"
    8 -> "第八天"
    9 -> "第九天"
    10 -> "第十天"
    else -> "第 $dayNumber 天"
}
