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
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.ui.component.EasyTripDialogSurface
import com.yangchengwei.easytrip.core.ui.component.EasyTripPrimaryButton
import com.yangchengwei.easytrip.core.ui.component.EasyTripSecondaryButton

@Composable
fun PermissionExplanationContent(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    EasyTripDialogSurface(onDismiss = onDismiss) {
        Column(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("允许定位", style = MaterialTheme.typography.titleLarge)
            Text(
                "定位仅在你主动点击后用于在地图上显示当前位置。地图服务授权与设备定位权限相互独立。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                EasyTripSecondaryButton(onDismiss, Modifier.weight(1f)) { Text("暂不") }
                EasyTripPrimaryButton(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f).testTag("permission-explanation-confirm"),
                ) { Text("允许定位") }
            }
        }
    }
}
