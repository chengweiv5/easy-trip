package com.yangchengwei.easytrip.core.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/** A single rounded track with a flush fill boundary and no trailing stop marker. */
@Composable
fun ContinuousProgressBar(
    progress: Float,
    color: Color,
    trackColor: Color,
    modifier: Modifier = Modifier,
    minimumVisibleFraction: Float = 0f,
) {
    val actualProgress = progress.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0f
    val minimum = minimumVisibleFraction.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0f
    Canvas(
        modifier.height(5.dp).semantics {
            progressBarRangeInfo = ProgressBarRangeInfo(actualProgress, 0f..1f)
        },
    ) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas
        val radius = minOf(size.height, size.width) / 2f
        val outline = Path().apply {
            addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(radius)))
        }
        var fillWidth = size.width * maxOf(actualProgress, minimum)
        // The travel card intentionally keeps a small starting mark at 0%.
        // Expense shares leave minimumVisibleFraction at zero to retain true zero.
        if (minimum > 0f) fillWidth = fillWidth.coerceAtLeast(size.height).coerceAtMost(size.width)
        clipPath(outline) {
            drawRect(trackColor)
            if (fillWidth > 0f) {
                drawRect(
                    color = color,
                    topLeft = Offset(if (layoutDirection == LayoutDirection.Rtl) size.width - fillWidth else 0f, 0f),
                    size = Size(fillWidth, size.height),
                )
            }
        }
    }
}
