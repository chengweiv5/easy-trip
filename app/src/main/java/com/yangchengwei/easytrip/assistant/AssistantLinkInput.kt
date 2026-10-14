package com.yangchengwei.easytrip.assistant

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.net.InetAddress

enum class AssistantLinkPlatform(val label: String) { CTRIP("携程"), XIAOHONGSHU("小红书") }

data class AssistantLinkInput(val platform: AssistantLinkPlatform, val url: String, val context: String) {
    override fun toString() = "AssistantLinkInput(platform=$platform, content=redacted)"
    companion object {
        private val urls = Regex("https?://[^\\s<>\\[\\]【】\"“”]+", RegexOption.IGNORE_CASE)
        private val shareSuffix = Regex("(?:复制内容后去|复制一下[，,]?跳转|进入|打开)【小红书】")
        fun parse(text: String): AssistantLinkInput? {
            // Separate only known share boilerplate, never strip arbitrary Unicode or signed parameters.
            val searchable = shareSuffix.replace(text) { " ${it.value}" }
            val matches = urls.findAll(searchable).toList()
            if (matches.isEmpty()) return null
            val found = matches.map { it.value.trimEnd('。', '，', '；', '！', '？', '.', ',', ';', '!', '?', ')', '）') }.distinct()
            if (found.size != 1) throw AssistantFailure("每次请粘贴一条携程或小红书笔记链接，原输入已保留。")
            val url = AssistantLinkPolicy.https(found.single())
            val platform = when {
                url.host in setOf("xhslink.cn", "xhslink.com") && url.encodedPath != "/" -> AssistantLinkPlatform.XIAOHONGSHU
                url.host in setOf("www.xiaohongshu.com", "xiaohongshu.com") &&
                    Regex("^/(explore|discovery/item)/[a-zA-Z0-9]+/?$").matches(url.encodedPath) -> AssistantLinkPlatform.XIAOHONGSHU
                url.host == "m.ctrip.com" && url.encodedPath == "/webapp/you/community/detail" &&
                    url.queryParameter("articleId")?.matches(Regex("\\d+")) == true -> AssistantLinkPlatform.CTRIP
                else -> throw AssistantFailure("目前仅支持携程和小红书的笔记链接，请换一条链接或直接输入地点。")
            }
            val context = urls.replace(searchable, "").replace(Regex(
                "(?:复制内容后去【小红书】，?直接看笔记详情[。.]?|复制一下，?跳转【小红书】即刻浏览笔记[。.]?|复制内容后去【小红书】)"
            ), "")
                .trim().trimEnd('。', '，').take(2000)
            return AssistantLinkInput(platform, url.toString(), context)
        }
    }
}

object AssistantLinkPolicy {
    fun https(raw: String): HttpUrl {
        val url = raw.toHttpUrlOrNull()
        if (url == null || !url.isHttps || url.port != 443 || url.username.isNotEmpty() || url.password.isNotEmpty())
            throw AssistantFailure("链接地址不安全或不完整，请重新复制 HTTPS 分享链接。")
        return url
    }
    private fun hostIn(host: String, domains: Set<String>) = domains.any { host == it || host.endsWith(".$it") }
    fun navigation(platform: AssistantLinkPlatform, raw: String): Boolean {
        val url = runCatching { https(raw) }.getOrNull() ?: return false
        return when (platform) {
            AssistantLinkPlatform.CTRIP -> url.host == "m.ctrip.com"
            AssistantLinkPlatform.XIAOHONGSHU -> url.host in setOf("xhslink.cn", "xhslink.com", "www.xiaohongshu.com", "xiaohongshu.com")
        }
    }
    fun resource(platform: AssistantLinkPlatform, raw: String): Boolean {
        val url = runCatching { https(raw) }.getOrNull() ?: return false
        return when (platform) {
            AssistantLinkPlatform.CTRIP -> hostIn(url.host, setOf("ctrip.com", "c-ctrip.com", "tripcdn.com", "tripcdn.cn"))
            AssistantLinkPlatform.XIAOHONGSHU -> hostIn(url.host, setOf("xiaohongshu.com", "xhscdn.com", "xiaohongshu.com.cn", "xhslink.cn", "xhslink.com"))
        }
    }
    fun image(platform: AssistantLinkPlatform, raw: String): String {
        val original = raw.toHttpUrlOrNull() ?: throw AssistantFailure("笔记配图地址无效。")
        val url = if (platform == AssistantLinkPlatform.XIAOHONGSHU && original.host.endsWith(".xhscdn.com"))
            original.newBuilder().scheme("https").build() else original
        https(url.toString())
        val allowed = when (platform) {
            AssistantLinkPlatform.CTRIP -> url.host.matches(Regex("dimg\\d+\\.c-ctrip\\.com")) && url.encodedPath.startsWith("/images/")
            AssistantLinkPlatform.XIAOHONGSHU -> url.host.matches(Regex("sns-webpic(?:-[a-z0-9]+)?\\.xhscdn\\.com")) &&
                (url.encodedPath.contains("/notes_pre_post/") ||
                    url.encodedPath.matches(Regex("/\\d{12}/[a-fA-F0-9]{32}/1040[a-zA-Z0-9]+(?:![a-zA-Z0-9_]+)?")))
        }
        if (!allowed) throw AssistantFailure("笔记配图来源不受支持，未下载不可信图片。")
        return url.toString()
    }

    /** Identity for duplicates within one original note; retain the original signed URL for downloads. */
    fun imageIdentity(platform: AssistantLinkPlatform, raw: String): String {
        val url = image(platform, raw).toHttpUrlOrNull()!!
        return when (platform) {
            AssistantLinkPlatform.XIAOHONGSHU -> url.encodedPath.substringAfter("/notes_pre_post/", "")
                .ifEmpty { url.encodedPath.substringAfterLast('/') }.substringBefore('!')
            AssistantLinkPlatform.CTRIP -> url.encodedPath.substringAfter("/images/")
                .replace(Regex("_(?:W|R)_\\d+.*$"), "").substringBefore(".jpg").substringBefore(".png").substringBefore(".webp")
        }
    }
    fun isPublicAddress(address: InetAddress): Boolean {
        if (address.isAnyLocalAddress || address.isLoopbackAddress || address.isLinkLocalAddress ||
            address.isSiteLocalAddress || address.isMulticastAddress) return false
        val bytes = address.address
        if (bytes.size == 16 && (bytes[0].toInt() and 0xfe) == 0xfc) return false
        if (bytes.size == 4) {
            val a = bytes[0].toInt() and 255
            val b = bytes[1].toInt() and 255
            if (a == 0 || a >= 224 || (a == 100 && b in 64..127) || (a == 198 && b in 18..19)) return false
        }
        return true
    }
}

data class AssistantLinkDocument(
    val platform: AssistantLinkPlatform,
    val url: String,
    val title: String,
    val body: String,
    val imageUrls: List<String>,
    val expectedImages: Int,
) {
    override fun toString() = "AssistantLinkDocument(platform=$platform, images=${imageUrls.size}, content=redacted)"
    fun validated(input: AssistantLinkInput): AssistantLinkDocument {
        if (platform != input.platform || !AssistantLinkPolicy.navigation(platform, url))
            throw AssistantFailure("笔记跳转到了不受支持的页面。")
        val finalUrl = AssistantLinkPolicy.https(url)
        val original = AssistantLinkPolicy.https(input.url)
        val sameNote = when (platform) {
            AssistantLinkPlatform.CTRIP -> finalUrl.encodedPath == "/webapp/you/community/detail" &&
                finalUrl.queryParameter("articleId") == original.queryParameter("articleId")
            AssistantLinkPlatform.XIAOHONGSHU -> finalUrl.host in setOf("www.xiaohongshu.com", "xiaohongshu.com") &&
                Regex("^/(explore|discovery/item)/[a-zA-Z0-9]+/?$").matches(finalUrl.encodedPath) &&
                (original.host in setOf("xhslink.cn", "xhslink.com") ||
                    finalUrl.pathSegments.filter { it.isNotBlank() }.last() == original.pathSegments.filter { it.isNotBlank() }.last())
        }
        if (!sameNote) throw AssistantFailure("未读取到原始笔记，请重新复制分享链接。")
        if (title.isBlank() || body.isBlank()) throw AssistantFailure("未读取到完整笔记正文，请重试或改用文字／图片。")
        if (body.length > 20_000 || title.length > 500)
            throw AssistantFailure("笔记文字过长，请改为分段粘贴地点文字。")
        val images = imageUrls.map { AssistantLinkPolicy.image(platform, it) }
            .distinctBy { AssistantLinkPolicy.imageIdentity(platform, it) }
        if (expectedImages < 0 || expectedImages > AssistantImagePolicy.MAX_IMAGES || images.size > AssistantImagePolicy.MAX_IMAGES)
            throw AssistantFailure("笔记超过 ${AssistantImagePolicy.MAX_IMAGES} 张图片，请分批选择图片；没有截断笔记。")
        if (expectedImages != images.size) throw AssistantFailure("笔记图片尚未读取完整，请重试；没有只处理部分图片。")
        return copy(imageUrls = images)
    }
}

fun interface AssistantLinkReader {
    suspend fun read(input: AssistantLinkInput): AssistantLinkDocument
}

data class LoadedAssistantLink(val modelText: String, val images: List<AssistantImage>) {
    override fun toString() = "LoadedAssistantLink(images=${images.size}, content=redacted)"
}

interface AssistantLinkLoader {
    suspend fun load(sessionId: String, input: AssistantLinkInput, existing: List<AssistantImage>,
        onProgress: (String) -> Unit): LoadedAssistantLink
    suspend fun discard(sessionId: String, images: List<AssistantImage>)
}
