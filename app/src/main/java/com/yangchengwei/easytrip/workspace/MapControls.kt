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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
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
) {
    val controlSize = EasyTripTheme.sizes.workspaceDenseTouchTarget
    val iconSize = EasyTripTheme.sizes.workspaceIconSize
    val controlGap = EasyTripTheme.spacing.workspaceControlGap
    Column(modifier, horizontalAlignment = Alignment.End) {
        Surface(
            Modifier.size(controlSize).testTag("workspace-locate").semantics { contentDescription = "定位" }.clickable(onClick = onLocate),
            shape = RoundedCornerShape(controlSize / 2), color = MaterialTheme.colorScheme.surface, shadowElevation = EasyTripTheme.elevation.floating,
        ) { Box(contentAlignment = Alignment.Center) { WorkspaceLocateIcon(Modifier.size(iconSize)) } }
        Surface(
            Modifier.padding(top = controlGap).size(controlSize).testTag("layer-menu").semantics {
                contentDescription = "地图图层"
                selected = active
            }.clickable(onClick = onOpenLayerMenu),
            shape = RoundedCornerShape(controlSize / 2),
            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            shadowElevation = EasyTripTheme.elevation.floating,
        ) {
            Box(contentAlignment = Alignment.Center) {
                WorkspaceLayerIcon(Modifier.size(iconSize), if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
internal fun MapLayerMenu(
    layer: MapLayer,
    onClose: () -> Unit,
    onSelectLayer: (MapLayer) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    Surface(
        modifier.width(240.dp).height(289.dp).testTag("layer-menu-panel"),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = EasyTripTheme.elevation.dialog,
    ) {
        Column(Modifier.padding(14.dp).pointerInput(Unit) { detectTapGestures { } }, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            MapLayer.entries.forEach { option ->
                val selected = layer == option
                val rowShape = RoundedCornerShape(10.dp)
                Box(Modifier.fillMaxWidth().height(62.dp)) {
                    if (selected) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .border(2.dp, MaterialTheme.colorScheme.primary, rowShape)
                                .testTag("layer-selected-border-${option.name}"),
                        )
                    }
                    Row(
                        Modifier
                            .fillMaxSize()
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
                            .padding(horizontal = 12.dp),
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
                        Text(option.label(), Modifier.padding(start = 10.dp), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Text("选择将应用到所有旅行，并在下次打开时保留。", style = MaterialTheme.typography.labelSmall)
        }
    }
}

internal fun MapLayer.description() = when (this) {
    MapLayer.STANDARD -> "道路、建筑和地点信息"
    MapLayer.SATELLITE -> "仅显示卫星影像"
    MapLayer.SATELLITE_ROAD -> "卫星影像叠加道路"
}
