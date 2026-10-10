package com.yangchengwei.easytrip.place.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.yangchengwei.easytrip.place.domain.PlaceCategory

/** Shared by itinerary list/calendar; the workspace owns persistence and the modal. */
val LocalPlaceCategoryEdit = staticCompositionLocalOf<((String) -> Unit)?> { null }

@Composable
fun PlaceCategoryGlyph(
    category: PlaceCategory,
    modifier: Modifier = Modifier,
    description: String? = category.label,
) {
    val style = placeCategoryStyle(category)
    Icon(painterResource(style.iconRes), description, modifier, tint = Color(style.foregroundArgb))
}

@Composable
fun PlaceCategoryButton(
    category: PlaceCategory,
    name: String,
    identity: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    tintedBackground: Boolean = false,
) {
    val style = placeCategoryStyle(category)
    Box(
        modifier.size(size).clip(RoundedCornerShape(8.dp))
            .then(if (tintedBackground) Modifier.background(Color(style.backgroundArgb)) else Modifier)
            .testTag("place-category-button-$identity")
            .then(if (onClick != null) Modifier.clickable(
                role = Role.Button, onClickLabel = "修改地点分类", onClick = onClick,
            ) else Modifier)
            .semantics { contentDescription = if (onClick == null) category.label else "修改${name}分类，当前${category.label}" },
        contentAlignment = Alignment.Center,
    ) {
        PlaceCategoryGlyph(category, Modifier.size(if (size >= 40.dp) 22.dp else 18.dp), description = null)
    }
}

@Composable
fun PlaceCategoryQuickDialog(
    state: PlaceCategoryPickerState,
    onSelect: (PlaceCategory) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        modifier = Modifier.testTag("place-category-quick-dialog"),
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = { if (!state.saving) onDismiss() },
        properties = DialogProperties(dismissOnBackPress = !state.saving, dismissOnClickOutside = !state.saving),
        title = { Text("修改地点分类") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(state.name, style = MaterialTheme.typography.bodyMedium)
                Column(Modifier.selectableGroup()) {
                    PlaceCategory.entries.forEach { category ->
                        val selected = state.category == category
                        val style = placeCategoryStyle(category)
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) Color(style.backgroundArgb) else Color.Transparent)
                                .testTag("quick-category-${category.storageKey}")
                                .selectable(selected, enabled = !state.saving, role = Role.RadioButton) { onSelect(category) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            PlaceCategoryGlyph(category, Modifier.size(22.dp), description = null)
                            Text(category.label, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                            if (selected) Text("✓", color = Color(style.foregroundArgb))
                        }
                    }
                }
                Text(
                    if (state.saving) "正在保存…" else state.error ?: "选择后立即保存",
                    color = if (state.error == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss, enabled = !state.saving) { Text("取消") } },
    )
}
