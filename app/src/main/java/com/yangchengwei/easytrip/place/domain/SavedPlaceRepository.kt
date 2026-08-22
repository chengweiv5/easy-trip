package com.yangchengwei.easytrip.place.domain

import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import kotlinx.coroutines.flow.Flow

data class PlaceTag(val id: String, val name: String)
data class SavedPlace(val id: String, val tripId: String, val amapPoiId: String, val name: String, val address: String, val point: GeoPoint, val note: String, val tags: List<PlaceTag>)
sealed interface SavePlaceResult {
    data class Saved(val id: String) : SavePlaceResult
    data class AlreadySaved(val existingId: String) : SavePlaceResult
}

interface SavedPlaceRepository {
    fun observePlaces(tripId: String, tagIds: Set<String>): Flow<List<SavedPlace>>
    fun observeTags(tripId: String): Flow<List<PlaceTag>>
    fun observeSavedPoiIds(tripId: String): Flow<Set<String>>
    suspend fun save(tripId: String, candidate: PlaceCandidate): SavePlaceResult
    suspend fun updateDetails(placeId: String, note: String, tagNames: Set<String>)
    suspend fun usageCount(placeId: String): Int
    suspend fun deletePlaceAndReferences(placeId: String)
}
