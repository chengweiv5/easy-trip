package com.yangchengwei.easytrip.assistant

import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.amap.PlaceSearchDataSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AssistantCityContextTest {
    private class Importer : PlaceImport {
        override suspend fun commit(confirmation: ImportConfirmation) = error("Must not save")
        override suspend fun latestReceipt(tripId: String): ImportReceipt? = null
        override suspend fun receipt(operationId: String): ImportReceipt? = null
    }

    @Test fun openingAssistantFillsTripCityBeforeAnyInputOrSearch() = runTest {
        val controller = PlaceAssistantController("xuzhou-trip", this,
            PlaceIntentParser { _, _ -> error("Opening must not call the model") },
            null, Importer(), resolveDefaultCity = { "徐州市" })
        controller.environment(configured = false, consent = true, mapReady = false)

        controller.prepareDefaultCity()
        advanceUntilIdle()

        assertEquals("徐州市", controller.state.value.city)
        assertEquals("", controller.state.value.input)
        assertTrue(controller.state.value.items.isEmpty())
        assertFalse(controller.state.value.busy)
    }

    @Test fun manualCityEnteredDuringOpeningLookupIsNeverOverwritten() = runTest {
        val lookup = CompletableDeferred<String?>()
        val controller = PlaceAssistantController("trip", this,
            PlaceIntentParser { _, _ -> error("Must not parse") }, null, Importer(),
            resolveDefaultCity = { lookup.await() })
        controller.environment(false, true, false)
        launch { controller.prepareDefaultCity() }
        runCurrent()
        assertTrue(controller.state.value.resolvingCity)
        controller.editCity("杭州")
        lookup.complete("徐州市")
        advanceUntilIdle()
        assertEquals("杭州", controller.state.value.city)
        assertFalse(controller.state.value.resolvingCity)
        controller.prepareDefaultCity()
        assertEquals("杭州", controller.state.value.city)
    }

    @Test fun cancelledOpeningDoesNotFillLateAndCanRetryOnReopen() = runTest {
        val lookup = CompletableDeferred<String?>()
        val controller = PlaceAssistantController("trip", this,
            PlaceIntentParser { _, _ -> error("Must not parse") }, null, Importer(),
            resolveDefaultCity = { lookup.await() })
        controller.environment(false, true, false)
        val opening = launch { controller.prepareDefaultCity() }
        runCurrent()
        opening.cancel()
        lookup.complete("徐州市")
        advanceUntilIdle()
        assertEquals("", controller.state.value.city)
        assertFalse(controller.state.value.resolvingCity)
        controller.prepareDefaultCity()
        assertEquals("徐州市", controller.state.value.city)
    }

    @Test fun revokedConsentRejectsPendingCityAndNoConsentDoesNotQuery() = runTest {
        val lookup = CompletableDeferred<String?>()
        var lookups = 0
        val controller = PlaceAssistantController("trip", this,
            PlaceIntentParser { _, _ -> error("Must not parse") }, null, Importer(),
            resolveDefaultCity = { lookups++; lookup.await() })
        controller.prepareDefaultCity()
        assertEquals(0, lookups)
        controller.environment(false, true, false)
        launch { controller.prepareDefaultCity() }
        runCurrent()
        controller.environment(false, false, false)
        lookup.complete("徐州市"); advanceUntilIdle()
        assertEquals("", controller.state.value.city)
        assertFalse(controller.state.value.resolvingCity)
    }

    @Test fun openingTimeoutAndFailureKeepCityEditable() = runTest {
        val controller = PlaceAssistantController("trip", this,
            PlaceIntentParser { _, _ -> error("Must not parse") }, null, Importer(),
            resolveDefaultCity = { awaitCancellation() })
        controller.environment(false, true, false)
        launch { controller.prepareDefaultCity() }
        runCurrent()
        advanceTimeBy(12_000); runCurrent()
        assertEquals("", controller.state.value.city)
        assertFalse(controller.state.value.resolvingCity)
        controller.updateCityResolver { throw IllegalStateException("private SDK failure") }
        controller.prepareDefaultCity()
        assertEquals("", controller.state.value.city)
        assertNull(controller.state.value.error)
        controller.editCity("徐州")
        assertEquals("徐州", controller.state.value.city)
    }

    @Test fun openingCityLookupDoesNotSubmitOrRunTwiceWhilePending() = runTest {
        val lookup = CompletableDeferred<String?>()
        var lookups = 0
        val controller = PlaceAssistantController("trip", this,
            PlaceIntentParser { _, _ -> error("Must not parse while city is pending") }, null, Importer(),
            resolveDefaultCity = { lookups++; lookup.await() })
        controller.environment(true, true, true); controller.editInput("云龙湖")
        launch { controller.prepareDefaultCity() }; runCurrent()
        controller.prepareDefaultCity()
        controller.submit()
        assertFalse(controller.state.value.busy)
        assertTrue(controller.state.value.items.isEmpty())
        assertEquals(1, lookups)
        lookup.complete("徐州市"); advanceUntilIdle()
        assertEquals("徐州市", controller.state.value.city)
    }

    @Test fun xuzhouTripSearchesBothNamesWithoutRequiringCityInInput() = runTest {
        val queries = mutableListOf<Pair<String, String?>>()
        var parserCity = ""
        val controller = PlaceAssistantController("xuzhou-trip", this,
            PlaceIntentParser { input, city ->
                assertEquals("云龙湖、云龙山", input)
                parserCity = city
                listOf("云龙湖", "云龙山").map { PlaceIntent(it, "", it) }
            },
            object : PlaceSearchDataSource {
                override suspend fun search(keyword: String, city: String?): List<PlaceCandidate> {
                    queries += keyword to city
                    return listOf(PlaceCandidate(keyword, keyword, "徐州市", GeoPoint(34.23, 117.17), null,
                        cityName = "徐州市"))
                }
            }, Importer(),
            resolveDefaultCity = { "徐州市" })
        controller.environment(true, true, true)
        controller.editInput("云龙湖、云龙山")
        controller.submit()
        advanceUntilIdle()

        assertEquals("徐州市", parserCity)
        assertEquals(listOf("云龙湖" to "徐州市", "云龙山" to "徐州市"), queries)
        assertEquals(listOf(IntakeStatus.READY, IntakeStatus.READY), controller.state.value.items.map { it.status })
        assertTrue(controller.state.value.selected.isEmpty())
    }

    @Test fun explicitCityWinsAndNoDefaultLookupIsMade() = runTest {
        var lookups = 0
        var parserCity = ""
        val controller = PlaceAssistantController("trip", this,
            PlaceIntentParser { _, city -> parserCity = city; listOf(PlaceIntent("云龙湖", city, "云龙湖")) },
            null, Importer(), resolveDefaultCity = { lookups++; "徐州市" })
        controller.environment(true, true, true)
        controller.editCity("杭州"); controller.editInput("云龙湖"); controller.submit(); advanceUntilIdle()
        assertEquals(0, lookups)
        assertEquals("杭州", parserCity)
    }

    @Test fun cityEditCancelsPendingLookupAndDoesNotSendOldInput() = runTest {
        val lookup = CompletableDeferred<String?>()
        var parses = 0
        val controller = PlaceAssistantController("trip", this,
            PlaceIntentParser { _, _ -> parses++; emptyList() }, null, Importer(),
            resolveDefaultCity = { lookup.await() })
        controller.environment(true, true, true)
        controller.editInput("云龙湖"); controller.submit(); runCurrent()
        controller.editCity("杭州")
        lookup.complete("徐州市"); advanceUntilIdle()
        assertEquals("杭州", controller.state.value.city)
        assertEquals(0, parses)
        assertFalse(controller.state.value.busy)
    }

    @Test fun unknownTripCityCanBeSuppliedOnceWithoutReparsingOrChangingOtherResults() = runTest {
        var parses = 0
        val queries = mutableListOf<Pair<String, String?>>()
        val controller = PlaceAssistantController("trip", this,
            PlaceIntentParser { _, _ ->
                parses++
                listOf(PlaceIntent("云龙湖", "", "云龙湖"), PlaceIntent("云龙山", "", "云龙山"),
                    PlaceIntent("故宫", "北京", "故宫"))
            },
            object : PlaceSearchDataSource {
                override suspend fun search(keyword: String, city: String?): List<PlaceCandidate> {
                    queries += keyword to city
                    return listOf(PlaceCandidate(keyword, keyword, "地址", GeoPoint(34.23, 117.17), null, city))
                }
            }, Importer(), resolveDefaultCity = { null })
        controller.environment(true, true, true)
        controller.editInput("云龙湖、云龙山、北京的故宫"); controller.submit(); advanceUntilIdle()
        assertEquals(listOf(IntakeStatus.NEED_CITY, IntakeStatus.NEED_CITY, IntakeStatus.READY),
            controller.state.value.items.map { it.status })
        val palace = controller.state.value.items.last()
        controller.toggle(palace.id)
        controller.supplyMissingCity("徐州"); advanceUntilIdle()
        assertEquals(1, parses)
        assertEquals(listOf("故宫" to "北京", "云龙湖" to "徐州", "云龙山" to "徐州"), queries)
        assertEquals(palace, controller.state.value.items.last())
        assertEquals(setOf(palace.id), controller.state.value.selected)
        assertTrue(controller.state.value.items.all { it.status == IntakeStatus.READY })
    }
}
