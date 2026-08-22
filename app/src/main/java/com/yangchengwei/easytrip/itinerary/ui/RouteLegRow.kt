package com.yangchengwei.easytrip.itinerary.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.yangchengwei.easytrip.core.model.RouteStatus
import com.yangchengwei.easytrip.core.model.TransportMode

@Composable
fun RouteLegRow(leg: RouteLegUi, onMode: () -> Unit, onRetry: () -> Unit) {
    val detail = when (leg.status) {
        RouteStatus.WAITING_NETWORK -> "等待联网"
        RouteStatus.PENDING -> "等待计算"
        RouteStatus.CALCULATING -> "计算中"
        RouteStatus.SUCCESS -> listOfNotNull(leg.distanceMeters?.let { "$it 米" }, leg.durationSeconds?.let { "${it / 60} 分钟" }).joinToString(" · ")
        RouteStatus.FAILED -> leg.error ?: "路线规划失败"
    }
    ListItem(
        headlineContent = { Text("${leg.modeLabel()} · $detail") },
        trailingContent = {
            Row {
                if (leg.status == RouteStatus.CALCULATING) {
                    CircularProgressIndicator(Modifier.semantics { contentDescription = "路线计算中" })
                }
                TextButton(onMode, Modifier.testTag("mode-${leg.id}")) { Text("交通方式") }
                if (leg.status == RouteStatus.FAILED) TextButton(onRetry, Modifier.testTag("retry-${leg.id}")) { Text("重试") }
            }
        },
        modifier = Modifier.fillMaxWidth().testTag("leg-${leg.id}"),
    )
}

private fun RouteLegUi.modeLabel() = when (mode) {
    TransportMode.WALK -> "步行"
    TransportMode.TAXI -> "打车"
    TransportMode.DRIVE -> "驾车"
    TransportMode.TRANSIT -> "公交"
}
