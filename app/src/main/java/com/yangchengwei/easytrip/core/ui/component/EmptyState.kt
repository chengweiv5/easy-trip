package com.yangchengwei.easytrip.core.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.R
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme

enum class EmptyIllustration(@DrawableRes val resource: Int) {
    Trips(R.drawable.empty_trips),
    Places(R.drawable.empty_places),
    Itinerary(R.drawable.empty_itinerary),
    Search(R.drawable.empty_search),
    Failure(R.drawable.empty_failure),
}

@Composable
fun EmptyIllustrationImage(
    illustration: EmptyIllustration,
    modifier: Modifier = Modifier,
) {
    val source = ImageVector.vectorResource(illustration.resource)
    val primary = MaterialTheme.colorScheme.primary
    val soft = MaterialTheme.colorScheme.primaryContainer
    val vector = remember(source, primary, soft) {
        ImageVector.Builder(source.name, source.defaultWidth, source.defaultHeight, source.viewportWidth, source.viewportHeight).apply {
            // These five resource vectors contain two root paths: line art and soft accents.
            source.root.forEach { node ->
                if (node is VectorPath) addPath(
                    pathData = node.pathData,
                    pathFillType = node.pathFillType,
                    fill = if (node.fill is SolidColor && (node.fill as SolidColor).value == com.yangchengwei.easytrip.core.ui.theme.EasyTripSurfaceSoft) SolidColor(soft) else node.fill,
                    fillAlpha = node.fillAlpha,
                    stroke = if (node.stroke != null) SolidColor(primary) else null,
                    strokeAlpha = node.strokeAlpha,
                    strokeLineWidth = node.strokeLineWidth,
                    strokeLineCap = node.strokeLineCap,
                    strokeLineJoin = node.strokeLineJoin,
                    strokeLineMiter = node.strokeLineMiter,
                )
            }
        }.build()
    }
    Image(
        painter = rememberVectorPainter(vector),
        contentDescription = null,
        modifier = modifier.size(96.dp).testTag("empty-illustration-${illustration.name.lowercase()}"),
    )
}

@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    emptyIllustration: EmptyIllustration? = null,
    illustration: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
    verticalPadding: androidx.compose.ui.unit.Dp = 48.dp,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = verticalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(EasyTripTheme.spacing.small),
    ) {
        emptyIllustration?.let { EmptyIllustrationImage(it) }
        illustration?.invoke()
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        action?.invoke()
    }
}
