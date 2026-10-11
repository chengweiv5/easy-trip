package com.yangchengwei.easytrip.assistant

import kotlin.math.min
import kotlin.math.sqrt

object AssistantImagePolicy {
    const val MAX_IMAGES = 6
    const val MAX_SINGLE_BYTES = 10L * 1024 * 1024
    const val MAX_TOTAL_BYTES = 30L * 1024 * 1024
    const val MAX_PREPARED_BYTES = 4 * 1024 * 1024

    fun rejection(existingCount: Int, totalBytes: Long, nextBytes: Long): String? = when {
        existingCount >= MAX_IMAGES -> "每批最多 6 张图片"
        nextBytes <= 0 -> "图片为空或无法读取"
        nextBytes > MAX_SINGLE_BYTES -> "单张图片不能超过 10 MB"
        totalBytes < 0 || totalBytes > MAX_TOTAL_BYTES || nextBytes > MAX_TOTAL_BYTES - totalBytes -> "图片总计不能超过 30 MB"
        else -> null
    }

    fun targetSize(width: Int, height: Int): Pair<Int, Int> {
        if (width <= 0 || height <= 0 || width > 50_000 || height > 50_000 ||
            width.toLong() * height > 1_000_000_000L) throw AssistantFailure("图片尺寸过大或无效，请重新选择。")
        val scale = min(1.0, min(4096.0 / maxOf(width, height), sqrt(8_388_608.0 / (width.toLong() * height))))
        return maxOf(1, (width * scale).toInt()) to maxOf(1, (height * scale).toInt())
    }

    /** Inspect container chunks, not a filename or a provider's claimed MIME. */
    fun mime(bytes: ByteArray): String {
        fun match(offset: Int, text: String) = offset >= 0 && offset + text.length <= bytes.size &&
            text.indices.all { bytes[offset + it].toInt() and 255 == text[it].code }
        fun uint(offset: Int, little: Boolean): Long {
            if (offset < 0 || offset + 4 > bytes.size) throw AssistantFailure("图片文件不完整。")
            return (0..3).fold(0L) { value, i ->
                value or ((bytes[offset + i].toLong() and 255) shl (if (little) i * 8 else (3 - i) * 8))
            }
        }
        if (bytes.size >= 3 && bytes[0].toInt() and 255 == 255 && bytes[1].toInt() and 255 == 216 &&
            bytes[2].toInt() and 255 == 255) return "image/jpeg"
        if (bytes.size >= 8 && bytes.take(8).map { it.toInt() and 255 } == listOf(137,80,78,71,13,10,26,10)) {
            var offset = 8
            while (offset + 12 <= bytes.size) {
                val size = uint(offset, false)
                if (match(offset + 4, "acTL")) throw AssistantFailure("仅支持静态图片，不支持动画 PNG。")
                if (size > bytes.size - offset - 12) throw AssistantFailure("图片文件不完整。")
                offset += size.toInt() + 12
            }
            return "image/png"
        }
        if (match(0, "RIFF") && match(8, "WEBP")) {
            var offset = 12
            while (offset + 8 <= bytes.size) {
                val size = uint(offset + 4, true)
                if (match(offset, "ANIM") || match(offset, "ANMF") ||
                    (match(offset, "VP8X") && offset + 8 < bytes.size && bytes[offset + 8].toInt() and 2 != 0))
                    throw AssistantFailure("仅支持静态图片，不支持动画 WebP。")
                if (size > bytes.size - offset - 8) throw AssistantFailure("图片文件不完整。")
                offset += size.toInt() + 8 + (size % 2).toInt()
            }
            return "image/webp"
        }
        throw AssistantFailure("仅支持 JPG、PNG、WebP 静态图片。")
    }
}
