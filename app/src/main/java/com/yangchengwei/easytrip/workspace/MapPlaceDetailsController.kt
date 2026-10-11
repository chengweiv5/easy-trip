package com.yangchengwei.easytrip.workspace

import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.amap.PlaceSearchDataSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

enum class MapPlaceAddressStatus { LOADING, READY, UNAVAILABLE, FAILED }

data class MapPlaceDetailsState(
    val poi: MapPoiUi,
    val status: MapPlaceAddressStatus,
    val details: PlaceCandidate? = null,
)

/** One visible card owns one cancellable lookup; a late callback cannot update a newer card. */
class MapPlaceDetailsController(
    private val scope: CoroutineScope,
    private val source: PlaceSearchDataSource?,
) {
    private val mutableState = MutableStateFlow<MapPlaceDetailsState?>(null)
    val state = mutableState.asStateFlow()
    private var request: Job? = null
    private var generation = 0L

    fun select(poi: MapPoiUi?) {
        generation++
        request?.cancel()
        mutableState.value = poi?.let {
            MapPlaceDetailsState(it, if (it.address.isNotBlank()) MapPlaceAddressStatus.READY
                else if (it.poiId.isNullOrBlank()) MapPlaceAddressStatus.UNAVAILABLE
                else MapPlaceAddressStatus.LOADING)
        }
        if (mutableState.value?.status == MapPlaceAddressStatus.LOADING) lookup()
    }

    fun retry() {
        if (mutableState.value?.status == MapPlaceAddressStatus.FAILED) lookup()
    }

    fun collectionCandidate(allowWhileLoading: Boolean = false): PlaceCandidate? {
        val current = state.value ?: return null
        val id = current.poi.poiId?.takeIf(String::isNotBlank) ?: return null
        if (!allowWhileLoading && current.status == MapPlaceAddressStatus.LOADING) return null
        return current.details ?: PlaceCandidate(id, current.poi.name, current.poi.address, current.poi.point, null)
    }

    private fun lookup() {
        val current = state.value ?: return
        val id = current.poi.poiId?.takeIf(String::isNotBlank) ?: return
        val version = ++generation
        request?.cancel()
        mutableState.value = current.copy(status = MapPlaceAddressStatus.LOADING)
        request = scope.launch {
            try {
                val result = withTimeout(8_000) {
                    checkNotNull(source) { "Place lookup unavailable" }.details(id)
                }
                check(result == null || result.poiId == id) { "Place lookup identity mismatch" }
                if (generation != version) return@launch
                val address = result?.address?.trim().orEmpty()
                mutableState.value = current.copy(
                    poi = current.poi.copy(address = address),
                    status = if (address.isBlank()) MapPlaceAddressStatus.UNAVAILABLE else MapPlaceAddressStatus.READY,
                    // The clicked identity and coordinate stay authoritative; never move the map.
                    details = result?.copy(name = current.poi.name, address = address, point = current.poi.point),
                )
            } catch (_: TimeoutCancellationException) {
                if (generation == version) mutableState.value = current.copy(status = MapPlaceAddressStatus.FAILED)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (generation == version) mutableState.value = current.copy(status = MapPlaceAddressStatus.FAILED)
            }
        }
    }
}
