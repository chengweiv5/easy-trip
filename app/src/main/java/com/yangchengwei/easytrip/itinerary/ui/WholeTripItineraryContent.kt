package com.yangchengwei.easytrip.itinerary.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun WholeTripItineraryContent(
    days: List<WholeTripDayUi>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (days.isEmpty()) {
            item { Text("暂无旅行日") }
        }
        days.forEach { day ->
            item(key = "heading-${day.dayId}") {
                Text(
                    text = dayHeading(day.dayNumber),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("whole-trip-day-${day.dayId}")
                        .semantics { heading() },
                )
            }
            if (day.items.isEmpty()) {
                item(key = "empty-${day.dayId}") { Text("暂无行程") }
            } else {
                day.items.forEachIndexed { index, itineraryItem ->
                    item(key = "${day.dayId}-item-${itineraryItem.id}") {
                        ItineraryItemCard(
                            item = itineraryItem,
                            displayOrder = index + 1,
                            modifier = Modifier.fillMaxWidth().testTag("item-${itineraryItem.id}"),
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
