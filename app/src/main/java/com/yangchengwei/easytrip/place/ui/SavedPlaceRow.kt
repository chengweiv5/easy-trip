package com.yangchengwei.easytrip.place.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.ui.component.CompactSecondaryButton

@Composable
fun SavedPlaceRow(
    place: SavedPlaceRowUi,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier.fillMaxWidth().testTag("saved-place-${place.id}"), color = MaterialTheme.colorScheme.surface) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f).widthIn(min = 0.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        place.name,
                        modifier = Modifier.weight(1f).widthIn(min = 0.dp),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        when {
                            place.recentlyCollected -> "刚刚收藏 · 待安排行程"
                            place.scheduled -> "已排入 ${place.itineraryOccurrenceCount} 次"
                            else -> "仅收藏"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("place-status-${place.id}"),
                    )
                }
                Text(
                    place.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                place.note?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                if (place.tags.isNotEmpty()) Text(place.tags.joinToString(" · "), style = MaterialTheme.typography.labelSmall)
            }
            Column {
                CompactSecondaryButton(onEdit, modifier = Modifier.testTag("edit-place-${place.id}")) { Text("编辑") }
                CompactSecondaryButton(onDelete, modifier = Modifier.testTag("delete-place-${place.id}")) { Text("删除") }
            }
        }
    }
}
