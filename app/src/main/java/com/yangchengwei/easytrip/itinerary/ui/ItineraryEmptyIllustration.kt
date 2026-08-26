package com.yangchengwei.easytrip.itinerary.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
internal fun ItineraryEmptyIllustration(
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    Canvas(
        modifier = modifier
            .size(88.dp)
            .testTag("itinerary-empty-illustration")
            .semantics { this.contentDescription = contentDescription },
    ) {
        val thinStroke = 2.dp.toPx()
        val boldStroke = 3.dp.toPx()
        drawCircle(surfaceVariant, radius = size.minDimension / 2)
        drawRoundRect(
            color = primary,
            topLeft = Offset(size.width * 0.25f, size.height * 0.22f),
            size = Size(size.width * 0.5f, size.height * 0.56f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),
            style = Stroke(width = boldStroke),
        )
        drawLine(
            color = secondary,
            start = Offset(size.width * 0.36f, size.height * 0.42f),
            end = Offset(size.width * 0.64f, size.height * 0.42f),
            strokeWidth = thinStroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = secondary,
            start = Offset(size.width * 0.36f, size.height * 0.55f),
            end = Offset(size.width * 0.58f, size.height * 0.55f),
            strokeWidth = thinStroke,
            cap = StrokeCap.Round,
        )
        drawCircle(primary, radius = 4.dp.toPx(), center = Offset(size.width * 0.68f, size.height * 0.68f))
    }
}
