package com.yangchengwei.easytrip.place.ui

import com.yangchengwei.easytrip.place.amap.PlaceSearchDataSource
import com.yangchengwei.easytrip.place.domain.SavedPlace
import com.yangchengwei.easytrip.place.domain.SavedPlaceRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** Best-effort metadata repair, once per POI and source session. Never blocks the list. */
internal class PlaceCityEnricher(
    private val scope: CoroutineScope,
    private val repository: SavedPlaceRepository,
    private var source: PlaceSearchDataSource?,
) {
    private var places = emptyList<SavedPlace>()
    private val attempted = mutableSetOf<String>()
    private var job: Job? = null

    fun setSource(value: PlaceSearchDataSource?) {
        if (source === value) return
        job?.cancel()
        job = null
        source = value
        attempted.clear()
        start()
    }

    fun submit(value: List<SavedPlace>) { places = value; start() }

    private fun start() {
        val currentSource = source ?: return
        if (job?.isActive == true) return
        job = scope.launch {
            while (true) {
                val place = places.firstOrNull { it.cityMetadataVersion < 1 && it.amapPoiId !in attempted } ?: break
                attempted += place.amapPoiId
                try {
                    val poiCity = try { withTimeoutOrNull(4_000) { currentSource.cityForPoi(place.amapPoiId) } }
                    catch (error: CancellationException) { throw error }
                    catch (_: Exception) { null }
                    val city = poiCity?.takeIf { it.adCode != null }
                        ?: withTimeoutOrNull(4_000) { currentSource.cityAt(place.point) }
                        ?: poiCity
                    if (city?.adCode != null && source === currentSource) repository.updateCityMetadata(place.id, city)
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    // Missing metadata stays selectable and is retried on the next workspace visit.
                }
            }
        }
    }
}
