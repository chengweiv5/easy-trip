package com.yangchengwei.easytrip.settings

import androidx.test.platform.app.InstrumentationRegistry
import java.io.ByteArrayInputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class UpdateServiceTest {
    private val bytes = "a complete apk fixture".toByteArray()
    private val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    private fun release() = OfficialRelease("v1.8.0", "notes", false, false, ReleaseAsset("easy-trip-v1.8.0-release.apk", "https://github.com/chengweiv5/easy-trip/releases/download/v1.8.0/easy-trip-v1.8.0-release.apk", bytes.size.toLong(), hash))
    private class Connection(url: URL, private val body: ByteArray, private val status: Int = 200, private val redirect: String? = null): HttpURLConnection(url) {
        override fun connect() = Unit
        override fun disconnect() = Unit
        override fun usingProxy() = false
        override fun getResponseCode() = status
        override fun getInputStream() = ByteArrayInputStream(body)
        override fun getContentLengthLong() = body.size.toLong()
        override fun getHeaderField(name: String?) = if (name == "Location") redirect else null
    }
    private fun directory() = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "update-test-${System.nanoTime()}").apply { mkdirs() }
    @Test fun officialJsonSelectsOnlyExactApkAndRequiresSha256Digest() {
        val body = """{"tag_name":"v1.8.0","draft":false,"prerelease":false,"body":"中文说明","assets":[{"name":"easy-trip-v1.8.0-release.apk","browser_download_url":"${release().asset!!.url}","size":${bytes.size},"digest":"sha256:$hash"}]}"""
        val parsed = parseOfficialRelease(body)
        assertEquals("中文说明", parsed.notes)
        assertTrue(decideRelease("1.7.0", parsed) is ReleaseDecision.Available)
        assertFalse(parseOfficialRelease(body.replace("sha256:", "")).asset!!.isTrusted(parsed.tag))
    }
    @Test fun verifiedDownloadReportsProgressAndYieldsOnlyCompletedFile(): Unit=runBlocking {
        val dir=directory();var verified=false;val progress=mutableListOf<Long>()
        val service=GitHubUpdateService(dir, { file, _ -> assertArrayEquals(bytes,file.readBytes());verified=true }, { Connection(it,bytes) })
        val file=service.download(release()) { n,_ -> progress+=n }
        assertTrue(verified);assertArrayEquals(bytes,file.readBytes());assertEquals(listOf(bytes.size.toLong()),progress)
        assertEquals(listOf(file),dir.listFiles()!!.toList());service.discard(file);assertTrue(dir.listFiles()!!.isEmpty());dir.delete()
    }
    @Test fun digestOrIdentityFailureLeavesNoInstallableFile(): Unit=runBlocking {
        val dir=directory()
        val bad=release().copy(asset=release().asset!!.copy(sha256="0".repeat(64)))
        val service=GitHubUpdateService(dir, { _,_ -> }, { Connection(it,bytes) })
        assertTrue(runCatching { service.download(bad) { _,_ -> } }.isFailure)
        assertTrue(dir.listFiles()!!.isEmpty())
        val invalidApk=GitHubUpdateService(dir, { _,_ -> error("signature mismatch") }, { Connection(it,bytes) })
        assertTrue(runCatching { invalidApk.download(release()) { _,_ -> } }.isFailure)
        assertTrue(dir.listFiles()!!.isEmpty());dir.delete()
    }
    @Test fun unsafeRedirectNeverConnectsToUntrustedHost(): Unit=runBlocking {
        val dir=directory();val hosts=mutableListOf<String>()
        val service=GitHubUpdateService(dir, { _,_ -> }, { hosts+=it.host;Connection(it,bytes,302,"https://evil.example/file.apk") })
        assertTrue(runCatching { service.download(release()) { _,_ -> } }.isFailure)
        assertEquals(listOf("github.com"),hosts);assertTrue(dir.listFiles()!!.isEmpty());dir.delete()
    }
    @Test fun cancellationDuringDownloadRemovesPartialPackage(): Unit = runBlocking {
        val dir=directory()
        val service=GitHubUpdateService(dir, { _,_ -> error("cancelled download must not verify") }, { Connection(it,bytes) })
        val result=runCatching { service.download(release()) { _,_ -> throw kotlinx.coroutines.CancellationException("user cancelled") } }
        assertTrue(result.exceptionOrNull() is kotlinx.coroutines.CancellationException)
        assertTrue(dir.listFiles()!!.isEmpty());dir.delete()
    }
    @Test fun wrongSizeOrStatusNeverCreatesAnInstallableFile(): Unit = runBlocking {
        val dir=directory()
        val service=GitHubUpdateService(dir, { _,_ -> error("must not verify") }, { Connection(it,bytes) })
        assertTrue(runCatching { service.download(release().copy(asset=release().asset!!.copy(size=999))) { _,_ -> } }.isFailure)
        val denied=GitHubUpdateService(dir, { _,_ -> error("must not verify") }, { Connection(it,bytes,403) })
        assertTrue(runCatching { denied.download(release()) { _,_ -> } }.isFailure)
        assertTrue(dir.listFiles()!!.isEmpty());dir.delete()
    }
    @Test fun metadataHasNoHiddenDownloadSideEffect(): Unit=runBlocking {
        val dir=directory();val urls=mutableListOf<String>()
        val service=GitHubUpdateService(dir, { _,_ -> error("should not verify") }, { urls+=it.toString();Connection(it,"""{"tag_name":"v1.6.0","draft":false,"prerelease":false,"assets":[]}""".toByteArray()) })
        assertEquals(ReleaseDecision.Ahead,decideRelease("1.7.0",service.latest()))
        assertEquals(listOf("https://api.github.com/repos/chengweiv5/easy-trip/releases/latest"),urls);assertTrue(dir.listFiles()!!.isEmpty());dir.delete()
    }
}
