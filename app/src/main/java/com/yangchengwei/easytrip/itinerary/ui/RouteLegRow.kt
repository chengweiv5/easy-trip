package com.yangchengwei.easytrip.itinerary.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.yangchengwei.easytrip.core.ui.component.CompactSecondaryButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.model.RouteStatus
import com.yangchengwei.easytrip.core.model.TransportMode

@Composable
fun RouteLegRow(leg: RouteLegUi, onMode: () -> Unit, onRetry: () -> Unit) {
    val detail = when (leg.status) {
        RouteStatus.WAITING_NETWORK -> "等待联网"
        RouteStatus.PENDING -> "等待计算"
        RouteStatus.CALCULATING -> "计算中"
        RouteStatus.SUCCESS -> listOfNotNull(
            leg.distanceMeters?.let(::formatDistance),
            leg.durationSeconds?.let(::formatDuration),
        ).joinToString(" · ")
        RouteStatus.FAILED -> leg.error ?: "路线规划失败"
    }
    Row(
        Modifier.fillMaxWidth().testTag("leg-${leg.id}").padding(start = 28.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.width(2.dp).height(18.dp).background(MaterialTheme.colorScheme.outlineVariant))
            Text("↓", color = MaterialTheme.colorScheme.primary, modifier = Modifier.clearAndSetSemantics {})
            Box(Modifier.width(2.dp).height(18.dp).background(MaterialTheme.colorScheme.outlineVariant))
        }
        Row(
            Modifier.weight(1f).padding(start = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(leg.modeLabel(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (leg.status == RouteStatus.CALCULATING) {
                    CircularProgressIndicator(Modifier.size(24.dp).semantics { contentDescription = "路线计算中" })
                }
                TextButton(onMode, Modifier.testTag("mode-${leg.id}")) { Text("交通方式") }
                if (leg.status == RouteStatus.FAILED) {
                    TextButton(onRetry, Modifier.testTag("retry-${leg.id}")) { Text("重试") }
                }
            }
        }
    }
}

private fun RouteLegUi.modeLabel() = when (mode) {
    TransportMode.WALK -> "步行"
    TransportMode.TAXI -> "打车"
    TransportMode.DRIVE -> "驾车"
    TransportMode.TRANSIT -> "公交"
}
