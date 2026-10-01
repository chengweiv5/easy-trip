package com.yangchengwei.easytrip.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yangchengwei.easytrip.amap.AmapConsentFact
import com.yangchengwei.easytrip.amap.AmapConsentStore
import kotlinx.coroutines.launch

@Composable
fun MapConsentContent(store: AmapConsentStore, onBack: () -> Unit, onPolicy: () -> Unit, onRevoked: () -> Unit) {
    val state by store.state.collectAsStateWithLifecycle()
    val shown by store.shown.collectAsStateWithLifecycle()
    var read by rememberSaveable { mutableStateOf(false) }
    var confirmRevoke by rememberSaveable { mutableStateOf(false) }
    var reportAttempt by remember { mutableIntStateOf(0) }
    var reportFailed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val granted = state.fact is AmapConsentFact.Accepted
    LaunchedEffect(reportAttempt) { reportFailed = store.reportShown().isFailure }
    SettingsPage("地图授权", "app-map-consent", onBack) {
        Hint("此授权应用于所有旅行，不是手机系统的定位权限。")
        SettingsCard {
            Text("高德地图服务", style = MaterialTheme.typography.titleLarge)
            Text(if (granted) "已授权" else "未授权", color = MaterialTheme.colorScheme.primary, modifier = Modifier.testTag("map-authorization-state"))
            Text("用于展示收藏地点、每天的路线和交通距离。")
            Hint("未授权时仍可编辑已有地点池和行程。")
        }
        Secondary("阅读高德隐私权政策", "map-policy-link", onPolicy)
        if (reportFailed) {
            Text("隐私说明展示失败，请重试", color = MaterialTheme.colorScheme.error)
            Secondary("重试", "map-report-retry", { reportAttempt++ })
        }
        if (!granted) {
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("map-policy-read")
                .toggleable(value = read, enabled = !state.updating, role = Role.Checkbox, onValueChange = { read = it })) {
                Checkbox(checked = read, onCheckedChange = null)
                Text("我已阅读高德隐私权政策", Modifier.padding(top = 12.dp), style = MaterialTheme.typography.bodyMedium)
            }
            UpdateButton(if (state.updating) "正在更新…" else "允许使用地图", "map-allow", {
                scope.launch { store.decide(true).onSuccess { read = false } }
            }, shown && read && !state.updating)
            Secondary("暂不允许", "map-not-now", onBack, !state.updating)
        } else {
            Secondary("撤回地图授权", "map-revoke", { confirmRevoke = true }, !state.updating)
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Hint("定位权限仍由手机系统管理，仅在使用定位时请求；本页不会自动申请定位权限。")
    }
    if (confirmRevoke) AlertDialog(
        onDismissRequest = { if (!state.updating) confirmRevoke = false },
        title = { Text("撤回地图授权？") },
        text = { Column { Text("地图与在线地点搜索、路线服务将暂停。已有旅行、收藏地点和行程不会删除，之后可重新授权。");state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) } } },
        confirmButton = { TextButton(enabled = !state.updating, onClick = {
            scope.launch { store.decide(false).onSuccess { read = false; confirmRevoke = false; onRevoked() } }
        }, modifier = Modifier.testTag("map-revoke-confirm")) { Text(if (state.updating) "正在撤回…" else "确认撤回") } },
        dismissButton = { TextButton(enabled = !state.updating, onClick = { confirmRevoke = false }) { Text("取消") } },
    )
}
