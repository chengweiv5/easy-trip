package com.yangchengwei.easytrip.assistant

import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class OptionalImageInputTest {
    private val image = AssistantImage("cfad26df-5211-4940-8a60-f20d6ec57a47", 1, "image/jpeg", 3, 1, 1)
    @Test fun successfulTextPrecedesOptionalPreparationAndTimeoutCannotEraseIt() = runTest {
        val order = mutableListOf<String>()
        val stages = mutableListOf<SourceProgress>()
        val p = AssistantInputProcessor(PlaceIntentParser { _, _ ->
            order += "text"; listOf(PlaceIntent("公园", "北城", "公园"))
        }, ImageIntentParser { _, _, _, _, _ -> delay(190_000); emptyList() }, GuideTestImages())
        val result = p.parse(AssistantInputRequest("session", "公园", "北城", listOf(image),
            imagePlan = OptionalImagePlan.local(listOf(image)), prepareOptionalImages = {
                order += "prepare"; OptionalImagePlan.local(listOf(image))
            }), onProgress = stages::add)
        assertEquals(listOf("text", "prepare"), order)
        assertNull(result.sources.single { it.sourceId == "text" }.failure)
        assertNotNull(result.sources.single { it.sourceId == image.id }.failure)
        assertEquals(SourceStage.SKIPPED, stages.last().stage)
        assertEquals(SendEvidence.POSSIBLY_SENT, stages.last().sendEvidence)
    }
    @Test fun rejectedImageOverflowDoesNotRejectSuccessfulText() = runTest {
        val p = AssistantInputProcessor(PlaceIntentParser { _, _ -> listOf(PlaceIntent("公园", "北城", "公园")) },
            ImageIntentParser { _, _, _, _, _ -> throw AssistantFailure("too many", AssistantFailureCode.OVERFLOW) }, GuideTestImages())
        val result = p.parse(AssistantInputRequest("session", "公园", "北城", listOf(image))) {}
        assertFalse(result.overflow)
        assertEquals("公园", result.sources.first().intents.single().query)
        assertEquals(SourceStage.SKIPPED, result.sources.last().completionStage())
    }
    @Test fun localSkippedImagesAreNotPreparedOrSentAgainWithoutExplicitRetry() = runTest {
        var calls = 0
        val p = AssistantInputProcessor(PlaceIntentParser { _, _ -> emptyList() },
            ImageIntentParser { _, _, _, _, _ -> calls++; emptyList() }, GuideTestImages())
        val plan = OptionalImagePlan.local(listOf(image)).let { it.updated(it.assets.single().skipped(OptionalImageSkip.MODEL)) }
        val input = AssistantInputRequest("session", "", "北城", listOf(image), imagePlan = plan)
        val first = p.parse(input) {}
        assertEquals(0, calls)
        assertEquals(SourceStage.SKIPPED, first.sources.single().completionStage())
        p.parse(input.copy(retrySourceIds = setOf(image.id))) {}
        assertEquals(1, calls)
    }
    @Test fun activeCancellationIsNotAnOptionalFailure() = runTest {
        val entered = CompletableDeferred<Unit>()
        val published = mutableListOf<ParsedSource>()
        val p = AssistantInputProcessor(PlaceIntentParser { _, _ -> emptyList() },
            ImageIntentParser { _, _, _, _, _ -> entered.complete(Unit); awaitCancellation() }, GuideTestImages())
        val job = launch { p.parse(AssistantInputRequest("session", "", "北城", listOf(image)), published::add) {} }
        entered.await(); job.cancelAndJoin()
        assertTrue(published.isEmpty())
    }
    @Test fun authInTextPreventsImageDownloadAndRemainsARealError() = runTest {
        val p = AssistantInputProcessor(PlaceIntentParser { _, _ -> throw AssistantFailure("auth", AssistantFailureCode.AUTH) },
            ImageIntentParser { _, _, _, _, _ -> error("no image request") }, GuideTestImages())
        val result = p.parse(AssistantInputRequest("session", "公园", "北城", listOf(image),
            prepareOptionalImages = { error("no download") })) {}
        assertEquals(2, result.sources.size)
        assertTrue(result.sources.all { it.failure?.code == AssistantFailureCode.AUTH })
    }
}
