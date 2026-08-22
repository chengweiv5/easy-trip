package com.yangchengwei.easytrip.place.ui

import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import com.yangchengwei.easytrip.core.ui.component.CompactSecondaryButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import com.yangchengwei.easytrip.place.amap.PlaceCandidate

@Composable
fun PlaceSearchResults(
    state: PlaceSearchState,
    savedPoiIds: Set<String>,
    onSelect: (PlaceCandidate) -> Unit,
    onToggleCollection: (PlaceCandidate) -> Unit,
    modifier: Modifier = Modifier,
    selectedPoiId: String? = null,
    collectionBusyPoiIds: Set<String> = emptySet(),
) {
    LazyColumn(modifier) {
        if (state.query.isBlank()) item { Text("输入地点开始搜索") }
        state.error?.let { error -> item { Text(error) } }
        if (state.searching) item { Text("搜索中") }
        items(state.results, key = { "candidate-${it.poiId}" }) { candidate ->
            val saved = candidate.poiId in savedPoiIds
            val locatable = candidate.point != null
            ListItem(
                headlineContent = { Text(candidate.name) },
                supportingContent = { Text(listOf(candidate.address, if (locatable) "" else "无法在地图定位").filter(String::isNotBlank).joinToString(" · ")) },
                trailingContent = {
                    Row {
                        TextButton(
                            onClick = { onToggleCollection(candidate) },
                            enabled = locatable && candidate.poiId !in collectionBusyPoiIds,
                            modifier = Modifier.testTag("save-result-${candidate.poiId}"),
                        ) { Text(when { saved -> "已收藏"; !locatable -> "无法收藏"; else -> "收藏" }) }
                    }
                },
                modifier = Modifier
                    .testTag("search-result-${candidate.poiId}")
                    .selectable(enabled = locatable, selected = candidate.poiId == selectedPoiId, role = Role.Button) { onSelect(candidate) }
            )
        }
    }
}
