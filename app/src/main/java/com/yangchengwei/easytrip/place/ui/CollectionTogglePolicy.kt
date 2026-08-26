package com.yangchengwei.easytrip.place.ui

import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.domain.PlaceDeletionImpact
import com.yangchengwei.easytrip.place.domain.SavedPlace

data class PendingCollectionRemoval(
    val candidate: PlaceCandidate,
    val place: SavedPlace,
    val impact: PlaceDeletionImpact,
)

sealed interface CollectionDecision {
    data object Save : CollectionDecision
    data class RemoveNow(val placeId: String) : CollectionDecision
    data class Confirm(val placeId: String, val impact: PlaceDeletionImpact) : CollectionDecision
}

fun decideCollectionToggle(
    candidate: PlaceCandidate,
    savedPlace: SavedPlace?,
    impact: PlaceDeletionImpact?,
): CollectionDecision {
    if (savedPlace == null) return CollectionDecision.Save
    require(impact != null && impact.itineraryItemCount >= 0 && impact.routeLegCount >= 0) {
        "Saved place deletion impact must be non-negative"
    }
    return if (impact.itineraryItemCount == 0 && impact.routeLegCount == 0) {
        CollectionDecision.RemoveNow(savedPlace.id)
    } else {
        CollectionDecision.Confirm(savedPlace.id, impact)
    }
}
