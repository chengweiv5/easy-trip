package com.yangchengwei.easytrip

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertTrue
import org.junit.Test

class AppIconManifestTest {
    @Test fun manifestDeclaresLauncherAndRoundLauncher() {
        val manifest = String(Files.readAllBytes(Paths.get("src/main/AndroidManifest.xml")))
        assertTrue(manifest.contains("android:icon=\"@mipmap/ic_launcher\""))
        assertTrue(manifest.contains("android:roundIcon=\"@mipmap/ic_launcher_round\""))
    }
}
