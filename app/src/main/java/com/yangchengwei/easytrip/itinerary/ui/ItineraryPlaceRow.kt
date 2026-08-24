package com.yangchengwei.easytrip.itinerary.ui

import androidx.compose.runtime.Composable

@Composable
fun ItineraryPlaceRow(
    item: ItineraryItemUi,
    index: Int,
    count: Int,
    onPreview: (Int) -> Unit,
    onCommit: (Int) -> Unit,
    onTiming: () -> Unit,
    onCrossDay: () -> Unit,
    onDelete: () -> Unit,
) {
    ItineraryItemRow(
        item = item,
        index = index,
        count = count,
        onPreview = onPreview,
        onCommit = onCommit,
        onTiming = onTiming,
        onCrossDay = onCrossDay,
        onDelete = onDelete,
    )
}
