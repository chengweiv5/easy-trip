package com.yangchengwei.easytrip.assistant

import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class AssistantLinkReadinessTest {
    private val input = AssistantLinkInput.parse("https://xhslink.cn/o/a")!!
    private fun snapshot(expected: Int, images: Int, extra: String = "") = Json.parseToJsonElement("""
        {"url":"https://www.xiaohongshu.com/discovery/item/abc","title":"攻略","body":"景点",
         "expected":$expected,"images":[${(1..images).joinToString { "\"https://sns-webpic-qc.xhscdn.com/notes_pre_post/$it\"" }}]
         $extra}
    """).jsonObject

    @Test fun knownOverflowIsRejectedBeforeAllImagesHaveLoaded() {
        val error = assertThrows(AssistantFailure::class.java) { AssistantLinkDom.decode(input, snapshot(21, 1)) }
        assertTrue(error.userMessage.contains("20"))
    }

    @Test fun videoOrRestrictedContentCannotBecomeReadyFromPosterAndCounter() {
        for (flag in listOf("video", "restricted")) {
            assertThrows(AssistantFailure::class.java) {
                AssistantLinkDom.decode(input, snapshot(1, 1, ",\"$flag\":true"))
            }
        }
    }

    @Test fun unknownOrMissingCountNeverReturnsPartialMaterial() {
        assertNull(AssistantLinkDom.decode(input, snapshot(-1, 1)))
        assertNull(AssistantLinkDom.decode(input, snapshot(3, 1)))
    }

    @Test fun imageVariantsAreCountedByOriginalAssetNotSignedRenderingUrl() {
        val urls = listOf(
            "https://sns-webpic-qc.xhscdn.com/notes_pre_post/asset!h5_1080jpg",
            "https://sns-webpic-qc.xhscdn.com/notes_pre_post/asset!h5_720jpg",
        )
        val doc = AssistantLinkDocument(input.platform, "https://www.xiaohongshu.com/discovery/item/abc",
            "攻略", "景点", urls, 1).validated(input)
        assertEquals(1, doc.imageUrls.size)
    }

    @Test fun incompleteMessagesDistinguishUncertainCountsMissingImagesAndNetwork() {
        for ((reason, phrase) in listOf("count" to "总数", "count_conflict" to "总数",
            "images" to "未读取完整", "body" to "正文", "structure" to "页面结构")) {
            val error = AssistantLinkDom.incompleteFailure(buildJsonObject { put("reason", reason) })
            assertTrue(error.userMessage.contains(phrase))
            assertTrue(error.userMessage.contains("原输入已保留"))
            assertFalse(error.userMessage.contains("超时"))
        }
        assertTrue(AssistantLinkDom.incompleteFailure(null).userMessage.contains("超时"))
    }
}
