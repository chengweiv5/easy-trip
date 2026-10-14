package com.yangchengwei.easytrip.assistant.cityfirst

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yangchengwei.easytrip.assistant.*
import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.trip.ui.*
import com.yangchengwei.easytrip.core.ui.component.EasyTripPrimaryButton
import com.yangchengwei.easytrip.core.ui.component.EasyTripSecondaryButton
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.launch

@Composable
fun GuideCreationRoute(model: GuideCreationViewModel, onBack: () -> Unit, onCreated: (String) -> Unit) {
    val s by model.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var settings by rememberSaveable { mutableStateOf(false) }
    var leave by rememberSaveable { mutableStateOf(false) }
    var datePicker by rememberSaveable { mutableStateOf(false) }
    var daysPicker by rememberSaveable { mutableStateOf(false) }
    var navigationError by remember { mutableStateOf<String?>(null) }
    var navigateAttempt by remember { mutableIntStateOf(0) }
    var preview by remember { mutableStateOf<String?>(null) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(20), model::selectImages)
    val back = {
        when {
            s.saving -> Unit
            settings -> settings = false
            s.busy -> model.cancel()
            s.confirming && !s.locked -> model.backToInput()
            s.hasDraft || s.locked -> leave = true
            else -> onBack()
        }
    }
    BackHandler(onBack = back)
    LaunchedEffect(s.createdTripId, navigateAttempt) {
        s.createdTripId?.let { id ->
            try { onCreated(id); model.acknowledge() }
            catch (_: Exception) { navigationError = "旅行已创建，尚未完成打开。请重试，不会重复创建。" }
        }
    }
    if (settings) { AssistantSettings(model.config) { settings = false }; return }
    val editable = !s.busy && !s.saving && !s.recovering && !s.locked
    val large = LocalDensity.current.fontScale >= 1.5f
    TripDateRangePickerModalHost(datePicker, background = {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding().imePadding().testTag("global-assistant")) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(back, enabled = !s.saving, modifier = Modifier.background(
                    MaterialTheme.colorScheme.surface, androidx.compose.foundation.shape.CircleShape)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                }
                Column(Modifier.weight(1f)) {
                    Text(if (s.confirming) "创建旅行" else "旅行助手", style = MaterialTheme.typography.titleLarge)
                    Text(if (s.confirming) "日期可选，创建后导入攻略" else "从攻略创建旅行", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (!s.confirming) IconButton({ settings = true }, enabled = !s.busy && !s.saving) {
                    Icon(Icons.Outlined.Settings, "助手设置")
                }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                when {
                    s.recovering -> { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("正在恢复旅行信息…") }
                    s.recoveryFailed -> {
                        Text(s.error.orEmpty())
                        Button(model::retryRecovery) { Text("重试恢复") }
                        TextButton(onBack) { Text("返回我的旅行") }
                    }
                    s.deleted -> { Text(s.error.orEmpty()); Button({ scope.launch { model.discard(); onBack() } }) { Text("返回我的旅行") } }
                    s.createdTripId != null -> {
                        Text(navigationError ?: "旅行已创建，正在打开助手…")
                        Button({ navigateAttempt++ }) { Text("打开旅行助手") }
                    }
                    s.confirming -> {
                        Text("目的地城市", style = MaterialTheme.typography.titleSmall)
                        OutlinedTextField(s.cityText, model::editCities, enabled = editable,
                            trailingIcon = { Icon(Icons.Outlined.Edit, null) }, shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedContainerColor = MaterialTheme.colorScheme.surface),
                            placeholder = { Text("填写城市；多个城市用顿号分隔") },
                            supportingText = if (s.cityText.isBlank()) ({ Text("请填写目的地城市后创建") }) else null,
                            isError = s.cityText.isBlank(), modifier = Modifier.fillMaxWidth().testTag("guide-city-field"))
                        Text("旅行名称", style = MaterialTheme.typography.titleSmall)
                        OutlinedTextField(s.name, model::editName, enabled = editable, maxLines = 3,
                            leadingIcon = {
                                val color = MaterialTheme.colorScheme.onSurfaceVariant
                                Canvas(Modifier.size(20.dp)) {
                                    val stroke = Stroke(1.5.dp.toPx())
                                    drawRoundRect(color, Offset(size.width * .2f, size.height * .25f),
                                        Size(size.width * .6f, size.height * .65f), style = stroke)
                                    drawRoundRect(color, Offset(size.width * .38f, size.height * .1f),
                                        Size(size.width * .24f, size.height * .15f), style = stroke)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedContainerColor = MaterialTheme.colorScheme.surface),
                            isError = s.name.isBlank(), modifier = Modifier.fillMaxWidth().testTag("trip-draft-name"))
                        Text("出行日期（可选）", style = MaterialTheme.typography.titleSmall)
                        EasyTripSecondaryButton({ datePicker = true }, enabled = editable,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("trip-draft-date")) {
                            DateFieldIcon(Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                                Text(s.startDate?.let { "$it 至 ${it.plusDays(s.dayCount - 1L)}" } ?: "日期未定")
                                if (s.startDate == null) Text("不填写也可以创建旅行", style = MaterialTheme.typography.bodySmall)
                            }; Text("›")
                        }
                        if (s.startDate != null) TextButton({ model.editDates(null) }, enabled = editable,
                            modifier = Modifier.testTag("trip-draft-clear-date")) { Text("清空日期") }
                        EasyTripSecondaryButton({ daysPicker = true }, enabled = editable,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("trip-draft-days")) {
                            Text("旅行天数", Modifier.weight(1f))
                            Text("${s.dayCount} 天", color = MaterialTheme.colorScheme.primary)
                            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) { Text("›") }
                        }
                        Text("出行方式", style = MaterialTheme.typography.titleSmall)
                        val tile: @Composable (TravelMode, Modifier) -> Unit = { mode, modifier ->
                            Surface(onClick = { model.editMode(mode) }, enabled = editable, modifier = modifier.heightIn(min = 72.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = if (s.travelMode == mode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                contentColor = if (s.travelMode == mode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface) {
                                Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    TravelModeSymbol(mode, Modifier.size(20.dp))
                                    Spacer(Modifier.height(4.dp))
                                    Text(if (mode == TravelMode.FLEXIBLE) "灵活出行" else "自驾")
                                    Text(if (mode == TravelMode.FLEXIBLE) "步行与公共交通" else "以驾车为主", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        if (large) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            tile(TravelMode.FLEXIBLE, Modifier.fillMaxWidth()); tile(TravelMode.SELF_DRIVE, Modifier.fillMaxWidth())
                        } else Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            tile(TravelMode.FLEXIBLE, Modifier.weight(1f)); tile(TravelMode.SELF_DRIVE, Modifier.weight(1f))
                        }
                        s.warning?.let { Text(it) }
                        EasyTripPrimaryButton(model::create, enabled = s.canCreate, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("trip-draft-submit")) {
                            Text(if (s.saving) "正在创建…" else "创建并导入攻略")
                        }
                    }
                    else -> {
                        Text("从一篇攻略开始", style = MaterialTheme.typography.headlineSmall)
                        Text("先确认目的地，再把攻略里的地点加入旅行。")
                        Column(Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(12.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(s.input, model::editInput, enabled = editable, minLines = 3, maxLines = 8,
                                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color.Transparent,
                                    focusedBorderColor = Color.Transparent, disabledBorderColor = Color.Transparent),
                                placeholder = { Text("粘贴携程／小红书分享文案或链接…\n\n也可以补充：我想去青州玩三天。") },
                                modifier = Modifier.fillMaxWidth().testTag("assistant-input"))
                            AssistantImageInput(PlaceAssistantState(images = s.images, busy = !editable),
                                { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                { preview = it }, model::removeImage, model.materials.images, model.sessionId, compact = true)
                        }
                        Text("每批1条链接 · 最多20张图", style = MaterialTheme.typography.bodySmall)
                        Text("可补充你的出行日期，也可以以后再定。", style = MaterialTheme.typography.bodySmall)
                        if (s.busy) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("正在识别目的地…") }
                        EasyTripPrimaryButton(if (s.busy) model::cancel else model::identify,
                            enabled = s.busy || editable && (s.input.isNotBlank() || s.images.isNotEmpty()),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("guide-identify-destination")) {
                            Text(if (s.busy) "取消识别" else "识别目的地")
                        }
                        Text("点击后才读取链接，并将图文发送至已配置模型，可能产生费用。", style = MaterialTheme.typography.bodySmall)
                    }
                }
                s.error?.takeUnless { s.deleted }?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }
    }, sheet = {
        TripDateRangePickerSheet(DateRangeSelection(s.startDate, s.startDate?.plusDays(s.dayCount - 1L)),
            allowStartOnly = true, title = "选择出行日期 · 仅选开始日暂按1天",
            onConfirm = { range ->
                val start = requireNotNull(range.startDate)
                model.editDates(start, (ChronoUnit.DAYS.between(start, range.endDate ?: start) + 1).toInt())
                datePicker = false
            }, onDismiss = { datePicker = false })
    })
    if (daysPicker) {
        var text by remember { mutableStateOf(s.dayCount.toString()) }
        val days = text.toIntOrNull()
        val validDays = days != null && days in 1..30
        AlertDialog(onDismissRequest = { daysPicker = false }, title = { Text("旅行天数") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(text, { text = it.filter(Char::isDigit).take(3) },
                        label = { Text("天数（1–30）") }, singleLine = true, isError = !validDays,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.testTag("trip-days-input"))
                    if (!validDays) Text("请输入1–30天", color = MaterialTheme.colorScheme.error)
                    val startDate = s.startDate
                    if (startDate != null && validDays) Text("结束日期：${startDate.plusDays(days!! - 1L)}",
                        Modifier.testTag("trip-days-end-date"))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button({ text = (days!! - 1).toString() }, enabled = validDays && days!! > 1,
                            modifier = Modifier.testTag("trip-days-minus")) { Text("−") }
                        Button({ text = (days!! + 1).toString() }, enabled = validDays && days!! < 30,
                            modifier = Modifier.testTag("trip-days-plus")) { Text("＋") }
                    }
                }
            },
            confirmButton = { TextButton({ model.editDays(days!!); daysPicker = false }, enabled = validDays,
                modifier = Modifier.testTag("trip-days-confirm")) { Text("确认") } },
            dismissButton = { TextButton({ daysPicker = false }) { Text("取消") } })
    }
    if (leave) AlertDialog(onDismissRequest = { leave = false }, title = { Text("离开本次创建？") },
        text = { Text("未创建的草稿将放弃，已经创建的旅行不会删除。") },
        confirmButton = { TextButton({ scope.launch { model.discard(); onBack() } }) { Text("离开") } },
        dismissButton = { TextButton({ leave = false }) { Text("继续编辑") } })
    preview?.let { id -> s.images.find { it.id == id }?.let {
        AssistantImagePreview(model.materials.images, model.sessionId, it) { preview = null }
    } }
}
