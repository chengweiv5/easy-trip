package com.yangchengwei.easytrip.permission

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.ui.component.EasyTripDialogSurface
import com.yangchengwei.easytrip.core.ui.component.EasyTripPrimaryButton
import com.yangchengwei.easytrip.core.ui.component.EasyTripSecondaryButton

@Composable
fun PermissionExplanationContent(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    busy: Boolean = false,
    error: String? = null,
) {
    EasyTripDialogSurface(onDismiss = onDismiss) {
        PermissionExplanationBody(
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            busy = busy,
            error = error,
        )
    }
}

@Composable
fun PermissionExplanationBody(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    busy: Boolean = false,
    error: String? = null,
) {
    Column(
        modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            "允许 Easy Trip 获取你的位置",
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            "用于搜索当前城市、显示距我的距离和地图上的我的位置，不会持续后台定位。拒绝后仍可正常规划行程。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        error?.let {
            Text(
                text = it,
                modifier = Modifier
                    .testTag("permission-explanation-error")
                    .semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            EasyTripSecondaryButton(onDismiss, Modifier.weight(1f)) { Text("暂不使用") }
            EasyTripPrimaryButton(
                onClick = onConfirm,
                modifier = Modifier.weight(1f).testTag("permission-explanation-confirm"),
                enabled = !busy,
            ) { Text("继续") }
        }
    }
}
