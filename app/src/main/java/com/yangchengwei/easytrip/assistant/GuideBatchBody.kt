package com.yangchengwei.easytrip.assistant

import java.io.File
import java.io.FilterOutputStream
import java.util.Base64
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

/** One representation drives both exact byte accounting and streaming writes. */
class GuideBatchBody(private val protocol: GuideWireProtocol = LegacyGuideWireProtocol) {
    private sealed interface Part {
        data class Text(val bytes: ByteArray) : Part
        data class Image(val file: File, val length: Long) : Part
    }
    private fun parts(config: ProviderConfig, context: GuideContext, group: GuideGroup,
        policy: GuideBatchPolicy, repair: Boolean): List<Part> {
        val meta = protocol.metadata(config, context, group.sources.map { it.source }, policy, repair)
        val result = mutableListOf<Part>()
        fun text(value: String) { result += Part.Text(value.toByteArray(Charsets.UTF_8)) }
        text("{")
        meta.entries.forEachIndexed { index, (key, value) ->
            if (index > 0) text(",")
            text(JsonPrimitive(key).toString() + ":")
            if (key != "messages") text(value.toString())
            else {
                text(value.toString().dropLast(1))
                text(",{\"role\":\"user\",\"content\":[")
                group.sources.forEachIndexed { n, source ->
                    if (n > 0) text(",")
                    text(buildJsonObject { put("type", "text"); put("text",
                        if (source.source.ordinal == null) "目标text：提取前述正文"
                        else "目标${source.source.token}：以下图片是唯一对应来源") }.toString())
                    if (source.source.ordinal != null) {
                        val file = requireNotNull(source.file)
                        val length = requireNotNull(source.byteCount)
                        require(file.isFile && file.length() == length &&
                            length in 1..AssistantImagePolicy.MAX_PREPARED_BYTES.toLong())
                        text(",{\"type\":\"image_url\",\"image_url\":{\"url\":\"data:image/jpeg;base64,")
                        result += Part.Image(file, length)
                        text("\"}}")
                    }
                }
                text("]}]")
            }
        }
        text("}")
        return result
    }
    private fun length(parts: List<Part>): Long = parts.sumOf {
        when (it) { is Part.Text -> it.bytes.size.toLong(); is Part.Image -> 4L * ((it.length + 2) / 3) }
    }
    fun size(config: ProviderConfig, context: GuideContext, group: GuideGroup,
        policy: GuideBatchPolicy, repair: Boolean): Long = length(parts(config, context, group, policy, repair))

    fun groups(config: ProviderConfig, context: GuideContext, sources: List<GuidePreparedSource>,
        policy: GuideBatchPolicy): GuideGroupPlan {
        val groups = mutableListOf<GuideGroup>()
        val failures = mutableListOf<ParsedSource>()
        var current = emptyList<GuidePreparedSource>()
        fun fits(values: List<GuidePreparedSource>): Boolean {
            val g = GuideGroup(groups.size + 1, values)
            return maxOf(size(config, context, g, policy, false), size(config, context, g, policy, true)) <= policy.bodyLimit
        }
        for (source in sources) {
            try {
                if (!fits(listOf(source))) throw AssistantFailure("这份素材超过请求体预算，尚未发送；其他素材继续处理。")
                if (current.isNotEmpty() && !fits(current + source)) {
                    groups += GuideGroup(groups.size + 1, current); current = emptyList()
                }
                current = current + source
            } catch (_: Exception) {
                failures += ParsedSource(source.source.localId, emptyList(),
                    AssistantFailure("这份素材无法组包或超过请求体预算，尚未发送；原素材保留。"))
            }
        }
        if (current.isNotEmpty()) groups += GuideGroup(groups.size + 1, current)
        return GuideGroupPlan(groups, failures)
    }

    suspend fun write(config: ProviderConfig, context: GuideContext, group: GuideGroup,
        policy: GuideBatchPolicy, repair: Boolean, directory: File): File = withContext(Dispatchers.IO) {
        currentCoroutineContext().ensureActive()
        val parts = parts(config, context, group, policy, repair)
        val expected = length(parts)
        if (expected > policy.bodyLimit) throw AssistantFailure("请求超过安全上限，尚未发送，请分批。")
        check(directory.isDirectory)
        val target = File(directory, "body-${UUID.randomUUID()}.json")
        val temporary = File(directory, "${target.name}.part")
        try {
            temporary.outputStream().buffered().use { out ->
                for (part in parts) {
                    currentCoroutineContext().ensureActive()
                    when (part) {
                        is Part.Text -> out.write(part.bytes)
                        is Part.Image -> {
                            check(part.file.isFile && part.file.length() == part.length)
                            val keepOpen = object : FilterOutputStream(out) {
                                override fun close() { flush() }
                                override fun write(b: ByteArray, off: Int, len: Int) { this.out.write(b, off, len) }
                            }
                            Base64.getEncoder().wrap(keepOpen).use { encoded ->
                                part.file.inputStream().use { input ->
                                    val buffer = ByteArray(12 * 1024)
                                    var count = 0L
                                    while (true) {
                                        currentCoroutineContext().ensureActive()
                                        val n = input.read(buffer)
                                        if (n < 0) break
                                        count += n; check(count <= part.length)
                                        encoded.write(buffer, 0, n)
                                    }
                                    check(count == part.length)
                                }
                            }
                        }
                    }
                }
            }
            check(temporary.length() == expected)
            check(temporary.renameTo(target))
            target
        } catch (e: Exception) { temporary.delete(); target.delete(); throw e }
    }
}
