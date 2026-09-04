package com.yangchengwei.easytrip.itinerary.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.ui.component.CompactPrimaryButton
import com.yangchengwei.easytrip.core.ui.component.CompactSecondaryButton

@Composable
fun ItinerarySaveFailureContent(
    onKeepEditing: () -> Unit,
    onRetrySave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .imePadding()
            .semantics { liveRegion = LiveRegionMode.Polite }
            .testTag("itinerary-save-failure"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .testTag("itinerary-save-failure-scroll"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("修改尚未保存", Modifier.semantics { heading() })
            Text("到达时间、停留时长和备注仍保留在当前页面。请重新保存，或稍后再试。")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("当前编辑内容不会自动回滚")
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth < 330.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CompactSecondaryButton(
                        onClick = onKeepEditing,
                        modifier = Modifier.fillMaxWidth().testTag("itinerary-save-failure-keep-editing"),
                    ) { Text("继续编辑") }
                    CompactPrimaryButton(
                        onClick = onRetrySave,
                        modifier = Modifier.fillMaxWidth().testTag("itinerary-save-failure-retry"),
                    ) { Text("重新保存") }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CompactSecondaryButton(
                        onClick = onKeepEditing,
                        modifier = Modifier.testTag("itinerary-save-failure-keep-editing"),
                    ) { Text("继续编辑") }
                    CompactPrimaryButton(
                        onClick = onRetrySave,
                        modifier = Modifier.testTag("itinerary-save-failure-retry"),
                    ) { Text("重新保存") }
                }
            }
        }
    }
}
