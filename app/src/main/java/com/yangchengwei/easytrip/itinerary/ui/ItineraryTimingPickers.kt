package com.yangchengwei.easytrip.itinerary.ui

import android.os.Build
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import java.time.LocalTime
import kotlin.math.roundToInt

@Composable
internal fun ItineraryTimingPickers(
    draft: ItineraryEditDraft,
    onArrivalTimeChange: (String) -> Unit,
    onStayMinutesChange: (String) -> Unit,
) {
    val time = draft.arrivalTime
    // Capture once for the edit session, so a historical value remains available after scrolling away.
    val originalMinutes = remember(draft.itemId, draft.generation) { draft.stayMinutes }
    val stays = remember(originalMinutes, draft.stayMinutes) {
        (stayMinuteOptions(originalMinutes) + listOfNotNull(draft.stayMinutes)).distinct()
            .sortedWith(nullsFirst())
    }
    val hourLabels = remember { listOf("待定") + (0..23).map { it.toString().padStart(2, '0') } }
    val minuteLabels = remember { listOf("00", "30") }
    val enabled = !draft.isSaving
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.weight(2f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("到达时间", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TimingWheel("时", "到达小时", hourLabels, time?.hour?.plus(1) ?: 0, enabled,
                    Modifier.weight(1f).testTag("arrival-hour-picker")) { index ->
                    onArrivalTimeChange(if (index == 0) "" else LocalTime.of(index - 1, (time?.minute ?: 0) / 30 * 30).toString())
                }
                TimingWheel("分", "到达分钟", minuteLabels, (time?.minute ?: 0) / 30, enabled && time != null,
                    Modifier.weight(1f).testTag("arrival-minute-picker")) { index ->
                    time?.let { onArrivalTimeChange(LocalTime.of(it.hour, index * 30).toString()) }
                }
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("停留时长", style = MaterialTheme.typography.labelMedium)
            TimingWheel("小时", "停留小时", stays.map { it?.let(::formatStayHours)?.removeSuffix(" 小时") ?: "未设置" },
                stays.indexOf(draft.stayMinutes).coerceAtLeast(0), enabled,
                Modifier.fillMaxWidth().testTag("stay-hours-picker")) { index ->
                onStayMinutesChange(stays[index]?.toString().orEmpty())
            }
        }
    }
}

@Composable
private fun TimingWheel(
    unit: String,
    label: String,
    values: List<String>,
    selected: Int,
    enabled: Boolean,
    modifier: Modifier,
    onSelected: (Int) -> Unit,
) {
    val currentOnSelected = rememberUpdatedState(onSelected)
    val currentEnabled = rememberUpdatedState(enabled)
    val color = MaterialTheme.colorScheme.primary.toArgb()
    val textSize = with(LocalDensity.current) { 20.sp.toPx() }
    Column(modifier.semantics(mergeDescendants = true) {}, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(unit, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(Modifier.fillMaxWidth().height(144.dp).semantics {
            contentDescription = label
            stateDescription = values[selected]
            progressBarRangeInfo = ProgressBarRangeInfo(selected.toFloat(), 0f..values.lastIndex.toFloat(), values.size - 2)
            if (!enabled) disabled()
            setProgress { requested ->
                if (enabled && requested.isFinite()) {
                    onSelected(requested.roundToInt().coerceIn(values.indices)); true
                } else false
            }
            customActions = if (!enabled) emptyList() else listOf(
                CustomAccessibilityAction("增加$label") { onSelected((selected + 1).coerceAtMost(values.lastIndex)); true },
                CustomAccessibilityAction("减少$label") { onSelected((selected - 1).coerceAtLeast(0)); true },
            )
        }, contentAlignment = Alignment.Center) {
            Box(Modifier.fillMaxWidth().height(44.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)))
            AndroidView(
                modifier = Modifier.fillMaxWidth().height(144.dp).padding(horizontal = 2.dp),
                factory = { context ->
                    object : NumberPicker(context) {
                        override fun dispatchTouchEvent(event: MotionEvent): Boolean {
                            if (event.actionMasked == MotionEvent.ACTION_DOWN) parent?.requestDisallowInterceptTouchEvent(true)
                            val result = super.dispatchTouchEvent(event)
                            if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
                                parent?.requestDisallowInterceptTouchEvent(false)
                            }
                            return result
                        }
                    }.apply {
                        descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
                        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                        wrapSelectorWheel = false
                        setOnValueChangedListener { _, _, value ->
                            if (currentEnabled.value) currentOnSelected.value(value)
                        }
                    }
                },
                update = { picker ->
                    if (picker.displayedValues?.toList() != values) {
                        picker.displayedValues = null
                        picker.minValue = 0
                        picker.maxValue = values.lastIndex
                        picker.displayedValues = values.toTypedArray()
                        picker.wrapSelectorWheel = false
                    }
                    if (picker.value != selected) picker.value = selected
                    picker.isEnabled = enabled
                    picker.alpha = if (enabled) 1f else 0.4f
                    if (Build.VERSION.SDK_INT >= 29) {
                        picker.textColor = color
                        picker.textSize = textSize
                        picker.selectionDividerHeight = 0
                    }
                },
            )
        }
    }
}
