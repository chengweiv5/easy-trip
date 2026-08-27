package com.yangchengwei.easytrip.trip.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.trip.domain.TripService
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class CreateTripViewModel(
    private val service: TripService,
    private val savedState: SavedStateHandle = SavedStateHandle(),
    private val requestIdFactory: () -> String = { UUID.randomUUID().toString() },
) : ViewModel() {
    private val mutableState = MutableStateFlow(savedCreateState())
    val state: StateFlow<CreateTripUiState> = mutableState.asStateFlow()
    private val effectChannel = Channel<CreateTripEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()
    private var submitJob: Job? = null
    private var submitGeneration = 0L

    fun onAction(action: CreateTripAction) {
        val current = mutableState.value
        if (current.isSubmitting) return
        when (action) {
            CreateTripAction.Back -> viewModelScope.launch { effectChannel.send(CreateTripEffect.NavigateBack) }
            is CreateTripAction.NameChanged -> updateForCommandChange(current, current.copy(name = action.value, nameError = null, submitError = null))
            is CreateTripAction.DayCountChanged -> updateForCommandChange(current, current.copy(dayCount = action.value.filter(Char::isDigit), dayCountError = null, submitError = null))
            is CreateTripAction.TimeModeChanged -> updateForCommandChange(current, current.copy(timeMode = action.value, startDate = if (action.value == CreateTimeMode.DRAFT) null else current.startDate, dateError = null))
            is CreateTripAction.StartDateChanged -> if (action.value != null) updateForCommandChange(current, current.copy(startDate = action.value, dateError = null))
            is CreateTripAction.TravelModeChanged -> updateForCommandChange(current, current.copy(travelMode = action.value))
            CreateTripAction.Submit -> submit(current)
        }
    }

    private fun submit(current: CreateTripUiState) {
        if (submitJob?.isActive == true) return
        val validation = validateCreateTrip(current)
        val valid = validation.valid
        if (valid == null) {
            update(current.copy(nameError = validation.nameError, dayCountError = validation.dayCountError, dateError = validation.dateError))
            return
        }
        val requestId = current.requestId ?: requestIdFactory()
        val submitted = current.copy(
            isSubmitting = true,
            submitError = null,
            nameError = null,
            dayCountError = null,
            dateError = null,
            requestId = requestId,
        )
        update(submitted)
        val command = valid.command.copy(requestId = requestId)
        val generation = ++submitGeneration
        submitJob = viewModelScope.launch {
            try {
                val tripId = service.createTrip(command)
                if (isCurrentSubmission(generation, requestId)) {
                    clear()
                    effectChannel.send(CreateTripEffect.OpenWorkspace(tripId))
                }
            } catch (failure: CancellationException) {
                throw failure
            } catch (_: Throwable) {
                if (isCurrentSubmission(generation, requestId)) {
                    update(mutableState.value.copy(isSubmitting = false, submitError = "创建旅行失败，请重试"))
                }
            }
        }
    }

    private fun isCurrentSubmission(generation: Long, requestId: String): Boolean {
        val current = mutableState.value
        return generation == submitGeneration && current.isSubmitting && current.requestId == requestId
    }

    private fun updateForCommandChange(current: CreateTripUiState, value: CreateTripUiState) {
        update(if (sameCommand(current, value)) value else value.copy(requestId = null))
    }

    private fun sameCommand(left: CreateTripUiState, right: CreateTripUiState) =
        left.name == right.name &&
            left.dayCount == right.dayCount &&
            left.timeMode == right.timeMode &&
            left.startDate == right.startDate &&
            left.travelMode == right.travelMode

    private fun update(value: CreateTripUiState) {
        mutableState.value = value
        savedState[CREATE_NAME] = value.name
        savedState[CREATE_DAYS] = value.dayCount
        savedState[CREATE_TIME_MODE] = value.timeMode.name
        savedState[CREATE_START_DATE] = value.startDate?.toString()
        savedState[CREATE_TRAVEL_MODE] = value.travelMode.name
        savedState[CREATE_REQUEST_ID] = value.requestId
    }

    private fun clear() = update(CreateTripUiState())

    override fun onCleared() {
        submitGeneration++
        super.onCleared()
    }

    private fun savedCreateState() = CreateTripUiState(
        name = savedState[CREATE_NAME] ?: "",
        dayCount = savedState[CREATE_DAYS] ?: "",
        timeMode = savedState.get<String>(CREATE_TIME_MODE)?.let { saved -> CreateTimeMode.entries.firstOrNull { it.name == saved } } ?: CreateTimeMode.DRAFT,
        startDate = savedState.get<String>(CREATE_START_DATE)?.let(LocalDate::parse),
        travelMode = savedState.get<String>(CREATE_TRAVEL_MODE)?.let { saved -> TravelMode.entries.firstOrNull { it.name == saved } } ?: TravelMode.FLEXIBLE,
        requestId = savedState[CREATE_REQUEST_ID],
    )

    class Factory(private val service: TripService) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
            CreateTripViewModel(service, extras.createSavedStateHandle()) as T
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
