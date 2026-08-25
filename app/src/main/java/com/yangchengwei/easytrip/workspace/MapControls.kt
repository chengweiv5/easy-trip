package com.yangchengwei.easytrip.workspace

import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme

@Composable
fun MapControls(
    layer: MapLayer,
    overlay: WorkspaceOverlay,
    onOpenLayerMenu: () -> Unit,
    onCloseOverlay: () -> Unit,
    onSelectLayer: (MapLayer) -> Unit,
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
            Modifier.padding(top = controlGap).size(controlSize).testTag("layer-menu").semantics { contentDescription = "地图图层" }.clickable(onClick = onOpenLayerMenu),
            shape = RoundedCornerShape(controlSize / 2), color = MaterialTheme.colorScheme.surface, shadowElevation = EasyTripTheme.elevation.floating,
        ) { Box(contentAlignment = Alignment.Center) { WorkspaceLayerIcon(Modifier.size(iconSize)) } }
        if (overlay == WorkspaceOverlay.LayerMenu) {
            Surface(
                Modifier.padding(top = controlGap).widthIn(max = 280.dp).testTag("layer-menu-panel"),
                shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = EasyTripTheme.elevation.dialog,
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("地图图层", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Box(
                            Modifier.size(controlSize).testTag("layer-menu-close")
                                .semantics { contentDescription = "关闭图层菜单" }
                                .clickable(onClick = onCloseOverlay),
                            contentAlignment = Alignment.Center,
                        ) { WorkspaceCloseIcon(Modifier.size(iconSize)) }
                    }
                    MapLayer.entries.forEach { option ->
                        val selected = layer == option
                        Row(
                            Modifier.fillMaxWidth().testTag("layer-${option.name}").selectable(
                                selected = selected,
                                role = Role.RadioButton,
                                onClick = {
                                    onSelectLayer(option)
                                    onCloseOverlay()
                                },
                            ).padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (selected) WorkspaceCheckIcon(Modifier.padding(end = 8.dp).size(iconSize))
                            Column {
                                Text(option.label())
                                Text(option.description(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Text("选择将应用到所有旅行，并在下次打开时保留。", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

internal fun MapLayer.description() = when (this) {
    MapLayer.STANDARD -> "道路、建筑和地点信息"
    MapLayer.SATELLITE_ROAD -> "卫星影像叠加道路"
}
