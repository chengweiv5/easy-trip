package com.yangchengwei.easytrip.place.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import com.yangchengwei.easytrip.core.ui.component.CompactSecondaryButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.ui.component.SelectablePill
import com.yangchengwei.easytrip.place.domain.PlaceTag
import com.yangchengwei.easytrip.place.domain.SavedPlace

@Composable
fun SavedPlacesContent(
    places: List<SavedPlace>,
    tags: List<PlaceTag>,
    selectedTagIds: Set<String>,
    onToggleTag: (String) -> Unit,
    onEdit: (SavedPlace) -> Unit,
    onDelete: (SavedPlace) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    Box(modifier) {
        LazyColumn(state = listState) {
            if (tags.isNotEmpty()) {
                item {
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                    ) {
                        tags.forEach { tag ->
                            SelectablePill(
                                selected = tag.id in selectedTagIds,
                                onClick = { onToggleTag(tag.id) },
                                label = { Text(tag.name) },
                                modifier = Modifier.testTag("tag-${tag.id}"),
                            )
                        }
                    }
                }
            }
            items(places, key = { "saved-${it.id}" }) { place ->
                ListItem(
                    headlineContent = { Text(place.name) },
                    supportingContent = {
                        Text(
                            listOf(place.address, place.note, place.tags.joinToString(" · ") { it.name })
                                .filter(String::isNotBlank)
                                .joinToString("\n"),
                        )
                    },
                    trailingContent = {
                        Row {
                            TextButton(onClick = { onEdit(place) }) { Text("编辑") }
                            TextButton(
                                onClick = { onDelete(place) },
                                modifier = Modifier.testTag("delete-place-${place.id}"),
                            ) { Text("删除") }
                        }
                    },
                    modifier = Modifier.testTag("saved-place-${place.id}"),
                )
            }
        }
        ReadOnlyLazyScrollbar(listState, Modifier.align(Alignment.CenterEnd))
    }
}
