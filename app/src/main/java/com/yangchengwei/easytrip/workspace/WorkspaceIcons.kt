package com.yangchengwei.easytrip.workspace

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material3.MaterialTheme

@Composable
internal fun WorkspaceBackIcon(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onSurface
    Canvas(modifier) {
        val stroke = size.minDimension / 10f
        drawLine(color, Offset(size.width * .68f, size.height * .2f), Offset(size.width * .32f, size.height * .5f), stroke, StrokeCap.Round)
        drawLine(color, Offset(size.width * .32f, size.height * .5f), Offset(size.width * .68f, size.height * .8f), stroke, StrokeCap.Round)
    }
}

@Composable
internal fun WorkspaceMoreIcon(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onSurface
    Canvas(modifier) {
        val radius = size.minDimension / 11f
        listOf(.25f, .5f, .75f).forEach { fraction ->
            drawCircle(color, radius, Offset(size.width * fraction, size.height / 2f))
        }
    }
}

@Composable
internal fun WorkspaceSearchIcon(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier) {
        val stroke = Stroke(width = size.minDimension / 12f, cap = StrokeCap.Round)
        drawCircle(color, size.minDimension * .27f, Offset(size.width * .43f, size.height * .43f), style = stroke)
        drawLine(color, Offset(size.width * .63f, size.height * .63f), Offset(size.width * .83f, size.height * .83f), stroke.width, StrokeCap.Round)
    }
}

@Composable
internal fun WorkspaceLocateIcon(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onSurface
    Canvas(modifier) {
        val strokeWidth = size.minDimension / 12f
        drawCircle(color, size.minDimension * .24f, center, style = Stroke(strokeWidth))
        drawCircle(color, size.minDimension * .07f, center)
        drawLine(color, Offset(center.x, 0f), Offset(center.x, size.height * .2f), strokeWidth, StrokeCap.Round)
        drawLine(color, Offset(center.x, size.height * .8f), Offset(center.x, size.height), strokeWidth, StrokeCap.Round)
        drawLine(color, Offset(0f, center.y), Offset(size.width * .2f, center.y), strokeWidth, StrokeCap.Round)
        drawLine(color, Offset(size.width * .8f, center.y), Offset(size.width, center.y), strokeWidth, StrokeCap.Round)
    }
}

@Composable
internal fun WorkspaceLayerIcon(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onSurface
    Canvas(modifier) {
        val stroke = Stroke(size.minDimension / 14f, join = StrokeJoin.Round)
        fun layer(centerY: Float) {
            val path = Path().apply {
                moveTo(size.width / 2f, centerY - size.height * .16f)
                lineTo(size.width, centerY)
                lineTo(size.width / 2f, centerY + size.height * .16f)
                lineTo(0f, centerY)
                close()
            }
            drawPath(path, color, style = stroke)
        }
        layer(size.height * .36f)
        layer(size.height * .64f)
    }
}

@Composable
internal fun WorkspaceCloseIcon(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onSurface
    Canvas(modifier) {
        val strokeWidth = size.minDimension / 10f
        drawLine(color, Offset(size.width * .22f, size.height * .22f), Offset(size.width * .78f, size.height * .78f), strokeWidth, StrokeCap.Round)
        drawLine(color, Offset(size.width * .78f, size.height * .22f), Offset(size.width * .22f, size.height * .78f), strokeWidth, StrokeCap.Round)
    }
}

@Composable
internal fun WorkspaceCheckIcon(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onSurface
    Canvas(modifier) {
        val stroke = size.minDimension / 10f
        drawLine(color, Offset(size.width * .2f, size.height * .5f), Offset(size.width * .42f, size.height * .72f), stroke, StrokeCap.Round)
        drawLine(color, Offset(size.width * .42f, size.height * .72f), Offset(size.width * .82f, size.height * .28f), stroke, StrokeCap.Round)
    }
}
