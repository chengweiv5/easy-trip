package com.yangchengwei.easytrip.assistant

import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class AssistantLinkReadinessTest {
    private val input = AssistantLinkInput.parse("https://xhslink.cn/o/a")!!
    private fun snapshot(expected: Int, images: Int, extra: String = "") = Json.parseToJsonElement("""
        {"url":"https://www.xiaohongshu.com/discovery/item/abc","title":"攻略","body":"景点","noteLocated":true,
         "expected":$expected,"images":[${(1..images).joinToString { "\"https://sns-webpic-qc.xhscdn.com/notes_pre_post/$it\"" }}]
         $extra}
    """).jsonObject

    @Test fun textIsReadyWithoutACompleteCarousel() {
        val doc = requireNotNull(AssistantLinkDom.decode(input, snapshot(-1, 1))).validated(input)
        assertEquals("景点", doc.body)
        assertEquals(1, doc.imageUrls.size)
    }

    @Test fun knownOverflowStillReturnsTextAndAtMostTwentyImages() {
        val doc = requireNotNull(AssistantLinkDom.decode(input, snapshot(21, 21))).validated(input)
        assertEquals(20, doc.imageUrls.size)
        assertEquals(1, doc.skippedImageCount)
    }

    @Test fun videoOrRestrictedContentCannotBecomeReadyFromPosterAndCounter() {
        for (flag in listOf("video", "restricted")) {
            assertThrows(AssistantFailure::class.java) {
                AssistantLinkDom.decode(input, snapshot(1, 1, ",\"$flag\":true"))
            }
        }
    }

    @Test fun unknownOrMissingCountDoesNotBlockText() {
        assertNotNull(AssistantLinkDom.decode(input, snapshot(-1, 1)))
        assertNotNull(AssistantLinkDom.decode(input, snapshot(3, 1)))
    }

    @Test fun imageVariantsAreCountedByOriginalAssetNotSignedRenderingUrl() {
        val urls = listOf(
            "https://sns-webpic-qc.xhscdn.com/notes_pre_post/asset!h5_1080jpg",
            "https://sns-webpic-qc.xhscdn.com/notes_pre_post/asset!h5_720jpg",
        )
        val doc = AssistantLinkDocument(input.platform, "https://www.xiaohongshu.com/discovery/item/abc",
            "攻略", "景点", urls, 1).validated(input)
        assertEquals(1, doc.imageUrls.size)
        assertEquals(0, doc.skippedImageCount)
    }

    @Test fun noOriginalNoteEvidenceCannotBecomeReady() {
        assertNull(AssistantLinkDom.decode(input, JsonObject(snapshot(1, 1) - "noteLocated")))
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
