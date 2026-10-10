package com.yangchengwei.easytrip.core.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yangchengwei.easytrip.core.ui.theme.LocalThemePalette
import com.yangchengwei.easytrip.core.ui.theme.ThemePalette

internal val editorPageColor: Color
    @Composable get() = if (LocalThemePalette.current == ThemePalette.LAKE) Color(0xFFE5EEF0)
    else lerp(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.primaryContainer, .28f)

internal val editorBorderColor: Color
    @Composable get() = if (LocalThemePalette.current == ThemePalette.LAKE) Color(0xFFC7D8DC)
    else MaterialTheme.colorScheme.outline

internal enum class EditorSectionGlyph { TIME, WALLET, NOTE }

@Composable
internal fun EditorSection(
    title: String,
    glyph: EditorSectionGlyph,
    modifier: Modifier = Modifier,
    summary: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, editorBorderColor),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val stackSummary = maxWidth < 290.dp || LocalDensity.current.fontScale >= 1.5f
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            Modifier.size(28.dp).background(
                                MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp),
                            ),
                            contentAlignment = Alignment.Center,
                        ) { EditorGlyph(glyph) }
                        Text(title, Modifier.weight(1f).semantics { heading() },
                            fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        if (!stackSummary && summary != null) SectionSummary(summary)
                    }
                    if (stackSummary && summary != null) SectionSummary(summary)
                }
            }
            content()
        }
    }
}

@Composable
private fun SectionSummary(summary: String) {
    Text(summary, style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun EditorGlyph(glyph: EditorSectionGlyph) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(Modifier.size(17.dp)) {
        val unit = size.minDimension / 24f
        val stroke = Stroke(1.5f * unit, cap = StrokeCap.Round)
        fun line(x: Float, y: Float, endX: Float, endY: Float) =
            drawLine(color, Offset(x * unit, y * unit), Offset(endX * unit, endY * unit),
                stroke.width, StrokeCap.Round)
        when (glyph) {
            EditorSectionGlyph.TIME -> {
                drawCircle(color, 9 * unit, center, style = stroke)
                line(12f, 6f, 12f, 12f)
                line(12f, 12f, 16f, 15f)
            }
            EditorSectionGlyph.WALLET -> {
                drawRoundRect(color, Offset(3 * unit, 5 * unit), Size(18 * unit, 15 * unit),
                    CornerRadius(2 * unit), style = stroke)
                drawRoundRect(color, Offset(13 * unit, 10 * unit), Size(8 * unit, 6 * unit),
                    CornerRadius(unit), style = stroke)
                line(16f, 13f, 17f, 13f)
            }
            EditorSectionGlyph.NOTE -> {
                line(4f, 5f, 17f, 5f)
                line(4f, 10f, 14f, 10f)
                line(4f, 15f, 10f, 15f)
                line(13f, 18f, 20f, 11f)
                line(12f, 20f, 13f, 17f)
            }
        }
    }
}
