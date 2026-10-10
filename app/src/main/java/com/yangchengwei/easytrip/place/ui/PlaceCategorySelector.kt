package com.yangchengwei.easytrip.place.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.place.domain.PlaceCategory

@Composable
fun PlaceCategorySelector(
    selected: PlaceCategory, enabled: Boolean, onSelect: (PlaceCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PlaceCategory.entries.chunked(3).forEach { categories ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEach { category ->
                    val style = placeCategoryStyle(category)
                    val isSelected = selected == category
                    Surface(
                        modifier = Modifier.weight(1f).heightIn(min = 60.dp)
                            .testTag("place-category-${category.storageKey}")
                            .selectable(isSelected, enabled = enabled, role = Role.RadioButton,
                                onClick = { onSelect(category) }),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color(style.backgroundArgb) else MaterialTheme.colorScheme.surface,
                        contentColor = Color(style.foregroundArgb),
                        border = BorderStroke(if (isSelected) 2.dp else 1.dp,
                            Color(style.foregroundArgb).copy(alpha = if (isSelected) 1f else .15f)),
                    ) {
                        Box(Modifier.padding(8.dp)) {
                            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(painterResource(style.iconRes), null, Modifier.size(22.dp))
                                Text(category.label, style = MaterialTheme.typography.labelMedium)
                            }
                            if (isSelected) Text("✓", Modifier.align(Alignment.TopEnd),
                                style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                repeat(3 - categories.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
