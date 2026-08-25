package com.yangchengwei.easytrip.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun WorkspaceTabs(
    selected: WorkspaceSection,
    onSelect: (WorkspaceSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .selectableGroup()
            .testTag("workspace-tabs"),
    ) {
        WorkspaceSection.entries.forEach { section ->
            val isSelected = selected == section
            Column(
                Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clickable(role = Role.Tab) { onSelect(section) }
                    .semantics { this.selected = isSelected }
                    .testTag("section-${section.name}"),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (section == WorkspaceSection.PLACE_POOL) "地点池" else "行程",
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    )
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .then(if (isSelected) Modifier.testTag("workspace-tab-indicator-${section.name}") else Modifier),
                )
            }
        }
    }
}
