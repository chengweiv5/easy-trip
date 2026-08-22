package com.yangchengwei.easytrip.place.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp


data class ScrollbarThumb(
    val offsetFraction: Float,
    val sizeFraction: Float,
)

fun calculateScrollbarThumb(
    totalItems: Int,
    visibleItems: Int,
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffsetPx: Int,
    firstVisibleItemSizePx: Int,
    contentOverflows: Boolean = visibleItems < totalItems,
    minimumSizeFraction: Float = 0.08f,
): ScrollbarThumb? {
    if (totalItems <= 0 || !contentOverflows) return null
    val sizeFraction = (visibleItems.toFloat() / totalItems)
        .coerceAtLeast(minimumSizeFraction)
        .coerceIn(0f, 1f)
    val itemProgress = if (firstVisibleItemSizePx > 0) {
        firstVisibleItemScrollOffsetPx.toFloat() / firstVisibleItemSizePx
    } else 0f
    val scrollableItems = totalItems - visibleItems
    val offsetFraction = ((firstVisibleItemIndex + itemProgress) / scrollableItems)
        .coerceIn(0f, 1f)
    return ScrollbarThumb(offsetFraction, sizeFraction)
}

@Composable
fun ReadOnlyLazyScrollbar(
    state: LazyListState,
    modifier: Modifier = Modifier,
) {
    val layoutInfo = state.layoutInfo
    val visibleItems = layoutInfo.visibleItemsInfo
    val firstVisible = visibleItems.firstOrNull()
    val thumb = calculateScrollbarThumb(
        totalItems = layoutInfo.totalItemsCount,
        visibleItems = visibleItems.size,
        firstVisibleItemIndex = firstVisible?.index ?: 0,
        firstVisibleItemScrollOffsetPx = state.firstVisibleItemScrollOffset,
        firstVisibleItemSizePx = firstVisible?.size ?: 0,
        contentOverflows = state.canScrollBackward || state.canScrollForward,
    ) ?: return

    BoxWithConstraints(modifier.fillMaxHeight().padding(vertical = 4.dp, horizontal = 2.dp)) {
        val thumbHeight = maxHeight * thumb.sizeFraction
        val travel = maxHeight - thumbHeight
        androidx.compose.foundation.layout.Box(
            Modifier
                .offset(y = travel * thumb.offsetFraction)
                .width(4.dp)
                .height(thumbHeight)
                .background(
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    RoundedCornerShape(2.dp),
                )
                .testTag("place-pool-scrollbar-thumb"),
        )
    }
}
