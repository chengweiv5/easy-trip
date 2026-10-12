package com.yangchengwei.easytrip.assistant

import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.amap.PlaceSearchDataSource
import com.yangchengwei.easytrip.place.domain.PlaceCategory
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AssistantDirectSaveTest {
    private fun TestScope.controller(importer: PlaceImport, source: PlaceSearchDataSource? = null) =
        PlaceAssistantController("trip", this,
            PlaceIntentParser { _, _ -> listOf("灵隐寺", "河坊街").map { PlaceIntent(it, "杭州", it) } },
            source ?: object : PlaceSearchDataSource {
                override suspend fun search(keyword: String, city: String?) =
                    listOf(PlaceCandidate(keyword.removePrefix("杭州"), keyword.removePrefix("杭州"), "地址", GeoPoint(30.2, 120.1), null, "杭州市"))
            }, importer).also {
            it.environment(true, true, true)
            it.editInput("灵隐寺、河坊街")
            it.submit()
        }

    private fun ImportConfirmation.success() = ImportReceipt(operationId, tripId, digest,
        items.map { ImportItemResult(it.itemId, it.poi.poiId, "saved-${it.itemId}", true) })

    @Test fun selectionDoesNotWriteAndOneSaveClickWritesOnlySelectedPlaces() = runTest {
        val requests = mutableListOf<ImportConfirmation>()
        val importer = object : PlaceImport {
            override suspend fun receipt(operationId: String): ImportReceipt? = null
            override suspend fun commit(confirmation: ImportConfirmation): ImportReceipt {
                requests += confirmation
                return ImportReceipt(confirmation.operationId, confirmation.tripId, confirmation.digest,
                    confirmation.items.map { ImportItemResult(it.itemId, it.poi.poiId, "saved-${it.itemId}", true) })
            }
        }
        val controller = PlaceAssistantController("trip", this,
            PlaceIntentParser { _, _ -> listOf("灵隐寺", "河坊街").map { PlaceIntent(it, "杭州", it) } },
            object : PlaceSearchDataSource {
                override suspend fun search(keyword: String, city: String?) =
                    listOf(PlaceCandidate(keyword.removePrefix("杭州"), keyword.removePrefix("杭州"), "地址", GeoPoint(30.2, 120.1), null, "杭州市"))
            }, importer)
        controller.environment(true, true, true)
        controller.editInput("灵隐寺、河坊街")
        controller.submit()
        advanceUntilIdle()
        controller.toggle(controller.state.value.items.first().id)
        assertTrue(requests.isEmpty())

        controller.saveSelected()
        assertTrue(controller.state.value.saving)
        controller.saveSelected()
        advanceUntilIdle()

        assertEquals(1, requests.size)
        assertEquals(listOf("灵隐寺"), requests.single().items.map { it.poi.name })
        assertTrue(controller.state.value.items.isEmpty())
        assertEquals("", controller.state.value.input)
        assertFalse(controller.state.value.hasDraft)
        assertTrue(controller.state.value.selected.isEmpty())
        assertEquals(1, controller.state.value.receipt?.addedCount)
        controller.editInput("新一轮地点")
        assertEquals("新一轮地点", controller.state.value.input)
    }

    @Test fun failedSaveKeepsSelectionAndRetriesTheSameOperation() = runTest {
        val requests = mutableListOf<ImportConfirmation>()
        val c = controller(object : PlaceImport {
            override suspend fun receipt(operationId: String): ImportReceipt? = null
            override suspend fun commit(confirmation: ImportConfirmation): ImportReceipt {
                requests += confirmation
                if (requests.size == 1) error("response lost")
                return confirmation.success()
            }
        })
        advanceUntilIdle()
        c.selectAll()
        val selection = c.state.value.selected
        c.saveSelected(); advanceUntilIdle()
        assertEquals(selection, c.state.value.selected)
        assertFalse(c.state.value.saving)
        assertNotNull(c.state.value.error)
        c.saveSelected(); advanceUntilIdle()
        assertEquals(2, requests.size)
        assertEquals(requests[0].operationId, requests[1].operationId)
        assertNull(c.state.value.error)
        assertEquals(2, c.state.value.receipt?.addedCount)
    }

    @Test fun successClearsUnselectedFailedAndLateCandidatesButKeepsCityAndSavedPlaces() = runTest {
        val late = CompletableDeferred<Unit>()
        val started = CompletableDeferred<Unit>()
        val requests = mutableListOf<ImportConfirmation>()
        val c = PlaceAssistantController("trip", this,
            PlaceIntentParser { text, _ ->
                (if (text == "下一轮") listOf("甲", "新地点") else listOf("甲", "未选", "失败", "慢"))
                    .map { PlaceIntent(it, "", it) }
            },
            object : PlaceSearchDataSource {
                override suspend fun search(keyword: String, city: String?): List<PlaceCandidate> {
                    val name = keyword.removePrefix("杭州")
                    if (name == "失败") error("search failed")
                    if (name == "慢") withContext(NonCancellable) { started.complete(Unit); late.await() }
                    return listOf(PlaceCandidate(name, name, "地址", GeoPoint(30.2, 120.1), null, "杭州"))
                }
            }, object : PlaceImport {
                override suspend fun receipt(operationId: String): ImportReceipt? = null
                override suspend fun commit(confirmation: ImportConfirmation): ImportReceipt {
                    requests += confirmation
                    return confirmation.success()
                }
            }, resolveDefaultCity = { "上海" })
        c.environment(true, true, true)
        c.editCity("杭州")
        c.editInput("本批")
        c.submit()
        advanceTimeBy(2_101); runCurrent()
        assertTrue(started.isCompleted)
        assertEquals(listOf(IntakeStatus.READY, IntakeStatus.READY, IntakeStatus.FAILED, IntakeStatus.QUERYING),
            c.state.value.items.map { it.status })
        c.toggle(c.state.value.items.first().id)
        c.saveSelected(); runCurrent()
        assertEquals("trip", requests.single().tripId)
        assertEquals(listOf("甲"), requests.single().items.map { it.poi.name })
        assertEquals("杭州", c.state.value.city)
        assertFalse(c.state.value.hasDraft)
        assertTrue(assistantMarkers(c.state.value, null).isEmpty())
        c.editInput("下一轮")
        late.complete(Unit); advanceUntilIdle()
        assertTrue(c.state.value.items.isEmpty())
        assertEquals("下一轮", c.state.value.input)
        c.submit(); advanceUntilIdle()
        assertEquals("杭州", c.state.value.city)
        assertEquals(listOf(IntakeStatus.ALREADY_SAVED, IntakeStatus.READY), c.state.value.items.map { it.status })
        c.selectAll(); c.saveSelected(); advanceUntilIdle()
        assertEquals(listOf("新地点"), requests.last().items.map { it.poi.name })
        assertNotEquals(requests.first().operationId, requests.last().operationId)
    }

    @Test fun changedCategoryAfterFailureCreatesNewContentAndOperation() = runTest {
        val requests = mutableListOf<ImportConfirmation>()
        val c = controller(object : PlaceImport {
            override suspend fun receipt(operationId: String): ImportReceipt? = null
            override suspend fun commit(confirmation: ImportConfirmation): ImportReceipt {
                requests += confirmation
                if (requests.size == 1) error("before transaction")
                return confirmation.success()
            }
        })
        advanceUntilIdle(); c.selectAll(); c.saveSelected(); advanceUntilIdle()
        c.category(c.state.value.items.first().id, PlaceCategory.FOOD)
        c.saveSelected(); advanceUntilIdle()
        assertNotEquals(requests[0].operationId, requests[1].operationId)
        assertEquals(PlaceCategory.FOOD, requests.last().items.single { it.poi.name == "灵隐寺" }.category)
    }

    @Test fun savingLocksSelectionAndEditsUntilTheWriteCompletes() = runTest {
        val gate = CompletableDeferred<Unit>()
        var writes = 0
        val c = controller(object : PlaceImport {
            override suspend fun receipt(operationId: String): ImportReceipt? = null
            override suspend fun commit(confirmation: ImportConfirmation): ImportReceipt {
                writes++; gate.await(); return confirmation.success()
            }
        })
        advanceUntilIdle(); c.selectAll()
        val before = c.state.value
        c.saveSelected(); runCurrent()
        val first = before.items.first()
        c.toggle(first.id); c.selectAll(); c.category(first.id, PlaceCategory.FOOD)
        c.editItem(first.id, "改变", "北京"); c.choosePoi(first.id, first.poi!!.poiId)
        c.editCity("北京"); c.clear(); c.saveSelected()
        assertEquals(before.items, c.state.value.items)
        assertEquals(before.selected, c.state.value.selected)
        assertEquals(1, writes)
        gate.complete(Unit); advanceUntilIdle()
        assertEquals(2, c.state.value.receipt?.addedCount)
    }

    @Test fun mapRevocationOrTripInvalidationBeforeDispatchPreventsWrite() = runTest {
        var writes = 0
        val importer = object : PlaceImport {
            override suspend fun receipt(operationId: String): ImportReceipt? = null
            override suspend fun commit(confirmation: ImportConfirmation): ImportReceipt {
                writes++; return confirmation.success()
            }
        }
        val c = controller(importer)
        advanceUntilIdle(); c.selectAll(); c.saveSelected()
        c.environment(true, false, false); advanceUntilIdle()
        assertEquals(0, writes)
        assertFalse(c.state.value.saving)
        val second = controller(importer)
        advanceUntilIdle(); second.selectAll(); second.saveSelected()
        second.invalidateTrip(); advanceUntilIdle()
        assertEquals(0, writes)
        assertTrue(second.state.value.items.isEmpty())
        assertNull(second.state.value.receipt)
    }

    @Test fun allSelectionExcludesDuplicatesSavedAndLateResults() = runTest {
        val slow = CompletableDeferred<List<PlaceCandidate>>()
        val c = PlaceAssistantController("trip", this,
            PlaceIntentParser { _, _ -> listOf("甲", "重复甲", "已收藏", "慢", "失败").map { PlaceIntent(it, "杭州", it) } },
            object : PlaceSearchDataSource {
                override suspend fun search(keyword: String, city: String?): List<PlaceCandidate> = when (keyword.removePrefix("杭州")) {
                    "慢" -> slow.await()
                    "失败" -> error("failed")
                    else -> listOf(PlaceCandidate(if (keyword == "杭州重复甲") "甲" else keyword.removePrefix("杭州"),
                        keyword, "地址", GeoPoint(30.2, 120.1), null, "杭州"))
                }
            }, object : PlaceImport {
                override suspend fun receipt(operationId: String): ImportReceipt? = null
                override suspend fun commit(confirmation: ImportConfirmation): ImportReceipt = error("Must not save")
            })
        c.environment(true, true, true); c.updateSaved(setOf("已收藏")); c.editInput("地点"); c.submit(); runCurrent()
        c.selectAll()
        val first = c.state.value.items.first().id
        assertEquals(setOf(first), c.state.value.selected)
        slow.complete(listOf(PlaceCandidate("慢", "慢", "地址", GeoPoint(30.2, 120.1), null, "杭州")))
        advanceUntilIdle()
        assertEquals(setOf(first), c.state.value.selected)
        c.selectAll(); assertEquals(2, c.state.value.selected.size)
        c.selectAll(); assertTrue(c.state.value.selected.isEmpty())
    }
}
