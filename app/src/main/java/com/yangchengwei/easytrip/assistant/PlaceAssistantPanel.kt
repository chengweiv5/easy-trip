package com.yangchengwei.easytrip.assistant

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.filled.KeyboardArrowDown

@Composable
fun AssistantPanelHeader(host: AssistantWorkspace) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("✦ 助手", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        IconButton(host.onSettings, enabled = !host.state.busy && !host.state.saving) {
            Icon(Icons.Outlined.Settings, contentDescription = "助手设置")
        }
        IconButton({ host.model.hide() }, enabled = !host.state.saving, modifier = Modifier.testTag("assistant-collapse")) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "收起助手")
        }
    }
}

@Composable
fun PlaceAssistantPanel(host: AssistantWorkspace, modifier: Modifier = Modifier) {
    val s = host.state
    val controller = host.controller
    var clearPrompt by remember { mutableStateOf(false) }
    var batchCityPrompt by remember { mutableStateOf(false) }
    var batchCity by remember { mutableStateOf("") }
    val missingCityCount = s.items.count { it.status == IntakeStatus.NEED_CITY && it.intent.city.isBlank() }
    var previewId by remember { mutableStateOf<String?>(null) }
    var pickerRevision by remember { mutableLongStateOf(-1L) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(6)) {
        if (pickerRevision == controller.draftRevision) host.model.selectImages(it)
    }
    Column(modifier.fillMaxSize().imePadding().testTag("assistant-panel")) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!s.configured) {
                Text("先配置模型服务，再开始标记。普通搜索和已有地点不受影响。")
                Button(host.onSettings, Modifier.fillMaxWidth().testTag("assistant-open-settings")) { Text("前往助手设置") }
            }
            if (!s.consent) {
                Text("查询和地图标记需要你先授权地图服务。")
                Button(host.onConsent, Modifier.fillMaxWidth()) { Text("查看地图授权") }
            }
            if (s.items.isEmpty()) {
                OutlinedTextField(s.city, controller::editCity, label = { Text("默认搜索城市") },
                    placeholder = { Text(if (s.resolvingCity) "正在获取旅行城市…" else "请填写搜索城市") },
                    supportingText = { Text(if (s.resolvingCity) "进入助手即获取旅行城市，无需先搜索。也可直接手动填写。"
                        else if (s.city.isBlank()) "未能确认旅行城市，请手动填写；不会使用手机所在地。"
                        else "按当前旅行填入，可手动修改；不会使用手机所在地。") },
                    singleLine = true, enabled = !s.busy, modifier = Modifier.fillMaxWidth().testTag("assistant-city"))
                OutlinedTextField(s.input, controller::editInput, label = { Text("把想去的地点一次告诉我") },
                    placeholder = { Text("例如：杭州的灵隐寺、河坊街、雷峰塔") }, minLines = 3, maxLines = 6,
                    enabled = !s.busy, supportingText = { Text("${s.input.length} / 2000 · 每批最多 20 个地点") },
                    modifier = Modifier.fillMaxWidth().testTag("assistant-input"))
                Text("默认采用第一条有效结果，可在地图查看、更换，勾选后收藏。", style = MaterialTheme.typography.bodySmall)
                if (host.model.images != null) AssistantImageInput(s,
                    {
                        pickerRevision = controller.draftRevision
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    { previewId = it }, host.model::removeImage, host.model.images, host.model.sessionId)
                Text("点击即发送至助手设置中的模型服务，仅发送本次所选图片、文字和必要城市。文字与图片分别处理，可能产生多次调用费用。", style = MaterialTheme.typography.bodySmall)
                Button(controller::submit, enabled = s.configured && s.consent && !s.busy && !s.resolvingCity && !s.attachmentSelectionBusy &&
                    (s.input.isNotBlank() || s.images.isNotEmpty()),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("assistant-submit")) {
                    Text(if (s.busy) "正在理解地点…" else "全部标记到地图")
                }
            }
            s.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("assistant-error")) }
            s.sourceProgress.filter { s.busy || it.stage != SourceStage.COMPLETE }.forEach { progress ->
                val name = if (progress.sourceId == "text") "文字" else "图 ${s.images.find { it.id == progress.sourceId }?.ordinal ?: "?"}"
                Text("$name · ${when (progress.stage) {
                    SourceStage.SELECTED -> "等待处理"
                    SourceStage.PREPARING -> "正在本地准备"
                    SourceStage.SENDING -> "正在发送"
                    SourceStage.UNDERSTANDING -> "已发送，等待模型理解"
                    SourceStage.COMPLETE -> "理解完成"
                    SourceStage.FAILED -> progress.error ?: "处理失败"
                    SourceStage.CANCELLED -> "已停止，素材保留"
                }}", style = MaterialTheme.typography.bodySmall)
            }
            if (!s.busy && s.sourceProgress.any { it.stage == SourceStage.FAILED || it.stage == SourceStage.CANCELLED }) {
                OutlinedButton({ controller.retryImages() }, enabled = !s.saving,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .testTag("assistant-retry-images")) { Text("仅重试失败 / 未完成素材") }
                Text("成功素材不重复发送，重试可能产生费用。", style = MaterialTheme.typography.bodySmall)
            }
            if (s.busy) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                if (s.items.isNotEmpty()) Text("地图查询已完成 ${s.items.count { it.status !in setOf(IntakeStatus.WAITING, IntakeStatus.QUERYING) }} / ${s.items.size} 项",
                    style = MaterialTheme.typography.bodySmall)
                OutlinedButton(controller::cancel, enabled = !s.saving,
                    modifier = Modifier.fillMaxWidth().testTag("assistant-cancel")) { Text("停止剩余处理") }
                Text("停止保留素材和已完成结果；已发送内容无法撤回。", style = MaterialTheme.typography.bodySmall)
            }
            if (!s.mapReady && s.items.isNotEmpty()) Text("地图尚未就绪，结果已保留；恢复后可收藏。", color = MaterialTheme.colorScheme.error)
            s.receipt?.let { receipt ->
                Text("${if (s.items.isEmpty()) "上次" else "本次"}已收藏 ${receipt.addedCount} 个地点",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("assistant-receipt"))
            }
            if (s.items.isNotEmpty()) {
                Text("找到 ${s.items.size} 个地点", style = MaterialTheme.typography.titleSmall)
                if (missingCityCount > 0) {
                    TextButton({ batchCity = s.city; batchCityPrompt = true }, enabled = !s.busy && !s.saving,
                        modifier = Modifier.testTag("assistant-supply-city")) { Text("$missingCityCount 项缺少城市 · 补充") }
                }
                s.items.forEach { item -> key(item.id) { AssistantResultRow(host, item) { previewId = it } } }
            }
            if (s.hasDraft && !s.saving) TextButton({ clearPrompt = true }) { Text("清空本批，输入新的地点") }
        }
        if (s.items.isNotEmpty()) AssistantSelectionBar(s, controller::selectAll, controller::saveSelected)
    }
    if (clearPrompt) AlertDialog(onDismissRequest = { clearPrompt = false }, title = { Text("清空这次输入和候选？") },
        text = { Text("未收藏候选将清除；已经收藏的地点不受影响。") },
        confirmButton = { TextButton({ host.model.clearDraft(); clearPrompt = false }) { Text("清空未收藏内容") } },
        dismissButton = { TextButton({ clearPrompt = false }) { Text("保留") } })
    if (batchCityPrompt) AlertDialog(
        onDismissRequest = { batchCityPrompt = false },
        title = { Text("补充本批搜索城市") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("仅重查缺少城市的 $missingCityCount 项，不重复理解原文，也不改变其他地点。")
                OutlinedTextField(batchCity, { batchCity = it.take(50) }, singleLine = true,
                    label = { Text("搜索城市") }, placeholder = { Text("例如：徐州") },
                    modifier = Modifier.fillMaxWidth().testTag("assistant-batch-city"))
            }
        },
        confirmButton = {
            TextButton({
                controller.supplyMissingCity(batchCity)
                batchCityPrompt = false
            }, enabled = batchCity.isNotBlank() && !s.busy && !s.saving && missingCityCount > 0,
                modifier = Modifier.testTag("assistant-batch-city-confirm")) {
                Text("补充并重查 $missingCityCount 项")
            }
        },
        dismissButton = { TextButton({ batchCityPrompt = false }) { Text("取消") } },
    )
    s.images.find { it.id == previewId }?.let { image ->
        host.model.images?.let { AssistantImagePreview(it, host.model.sessionId, image) { previewId = null } }
    }
}
