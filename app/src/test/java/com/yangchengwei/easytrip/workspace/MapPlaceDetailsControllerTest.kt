package com.yangchengwei.easytrip.workspace

import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.amap.PlaceSearchDataSource
import kotlinx.coroutines.test.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MapPlaceDetailsControllerTest {
    private val point = GeoPoint(30.2, 120.1)

    @Test fun selectingMapPlaceLoadsAddressAndCollectionUsesResolvedDetails() = runTest {
        val controller = MapPlaceDetailsController(this, object : PlaceSearchDataSource {
            override suspend fun search(keyword: String, city: String?) = emptyList<PlaceCandidate>()
            override suspend fun details(poiId: String) =
                PlaceCandidate(poiId, "西湖天地", "上城区南山路147号", point, "0571", "杭州市")
        })
        controller.select(MapPoiUi("poi", "西湖天地", "", point))
        assertEquals(MapPlaceAddressStatus.LOADING, controller.state.value?.status)
        assertNull(controller.collectionCandidate())
        advanceUntilIdle()
        assertEquals(MapPlaceAddressStatus.READY, controller.state.value?.status)
        assertEquals("上城区南山路147号", controller.collectionCandidate()?.address)
        assertEquals("杭州市", controller.collectionCandidate()?.cityName)
    }

    @Test fun existingAddressDoesNotQueryAndKeepsClickedLocation() = runTest {
        var queries = 0
        val controller = MapPlaceDetailsController(this, source { queries++; error("must not query") })
        controller.select(MapPoiUi("poi", "西湖", "已有地址", point))
        advanceUntilIdle()
        assertEquals(0, queries)
        assertEquals("已有地址", controller.collectionCandidate()?.address)
        assertEquals(point, controller.collectionCandidate()?.point)
    }

    @Test fun failureCanRetryWithoutLosingThePlaceAndDoesNotLeakExceptionText() = runTest {
        var attempts = 0
        val controller = MapPlaceDetailsController(this, source {
            if (++attempts == 1) error("untrusted credentials")
            PlaceCandidate(it, "西湖", "补全地址", point, null)
        })
        controller.select(MapPoiUi("poi", "西湖", "", point)); advanceUntilIdle()
        assertEquals(MapPlaceAddressStatus.FAILED, controller.state.value?.status)
        assertEquals("poi", controller.collectionCandidate()?.poiId)
        controller.retry(); advanceUntilIdle()
        assertEquals("补全地址", controller.collectionCandidate()?.address)
        assertEquals(2, attempts)
    }

    @Test fun slowOldRequestCannotReplaceNewlySelectedPlace() = runTest {
        val old = CompletableDeferred<PlaceCandidate?>()
        val controller = MapPlaceDetailsController(this, source {
            if (it == "old") withContext(NonCancellable) { old.await() }
            else PlaceCandidate(it, "新地点", "新地址", point, null)
        })
        controller.select(MapPoiUi("old", "旧地点", "", point)); runCurrent()
        controller.select(MapPoiUi("new", "新地点", "", point)); runCurrent()
        old.complete(PlaceCandidate("old", "旧地点", "旧地址", point, null))
        advanceUntilIdle()
        assertEquals("new", controller.collectionCandidate()?.poiId)
        assertEquals("新地址", controller.collectionCandidate()?.address)
    }

    @Test fun dismissCancelsLookupAndLateResultCannotReopenTheCard() = runTest {
        val response = CompletableDeferred<PlaceCandidate?>()
        val controller = MapPlaceDetailsController(this, source { withContext(NonCancellable) { response.await() } })
        controller.select(MapPoiUi("poi", "西湖", "", point)); runCurrent()
        controller.select(null)
        response.complete(PlaceCandidate("poi", "西湖", "晚到地址", point, null))
        advanceUntilIdle()
        assertNull(controller.state.value)
    }

    @Test fun queryTimeoutIsRetryableAndUnknownIdDoesNotQuery() = runTest {
        var queries = 0
        val controller = MapPlaceDetailsController(this, source { queries++; CompletableDeferred<PlaceCandidate?>().await() })
        controller.select(MapPoiUi(null, "无编号", "", point)); advanceUntilIdle()
        assertNull(controller.collectionCandidate())
        assertEquals(0, queries)
        controller.select(MapPoiUi("poi", "西湖", "", point)); advanceUntilIdle()
        assertEquals(MapPlaceAddressStatus.FAILED, controller.state.value?.status)
        assertEquals(1, queries)
    }

    @Test fun noAddressAndMismatchedIdentityAreDifferentOutcomes() = runTest {
        val empty = MapPlaceDetailsController(this, source { null })
        empty.select(MapPoiUi("poi", "西湖", "", point)); advanceUntilIdle()
        assertEquals(MapPlaceAddressStatus.UNAVAILABLE, empty.state.value?.status)
        val mismatch = MapPlaceDetailsController(this, source { PlaceCandidate("other", "别处", "错误地址", point, null) })
        mismatch.select(MapPoiUi("poi", "西湖", "", point)); advanceUntilIdle()
        assertEquals(MapPlaceAddressStatus.FAILED, mismatch.state.value?.status)
        assertEquals("", mismatch.collectionCandidate()?.address)
    }

    private fun source(lookup: suspend (String) -> PlaceCandidate?) = object : PlaceSearchDataSource {
        override suspend fun search(keyword: String, city: String?) = emptyList<PlaceCandidate>()
        override suspend fun details(poiId: String) = lookup(poiId)
    }
}
