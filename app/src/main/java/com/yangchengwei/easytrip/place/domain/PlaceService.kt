package com.yangchengwei.easytrip.place.domain

class PlaceService(private val repository: SavedPlaceRepository) {
    suspend fun deletionImpact(placeId: String) = repository.deletionImpact(placeId)
    suspend fun deletePlaceAndReferences(placeId: String) = repository.deletePlaceAndReferences(placeId)
}
