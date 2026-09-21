package com.yangchengwei.easytrip.itinerary.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.yangchengwei.easytrip.workspace.routeColorForDay

@Composable
fun ItineraryPlaceRow(
    item: ItineraryItemUi,
    displayOrder: Int,
    modifier: Modifier = Modifier,
    orderColor: Color = Color(routeColorForDay(0)),
) = ItineraryPlaceContent(item, displayOrder, modifier, compactTimeline = true, orderColor = orderColor)
