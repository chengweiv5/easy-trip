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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
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
    var unsetAfternoon by rememberSaveable(draft.itemId, draft.generation) {
        mutableStateOf((time?.hour ?: 0) >= 12)
    }
    val afternoon = time?.let { it.hour >= 12 } ?: unsetAfternoon
    val hourOffset = if (afternoon) 12 else 0
    // Capture once for the edit session, so a historical value remains available after scrolling away.
    val originalMinutes = remember(draft.itemId, draft.generation) { draft.stayMinutes }
    val stays = remember(originalMinutes, draft.stayMinutes) {
        (stayMinuteOptions(originalMinutes) + listOfNotNull(draft.stayMinutes)).distinct()
            .sortedWith(nullsFirst())
    }
    val hourLabels = remember(afternoon) {
        listOf("待定") + (hourOffset until hourOffset + 12).map { it.toString().padStart(2, '0') }
    }
    val minuteLabels = remember { listOf("00", "30") }
    val enabled = !draft.isSaving
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            FlowRow(
                Modifier.weight(2f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Text("到达时间", style = MaterialTheme.typography.labelSmall)
                ArrivalPeriodFilter(afternoon, enabled) { selectedAfternoon ->
                    if (selectedAfternoon != afternoon) {
                        unsetAfternoon = selectedAfternoon
                        time?.let {
                            onArrivalTimeChange(it.withHour(it.hour % 12 + if (selectedAfternoon) 12 else 0).toString())
                        }
                    }
                }
            }
            Text("停留时长", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.weight(2f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Discard a previous hour wheel's fling when its range or edit session changes.
                key(draft.itemId, draft.generation, afternoon) {
                    TimingWheel("时", "到达小时", hourLabels, time?.hour?.rem(12)?.plus(1) ?: 0, enabled,
                        Modifier.weight(1f).testTag("arrival-hour-picker")) { index ->
                        unsetAfternoon = afternoon
                        onArrivalTimeChange(if (index == 0) "" else LocalTime.of(hourOffset + index - 1, (time?.minute ?: 0) / 30 * 30).toString())
                    }
                }
                TimingWheel("分", "到达分钟", minuteLabels, (time?.minute ?: 0) / 30, enabled && time != null,
                    Modifier.weight(1f).testTag("arrival-minute-picker")) { index ->
                    time?.let { onArrivalTimeChange(LocalTime.of(it.hour, index * 30).toString()) }
                }
            }
            TimingWheel("小时", "停留小时", stays.map { it?.let(::formatStayHours)?.removeSuffix(" 小时") ?: "未设置" },
                stays.indexOf(draft.stayMinutes).coerceAtLeast(0), enabled,
                Modifier.weight(1f).testTag("stay-hours-picker")) { index ->
                onStayMinutesChange(stays[index]?.toString().orEmpty())
            }
        }
    }
}

@Composable
private fun ArrivalPeriodFilter(afternoon: Boolean, enabled: Boolean, onSelect: (Boolean) -> Unit) {
    Row(
        Modifier.selectableGroup(),
    ) {
        listOf(false, true).forEach { isAfternoon ->
            val selected = afternoon == isAfternoon
            Box(
                Modifier.widthIn(min = 50.dp).heightIn(min = 40.dp)
                    .selectable(selected, enabled = enabled, role = Role.Tab, onClick = { onSelect(isAfternoon) })
                    .testTag(if (isAfternoon) "arrival-period-pm" else "arrival-period-am")
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                val outerShape = if (isAfternoon) {
                    RoundedCornerShape(topEnd = 7.dp, bottomEnd = 7.dp)
                } else {
                    RoundedCornerShape(topStart = 7.dp, bottomStart = 7.dp)
                }
                Box(
                    Modifier.clip(outerShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(2.dp),
                ) {
                    Text(
                        if (isAfternoon) "下午" else "上午",
                        modifier = Modifier.widthIn(min = 46.dp).heightIn(min = 24.dp)
                            .background(if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(5.dp))
                            .padding(horizontal = 10.dp, vertical = 3.5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = (if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            .copy(alpha = if (enabled) 1f else 0.4f),
                    )
                }
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
    val textSize = with(LocalDensity.current) { 16.sp.toPx() }
    val selectionHeight = with(LocalDensity.current) { (16.sp.toDp() + 8.dp).coerceAtLeast(32.dp) }
    val wheelHeight = selectionHeight * 3
    Column(modifier.semantics(mergeDescendants = true) {}, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(unit, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(Modifier.fillMaxWidth().height(wheelHeight).semantics {
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
            Box(Modifier.fillMaxWidth().height(selectionHeight).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp)))
            AndroidView(
                modifier = Modifier.fillMaxWidth().height(wheelHeight).padding(horizontal = 2.dp),
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
                onRelease = { picker -> picker.setOnValueChangedListener(null) },
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
