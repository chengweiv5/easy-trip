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
    private fun xhs(images: String, indicator: String = "", extras: String = "") = """
        <div class="narmal-note-container"><div class="author-desc-content">旅行攻略<br>景点</div>
        <div class="image-gallery-container"><div class="onix-carousel">$images</div>$indicator</div>$extras</div>
    """
    private fun xhsImage(id: String, lazy: Boolean = false) = """<div class="onix-carousel-item">
        <div class="note-image-container"><div class="note-image-box">
        <img ${if (lazy) "data-src" else "src"}="https://sns-webpic-qc.xhscdn.com/notes_pre_post/$id">
        </div></div></div>"""
    private fun ctrip(images: String, indicator: String = "", extras: String = "") = """
        <div class="paipai-detail"><div class="article-content-container">景点</div><div class="article-title">旅行攻略</div>
        <div class="swiper-wapper"><div class="swiper-container"><div class="swiper-wrapper">$images</div></div>
        $indicator</div></div>$extras
    """
    private fun ctripImage(id: String) = """<div class="swiper-slide"><div class="swiper-item-image">
        <img src="https://dimg04.c-ctrip.com/images/$id.jpg"></div></div>"""

    @Test fun observedXhs1040CarouselKeepsAllImagesAndExcludesAvatar() = runBlocking {
        val images = (1..10).joinToString("") { xhsImage("$it").replace("notes_pre_post/$it",
            "202610142202/8a3598d07b386e520d0d445654cbe3d7/1040abc$it!h5_1080jpg") }
        val json = extract(AssistantLinkPlatform.XIAOHONGSHU, xhs(images,
            """<div class="carousel-indicator">1/10</div>""", """<img src="https://sns-avatar-qc.xhscdn.com/avatar/1">"""))
        val input = AssistantLinkInput.parse("https://xhslink.cn/o/a")!!
        assertEquals(10, AssistantLinkDom.decode(input, json)!!.validated(input).imageUrls.size)
    }

    @Test fun originalCarouselCountMetadataSupportsAlternativeCounterWithoutBackgroundFraction() = runBlocking {
        val html = ctrip((1..3).joinToString("") { ctripImage("$it") },
            """<span class="swiper-tip">共3张</span>""").replace("景点</div>", "景点<div>1/5</div></div>")
        val json = extract(AssistantLinkPlatform.CTRIP, html)
        assertEquals(3, json["expected"]!!.jsonPrimitive.int)
        assertEquals(3, json["images"]!!.jsonArray.size)
    }

    @Test fun unknownCountAndLazySlidesCannotMasqueradeAsCompleteSingleImage() = runBlocking {
        for (html in listOf(xhs(xhsImage("1")), xhs(xhsImage("1") + xhsImage("2", true)))) {
            val json = extract(AssistantLinkPlatform.XIAOHONGSHU, html)
            assertNull(AssistantLinkDom.decode(AssistantLinkInput.parse("https://xhslink.cn/o/a")!!, json))
        }
    }

    @Test fun carouselAriaSetSizeProvesSingleImageWithoutGuessingFromImgCount() = runBlocking {
        val html = xhs(xhsImage("1").replace("class=\"onix-carousel-item\"", "class=\"onix-carousel-item\" aria-setsize=\"1\""))
        val json = extract(AssistantLinkPlatform.XIAOHONGSHU, html)
        assertEquals(1, json["expected"]!!.jsonPrimitive.int)
    }

    @Test fun ctripRecommendationsCannotEnterOriginalCarousel() = runBlocking {
        val html = ctrip(ctripImage("original"), """<span class="swiper-tip">1/1</span>""",
            """<aside>${ctripImage("recommendation")}</aside>""")
        val json = extract(AssistantLinkPlatform.CTRIP, html)
        assertEquals(1, json["images"]!!.jsonArray.size)
        assertTrue(json["images"]!!.jsonArray.single().jsonPrimitive.content.contains("original"))
    }

    @Test fun recommendationOnlyIsNotAnArticleImage() = runBlocking {
        val html = """<div class="paipai-detail"><div class="article-title">攻略</div>
            <div class="article-content-container">景点</div></div><aside>${ctripImage("recommendation")}</aside>"""
        val json = extract(AssistantLinkPlatform.CTRIP, html)
        assertTrue(json["images"]!!.jsonArray.isEmpty())
    }

    @Test fun twentyImagesPassAndKnownTwentyFirstIsRejectedBeforeLoading() = runBlocking {
        val input = AssistantLinkInput.parse("https://xhslink.cn/o/a")!!
        val twenty = extract(AssistantLinkPlatform.XIAOHONGSHU, xhs(
            (1..20).joinToString("") { xhsImage("$it") }, """<div class="carousel-indicator">1/20</div>"""))
        assertEquals(20, AssistantLinkDom.decode(input, twenty)!!.validated(input).imageUrls.size)
        val overflow = extract(AssistantLinkPlatform.XIAOHONGSHU,
            xhs(xhsImage("1"), """<div class="carousel-indicator">1/21</div>"""))
        assertTrue(assertThrows(AssistantFailure::class.java) {
            AssistantLinkDom.decode(input, overflow)
        }.userMessage.contains("20"))
    }

    @Test fun trustedLazySourceIsReadOnlyInsideOriginalCarousel() = runBlocking {
        val json = extract(AssistantLinkPlatform.XIAOHONGSHU,
            xhs(xhsImage("1") + xhsImage("2", true), """<div class="carousel-indicator">1/2</div>"""))
        assertEquals(2, AssistantLinkDom.decode(AssistantLinkInput.parse("https://xhslink.cn/o/a")!!, json)!!.imageUrls.size)
    }

    @Test fun conflictingCountersStayIncompleteAndDoNotChooseSmallerOne() = runBlocking {
        val json = extract(AssistantLinkPlatform.XIAOHONGSHU, xhs(xhsImage("1"),
            """<div class="carousel-indicator">1/1</div><div class="image-count">共3张</div>"""))
        assertEquals(-1, json["expected"]!!.jsonPrimitive.int)
        assertNull(AssistantLinkDom.decode(AssistantLinkInput.parse("https://xhslink.cn/o/a")!!, json))
    }

    @Test fun repeatedImageRenderingVariantsDoNotInflateOriginalCount() = runBlocking {
        val json = extract(AssistantLinkPlatform.XIAOHONGSHU, xhs(
            xhsImage("asset!h5_1080jpg") + xhsImage("asset!h5_720jpg"),
            """<div class="carousel-indicator">1/1</div>"""))
        assertEquals(1, json["images"]!!.jsonArray.size)
    }

    @Test fun unsafeVariantCannotBeHiddenBehindAValidDuplicate() = runBlocking {
        val json = extract(AssistantLinkPlatform.XIAOHONGSHU, xhs(
            xhsImage("asset!h5_1080jpg") + xhsImage("asset!h5_720jpg")
                .replace("sns-webpic-qc.xhscdn.com", "evil.invalid"),
            """<div class="carousel-indicator">1/1</div>"""))
        assertThrows(AssistantFailure::class.java) {
            val input = AssistantLinkInput.parse("https://xhslink.cn/o/a")!!
            AssistantLinkDom.decode(input, json)!!.validated(input)
        }
        Unit
    }

    @Test fun deletedPageWithEmptyArticlePlaceholderIsRestricted() = runBlocking {
        val json = extract(AssistantLinkPlatform.CTRIP,
            """<div class="article-content-container"></div><div>内容已删除</div>""")
        assertTrue(json["restricted"]!!.jsonPrimitive.boolean)
    }

    @Test fun explicitSinglePaginationDotCanProveOneImage() = runBlocking {
        val json = extract(AssistantLinkPlatform.CTRIP, ctrip(ctripImage("1"),
            """<div class="swiper-pagination-bullets"><span class="swiper-pagination-bullet"></span></div>"""))
        assertEquals(1, json["expected"]!!.jsonPrimitive.int)
    }

    @Test fun blankImageSourcesRemainPendingUntilBothPlatformsFillTheCarousel() = runBlocking {
        for (platform in AssistantLinkPlatform.entries) {
            val image = if (platform == AssistantLinkPlatform.CTRIP) ctripImage("asset") else xhsImage("asset")
            val input = AssistantLinkInput.parse(if (platform == AssistantLinkPlatform.CTRIP)
                "https://m.ctrip.com/webapp/you/community/detail?articleId=123" else "https://xhslink.cn/o/a")!!
            fun page(content: String) = if (platform == AssistantLinkPlatform.CTRIP)
                ctrip(content, """<span class="swiper-tip">1/1</span>""")
            else xhs(content, """<div class="carousel-indicator">1/1</div>""")
            for (attribute in listOf("", """src="" """, """src=" " """)) {
                val pending = extract(platform, page(image.replace(Regex("""src="[^"]*""""), attribute)))
                assertEquals("Platform $platform, attribute $attribute", 1, pending["pendingImages"]!!.jsonPrimitive.int)
                assertTrue(pending["images"]!!.jsonArray.isEmpty())
                assertNull(AssistantLinkDom.decode(input, pending))
                val loaded = extract(platform, page(image))
                assertEquals(1, AssistantLinkDom.decode(input, loaded)!!.validated(input).imageUrls.size)
                val lazy = extract(platform, page(image.replace("src=", """src=" " data-src=""")))
                assertEquals(1, AssistantLinkDom.decode(input, lazy)!!.validated(input).imageUrls.size)
            }
        }
    }

    @Test fun videoPosterNeverPassesEvenWithCounter(): Unit = runBlocking {
        val html = xhs(xhsImage("1"), """<div class="carousel-indicator">1/1</div>""",
            """<video src="https://example.invalid/video"></video>""")
        val json = extract(AssistantLinkPlatform.XIAOHONGSHU, html)
        assertThrows(AssistantFailure::class.java) {
            AssistantLinkDom.decode(AssistantLinkInput.parse("https://xhslink.cn/o/a")!!, json)
        }
    }

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
            <html><body><div class="paipai-detail"><div class="article-content-container">
            <h1 class="article-title">青州周末</h1>
            <div class="article-content-container article-content-collapsed">$body</div>
            <div class="article-content-container-background">$body</div><div>携程推荐：无关地点</div></div>
            <div class="swiper-wapper"><span class="swiper-tip">1/1</span>
            <div class="swiper-item-image"><img src="https://dimg04.c-ctrip.com/images/1.jpg"></div></div></div></body></html>
        """.trimIndent())
        assertEquals(body, result["body"]!!.jsonPrimitive.content)
        assertEquals(1, result["expected"]!!.jsonPrimitive.int)
        val input = AssistantLinkInput.parse("https://m.ctrip.com/webapp/you/community/detail?articleId=123")!!
        assertEquals(1, AssistantLinkDom.decode(input, result)!!.validated(input).imageUrls.size)
    }

    @Test fun xhsSingleImageWithExplicitCountIsReadButUnloadedCarouselIsNotComplete() = runBlocking {
        val html = """<html><body><div class="narmal-note-container">
            <div class="author-desc-content">青州周末<br>青州古城</div>
            <div class="image-gallery-container"><div class="carousel-indicator">1/1</div>
            <div class="note-image-box"><img src="https://sns-webpic-qc.xhscdn.com/notes_pre_post/1"></div></div>
            <img src="https://sns-avatar.xhscdn.com/avatar/1"></div>
            <div>猜你喜欢<img src="https://sns-webpic-qc.xhscdn.com/notes_pre_post/unrelated"></div></body></html>"""
        val input = AssistantLinkInput.parse("https://xhslink.cn/o/a")!!
        val single = extract(AssistantLinkPlatform.XIAOHONGSHU, html)
        assertEquals(1, AssistantLinkDom.decode(input, single)!!.validated(input).imageUrls.size)
        val incomplete = extract(AssistantLinkPlatform.XIAOHONGSHU, html.replace("1/1", "1/3"))
        assertNull(AssistantLinkDom.decode(input, incomplete))
    }
}
