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
fun LocationPermissionSettingsContent(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
    busy: Boolean = false,
    error: String? = null,
) {
    EasyTripDialogSurface(onDismiss = onDismiss) {
        LocationPermissionSettingsBody(
            onOpenSettings = onOpenSettings,
            onDismiss = onDismiss,
            busy = busy,
            error = error,
        )
    }
}

@Composable
fun LocationPermissionSettingsBody(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
    busy: Boolean = false,
    error: String? = null,
) {
    Column(
        modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            "定位权限未开启",
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            "请前往系统设置，为 Easy Trip 开启定位权限。地图和行程仍可正常使用。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        error?.let {
            Text(
                text = it,
                modifier = Modifier
                    .testTag("location-settings-error")
                    .semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            EasyTripSecondaryButton(onDismiss, Modifier.weight(1f)) { Text("取消") }
            EasyTripPrimaryButton(
                onClick = onOpenSettings,
                modifier = Modifier.weight(1f).testTag("location-settings-open"),
                enabled = !busy,
            ) { Text("前往设置") }
        }
    }
}
