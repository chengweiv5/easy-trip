package com.yangchengwei.easytrip.assistant

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.yangchengwei.easytrip.core.ui.component.EasyTripPrimaryButton
import com.yangchengwei.easytrip.core.ui.component.EasyTripSecondaryButton
import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.trip.ui.*
import java.time.temporal.ChronoUnit

@Composable
fun TripCreationForm(model: TripDraftViewModel, onSubmit: () -> Unit, onBack: () -> Unit,
    onRetryMetadata: () -> Unit = {}) {
    val s by model.state.collectAsState()
    var datePicker by rememberSaveable { mutableStateOf(false) }
    var daysPicker by rememberSaveable { mutableStateOf(false) }
    val editable = !s.saving && !s.submissionLocked
    val validName = s.name.trim().let { it.isNotEmpty() && it.length <= 100 && it.none(Char::isISOControl) }
    val nameError = if (!validName) "请填写1–100字的旅行名称" else null
    val largeFont = LocalDensity.current.fontScale >= 1.5f
    TripDateRangePickerModalHost(datePicker, background = {
        Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState())
            .padding(16.dp).testTag("trip-from-guide-form"), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("先收藏地点，日期可以以后再定", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("已选 ${s.selectionCount} 个地点", Modifier.weight(1f))
                    TextButton(onBack, enabled = editable) { Text("更改") }
                }
            }
            Text("旅行名称", style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(s.name, model::editName, enabled = editable,
                isError = nameError != null,
                supportingText = nameError?.let { { Text(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite }) } },
                singleLine = false, maxLines = 3, modifier = Modifier.fillMaxWidth().testTag("trip-draft-name"))
            Text("出行日期（可选）", style = MaterialTheme.typography.titleSmall)
            EasyTripSecondaryButton({ datePicker = true }, enabled = editable,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("trip-draft-date")) {
                Column(Modifier.weight(1f).padding(vertical = 12.dp)) {
                    Text(s.startDate?.let { "$it 至 ${it.plusDays(s.dayCount - 1L)}" } ?: "日期未定")
                    if (s.startDate == null) Text("不填写也可以创建旅行", style = MaterialTheme.typography.bodySmall)
                }
                Text("›")
            }
            if (s.startDate != null) TextButton({ model.editDates(null) }, enabled = editable,
                modifier = Modifier.testTag("trip-draft-clear-date")) { Text("清空日期") }
            EasyTripSecondaryButton({ daysPicker = true }, enabled = editable,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("trip-draft-days")) {
                Text("旅行天数", Modifier.weight(1f)); Text("${s.dayCount} 天"); Spacer(Modifier.width(16.dp)); Text("›")
            }
            Text("出行方式", style = MaterialTheme.typography.titleSmall)
            val modeTile: @Composable (TravelMode, Modifier) -> Unit = { mode, modifier ->
                    Surface(onClick = { model.editMode(mode) }, enabled = editable, shape = RoundedCornerShape(12.dp),
                        color = if (s.travelMode == mode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        contentColor = if (s.travelMode == mode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        modifier = modifier.heightIn(min = 72.dp)) {
                        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (mode == TravelMode.FLEXIBLE) "灵活出行" else "自驾")
                            Text(if (mode == TravelMode.FLEXIBLE) "步行与公共交通" else "以驾车为主", style = MaterialTheme.typography.bodySmall)
                        }
                    }
            }
            if (largeFont) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TravelMode.entries.reversed().forEach { modeTile(it, Modifier.fillMaxWidth()) }
            } else Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TravelMode.entries.reversed().forEach { modeTile(it, Modifier.weight(1f).fillMaxHeight()) }
            }
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("先建立 ${s.dayCount} 个空白日")
                    Text("${s.selectionCount} 个地点只加入地点池，后续再按天安排。")
                    if (s.startDate != null && s.dayCount == 1) Text("暂按1天建立，可修改")
                }
            }
            if (s.metadataBusy) Text("正在确认你补充的旅行日期…")
            s.warning?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (s.metadataRetryAvailable) TextButton(onRetryMetadata, enabled = editable && !s.metadataBusy,
                modifier = Modifier.testTag("trip-draft-retry-dates")) { Text("仅重试日期识别") }
            s.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("trip-draft-error")) }
            if (s.submissionLocked && !s.saving) TextButton(model::resumeEditing) { Text("核对结果并修改信息") }
            EasyTripPrimaryButton(onSubmit, enabled = validName && !s.saving && !s.recovering && !s.deleted,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("trip-draft-submit")) {
                Text(if (s.saving) "创建并收藏中…" else "创建并收藏")
            }
            Text("返回可修改地点，当前选择不会丢失。", style = MaterialTheme.typography.bodySmall)
        }
    }, sheet = {
        TripDateRangePickerSheet(initialSelection = DateRangeSelection(s.startDate,
            s.startDate?.plusDays(s.dayCount - 1L)), allowStartOnly = true,
            title = "选择出行日期 · 仅选开始日暂按1天",
            onConfirm = {
                val start = requireNotNull(it.startDate)
                val days = (ChronoUnit.DAYS.between(start, it.endDate ?: start) + 1).toInt()
                model.editDates(start, days); datePicker = false
            }, onDismiss = { datePicker = false })
    })
    if (daysPicker) {
        var daysText by remember { mutableStateOf(s.dayCount.toString()) }
        val days = daysText.toIntOrNull()
        val validDays = days != null && days in 1..30
        AlertDialog(onDismissRequest = { daysPicker = false }, title = { Text("旅行天数") },
            text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(daysText, { daysText = it.filter(Char::isDigit).take(3) },
                    label = { Text("天数（1–30）") }, singleLine = true, isError = !validDays,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.testTag("trip-days-input"))
                if (!validDays) Text("请输入1–30天", color = MaterialTheme.colorScheme.error)
                if (s.startDate != null && validDays) Text("结束日期：${s.startDate?.plusDays(days!! - 1L)}",
                    Modifier.testTag("trip-days-end-date"))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button({ daysText = (days!! - 1).toString() }, enabled = validDays && days!! > 1, modifier = Modifier.testTag("trip-days-minus")) { Text("−") }
                    Button({ daysText = (days!! + 1).toString() }, enabled = validDays && days!! < 30, modifier = Modifier.testTag("trip-days-plus")) { Text("＋") }
                }
            } }, confirmButton = { TextButton({ model.editDays(days!!); daysPicker = false }, enabled = validDays,
                modifier = Modifier.testTag("trip-days-confirm")) { Text("确认") } },
            dismissButton = { TextButton({ daysPicker = false }) { Text("取消") } })
    }
}
