package com.yangchengwei.easytrip.settings

import org.junit.Assert.*
import org.junit.Test

class ReleasePolicyTest {
    @Test fun comparesNumericVersionsWithoutDowngrading() {
        assertTrue(AppVersion.parse("v1.10.0")!! > AppVersion.parse("1.9.9")!!)
        assertEquals(ReleaseDecision.Current, decideRelease("1.7.0", release("1.7.0")))
        assertEquals(ReleaseDecision.Ahead, decideRelease("1.7.0", release("1.6.0")))
        assertTrue(decideRelease("1.7.0", release("1.8.0")) is ReleaseDecision.Available)
    }
    @Test fun invalidOrUnverifiableMetadataNeverBecomesAnUpdate() {
        listOf("1.7", "1.7.0-beta", "1.7.0/evil", "999999999999.0.0").forEach { assertNull(AppVersion.parse(it)) }
        assertThrows(IllegalArgumentException::class.java) { decideRelease("1.7.0", release("1.8.0").copy(draft = true)) }
        assertThrows(IllegalArgumentException::class.java) { decideRelease("1.7.0", release("1.8.0").copy(asset = null)) }
        assertEquals(ReleaseDecision.Ahead, decideRelease("1.7.0", release("1.6.0").copy(asset = null)))
    }
    @Test fun rejectsUntrustedPackageUrlsAndDigestOrSize() {
        val good=release("1.8.0").asset!!
        assertTrue(good.isTrusted("v1.8.0"))
        listOf("http://github.com/chengweiv5/easy-trip/releases/download/v1.8.0/easy-trip-v1.8.0-release.apk", "https://github.com.evil.test/x.apk", "https://github.com/other/repo/releases/download/v1.8.0/a.apk", "https://github.com@evil.test/a.apk").forEach { assertFalse(good.copy(url=it).isTrusted("v1.8.0")) }
        assertFalse(good.copy(sha256="bad").isTrusted("v1.8.0"))
        assertFalse(good.copy(size=0).isTrusted("v1.8.0"))
        assertFalse(good.copy(size=MAX_APK_BYTES+1).isTrusted("v1.8.0"))
    }
    @Test fun upgradeRequiresMatchingPackageSignerAndIncreasingVersionCode() {
        val installed = PackageIdentity("com.yangchengwei.easytrip", "1.7.0", 9, setOf("official"))
        val newer = installed.copy(versionName = "1.8.0", versionCode = 10)
        requireTrustedUpgrade(installed, newer, "v1.8.0")
        listOf(
            newer.copy(packageName = "another.package"),
            newer.copy(versionCode = 9),
            newer.copy(versionCode = 8),
            newer.copy(signers = setOf("other")),
            newer.copy(signers = emptySet()),
            newer.copy(versionName = "1.9.0"),
            newer.copy(versionName = "invalid"),
        ).forEach { candidate ->
            assertThrows(IllegalArgumentException::class.java) { requireTrustedUpgrade(installed, candidate, "v1.8.0") }
        }
        assertThrows(IllegalArgumentException::class.java) { requireTrustedUpgrade(installed.copy(signers=emptySet()), newer, "v1.8.0") }
        assertThrows(IllegalArgumentException::class.java) { requireTrustedUpgrade(installed, installed.copy(versionCode=10), "v1.7.0") }
    }
    private fun release(version: String)=OfficialRelease("v$version", "更新说明", false, false,
        ReleaseAsset("easy-trip-v$version-release.apk", "https://github.com/chengweiv5/easy-trip/releases/download/v$version/easy-trip-v$version-release.apk", 100, "a".repeat(64)))
}
