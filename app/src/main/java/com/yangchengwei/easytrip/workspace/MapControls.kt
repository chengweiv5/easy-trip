package com.yangchengwei.easytrip.workspace

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun MapControls(
    layer: MapLayer,
    overlay: WorkspaceOverlay,
    onOpenLayerMenu: () -> Unit,
    onCloseOverlay: () -> Unit,
    onSelectLayer: (MapLayer) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, horizontalAlignment = Alignment.End) {
        Surface(
            Modifier.size(48.dp).testTag("layer-menu").semantics { contentDescription = "地图图层" }.clickable(onClick = onOpenLayerMenu),
            shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp,
        ) { LayerIcon() }
        if (overlay == WorkspaceOverlay.LayerMenu) {
            Surface(
                Modifier.padding(top = 8.dp).fillMaxWidth().testTag("layer-menu-panel"),
                shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp,
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("地图图层", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Text("关闭", Modifier.clickable(onClick = onCloseOverlay))
                    }
                    MapLayer.entries.forEach { option ->
                        Column(
                            Modifier.fillMaxWidth().testTag("layer-${option.name}").clickable {
                                onSelectLayer(option)
                                onCloseOverlay()
                            }.padding(vertical = 12.dp),
                        ) {
                            Text(if (layer == option) "✓ ${option.label()}" else option.label())
                            Text(option.description(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
