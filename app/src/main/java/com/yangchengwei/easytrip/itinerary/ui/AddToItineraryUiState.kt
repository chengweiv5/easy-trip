package com.yangchengwei.easytrip.itinerary.ui

import com.yangchengwei.easytrip.itinerary.domain.AddPlacesOutcome

enum class AddToItineraryStep { IDLE, SELECT_PLACES, SELECT_TARGET_DAY, COMPLETED }

sealed interface AddToItineraryEditingTarget {
    data object FromPlacePool : AddToItineraryEditingTarget
    data class ForPlace(val placeId: String) : AddToItineraryEditingTarget
    data class ForDay(val dayId: String) : AddToItineraryEditingTarget
}

data class UndoCreatedItemsBatch(
    val dayId: String,
    val itemIds: List<String>,
)

data class FailedItineraryAddition(
    val dayId: String,
    val placeId: String,
)

data class AddToItinerarySubmissionResult(
    val createdItemsByDay: List<UndoCreatedItemsBatch> = emptyList(),
    val failedAdditions: List<FailedItineraryAddition> = emptyList(),
    val missingTargetDayIds: List<String> = emptyList(),
    val missingTargetDayLabels: Map<String, String> = emptyMap(),
    val retryTargetDayIds: List<String> = emptyList(),
)

data class AddToItineraryUiState(
    val selectedPlaceIds: List<String> = emptyList(),
    val targetDayId: String? = null,
    val selectedTargetDayIds: List<String> = emptyList(),
    val editingTarget: AddToItineraryEditingTarget? = null,
    val validityInitialized: Boolean = false,
    val step: AddToItineraryStep = AddToItineraryStep.IDLE,
    val isSubmitting: Boolean = false,
    val result: AddPlacesOutcome? = null,
    val submissionResult: AddToItinerarySubmissionResult? = null,
    val undoBatches: List<UndoCreatedItemsBatch> = emptyList(),
    val isUndoing: Boolean = false,
    val errorMessage: String? = null,
) {
    val selectedPlaceIdSet: Set<String> get() = selectedPlaceIds.toSet()
    val undoCreatedItemIds: List<String> get() = undoBatches.flatMap { it.itemIds }
    val hasStaleFixedDay: Boolean get() = editingTarget is AddToItineraryEditingTarget.ForDay && targetDayId == null
    val canContinue: Boolean get() = selectedPlaceIds.isNotEmpty() && !isSubmitting
    val canSubmit: Boolean get() = validityInitialized && canContinue && targetDayId != null && !isUndoing
}
