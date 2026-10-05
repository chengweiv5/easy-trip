package com.yangchengwei.easytrip.workspace

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.ui.component.CompactSecondaryButton

@Composable
internal fun WorkspaceSearchSummary(
    results: WorkspaceSearchResults,
    onOpenList: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    Surface(
        modifier = modifier.fillMaxWidth().height(68.dp).testTag("workspace-search-summary"),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp,
    ) {
        Row(
            Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                Modifier.weight(1f).height(48.dp)
                    .clickable(role = Role.Button, onClick = onOpenList)
                    .testTag("workspace-search-query").padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Canvas(Modifier.size(20.dp)) {
                    drawCircle(primary, size.width * .29f, Offset(size.width * .4f, size.height * .4f), style = Stroke(1.7.dp.toPx()))
                    drawLine(primary, Offset(size.width * .62f, size.height * .62f),
                        Offset(size.width * .9f, size.height * .9f), 1.7.dp.toPx(), StrokeCap.Round)
                }
                Column {
                    Text(results.query, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    val missing = results.places.size - results.mappedPlaces.size
                    Text(
                        if (missing == 0) "${results.places.size} 个搜索结果"
                        else "${results.places.size} 个结果 · ${results.mappedPlaces.size} 个可定位",
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            CompactSecondaryButton(onClick = onOpenList, modifier = Modifier.height(48.dp).testTag("workspace-search-results-list")) {
                Text("列表")
            }
            Box(
                Modifier.size(48.dp).testTag("workspace-search-results-clear")
                    .clickable(role = Role.Button, onClick = onClear)
                    .semantics { contentDescription = "清除搜索结果" },
                contentAlignment = Alignment.Center,
            ) {
                Text("×", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
