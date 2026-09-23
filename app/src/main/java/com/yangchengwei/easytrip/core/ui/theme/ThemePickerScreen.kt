package com.yangchengwei.easytrip.core.ui.theme

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import com.yangchengwei.easytrip.core.ui.component.EasyTripPrimaryButton

@Composable
fun ThemePickerScreen(
    state: ThemePickerState,
    saved: ThemePalette,
    onSelect: (ThemePalette) -> Unit,
    onApply: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler { if (!state.saving) onBack() }
    EasyTripTheme(state.preview) {
        Surface(Modifier.fillMaxSize().testTag("theme-picker"), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
                Surface(color = MaterialTheme.colorScheme.surface) {
                    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack, enabled = !state.saving, modifier = Modifier.size(48.dp).testTag("theme-back")) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                        }
                        Text("主题配色", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    }
                }
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp).testTag("theme-options-scroll"),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("效果预览", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                        Text("预览 · ${state.preview.displayName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    ThemePreviewCard()
                    Text("选择主题", style = MaterialTheme.typography.titleSmall)
                    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemePalette.entries.forEach { palette ->
                            ThemeOption(palette, state.preview == palette, !state.saving) { onSelect(palette) }
                        }
                    }
                    Text("应用于所有旅行，随时可以换回来。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        if (state.failed) Text("未能应用主题，请重试", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("theme-error"))
                        EasyTripPrimaryButton(
                            onClick = onApply,
                            enabled = !state.saving && state.preview != saved,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("theme-apply"),
                        ) {
                            Text(when {
                                state.saving -> "正在应用…"
                                state.preview == saved -> "已使用${saved.displayName}"
                                state.failed -> "重新应用${state.preview.displayName}"
                                else -> "应用${state.preview.displayName}"
                            })
                        }
                        Text("返回可放弃本次预览", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            // Also covers predictive/system back while persistence is in flight.
        }
    }
}

@Composable
private fun ThemePreviewCard() {
    Surface(shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("杭州 · 春日慢游", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Text("待出行", Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall)
                }
            }
            Text("4月12日 — 4月14日 · 3天9站", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("♧  西湖天地", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                // A preview, deliberately without click semantics.
                Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
                    Text("继续规划", Modifier.padding(horizontal = 24.dp, vertical = 12.dp), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun ThemeOption(palette: ThemePalette, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(min = 62.dp).testTag("theme-option-${palette.id}")
            .selectable(selected, enabled = enabled, role = Role.RadioButton, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.size(50.dp, 40.dp).clip(RoundedCornerShape(5.dp))) {
                Box(Modifier.width(22.dp).fillMaxHeight().background(palette.colors.primary))
                Column(Modifier.weight(1f)) {
                    Box(Modifier.fillMaxWidth().weight(1f).background(palette.colors.background))
                    Box(Modifier.fillMaxWidth().weight(1f).background(palette.colors.primaryContainer))
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(palette.displayName + if (palette == ThemePalette.LAKE) " · 默认" else "", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                Text(palette.description, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            if (selected) Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            else Canvas(Modifier.size(20.dp)) {
                drawCircle(palette.colors.onSurfaceVariant, radius = size.minDimension * .38f, style = Stroke(1.dp.toPx()))
            }
        }
    }
}

@Composable
fun ThemePaletteIcon(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawOval(color, style = Stroke(1.8.dp.toPx()))
        listOf(.30f to .32f, .58f to .23f, .77f to .45f, .28f to .64f).forEach { (x, y) ->
            drawCircle(color, w * .065f, Offset(w * x, h * y))
        }
        drawCircle(color, w * .11f, Offset(w * .66f, h * .73f))
    }
}
