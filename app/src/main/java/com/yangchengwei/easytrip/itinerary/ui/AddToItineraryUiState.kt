package com.yangchengwei.easytrip.itinerary.ui

import com.yangchengwei.easytrip.itinerary.domain.AddPlacesOutcome

enum class AddToItineraryStep { IDLE, SELECT_PLACES, SELECT_TARGET_DAY, COMPLETED }

sealed interface AddToItineraryEditingTarget {
    data object FromPlacePool : AddToItineraryEditingTarget
    data class ForDay(val dayId: String) : AddToItineraryEditingTarget
}

data class AddToItineraryUiState(
    val selectedPlaceIds: List<String> = emptyList(),
    val targetDayId: String? = null,
    val editingTarget: AddToItineraryEditingTarget? = null,
    val validityInitialized: Boolean = false,
    val step: AddToItineraryStep = AddToItineraryStep.IDLE,
    val isSubmitting: Boolean = false,
    val result: AddPlacesOutcome? = null,
    val undoCreatedItemIds: List<String> = emptyList(),
    val isUndoing: Boolean = false,
    val errorMessage: String? = null,
) {
    val selectedPlaceIdSet: Set<String> get() = selectedPlaceIds.toSet()
    val canContinue: Boolean get() = selectedPlaceIds.isNotEmpty() && !isSubmitting
    val canSubmit: Boolean get() = validityInitialized && canContinue && targetDayId != null && !isUndoing
}
