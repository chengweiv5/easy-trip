package com.yangchengwei.easytrip.place.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun SavedPlaceRow(
    place: SavedPlaceRowUi,
    onQuickAdd: (() -> Unit)?,
    onOpenDetail: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember(place.id) { mutableStateOf(false) }
    Surface(modifier.fillMaxWidth().testTag("saved-place-${place.id}"), color = MaterialTheme.colorScheme.surface) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .widthIn(min = 0.dp)
                    .testTag("open-place-detail-${place.id}")
                    .semantics { contentDescription = "查看${place.name}详情" }
                    .clickable(onClick = onOpenDetail),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        place.name,
                        modifier = Modifier.weight(1f).widthIn(min = 0.dp),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        when {
                            place.recentlyCollected -> "刚刚收藏 · 待安排行程"
                            place.scheduled -> "已排入 ${place.itineraryOccurrenceCount} 次"
                            else -> "仅收藏"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("place-status-${place.id}"),
                    )
                }
                if (place.address.isNotBlank()) {
                    Text(
                        place.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                place.note?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (place.tags.isNotEmpty()) {
                    Text(
                        place.tags.joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                onQuickAdd?.let { quickAdd ->
                    Surface(
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("quick-add-place-${place.id}")
                            .semantics { contentDescription = "添加${place.name}到行程" }
                            .clickable(onClick = quickAdd),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            PlusIcon(Modifier.size(20.dp))
                        }
                    }
                }
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .size(48.dp)
                            .testTag("more-place-${place.id}")
                            .semantics { contentDescription = "${place.name}，更多操作" }
                            .clickable { menuExpanded = !menuExpanded },
                        contentAlignment = Alignment.Center,
                    ) {
                        MoreIcon(Modifier.size(22.dp))
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("编辑") },
                            onClick = { menuExpanded = false; onEdit() },
                            modifier = Modifier.testTag("menu-edit-place-${place.id}"),
                        )
                        DropdownMenuItem(
                            text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                            onClick = { menuExpanded = false; onDelete() },
                            modifier = Modifier.testTag("menu-delete-place-${place.id}"),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlusIcon(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier) {
        val strokeWidth = size.minDimension / 9f
        drawLine(color, Offset(size.width / 2f, size.height * .2f), Offset(size.width / 2f, size.height * .8f), strokeWidth)
        drawLine(color, Offset(size.width * .2f, size.height / 2f), Offset(size.width * .8f, size.height / 2f), strokeWidth)
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
