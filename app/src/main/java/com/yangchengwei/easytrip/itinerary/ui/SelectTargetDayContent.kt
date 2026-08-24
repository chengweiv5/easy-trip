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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
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
    onSubmit: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("加入哪一天？", style = MaterialTheme.typography.titleLarge)
        Text("已选 ${state.selectedPlaceIds.size} 个地点", style = MaterialTheme.typography.bodyMedium)
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
        } else {
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth().testTag("select-target-day-list"),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(days, key = { it.id }) { day ->
                    val isSelected = state.targetDayId == day.id
                    Row(
                        Modifier.fillMaxWidth()
                            .testTag("target-day-${day.id}")
                            .semantics { selected = isSelected }
                            .clickable(enabled = !state.isSubmitting && !state.isUndoing, role = Role.RadioButton) { onSelectDay(day.id) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(isSelected, null, enabled = !state.isSubmitting && !state.isUndoing)
                        Text("第 ${day.index + 1} 天")
                    }
                }
            }
        }
        if (state.isSubmitting) Text("正在创建行程项，请勿重复操作")
        Text("地点将添加到当天行程末尾，可稍后调整顺序", style = MaterialTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            CompactSecondaryButton(onClose, enabled = !state.isSubmitting) { Text("取消") }
            CompactPrimaryButton(
                onClick = onSubmit,
                enabled = state.canSubmit && !state.isSubmitting,
                modifier = Modifier.padding(start = 8.dp).testTag("select-target-day-submit"),
            ) { Text(if (state.targetDayId == null) "请选择旅行日" else "加入行程") }
        }
    }
}
