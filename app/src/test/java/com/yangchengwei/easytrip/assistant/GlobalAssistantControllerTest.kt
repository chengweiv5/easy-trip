package com.yangchengwei.easytrip.assistant

import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.amap.PlaceSearchDataSource
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

class GlobalAssistantControllerTest {
    @Test fun newTripNeverUsesLocationAndWaitsForCityConfirmationBeforeSearching() = runTest {
        var searches = 0
        val controller = PlaceAssistantController(null, this,
            PlaceIntentParser { _, city ->
                assertEquals("", city)
                listOf(PlaceIntent("雷峰塔", "杭州", "杭州雷峰塔"))
            }, object : PlaceSearchDataSource {
                override suspend fun search(keyword: String, city: String?): List<PlaceCandidate> {
                    searches++
                    return listOf(PlaceCandidate("p", "雷峰塔", "地址", GeoPoint(30.2, 120.1), null, "杭州"))
                }
            }, object : PlaceImport {
                override suspend fun commit(confirmation: ImportConfirmation): ImportReceipt = error("new trip must not import")
                override suspend fun receipt(operationId: String): ImportReceipt? = null
                override suspend fun latestReceipt(tripId: String): ImportReceipt? = error("new trip must not read an old receipt")
            }, resolveDefaultCity = { error("new trip must not locate") }, target = AssistantTarget.NewTripDraft)
        controller.environment(true, true, true)
        controller.recoverReceipt()
        controller.editInput("杭州雷峰塔")
        controller.submit(); advanceUntilIdle()
        assertEquals(0, searches)
        assertTrue(controller.state.value.awaitingCityConfirmation)
        controller.confirmCities(); advanceUntilIdle()
        assertEquals(1, searches)
        assertEquals(IntakeStatus.READY, controller.state.value.items.single().status)
        controller.selectAll()
        assertEquals(1, controller.selectedImportItems().size)
        controller.environment(true, true, false)
        assertTrue(controller.selectedImportItems().isEmpty())
        assertEquals(1, controller.selectedImportItems(requireMapReady = false).size)
        controller.saveSelected(); advanceUntilIdle()
        assertNull(controller.state.value.receipt)
    }
}
