package com.yangchengwei.easytrip.core.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

/** Shared three-stroke grip for itinerary places and trip settings. */
@Composable
fun DragHandleIcon(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier.size(18.dp)) {
        listOf(-5.dp, 0.dp, 5.dp).forEach { offset ->
            val y = size.height / 2f + offset.toPx()
            drawLine(color, Offset(4.dp.toPx(), y), Offset(size.width - 4.dp.toPx(), y), 2.dp.toPx(), StrokeCap.Round)
        }
    }
}
