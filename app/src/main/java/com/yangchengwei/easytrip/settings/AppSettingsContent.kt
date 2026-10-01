package com.yangchengwei.easytrip.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.ui.component.EasyTripPrimaryButton
import com.yangchengwei.easytrip.core.ui.component.EasyTripSecondaryButton
import com.yangchengwei.easytrip.core.ui.theme.ThemePaletteIcon
import java.util.Locale

@Composable
internal fun SettingsPage(title: String, tag: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxSize().testTag(tag), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp).testTag("$tag-back")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                    }
                    Text(title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                }
            }
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        }
    }
}

@Composable
fun AppSettingsContent(
    theme: String, consentGranted: Boolean, version: String, hasUpdate: Boolean,
    onBack: () -> Unit, onTheme: () -> Unit, onConsent: () -> Unit, onUpdate: () -> Unit,
) {
    SettingsPage("设置", "app-settings", onBack) {
        Hint("管理整个 App 的偏好与服务")
        SectionLabel("外观")
        SettingRow("主题配色", theme, "app-settings-theme", onTheme) { ThemePaletteIcon(Modifier.size(22.dp)) }
        SectionLabel("权限与服务")
        SettingRow("地图授权", if (consentGranted) "已授权" else "未授权", "app-settings-map", onConsent) { Icon(Icons.Rounded.Lock, null) }
        Hint("地图服务授权应用于所有旅行。")
        SectionLabel("关于应用")
        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface) {
            Column {
                SettingRow("当前版本", "v$version", "app-version", null) { Icon(Icons.Rounded.Info, null) }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingRow("检查更新", if (hasUpdate) "有新版本" else "", "app-settings-update", onUpdate) { Icon(Icons.Rounded.Refresh, null) }
            }
        }
        Hint("主动检查，不自动下载或强制更新。")
        Column(Modifier.fillMaxWidth().padding(top = 44.dp, bottom = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Easy Trip", fontWeight = FontWeight.SemiBold)
            Hint("周末，去远一点")
        }
    }
}

@Composable
private fun SettingRow(title: String, value: String, tag: String, onClick: (() -> Unit)?, icon: @Composable () -> Unit) {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.fillMaxWidth().heightIn(min = 66.dp).testTag(tag)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.primary) { Box(Modifier.size(24.dp)) { icon() } }
            if (LocalDensity.current.fontScale > 1.3f) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.bodyLarge)
                    if (value.isNotEmpty()) Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                if (value.isNotEmpty()) Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (onClick != null) Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable internal fun SectionLabel(text: String) { Text(text, Modifier.padding(top = 10.dp, start = 4.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
@Composable internal fun Hint(text: String) { Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
@Composable internal fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
fun UpdateContent(state: UpdateState, version: String, code: Long, onBack: () -> Unit, onCheck: () -> Unit, onDownload: () -> Unit, onCancel: () -> Unit, onInstall: () -> Unit) {
    SettingsPage("版本更新", "app-update", onBack) {
        SettingsCard {
            Icon(Icons.Rounded.Info, null, tint = MaterialTheme.colorScheme.primary)
            Hint("当前安装版本")
            Text("v$version", style = MaterialTheme.typography.headlineMedium)
            Hint("构建号 $code")
            HorizontalDivider()
            Text(when (state) {
                UpdateState.Idle -> "尚未检查更新"
                UpdateState.Checking -> "正在检查更新…"
                UpdateState.Current -> "已是最新版本"
                UpdateState.Ahead -> "当前版本已领先正式版"
                is UpdateState.Available -> "发现新版本 ${state.release.tag}"
                is UpdateState.Downloading -> "正在下载 ${state.release.tag}"
                is UpdateState.Ready -> "安装包已准备好"
                is UpdateState.Failed -> if (state.release == null) "暂时无法检查更新" else "下载中断"
            }, Modifier.testTag("update-status"), color = if (state is UpdateState.Failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
        }
        val release = when (state) {
            is UpdateState.Available -> state.release
            is UpdateState.Downloading -> state.release
            is UpdateState.Ready -> state.release
            is UpdateState.Failed -> state.release
            else -> null
        }
        if (release != null) SettingsCard {
            Text(release.tag, style = MaterialTheme.typography.titleLarge)
            Hint("正式更新 · 安装包 ${formatSize(release.asset!!.size)}")
            Text(release.notes.ifBlank { "此版本未提供更新说明。" }, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("release-notes"))
        }
        when (state) {
            UpdateState.Checking -> LinearProgressIndicator(Modifier.fillMaxWidth())
            is UpdateState.Downloading -> {
                LinearProgressIndicator(progress = { (state.bytes.toFloat() / state.total).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                Hint("${formatSize(state.bytes)} / ${formatSize(state.total)} · ${(state.bytes * 100 / state.total).coerceIn(0,100)}%")
            }
            is UpdateState.Failed -> Text(state.message, color = MaterialTheme.colorScheme.error)
            UpdateState.Ahead -> Hint("无需降级，可继续使用当前版本。")
            UpdateState.Current -> Hint("目前没有可更新的正式版本。")
            else -> Unit
        }
        Hint(if (state is UpdateState.Ready) "安装需要在系统页面再次确认。拒绝安装许可也能继续使用当前版本。" else "只下载官方发布的安装包，校验通过后交由系统确认安装；不卸载或清除旅行数据。")
        if (state is UpdateState.Ready) state.notice?.let { Text(it, Modifier.testTag("install-notice"), style = MaterialTheme.typography.bodyMedium) }
        when (state) {
            UpdateState.Checking -> {
                UpdateButton("正在检查…", "update-check", {}, false)
                Secondary("取消检查", "update-cancel", onCancel)
            }
            is UpdateState.Downloading -> Secondary("取消下载", "update-cancel", onCancel)
            is UpdateState.Available -> { UpdateButton("下载更新", "update-download", onDownload); Secondary("稍后再说", "update-later", onBack) }
            is UpdateState.Ready -> { UpdateButton("前往安装", "update-install", onInstall); Secondary("稍后安装", "update-later", onBack); Secondary("重新检查更新", "update-check", onCheck) }
            is UpdateState.Failed -> if (state.release != null) UpdateButton("重新下载", "update-download", onDownload) else UpdateButton("重试检查", "update-check", onCheck)
            else -> UpdateButton(if (state == UpdateState.Idle) "检查更新" else "重新检查", "update-check", onCheck)
        }
    }
}
@Composable internal fun UpdateButton(text: String, tag: String, action: () -> Unit, enabled: Boolean = true) {
    EasyTripPrimaryButton(onClick = action, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag(tag)) { Text(text) }
}
@Composable internal fun Secondary(text: String, tag: String, action: () -> Unit, enabled: Boolean = true) {
    EasyTripSecondaryButton(onClick = action, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag(tag)) { Text(text) }
}
private fun formatSize(bytes: Long) = String.format(Locale.ROOT, "%.1f MB", bytes / 1024.0 / 1024.0)
