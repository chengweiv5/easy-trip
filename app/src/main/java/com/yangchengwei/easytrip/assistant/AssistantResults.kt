package com.yangchengwei.easytrip.assistant

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.triStateToggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yangchengwei.easytrip.place.domain.PlaceCategory

/** Visual selection column is shared by rows and the pinned bulk action. */
@Composable
private fun AssistantSelectionBox(
    state: ToggleableState, enabled: Boolean, label: String, tag: String, onClick: () -> Unit,
) {
    Box(Modifier.size(48.dp).testTag(tag)
        .semantics { contentDescription = label }
        .triStateToggleable(state, enabled = enabled, role = Role.Checkbox, onClick = onClick),
        contentAlignment = Alignment.CenterStart) {
        TriStateCheckbox(state, onClick = null, enabled = enabled,
            modifier = Modifier.size(20.dp).testTag("$tag-box"))
    }
}

@Composable
internal fun AssistantSelectionBar(
    state: PlaceAssistantState, onSelectAll: () -> Unit, onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = state.selected.size
    val toggle = when {
        selected == 0 -> ToggleableState.Off
        selected == state.eligible.size -> ToggleableState.On
        else -> ToggleableState.Indeterminate
    }
    val large = LocalDensity.current.fontScale > 1.3f
    Box(modifier.fillMaxWidth().testTag("assistant-selection-bar")) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            val canSelect = !state.saving && state.eligible.isNotEmpty()
            Row(Modifier.sizeIn(minWidth = 86.dp, minHeight = 48.dp)
                .testTag("assistant-select-all")
                .semantics { contentDescription = "全选可收藏地点" }
                .triStateToggleable(toggle, enabled = canSelect, role = Role.Checkbox, onClick = onSelectAll),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TriStateCheckbox(toggle, onClick = null, enabled = canSelect,
                    modifier = Modifier.size(20.dp).testTag("assistant-select-all-box"))
                Text("全选", fontSize = if (large) 12.sp else 15.sp,
                    color = MaterialTheme.colorScheme.onSurface)
            }
            Button(onSave, enabled = selected > 0 && !state.saving && state.mapReady && state.consent,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("assistant-save")) {
                Text(when {
                    state.saving -> "收藏中…"
                    selected == 0 -> "收藏"
                    else -> "收藏（$selected）"
                }, fontSize = if (large) 13.sp else 16.sp)
            }
        }
    }
}

@Composable
internal fun AssistantResultRow(host: AssistantWorkspace, item: IntakeItem, onPreview: (String) -> Unit) {
    val s = host.state
    val controller = host.controller
    var editing by remember(item.id) { mutableStateOf(false) }
    var categories by remember(item.id) { mutableStateOf(false) }
    var candidates by remember(item.id) { mutableStateOf(false) }
    var sources by remember(item.id) { mutableStateOf(false) }
    val bringIntoView = remember { BringIntoViewRequester() }
    LaunchedEffect(host.focused, host.detail) {
        if (host.focused == item.id && host.detail != null) {
            categories = host.detail == AssistantDetail.CATEGORY
            candidates = host.detail == AssistantDetail.CANDIDATES
            bringIntoView.bringIntoView()
            host.model.consumeDetail()
        }
    }
    val ready = item.status == IntakeStatus.READY
    val attention = item.status in setOf(IntakeStatus.NEED_CITY, IntakeStatus.NOT_FOUND,
        IntakeStatus.AMBIGUOUS, IntakeStatus.FAILED, IntakeStatus.CANCELLED)
    val expanded = host.focused == item.id || editing || candidates || categories
    Column(Modifier.fillMaxWidth().bringIntoViewRequester(bringIntoView).testTag("assistant-item-${item.label}")) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (ready) AssistantSelectionBox(
                if (item.id in s.selected) ToggleableState.On else ToggleableState.Off,
                !s.saving, "选择${item.poi?.name ?: item.intent.query}", "assistant-select-${item.label}",
                { controller.toggle(item.id) })
            else Spacer(Modifier.size(48.dp))
            Box(Modifier.widthIn(min = 28.dp).heightIn(min = 48.dp)
                .clickable(enabled = !s.saving) { host.model.focus(item.id) }
                .testTag("assistant-marker-${item.label}"), contentAlignment = Alignment.Center) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                    Box(Modifier.sizeIn(minWidth = 28.dp, minHeight = 28.dp).padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center) {
                    Text(item.label, style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false)
                    }
                }
            }
            Column(Modifier.weight(1f).heightIn(min = 48.dp)
                .clickable(enabled = !s.saving) { host.model.focus(item.id) }
                .testTag("assistant-focus-${item.label}"),
                verticalArrangement = Arrangement.Center) {
                Text(item.poi?.name ?: item.intent.query, fontWeight = FontWeight.SemiBold)
                Text(when (item.status) {
                    IntakeStatus.READY -> listOfNotNull(item.poi?.cityName, item.poi?.address?.takeIf { it.isNotBlank() })
                        .distinct().joinToString(" · ")
                    IntakeStatus.ALREADY_SAVED -> "已收藏"
                    IntakeStatus.DUPLICATE -> "与本批地点重复"
                    IntakeStatus.QUERYING -> "正在查询"
                    IntakeStatus.WAITING -> "等待查询"
                    else -> item.detail ?: "待处理"
                }, style = MaterialTheme.typography.bodySmall,
                    color = if (item.status == IntakeStatus.ALREADY_SAVED) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (expanded || attention) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (ready) TextButton({ categories = !categories }, enabled = !s.saving,
                    modifier = Modifier.testTag("assistant-category-${item.label}")) { Text("分类：${item.category.label}") }
                if (item.candidates.isNotEmpty() && (item.poi == null || item.candidates.size > 1)) {
                    TextButton({ candidates = !candidates }, enabled = !s.saving,
                        modifier = Modifier.testTag("assistant-change-${item.label}")) {
                        Text(if (candidates) "收起候选" else "更换地点")
                    }
                }
                if (attention) TextButton({ editing = !editing; host.model.focus(item.id) },
                    enabled = !s.busy && !s.saving,
                    modifier = Modifier.testTag("assistant-edit-${item.label}")) { Text("修改名称 / 城市") }
                if (item.sources.isNotEmpty()) TextButton({ sources = !sources }, enabled = !s.saving,
                    modifier = Modifier.testTag("assistant-sources-${item.label}")) { Text("查看来源") }
                if (item.status in setOf(IntakeStatus.FAILED, IntakeStatus.CANCELLED)) {
                    TextButton({ controller.retry(setOf(item.id)) }, enabled = !s.busy && !s.saving) { Text("重试此项") }
                }
                if (expanded) TextButton({
                    editing = false; categories = false; candidates = false; sources = false; host.model.focus(null)
                }, enabled = !s.saving, modifier = Modifier.testTag("assistant-show-all")) { Text("收起详情") }
            }
        }
        if (sources) item.sources.distinct().forEach { source ->
            when (source) {
                is IntakeSource.Image -> TextButton({ onPreview(source.imageId) }, enabled = !s.saving) {
                    Text("图 ${source.ordinal} · ${source.excerpt.take(60)} · 回看来源")
                }
                is IntakeSource.Text -> Text("原文：${source.span}", style = MaterialTheme.typography.bodySmall)
            }
        }
        if (categories && ready) {
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PlaceCategory.entries.forEach { category ->
                    FilterChip(selected = item.category == category,
                        onClick = { controller.category(item.id, category); categories = false },
                        enabled = !s.saving, label = { Text(category.label) },
                        modifier = Modifier.heightIn(min = 48.dp).testTag("assistant-category-${item.label}-${category.storageKey}"))
                }
            }
        }
        if ((candidates || (expanded && item.poi == null)) && item.candidates.isNotEmpty()) {
            item.candidates.forEachIndexed { index, poi ->
                OutlinedButton({ controller.choosePoi(item.id, poi.poiId); host.model.focus(item.id) },
                    enabled = !s.saving,
                    modifier = Modifier.fillMaxWidth().testTag("assistant-option-${item.label}-$index")) {
                    Column(Modifier.fillMaxWidth()) {
                        Text("${item.label}${index + 1}  ${poi.name}${if (item.poi?.poiId == poi.poiId) " · 已采用" else ""}")
                        Text("${poi.cityName ?: "城市信息缺失"} · ${poi.address}", style = MaterialTheme.typography.bodySmall)
                        if (!sameCity(poi.cityName, item.intent.city)) Text("请确认城市和地址", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        if (item.cityConflict) TextButton({ controller.confirmCity(item.id) }, enabled = !s.busy && !s.saving) {
            Text("此项在${item.intent.city}，继续查询")
        }
        if (editing) {
            var query by remember(item.id) { mutableStateOf(item.intent.query) }
            var city by remember(item.id) { mutableStateOf(item.intent.city) }
            Column(Modifier.fillMaxWidth().testTag("assistant-edit-fields-${item.label}"),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(query, { query = it.take(100) }, label = { Text("地点名称") },
                    enabled = !s.busy && !s.saving, modifier = Modifier.fillMaxWidth().testTag("assistant-edit-name-${item.label}"))
                OutlinedTextField(city, { city = it.take(50) }, label = { Text("搜索城市") },
                    enabled = !s.busy && !s.saving, modifier = Modifier.fillMaxWidth().testTag("assistant-edit-city-${item.label}"))
                Row {
                    TextButton({
                        controller.editItem(item.id, query, city)
                        controller.retry(setOf(item.id)); editing = false
                    }, enabled = query.isNotBlank() && city.isNotBlank() && !s.busy && !s.saving,
                        modifier = Modifier.testTag("assistant-edit-retry-${item.label}")) { Text("只重查这一项") }
                    TextButton({ editing = false }, enabled = !s.saving) { Text("取消") }
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .4f))
    }
}

@Composable
fun AssistantFocusedPlaceCard(host: AssistantWorkspace, modifier: Modifier = Modifier) {
    val item = host.state.items.find { it.id == host.focused } ?: return
    val poi = item.poi ?: return
    Surface(modifier.testTag("assistant-map-card"), shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(poi.name, fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleSmall, modifier = Modifier.testTag("assistant-map-card-name"))
            Text(listOfNotNull(poi.cityName, poi.address.takeIf { it.isNotBlank() }).distinct().joinToString(" · "),
                style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("assistant-map-card-address"))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (item.status == IntakeStatus.READY) {
                    TextButton({ host.model.showDetail(item.id, AssistantDetail.CATEGORY) }, enabled = !host.state.saving,
                        modifier = Modifier.testTag("assistant-map-category")) { Text(item.category.label) }
                    if (item.candidates.size > 1) TextButton({
                        host.model.showDetail(item.id, AssistantDetail.CANDIDATES)
                    }, enabled = !host.state.saving, modifier = Modifier.testTag("assistant-map-change")) { Text("更换") }
                }
                TextButton({ host.model.focus(null) }, enabled = !host.state.saving) { Text("收起") }
            }
        }
    }
}
