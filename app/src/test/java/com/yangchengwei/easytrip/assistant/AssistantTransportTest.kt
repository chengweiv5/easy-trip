package com.yangchengwei.easytrip.assistant

import org.junit.Assert.*
import org.junit.Test

class AssistantTransportTest {
    @Test fun genericBadRequestIsNotProofOfUnsupportedImages() {
        val t = AssistantTransport()
        assertEquals(AssistantFailureCode.PROTOCOL, t.failure(400, """{"error":{"message":"bad tool schema"}}""").code)
        val rejected = t.failure(400, """{"error":{"code":"unsupported_image_input","message":"private response"}}""")
        assertEquals(AssistantFailureCode.UNSUPPORTED_IMAGE, rejected.code)
        assertEquals(SendEvidence.RESPONSE_RECEIVED, rejected.sendEvidence)
        assertFalse(rejected.userMessage.contains("private response"))
        assertEquals(AssistantFailureCode.AUTH, t.failure(401, "secret").code)
        assertEquals(AssistantFailureCode.LIMIT, t.failure(429, "secret").code)
    }
}
