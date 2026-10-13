package com.yangchengwei.easytrip.assistant

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.coroutines.resume
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class AssistantLinkDomTest {
    private suspend fun extract(platform: AssistantLinkPlatform, html: String): JsonObject =
        withContext(Dispatchers.Main) {
            val web = WebView(InstrumentationRegistry.getInstrumentation().targetContext)
            try {
                val loaded = CompletableDeferred<Unit>()
                web.settings.javaScriptEnabled = true
                web.settings.blockNetworkLoads = true
                web.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) { loaded.complete(Unit) }
                }
                web.loadDataWithBaseURL(
                    if (platform == AssistantLinkPlatform.CTRIP) "https://m.ctrip.com/webapp/you/community/detail?articleId=123"
                    else "https://www.xiaohongshu.com/discovery/item/abc123", html, "text/html", "UTF-8", null)
                withTimeout(5_000) { loaded.await() }
                val value = suspendCancellableCoroutine<String> { continuation ->
                    web.evaluateJavascript(AssistantLinkDom.script(platform)) {
                        if (continuation.isActive) continuation.resume(Json.parseToJsonElement(it).jsonPrimitive.content)
                    }
                }
                Json.parseToJsonElement(value).jsonObject
            } finally { web.destroy() }
        }

    @Test fun ctripNestedArticleChoosesBodyNotBackgroundOrRecommendations() = runBlocking {
        val body = "青州古城，井塘古村。"
        val result = extract(AssistantLinkPlatform.CTRIP, """
            <html><body><div class="article-content-container">
            <h1 class="article-title">青州周末</h1>
            <div class="article-content-container article-content-collapsed">$body</div>
            <div class="article-content-container-background">$body</div><div>携程推荐：无关地点</div></div>
            <div class="swiper-item-image"><img src="https://dimg04.c-ctrip.com/images/1.jpg"></div></body></html>
        """.trimIndent())
        assertEquals(body, result["body"]!!.jsonPrimitive.content)
        assertEquals(1, result["expected"]!!.jsonPrimitive.int)
        val input = AssistantLinkInput.parse("https://m.ctrip.com/webapp/you/community/detail?articleId=123")!!
        assertEquals(1, AssistantLinkDom.decode(input, result)!!.validated(input).imageUrls.size)
    }

    @Test fun xhsSingleImageWithoutCounterIsReadButUnloadedCarouselIsNotComplete() = runBlocking {
        val html = """<html><body><div class="narmal-note-container">
            <div class="author-desc-content">青州周末<br>青州古城</div>
            <img src="https://sns-webpic-qc.xhscdn.com/notes_pre_post/1">
            <img src="https://sns-avatar.xhscdn.com/avatar/1"></div>
            <div>猜你喜欢<img src="https://sns-webpic-qc.xhscdn.com/notes_pre_post/unrelated"></div></body></html>"""
        val input = AssistantLinkInput.parse("https://xhslink.cn/o/a")!!
        val single = extract(AssistantLinkPlatform.XIAOHONGSHU, html)
        assertEquals(1, AssistantLinkDom.decode(input, single)!!.validated(input).imageUrls.size)
        val incomplete = extract(AssistantLinkPlatform.XIAOHONGSHU, html.replace("<img src=\"https://sns-webpic-qc.xhscdn.com/notes_pre_post/1\">", "<div>1/3</div>"))
        assertNull(AssistantLinkDom.decode(input, incomplete))
    }
}
