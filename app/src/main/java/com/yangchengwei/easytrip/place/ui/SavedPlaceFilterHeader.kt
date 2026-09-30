package com.yangchengwei.easytrip.place.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.workspace.WorkspaceCloseIcon
import com.yangchengwei.easytrip.workspace.WorkspaceSearchIcon

/** Shared presentation only: the pool and picker each own their filter state. */
@Composable
internal fun SavedPlaceFilterHeader(
    cities: List<PlaceCityGroup>,
    selectedCityKey: String?,
    onSelectCity: (String?) -> Unit,
    schedule: PlaceScheduleFilter,
    onSelectSchedule: (PlaceScheduleFilter) -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
    searchTag: String,
    clearSearchTag: String,
    scheduleTagPrefix: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    controlColor: Color = MaterialTheme.colorScheme.background,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (cities.isNotEmpty()) {
            PlaceCityFilterBar(cities, selectedCityKey, onSelectCity, Modifier.fillMaxWidth(), enabled, controlColor)
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            PlaceScheduleFilter.entries.forEach { filter ->
                Surface(
                    modifier = Modifier.testTag("$scheduleTagPrefix-${filter.name}")
                        .semantics { selected = schedule == filter }
                        .clickable(enabled = enabled, role = Role.Tab) { onSelectSchedule(filter) },
                    shape = RoundedCornerShape(8.dp),
                    color = controlColor,
                ) {
                    Text(
                        filter.label,
                        Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (schedule == filter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
        SavedPlaceSearchField(
            query, onQueryChange, enabled,
            cities.firstOrNull { it.key == selectedCityKey }?.name,
            searchTag, clearSearchTag, controlColor,
        )
    }
}

@Composable
private fun SavedPlaceSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    enabled: Boolean,
    cityName: String?,
    searchTag: String,
    clearSearchTag: String,
    controlColor: Color,
) {
    val focusManager = LocalFocusManager.current
    Surface(shape = RoundedCornerShape(8.dp), color = controlColor) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 36.dp).padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            WorkspaceSearchIcon(Modifier.size(16.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                enabled = enabled,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier.weight(1f).testTag(searchTag)
                    .semantics { contentDescription = "搜索已收藏的地点" },
                decorationBox = { input ->
                    Box {
                        if (query.isEmpty()) {
                            Text(
                                cityName?.let { "搜索${it}收藏的地点" } ?: "搜索已收藏的地点",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        input()
                    }
                },
            )
            if (query.isNotEmpty()) {
                Box(
                    Modifier.size(24.dp).testTag(clearSearchTag)
                        .semantics { contentDescription = "清空搜索" }
                        .clickable(enabled = enabled, role = Role.Button) { onQueryChange("") },
                    contentAlignment = Alignment.Center,
                ) { WorkspaceCloseIcon(Modifier.size(14.dp)) }
            }
        }
    }
}
