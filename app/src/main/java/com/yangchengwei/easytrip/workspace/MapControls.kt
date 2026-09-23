package com.yangchengwei.easytrip.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme

@Composable
fun MapControls(
    active: Boolean,
    onOpenLayerMenu: () -> Unit,
    onLocate: () -> Unit = {},
    modifier: Modifier = Modifier,
    bearing: Float = 0f,
    onResetNorth: () -> Unit = {},
) {
    val controlSize = EasyTripTheme.sizes.workspaceDenseTouchTarget
    val iconSize = EasyTripTheme.sizes.workspaceIconSize
    val controlGap = EasyTripTheme.spacing.workspaceControlGap
    Column(modifier, verticalArrangement = Arrangement.spacedBy(controlGap), horizontalAlignment = Alignment.End) {
        Surface(
            Modifier.size(controlSize).testTag("layer-menu").semantics {
                contentDescription = "地图图层"
                selected = active
            }.clickable(onClick = onOpenLayerMenu),
            shape = RoundedCornerShape(controlSize / 2),
            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            shadowElevation = EasyTripTheme.elevation.floating,
        ) {
            Box(contentAlignment = Alignment.Center) {
                WorkspaceLayerIcon(Modifier.size(iconSize), if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
            }
        }
        MapControlButton("定位", "workspace-locate", controlSize, onLocate) {
            WorkspaceLocateIcon(Modifier.size(iconSize))
        }
        MapControlButton("指北针，点击恢复正北", "workspace-compass", controlSize, onResetNorth) {
            WorkspaceCompassIcon(bearing = bearing, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun MapControlButton(
    description: String,
    tag: String,
    size: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Surface(
        Modifier.size(size).testTag(tag).semantics { contentDescription = description }.clickable(onClick = onClick),
        shape = RoundedCornerShape(size / 2),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = EasyTripTheme.elevation.floating,
    ) { Box(contentAlignment = Alignment.Center) { content() } }
}

@Composable
internal fun MapLayerMenu(
    layer: MapLayer,
    onClose: () -> Unit,
    onSelectLayer: (MapLayer) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    Surface(
        modifier.width(240.dp).heightIn(min = 284.dp).testTag("layer-menu-panel"),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = EasyTripTheme.elevation.dialog,
    ) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(12.dp).pointerInput(Unit) { detectTapGestures { } }, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth().heightIn(min = 28.dp), verticalAlignment = Alignment.CenterVertically) {
                WorkspaceLayerIcon(Modifier.size(14.dp), MaterialTheme.colorScheme.primary)
                Text("地图图层", Modifier.weight(1f).padding(start = 8.dp), style = MaterialTheme.typography.labelMedium)
                Box(Modifier.size(28.dp).clickable(onClick = onClose).semantics { contentDescription = "关闭地图图层" }, contentAlignment = Alignment.Center) {
                    Text("×", style = MaterialTheme.typography.titleMedium)
                }
            }
            MapLayer.entries.forEach { option ->
                val selected = layer == option
                val rowShape = RoundedCornerShape(8.dp)
                Box(Modifier.fillMaxWidth()) {
                    if (selected) {
                        Box(
                            Modifier
                                .matchParentSize()
                                .border(2.dp, MaterialTheme.colorScheme.primary, rowShape)
                                .testTag("layer-selected-border-${option.name}"),
                        )
                    }
                    Row(
                        Modifier
                            .fillMaxWidth().heightIn(min = 58.dp)
                            .clip(rowShape)
                            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                            .then(
                                if (selected) Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = .12f), rowShape) else Modifier,
                            )
                            .testTag("layer-${option.name}")
                            .selectable(
                                selected = selected,
                                role = Role.RadioButton,
                                onClick = {
                                    onSelectLayer(option)
                                    onClose()
                                },
                            )
                            .border(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, rowShape)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.size(22.dp).clip(RoundedCornerShape(11.dp)).background(
                                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selected) WorkspaceCheckIcon(Modifier.size(14.dp), MaterialTheme.colorScheme.onPrimary)
                        }
                        Column(Modifier.padding(start = 10.dp)) {
                            Text(option.label(), style = MaterialTheme.typography.labelMedium)
                            Text(option.description(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Text("选择将应用到所有旅行，并在下次打开时保留。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

internal fun MapLayer.description() = when (this) {
    MapLayer.STANDARD -> "道路、建筑和地点信息"
    MapLayer.SATELLITE -> "仅显示卫星影像"
    MapLayer.SATELLITE_ROAD -> "卫星影像叠加道路"
}
