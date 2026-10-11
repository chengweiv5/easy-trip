package com.yangchengwei.easytrip.assistant

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.settings.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private tailrec fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}

@Composable
fun AssistantSettings(
    store: AssistantConfigStore,
    imageCheck: suspend (ProviderConfig) -> ImageCapabilityResult = ::checkImageCapability,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val initial = remember(store) { store.read() }
    var url by remember { mutableStateOf(initial?.baseUrl ?: "https://api.deepseek.com") }
    var model by remember { mutableStateOf(initial?.model ?: "deepseek-flash") }
    var key by remember { mutableStateOf(initial?.apiKey.orEmpty()) }
    var hasSaved by remember { mutableStateOf(initial != null) }
    var visible by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var checkJob by remember { mutableStateOf<Job?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    var capability by remember { mutableStateOf(store.imageCapability()) }
    val savedConfigMatches = runCatching { ProviderConfig(url, model, key).validated().sameAs(store.read()) }.getOrDefault(false)
    val window = LocalContext.current.activity()?.window
    DisposableEffect(window) {
        val wasSecure = window?.attributes?.flags?.and(WindowManager.LayoutParams.FLAG_SECURE) != 0
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { if (!wasSecure) window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }
    val leave = {
        checkJob?.cancel()
        onBack()
    }
    BackHandler(enabled = busy, onBack = leave)
    SettingsPage("助手设置", "assistant-settings", leave) {
        Hint("App 全局设置 · 所有旅行共用")
        SectionLabel("模型服务")
        Hint("配置自己的模型服务，无需先搭建网关。测试成功后保存，未通过时保留原配置。")
        OutlinedTextField(url, { url = it; key = ""; visible = false; message = "接口地址已修改，请重新填写用于该服务的 Key。" }, label = { Text("API Base URL") },
            singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth().testTag("assistant-url"))
        OutlinedTextField(model, { model = it.take(100); message = null }, label = { Text("模型名称") },
            singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth().testTag("assistant-model"))
        OutlinedTextField(key, { key = it.take(4096); message = null }, label = { Text("API Key") },
            singleLine = true, enabled = !busy, visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().testTag("assistant-key"),
            trailingIcon = { TextButton({ visible = !visible }) { Text(if (visible) "隐藏" else "显示") } })
        Hint("Key 仅加密保存在本机，不进入备份、日志或对话。此页面禁止系统截图。")
        message?.let { Text(it, Modifier.testTag("assistant-settings-result")) }
        Button(onClick = {
            busy = true; message = null; visible = false
            checkJob = scope.launch {
                try {
                    val config = ProviderConfig(url, model, key).validated()
                    val result = DeepSeekPlaceParser(config = { config }).parse("把杭州的雷峰塔标记出来", "杭州")
                    if (result.isEmpty()) throw AssistantFailure("未验证到有效工具参数。")
                    withContext(Dispatchers.IO) { store.save(config) }
                    hasSaved = true
                    capability = store.imageCapability()
                    message = "文字连接与工具格式验证通过，已保存；不代表图片可用。尚未查询真实地点。"
                } catch (e: CancellationException) { throw e }
                catch (e: Exception) { message = (e as? AssistantFailure)?.userMessage ?: "配置格式有误或无法安全保存，请检查后重试。" }
                finally { busy = false }
            }
        }, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("assistant-test-save")) {
            Text(if (busy) "正在测试…" else "测试连接并保存")
        }
        Hint("测试将发送固定的杭州地点文字，产生一次模型调用及费用；不发送你的旅行内容，不执行真实 POI 查询。")
        SectionLabel("图片能力")
        Hint("当前保存配置：${when (capability.capability) {
            ImageCapability.UNKNOWN -> "尚未验证图片"
            ImageCapability.AVAILABLE -> "图片与工具格式检查通过"
            ImageCapability.UNSUPPORTED -> "服务明确拒绝图片输入"
        }}")
        Hint("点击检查将直接发送一张内置示例图片至已配置服务，可能产生费用。不使用相册，不查询地图，不自动切换服务商。")
        OutlinedButton({
            val snapshot = store.read() ?: return@OutlinedButton
            busy = true; message = null
            checkJob = scope.launch {
                try {
                    val result = imageCheck(snapshot)
                    val saved = withContext(Dispatchers.IO) { store.saveImageCapability(snapshot, result) }
                    message = if (saved) "内置图片识别和工具参数验证通过。" else "配置已变化，旧检查结果未保存。"
                } catch (e: CancellationException) { throw e }
                catch (e: Exception) {
                    if ((e as? AssistantFailure)?.code == AssistantFailureCode.UNSUPPORTED_IMAGE)
                        withContext(Dispatchers.IO) { store.saveImageCapability(snapshot,
                            ImageCapabilityResult(ImageCapability.UNSUPPORTED, System.currentTimeMillis())) }
                    message = (e as? AssistantFailure)?.userMessage ?: "图片能力未验证，请稍后重试。"
                } finally { capability = store.imageCapability(); busy = false }
            }
        }, enabled = !busy && hasSaved && savedConfigMatches,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("assistant-image-capability-check")) { Text("检查图片能力") }
        if (!savedConfigMatches) Hint("服务配置已修改，请先测试并保存；图片检查使用保存后的配置。")
        if (hasSaved) TextButton({ confirmClear = true }, enabled = !busy) { Text("清除模型配置") }
    }
    if (confirmClear) AlertDialog(onDismissRequest = { confirmClear = false }, title = { Text("清除助手模型配置？") },
        text = { Text("停止后续模型调用，不删除旅行和已收藏地点。") },
        confirmButton = { TextButton({
            store.clear(); hasSaved = false; key = ""; capability = store.imageCapability(); confirmClear = false; message = "已清除配置，手动功能不受影响。"
        }) { Text("确认清除") } }, dismissButton = { TextButton({ confirmClear = false }) { Text("取消") } })
}
