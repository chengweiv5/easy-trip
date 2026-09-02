package com.yangchengwei.easytrip.workspace

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme

@Composable
fun MapLegend(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.wrapContentWidth().testTag("map-legend"),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = EasyTripTheme.elevation.floating,
    ) {
        Row(
            modifier = Modifier.wrapContentWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BookmarkLegendShape(scheduled = true, Modifier.testTag("legend-scheduled-shape"))
            Text("已排入", style = MaterialTheme.typography.labelSmall)
            BookmarkLegendShape(scheduled = false, Modifier.testTag("legend-saved-shape"))
            Text("仅收藏", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun BookmarkLegendShape(scheduled: Boolean, modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    Canvas(modifier.size(12.dp)) {
        val path = Path()
        BookmarkGeometry.forEachIndexed { index, point ->
            val x = point.x * size.width
            val y = point.y * size.height
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        if (scheduled) drawPath(path, primary) else drawPath(path, primary, style = Stroke(width = 1.5.dp.toPx()))
    }
}
