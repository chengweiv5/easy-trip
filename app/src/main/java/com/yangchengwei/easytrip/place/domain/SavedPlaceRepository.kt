package com.yangchengwei.easytrip.place.domain

import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

data class PlaceTag(val id: String, val name: String)
data class SavedPlace(val id: String, val tripId: String, val amapPoiId: String, val name: String, val address: String, val point: GeoPoint, val note: String, val tags: List<PlaceTag>, val cityName: String? = null, val cityAdCode: String? = null, val cityCode: String? = null)
data class PlaceDeletionImpact(val itineraryItemCount: Int, val routeLegCount: Int)
sealed interface SavePlaceResult {
    data class Saved(val id: String) : SavePlaceResult
    data class AlreadySaved(val existingId: String) : SavePlaceResult
}

interface SavedPlaceRepository {
    fun observePlaces(tripId: String, tagIds: Set<String>): Flow<List<SavedPlace>>
    fun observeTags(tripId: String): Flow<List<PlaceTag>>
    fun observeSavedPoiIds(tripId: String): Flow<Set<String>>
    fun observeUsageCounts(tripId: String): Flow<Map<String, Int>> =
        observePlaces(tripId, emptySet()).map { places ->
            places.associate { place -> place.id to usageCount(place.id) }
        }
    fun observePlacesWithUsage(tripId: String, tagIds: Set<String>): Flow<List<Pair<SavedPlace, Int>>> =
        combine(observePlaces(tripId, tagIds), observeUsageCounts(tripId)) { places, usageCounts ->
            places.mapNotNull { place -> usageCounts[place.id]?.let { place to it } }
        }
    suspend fun save(tripId: String, candidate: PlaceCandidate): SavePlaceResult
    suspend fun updateCityIfMissing(placeId: String, city: PlaceCity) = Unit
    suspend fun updateDetails(placeId: String, note: String, tagNames: Set<String>)
    suspend fun usageCount(placeId: String): Int
    suspend fun deletionImpact(placeId: String): PlaceDeletionImpact
    suspend fun deletePlaceAndReferences(placeId: String)
}
