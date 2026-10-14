package com.yangchengwei.easytrip.assistant

import com.yangchengwei.easytrip.assistant.cityfirst.GuideResultCodec
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import com.yangchengwei.easytrip.place.amap.PlaceSearchDataSource
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.core.model.GeoPoint

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class GuideControllerRecoveryTest {
    private val importer = object : PlaceImport {
        override suspend fun receipt(operationId: String): ImportReceipt? = null
        override suspend fun commit(confirmation: ImportConfirmation): ImportReceipt = error("No automatic collection")
    }

    @Test fun confirmedCitiesWinOverAlreadyRunningLocationLookup() = runTest {
        val location = CompletableDeferred<String>()
        val c = PlaceAssistantController("trip", this, PlaceIntentParser { _, _ -> emptyList() }, null, importer)
        c.environment(true, true, true)
        c.updateCityResolver { location.await() }
        launch { c.prepareDefaultCity() }
        runCurrent()
        c.setGuideCityScope(AssistantCityScope.parse("青州、济南"))
        location.complete("北京")
        advanceUntilIdle()
        assertEquals("", c.state.value.city)
        assertFalse(c.state.value.resolvingCity)
    }

    @Test fun explicitClearingAllowsClaimedRetryButSameLiveBatchIsNotAcceptedTwice() = runTest {
        val c = PlaceAssistantController("trip", this, PlaceIntentParser { _, _ -> emptyList() }, null, importer)
        val scope = AssistantCityScope.parse("青州")
        assertTrue(c.acceptGuideInput("operation", "原攻略", emptyList(), null, scope))
        assertFalse(c.acceptGuideInput("operation", "原攻略", emptyList(), null, scope))
        c.clear()
        assertTrue(c.acceptGuideInput("operation", "原攻略", emptyList(), null, scope))
    }

    @Test fun interruptedQueriesRestoreAsManualRetryNotPermanentlyQuerying() {
        val state = PlaceAssistantState(input = "博物馆", items = listOf(
            IntakeItem("a", "A", PlaceIntent("博物馆", "青州", "博物馆"), status = IntakeStatus.QUERYING)))
        val recovered = GuideResultCodec.decode(GuideResultCodec.encode(state))
        assertEquals(IntakeStatus.CANCELLED, recovered.items.single().status)
        assertFalse(recovered.busy)
    }

    @Test fun multiCityScopePreservesIndividualCitiesAndLeavesUnknownUnqueried() = runTest {
        val searched = mutableListOf<String?>()
        val parser = object : PlaceIntentParser {
            override suspend fun parse(input: String, defaultCity: String): List<PlaceIntent> = error("Use city-aware seam")
            override suspend fun parseInScope(input: String, defaultCity: String, scope: AssistantCityScope?): List<PlaceIntent> {
                assertEquals(listOf("青州", "济南"), scope!!.names)
                assertEquals("", defaultCity)
                return listOf(PlaceIntent("博物馆", "青州", "博物馆"),
                    PlaceIntent("公园", "济南", "公园"), PlaceIntent("南门", "", "南门"))
            }
        }
        val c = PlaceAssistantController("trip", this, parser, object : PlaceSearchDataSource {
            override suspend fun search(keyword: String, city: String?): List<PlaceCandidate> {
                searched += city
                return listOf(PlaceCandidate(keyword, keyword.removePrefix(city.orEmpty()), "地址", GeoPoint(30.0, 120.0), null, city))
            }
        }, importer)
        c.environment(true, true, true)
        c.acceptGuideInput("operation", "博物馆 公园 南门", emptyList(), null, AssistantCityScope.parse("青州、济南"))
        c.submit(); advanceUntilIdle()
        assertEquals(listOf("青州", "济南"), searched)
        assertEquals(IntakeStatus.NEED_CITY, c.state.value.items.last().status)
        assertEquals("", c.state.value.items.last().intent.city)
    }
}
