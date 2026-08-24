package com.yangchengwei.easytrip.itinerary.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.ui.component.CompactPrimaryButton
import com.yangchengwei.easytrip.core.ui.component.CompactSecondaryButton

@Composable
fun AddTripDayContent(
    isAppending: Boolean,
    error: String?,
    onConfirm: () -> Unit,
    onClose: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("新的一天会追加到当前旅行末尾。")
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        CompactPrimaryButton(onConfirm, enabled = !isAppending) {
            Text(if (isAppending) "添加中…" else "添加一天")
        }
        CompactSecondaryButton(onClose, enabled = !isAppending) { Text("取消") }
    }
}
