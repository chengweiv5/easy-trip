package com.yangchengwei.easytrip.assistant

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class GuideBatchInputRoutingTest {
    @Test fun onlyGuideWithNormalizerDelegatesAndRetainsRetryAndCityContext() = runTest {
        var textCalls = 0
        val text = PlaceIntentParser { _, _ -> textCalls++; emptyList() }
        val image = ImageIntentParser { _, _, _, _, _ -> error("unexpected image") }
        val requests = mutableListOf<AssistantInputRequest>()
        val guide = object : GuideMaterialParser {
            override suspend fun parse(request: AssistantInputRequest, onSource: (ParsedSource) -> Unit,
                onProgress: (SourceProgress) -> Unit): ParsedInputBatch {
                requests += request
                return ParsedInputBatch(emptyList(), false)
            }
        }
        val processor = AssistantInputProcessor(text, image, GuideTestImages(),
            normalizer = PlaceIntentNormalizer { values, _, _ -> values }, guideParser = guide)
        val request = guideRequest(0).copy(retrySourceIds = emptySet(),
            cityScope = AssistantCityScope(listOf("北城", "南城")))
        processor.parse(request) {}
        assertEquals(request, requests.single())
        processor.parse(request.copy(travelMaterials = false, retrySourceIds = null, cityScope = null)) {}
        AssistantInputProcessor(text, image, GuideTestImages(), guideParser = guide)
            .parse(request.copy(retrySourceIds = null, cityScope = null)) {}
        assertEquals(2, textCalls)
        assertEquals(1, requests.size)
    }
}
