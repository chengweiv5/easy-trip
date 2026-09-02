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

internal fun mergeSubmissionResults(
    previous: AddToItinerarySubmissionResult?,
    current: AddToItinerarySubmissionResult,
): AddToItinerarySubmissionResult {
    val created = current.createdItemsByDay.fold(previous?.createdItemsByDay.orEmpty()) { batches, batch ->
        mergeUndoBatch(batches, batch.dayId, batch.itemIds)
    }
    val missingTargetDayIds = (previous?.missingTargetDayIds.orEmpty() + current.missingTargetDayIds).distinct()
    val missingTargetDayLabels = previous?.missingTargetDayLabels.orEmpty() + current.missingTargetDayLabels
    return current.copy(
        createdItemsByDay = created,
        missingTargetDayIds = missingTargetDayIds,
        missingTargetDayLabels = missingTargetDayLabels.filterKeys { it in missingTargetDayIds },
    )
}

private fun AddPlacesOutcome.toSubmissionResult(request: AddPlacesRequest): AddToItinerarySubmissionResult = when (this) {
    is AddPlacesOutcome.Success -> AddToItinerarySubmissionResult(
        createdItemsByDay = listOfNotNull(
            UndoCreatedItemsBatch(request.dayId, createdItemIds).takeIf { it.itemIds.isNotEmpty() },
        ),
    )
    is AddPlacesOutcome.PartialSuccess -> AddToItinerarySubmissionResult(
        createdItemsByDay = listOfNotNull(
            UndoCreatedItemsBatch(request.dayId, createdItemIds).takeIf { it.itemIds.isNotEmpty() },
        ),
        failedAdditions = failedPlaceIds.map { FailedItineraryAddition(request.dayId, it) },
        retryTargetDayIds = listOf(request.dayId).takeIf { failedPlaceIds.isNotEmpty() }.orEmpty(),
    )
    is AddPlacesOutcome.TargetDayMissing -> AddToItinerarySubmissionResult(
        missingTargetDayIds = listOf(request.dayId),
        retryTargetDayIds = emptyList(),
    )
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
    private var validDayOrder: List<String> = emptyList()
    private var validPlaceIds: Set<String> = emptySet()
    private var submitGeneration = 0L
    private var submitJob: Job? = null
    private var undoGeneration = 0L
    private var undoJob: Job? = null

    fun startFromPool(): Boolean {
        if (draftLocked()) return false
        updateDraft {
            it.copy(
                targetDayId = null,
                selectedTargetDayIds = emptyList(),
                editingTarget = AddToItineraryEditingTarget.FromPlacePool,
                step = AddToItineraryStep.SELECT_PLACES,
            )
        }
        return true
    }

    fun startForDay(dayId: String): Boolean {
        if (draftLocked() || dayId !in validDayIds) return false
        updateDraft {
            it.copy(
                targetDayId = dayId,
                selectedTargetDayIds = emptyList(),
                editingTarget = AddToItineraryEditingTarget.ForDay(dayId),
                step = AddToItineraryStep.SELECT_PLACES,
            )
        }
        return true
    }

    fun startForPlace(placeId: String): Boolean {
        if (draftLocked() || mutableState.value.step != AddToItineraryStep.IDLE || placeId !in validPlaceIds) return false
        updateDraft {
            it.copy(
                selectedPlaceIds = listOf(placeId),
                targetDayId = null,
                selectedTargetDayIds = emptyList(),
                editingTarget = AddToItineraryEditingTarget.ForPlace(placeId),
                step = AddToItineraryStep.SELECT_TARGET_DAY,
                result = null,
                undoBatches = emptyList(),
                errorMessage = null,
            )
        }
        return true
    }

    fun toggleTargetDay(dayId: String) {
        if (draftLocked() || dayId !in validDayIds || mutableState.value.editingTarget !is AddToItineraryEditingTarget.ForPlace) return
        updateDraft { current ->
            val selected = if (dayId in current.selectedTargetDayIds) {
                current.selectedTargetDayIds - dayId
            } else {
                (current.selectedTargetDayIds + dayId).sortedBy { validDayOrder.indexOf(it) }
            }
            current.copy(
                targetDayId = selected.firstOrNull(),
                selectedTargetDayIds = selected,
                result = null,
                errorMessage = null,
            )
        }
    }

    fun togglePlace(placeId: String) {
        if (draftLocked() || placeId !in validPlaceIds || mutableState.value.editingTarget is AddToItineraryEditingTarget.ForPlace) return
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
        val current = mutableState.value
        if (!current.canContinue) return
        if (current.editingTarget is AddToItineraryEditingTarget.ForDay) {
            submit()
        } else {
            updateDraft { it.copy(step = AddToItineraryStep.SELECT_TARGET_DAY) }
        }
    }

    fun selectTargetDay(dayId: String) {
        if (draftLocked() || dayId !in validDayIds || mutableState.value.editingTarget is AddToItineraryEditingTarget.ForDay) return
        updateDraft {
            it.copy(
                targetDayId = dayId,
                selectedTargetDayIds = if (it.editingTarget is AddToItineraryEditingTarget.ForPlace) listOf(dayId) else it.selectedTargetDayIds,
                step = AddToItineraryStep.SELECT_TARGET_DAY,
            )
        }
    }

    fun reconcile(validDayIds: Collection<String>, validPlaceIds: Set<String>) {
        this.validDayOrder = validDayIds.toList()
        this.validDayIds = validDayOrder.toSet()
        this.validPlaceIds = validPlaceIds
        if (draftLocked()) return
        applyCurrentValidity()
    }

    fun retryPartial() {
        val current = mutableState.value
        if (draftLocked() || (current.result !is AddPlacesOutcome.PartialSuccess && current.submissionResult?.failedAdditions.isNullOrEmpty())) return
        updateDraft { it.copy(result = null, step = AddToItineraryStep.SELECT_TARGET_DAY) }
    }

    fun reselectTargetDays() {
        val current = mutableState.value
        val result = current.submissionResult ?: return
        if (draftLocked() || result.missingTargetDayIds.isEmpty()) return
        val remaining = result.retryTargetDayIds.filter { it in validDayIds }
        updateDraft {
            it.copy(
                targetDayId = remaining.firstOrNull(),
                selectedTargetDayIds = remaining,
                result = null,
                step = AddToItineraryStep.SELECT_TARGET_DAY,
            )
        }
    }

    fun submit() {
        val current = mutableState.value
        val targetDayIds = when (current.editingTarget) {
            is AddToItineraryEditingTarget.ForPlace -> current.selectedTargetDayIds
            else -> listOfNotNull(current.targetDayId)
        }
        if (!current.canSubmit || targetDayIds.isEmpty() || targetDayIds.any { it !in validDayIds } || current.selectedPlaceIds.any { it !in validPlaceIds }) return
        val requests = targetDayIds.map { dayId -> AddPlacesRequest(tripId, dayId, current.selectedPlaceIds) }
        val generation = ++submitGeneration
        mutableState.value = current.copy(isSubmitting = true, result = null, errorMessage = null)
        submitJob = viewModelScope.launch {
            val outcomes = mutableListOf<Pair<AddPlacesRequest, AddPlacesOutcome>>()
            try {
                var index = 0
                while (index < requests.size) {
                    val request = requests[index]
                    try {
                        outcomes += request to addPlaces(request)
                        index++
                    } catch (failure: CancellationException) {
                        throw failure
                    } catch (_: Throwable) {
                        if (requests.size == 1) throw IllegalStateException("single request failed")
                        requests.drop(index).forEach { unfinished ->
                            outcomes += unfinished to AddPlacesOutcome.PartialSuccess(
                                unfinished.dayId,
                                emptyList(),
                                unfinished.savedPlaceIds,
                            )
                        }
                        break
                    }
                }
                if (generation == submitGeneration) {
                    if (current.editingTarget is AddToItineraryEditingTarget.ForPlace) {
                        applyForPlaceOutcomes(requests, outcomes.take(requests.size), current.submissionResult)
                    } else {
                        applyOutcome(requests.single(), outcomes.single().second)
                    }
                }
            } catch (failure: CancellationException) {
                throw failure
            } catch (_: Throwable) {
                if (generation == submitGeneration) {
                    mutableState.value = mutableState.value.copy(isSubmitting = false, errorMessage = "加入行程失败，请重试")
                    persistDraft(mutableState.value)
                    applyCurrentValidity()
                }
            }
        }
    }

    private fun applyForPlaceOutcomes(
        requests: List<AddPlacesRequest>,
        outcomes: List<Pair<AddPlacesRequest, AddPlacesOutcome>>,
        previousSubmissionResult: AddToItinerarySubmissionResult?,
    ) {
        val current = mutableState.value
        if (!current.isSubmitting || current.editingTarget !is AddToItineraryEditingTarget.ForPlace || current.selectedTargetDayIds != requests.map { it.dayId }) return
        val createdBatches = outcomes.mapNotNull { (request, outcome) ->
            val batch = when (outcome) {
                is AddPlacesOutcome.Success -> UndoCreatedItemsBatch(request.dayId, outcome.createdItemIds)
                is AddPlacesOutcome.PartialSuccess -> UndoCreatedItemsBatch(request.dayId, outcome.createdItemIds)
                is AddPlacesOutcome.TargetDayMissing -> null
            }
            batch?.takeIf { it.itemIds.isNotEmpty() }
        }
        val failed = outcomes.flatMap { (request, outcome) ->
            when (outcome) {
                is AddPlacesOutcome.PartialSuccess -> outcome.failedPlaceIds.map { FailedItineraryAddition(request.dayId, it) }
                is AddPlacesOutcome.TargetDayMissing -> emptyList()
                is AddPlacesOutcome.Success -> emptyList()
            }
        }
        val missingDays = outcomes.mapNotNull { (request, outcome) -> request.dayId.takeIf { outcome is AddPlacesOutcome.TargetDayMissing } }
        val missingDayLabels = missingDays.associateWith { dayId ->
            validDayOrder.indexOf(dayId).takeIf { it >= 0 }?.let { "第 ${it + 1} 天" } ?: "已删除的旅行日（$dayId）"
        }
        val retryDayIds = failed.map { it.dayId }.distinct() - missingDays.toSet()
        val mergedUndo = createdBatches.fold(current.undoBatches) { batches, batch -> mergeUndoBatch(batches, batch.dayId, batch.itemIds) }
        val hasRetryableFailures = retryDayIds.isNotEmpty()
        val hasMissingTargets = missingDays.isNotEmpty()
        val aggregate = mergeSubmissionResults(
            previousSubmissionResult,
            AddToItinerarySubmissionResult(
                createdItemsByDay = createdBatches,
                failedAdditions = failed,
                missingTargetDayIds = missingDays,
                missingTargetDayLabels = missingDayLabels,
                retryTargetDayIds = retryDayIds,
            ),
        )
        val next = current.copy(
            selectedPlaceIds = if (hasRetryableFailures || hasMissingTargets) listOf((current.editingTarget as AddToItineraryEditingTarget.ForPlace).placeId) else emptyList(),
            targetDayId = if (hasRetryableFailures) retryDayIds.firstOrNull() else null,
            selectedTargetDayIds = if (hasRetryableFailures) retryDayIds else emptyList(),
            editingTarget = if (hasRetryableFailures || hasMissingTargets) current.editingTarget else null,
            step = if (hasRetryableFailures) AddToItineraryStep.SELECT_TARGET_DAY else AddToItineraryStep.COMPLETED,
            isSubmitting = false,
            result = outcomes.lastOrNull()?.second,
            submissionResult = aggregate,
            undoBatches = mergedUndo,
        )
        mutableState.value = next
        persistDraft(next)
        applyCurrentValidity()
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
        savedState[SELECTED_TARGET_DAY_IDS] = null
        savedState[EDITING_TARGET] = null
        savedState[STEP] = null
        savedState[LEGACY_RESULT] = null
        savedState[UNDO_BATCHES] = null
        savedState[SUBMISSION_CREATED] = null
        savedState[SUBMISSION_FAILED] = null
        savedState[SUBMISSION_MISSING_IDS] = null
        savedState[SUBMISSION_MISSING_LABELS] = null
        savedState[SUBMISSION_RETRY_DAYS] = null
        savedState[HAS_SUBMISSION_RESULT] = null
        mutableState.value = AddToItineraryUiState(validityInitialized = mutableState.value.validityInitialized)
    }

    private fun applyOutcome(request: AddPlacesRequest, outcome: AddPlacesOutcome) {
        val current = mutableState.value
        if (!current.isSubmitting || current.selectedPlaceIds != request.savedPlaceIds || current.targetDayId != request.dayId) return
        val next = when (outcome) {
            is AddPlacesOutcome.Success -> current.copy(
                selectedPlaceIds = emptyList(),
                targetDayId = null,
                selectedTargetDayIds = emptyList(),
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
                selectedTargetDayIds = if (current.editingTarget is AddToItineraryEditingTarget.ForPlace) {
                    current.selectedTargetDayIds - request.dayId
                } else {
                    current.selectedTargetDayIds
                },
                step = AddToItineraryStep.SELECT_TARGET_DAY,
                isSubmitting = false,
                result = outcome,
                undoBatches = current.undoBatches.filterNot { it.dayId == request.dayId },
            )
        }
        val currentProjection = outcome.toSubmissionResult(request).let { projection ->
            if (outcome !is AddPlacesOutcome.TargetDayMissing) projection else projection.copy(
                missingTargetDayLabels = mapOf(
                    request.dayId to (
                        validDayOrder.indexOf(request.dayId).takeIf { it >= 0 }?.let { "第 ${it + 1} 天" }
                            ?: "已删除的旅行日（${request.dayId}）"
                        ),
                ),
            )
        }
        val projected = next.copy(
            submissionResult = mergeSubmissionResults(current.submissionResult, currentProjection),
        )
        mutableState.value = projected
        persistDraft(projected)
        applyCurrentValidity()
    }

    private fun applyCurrentValidity() {
        updateDraft { current ->
            val selected = current.selectedPlaceIds.filter { it in validPlaceIds }
            val target = current.targetDayId?.takeIf { it in validDayIds }
            val selectedTargets = current.selectedTargetDayIds.filter { it in validDayIds }
            current.copy(
                selectedPlaceIds = selected,
                targetDayId = if (current.editingTarget is AddToItineraryEditingTarget.ForPlace) selectedTargets.firstOrNull() else target,
                selectedTargetDayIds = selectedTargets,
                validityInitialized = true,
                undoBatches = current.undoBatches.filter { it.dayId in validDayIds },
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
        val selectedTargets = savedState.get<ArrayList<String>>(SELECTED_TARGET_DAY_IDS)?.toList().orEmpty()
        val editing = decodeEditingTarget(savedState[EDITING_TARGET])
        val restoredResult = decodeLegacyResult(savedState[LEGACY_RESULT])
        val submissionResult = decodeSubmissionResult()
        val undoBatches = decodeBatches(savedState.get<ArrayList<String>>(UNDO_BATCHES).orEmpty())
        val persistedStep = savedState.get<String>(STEP)
            ?.let { raw -> AddToItineraryStep.entries.firstOrNull { it.name == raw } }
        val step = when {
            persistedStep == AddToItineraryStep.COMPLETED && submissionResult != null -> AddToItineraryStep.COMPLETED
            editing != null && persistedStep != null && persistedStep != AddToItineraryStep.IDLE -> persistedStep
            editing == null -> AddToItineraryStep.IDLE
            target != null -> AddToItineraryStep.SELECT_TARGET_DAY
            else -> AddToItineraryStep.SELECT_PLACES
        }
        return AddToItineraryUiState(
            selectedPlaceIds = selected,
            targetDayId = target,
            selectedTargetDayIds = selectedTargets,
            editingTarget = editing,
            step = step,
            result = restoredResult,
            submissionResult = submissionResult,
            undoBatches = undoBatches,
        )
    }

    private fun persistDraft(state: AddToItineraryUiState) {
        savedState[SELECTED_PLACE_IDS] = ArrayList(state.selectedPlaceIds)
        savedState[TARGET_DAY_ID] = state.targetDayId
        savedState[SELECTED_TARGET_DAY_IDS] = ArrayList(state.selectedTargetDayIds)
        savedState[EDITING_TARGET] = encodeEditingTarget(state.editingTarget)
        savedState[STEP] = state.step.name
        savedState[LEGACY_RESULT] = encodeLegacyResult(state.result)
        savedState[UNDO_BATCHES] = ArrayList(encodeBatches(state.undoBatches))
        val result = state.submissionResult
        savedState[SUBMISSION_CREATED] = result?.let { ArrayList(encodeBatches(it.createdItemsByDay)) }
        savedState[SUBMISSION_FAILED] = result?.let { ArrayList(it.failedAdditions.map { failure -> encodeFields(failure.dayId, failure.placeId) }) }
        savedState[SUBMISSION_MISSING_IDS] = result?.let { ArrayList(it.missingTargetDayIds) }
        savedState[SUBMISSION_MISSING_LABELS] = result?.let {
            ArrayList(it.missingTargetDayLabels.map { (dayId, label) -> encodeFields(dayId, label) })
        }
        savedState[SUBMISSION_RETRY_DAYS] = result?.let { ArrayList(it.retryTargetDayIds) }
        savedState[HAS_SUBMISSION_RESULT] = result != null
    }

    private fun decodeSubmissionResult(): AddToItinerarySubmissionResult? {
        if (savedState.get<Boolean>(HAS_SUBMISSION_RESULT) != true) return null
        return AddToItinerarySubmissionResult(
            createdItemsByDay = decodeBatches(savedState.get<ArrayList<String>>(SUBMISSION_CREATED).orEmpty()),
            failedAdditions = savedState.get<ArrayList<String>>(SUBMISSION_FAILED).orEmpty().mapNotNull { encoded ->
                decodeFields(encoded).takeIf { it.size == 2 }?.let { FailedItineraryAddition(it[0], it[1]) }
            },
            missingTargetDayIds = savedState.get<ArrayList<String>>(SUBMISSION_MISSING_IDS)?.toList().orEmpty(),
            missingTargetDayLabels = savedState.get<ArrayList<String>>(SUBMISSION_MISSING_LABELS).orEmpty().mapNotNull { encoded ->
                decodeFields(encoded).takeIf { it.size == 2 }?.let { it[0] to it[1] }
            }.toMap(),
            retryTargetDayIds = savedState.get<ArrayList<String>>(SUBMISSION_RETRY_DAYS)?.toList().orEmpty(),
        )
    }

    private fun encodeBatches(batches: List<UndoCreatedItemsBatch>): List<String> =
        batches.map { encodeFields(it.dayId, *it.itemIds.toTypedArray()) }

    private fun decodeBatches(encoded: List<String>): List<UndoCreatedItemsBatch> = encoded.mapNotNull { value ->
        decodeFields(value).takeIf { it.isNotEmpty() }?.let { UndoCreatedItemsBatch(it.first(), it.drop(1)) }
    }

    private fun encodeLegacyResult(result: AddPlacesOutcome?): String? = when (result) {
        is AddPlacesOutcome.Success -> encodeFields("success", result.dayId, *result.createdItemIds.toTypedArray())
        is AddPlacesOutcome.PartialSuccess -> encodeFields(
            "partial",
            result.dayId,
            result.createdItemIds.size.toString(),
            *result.createdItemIds.toTypedArray(),
            *result.failedPlaceIds.toTypedArray(),
        )
        is AddPlacesOutcome.TargetDayMissing -> encodeFields(
            "missing",
            result.retainedPlaceIds.size.toString(),
            *result.retainedPlaceIds.toTypedArray(),
            *result.createdItemIds.toTypedArray(),
        )
        null -> null
    }

    private fun decodeLegacyResult(encoded: String?): AddPlacesOutcome? {
        val fields = encoded?.let(::decodeFields).orEmpty()
        return when (fields.firstOrNull()) {
            "success" -> fields.getOrNull(1)?.let { AddPlacesOutcome.Success(it, fields.drop(2)) }
            "partial" -> {
                val count = fields.getOrNull(2)?.toIntOrNull() ?: return null
                fields.getOrNull(1)?.let { AddPlacesOutcome.PartialSuccess(it, fields.drop(3).take(count), fields.drop(3 + count)) }
            }
            "missing" -> {
                val count = fields.getOrNull(1)?.toIntOrNull() ?: return null
                AddPlacesOutcome.TargetDayMissing(fields.drop(2).take(count), fields.drop(2 + count))
            }
            else -> null
        }
    }

    private fun encodeFields(vararg fields: String): String = fields.joinToString(separator = "") { "${it.length}:$it" }

    private fun decodeFields(encoded: String): List<String> {
        val fields = mutableListOf<String>()
        var offset = 0
        while (offset < encoded.length) {
            val separator = encoded.indexOf(':', offset)
            if (separator < 0) return emptyList()
            val length = encoded.substring(offset, separator).toIntOrNull() ?: return emptyList()
            val start = separator + 1
            val end = start + length
            if (end > encoded.length) return emptyList()
            fields += encoded.substring(start, end)
            offset = end
        }
        return fields
    }

    private fun encodeEditingTarget(target: AddToItineraryEditingTarget?): String? = when (target) {
        AddToItineraryEditingTarget.FromPlacePool -> FROM_PLACE_POOL
        is AddToItineraryEditingTarget.ForPlace -> "for-place:${target.placeId}"
        is AddToItineraryEditingTarget.ForDay -> "for-day:${target.dayId}"
        null -> null
    }

    private fun decodeEditingTarget(raw: String?): AddToItineraryEditingTarget? = when {
        raw == FROM_PLACE_POOL -> AddToItineraryEditingTarget.FromPlacePool
        raw?.startsWith("for-place:") == true && raw.removePrefix("for-place:").isNotBlank() ->
            AddToItineraryEditingTarget.ForPlace(raw.removePrefix("for-place:"))
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
        const val SELECTED_TARGET_DAY_IDS = "workspace.addToItinerary.selectedTargetDayIds"
        const val EDITING_TARGET = "workspace.addToItinerary.editingTarget"
        const val STEP = "workspace.addToItinerary.step"
        const val LEGACY_RESULT = "workspace.addToItinerary.result"
        const val UNDO_BATCHES = "workspace.addToItinerary.undoBatches"
        const val SUBMISSION_CREATED = "workspace.addToItinerary.submission.created"
        const val SUBMISSION_FAILED = "workspace.addToItinerary.submission.failed"
        const val SUBMISSION_MISSING_IDS = "workspace.addToItinerary.submission.missingIds"
        const val SUBMISSION_MISSING_LABELS = "workspace.addToItinerary.submission.missingLabels"
        const val SUBMISSION_RETRY_DAYS = "workspace.addToItinerary.submission.retryDays"
        const val HAS_SUBMISSION_RESULT = "workspace.addToItinerary.submission.present"
        const val FROM_PLACE_POOL = "from-place-pool"
    }
}
