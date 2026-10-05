package com.yangchengwei.easytrip.workspace

import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.place.amap.PlaceCandidate

/** Temporary search snapshot, scoped to one workspace; never written to the place repository. */
data class WorkspaceSearchResults(val query: String, val places: List<PlaceCandidate>) {
    val mappedPlaces: List<PlaceCandidate>
        get() = places.filter { candidate ->
            candidate.point?.let {
                it.latitude.isFinite() && it.longitude.isFinite() &&
                    it.latitude in -90.0..90.0 && it.longitude in -180.0..180.0
            } == true
        }.distinctBy(PlaceCandidate::poiId)

    // SavedStateHandle supports string lists. Keep the complete result, including places
    // without coordinates, so returning to the list does not silently drop results.
    fun save(): ArrayList<String> = arrayListOf(query).apply {
        places.forEach {
            addAll(listOf(
                it.poiId, it.name, it.address,
                it.point?.latitude?.toString().orEmpty(), it.point?.longitude?.toString().orEmpty(),
                it.cityCode.orEmpty(), it.cityName.orEmpty(), it.cityAdCode.orEmpty(),
                it.cityMetadataVersion.toString(),
            ))
        }
    }

    companion object {
        fun restore(payload: List<String>?): WorkspaceSearchResults? {
            if (payload.isNullOrEmpty() || (payload.size - 1) % 9 != 0) return null
            return WorkspaceSearchResults(payload.first(), payload.drop(1).chunked(9).map { row ->
                val latitude = row[3].toDoubleOrNull()
                val longitude = row[4].toDoubleOrNull()
                PlaceCandidate(
                    row[0], row[1], row[2],
                    if (latitude != null && longitude != null) GeoPoint(latitude, longitude) else null,
                    row[5].ifEmpty { null }, row[6].ifEmpty { null }, row[7].ifEmpty { null },
                    row[8].toIntOrNull() ?: 0,
                )
            })
        }
    }
}
