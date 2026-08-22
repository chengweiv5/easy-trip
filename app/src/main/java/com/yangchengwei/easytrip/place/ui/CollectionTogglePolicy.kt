package com.yangchengwei.easytrip.place.ui

import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.domain.SavedPlace

data class PendingCollectionRemoval(
    val candidate: PlaceCandidate,
    val place: SavedPlace,
    val usageCount: Int,
)

sealed interface CollectionDecision {
    data object Save : CollectionDecision
    data class RemoveNow(val placeId: String) : CollectionDecision
    data class Confirm(val placeId: String, val usageCount: Int) : CollectionDecision
}

fun decideCollectionToggle(
    candidate: PlaceCandidate,
    savedPlace: SavedPlace?,
    usageCount: Int?,
): CollectionDecision {
    if (savedPlace == null) return CollectionDecision.Save
    require(usageCount != null && usageCount >= 0) { "Saved place usage count must be non-negative" }
    return if (usageCount == 0) {
        CollectionDecision.RemoveNow(savedPlace.id)
    } else {
        CollectionDecision.Confirm(savedPlace.id, usageCount)
    }
}
