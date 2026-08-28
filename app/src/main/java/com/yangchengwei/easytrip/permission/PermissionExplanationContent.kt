package com.yangchengwei.easytrip.permission

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.yangchengwei.easytrip.core.ui.component.CompactPrimaryButton
import com.yangchengwei.easytrip.core.ui.component.CompactSecondaryButton

@Composable
fun PermissionExplanationContent(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("允许定位") },
        text = { Text("定位仅在你主动点击后用于在地图上显示当前位置。地图服务授权与设备定位权限相互独立。") },
        confirmButton = {
            CompactPrimaryButton(
                onClick = onConfirm,
                modifier = Modifier.testTag("permission-explanation-confirm"),
            ) { Text("允许定位") }
        },
        dismissButton = { CompactSecondaryButton(onDismiss) { Text("暂不") } },
    )
}
