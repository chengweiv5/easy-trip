package com.yangchengwei.easytrip.assistant

import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink

class AssistantTransport(private val client: OkHttpClient = OkHttpClient.Builder()
    .followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false)
    .callTimeout(45, TimeUnit.SECONDS).connectTimeout(15, TimeUnit.SECONDS).build()) {
    suspend fun post(config: ProviderConfig, json: String, onResponseWait: () -> Unit = {}): String {
        val current = config.validated()
        val bytes = json.toByteArray(Charsets.UTF_8)
        if (bytes.size > 6 * 1024 * 1024) throw AssistantFailure("请求超过安全上限，尚未发送，请分批。")
        val body = bytes.toRequestBody("application/json".toMediaType())
        val tracked = object : RequestBody() {
            override fun contentType() = body.contentType()
            override fun contentLength() = body.contentLength()
            override fun writeTo(sink: BufferedSink) { body.writeTo(sink); onResponseWait() }
        }
        val request = Request.Builder().url(current.chatUrl).header("Authorization", "Bearer ${current.apiKey}").post(tracked).build()
        return suspendCancellableCoroutine { continuation ->
            val call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(AssistantFailure(
                        "网络中断或超时，输入已保留；内容可能已发送，重试可能产生费用。",
                        AssistantFailureCode.NETWORK, SendEvidence.POSSIBLY_SENT))
                }
                override fun onResponse(call: Call, response: Response) {
                    val result = runCatching {
                        response.use {
                            val source = it.body?.source() ?: throw AssistantFailure("模型返回空响应。",
                                AssistantFailureCode.PROTOCOL, SendEvidence.RESPONSE_RECEIVED)
                            if (source.request(256 * 1024L + 1)) throw AssistantFailure("模型响应超过安全上限。",
                                AssistantFailureCode.PROTOCOL, SendEvidence.RESPONSE_RECEIVED)
                            val text = source.readUtf8()
                            if (!it.isSuccessful) throw failure(it.code, text)
                            text
                        }
                    }
                    if (continuation.isActive) result.fold({ continuation.resume(it) }, {
                        continuation.resumeWithException(it as? AssistantFailure ?: AssistantFailure("读取响应失败，内容可能已发送，请手动重试。",
                            AssistantFailureCode.NETWORK, SendEvidence.RESPONSE_RECEIVED))
                    })
                }
            })
        }
    }
    internal fun failure(status: Int, body: String): AssistantFailure {
        val error = runCatching { Json.parseToJsonElement(body).jsonObject["error"]?.jsonObject }.getOrNull()
        val code = error?.get("code")?.jsonPrimitive?.content.orEmpty().lowercase()
        val message = error?.get("message")?.jsonPrimitive?.content.orEmpty().lowercase()
        val unsupported = status in setOf(400, 422) && (code in setOf("unsupported_image", "unsupported_image_input", "image_input_not_supported") ||
            message.contains("does not support image") || message.contains("image input is not supported"))
        val (reason, kind) = when {
            unsupported -> "服务收到请求后拒绝图片输入。请检查模型图片能力，原素材已保留。" to AssistantFailureCode.UNSUPPORTED_IMAGE
            status == 401 || status == 403 -> "鉴权失败，请检查助手设置中的 Key。" to AssistantFailureCode.AUTH
            status == 429 -> "模型服务限流或额度不足，请稍后手动重试。" to AssistantFailureCode.LIMIT
            status in 300..399 -> "接口重定向已被阻止，未向新地址转发凭据。" to AssistantFailureCode.PROTOCOL
            else -> "服务收到请求后返回 HTTP $status，原输入已保留；未查询地点。" to AssistantFailureCode.PROTOCOL
        }
        return AssistantFailure(reason, kind, SendEvidence.RESPONSE_RECEIVED)
    }
}
