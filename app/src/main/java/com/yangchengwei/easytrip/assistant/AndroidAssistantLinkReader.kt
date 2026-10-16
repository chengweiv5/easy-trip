package com.yangchengwei.easytrip.assistant

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.*
import java.io.ByteArrayInputStream
import kotlin.coroutines.resume

/** A bounded read-only page renderer, not an account browser or JavaScript bridge. */
class AndroidAssistantLinkReader(
    context: Context,
    private val diagnostic: (String) -> Unit = { android.util.Log.i("AssistantLinkRead", it) },
) : AssistantLinkReader {
    private val context = context.applicationContext
    override suspend fun read(input: AssistantLinkInput): AssistantLinkDocument = gate.withLock {
        var lastSnapshot: JsonObject? = null
        try {
            diagnostic("begin platform=${input.platform} adapter=${AssistantLinkDom.ADAPTER_VERSION}")
            withTimeoutOrNull(45_000) {
                withContext(Dispatchers.Main.immediate) { render(input) { lastSnapshot = it } }
            } ?: throw AssistantLinkDom.incompleteFailure(lastSnapshot)
        } catch (error: AssistantFailure) {
            diagnostic("failed platform=${input.platform} reason=${lastSnapshot?.get("reason")?.jsonPrimitive?.contentOrNull ?: "network"}")
            throw error
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private suspend fun render(input: AssistantLinkInput, observe: (JsonObject) -> Unit): AssistantLinkDocument {
        val web = WebView(context)
        var error: AssistantFailure? = null
        var pageFinished = CompletableDeferred<Unit>()
        val main = Handler(Looper.getMainLooper())
        fun fail(message: String) {
            if (error == null) error = AssistantFailure(message)
        }
        fun checkFailure() {
            error?.let { throw it }
        }
        try {
            web.layout(0, 0, 1080, 1920)
            web.settings.apply {
                javaScriptEnabled = true
                blockNetworkImage = true
                domStorageEnabled = true
                allowFileAccess = false
                allowContentAccess = false
                setSupportMultipleWindows(false)
                javaScriptCanOpenWindowsAutomatically = false
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                mediaPlaybackRequiresUserGesture = true
                setGeolocationEnabled(false)
            }
            CookieManager.getInstance().setAcceptThirdPartyCookies(web, false)
            web.webChromeClient = object : WebChromeClient() {
                override fun onPermissionRequest(request: PermissionRequest) { request.deny() }
            }
            web.setDownloadListener { _, _, _, _, _ -> fail("链接不是可读取的笔记页面。") }
            web.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val allowed = AssistantLinkPolicy.navigation(input.platform, request.url.toString())
                    if (!allowed) diagnostic("blocked scheme=${request.url.scheme} host=${request.url.host}")
                    // Sites may attempt an optional app deeplink while the public note still loads.
                    // Deny the launch without treating it as a failed HTTPS document.
                    if (!allowed && request.isForMainFrame && request.url.scheme in setOf("http", "https"))
                        fail("页面要求登录／验证或跳转其他网站，请改用文字／图片。")
                    return !allowed
                }
                override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                    val allowed = if (request.isForMainFrame)
                        AssistantLinkPolicy.navigation(input.platform, request.url.toString())
                    else AssistantLinkPolicy.resource(input.platform, request.url.toString())
                    val optionalImage = !request.isForMainFrame && runCatching {
                        AssistantLinkPolicy.image(input.platform, request.url.toString())
                    }.isSuccess
                    if (allowed && !optionalImage) return null
                    if (request.isForMainFrame) main.post { fail("页面要求登录／验证或跳转到了不支持的网站。") }
                    return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
                }
                override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                    diagnostic("start host=${android.net.Uri.parse(url).host}")
                    if (!AssistantLinkPolicy.navigation(input.platform, url))
                        fail("页面要求登录／验证或跳转到了不支持的网站。")
                }
                override fun onPageFinished(view: WebView, url: String) {
                    if (!pageFinished.isCompleted) pageFinished.complete(Unit)
                }
                override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                    if (request.isForMainFrame) diagnostic("network code=${error.errorCode} host=${request.url.host}")
                    if (request.isForMainFrame) fail("笔记网络加载失败，请重试。")
                }
                override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                    fail("网页渲染中断，请重试或改用文字／图片。")
                    return true
                }
                override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) {
                    if (request.isForMainFrame) fail("笔记暂时不可访问，请重试或改用文字／图片。")
                }
            }
            if (input.platform == AssistantLinkPlatform.CTRIP) {
                web.loadUrl("https://m.ctrip.com/")
                while (!pageFinished.isCompleted) { checkFailure(); delay(100) }
                // A normal first-party visit initializes the site's anonymous session.
                // This delay never constitutes article success; the document predicate below does.
                repeat(6) { checkFailure(); delay(500) }
                pageFinished = CompletableDeferred()
            }
            web.loadUrl(input.url)
            var previous: AssistantLinkDocument? = null
            var stable = 0
            while (true) {
                checkFailure()
                val raw = web.readJson(AssistantLinkDom.script(input.platform))
                checkFailure()
                val json = runCatching { Json.parseToJsonElement(raw).jsonObject }.getOrNull()
                if (json != null) {
                    observe(json)
                    diagnostic("dom body=${json["body"]?.jsonPrimitive?.contentOrNull?.length ?: 0} images=${json["images"]?.jsonArray?.size ?: 0} expected=${json["expected"]} reason=${json["reason"]}")
                    if (json["restricted"]?.jsonPrimitive?.booleanOrNull == true)
                        throw AssistantFailure("笔记需要登录／验证或内容不可用，请改用文字／图片。")
                    val document = AssistantLinkDom.decode(input, json)
                    if (document != null) {
                        val validated = document.validated(input)
                        stable = if (previous?.let {
                            it.platform == validated.platform && it.url == validated.url &&
                                it.title == validated.title && it.body == validated.body &&
                                (validated.title.isNotBlank() || validated.body.isNotBlank() ||
                                    it.imageUrls == validated.imageUrls)
                        } == true) stable + 1 else 0
                        previous = validated
                        if (stable >= 2) {
                            diagnostic("ready platform=${input.platform} images=${validated.imageUrls.size}")
                            return validated
                        }
                    } else {
                        // An intermediate incomplete frame must break consecutive-ready evidence.
                        previous = null
                        stable = 0
                    }
                } else {
                    previous = null
                    stable = 0
                }
                delay(700)
            }
        } finally {
            main.removeCallbacksAndMessages(null)
            web.stopLoading()
            web.webChromeClient = null
            web.webViewClient = WebViewClient()
            web.destroy()
        }
    }

    private suspend fun WebView.readJson(script: String): String = suspendCancellableCoroutine { continuation ->
        evaluateJavascript(script) { encoded ->
            if (continuation.isActive) {
                val value = runCatching { Json.parseToJsonElement(encoded).jsonPrimitive.content }.getOrDefault("{}")
                continuation.resume(value)
            }
        }
    }
    companion object { private val gate = Mutex() }
}
