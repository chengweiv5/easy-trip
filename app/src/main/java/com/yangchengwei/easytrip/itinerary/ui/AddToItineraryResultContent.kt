package com.yangchengwei.easytrip.itinerary.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.yangchengwei.easytrip.core.ui.component.CompactPrimaryButton
import com.yangchengwei.easytrip.core.ui.component.CompactSecondaryButton
import com.yangchengwei.easytrip.trip.domain.TripDay

@Composable
fun AddToItineraryResultContent(
    state: AddToItineraryUiState,
    days: List<TripDay>,
    placeNameForId: (String) -> String?,
    onUndo: () -> Unit,
    onRetryFailed: () -> Unit,
    onReselectDates: () -> Unit,
    onViewResult: (List<String>) -> Unit,
    onViewPlacePool: () -> Unit,
    onClose: () -> Unit,
) {
    val result = state.submissionResult ?: AddToItinerarySubmissionResult()
    val createdDayIds = result.createdItemsByDay.map(UndoCreatedItemsBatch::dayId)
    val dayLabel = { dayId: String ->
        days.firstOrNull { it.id == dayId }?.let { "第 ${it.index + 1} 天" }
            ?: result.missingTargetDayLabels[dayId]
            ?: "已删除的旅行日（$dayId）"
    }
    val missing = result.missingTargetDayIds
    val failed = result.failedAdditions
    val isUndone = createdDayIds.isNotEmpty() && state.undoCreatedItemIds.isEmpty() && !state.isUndoing && state.errorMessage == null

    Column {
        when {
            isUndone -> Text("已从${createdDayIds.joinToString("、") { dayLabel(it) }}移除，收藏地点仍保留")
            createdDayIds.isNotEmpty() && failed.isNotEmpty() -> {
                Text("部分地点已加入行程")
                Text("已加入${createdDayIds.joinToString("、") { dayLabel(it) }}")
                result.createdItemsByDay.forEach { Text("${dayLabel(it.dayId)}：已加入") }
            }
            createdDayIds.isNotEmpty() -> Text("已加入${createdDayIds.joinToString("、") { dayLabel(it) }}")
            failed.isNotEmpty() -> Text("加入行程未完成")
            missing.isNotEmpty() -> Unit
            else -> Text(state.errorMessage ?: "加入行程失败，请重试")
        }
        if (failed.isNotEmpty()) {
            Text("以下地点未加入，收藏地点仍保留：")
            failed.forEach { failure ->
                val place = placeNameForId(failure.placeId) ?: "收藏地点"
                Text("${dayLabel(failure.dayId)} · $place：未加入")
            }
        }
        if (missing.isNotEmpty()) {
            Text("所选旅行日已不存在")
            Text("${missing.joinToString("、") { dayLabel(it) }}已被删除，未将地点改投其他日期。")
            Text("请重新选择旅行日；收藏地点仍保留。")
        }
        if (
            state.errorMessage != null &&
            state.undoCreatedItemIds.isNotEmpty() &&
            (createdDayIds.isNotEmpty() || failed.isNotEmpty() || missing.isNotEmpty())
        ) Text(state.errorMessage)

        when {
            missing.isNotEmpty() -> {
                if (state.undoCreatedItemIds.isNotEmpty()) {
                    CompactPrimaryButton(onUndo, enabled = !state.isSubmitting && !state.isUndoing) {
                        Text(if (state.isUndoing) "撤销中…" else "撤销")
                    }
                }
                if (createdDayIds.isNotEmpty()) {
                    CompactSecondaryButton(onClick = { onViewResult(createdDayIds) }, enabled = !state.isSubmitting && !state.isUndoing) {
                        Text(if (createdDayIds.size == 1) "查看当天" else "查看行程")
                    }
                }
                CompactSecondaryButton(onReselectDates, enabled = !state.isSubmitting && !state.isUndoing) {
                    Text("请重新选择旅行日")
                }
                if (failed.isNotEmpty() && result.retryTargetDayIds.isNotEmpty()) {
                    CompactSecondaryButton(onRetryFailed, enabled = !state.isSubmitting && !state.isUndoing) { Text("重试失败地点") }
                }
                CompactSecondaryButton(onClose, enabled = !state.isSubmitting && !state.isUndoing) { Text("仅保留收藏") }
            }
            isUndone -> CompactPrimaryButton(onViewPlacePool) { Text("查看地点池") }
            else -> {
                if (state.undoCreatedItemIds.isNotEmpty()) {
                    CompactPrimaryButton(onUndo, enabled = !state.isSubmitting && !state.isUndoing) { Text(if (state.isUndoing) "撤销中…" else "撤销") }
                }
                if (failed.isNotEmpty() && result.retryTargetDayIds.isNotEmpty()) {
                    CompactSecondaryButton(onRetryFailed, enabled = !state.isSubmitting && !state.isUndoing) { Text("重试失败地点") }
                }
                if (createdDayIds.isNotEmpty()) {
                    CompactSecondaryButton(onClick = { onViewResult(createdDayIds) }, enabled = !state.isSubmitting && !state.isUndoing) {
                        Text(if (createdDayIds.size == 1) "查看当天" else "查看行程")
                    }
                }
                CompactSecondaryButton(onClose, enabled = !state.isSubmitting && !state.isUndoing) { Text("关闭") }
            }
        }
    }
}
