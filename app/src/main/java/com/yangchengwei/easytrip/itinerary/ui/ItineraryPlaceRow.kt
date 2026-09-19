package com.yangchengwei.easytrip.itinerary.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ItineraryPlaceRow(
    item: ItineraryItemUi,
    displayOrder: Int,
    modifier: Modifier = Modifier,
) = ItineraryPlaceContent(item, displayOrder, modifier, compactTimeline = true)
