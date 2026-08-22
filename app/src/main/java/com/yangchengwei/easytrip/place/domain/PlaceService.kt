package com.yangchengwei.easytrip.place.domain

class PlaceService(private val repository: SavedPlaceRepository) {
    suspend fun deletionUsageCount(placeId: String) = repository.usageCount(placeId)
    suspend fun deletePlaceAndReferences(placeId: String) = repository.deletePlaceAndReferences(placeId)
}
