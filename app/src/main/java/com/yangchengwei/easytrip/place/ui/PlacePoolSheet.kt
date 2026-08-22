package com.yangchengwei.easytrip.place.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun PlacePoolSheet(viewModel: PlacePoolViewModel, modifier: Modifier = Modifier, showSearch: Boolean = true) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    Column(modifier.padding(16.dp)) {
        Text("地点池", style = MaterialTheme.typography.headlineSmall)
        if (showSearch) PlaceSearchField(state.search.query, viewModel::setQuery)
        if (state.tags.isNotEmpty()) Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { state.tags.forEach { tag -> FilterChip(tag.id in state.selectedTagIds, { viewModel.toggleTag(tag.id) }, { Text(tag.name) }, modifier = Modifier.testTag("tag-${tag.id}")) } }
        state.search.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        LazyColumn(Modifier.weight(1f, fill = false)) {
            item { Text("已收藏", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp)) }
            items(state.search.savedPlaces, key = { "saved-${it.id}" }) { place -> ListItem(headlineContent = { Text(place.name) }, supportingContent = { Text(listOf(place.address, place.note, place.tags.joinToString(" · ") { it.name }).filter(String::isNotBlank).joinToString("\n")) }, trailingContent = { Row { TextButton(onClick = { viewModel.edit(place) }) { Text("编辑") }; TextButton(onClick = { viewModel.requestDelete(place) }, modifier = Modifier.testTag("delete-place-${place.id}")) { Text("删除") } } }) }
            item { Text("搜索结果", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp)) }
            items(state.search.results, key = { "candidate-${it.poiId}" }) { candidate ->
                val saved = candidate.poiId in state.savedPoiIds
                ListItem(headlineContent = { Text(candidate.name) }, supportingContent = { Text(candidate.address) }, trailingContent = { TextButton(onClick = { if (!saved) viewModel.save(candidate) }, enabled = !saved) { Text(if (saved) "已收藏" else "收藏") } })
            }
        }
    }
    state.editing?.let { EditSavedPlaceDialog(it, viewModel::dismissEdit, viewModel::updateDetails) }
    state.deleting?.let { place ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDelete,
            title = { Text("删除 ${place.name}？") },
            text = { Text("将同时删除 ${state.deletionUsageCount} 次行程安排及受影响路线。") },
            confirmButton = { TextButton(viewModel::confirmDelete) { Text("确认删除地点") } },
            dismissButton = { TextButton(viewModel::dismissDelete) { Text("取消") } },
        )
    }
}
