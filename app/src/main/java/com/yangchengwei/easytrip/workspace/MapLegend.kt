package com.yangchengwei.easytrip.workspace

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
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
        modifier = modifier.wrapContentWidth().height(32.dp).testTag("map-legend"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = EasyTripTheme.elevation.floating,
    ) {
        Row(
            modifier = Modifier.wrapContentWidth().padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                CircleLegendShape(scheduled = true, Modifier.testTag("legend-scheduled-shape"))
                Text("已排入", style = MaterialTheme.typography.bodySmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                CircleLegendShape(scheduled = false, Modifier.testTag("legend-saved-shape"))
                Text("仅收藏", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun CircleLegendShape(scheduled: Boolean, modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    Canvas(modifier.size(12.dp)) {
        if (scheduled) drawCircle(primary) else {
            val strokeWidth = 1.5.dp.toPx()
            drawCircle(primary, radius = size.minDimension / 2 - strokeWidth / 2, style = Stroke(width = strokeWidth))
        }
    }
}
