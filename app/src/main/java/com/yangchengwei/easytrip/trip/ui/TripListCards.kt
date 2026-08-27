package com.yangchengwei.easytrip.trip.ui

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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.ui.component.EasyTripPrimaryButton
import com.yangchengwei.easytrip.core.ui.theme.EasyTripSurfaceSoft

internal val TripMenuItem = SemanticsPropertyKey<Boolean>("TripMenuItem")
internal var SemanticsPropertyReceiver.tripMenuItem by TripMenuItem
internal val TripMenuTone = SemanticsPropertyKey<String>("TripMenuTone")
internal var SemanticsPropertyReceiver.tripMenuTone by TripMenuTone

@Composable
internal fun PrimaryTripCard(
    trip: TripCardUiModel,
    menuExpanded: Boolean,
    onMenuExpandedChange: (Boolean) -> Unit,
    onAction: (TripListAction) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("primary-trip-${trip.id}"),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Text(
                    trip.name,
                    modifier = Modifier.weight(1f).testTag("primary-trip-name-${trip.id}"),
                    style = MaterialTheme.typography.headlineLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                TripMenu(trip, menuExpanded, onMenuExpandedChange, onAction)
            }
            Text(
                listOfNotNull(trip.dateLabel ?: "待定日期", trip.dayCountLabel, trip.travelModeLabel).joinToString(" · "),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            EasyTripPrimaryButton(
                onClick = { onAction(TripListAction.OpenTrip(trip.id)) },
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("continue-trip-${trip.id}"),
            ) { Text("继续规划") }
        }
    }
}

@Composable
internal fun OtherTripRow(
    trip: TripCardUiModel,
    menuExpanded: Boolean,
    onMenuExpandedChange: (Boolean) -> Unit,
    onAction: (TripListAction) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().testTag("other-trip-${trip.id}")
            .semantics { contentDescription = "打开旅行 ${trip.name}" }
            .clickable(role = Role.Button) { onAction(TripListAction.OpenTrip(trip.id)) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(Modifier.size(48.dp), RoundedCornerShape(12.dp), EasyTripSurfaceSoft) {
            Box(contentAlignment = Alignment.Center) {
                Text("旅", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                trip.name,
                modifier = Modifier.testTag("other-trip-name-${trip.id}"),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${trip.dateLabel ?: "待定日期"} · ${trip.travelModeLabel}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        TripMenu(trip, menuExpanded, onMenuExpandedChange, onAction)
    }
}

@Composable
private fun TripMenu(
    trip: TripCardUiModel,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onAction: (TripListAction) -> Unit,
) {
    Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
        IconButton(
            onClick = { onExpandedChange(!expanded) },
            modifier = Modifier.size(40.dp).testTag("trip-menu-${trip.id}")
                .semantics { contentDescription = "${trip.name}，更多旅行操作" },
        ) { MoreIcon(Modifier.size(22.dp)) }
        DropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
            DropdownMenuItem(
                text = { Text("设置") },
                onClick = {
                    onExpandedChange(false)
                    onAction(TripListAction.OpenSettings(trip.id))
                },
                modifier = Modifier.testTag("trip-menu-settings-${trip.id}").semantics { tripMenuItem = true },
            )
            DropdownMenuItem(
                text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                onClick = {
                    onExpandedChange(false)
                    onAction(TripListAction.RequestDelete(trip.id))
                },
                modifier = Modifier.testTag("trip-menu-delete-${trip.id}").semantics {
                    tripMenuItem = true
                    tripMenuTone = "danger"
                },
            )
        }
    }
}

@Composable
private fun MoreIcon(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier) {
        val radius = size.minDimension / 11f
        listOf(.25f, .5f, .75f).forEach { fraction ->
            drawCircle(color, radius, Offset(size.width * fraction, size.height / 2f))
        }
    }
}
