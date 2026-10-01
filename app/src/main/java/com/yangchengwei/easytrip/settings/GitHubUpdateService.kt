package com.yangchengwei.easytrip.settings

import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Fixed official origin; never accepts arbitrary download URLs from a document or user input. */
class GitHubUpdateService(
    private val directory: File,
    private val verifyApk: (File, OfficialRelease) -> Unit,
    private val connect: (URL) -> HttpURLConnection = { it.openConnection() as HttpURLConnection },
) : UpdateService {
    override suspend fun latest(): OfficialRelease = withContext(Dispatchers.IO) {
        val connection = open("https://api.github.com/repos/chengweiv5/easy-trip/releases/latest", metadata = true)
        try {
            require(connection.responseCode == 200) { "Release unavailable" }
            val text = connection.inputStream.use { input ->
                val buffer = java.io.ByteArrayOutputStream()
                val bytes = ByteArray(8192)
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val count = input.read(bytes)
                    if (count < 0) break
                    require(buffer.size() + count <= 1024 * 1024) { "Release response too large" }
                    buffer.write(bytes, 0, count)
                }
                buffer.toString("UTF-8")
            }
            parseOfficialRelease(text)
        } finally { connection.disconnect() }
    }

    override suspend fun download(release: OfficialRelease, progress: (Long, Long) -> Unit): File {
        var produced: File? = null
        try {
            return withContext(Dispatchers.IO) { downloadVerified(release, progress).also { produced = it } }
        } catch (error: Exception) {
            // withContext may discard a successful IO result if cancellation wins on return.
            produced?.delete()
            throw error
        }
    }

    private suspend fun downloadVerified(release: OfficialRelease, progress: (Long, Long) -> Unit): File {
        val asset = requireNotNull(release.asset)
        require(asset.isTrusted(release.tag))
        require(directory.isDirectory || directory.mkdirs())
        val temporary = File(directory, "${UUID.randomUUID()}.part")
        val completed = File(directory, "${UUID.randomUUID()}.apk")
        var success = false
        try {
            val connection = open(asset.url, metadata = false)
            try {
                require(connection.responseCode == 200)
                val length = connection.contentLengthLong
                require(length == -1L || length == asset.size) { "Package length mismatch" }
                val digest = MessageDigest.getInstance("SHA-256")
                var total = 0L
                connection.inputStream.use { input -> temporary.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= asset.size && total <= MAX_APK_BYTES) { "Package too large" }
                        digest.update(buffer, 0, count)
                        output.write(buffer, 0, count)
                        progress(total, asset.size)
                    }
                } }
                require(total == asset.size) { "Incomplete package" }
                val actual = digest.digest().joinToString("") { "%02x".format(it) }
                require(actual.equals(asset.sha256, ignoreCase = true)) { "Package digest mismatch" }
            } finally { connection.disconnect() }
            currentCoroutineContext().ensureActive()
            verifyApk(temporary, release)
            require(temporary.renameTo(completed))
            currentCoroutineContext().ensureActive()
            success = true
            return completed
        } finally {
            temporary.delete()
            if (!success) completed.delete()
        }
    }

    override fun discard(file: File) {
        if (file.parentFile?.canonicalFile == directory.canonicalFile) file.delete()
    }

    private suspend fun open(address: String, metadata: Boolean): HttpURLConnection {
        var url = address
        repeat(5) {
            currentCoroutineContext().ensureActive()
            val uri = URI(url)
            require(uri.scheme == "https" && uri.userInfo == null && (uri.port == -1 || uri.port == 443))
            require(if (metadata) uri.host == "api.github.com" else uri.host in setOf("github.com", "release-assets.githubusercontent.com", "objects.githubusercontent.com"))
            val connection = connect(URL(url))
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("User-Agent", "EasyTrip-Android")
            connection.setRequestProperty("Accept", if (metadata) "application/vnd.github+json" else "application/octet-stream")
            connection.setRequestProperty("Accept-Encoding", "identity")
            try {
                if (connection.responseCode !in listOf(301, 302, 303, 307, 308)) return connection
                val location = requireNotNull(connection.getHeaderField("Location"))
                url = uri.resolve(location).toString()
            } catch (error: Exception) {
                connection.disconnect()
                throw error
            }
            connection.disconnect()
        }
        error("Too many redirects")
    }
}

fun parseOfficialRelease(json: String): OfficialRelease {
    val root = JSONObject(json)
    val tag = root.getString("tag_name")
    val candidates = root.getJSONArray("assets")
    val assets = (0 until candidates.length()).map { candidates.getJSONObject(it) }
        .filter { it.optString("name") == "easy-trip-$tag-release.apk" }
    val asset = assets.singleOrNull()?.let {
        ReleaseAsset(it.getString("name"), it.getString("browser_download_url"), it.getLong("size"),
            it.optString("digest").takeIf { value -> value.startsWith("sha256:") }?.removePrefix("sha256:").orEmpty())
    }
    return OfficialRelease(tag, root.optString("body").take(30_000), root.getBoolean("draft"), root.getBoolean("prerelease"), asset)
}
