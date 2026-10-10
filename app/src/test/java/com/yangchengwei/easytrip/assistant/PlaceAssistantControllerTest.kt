package com.yangchengwei.easytrip.assistant

import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.amap.PlaceSearchDataSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PlaceAssistantControllerTest {
    private class Importer : PlaceImport {
        var calls = 0
        override suspend fun receipt(operationId: String): ImportReceipt? = null
        override suspend fun commit(confirmation: ImportConfirmation): ImportReceipt {
            calls++
            return ImportReceipt(confirmation.operationId, confirmation.tripId, confirmation.digest,
                confirmation.items.map { ImportItemResult(it.itemId, it.poi.poiId, "saved-${it.itemId}", true) })
        }
    }
    private fun poi(name: String, id: String = name) = PlaceCandidate(id, name, "真实地址", GeoPoint(30.2, 120.1), null, "杭州市")
    @Test fun matchedItemsCanBeSelectedAndOnlyExplicitConfirmationWrites() = runTest {
        val importer = Importer()
        val names = listOf("灵隐寺", "河坊街", "失败", "雷峰塔")
        val controller = PlaceAssistantController("trip", this,
            PlaceIntentParser { _, _ -> names.map { PlaceIntent(it, "杭州", it) } },
            object : PlaceSearchDataSource {
                override suspend fun search(keyword: String, city: String?) = when (keyword) {
                    "失败" -> throw IllegalStateException("untrusted secret")
                    "雷峰塔" -> listOf(poi("雷峰塔景区"), poi("雷峰塔塔体"))
                    else -> listOf(poi(keyword))
                }
            }, importer)
        controller.environment(true, true, true)
        controller.editInput(names.joinToString("、")); controller.allowSend()
        controller.submit(); advanceUntilIdle()
        assertEquals(listOf(IntakeStatus.READY, IntakeStatus.READY, IntakeStatus.FAILED, IntakeStatus.AMBIGUOUS),
            controller.state.value.items.map { it.status })
        controller.selectAll()
        controller.review()
        assertEquals(0, importer.calls)
        assertEquals(2, controller.state.value.confirmation!!.items.size)
        controller.confirm(); advanceUntilIdle()
        assertEquals(1, importer.calls)
        assertEquals(2, controller.state.value.receipt!!.addedCount)
        controller.confirm(); advanceUntilIdle()
        assertEquals(1, importer.calls)
    }

    @Test fun cancellationKeepsCompletedCandidatesAndSelection() = runTest {
        val waiting = CompletableDeferred<List<PlaceCandidate>>()
        val controller = PlaceAssistantController("trip", this,
            PlaceIntentParser { _, _ -> listOf(PlaceIntent("灵隐寺", "杭州", "灵隐寺"), PlaceIntent("慢", "杭州", "慢")) },
            object : PlaceSearchDataSource {
                override suspend fun search(keyword: String, city: String?) = if (keyword == "慢") waiting.await() else listOf(poi(keyword))
            }, Importer())
        controller.environment(true, true, true); controller.editInput("灵隐寺、慢"); controller.allowSend(); controller.submit()
        runCurrent()
        controller.selectAll(); controller.cancel()
        waiting.complete(listOf(poi("慢"))); advanceUntilIdle()
        assertEquals(IntakeStatus.READY, controller.state.value.items[0].status)
        assertEquals(IntakeStatus.CANCELLED, controller.state.value.items[1].status)
        assertEquals(1, controller.state.value.selected.size)
    }
    @Test fun duplicatePoiAndChangedConfirmationNeverWriteImplicitly() = runTest {
        val importer = Importer()
        val c = PlaceAssistantController("trip", this,
            PlaceIntentParser { _, _ -> listOf("灵隐寺", "灵隐寺").map { PlaceIntent(it, "杭州", it) } },
            object : PlaceSearchDataSource { override suspend fun search(keyword: String, city: String?) = listOf(poi(keyword)) }, importer)
        c.environment(true, true, true); c.allowSend(); c.editInput("灵隐寺、灵隐寺"); c.submit(); advanceUntilIdle()
        assertEquals(listOf(IntakeStatus.READY, IntakeStatus.DUPLICATE), c.state.value.items.map { it.status })
        assertTrue(c.state.value.selected.isEmpty()); assertEquals(0, importer.calls)
        c.selectAll(); c.review()
        c.category(c.state.value.items.first().id, com.yangchengwei.easytrip.place.domain.PlaceCategory.FOOD)
        c.confirm(); advanceUntilIdle(); assertEquals(0, importer.calls)
        c.review(); c.environment(true, true, false); c.confirm(); advanceUntilIdle(); assertEquals(0, importer.calls)
    }

    @Test fun retryOnlyFailedAndForeignCityNeedsExplicitChoice() = runTest {
        val calls = mutableListOf<String>()
        val c = PlaceAssistantController("trip", this,
            PlaceIntentParser { _, _ -> listOf("灵隐寺", "故宫", "失败").map { PlaceIntent(it, "杭州", it) } },
            object : PlaceSearchDataSource { override suspend fun search(keyword: String, city: String?): List<PlaceCandidate> {
                calls += keyword
                if (keyword == "失败") throw IllegalStateException()
                return listOf(poi(keyword).let { if (keyword == "故宫") it.copy(cityName = "北京") else it })
            } }, Importer())
        c.environment(true, true, true); c.allowSend(); c.editInput("灵隐寺、故宫、失败"); c.submit(); advanceUntilIdle()
        assertEquals(IntakeStatus.NEED_CITY, c.state.value.items[1].status)
        c.retry(); advanceUntilIdle()
        assertEquals(1, calls.count { it == "灵隐寺" }); assertEquals(1, calls.count { it == "故宫" }); assertEquals(2, calls.count { it == "失败" })
        c.choosePoi(c.state.value.items[1].id, "故宫")
        assertEquals(IntakeStatus.READY, c.state.value.items[1].status)
        assertTrue(c.state.value.selected.isEmpty())
    }

    @Test fun lateDuplicateKeepsSelectedRepresentativeAndDeletedSavedPlaceBecomesEligible() = runTest {
        val slow = CompletableDeferred<List<PlaceCandidate>>()
        val c = PlaceAssistantController("trip", this,
            PlaceIntentParser { _, _ -> listOf("慢", "快").map { PlaceIntent(it, "杭州", it) } },
            object : PlaceSearchDataSource { override suspend fun search(keyword: String, city: String?) =
                if (keyword == "慢") slow.await() else listOf(poi("快", "same")) }, Importer())
        c.environment(true, true, true); c.allowSend(); c.editInput("慢、快"); c.submit(); runCurrent()
        val fast = c.state.value.items[1].id
        c.toggle(fast); c.review(); val confirmation = c.state.value.confirmation
        slow.complete(listOf(poi("慢", "same"))); advanceUntilIdle()
        assertEquals(setOf(fast), c.state.value.selected)
        assertEquals(confirmation, c.state.value.confirmation)
        assertEquals(IntakeStatus.DUPLICATE, c.state.value.items[0].status)
        c.updateSaved(setOf("same")); assertTrue(c.state.value.eligible.isEmpty())
        c.updateSaved(emptySet()); assertEquals(1, c.state.value.eligible.size)
    }

    @Test fun conflictingInputCityWaitsForExplicitConfirmation() = runTest {
        var queries = 0
        val c = PlaceAssistantController("trip", this,
            PlaceIntentParser { _, _ -> listOf(PlaceIntent("故宫", "北京", "故宫")) },
            object : PlaceSearchDataSource { override suspend fun search(keyword: String, city: String?): List<PlaceCandidate> {
                queries++; return listOf(poi("故宫").copy(cityName = "北京"))
            } }, Importer())
        c.environment(true, true, true); c.allowSend(); c.editCity("杭州"); c.editInput("北京的故宫"); c.submit(); advanceUntilIdle()
        assertEquals(0, queries); assertEquals(IntakeStatus.NEED_CITY, c.state.value.items.single().status)
        c.confirmCity(c.state.value.items.single().id); advanceUntilIdle()
        assertEquals(1, queries); assertEquals(IntakeStatus.READY, c.state.value.items.single().status)
    }

}
