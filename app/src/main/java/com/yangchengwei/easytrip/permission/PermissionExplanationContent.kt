package com.yangchengwei.easytrip.permission

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.yangchengwei.easytrip.core.ui.component.CompactPrimaryButton
import com.yangchengwei.easytrip.core.ui.component.CompactSecondaryButton
import com.yangchengwei.easytrip.workspace.PermissionKind

@Composable
fun PermissionExplanationContent(
    kind: PermissionKind,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val content = when (kind) {
        PermissionKind.MAP_SERVICE_CONSENT -> PermissionCopy(
            "地图服务授权",
            "地图展示和地点搜索使用高德地图服务。是否启用由地图服务隐私授权单独决定。",
            "查看地图授权",
        )
        PermissionKind.DEVICE_LOCATION -> PermissionCopy(
            "允许定位",
            "定位仅在你主动点击后用于在地图上显示当前位置。地图服务授权与设备定位权限相互独立。",
            "允许定位",
        )
        PermissionKind.DEVICE_LOCATION_SETTINGS -> PermissionCopy(
            "需要在设置中允许定位",
            "系统已不再显示定位权限请求。你可以前往应用详情设置手动允许。",
            "打开应用设置",
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(content.title) },
        text = { Text(content.message) },
        confirmButton = {
            CompactPrimaryButton(
                onClick = onConfirm,
                modifier = Modifier.testTag("permission-explanation-confirm"),
            ) { Text(content.confirmLabel) }
        },
        dismissButton = { CompactSecondaryButton(onDismiss) { Text("暂不") } },
    )
}

private data class PermissionCopy(
    val title: String,
    val message: String,
    val confirmLabel: String,
)
