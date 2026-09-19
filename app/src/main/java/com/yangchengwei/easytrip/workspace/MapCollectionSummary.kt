package com.yangchengwei.easytrip.workspace

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme

@Composable
internal fun MapCollectionSummary(count: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(28.dp).testTag("map-collection-summary"),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = EasyTripTheme.elevation.floating,
    ) {
        Row(
            Modifier.padding(horizontal = 11.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val iconColor = MaterialTheme.colorScheme.primary
            Canvas(Modifier.size(17.dp)) {
                val front = Path().apply {
                    moveTo(size.width * .18f, size.height * .25f)
                    lineTo(size.width * .69f, size.height * .25f)
                    lineTo(size.width * .69f, size.height * .94f)
                    lineTo(size.width * .435f, size.height * .78f)
                    lineTo(size.width * .18f, size.height * .94f)
                    close()
                }
                val back = Path().apply {
                    moveTo(size.width * .35f, size.height * .08f)
                    lineTo(size.width * .86f, size.height * .08f)
                    lineTo(size.width * .86f, size.height * .78f)
                }
                val stroke = Stroke(1.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                drawPath(back, iconColor, style = stroke)
                drawPath(front, iconColor, style = stroke)
            }
            Text(
                "$count 个收藏地点",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
        }
    }
}
