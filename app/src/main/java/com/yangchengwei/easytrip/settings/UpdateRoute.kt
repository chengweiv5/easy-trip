package com.yangchengwei.easytrip.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yangchengwei.easytrip.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun UpdateRoute(controller: UpdateController, onBack: () -> Unit) {
    val context = LocalContext.current
    val installer = remember(context) { ApkInstaller(context) }
    val state by controller.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var launching by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        controller.installNotice(if (installer.canInstall()) "已允许安装，请点击“前往安装”继续。" else "未允许安装，仍可继续使用当前版本。可稍后重试。")
    }
    val installLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        controller.installNotice("已从系统安装页面返回。若未完成安装，可再次点击“前往安装”。")
    }
    UpdateContent(state, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE.toLong(), onBack,
        controller::check, controller::download, controller::cancel, onInstall = {
            val ready = state as? UpdateState.Ready
            if (ready != null && !launching) {
                launching = true
                scope.launch {
                    try {
                        val intent = withContext(Dispatchers.IO) { installer.installIntent(ready.file, ready.release) }
                        if ((controller.state.value as? UpdateState.Ready)?.file == ready.file) {
                            if (installer.canInstall()) installLauncher.launch(intent)
                            else {
                                controller.installNotice("请在系统页面允许此来源安装；返回后再次点击“前往安装”。")
                                permissionLauncher.launch(installer.permissionIntent())
                            }
                        }
                    } catch (cancelled: kotlinx.coroutines.CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        controller.installNotice("无法打开安装，或安装包已失效。请重新检查并下载更新。")
                    } finally { launching = false }
                }
            }
        })
}
