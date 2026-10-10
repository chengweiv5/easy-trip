package com.yangchengwei.easytrip.place.ui

import com.yangchengwei.easytrip.place.domain.PlaceCategory

data class PlaceDetailValues(val category: PlaceCategory, val note: String, val tags: Set<String>)

fun hasPlaceDetailChanges(current: PlaceDetailValues, original: PlaceDetailValues, newTagInput: String): Boolean =
    current != original || newTagInput.isNotBlank()

enum class PlaceDetailCloseDecision { IGNORE, CLOSE, CONFIRM_DISCARD }

fun placeDetailCloseDecision(isSaving: Boolean, isDirty: Boolean): PlaceDetailCloseDecision = when {
    isSaving -> PlaceDetailCloseDecision.IGNORE
    isDirty -> PlaceDetailCloseDecision.CONFIRM_DISCARD
    else -> PlaceDetailCloseDecision.CLOSE
}
