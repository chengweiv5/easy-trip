package com.yangchengwei.easytrip.trip.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewModelScope
import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.trip.domain.TripRepository
import com.yangchengwei.easytrip.trip.domain.TripService
import com.yangchengwei.easytrip.trip.domain.TripSummary
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

enum class CreateTimeMode { DRAFT, DATED }

data class TripListUiState(
    val page: TripListPageState = TripListPageState.Loading,
    val trips: List<TripSummary> = emptyList(),
    val create: CreateTripUiState = CreateTripUiState(),
    val pendingDelete: TripSummary? = null,
    val pendingDeleteImpact: TripDeleteImpact? = null,
) {
    val showCreateDialog: Boolean get() = create.visible
}

sealed interface TripListNavigation {
    data class OpenWorkspace(val tripId: String) : TripListNavigation
    data class OpenSettings(val tripId: String) : TripListNavigation
}

class TripListViewModel(
    private val service: TripService,
    private val repository: TripRepository,
    private val impacts: DeleteImpactProvider,
    private val savedState: SavedStateHandle = SavedStateHandle(),
    private val requestIdFactory: () -> String = { UUID.randomUUID().toString() },
) : ViewModel() {
    private val mutableState = MutableStateFlow(
        TripListUiState(create = savedCreateState()),
    )
    val state: StateFlow<TripListUiState> = mutableState.asStateFlow()
    private val navigationChannel = Channel<TripListNavigation>(Channel.BUFFERED)
    val navigation = navigationChannel.receiveAsFlow()
    private var tripsJob: Job? = null
    private var submitJob: Job? = null

    init { observeTrips() }

    fun observeTrips() {
        tripsJob?.cancel()
        mutableState.value = mutableState.value.copy(page = TripListPageState.Loading)
        tripsJob = viewModelScope.launch {
            repository.observeTrips().catch {
                mutableState.value = mutableState.value.copy(page = TripListPageState.Error("无法加载旅行"))
            }.collect { trips ->
                mutableState.value = mutableState.value.copy(
                    trips = trips,
                    page = if (trips.isEmpty()) TripListPageState.Empty else TripListPageState.Content(trips.map(TripSummary::toTripCardUiModel)),
                )
            }
        }
    }

    fun onAction(action: TripListAction) {
        when (action) {
            TripListAction.CreateTrip -> showCreate()
            TripListAction.Retry -> observeTrips()
            is TripListAction.OpenTrip -> openWorkspace(action.tripId)
            is TripListAction.OpenSettings -> openSettings(action.tripId)
            is TripListAction.RequestDelete -> mutableState.value.trips.firstOrNull { it.id == action.tripId }?.let(::requestDelete)
        }
    }

    fun onCreateAction(action: CreateTripAction) {
        val current = mutableState.value.create
        val fieldsLocked = current.requestId != null
        val next = when (action) {
            CreateTripAction.Dismiss -> {
                clearCreate()
                return
            }
            is CreateTripAction.NameChanged -> if (fieldsLocked) current else current.copy(name = action.value, nameError = null, submitError = null)
            is CreateTripAction.DayCountChanged -> if (fieldsLocked) current else current.copy(dayCount = action.value.filter(Char::isDigit), dayCountError = null, submitError = null)
            is CreateTripAction.TimeModeChanged -> if (fieldsLocked) current else current.copy(timeMode = action.value, startDate = if (action.value == CreateTimeMode.DRAFT) null else current.startDate, dateError = null)
            is CreateTripAction.StartDateChanged -> if (fieldsLocked) current else current.copy(startDate = action.value, dateError = null)
            is CreateTripAction.TravelModeChanged -> if (fieldsLocked) current else current.copy(travelMode = action.value)
            CreateTripAction.Submit -> {
                submitCreate(current)
                return
            }
        }
        updateCreate(next)
    }

    private fun submitCreate(current: CreateTripUiState) {
        if (current.isSubmitting || submitJob?.isActive == true) return
        val requestId = current.requestId ?: requestIdFactory()
        if (current.requestId == null) {
            updateCreate(current.copy(requestId = requestId))
        }
        val submitted = mutableState.value.create
        val validation = validateCreateTrip(submitted)
        val valid = validation.valid
        if (valid == null) {
            updateCreate(submitted.copy(nameError = validation.nameError, dayCountError = validation.dayCountError, dateError = validation.dateError, submitError = null, requestId = null))
            return
        }
        updateCreate(submitted.copy(isSubmitting = true, submitError = null, nameError = null, dayCountError = null, dateError = null))
        submitJob = viewModelScope.launch {
            runCatching { service.createTrip(valid.command) }
                .onSuccess { id ->
                    clearCreate()
                    navigationChannel.send(TripListNavigation.OpenWorkspace(id))
                }
                .onFailure {
                    updateCreate(mutableState.value.create.copy(isSubmitting = false, submitError = "创建旅行失败，请重试"))
                }
        }
    }

    fun showCreate() { updateCreate(mutableState.value.create.copy(visible = true, submitError = null)) }
    fun dismissCreate() = onCreateAction(CreateTripAction.Dismiss)
    fun setCreateName(value: String) = onCreateAction(CreateTripAction.NameChanged(value))
    fun setCreateDays(value: String) = onCreateAction(CreateTripAction.DayCountChanged(value))
    fun setCreateTimeMode(value: CreateTimeMode) = onCreateAction(CreateTripAction.TimeModeChanged(value))
    fun setCreateStartDate(value: LocalDate) = onCreateAction(CreateTripAction.StartDateChanged(value))
    fun setCreateTravelMode(value: TravelMode) = onCreateAction(CreateTripAction.TravelModeChanged(value))
    fun create() = onCreateAction(CreateTripAction.Submit)

    private fun updateCreate(value: CreateTripUiState) {
        mutableState.value = mutableState.value.copy(create = value)
        savedState[CREATE_NAME] = value.name
        savedState[CREATE_DAYS] = value.dayCount
        savedState[CREATE_TIME_MODE] = value.timeMode.name
        savedState[CREATE_START_DATE] = value.startDate?.toString()
        savedState[CREATE_TRAVEL_MODE] = value.travelMode.name
        savedState[CREATE_REQUEST_ID] = value.requestId
    }

    private fun clearCreate() = updateCreate(CreateTripUiState())

    private fun savedCreateState(): CreateTripUiState = CreateTripUiState(
        name = savedState[CREATE_NAME] ?: "",
        dayCount = savedState[CREATE_DAYS] ?: "",
        timeMode = savedState.get<String>(CREATE_TIME_MODE)?.let { CreateTimeMode.entries.firstOrNull { mode -> mode.name == it } } ?: CreateTimeMode.DRAFT,
        startDate = savedState.get<String>(CREATE_START_DATE)?.let(LocalDate::parse),
        travelMode = savedState.get<String>(CREATE_TRAVEL_MODE)?.let { TravelMode.entries.firstOrNull { mode -> mode.name == it } } ?: TravelMode.FLEXIBLE,
        requestId = savedState[CREATE_REQUEST_ID],
    )

    fun openWorkspace(id: String) { viewModelScope.launch { navigationChannel.send(TripListNavigation.OpenWorkspace(id)) } }
    fun openSettings(id: String) { viewModelScope.launch { navigationChannel.send(TripListNavigation.OpenSettings(id)) } }
    fun requestDelete(value: TripSummary) { viewModelScope.launch { mutableState.value = mutableState.value.copy(pendingDelete = value, pendingDeleteImpact = impacts.trip(value.id)) } }
    fun cancelDelete() { mutableState.value = mutableState.value.copy(pendingDelete = null, pendingDeleteImpact = null) }
    fun confirmDelete() { val value = mutableState.value.pendingDelete ?: return; viewModelScope.launch { service.deleteTrip(value.id); cancelDelete() } }

    class Factory(
        private val service: TripService,
        private val repository: TripRepository,
        private val impacts: DeleteImpactProvider,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
            TripListViewModel(service, repository, impacts, extras.createSavedStateHandle()) as T
    }

    companion object {
        private const val CREATE_NAME = "trip.create.name"
        private const val CREATE_DAYS = "trip.create.days"
        private const val CREATE_TIME_MODE = "trip.create.timeMode"
        private const val CREATE_START_DATE = "trip.create.startDate"
        private const val CREATE_TRAVEL_MODE = "trip.create.travelMode"
        private const val CREATE_REQUEST_ID = "trip.create.requestId"
    }
}
