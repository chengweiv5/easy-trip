package com.yangchengwei.easytrip.settings

import android.content.Intent
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ApkInstallerTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    @Test fun sameInstalledApkIsRejectedEvenWithCorrectDigestAndSignature() {
        val file = File(context.applicationInfo.sourceDir)
        val sha = MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it) }
        val version = com.yangchengwei.easytrip.BuildConfig.VERSION_NAME
        val tag = "v$version"
        val release = OfficialRelease(tag,"",false,false, ReleaseAsset("easy-trip-$tag-release.apk", "https://github.com/chengweiv5/easy-trip/releases/download/$tag/easy-trip-$tag-release.apk",file.length(),sha))
        assertThrows(IllegalArgumentException::class.java) { ApkInstaller(context).verify(file,release) }
    }
    @Test fun signedNewerFixtureProducesReadOnlyInstallerIntentWithoutInstalling() {
        val name = InstrumentationRegistry.getArguments().getString("updateFixture")
        org.junit.Assume.assumeNotNull(name)
        val file = File(context.cacheDir, "app-updates/$name")
        val sha = MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it) }
        val tag = "v1.8.0"
        val release = OfficialRelease(tag,"",false,false,ReleaseAsset("easy-trip-$tag-release.apk", "https://github.com/chengweiv5/easy-trip/releases/download/$tag/easy-trip-$tag-release.apk",file.length(),sha))
        val intent = ApkInstaller(context).installIntent(file, release)
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("application/vnd.android.package-archive", intent.type)
        assertEquals("content", intent.data!!.scheme)
        assertEquals(Intent.FLAG_GRANT_READ_URI_PERMISSION, intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION)
        assertEquals(0, intent.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        assertNotNull(context.packageManager.resolveActivity(intent, 0))
        // Do not launch: this validates the system handoff without replacing the app under test.
    }
    @Test fun providerOnlySharesUpdateCacheAndPermissionIntentTargetsOwnApp() {
        val dir=File(context.cacheDir,"app-updates").apply { mkdirs() }
        val allowed=File(dir,"provider-test.apk").apply { writeText("fixture") }
        val outside=File(context.cacheDir,"unrelated-private-file").apply { writeText("private") }
        try {
            val authority="${context.packageName}.app-update"
            val uri=FileProvider.getUriForFile(context,authority,allowed)
            assertEquals("content",uri.scheme)
            assertEquals(authority,uri.authority)
            assertThrows(IllegalArgumentException::class.java) { FileProvider.getUriForFile(context,authority,outside) }
            val intent=ApkInstaller(context).permissionIntent()
            assertEquals(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,intent.action)
            assertEquals("package:${context.packageName}",intent.dataString)
        } finally { allowed.delete();outside.delete() }
    }
}
