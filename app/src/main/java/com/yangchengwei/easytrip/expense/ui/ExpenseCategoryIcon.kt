package com.yangchengwei.easytrip.expense.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.expense.ExpenseCategory

/** Decorative: the adjacent category label supplies the accessible name. */
@Composable
internal fun ExpenseCategoryIcon(category: ExpenseCategory?) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(Modifier.size(32.dp).background(
        MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(9.dp),
    ).padding(6.dp)) {
        val unit = size.minDimension / 24f
        val stroke = Stroke(1.6f * unit, cap = StrokeCap.Round)
        fun line(x: Float, y: Float, x2: Float, y2: Float) =
            drawLine(color, Offset(x * unit, y * unit), Offset(x2 * unit, y2 * unit),
                stroke.width, StrokeCap.Round)
        fun box(x: Float, y: Float, width: Float, height: Float) =
            drawRoundRect(color, Offset(x * unit, y * unit), Size(width * unit, height * unit),
                CornerRadius(2 * unit), style = stroke)
        when (category) {
            ExpenseCategory.LODGING -> {
                box(3f, 10f, 18f, 7f); box(5f, 6f, 14f, 4f)
                line(3f, 17f, 3f, 20f); line(21f, 17f, 21f, 20f)
            }
            ExpenseCategory.TRANSPORT -> {
                box(4f, 4f, 16f, 14f); line(4f, 11f, 20f, 11f)
                line(7f, 15f, 8f, 15f); line(16f, 15f, 17f, 15f)
                line(6f, 18f, 6f, 20f); line(18f, 18f, 18f, 20f)
            }
            ExpenseCategory.ATTRACTION -> {
                line(3f, 19f, 10f, 6f); line(10f, 6f, 17f, 19f)
                line(17f, 19f, 21f, 19f); line(21f, 19f, 16f, 10f)
                line(3f, 19f, 17f, 19f)
            }
            ExpenseCategory.FOOD -> {
                line(6f, 4f, 6f, 20f); line(3f, 4f, 3f, 9f)
                line(9f, 4f, 9f, 9f); line(3f, 9f, 9f, 9f)
                line(18f, 4f, 18f, 20f); line(15f, 4f, 15f, 12f)
                line(15f, 12f, 18f, 12f)
            }
            ExpenseCategory.SHOPPING -> {
                box(4f, 8f, 16f, 13f); box(8f, 3f, 8f, 8f)
            }
            ExpenseCategory.OTHER, null -> {
                box(3f, 3f, 7f, 7f); box(14f, 3f, 7f, 7f)
                box(3f, 14f, 7f, 7f); box(14f, 14f, 7f, 7f)
            }
        }
    }
}
