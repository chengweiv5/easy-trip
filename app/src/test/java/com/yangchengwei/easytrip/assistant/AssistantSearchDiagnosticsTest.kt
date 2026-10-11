package com.yangchengwei.easytrip.assistant

import com.yangchengwei.easytrip.amap.AmapServiceException
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.amap.PlaceSearchDataSource
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AssistantSearchDiagnosticsTest {
    private fun TestScope.controller(
        events: MutableList<AssistantSearchEvent>,
        diagnostics: AssistantSearchDiagnostics = AssistantSearchDiagnostics { events += it },
        search: suspend (String, String?) -> List<PlaceCandidate>,
    ) = PlaceAssistantController("private-trip", this,
        PlaceIntentParser { _, _ -> listOf(PlaceIntent("广福寺 secret-token", "青州市", "private source")) },
        object : PlaceSearchDataSource {
            override suspend fun search(keyword: String, city: String?) = search(keyword, city)
        }, object : PlaceImport {
            override suspend fun receipt(operationId: String): ImportReceipt? = null
            override suspend fun commit(confirmation: ImportConfirmation): ImportReceipt = error("No writes")
        }, diagnostics = diagnostics).also {
        it.environment(true, true, true); it.editInput("private input"); it.submit()
    }

    @Test fun recordsSafeRequestIdentityCountsAndSdkErrorWithoutPayload() = runTest {
        val events = mutableListOf<AssistantSearchEvent>()
        var fail = true
        val c = controller(events) { _, _ ->
            if (fail) throw AmapServiceException("secret operation", 1806, "secret-token private source")
            listOf(PlaceCandidate("private-poi-id", "广福寺", "私人地址", GeoPoint(36.69, 118.47), null, "青州市"))
        }
        advanceUntilIdle()
        assertEquals(listOf(SearchOutcome.STARTED, SearchOutcome.SDK_ERROR), events.map { it.outcome })
        assertEquals(1806, events.last().errorCode)
        assertEquals(IntakeStatus.FAILED, c.state.value.items.single().status)
        fail = false
        c.retry(); advanceUntilIdle()
        assertEquals(SearchOutcome.READY, events.last().outcome)
        assertEquals(1, events.last().resultCount)
        assertEquals(1, events.last().sameCityCount)
        assertTrue(events.all { it.cityQualified && it.elapsedMs >= 0 })
        assertEquals(1, events.map { it.queryId }.distinct().size)
        assertEquals(2, events.map { it.run }.distinct().size)
        val log = events.joinToString("\n") { it.toJsonLine() }
        listOf("广福寺", "青州", "secret", "private", "私人地址", "36.69", "118.47").forEach {
            assertFalse("Raw payload leaked: $it", log.contains(it))
        }
        assertTrue(c.state.value.selected.isEmpty())
    }

    @Test fun timeoutNoResultsAndWrongCityHaveDifferentOutcomes() = runTest {
        val timeout = mutableListOf<AssistantSearchEvent>()
        val slow = controller(timeout) { _, _ -> awaitCancellation() }
        advanceUntilIdle()
        assertEquals(SearchOutcome.TIMEOUT, timeout.last().outcome)
        assertEquals(IntakeStatus.FAILED, slow.state.value.items.single().status)
        val empty = mutableListOf<AssistantSearchEvent>()
        controller(empty) { _, _ -> emptyList() }; advanceUntilIdle()
        assertEquals(SearchOutcome.NOT_FOUND, empty.last().outcome)
        val foreign = mutableListOf<AssistantSearchEvent>()
        controller(foreign) { _, _ ->
            listOf(PlaceCandidate("foreign", "foreign", "", GeoPoint(36.6, 117.0), null, "济南市"))
        }; advanceUntilIdle()
        assertEquals(SearchOutcome.CITY_MISMATCH, foreign.last().outcome)
        assertEquals(1, foreign.last().resultCount)
        assertEquals(0, foreign.last().sameCityCount)
    }

    @Test fun userCancellationIsLoggedAndLateResultsCannotBecomeReady() = runTest {
        val events = mutableListOf<AssistantSearchEvent>()
        val c = controller(events) { _, _ -> awaitCancellation() }
        runCurrent()
        c.cancel(); advanceUntilIdle()
        assertEquals(SearchOutcome.CANCELLED, events.last().outcome)
        assertEquals(IntakeStatus.CANCELLED, c.state.value.items.single().status)
        assertTrue(c.state.value.selected.isEmpty())
    }

    @Test fun fingerprintsAreNotLinkableAcrossControllerSessions() = runTest {
        val a = mutableListOf<AssistantSearchEvent>()
        val b = mutableListOf<AssistantSearchEvent>()
        controller(a) { _, _ -> emptyList() }; controller(b) { _, _ -> emptyList() }
        advanceUntilIdle()
        assertNotEquals(a.first().session, b.first().session)
        assertNotEquals(a.first().queryId, b.first().queryId)
        assertNotEquals(a.first().cityId, b.first().cityId)
    }

    @Test fun unavailableOrSuspendedDiagnosticSinkDoesNotPreventSearch() = runTest {
        for (sink in listOf(
            AssistantSearchDiagnostics { error("disk unavailable") },
            AssistantSearchDiagnostics { awaitCancellation() },
        )) {
            val c = controller(mutableListOf(), sink) { _, city ->
                listOf(PlaceCandidate("id", "name", "", GeoPoint(36.69, 118.47), null, city))
            }
            advanceUntilIdle()
            assertEquals(IntakeStatus.READY, c.state.value.items.single().status)
            assertFalse(c.state.value.busy)
        }
    }
}
