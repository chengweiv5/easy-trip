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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private tailrec fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}

@Composable
fun AssistantSettings(store: AssistantConfigStore, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val initial = remember(store) { store.read() }
    var url by remember { mutableStateOf(initial?.baseUrl ?: "https://api.deepseek.com") }
    var model by remember { mutableStateOf(initial?.model ?: "deepseek-flash") }
    var key by remember { mutableStateOf(initial?.apiKey.orEmpty()) }
    var hasSaved by remember { mutableStateOf(initial != null) }
    var visible by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    val window = LocalContext.current.activity()?.window
    DisposableEffect(window) {
        val wasSecure = window?.attributes?.flags?.and(WindowManager.LayoutParams.FLAG_SECURE) != 0
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { if (!wasSecure) window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }
    BackHandler(enabled = busy) {}
    SettingsPage("助手设置", "assistant-settings", { if (!busy) onBack() }) {
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
            scope.launch {
                try {
                    val config = ProviderConfig(url, model, key).validated()
                    val result = DeepSeekPlaceParser(config = { config }).parse("把杭州的雷峰塔标记出来", "杭州")
                    if (result.isEmpty()) throw AssistantFailure("未验证到有效工具参数。")
                    withContext(Dispatchers.IO) { store.save(config) }
                    hasSaved = true
                    message = "连接与工具格式验证通过，已保存。尚未查询真实地点。"
                } catch (e: CancellationException) { throw e }
                catch (e: Exception) { message = (e as? AssistantFailure)?.userMessage ?: "配置格式有误或无法安全保存，请检查后重试。" }
                finally { busy = false }
            }
        }, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("assistant-test-save")) {
            Text(if (busy) "正在测试…" else "测试连接并保存")
        }
        Hint("测试将发送固定的杭州地点文字，产生一次模型调用及费用；不发送你的旅行内容，不执行真实 POI 查询。")
        if (hasSaved) TextButton({ confirmClear = true }, enabled = !busy) { Text("清除模型配置") }
    }
    if (confirmClear) AlertDialog(onDismissRequest = { confirmClear = false }, title = { Text("清除助手模型配置？") },
        text = { Text("停止后续模型调用，不删除旅行和已收藏地点。") },
        confirmButton = { TextButton({
            store.clear(); hasSaved = false; key = ""; confirmClear = false; message = "已清除配置，手动功能不受影响。"
        }) { Text("确认清除") } }, dismissButton = { TextButton({ confirmClear = false }) { Text("取消") } })
}
