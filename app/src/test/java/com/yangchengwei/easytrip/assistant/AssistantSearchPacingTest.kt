package com.yangchengwei.easytrip.assistant

import com.yangchengwei.easytrip.amap.AmapServiceException
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.amap.PlaceSearchDataSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AssistantSearchPacingTest {
    private class NoWrites : PlaceImport {
        var writes = 0
        override suspend fun receipt(operationId: String): ImportReceipt? = null
        override suspend fun commit(confirmation: ImportConfirmation): ImportReceipt {
            writes++
            error("Search must not write")
        }
    }

    private fun candidate(name: String, city: String?) =
        PlaceCandidate(name, name, "", GeoPoint(36.69, 118.47), null, city)

    private fun TestScope.controller(
        names: List<String>,
        pacer: AssistantSearchPacer = AssistantSearchPacer { testScheduler.currentTime },
        events: MutableList<AssistantSearchEvent> = mutableListOf(),
        diagnostics: AssistantSearchDiagnostics = AssistantSearchDiagnostics { events += it },
        search: suspend (String, String?) -> List<PlaceCandidate>,
    ) = PlaceAssistantController("trip", this,
        PlaceIntentParser { _, _ -> names.map { PlaceIntent(it, "青州市", it) } },
        object : PlaceSearchDataSource {
            override suspend fun search(keyword: String, city: String?) = search(keyword, city)
        }, NoWrites(), diagnostics = diagnostics, searchPacer = pacer).also {
        it.environment(true, true, true)
        it.editInput(names.joinToString("、"))
    }

    @Test fun fastSdkResolvesAllEightOnFirstAttemptWithoutBurstOrImplicitSave() = runTest {
        val starts = mutableListOf<Long>()
        val importer = NoWrites()
        val names = listOf("青州古城", "青州博物馆", "范公亭公园", "偶园",
            "昭德古街", "黄花溪", "九龙峪景区", "广福寺")
        val controller = PlaceAssistantController("trip", this,
            PlaceIntentParser { _, _ -> names.map { PlaceIntent(it, "青州市", it) } },
            object : PlaceSearchDataSource {
                override suspend fun search(keyword: String, city: String?): List<PlaceCandidate> {
                    val now = testScheduler.currentTime
                    val recent = starts.count { now - it < 1_000 }
                    starts += now
                    if (recent >= 3) throw AmapServiceException("POI_SEARCH", 1800, "rate limit fixture")
                    delay(200)
                    return listOf(candidate(keyword, city))
                }
            }, importer, searchPacer = AssistantSearchPacer { testScheduler.currentTime })
        controller.environment(true, true, true)
        controller.editInput(names.joinToString("、"))
        controller.submit()
        advanceUntilIdle()

        assertEquals(List(8) { IntakeStatus.READY }, controller.state.value.items.map { it.status })
        assertEquals("No hidden retries", 8, starts.size)
        assertTrue(starts.zipWithNext().all { (a, b) -> b - a >= 700 })
        assertTrue(controller.state.value.selected.isEmpty())
        assertEquals(0, importer.writes)
    }

    @Test fun twentyFastPlacesKeepTheirOrderAndFinishWithoutHiddenRetries() = runTest {
        val names = (1..20).map { "景点$it" }
        val starts = mutableListOf<Pair<String, Long>>()
        val c = controller(names) { name, city ->
            starts += name to testScheduler.currentTime
            listOf(candidate(name, city))
        }
        c.submit(); advanceUntilIdle()
        assertEquals(names.map { "青州$it" }, starts.map { it.first })
        assertEquals((0..19).map { it * 700L }, starts.map { it.second })
        assertEquals(List(20) { IntakeStatus.READY }, c.state.value.items.map { it.status })
        assertFalse(c.state.value.busy)
        assertTrue(c.state.value.selected.isEmpty())
    }

    @Test fun singleItemRetryAndNewBatchDoNotResetLastStart() = runTest {
        val starts = mutableListOf<Long>()
        val c = controller(listOf("偶园")) { name, city ->
            starts += testScheduler.currentTime
            if (starts.size == 1) throw AmapServiceException("POI_SEARCH", 1800, "fixture")
            listOf(candidate(name, city))
        }
        c.submit(); runCurrent()
        assertEquals(IntakeStatus.FAILED, c.state.value.items.single().status)
        assertEquals(listOf(0L), starts)
        c.retry(setOf(c.state.value.items.single().id)); runCurrent()
        assertEquals(listOf(0L), starts)
        advanceTimeBy(700); runCurrent()
        assertEquals(IntakeStatus.READY, c.state.value.items.single().status)
        c.clear(); c.editInput("新批次"); c.submit(); c.submit(); runCurrent()
        assertEquals(listOf(0L, 700L), starts)
        advanceUntilIdle()
        assertEquals(listOf(0L, 700L, 1400L), starts)
    }

    @Test fun sharedPacerLimitsStartsAndInFlightRequestsAcrossControllers() = runTest {
        val pacer = AssistantSearchPacer { testScheduler.currentTime }
        val starts = mutableListOf<Long>()
        var active = 0
        var peak = 0
        val search: suspend (String, String?) -> List<PlaceCandidate> = { name, city ->
            starts += testScheduler.currentTime
            active++; peak = maxOf(peak, active)
            try { delay(2_000); listOf(candidate(name, city)) } finally { active-- }
        }
        val a = controller(listOf("偶园", "广福寺"), pacer, search = search)
        val b = controller(listOf("黄花溪", "昭德古街"), pacer, search = search)
        a.submit(); b.submit(); advanceUntilIdle()
        assertEquals(4, starts.size)
        assertEquals(2, peak)
        assertTrue(starts.zipWithNext().all { (x, y) -> y - x >= 700 })
        assertTrue((a.state.value.items + b.state.value.items).all { it.status == IntakeStatus.READY })
    }

    @Test fun cancellingWaitingBatchDoesNotSendQueuedRequestsOrReserveFutureSlots() = runTest {
        val starts = mutableListOf<Pair<String, Long>>()
        val events = mutableListOf<AssistantSearchEvent>()
        val pacer = AssistantSearchPacer { testScheduler.currentTime }
        val search: suspend (String, String?) -> List<PlaceCandidate> = { name, city ->
            starts += name to testScheduler.currentTime
            listOf(candidate(name, city))
        }
        val a = controller(listOf("偶园", "广福寺", "黄花溪"), pacer, events, search = search)
        a.submit(); runCurrent()
        a.selectAll()
        advanceTimeBy(100); a.cancel(); runCurrent()
        assertEquals(1, events.count { it.outcome == SearchOutcome.STARTED })
        assertEquals(1, a.state.value.selected.size)
        assertEquals(listOf(IntakeStatus.READY, IntakeStatus.CANCELLED, IntakeStatus.CANCELLED),
            a.state.value.items.map { it.status })
        val b = controller(listOf("昭德古街"), pacer, search = search)
        b.submit(); advanceUntilIdle()
        assertEquals(listOf("青州偶园" to 0L, "青州昭德古街" to 700L), starts)
    }

    @Test fun revokedConsentOrInvalidTripCannotSendWaitingQueries() = runTest {
        for (invalidate in listOf(false, true)) {
            val starts = mutableListOf<String>()
            val c = controller(listOf("偶园", "广福寺")) { name, city ->
                starts += name
                listOf(candidate(name, city))
            }
            c.submit(); runCurrent()
            if (invalidate) c.invalidateTrip() else c.environment(true, false, false)
            advanceUntilIdle()
            assertEquals(listOf("青州偶园"), starts)
            assertFalse(c.state.value.busy)
        }
    }

    @Test fun editedItemWhileQueuedDoesNotSendItsStaleName() = runTest {
        val starts = mutableListOf<String>()
        val events = mutableListOf<AssistantSearchEvent>()
        val c = controller(listOf("偶园", "广福寺"), events = events) { name, city ->
            starts += name
            listOf(candidate(name, city))
        }
        c.submit(); runCurrent()
        val second = c.state.value.items[1].id
        c.editItem(second, "黄花溪", "青州市")
        advanceUntilIdle()
        assertEquals(listOf("青州偶园"), starts)
        assertEquals(1, events.count { it.outcome == SearchOutcome.STARTED })
        c.retry(setOf(second)); advanceUntilIdle()
        assertEquals(listOf("青州偶园", "青州黄花溪"), starts)
    }

    @Test fun requestTimeoutStartsAfterWaitingAndOtherResultsSurvive() = runTest {
        val starts = mutableListOf<Long>()
        val events = mutableListOf<AssistantSearchEvent>()
        val c = controller(listOf("偶园", "广福寺"), events = events) { name, city ->
            starts += testScheduler.currentTime
            if (name.endsWith("广福寺")) awaitCancellation()
            listOf(candidate(name, city))
        }
        c.submit(); advanceTimeBy(15_000); runCurrent()
        assertEquals(listOf(0L, 700L), starts)
        assertEquals(IntakeStatus.QUERYING, c.state.value.items[1].status)
        advanceTimeBy(700); runCurrent()
        assertEquals(IntakeStatus.READY, c.state.value.items[0].status)
        assertEquals(IntakeStatus.FAILED, c.state.value.items[1].status)
        assertEquals(SearchOutcome.TIMEOUT, events.last().outcome)
        assertFalse(c.state.value.busy)
    }

    @Test fun slowDiagnosticsDoNotCollapseActualSdkStartSpacing() = runTest {
        val starts = mutableListOf<Long>()
        var first = true
        val c = controller(listOf("偶园", "广福寺", "黄花溪"),
            diagnostics = AssistantSearchDiagnostics {
                if (it.outcome == SearchOutcome.STARTED && first) {
                    first = false
                    delay(450)
                }
            }) { name, city ->
            starts += testScheduler.currentTime
            listOf(candidate(name, city))
        }
        c.submit(); advanceUntilIdle()
        assertEquals(listOf(450L, 1150L, 1850L), starts)
        assertTrue(c.state.value.items.all { it.status == IntakeStatus.READY })
    }

    @Test fun permanentErrorsEmptyResultsAndForeignCityAreNotRetriedAutomatically() = runTest {
        var calls = 0
        val c = controller(listOf("权限错误", "空结果", "异地")) { name, _ ->
            calls++
            when {
                name.endsWith("权限错误") -> throw AmapServiceException("POI_SEARCH", 1001, "fixture")
                name.endsWith("空结果") -> emptyList()
                else -> listOf(candidate(name, "济南市"))
            }
        }
        c.submit(); advanceUntilIdle()
        assertEquals(3, calls)
        assertEquals(listOf(IntakeStatus.FAILED, IntakeStatus.NOT_FOUND, IntakeStatus.NEED_CITY),
            c.state.value.items.map { it.status })
    }
}
