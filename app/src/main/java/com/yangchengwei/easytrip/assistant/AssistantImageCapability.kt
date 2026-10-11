package com.yangchengwei.easytrip.assistant

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class ImageCapability { UNKNOWN, AVAILABLE, UNSUPPORTED }
data class ImageCapabilityResult(val capability: ImageCapability, val checkedAt: Long?)
fun ProviderConfig.sameAs(other: ProviderConfig?) = other != null && baseUrl == other.baseUrl &&
    model == other.model && apiKey == other.apiKey

/** Explicit user action only. The expected answer is in synthetic pixels, not the prompt. */
suspend fun checkImageCapability(config: ProviderConfig): ImageCapabilityResult {
    val (city, place) = listOf("杭州" to "雷峰塔", "北京" to "天坛", "苏州" to "拙政园").random()
    val prepared = withContext(Dispatchers.Default) {
        val bitmap = Bitmap.createBitmap(640, 320, Bitmap.Config.ARGB_8888)
        try {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 56f }
            Canvas(bitmap).apply { drawColor(Color.WHITE); drawText(city, 48f, 115f, paint); drawText(place, 48f, 220f, paint) }
            val bytes = ByteArrayOutputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it); it.toByteArray() }
            PreparedImage("capability-sample", "image/jpeg", bytes)
        } finally { bitmap.recycle() }
    }
    val result = ImagePlaceParser({ config }).parse(prepared, 1, "", "") {}
    if (result.size != 1 || !sameCity(result.single().city, city) || normalizedPlace(result.single().query) != normalizedPlace(place))
        throw AssistantFailure("服务已收到示例，但没有正确识别图片与工具参数；图片能力尚未验证。")
    return ImageCapabilityResult(ImageCapability.AVAILABLE, System.currentTimeMillis())
}
