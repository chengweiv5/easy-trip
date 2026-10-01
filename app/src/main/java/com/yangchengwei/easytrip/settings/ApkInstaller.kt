package com.yangchengwei.easytrip.settings

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File
import androidx.core.net.toUri

/** Also checked immediately before installation, not only after downloading. */
class ApkInstaller(private val context: Context) {
    @Suppress("DEPRECATION")
    fun verify(file: File, release: OfficialRelease) {
        val asset = requireNotNull(release.asset)
        require(asset.isTrusted(release.tag) && file.length() == asset.size) { "安装包已失效" }
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        require(digest.digest().joinToString("") { "%02x".format(it) }.equals(asset.sha256, true)) { "安装包校验失败" }
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val manager = context.packageManager
        val installed = manager.getPackageInfo(context.packageName, flags)
        val downloaded = requireNotNull(manager.getPackageArchiveInfo(file.absolutePath, flags)) { "无法读取安装包" }
        requireTrustedUpgrade(identity(installed), identity(downloaded), release.tag)
    }

    @Suppress("DEPRECATION")
    private fun identity(info: PackageInfo) = PackageIdentity(
        info.packageName, info.versionName.orEmpty(),
        if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong(),
        signers(info),
    )

    @Suppress("DEPRECATION")
    private fun signers(info: PackageInfo): Set<String> {
        val signatures = if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners else info.signatures
        return signatures.orEmpty().map { signature ->
            java.security.MessageDigest.getInstance("SHA-256").digest(signature.toByteArray()).joinToString("") { "%02x".format(it) }
        }.toSet()
    }

    fun canInstall(): Boolean = context.packageManager.canRequestPackageInstalls()

    fun permissionIntent(): Intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, "package:${context.packageName}".toUri())

    fun installIntent(file: File, release: OfficialRelease): Intent {
        verify(file, release)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.app-update", file)
        return Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .apply { clipData = ClipData.newRawUri("Easy Trip update", uri) }
    }
}
