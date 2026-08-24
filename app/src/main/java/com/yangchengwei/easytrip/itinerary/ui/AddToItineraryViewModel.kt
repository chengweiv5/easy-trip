package com.yangchengwei.easytrip.itinerary.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewModelScope
import com.yangchengwei.easytrip.itinerary.domain.AddPlacesOutcome
import com.yangchengwei.easytrip.itinerary.domain.AddPlacesRequest
import com.yangchengwei.easytrip.itinerary.domain.AddPlacesToDayUseCase
import com.yangchengwei.easytrip.itinerary.domain.UndoAddedItemsRequest
import com.yangchengwei.easytrip.itinerary.domain.UndoAddedItemsUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal fun mergeUndoIds(oldIds: List<String>, newIds: List<String>): List<String> =
    (oldIds + newIds).distinct()

internal fun mergeUndoBatch(
    batches: List<UndoCreatedItemsBatch>,
    dayId: String,
    newIds: List<String>,
): List<UndoCreatedItemsBatch> {
    if (newIds.isEmpty()) return batches
    val existing = batches.firstOrNull { it.dayId == dayId }
    val merged = UndoCreatedItemsBatch(dayId, mergeUndoIds(existing?.itemIds.orEmpty(), newIds))
    return if (existing == null) batches + merged else batches.map { if (it.dayId == dayId) merged else it }
}

internal fun retainUndoBatches(
    batches: List<UndoCreatedItemsBatch>,
    remainingIds: List<String>,
): List<UndoCreatedItemsBatch> {
    val remaining = remainingIds.toSet()
    return batches.mapNotNull { batch ->
        batch.copy(itemIds = batch.itemIds.filter { it in remaining })
            .takeIf { it.itemIds.isNotEmpty() }
    }
}

class AddToItineraryViewModel(
    private val tripId: String,
    private val addPlaces: AddPlacesToDayUseCase,
    private val undoAddedItems: UndoAddedItemsUseCase,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val mutableState = MutableStateFlow(restoreState())
    val state: StateFlow<AddToItineraryUiState> = mutableState.asStateFlow()
    private var validDayIds: Set<String> = emptySet()
    private var validPlaceIds: Set<String> = emptySet()
    private var submitGeneration = 0L
    private var submitJob: Job? = null
    private var undoGeneration = 0L
    private var undoJob: Job? = null

    fun startFromPool() {
        if (draftLocked()) return
        updateDraft {
            it.copy(editingTarget = AddToItineraryEditingTarget.FromPlacePool, step = AddToItineraryStep.SELECT_PLACES)
        }
    }

    fun startForDay(dayId: String) {
        if (draftLocked() || dayId !in validDayIds) return
        updateDraft {
            it.copy(
                targetDayId = dayId,
                editingTarget = AddToItineraryEditingTarget.ForDay(dayId),
                step = AddToItineraryStep.SELECT_PLACES,
            )
        }
    }

    fun togglePlace(placeId: String) {
        if (draftLocked() || placeId !in validPlaceIds) return
        updateDraft { current ->
            current.copy(
                selectedPlaceIds = if (placeId in current.selectedPlaceIds) {
                    current.selectedPlaceIds - placeId
                } else {
                    current.selectedPlaceIds + placeId
                },
                result = null,
                errorMessage = null,
            )
        }
    }

    fun continueToTargetDay() {
        if (!mutableState.value.canContinue) return
        updateDraft { it.copy(step = AddToItineraryStep.SELECT_TARGET_DAY) }
    }

    fun selectTargetDay(dayId: String) {
        if (draftLocked() || dayId !in validDayIds) return
        updateDraft { it.copy(targetDayId = dayId, step = AddToItineraryStep.SELECT_TARGET_DAY) }
    }

    fun reconcile(validDayIds: Collection<String>, validPlaceIds: Set<String>) {
        this.validDayIds = validDayIds.toSet()
        this.validPlaceIds = validPlaceIds
        if (draftLocked()) return
        applyCurrentValidity()
    }

    fun retryPartial() {
        val current = mutableState.value
        if (draftLocked() || current.result !is AddPlacesOutcome.PartialSuccess) return
        updateDraft { it.copy(result = null, step = AddToItineraryStep.SELECT_TARGET_DAY) }
    }

    fun submit() {
        val current = mutableState.value
        val dayId = current.targetDayId ?: return
        if (!current.canSubmit || dayId !in validDayIds || current.selectedPlaceIds.any { it !in validPlaceIds }) return
        val request = AddPlacesRequest(tripId, dayId, current.selectedPlaceIds)
        val generation = ++submitGeneration
        mutableState.value = current.copy(isSubmitting = true, result = null, errorMessage = null)
        submitJob = viewModelScope.launch {
            try {
                val outcome = addPlaces(request)
                if (generation == submitGeneration) applyOutcome(request, outcome)
            } catch (failure: CancellationException) {
                throw failure
            } catch (_: Throwable) {
                if (generation == submitGeneration) {
                    mutableState.value = mutableState.value.copy(isSubmitting = false, errorMessage = "加入行程失败，请重试")
                    applyCurrentValidity()
                }
            }
        }
    }

    fun undo() {
        val current = mutableState.value
        if (current.isSubmitting || current.isUndoing || current.undoCreatedItemIds.isEmpty()) return
        val itemIds = current.undoCreatedItemIds
        val generation = ++undoGeneration
        mutableState.value = current.copy(isUndoing = true, errorMessage = null)
        undoJob = viewModelScope.launch {
            val outcome = undoAddedItems(UndoAddedItemsRequest(itemIds))
            if (generation == undoGeneration) {
                val currentState = mutableState.value
                mutableState.value = currentState.copy(
                    isUndoing = false,
                    undoBatches = retainUndoBatches(
                        currentState.undoBatches,
                        outcome.remainingItemIds,
                    ),
                    errorMessage = outcome.failure?.let { "撤销失败，请重试" },
                )
                applyCurrentValidity()
            }
        }
    }

    fun cancel() {
        submitGeneration++
        submitJob?.cancel()
        submitJob = null
        undoGeneration++
        undoJob?.cancel()
        undoJob = null
        savedState[SELECTED_PLACE_IDS] = null
        savedState[TARGET_DAY_ID] = null
        savedState[EDITING_TARGET] = null
        mutableState.value = AddToItineraryUiState(validityInitialized = mutableState.value.validityInitialized)
    }

    private fun applyOutcome(request: AddPlacesRequest, outcome: AddPlacesOutcome) {
        val current = mutableState.value
        if (!current.isSubmitting || current.selectedPlaceIds != request.savedPlaceIds || current.targetDayId != request.dayId) return
        val next = when (outcome) {
            is AddPlacesOutcome.Success -> current.copy(
                selectedPlaceIds = emptyList(),
                targetDayId = null,
                editingTarget = null,
                step = AddToItineraryStep.COMPLETED,
                isSubmitting = false,
                result = outcome,
                undoBatches = mergeUndoBatch(current.undoBatches, request.dayId, outcome.createdItemIds),
            )
            is AddPlacesOutcome.PartialSuccess -> current.copy(
                selectedPlaceIds = orderedRetained(current.selectedPlaceIds, outcome.failedPlaceIds),
                step = AddToItineraryStep.SELECT_TARGET_DAY,
                isSubmitting = false,
                result = outcome,
                undoBatches = mergeUndoBatch(current.undoBatches, request.dayId, outcome.createdItemIds),
            )
            is AddPlacesOutcome.TargetDayMissing -> current.copy(
                selectedPlaceIds = orderedRetained(current.selectedPlaceIds, outcome.retainedPlaceIds),
                targetDayId = null,
                step = AddToItineraryStep.SELECT_TARGET_DAY,
                isSubmitting = false,
                result = outcome,
                undoBatches = current.undoBatches.filterNot { it.dayId == request.dayId },
            )
        }
        mutableState.value = next
        persistDraft(next)
        applyCurrentValidity()
    }

    private fun applyCurrentValidity() {
        updateDraft { current ->
            val selected = current.selectedPlaceIds.filter { it in validPlaceIds }
            val target = current.targetDayId?.takeIf { it in validDayIds }
            current.copy(
                selectedPlaceIds = selected,
                targetDayId = target,
                validityInitialized = true,
                step = if (current.targetDayId != null && target == null && selected.isNotEmpty()) {
                    AddToItineraryStep.SELECT_TARGET_DAY
                } else {
                    current.step
                },
            )
        }
    }

    private fun draftLocked(): Boolean = mutableState.value.isSubmitting || mutableState.value.isUndoing

    private fun orderedRetained(original: List<String>, retained: List<String>): List<String> {
        val counts = retained.groupingBy { it }.eachCount().toMutableMap()
        return original.filter { id ->
            val count = counts[id] ?: 0
            if (count == 0) false else {
                counts[id] = count - 1
                true
            }
        }
    }

    private fun updateDraft(transform: (AddToItineraryUiState) -> AddToItineraryUiState) {
        val next = transform(mutableState.value)
        mutableState.value = next
        persistDraft(next)
    }

    private fun restoreState(): AddToItineraryUiState {
        val selected = savedState.get<ArrayList<String>>(SELECTED_PLACE_IDS)?.toList().orEmpty()
        val target = savedState.get<String>(TARGET_DAY_ID)
        val editing = decodeEditingTarget(savedState[EDITING_TARGET])
        return AddToItineraryUiState(
            selectedPlaceIds = selected,
            targetDayId = target,
            editingTarget = editing,
            step = when {
                editing == null -> AddToItineraryStep.IDLE
                target != null -> AddToItineraryStep.SELECT_TARGET_DAY
                else -> AddToItineraryStep.SELECT_PLACES
            },
        )
    }

    private fun persistDraft(state: AddToItineraryUiState) {
        savedState[SELECTED_PLACE_IDS] = ArrayList(state.selectedPlaceIds)
        savedState[TARGET_DAY_ID] = state.targetDayId
        savedState[EDITING_TARGET] = encodeEditingTarget(state.editingTarget)
    }

    private fun encodeEditingTarget(target: AddToItineraryEditingTarget?): String? = when (target) {
        AddToItineraryEditingTarget.FromPlacePool -> FROM_PLACE_POOL
        is AddToItineraryEditingTarget.ForDay -> "for-day:${target.dayId}"
        null -> null
    }

    private fun decodeEditingTarget(raw: String?): AddToItineraryEditingTarget? = when {
        raw == FROM_PLACE_POOL -> AddToItineraryEditingTarget.FromPlacePool
        raw?.startsWith("for-day:") == true && raw.removePrefix("for-day:").isNotBlank() ->
            AddToItineraryEditingTarget.ForDay(raw.removePrefix("for-day:"))
        else -> null
    }

    class Factory(
        private val tripId: String,
        private val addPlaces: AddPlacesToDayUseCase,
        private val undoAddedItems: UndoAddedItemsUseCase,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
            AddToItineraryViewModel(tripId, addPlaces, undoAddedItems, extras.createSavedStateHandle()) as T
    }

    private companion object {
        const val SELECTED_PLACE_IDS = "workspace.addToItinerary.selectedPlaceIds"
        const val TARGET_DAY_ID = "workspace.addToItinerary.targetDayId"
        const val EDITING_TARGET = "workspace.addToItinerary.editingTarget"
        const val FROM_PLACE_POOL = "from-place-pool"
    }
}
