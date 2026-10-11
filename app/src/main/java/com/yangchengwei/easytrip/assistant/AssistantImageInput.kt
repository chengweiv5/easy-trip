package com.yangchengwei.easytrip.assistant

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.draw.clipToBounds

@Composable
fun AssistantImageInput(state: PlaceAssistantState, onPick: () -> Unit, onPreview: (String) -> Unit,
    onRemove: (String) -> Unit, store: AssistantImageStore? = null, sessionId: String = "") {
    val editable = !state.busy && !state.saving && state.items.isEmpty() && !state.attachmentSelectionBusy
    OutlinedButton(onPick, enabled = editable && state.images.size < 6,
        modifier = Modifier.heightIn(min = 48.dp).testTag("assistant-pick-images")) {
        Text(if (state.attachmentSelectionBusy) "正在读取所选图片…" else "添加图片 · ${state.images.size}/6")
    }
    if (state.images.isNotEmpty()) LazyRow(Modifier.fillMaxWidth().testTag("assistant-image-list"),
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(state.images, key = { it.id }) { image ->
            Card(Modifier.width(124.dp)) {
                Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    val bitmap by produceState<android.graphics.Bitmap?>(null, store, sessionId, image.id) {
                        if (store != null) {
                            try { value = store.thumbnail(sessionId, image.id) }
                            catch (e: kotlinx.coroutines.CancellationException) { throw e }
                            catch (_: Exception) { value = null }
                        }
                    }
                    // Compose's render thread may retain an ImageBitmap after composition disposal.
                    // These 256px thumbnails are GC-owned; never recycle a displayed bitmap manually.
                    TextButton({ onPreview(image.id) }, Modifier.heightIn(min = 48.dp).testTag("assistant-preview-${image.id}")) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            bitmap?.let { Image(it.asImageBitmap(), "图 ${image.ordinal}，点击预览",
                                Modifier.size(80.dp), contentScale = ContentScale.Fit) }
                            Text("图 ${image.ordinal} · 预览")
                        }
                    }
                    TextButton({ onRemove(image.id) }, enabled = editable,
                        modifier = Modifier.heightIn(min = 44.dp).testTag("assistant-remove-${image.id}")) { Text("移除图 ${image.ordinal}") }
                }
            }
        }
    }
    Text("JPG / PNG / WebP 静态图 · 单张 ≤10 MB，总计 ≤30 MB。选图不会发送。",
        style = MaterialTheme.typography.bodySmall)
}

@Composable
fun AssistantImagePreview(store: AssistantImageStore, sessionId: String, image: AssistantImage, onClose: () -> Unit) {
    var scale by remember(image.id) { mutableFloatStateOf(1f) }
    var offset by remember(image.id) { mutableStateOf(Offset.Zero) }
    var error by remember(image.id) { mutableStateOf(false) }
    val bitmap by produceState<android.graphics.Bitmap?>(null, image.id) {
        try { value = store.preview(sessionId, image.id) }
        catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (_: Exception) { error = true }
    }
    // The renderer, not composition disposal, determines the last use of this bitmap.
    Dialog(onDismissRequest = onClose) {
        Surface(shape = MaterialTheme.shapes.large) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("图 ${image.ordinal} · 双指缩放查看", style = MaterialTheme.typography.titleMedium)
                Box(Modifier.fillMaxWidth().heightIn(min = 180.dp, max = 440.dp).weight(1f, fill = false)
                    .clipToBounds()
                    .pointerInput(image.id) { detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 5f)
                        offset = if (scale == 1f) Offset.Zero else offset + pan
                    } }, contentAlignment = Alignment.Center) {
                    bitmap?.let {
                        Image(it.asImageBitmap(), "图 ${image.ordinal} 原始内容",
                            Modifier.fillMaxWidth().graphicsLayer { scaleX = scale; scaleY = scale; translationX = offset.x; translationY = offset.y },
                            contentScale = ContentScale.Fit)
                    }
                    if (bitmap == null) Text(if (error) "图片已失效，请重新选择。" else "正在读取图片…")
                }
                Text("可见个人信息不会因移除元数据而消失；需要时先在系统相册处理后重选。", style = MaterialTheme.typography.bodySmall)
                TextButton({ scale = 1f; offset = Offset.Zero }) { Text("重置缩放") }
                Button(onClose, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("关闭预览") }
            }
        }
    }
}
