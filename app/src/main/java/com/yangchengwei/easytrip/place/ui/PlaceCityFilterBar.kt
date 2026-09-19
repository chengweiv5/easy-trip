package com.yangchengwei.easytrip.place.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.ui.theme.EasyTripBackground

@Composable
internal fun PlaceCityFilterBar(
    groups: List<PlaceCityGroup>,
    selectedKey: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(modifier.horizontalScroll(rememberScrollState()).testTag("place-city-filters"), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        CityPill(null, "全部", groups.sumOf { it.rows.size }, selectedKey == null, enabled, onSelect)
        groups.forEach { CityPill(it.key, it.name, it.rows.size, selectedKey == it.key, enabled, onSelect) }
    }
}

@Composable
private fun CityPill(key: String?, name: String, count: Int, selected: Boolean, enabled: Boolean, onSelect: (String?) -> Unit) {
    Surface(
        Modifier.heightIn(min = 32.dp).testTag("place-city-${key ?: "all"}")
            .semantics { this.selected = selected; contentDescription = "$name，$count 个地点" }
            .clickable(enabled = enabled, role = Role.Tab) { onSelect(key) },
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else EasyTripBackground,
    ) {
        Text("$name $count", Modifier.padding(horizontal = 10.dp, vertical = 7.dp), style = MaterialTheme.typography.labelSmall, color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

@Composable
internal fun PlaceCityGroupHeading(group: PlaceCityGroup) {
    Text("${group.name} · ${group.rows.size}", Modifier.padding(start = 2.dp, top = 6.dp, bottom = 2.dp).testTag("place-city-heading-${group.key}"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
