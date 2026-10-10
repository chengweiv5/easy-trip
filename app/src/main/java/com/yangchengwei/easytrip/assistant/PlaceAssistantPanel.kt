package com.yangchengwei.easytrip.assistant

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.place.ui.PlaceCategorySelector

@Composable
fun AssistantPanelHeader(host: AssistantWorkspace) {
    val large = androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.3f
    if (large) {
        Column(Modifier.fillMaxWidth()) {
            Text("✦ 地点助手", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(host.onSettings, enabled = !host.state.busy && !host.state.saving) { Text("设置") }
                TextButton({ host.model.hide() }, enabled = !host.state.saving, modifier = Modifier.testTag("assistant-collapse")) { Text("收起") }
            }
        }
    } else Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("✦ 地点助手", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        TextButton(host.onSettings, enabled = !host.state.busy && !host.state.saving) { Text("设置") }
        TextButton({ host.model.hide() }, enabled = !host.state.saving, modifier = Modifier.testTag("assistant-collapse")) { Text("收起") }
    }
}

@Composable
fun PlaceAssistantPanel(host: AssistantWorkspace, modifier: Modifier = Modifier) {
    val s = host.state
    val controller = host.controller
    var clearPrompt by remember { mutableStateOf(false) }
    var consentPrompt by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var categoryId by remember { mutableStateOf<String?>(null) }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(bottom = 16.dp)
        .testTag("assistant-panel"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("当前旅行：${host.tripName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        if (!s.configured) {
            Text("先配置模型服务，再开始标记。普通搜索和已有地点不受影响。")
            Button(host.onSettings, Modifier.fillMaxWidth().testTag("assistant-open-settings")) { Text("前往助手设置") }
        }
        if (!s.consent) {
            Text("查询和地图标记需要你先授权地图服务。")
            Button(host.onConsent, Modifier.fillMaxWidth()) { Text("查看地图授权") }
        }
        if (s.items.isEmpty() && s.receipt == null) {
            OutlinedTextField(s.city, controller::editCity, label = { Text("默认搜索城市") }, placeholder = { Text("例如：杭州；也可在原文中写明城市") },
                singleLine = true, enabled = !s.busy, modifier = Modifier.fillMaxWidth().testTag("assistant-city"))
            OutlinedTextField(s.input, controller::editInput, label = { Text("把想去的地点一次告诉我") },
                placeholder = { Text("例如：杭州的灵隐寺、河坊街、雷峰塔") }, minLines = 3, maxLines = 6,
                enabled = !s.busy, supportingText = { Text("${s.input.length} / 2000 · 每批最多 20 个地点") },
                modifier = Modifier.fillMaxWidth().testTag("assistant-input"))
            Text("先统一标记明确位置，再逐个或勾选确认收藏。", style = MaterialTheme.typography.bodySmall)
            Button({
                if (!s.sendConsent) consentPrompt = true else controller.submit()
            }, enabled = s.configured && s.consent && !s.busy && s.input.isNotBlank(),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("assistant-submit")) {
                Text(if (s.busy) "正在理解地点…" else "全部标记到地图")
            }
        }
        s.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("assistant-error")) }
        if (s.busy) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text("已完成 ${s.items.count { it.status !in setOf(IntakeStatus.WAITING, IntakeStatus.QUERYING) }} / ${s.items.size} 项",
                style = MaterialTheme.typography.bodySmall)
            OutlinedButton(controller::cancel, Modifier.fillMaxWidth().testTag("assistant-cancel")) { Text("停止剩余查询") }
        }
        if (!s.mapReady && s.items.isNotEmpty()) Text("地图尚未就绪，已找到的结果保留；恢复地图后才可确认收藏。", color = MaterialTheme.colorScheme.error)
        when {
            s.receipt != null -> {
                Text("${if (s.items.isEmpty()) "上次" else "本次"}已收藏 ${s.receipt.addedCount} 个地点", style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.testTag("assistant-receipt"))
                val already = s.receipt.results.size - s.receipt.addedCount
                if (already > 0) Text("$already 项已在地点池，未覆盖原有内容。")
                Text("已保存到「${host.tripName}」地点池。未加入行程，不产生费用。")
                Button(controller::dismissReceipt, Modifier.fillMaxWidth()) { Text("继续查看本批地点") }
                OutlinedButton(host.onViewPool, Modifier.fillMaxWidth()) { Text("查看地点池") }
            }
            s.confirmation != null -> {
                Text("确认收藏这 ${s.confirmation.items.size} 个地点？", style = MaterialTheme.typography.titleLarge)
                Text("收藏到：${host.tripName} / 地点池", color = MaterialTheme.colorScheme.primary)
                s.confirmation.items.forEach { selected ->
                    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(selected.poi.name, fontWeight = FontWeight.SemiBold)
                            Text("${selected.poi.cityName ?: "城市待核对"} · ${selected.poi.address}")
                            TextButton({ categoryId = selected.itemId }, enabled = !s.saving) { Text("分类：${selected.category.label} · 更改") }
                        }
                    }
                }
                Text("仅收藏所选地点。不加入行程，不产生费用。保存失败时整批不新增。", style = MaterialTheme.typography.bodySmall)
                Button(controller::confirm, enabled = !s.saving && s.mapReady && s.consent,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("assistant-confirm")) {
                    Text(if (s.saving) "正在收藏…" else "确认收藏 ${s.confirmation.items.size} 个地点")
                }
                OutlinedButton(controller::backFromReview, enabled = !s.saving, modifier = Modifier.fillMaxWidth()) { Text("返回调整选择") }
            }
            s.items.isNotEmpty() -> {
                Text("${s.eligible.size} 个可收藏 · ${s.selected.size} 个已选", style = MaterialTheme.typography.titleMedium)
                if (host.focused != null) TextButton({ host.model.focus(null) }, Modifier.testTag("assistant-show-all")) { Text("查看本批全部标记") }
                s.items.forEach { item ->
                    Surface(shape = RoundedCornerShape(12.dp),
                        color = if (item.id in s.selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth().testTag("assistant-item-${item.label}")) {
                        Column(Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(item.label, Modifier.padding(end = 10.dp), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                Column(Modifier.weight(1f).clickable { host.model.focus(item.id) }) {
                                    Text(item.poi?.name ?: item.intent.query)
                                    Text(item.detail ?: when (item.status) {
                                        IntakeStatus.READY -> "${item.poi?.address.orEmpty()} · 未收藏"
                                        IntakeStatus.QUERYING -> "正在查询"
                                        IntakeStatus.WAITING -> "等待查询"
                                        else -> item.status.name
                                    }, style = MaterialTheme.typography.bodySmall)
                                }
                                if (item.status == IntakeStatus.READY) Checkbox(item.id in s.selected, { controller.toggle(item.id) },
                                    modifier = Modifier.testTag("assistant-select-${item.label}"))
                            }
                            if (host.focused == item.id || item.status == IntakeStatus.NEED_CITY) {
                                if (item.status == IntakeStatus.READY) {
                                    Text("${item.poi?.cityName.orEmpty()} · ${item.poi?.address.orEmpty()}")
                                    Row {
                                        TextButton({ categoryId = item.id }) { Text("分类：${item.category.label}") }
                                        TextButton({ controller.review(setOf(item.id)) }, enabled = s.mapReady) { Text("收藏此地点") }
                                    }
                                } else {
                                    item.candidates.forEachIndexed { index, poi ->
                                        OutlinedButton({ controller.choosePoi(item.id, poi.poiId); host.model.focus(item.id) },
                                            modifier = Modifier.fillMaxWidth().testTag("assistant-option-${item.label}-$index")) {
                                            Column(Modifier.fillMaxWidth()) {
                                                Text("${item.label}${index + 1}  ${poi.name}")
                                                Text("${poi.cityName ?: "城市信息缺失"} · ${poi.address}", style = MaterialTheme.typography.bodySmall)
                                                if (!sameCity(poi.cityName, item.intent.city)) Text("请确认这是你要找的城市/地址", style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }
                                }
                            }
                            if (item.cityConflict) TextButton({ controller.confirmCity(item.id) }, enabled = !s.busy) {
                                Text("确认此项在${item.intent.city}，继续查询")
                            }
                            if (item.status in setOf(IntakeStatus.NEED_CITY, IntakeStatus.NOT_FOUND, IntakeStatus.AMBIGUOUS, IntakeStatus.FAILED, IntakeStatus.CANCELLED)) {
                                Row {
                                    TextButton({ host.model.focus(item.id); editingId = item.id }, enabled = !s.busy) { Text("修改名称 / 城市") }
                                    if (item.status in setOf(IntakeStatus.CANCELLED, IntakeStatus.FAILED)) TextButton({ controller.retry(setOf(item.id)) },
                                        enabled = !s.busy) { Text("重试此项") }
                                }
                            }
                        }
                    }
                }
                if (s.eligible.isNotEmpty()) OutlinedButton(controller::selectAll, Modifier.fillMaxWidth().testTag("assistant-select-all")) {
                    Text(if (s.selected.size == s.eligible.size) "取消全选" else "全选可收藏的 ${s.eligible.size} 个")
                }
                if (s.selected.isNotEmpty()) Button({ controller.review() }, enabled = s.mapReady && s.consent,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("assistant-review")) {
                    Text("核对并收藏所选（${s.selected.size}）")
                }
                if (s.items.any { it.status in setOf(IntakeStatus.FAILED, IntakeStatus.CANCELLED) }) {
                    OutlinedButton({ controller.retry() }, enabled = !s.busy, modifier = Modifier.fillMaxWidth()) { Text("仅继续失败 / 未完成项") }
                }
                Text("勾选和全选不等于收藏；其余项不会被自动保存。", style = MaterialTheme.typography.bodySmall)
            }
        }
        if (s.hasDraft && !s.saving) TextButton({ clearPrompt = true }) { Text("清空本批，输入新的地点") }
    }
    if (consentPrompt) AlertDialog(onDismissRequest = { consentPrompt = false }, title = { Text("发送前，了解数据去向") },
        text = { Text("将本次地点文字和默认城市发送给助手设置中的模型服务，产生一次调用及费用。不发送完整旅行、已有费用、Cookie 或收藏授权。不会抓取其他 App 或链接。") },
        confirmButton = { TextButton({ controller.allowSend(); consentPrompt = false; controller.submit() }) { Text("同意并发送本次内容") } },
        dismissButton = { TextButton({ consentPrompt = false }) { Text("暂不发送") } })
    if (clearPrompt) AlertDialog(onDismissRequest = { clearPrompt = false }, title = { Text("清空这次输入和候选？") },
        text = { Text("未收藏候选将清除；已经收藏的地点不受影响。") },
        confirmButton = { TextButton({ controller.clear(); host.model.focus(null); clearPrompt = false }) { Text("清空未收藏内容") } },
        dismissButton = { TextButton({ clearPrompt = false }) { Text("保留") } })
    s.items.find { it.id == editingId }?.let { item ->
        var query by remember(item.id) { mutableStateOf(item.intent.query) }
        var city by remember(item.id) { mutableStateOf(item.intent.city) }
        AlertDialog(onDismissRequest = { editingId = null }, title = { Text("核对这一项") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(query, { query = it.take(100) }, label = { Text("地点名称") })
                OutlinedTextField(city, { city = it.take(50) }, label = { Text("搜索城市") })
            } },
            confirmButton = { TextButton({ controller.editItem(item.id, query, city); editingId = null; controller.retry(setOf(item.id)) },
                enabled = query.isNotBlank() && city.isNotBlank()) { Text("只重查这一项") } },
            dismissButton = { TextButton({ editingId = null }) { Text("取消") } })
    }
    s.items.find { it.id == categoryId }?.let { item ->
        AlertDialog(onDismissRequest = { categoryId = null }, title = { Text("地点分类") },
            text = { PlaceCategorySelector(item.category, true, { controller.category(item.id, it); categoryId = null }) },
            confirmButton = { TextButton({ categoryId = null }) { Text("返回") } })
    }
}
