package com.yangchengwei.easytrip.assistant

import com.yangchengwei.easytrip.assistant.cityfirst.GuideResultCodec
import com.yangchengwei.easytrip.assistant.cityfirst.GuideSnapshotSaveGuard
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class GuideBatchRecoveryTest {
    @Test fun displayErrorCannotRetriggerFailedSaveButUserEditsAndNewHandoffCan() = runTest {
        val guard = GuideSnapshotSaveGuard()
        val initial = PlaceAssistantState(input = "原文", city = "北城")
        var attempts = 0
        suspend fun save(id: String, state: PlaceAssistantState) =
            runCatching { guard.attempt(id, state) { attempts++; error("disk full") } }
        assertTrue(save("guide-a", initial).isFailure)
        assertTrue(save("guide-a", initial.copy(error = "保存失败")).isSuccess)
        assertEquals(1, attempts)
        assertTrue(save("guide-a", initial.copy(city = "南城")).isFailure)
        assertTrue(save("guide-b", initial.copy(city = "南城")).isFailure)
        assertEquals(3, attempts)
    }

    @Test fun networkBudgetDoesNotIncreaseRecoveryCapacityOrMutatePriorState() {
        val previous = PlaceAssistantState(input = "原文", city = "北城")
        val previousJson = GuideResultCodec.encode(previous)
        val images = guideImages(4)
        val parsed = images.map { image -> ParsedSource(image.id, (1..100).map { index ->
            val name = "美术馆${image.ordinal}_$index"
            val span = name + "字".repeat(500 - name.length)
            PlaceIntent(name, "北城", span, sources = listOf(IntakeSource.Image(image.id, image.ordinal, span,
                ImageTargetEvidence(ImageTargetBasis.AUTHOR_MARK, "图".repeat(300)))))
        }) }
        val oversized = previous.copy(images = images, parsedMaterials = parsed)
        assertTrue(runCatching { GuideResultCodec.encode(oversized) }.exceptionOrNull() is IllegalArgumentException)
        assertEquals(previousJson, GuideResultCodec.encode(previous))
        assertEquals(400, oversized.parsedMaterials.sumOf { it.intents.size })
    }
}
