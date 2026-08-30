package com.yangchengwei.easytrip.itinerary.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.ui.component.CompactSecondaryButton as TextButton
import com.yangchengwei.easytrip.core.ui.component.EmptyState

@Composable
fun WholeTripItineraryContent(
    days: List<WholeTripDayUi>,
    onAddDay: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.testTag("whole-trip-timeline"),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
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
        days.forEach { day ->
            item(key = "heading-${day.dayId}") {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .testTag("whole-trip-day-${day.dayId}")
                        .semantics { heading() },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .background(dayColors[wholeTripDayColorIndex(day.dayNumber)])
                            .testTag("whole-trip-day-marker-${day.dayId}"),
                    )
                    Text(
                        text = dayHeading(day.dayNumber),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = "第 ${day.dayNumber} 日",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }
            if (day.items.isEmpty()) {
                item(key = "empty-${day.dayId}") { Text("暂无行程") }
            } else {
                day.items.forEachIndexed { index, itineraryItem ->
                    item(key = "${day.dayId}-item-${itineraryItem.id}") {
                        ItineraryPlaceRow(
                            item = itineraryItem,
                            displayOrder = index + 1,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    val nextItemId = day.items.getOrNull(index + 1)?.id
                    day.legs.firstOrNull {
                        it.fromItemId == itineraryItem.id && it.toItemId == nextItemId
                    }?.let { leg ->
                        item(key = "${day.dayId}-leg-${leg.id}") {
                            RouteLegContent(
                                leg = leg,
                                modifier = Modifier.fillMaxWidth().testTag("leg-${leg.id}"),
                            )
                        }
                    }
                }
            }
        }
    }
}

private val dayColors = listOf(
    Color(0xFF2D5E3A),
    Color(0xFF9A5B13),
    Color(0xFF35678B),
    Color(0xFF8A4260),
    Color(0xFF5E528B),
)

internal fun wholeTripDayColorIndex(dayNumber: Int): Int = (dayNumber - 1).coerceAtLeast(0) % dayColors.size

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
