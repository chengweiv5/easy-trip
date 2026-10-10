package com.yangchengwei.easytrip.assistant

import org.junit.Assert.*
import org.junit.Test

class DeepSeekPlaceParserTest {
    @Test fun providerRejectsInsecureOrCredentialBearingUrls() {
        listOf("http://example.com", "https://user:pass@example.com", "https://example.com?key=a",
            "https://example.com/#secret", "https://example.com/chat/completions").forEach {
            assertThrows(IllegalArgumentException::class.java) {
                ProviderConfig(it, "deepseek-flash", "secret").validated()
            }
        }
        assertEquals("https://api.deepseek.com/chat/completions",
            ProviderConfig("https://api.deepseek.com/", "deepseek-flash", "secret").validated().chatUrl)
        assertFalse(ProviderConfig("https://api.deepseek.com", "flash", "secret").toString().contains("secret"))
    }

    @Test fun fullBatchIsDecodedWithoutInventingItems() {
        val response = response("""{"items":[{"query":"灵隐寺","city":"杭州","sourceSpan":"灵隐寺"},{"query":"河坊街","city":"杭州","sourceSpan":"河坊街"}],"overflow":false}""")
        assertEquals(listOf(PlaceIntent("灵隐寺", "杭州", "灵隐寺"), PlaceIntent("河坊街", "杭州", "河坊街")),
            BatchToolProtocol.decode(response, "杭州的灵隐寺和河坊街"))
    }

    @Test fun untrustedIncompleteAndWriteToolsAreRejected() {
        assertThrows(AssistantFailure::class.java) { BatchToolProtocol.decode(response("{}", "save_places"), "雷峰塔") }
        assertThrows(AssistantFailure::class.java) { BatchToolProtocol.decode(response("{}", finish = "length"), "雷峰塔") }
        assertThrows(AssistantFailure::class.java) {
            BatchToolProtocol.decode(response("""{"items":[{"query":"故宫","city":"北京","sourceSpan":"故宫"}],"overflow":false}"""), "杭州灵隐寺")
        }
        assertThrows(AssistantFailure::class.java) {
            BatchToolProtocol.decode(response("""{"items":[],"overflow":true}"""), "很多地点")
        }
    }

    @Test fun groundedSpanCannotAuthorizeAnInventedPlaceName() {
        assertThrows(AssistantFailure::class.java) {
            BatchToolProtocol.decode(response("""{"items":[{"query":"故宫","city":"北京","sourceSpan":"灵隐寺"}],"overflow":false}"""), "杭州灵隐寺")
        }
    }

    private fun response(arguments: String, tool: String = "search_place_batch", finish: String = "tool_calls") =
        """{"choices":[{"finish_reason":"$finish","message":{"tool_calls":[{"id":"one","type":"function","function":{"name":"$tool","arguments":${quote(arguments)}}}]}}]}"""
    private fun quote(value: String) = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}
