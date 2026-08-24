package com.yangchengwei.easytrip.place.amap

import android.content.Context
import com.amap.api.services.core.AMapException
import com.amap.api.services.poisearch.PoiResult
import com.amap.api.services.poisearch.PoiSearch
import com.yangchengwei.easytrip.amap.AmapConsentToken
import com.yangchengwei.easytrip.amap.AmapPrivacyGate
import com.yangchengwei.easytrip.amap.AmapServiceException
import com.yangchengwei.easytrip.amap.CallbackBoundary
import com.yangchengwei.easytrip.amap.awaitSdkCallback
import com.yangchengwei.easytrip.core.model.GeoPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class PlaceCandidate(
    val poiId: String,
    val name: String,
    val address: String,
    val point: GeoPoint?,
    val cityCode: String?,
)

interface PlaceSearchDataSource {
    suspend fun search(keyword: String, city: String?): List<PlaceCandidate>
}

class AmapPlaceDataSource(context: Context, private val consent: AmapConsentToken) : PlaceSearchDataSource {
    private val context = context.applicationContext

    init { consent.validateActive() }

    override suspend fun search(keyword: String, city: String?): List<PlaceCandidate> {
        consent.validateActive()
        if (keyword.isBlank()) throw AmapServiceException("POI_ARGUMENT", 0, "keyword must not be blank")
        val query = try {
            PoiSearch.Query(keyword, "", city.orEmpty()).apply { pageSize = 20; pageNum = 1 }
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            throw AmapServiceException("POI_QUERY", 0, error.message.orEmpty())
        }
        val search = try { PoiSearch(context, query) } catch (error: AMapException) {
            throw AmapServiceException("POI_CREATE", error.errorCode, error.errorMessage.orEmpty())
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            throw AmapServiceException("POI_CREATE", 0, error.message.orEmpty())
        }
        return awaitSdkCallback("POI", object : CallbackBoundary<Pair<PoiResult?, Int>> {
            override fun install(listener: (Result<Pair<PoiResult?, Int>>) -> Unit) {
                search.setOnPoiSearchListener(object : PoiSearch.OnPoiSearchListener {
                    override fun onPoiSearched(result: PoiResult?, code: Int) = listener(Result.success(result to code))
                    override fun onPoiItemSearched(item: com.amap.api.services.core.PoiItem?, code: Int) = Unit
                })
            }
            override fun clear() = search.setOnPoiSearchListener(null)
            override fun start() = search.searchPOIAsyn()
        }) { callback ->
            consent.validateActive()
            parsePoiSearchResponse(callback)
        }
    }
}

internal fun parsePoiSearchResponse(callback: Pair<PoiResult?, Int>): List<PlaceCandidate> {
    val (result, code) = callback
    if (code != AMapException.CODE_AMAP_SUCCESS) throw AmapServiceException("POI_SEARCH", code, "AMap POI search failed")
    val raw = result?.pois.orEmpty().map { item ->
        RawPlace(item.poiId.orEmpty(), item.title.orEmpty(), item.snippet.orEmpty(), item.latLonPoint?.let { GeoPoint(it.latitude, it.longitude) }, item.cityCode)
    }
    return parsePlaces(raw)
}
