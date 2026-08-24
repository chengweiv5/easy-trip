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
    private var nextEditGeneration = 0L
    private var nextDeleteGeneration = 0L
    private var nextMoveGeneration = 0L
    private var nextModeGeneration = 0L
    val state: StateFlow<DayItineraryUiState> = mutable.asStateFlow()

    init {
        viewModelScope.launch {
            trips.observeTrip(tripId).filterNotNull().collect { trip ->
                val chosen = if (hasExternalSelection) {
                    externalSelectedDayId?.takeIf { id -> trip.days.any { it.id == id } }
                } else {
                    selectedDay.value?.takeIf { id -> trip.days.any { it.id == id } } ?: trip.days.firstOrNull()?.id
                }
                selectedDay.value = chosen
                mutable.value = mutable.value.copy(days = trip.days, selectedDayId = chosen)
                if (chosen == null) clearDay()
            }
        }
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

    fun selectDay(id: String?) {
        if (id == selectedDay.value) return
        selectedDay.value = id
        clearDay(id)
    }

    fun addPlace(placeId: String) {
        val day = state.value.selectedDayId ?: return
        val index = state.value.items.size
        viewModelScope.launch { runCatching { itineraries.addItem(day, placeId, index) }.onFailure(::showError) }
    }

    fun appendTripDay() {
        if (mutable.value.isAppendingDay || mutable.value.appendDayCompletionToken != null) return
        mutable.value = mutable.value.copy(isAppendingDay = true, appendDayError = null)
        viewModelScope.launch {
            runCatching { tripService.appendTripDay(tripId) }
                .onSuccess {
                    mutable.value = mutable.value.copy(
                        isAppendingDay = false,
                        appendDayCompletionToken = ++nextAppendDayCompletionToken,
                    )
                }
                .onFailure {
                    mutable.value = mutable.value.copy(
                        isAppendingDay = false,
                        appendDayError = it.message ?: "新增旅行日失败",
                    )
                }
        }
    }

    fun consumeAppendDayCompletion(token: Long) {
        if (mutable.value.appendDayCompletionToken == token) {
            mutable.value = mutable.value.copy(appendDayCompletionToken = null)
        }
    }

    fun previewMove(itemId: String, target: Int) {
        val order = mutable.value.previewOrder.toMutableList()
        val old = order.indexOf(itemId)
        if (old < 0 || target !in order.indices) return
        order.add(target, order.removeAt(old))
        mutable.value = mutable.value.copy(previewOrder = order)
    }

    fun commitMove(itemId: String, target: Int) {
        val day = state.value.selectedDayId ?: return
        viewModelScope.launch {
            runCatching { itineraries.moveItem(itemId, day, target) }
                .onFailure { mutable.value = mutable.value.copy(previewOrder = mutable.value.items.map(ItineraryItemUi::id)); showError(it) }
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

    fun saveTiming() {
        val draft = mutable.value.editDraft ?: return
        if (draft.isSaving || !draft.isValid) return
        mutable.value = mutable.value.copy(editDraft = draft.copy(isSaving = true, saveError = null))
        viewModelScope.launch {
            try {
                itineraries.updateTiming(draft.itemId, draft.arrivalTime, draft.stayMinutes)
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
        val leg = state.value.legs.firstOrNull { it.id == legId } ?: return false
        mutable.value = mutable.value.copy(
            modeEditor = RouteModeEditDraft(
                legId = legId,
                selectedMode = leg.mode,
                generation = ++nextModeGeneration,
            ),
        )
        return true
    }

    fun selectMode(mode: TransportMode) {
        val editor = mutable.value.modeEditor ?: return
        if (editor.isSaving) return
        mutable.value = mutable.value.copy(modeEditor = editor.copy(selectedMode = mode, saveError = null))
    }

    fun overrideMode() {
        val editor = state.value.modeEditor ?: return
        if (editor.isSaving) return
        val started = editor.copy(isSaving = true, saveError = null)
        mutable.value = mutable.value.copy(modeEditor = started)
        viewModelScope.launch {
            try {
                val saved = coordinator?.overrideMode(started.legId, started.selectedMode) ?: false
                val current = mutable.value.modeEditor
                if (current.matches(started)) {
                    mutable.value = mutable.value.copy(
                        modeEditor = if (saved) null else current?.copy(
                            isSaving = false,
                            saveError = "联网并同意高德隐私政策后才能更新交通方式",
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
                            saveError = failure.message ?: "更新交通方式失败",
                        ),
                    )
                }
            }
        }
    }

    fun retry(legId: String) {
        viewModelScope.launch {
            runCatching { coordinator?.retry(legId) ?: false }
                .onSuccess { if (!it) showError(IllegalStateException("联网并同意高德隐私政策后才能重试")) }
                .onFailure(::showError)
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
            is DayItineraryAction.Retry -> retry(action.legId)
            is DayItineraryAction.MoveToDay -> moveToDay(action.dayId)
            is DayItineraryAction.UpdateArrivalTime -> updateArrivalTime(action.value)
            is DayItineraryAction.UpdateStayMinutes -> updateStayMinutes(action.value)
            DayItineraryAction.SaveEdit -> saveTiming()
            is DayItineraryAction.SelectMode -> selectMode(action.mode)
            DayItineraryAction.SaveMode -> overrideMode()
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
        val officialOrder = items.map(ItineraryItemUi::id)
        val keepPreview = previous.items.map(ItineraryItemUi::id) == officialOrder && previous.previewOrder.toSet() == officialOrder.toSet()
        mutable.value = previous.copy(
            items = items,
            previewOrder = if (keepPreview) previous.previewOrder else officialOrder,
            legs = legs.map { it.toRouteLegUi() },
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
