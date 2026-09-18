package com.yangchengwei.easytrip.itinerary.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Checkbox
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import com.yangchengwei.easytrip.itinerary.ui.AddToItineraryEditingTarget.ForPlace
import com.yangchengwei.easytrip.workspace.PlaceScheduleSummaryUi
import java.time.LocalDate
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.ui.component.CompactPrimaryButton
import com.yangchengwei.easytrip.core.ui.component.CompactSecondaryButton
import com.yangchengwei.easytrip.itinerary.domain.AddPlacesOutcome
import com.yangchengwei.easytrip.trip.domain.TripDay

@Composable
fun SelectTargetDayContent(
    days: List<TripDay>,
    state: AddToItineraryUiState,
    onSelectDay: (String) -> Unit,
    onToggleDay: (String) -> Unit = onSelectDay,
    onSubmit: () -> Unit,
    onGoToItineraryAddDay: () -> Unit = {},
    onClose: () -> Unit = {},
    modifier: Modifier = Modifier,
    startDate: LocalDate? = null,
    schedule: PlaceScheduleSummaryUi? = null,
    selectedPlaceName: String? = null,
) {
    Column(modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("加入行程", style = MaterialTheme.typography.titleLarge)
        Text(
            when {
                state.editingTarget is ForPlace && selectedPlaceName != null -> selectedPlaceName
                state.editingTarget is ForPlace -> "选择要加入的旅行日"
                else -> "已选 ${state.selectedPlaceIds.size} 个地点"
            },
            style = MaterialTheme.typography.bodyMedium,
        )
        when {
            state.result is AddPlacesOutcome.TargetDayMissing -> Text(
                "所选旅行日已不存在，请重新选择",
                color = MaterialTheme.colorScheme.error,
            )
            state.result is AddPlacesOutcome.PartialSuccess -> Text(
                "部分地点加入失败，失败收藏已保留，可重新选择日期后重试",
                color = MaterialTheme.colorScheme.error,
            )
            state.errorMessage != null -> Text(state.errorMessage, color = MaterialTheme.colorScheme.error)
        }
        if (days.isEmpty()) {
            Text("还没有旅行日")
            Text("请先添加旅行日后再加入地点")
            CompactPrimaryButton(
                onGoToItineraryAddDay,
                enabled = !state.isSubmitting && !state.isUndoing,
                modifier = Modifier.testTag("go-to-itinerary-add-day"),
            ) { Text("前往行程") }
        } else {
            val isForPlace = state.editingTarget is ForPlace
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth().testTag("select-target-day-list"),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(days, key = { it.id }) { day ->
                    val isSelected = if (isForPlace) day.id in state.selectedTargetDayIds else state.targetDayId == day.id
                    Row(
                        Modifier.fillMaxWidth()
                            .testTag("target-day-${day.id}")
                            .semantics { selected = isSelected }
                            .clickable(enabled = !state.isSubmitting && !state.isUndoing, role = if (isForPlace) Role.Checkbox else Role.RadioButton) {
                                if (isForPlace) onToggleDay(day.id) else onSelectDay(day.id)
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (isForPlace) Checkbox(isSelected, null, enabled = !state.isSubmitting && !state.isUndoing)
                        else RadioButton(isSelected, null, enabled = !state.isSubmitting && !state.isUndoing)
                        Column(Modifier.weight(1f)) {
                            val date = startDate?.plusDays(day.index.toLong())
                                ?.let { "${it.monthValue} 月 ${it.dayOfMonth} 日" }
                            Text(
                                listOfNotNull("第 ${day.index + 1} 天", date).joinToString(" · "),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            val occurrences = schedule?.days?.firstOrNull { it.dayId == day.id }?.occurrences
                            Text(
                                when {
                                    schedule?.isKnown == false -> "安排信息加载中"
                                    occurrences != null && occurrences > 0 -> "已安排 $occurrences 次"
                                    else -> "尚未安排"
                                },
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
        if (state.isSubmitting) Text("正在创建行程项，请勿重复操作")
        Text("地点将添加到当天行程末尾，可稍后调整顺序", style = MaterialTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            CompactSecondaryButton(
                onClose,
                enabled = !state.isSubmitting && !state.isUndoing,
            ) { Text(if (days.isEmpty()) "暂不添加" else "取消") }
            CompactPrimaryButton(
                onClick = onSubmit,
                enabled = state.canSubmit && !state.isSubmitting,
                modifier = Modifier.padding(start = 8.dp).testTag("select-target-day-submit"),
            ) {
                val selectedDays = if (state.editingTarget is ForPlace) state.selectedTargetDayIds else listOfNotNull(state.targetDayId)
                Text(
                    when (selectedDays.size) {
                        0 -> "请选择旅行日"
                        1 -> days.firstOrNull { it.id == selectedDays.single() }
                            ?.let { "加入第 ${it.index + 1} 天" }
                            ?: "加入行程"
                        else -> "加入 ${selectedDays.size} 天"
                    },
                )
            }
        }
    }
}
