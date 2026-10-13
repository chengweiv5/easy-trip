package com.yangchengwei.easytrip.assistant

import org.junit.Assert.*
import org.junit.Test

class AssistantLinkInputTest {
    @Test fun fullShareTextFindsAdjacentUrlAndPreservesParameters() {
        val url = "https://m.ctrip.com/webapp/you/community/detail?articleId=353997561&x=1"
        val result = AssistantLinkInput.parse("被低估的青州🌿${url}。")!!
        assertEquals(url, result.url)
        assertEquals(AssistantLinkPlatform.CTRIP, result.platform)
        assertEquals("被低估的青州🌿", result.context)
    }
    @Test fun shortLinkAndDuplicateIdenticalLinkAreOneSource() {
        val url = "https://xhslink.cn/o/onUvwErqYX"
        assertEquals(url, AssistantLinkInput.parse("青州 $url 复制内容后去【小红书】 $url")!!.url)
        assertNull(AssistantLinkInput.parse("青州博物馆 云门山"))
    }
    @Test fun unsupportedOrUnsafeInputCannotFallThroughToModel() {
        for (text in listOf(
            "https://m.dianping.com/feeddetail/1",
            "http://xhslink.cn/o/a",
            "https://xhslink.cn.evil.test/o/a",
            "https://user:password@xhslink.cn/o/a",
            "https://m.ctrip.com/webapp/hotel/1",
            "https://127.0.0.1/ https://xhslink.cn/o/a",
            "https://xhslink.cn/o/a https://xhslink.cn/o/b",
            "https://xhslink.cn:8443/o/a",
        )) assertThrows(AssistantFailure::class.java) { AssistantLinkInput.parse(text) }
    }
    @Test fun twentyImagesAreInclusiveAndMissingImagesNeverBecomeComplete() {
        val input = AssistantLinkInput.parse("https://xhslink.cn/o/a")!!
        fun doc(count: Int, expected: Int = count) = AssistantLinkDocument(
            input.platform, "https://www.xiaohongshu.com/discovery/item/abc123", "青州", "青州博物馆", (1..count).map {
                "https://sns-webpic-qc.xhscdn.com/notes_pre_post/$it"
            }, expected,
        )
        assertEquals(20, doc(20).validated(input).imageUrls.size)
        assertThrows(AssistantFailure::class.java) { doc(21).validated(input) }
        assertThrows(AssistantFailure::class.java) { doc(19, 20).validated(input) }
        assertThrows(AssistantFailure::class.java) {
            doc(1).copy(imageUrls = listOf("https://127.0.0.1/a")).validated(input)
        }
    }
    @Test fun differentArticleCannotMasqueradeAsOriginalNote() {
        val input = AssistantLinkInput.parse("https://m.ctrip.com/webapp/you/community/detail?articleId=123")!!
        val doc = AssistantLinkDocument(input.platform, input.url, "青州", "青州古城", emptyList(), 0)
        assertEquals(doc, doc.validated(input))
        assertThrows(AssistantFailure::class.java) { doc.copy(url = input.url.replace("123", "124")).validated(input) }
        assertThrows(AssistantFailure::class.java) { doc.copy(url = "https://m.ctrip.com/html5/").validated(input) }
    }
    @Test fun platformSuffixDoesNotEatUserInstructionAfterIt() {
        for (separator in listOf(" ", "\n")) {
            val result = AssistantLinkInput.parse("青州攻略 https://xhslink.cn/o/a 复制内容后去【小红书】，直接看笔记详情。${separator}只提取适合孩子的地点")!!
            assertTrue(result.context.contains("青州攻略"))
            assertTrue(result.context.contains("只提取适合孩子的地点"))
            assertFalse(result.context.contains("复制内容"))
        }
    }
}
