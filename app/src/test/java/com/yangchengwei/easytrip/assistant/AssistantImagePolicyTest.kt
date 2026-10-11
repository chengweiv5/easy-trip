package com.yangchengwei.easytrip.assistant

import org.junit.Assert.*
import org.junit.Test

class AssistantImagePolicyTest {
    @Test fun seventhImageIsRejectedWithoutDroppingSixAcceptedImages() {
        assertNull(AssistantImagePolicy.rejection(5, 5_000, 1_000))
        assertEquals("每批最多 6 张图片", AssistantImagePolicy.rejection(6, 6_000, 1_000))
    }
    @Test fun exactByteLimitsAreInclusiveAndOverflowNeverWraps() {
        val single = AssistantImagePolicy.MAX_SINGLE_BYTES
        assertNull(AssistantImagePolicy.rejection(2, single * 2, single))
        assertNotNull(AssistantImagePolicy.rejection(0, 0, single + 1))
        assertNotNull(AssistantImagePolicy.rejection(3, single * 3, 1))
        assertNotNull(AssistantImagePolicy.rejection(0, Long.MAX_VALUE, 1))
        assertNotNull(AssistantImagePolicy.rejection(0, 0, 0))
        assertNotNull(AssistantImagePolicy.rejection(0, 0, -1))
    }
    @Test fun untrustedMimeAndAnimatedContainersAreRejected() {
        assertThrows(AssistantFailure::class.java) { AssistantImagePolicy.mime("<html>not a png</html>".toByteArray()) }
        val png = byteArrayOf(137.toByte(),80,78,71,13,10,26,10) +
            byteArrayOf(0,0,0,0) + "acTL".toByteArray() + byteArrayOf(0,0,0,0)
        assertThrows(AssistantFailure::class.java) { AssistantImagePolicy.mime(png) }
        val webp = "RIFF".toByteArray() + byteArrayOf(20,0,0,0) + "WEBPVP8X".toByteArray() +
            byteArrayOf(1,0,0,0,2,0)
        assertThrows(AssistantFailure::class.java) { AssistantImagePolicy.mime(webp) }
    }
    @Test fun decodeBudgetPreservesAspectAndCapsPixels() {
        assertEquals(4096 to 2048, AssistantImagePolicy.targetSize(8192, 4096))
        val square = AssistantImagePolicy.targetSize(10000, 10000)
        assertTrue(square.first.toLong() * square.second <= 8_388_608)
        assertThrows(AssistantFailure::class.java) { AssistantImagePolicy.targetSize(Int.MAX_VALUE, Int.MAX_VALUE) }
    }
}
