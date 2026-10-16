package com.yangchengwei.easytrip.assistant

import com.yangchengwei.easytrip.assistant.cityfirst.ModelGuideDestinationInterpreter
import kotlinx.coroutines.test.runTest
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test

class GuideOptionalDestinationTest {
    @Test fun cityFromTextNeverPreparesImages() = runTest {
        val response = """{"choices":[{"finish_reason":"tool_calls","message":{"tool_calls":[{"type":"function","function":{"name":"identify_trip_destination","arguments":"{\"cities\":[{\"name\":\"青州\",\"sourceId\":\"text\",\"sourceSpan\":\"青州\"}],\"suggestedName\":\"青州之旅\"}"}}]}}]}"""
        val http = OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(200).message("OK").body(response.toResponseBody()).build()
        }.build()
        var prepared = 0
        val interpreter = ModelGuideDestinationInterpreter(
            { ProviderConfig("https://example.invalid", "fixture", "not-real") }, AssistantTransport(http))
        val result = interpreter.interpret("青州", images = { prepared++; emptyList() })
        assertEquals("青州", result.cities.single().name)
        assertEquals(0, prepared)
    }
}
