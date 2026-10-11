package com.yangchengwei.easytrip.workspace

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.amap.PlaceSearchDataSource

@Composable
fun MapPlaceDialog(
    poi: MapPoiUi,
    source: PlaceSearchDataSource?,
    isSaved: Boolean,
    collectionBusy: Boolean,
    collectionError: String?,
    onToggleCollection: (PlaceCandidate) -> Unit,
    onDismiss: () -> Unit,
    occurrences: List<String> = emptyList(),
    showCollection: Boolean = true,
) {
    val scope = rememberCoroutineScope()
    val controller = remember(poi, source) { MapPlaceDetailsController(scope, source) }
    DisposableEffect(controller) {
        controller.select(poi)
        onDispose { controller.select(null) }
    }
    val state by controller.state.collectAsState()
    val current = state ?: MapPlaceDetailsState(poi,
        if (poi.address.isNotBlank()) MapPlaceAddressStatus.READY else MapPlaceAddressStatus.LOADING)
    val canCollect = controller.collectionCandidate(allowWhileLoading = isSaved) != null && !collectionBusy
    val density = LocalDensity.current
    val windowHeight = with(density) { LocalWindowInfo.current.containerSize.height.toDp() }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)
                .widthIn(max = 350.dp).fillMaxWidth()
                .heightIn(max = (windowHeight - 48.dp).coerceAtLeast(100.dp))
                .testTag("map-place-card"),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 6.dp,
        ) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Box(
                        Modifier.weight(1f).heightIn(min = 48.dp).padding(end = 8.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text(
                            poi.name.ifBlank { "未命名地点" },
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "关闭地点详情", modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    when (current.status) {
                        MapPlaceAddressStatus.LOADING -> "正在获取地址…"
                        MapPlaceAddressStatus.READY -> current.poi.address
                        MapPlaceAddressStatus.FAILED -> "地址获取失败"
                        MapPlaceAddressStatus.UNAVAILABLE -> if (poi.poiId.isNullOrBlank()) "该地点未提供可查询的编号" else "该地点未提供地址"
                    },
                    modifier = Modifier.fillMaxWidth().testTag("place-card-address"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (current.status == MapPlaceAddressStatus.FAILED) {
                    TextButton(onClick = controller::retry, modifier = Modifier.heightIn(min = 48.dp)) { Text("重试") }
                }
                occurrences.forEach {
                    Text(it, Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodyMedium)
                }
                collectionError?.let {
                    Text(it, Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error)
                }
                if (showCollection) {
                    Spacer(Modifier.height(20.dp))
                    MapPlaceCollectionButton(
                        modifier = Modifier.align(Alignment.End),
                        label = when {
                            poi.poiId.isNullOrBlank() -> "无法收藏"
                            collectionBusy -> if (isSaved) "正在取消…" else "正在收藏…"
                            isSaved -> "取消收藏"
                            else -> "收藏地点"
                        },
                        secondary = isSaved,
                        enabled = canCollect,
                        onClick = { controller.collectionCandidate(allowWhileLoading = isSaved)?.let(onToggleCollection) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MapPlaceCollectionButton(
    label: String,
    secondary: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = if (secondary) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primary
    val foreground = if (secondary) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary
    Box(
        modifier.widthIn(min = 112.dp).heightIn(min = 48.dp)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .testTag("place-card-collection"),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            Modifier.padding(vertical = 6.dp).widthIn(min = 112.dp).heightIn(min = 36.dp)
                .testTag("place-card-button-visual"),
            shape = RoundedCornerShape(8.dp),
            color = if (enabled) background else background.copy(alpha = 0.55f),
            contentColor = if (enabled) foreground else foreground.copy(alpha = 0.5f),
            border = if (secondary) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
        ) {
            Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                Text(label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
            }
        }
    }
}
