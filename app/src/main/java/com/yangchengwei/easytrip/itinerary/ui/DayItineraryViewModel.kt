package com.yangchengwei.easytrip.itinerary.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yangchengwei.easytrip.core.model.TransportMode
import com.yangchengwei.easytrip.itinerary.domain.DayItinerary
import com.yangchengwei.easytrip.itinerary.domain.ItineraryRepository
import com.yangchengwei.easytrip.place.domain.SavedPlace
import com.yangchengwei.easytrip.route.data.RouteLegEntity
import com.yangchengwei.easytrip.route.domain.RouteLegRepository
import com.yangchengwei.easytrip.route.domain.RouteRefreshCoordinator
import com.yangchengwei.easytrip.trip.domain.TripDay
import com.yangchengwei.easytrip.trip.domain.TripRepository
import com.yangchengwei.easytrip.trip.domain.TripService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

private data class PendingAppendDay(
    val generation: Long,
    val baselineDayIds: List<String>,
    val expectedNewDayId: String? = null,
    val automaticObservationRetryUsed: Boolean = false,
) {
    val serviceCompleted: Boolean get() = expectedNewDayId != null
}

data class DayItineraryUiState(
    val days: List<TripDay> = emptyList(),
    val selectedDayId: String? = null,
    val savedPlaces: List<SavedPlace> = emptyList(),
    val items: List<ItineraryItemUi> = emptyList(),
    val legs: List<RouteLegUi> = emptyList(),
    val previewOrder: List<String> = emptyList(),
    val editDraft: ItineraryEditDraft? = null,
    val crossDayMove: CrossDayMoveDraft? = null,
    val deleteConfirmation: ItineraryDeleteConfirmation? = null,
    val modeEditor: RouteModeEditDraft? = null,
    val isAppendingDay: Boolean = false,
    val appendDayError: String? = null,
    val appendDayCompletionToken: Long? = null,
    val error: String? = null,
) {
    val moveItemId: String? get() = crossDayMove?.itemId
    val modeLegId: String? get() = modeEditor?.legId
}

@OptIn(ExperimentalCoroutinesApi::class)
class DayItineraryViewModel(
    private val tripId: String,
    private val trips: TripRepository,
    private val itineraries: ItineraryRepository,
    private val routeLegs: RouteLegRepository,
    private var coordinator: RouteRefreshCoordinator?,
    savedPlaces: Flow<List<SavedPlace>> = emptyFlow(),
    selectedDays: Flow<String?> = emptyFlow(),
    private val tripService: TripService = TripService(trips),
) : ViewModel() {
    private val selectedDay = MutableStateFlow<String?>(null)
    private var hasExternalSelection = false
    private var externalSelectedDayId: String? = null
    private val mutable = MutableStateFlow(DayItineraryUiState())
    private var nextAppendDayCompletionToken = 0L
    private var appendDayGeneration = 0L
    private var pendingAppendDay: PendingAppendDay? = null
    private var tripObservationJob: Job? = null
    private var tripObservationGeneration = 0L
    private var terminatedTripObservationGeneration: Long? = null
    private var requestedTripObservationRestartGeneration: Long? = null
    private var mustRestoreTripObservationGeneration: Long? = null
    private var nextEditGeneration = 0L
    private var nextDeleteGeneration = 0L
    private var nextMoveGeneration = 0L
    private var nextModeGeneration = 0L
    val state: StateFlow<DayItineraryUiState> = mutable.asStateFlow()

    init {
        startTripObservation()
        viewModelScope.launch {
            selectedDay.flatMapLatest { dayId ->
                if (dayId == null) flowOf(null)
                else combine(itineraries.observeDay(dayId), routeLegs.observeDay(dayId)) { day, legs -> day to legs }
            }.collect { dayAndLegs ->
                if (dayAndLegs == null) clearDay()
                else applyDay(dayAndLegs.first, dayAndLegs.second)
            }
        }
        viewModelScope.launch {
            savedPlaces.collect { mutable.value = mutable.value.copy(savedPlaces = it) }
        }
        viewModelScope.launch {
            selectedDays.collect {
                hasExternalSelection = true
                externalSelectedDayId = it
                selectDay(it)
            }
        }
    }

    private fun startTripObservation() {
        if (tripObservationJob?.isActive == true) return
        launchTripObservation(previous = tripObservationJob)
    }

    private fun restartTripObservation() {
        launchTripObservation(previous = tripObservationJob)
    }

    private fun launchTripObservation(previous: Job?) {
        val generation = ++tripObservationGeneration
        terminatedTripObservationGeneration = null
        requestedTripObservationRestartGeneration = null
        tripObservationJob = viewModelScope.launch {
            previous?.cancelAndJoin()
            try {
                trips.observeTrip(tripId).filterNotNull().collect { trip ->
                    if (generation == tripObservationGeneration) acceptTripObservation(trip)
                }
                handleTripObservationTermination(generation)
            } catch (failure: CancellationException) {
                throw failure
            } catch (_: Throwable) {
                handleTripObservationTermination(generation)
            }
        }
    }

    private fun handleTripObservationTermination(generation: Long) {
        if (generation != tripObservationGeneration) return
        terminatedTripObservationGeneration = generation
        val mustRestore = mustRestoreTripObservationGeneration == generation
        if (mustRestore) mustRestoreTripObservationGeneration = null
        val pending = pendingAppendDay
        val shouldRestart = when {
            pending == null -> mustRestore
            !pending.serviceCompleted -> {
                mutable.value = mutable.value.copy(appendDayError = "无法加载旅行日，正在等待同步")
                false
            }
            pending.automaticObservationRetryUsed -> {
                mutable.value = mutable.value.copy(
                    isAppendingDay = false,
                    appendDayError = "新增旅行日等待同步失败，请重试",
                )
                false
            }
            else -> true
        }
        if (shouldRestart) requestTripObservationRestart(generation)
        if (requestedTripObservationRestartGeneration == generation) {
            performRequestedTripObservationRestart(generation)
        } else {
            tripObservationJob = null
        }
    }

    private fun ensureTripObservation() {
        val generation = tripObservationGeneration
        if (terminatedTripObservationGeneration != generation) return
        requestTripObservationRestart(generation)
        if (tripObservationJob?.isActive != true) performRequestedTripObservationRestart(generation)
    }

    private fun requestTripObservationRestart(generation: Long) {
        if (generation != tripObservationGeneration || requestedTripObservationRestartGeneration == generation) return
        requestedTripObservationRestartGeneration = generation
        pendingAppendDay?.takeIf { it.serviceCompleted }?.let { pending ->
            pendingAppendDay = pending.copy(automaticObservationRetryUsed = true)
        }
    }

    private fun performRequestedTripObservationRestart(generation: Long) {
        if (generation != tripObservationGeneration || requestedTripObservationRestartGeneration != generation) return
        val previous = tripObservationJob
        requestedTripObservationRestartGeneration = null
        tripObservationJob = viewModelScope.launch {
            previous?.cancelAndJoin()
            if (generation == tripObservationGeneration) launchTripObservation(previous = null)
        }
    }

    private fun acceptTripObservation(trip: com.yangchengwei.easytrip.trip.domain.TripWithDays) {
        val chosen = if (hasExternalSelection) {
            externalSelectedDayId?.takeIf { id -> trip.days.any { it.id == id } }
        } else {
            selectedDay.value?.takeIf { id -> trip.days.any { it.id == id } } ?: trip.days.firstOrNull()?.id
        }
        selectedDay.value = chosen
        mutable.value = mutable.value.copy(days = trip.days, selectedDayId = chosen)
        reconcileAppendDayCompletion(trip.days)
        if (chosen == null) clearDay()
    }

    private fun completeAppendService(request: PendingAppendDay, newDayId: String) {
        if (pendingAppendDay?.generation != request.generation) return
        pendingAppendDay = request.copy(expectedNewDayId = newDayId)
        ensureTripObservation()
        reconcileAppendDayCompletion(state.value.days)
    }

    fun selectDay(id: String?) {
        if (id == selectedDay.value) return
        nextMoveGeneration++
        selectedDay.value = id
        clearDay(id)
    }

    fun addPlace(placeId: String) {
        val day = state.value.selectedDayId ?: return
        val index = state.value.items.size
        viewModelScope.launch { runCatching { itineraries.addItem(day, placeId, index) }.onFailure(::showError) }
    }

    fun appendTripDay() {
        if (mutable.value.appendDayCompletionToken != null) return
        val existing = pendingAppendDay
        if (existing != null) {
            if (!existing.serviceCompleted) return
            mutable.value = mutable.value.copy(isAppendingDay = true, appendDayError = null)
            startTripObservation()
            return
        }
        if (mutable.value.isAppendingDay) return
        val request = PendingAppendDay(++appendDayGeneration, state.value.days.map(TripDay::id))
        pendingAppendDay = request
        mutable.value = mutable.value.copy(isAppendingDay = true, appendDayError = null)
        viewModelScope.launch {
            try {
                val newDayId = tripService.appendTripDay(tripId)
                completeAppendService(request, newDayId)
            } catch (failure: CancellationException) {
                if (pendingAppendDay?.generation == request.generation) {
                    pendingAppendDay = null
                    mutable.value = mutable.value.copy(isAppendingDay = false, appendDayError = null)
                    mustRestoreTripObservationGeneration = tripObservationGeneration
                    ensureTripObservation()
                }
                throw failure
            } catch (failure: Throwable) {
                if (pendingAppendDay?.generation == request.generation) {
                    pendingAppendDay = null
                    mutable.value = mutable.value.copy(
                        isAppendingDay = false,
                        appendDayError = failure.message ?: "新增旅行日失败",
                    )
                    mustRestoreTripObservationGeneration = tripObservationGeneration
                    ensureTripObservation()
                }
            }
        }
    }

    private fun reconcileAppendDayCompletion(days: List<TripDay>) {
        val pending = pendingAppendDay ?: return
        if (!pending.serviceCompleted) return
        if (days.none { it.id == pending.expectedNewDayId }) return
        mustRestoreTripObservationGeneration = tripObservationGeneration
        pendingAppendDay = null
        mutable.value = mutable.value.copy(
            isAppendingDay = false,
            appendDayError = null,
            appendDayCompletionToken = ++nextAppendDayCompletionToken,
        )
    }

    fun consumeAppendDayCompletion(token: Long) {
        if (mutable.value.appendDayCompletionToken == token) {
            mutable.value = mutable.value.copy(appendDayCompletionToken = null)
        }
    }

    fun previewMove(itemId: String, target: Int) {
        nextMoveGeneration++
        val order = mutable.value.previewOrder.toMutableList()
        val old = order.indexOf(itemId)
        if (old < 0 || target !in order.indices) return
        order.add(target, order.removeAt(old))
        val visibleLegs = visibleRouteLegs(mutable.value.items, order, mutable.value.legs)
        val editor = mutable.value.modeEditor?.takeIf { draft -> visibleLegs.any { it.id == draft.legId } }
        mutable.value = mutable.value.copy(previewOrder = order, modeEditor = editor)
    }

    fun commitMove(itemId: String, target: Int) {
        val day = state.value.selectedDayId ?: return
        val generation = ++nextMoveGeneration
        viewModelScope.launch {
            try {
                itineraries.moveItem(itemId, day, target)
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: Throwable) {
                if (generation == nextMoveGeneration && state.value.selectedDayId == day) {
                    mutable.value = mutable.value.copy(previewOrder = mutable.value.items.map(ItineraryItemUi::id))
                    showError(failure)
                }
            }
        }
    }

    fun requestCrossDay(itemId: String) {
        if (state.value.items.none { it.id == itemId }) return
        mutable.value = mutable.value.copy(
            crossDayMove = CrossDayMoveDraft(itemId = itemId, generation = ++nextMoveGeneration),
        )
    }

    fun moveToDay(dayId: String) {
        val draft = state.value.crossDayMove ?: return
        if (draft.isMoving) return
        val started = draft.copy(targetDayId = dayId, isMoving = true, moveError = null)
        mutable.value = mutable.value.copy(crossDayMove = started)
        viewModelScope.launch {
            try {
                itineraries.moveItem(started.itemId, dayId, 0)
                if (mutable.value.crossDayMove.matches(started)) {
                    mutable.value = mutable.value.copy(crossDayMove = null)
                }
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: Throwable) {
                val current = mutable.value.crossDayMove
                if (current.matches(started)) {
                    mutable.value = mutable.value.copy(
                        crossDayMove = current?.copy(
                            isMoving = false,
                            moveError = failure.message ?: "移动失败",
                        ),
                    )
                }
            }
        }
    }

    fun requestDelete(itemId: String): Boolean {
        val item = state.value.items.firstOrNull { it.id == itemId } ?: return false
        mutable.value = mutable.value.copy(
            deleteConfirmation = ItineraryDeleteConfirmation(
                itemId = itemId,
                placeName = item.name,
                generation = ++nextDeleteGeneration,
            ),
        )
        return true
    }

    fun confirmDelete() {
        val confirmation = state.value.deleteConfirmation ?: return
        if (confirmation.isDeleting) return
        mutable.value = mutable.value.copy(
            deleteConfirmation = confirmation.copy(isDeleting = true, deleteError = null),
        )
        viewModelScope.launch {
            try {
                itineraries.deleteItem(confirmation.itemId)
                if (mutable.value.deleteConfirmation.matches(confirmation)) {
                    mutable.value = mutable.value.copy(deleteConfirmation = null)
                }
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: Throwable) {
                val current = mutable.value.deleteConfirmation
                if (current.matches(confirmation)) {
                    mutable.value = mutable.value.copy(
                        deleteConfirmation = current?.copy(
                            isDeleting = false,
                            deleteError = failure.message ?: "删除失败",
                        ),
                    )
                }
            }
        }
    }

    fun requestTiming(itemId: String): Boolean {
        val item = state.value.items.firstOrNull { it.id == itemId } ?: return false
        mutable.value = mutable.value.copy(
            editDraft = ItineraryEditDraft(
                itemId = itemId,
                arrivalTimeText = item.arrivalTime?.toString().orEmpty(),
                stayMinutesText = item.stayMinutes?.toString().orEmpty(),
                noteText = item.note.orEmpty(),
                generation = ++nextEditGeneration,
            ),
        )
        return true
    }

    fun updateArrivalTime(value: String) {
        val draft = mutable.value.editDraft ?: return
        mutable.value = mutable.value.copy(editDraft = draft.copy(arrivalTimeText = value, saveError = null))
    }

    fun updateStayMinutes(value: String) {
        val draft = mutable.value.editDraft ?: return
        mutable.value = mutable.value.copy(editDraft = draft.copy(stayMinutesText = value.filter(Char::isDigit), saveError = null))
    }

    fun updateNote(value: String) {
        val draft = mutable.value.editDraft ?: return
        mutable.value = mutable.value.copy(editDraft = draft.copy(noteText = value, saveError = null))
    }

    fun dismissEditSaveError() {
        val draft = mutable.value.editDraft ?: return
        if (draft.isSaving || draft.saveError == null) return
        mutable.value = mutable.value.copy(editDraft = draft.copy(saveError = null))
    }

    fun saveTiming() {
        val draft = mutable.value.editDraft ?: return
        if (draft.isSaving || !draft.isValid) return
        mutable.value = mutable.value.copy(editDraft = draft.copy(isSaving = true, saveError = null))
        viewModelScope.launch {
            try {
                itineraries.updateDetails(draft.itemId, draft.arrivalTime, draft.stayMinutes, draft.noteText.trim().ifEmpty { null })
                if (mutable.value.editDraft.matches(draft)) {
                    mutable.value = mutable.value.copy(editDraft = null)
                }
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: Throwable) {
                val current = mutable.value.editDraft
                if (current.matches(draft)) {
                    mutable.value = mutable.value.copy(
                        editDraft = current?.copy(
                            isSaving = false,
                            saveError = failure.message ?: "保存失败",
                        ),
                    )
                }
            }
        }
    }

    fun setRouteCoordinator(value: RouteRefreshCoordinator?) { coordinator = value }

    fun requestMode(legId: String): Boolean {
        val leg = visibleRouteLegs(state.value.items, state.value.previewOrder, state.value.legs)
            .firstOrNull { it.id == legId }
            ?.takeIf { it.state is RouteLegUiState.Ready }
            ?: return false
        mutable.value = mutable.value.copy(
            modeEditor = RouteModeEditDraft(
                legId = legId,
                selectedMode = leg.mode,
                selectedModeOverride = leg.selectedModeOverride,
                originalSelectedModeOverride = leg.selectedModeOverride,
                durationMinutesText = leg.durationOverrideSeconds?.takeIf { it > 0 }?.div(60)?.toString().orEmpty(),
                noteText = leg.note.orEmpty(),
                plannedDurationSeconds = leg.durationSeconds,
                originalDurationOverrideSeconds = leg.durationOverrideSeconds?.takeIf { it > 0 },
                generation = ++nextModeGeneration,
            ),
        )
        return true
    }

    fun selectMode(mode: TransportMode) {
        val editor = mutable.value.modeEditor ?: return
        if (editor.isSaving) return
        mutable.value = mutable.value.copy(
            modeEditor = editor.copy(selectedMode = mode, selectedModeOverride = mode, saveError = null),
        )
    }

    fun clearSelectedModeOverride() {
        val editor = mutable.value.modeEditor ?: return
        if (editor.isSaving) return
        mutable.value = mutable.value.copy(modeEditor = editor.copy(selectedModeOverride = null, saveError = null))
    }

    fun updateRouteDurationMinutes(value: String) {
        val editor = mutable.value.modeEditor ?: return
        if (editor.isSaving) return
        mutable.value = mutable.value.copy(modeEditor = editor.copy(durationMinutesText = value.filter(Char::isDigit), isDurationEdited = true, saveError = null))
    }

    fun updateRouteNote(value: String) {
        val editor = mutable.value.modeEditor ?: return
        if (editor.isSaving) return
        mutable.value = mutable.value.copy(modeEditor = editor.copy(noteText = value, saveError = null))
    }

    fun saveRouteEditor() {
        val editor = state.value.modeEditor ?: return
        if (editor.isSaving || !editor.isValid) return
        val isReady = visibleRouteLegs(state.value.items, state.value.previewOrder, state.value.legs)
            .any { it.id == editor.legId && it.state is RouteLegUiState.Ready }
        if (!isReady) {
            mutable.value = mutable.value.copy(modeEditor = null)
            return
        }
        val started = editor.copy(isSaving = true, saveError = null)
        mutable.value = mutable.value.copy(modeEditor = started)
        viewModelScope.launch {
            val isStillReady = visibleRouteLegs(state.value.items, state.value.previewOrder, state.value.legs)
                .any { it.id == started.legId && it.state is RouteLegUiState.Ready }
            if (!isStillReady) {
                if (mutable.value.modeEditor.matches(started)) {
                    mutable.value = mutable.value.copy(modeEditor = null)
                }
                return@launch
            }
            try {
                val modeChanged = started.selectedModeOverride != started.originalSelectedModeOverride
                val routeCoordinator = coordinator
                val saved = when {
                    routeCoordinator != null -> routeCoordinator.updateDetails(
                        started.legId,
                        started.selectedModeOverride,
                        started.durationOverrideSeconds,
                        started.noteText.trim().ifEmpty { null },
                    )
                    !modeChanged -> routeLegs.updateDetails(
                        started.legId,
                        started.selectedModeOverride,
                        started.durationOverrideSeconds,
                        started.noteText.trim().ifEmpty { null },
                        online = false,
                    )
                    else -> false
                }
                val current = mutable.value.modeEditor
                if (current.matches(started)) {
                    mutable.value = mutable.value.copy(
                        modeEditor = if (saved) null else current?.copy(
                            isSaving = false,
                            saveError = "联网并同意高德隐私政策后才能保存路段编辑",
                        ),
                    )
                }
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: Throwable) {
                val current = mutable.value.modeEditor
                if (current.matches(started)) {
                    mutable.value = mutable.value.copy(
                        modeEditor = current?.copy(
                            isSaving = false,
                            saveError = failure.message ?: "保存路段编辑失败",
                        ),
                    )
                }
            }
        }
    }

    private val retryingLegs = mutableSetOf<Pair<String, Long>>()

    fun retry(legId: String, expectedVersion: Long) {
        val current = visibleRouteLegs(state.value.items, state.value.previewOrder, state.value.legs)
            .firstOrNull { it.id == legId }
            ?: return
        if (
            current.status != com.yangchengwei.easytrip.core.model.RouteStatus.FAILED ||
            expectedVersion != current.version
        ) return
        val retryKey = legId to expectedVersion
        if (!retryingLegs.add(retryKey)) return
        viewModelScope.launch {
            try {
                val retried = coordinator?.retry(legId, expectedVersion) ?: false
                if (!retried) showError(IllegalStateException("联网并同意高德隐私政策后才能重试"))
            } catch (error: Throwable) {
                showError(error)
            } finally {
                retryingLegs.remove(retryKey)
            }
        }
    }

    fun dismissDialogs() {
        if (
            mutable.value.editDraft?.isSaving == true ||
            mutable.value.crossDayMove?.isMoving == true ||
            mutable.value.deleteConfirmation?.isDeleting == true ||
            mutable.value.modeEditor?.isSaving == true
        ) return
        mutable.value = mutable.value.copy(
            editDraft = null,
            crossDayMove = null,
            deleteConfirmation = null,
            modeEditor = null,
        )
    }

    fun dispatch(action: DayItineraryAction) {
        when (action) {
            DayItineraryAction.AppendTripDay -> appendTripDay()
            DayItineraryAction.AddPlaces -> Unit
            is DayItineraryAction.AddPlace -> addPlace(action.placeId)
            is DayItineraryAction.PreviewMove -> previewMove(action.itemId, action.target)
            is DayItineraryAction.CommitMove -> commitMove(action.itemId, action.target)
            is DayItineraryAction.RequestTiming -> requestTiming(action.itemId)
            is DayItineraryAction.RequestCrossDay -> requestCrossDay(action.itemId)
            is DayItineraryAction.RequestDelete -> requestDelete(action.itemId)
            is DayItineraryAction.RequestMode -> requestMode(action.legId)
            is DayItineraryAction.Retry -> retry(action.legId, action.expectedVersion)
            is DayItineraryAction.MoveToDay -> moveToDay(action.dayId)
            is DayItineraryAction.UpdateArrivalTime -> updateArrivalTime(action.value)
            is DayItineraryAction.UpdateStayMinutes -> updateStayMinutes(action.value)
            is DayItineraryAction.UpdateNote -> updateNote(action.value)
            DayItineraryAction.SaveEdit -> saveTiming()
            DayItineraryAction.DismissEditSaveError -> dismissEditSaveError()
            is DayItineraryAction.SelectMode -> selectMode(action.mode)
            DayItineraryAction.ClearSelectedModeOverride -> clearSelectedModeOverride()
            is DayItineraryAction.UpdateRouteDurationMinutes -> updateRouteDurationMinutes(action.value)
            is DayItineraryAction.UpdateRouteNote -> updateRouteNote(action.value)
            DayItineraryAction.SaveMode -> saveRouteEditor()
            DayItineraryAction.ConfirmDelete -> confirmDelete()
            DayItineraryAction.DismissDialogs -> dismissDialogs()
        }
    }

    private fun clearDay(selectedDayId: String? = null) {
        mutable.value = mutable.value.copy(
            selectedDayId = selectedDayId,
            items = emptyList(),
            legs = emptyList(),
            previewOrder = emptyList(),
            editDraft = null,
            crossDayMove = null,
            deleteConfirmation = null,
            modeEditor = null,
        )
    }

    private fun applyDay(day: DayItinerary, legs: List<RouteLegEntity>) {
        val items = day.items.map { it.toItineraryItemUi() }
        val previous = mutable.value
        val activeEdit = previous.editDraft?.takeIf { draft -> items.any { it.id == draft.itemId } }
        val officialOrder = items.map(ItineraryItemUi::id)
        val keepPreview = previous.items.map(ItineraryItemUi::id) == officialOrder && previous.previewOrder.toSet() == officialOrder.toSet()
        val previewOrder = if (keepPreview) previous.previewOrder else officialOrder
        val rawLegs = legs.map { it.toRouteLegUi() }
        val activeRouteEditor = previous.modeEditor?.takeIf { draft ->
            visibleRouteLegs(items, previewOrder, rawLegs)
                .any { it.id == draft.legId && it.state is RouteLegUiState.Ready }
        }
        mutable.value = previous.copy(
            items = items,
            previewOrder = previewOrder,
            legs = rawLegs,
            editDraft = activeEdit,
            modeEditor = activeRouteEditor,
        )
    }

    private fun ItineraryEditDraft?.matches(started: ItineraryEditDraft): Boolean =
        this?.itemId == started.itemId && this.generation == started.generation

    private fun ItineraryDeleteConfirmation?.matches(started: ItineraryDeleteConfirmation): Boolean =
        this?.itemId == started.itemId && this.generation == started.generation

    private fun CrossDayMoveDraft?.matches(started: CrossDayMoveDraft): Boolean =
        this?.itemId == started.itemId && this.generation == started.generation

    private fun RouteModeEditDraft?.matches(started: RouteModeEditDraft): Boolean =
        this?.legId == started.legId && this.generation == started.generation

    private fun showError(t: Throwable) { mutable.value = mutable.value.copy(error = t.message ?: "操作失败") }

    class Factory(
        private val tripId: String,
        private val trips: TripRepository,
        private val itineraries: ItineraryRepository,
        private val routeLegs: RouteLegRepository,
        private val coordinator: RouteRefreshCoordinator?,
        private val savedPlaces: Flow<List<SavedPlace>>,
        private val selectedDays: Flow<String?> = emptyFlow(),
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = DayItineraryViewModel(tripId, trips, itineraries, routeLegs, coordinator, savedPlaces, selectedDays) as T
    }
}
