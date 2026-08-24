package com.yangchengwei.easytrip.trip.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.trip.domain.DayDeletion
import com.yangchengwei.easytrip.trip.domain.TripDateRangeService
import com.yangchengwei.easytrip.trip.domain.TripRepository
import com.yangchengwei.easytrip.trip.domain.TripService
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

data class TripDeleteImpact(val days: Int, val places: Int, val tags: Int, val itineraryItems: Int, val routeLegs: Int)
data class DayDeleteImpact(val itineraryItems: Int, val routeLegs: Int, val retainedSavedPlaces: Int)
interface DeleteImpactProvider {
    suspend fun trip(tripId: String): TripDeleteImpact
    suspend fun day(dayId: String): DayDeleteImpact
}

data class DayUi(val id: String, val label: String)
data class PendingDayDeletion(val day: DayUi, val impact: DayDeleteImpact)
data class TripSettingsUiState(
    val tripId: String,
    val name: String = "",
    val startDate: LocalDate? = null,
    val travelMode: TravelMode = TravelMode.FLEXIBLE,
    val days: List<DayUi> = emptyList(),
    val dateRange: DateRangeChangeUiState = DateRangeChangeUiState(),
    val pendingDayDeletion: PendingDayDeletion? = null,
    val dayDeleteInProgress: Boolean = false,
    val dayDeleteError: String? = null,
)

class TripSettingsViewModel(
    savedStateHandle: SavedStateHandle,
    private val service: TripService,
    repository: TripRepository,
    private val impacts: DeleteImpactProvider,
    private val dateRanges: TripDateRangeService,
) : ViewModel() {
    private val tripId: String = checkNotNull(savedStateHandle["tripId"])
    private val mutableState = MutableStateFlow(TripSettingsUiState(tripId))
    val state: StateFlow<TripSettingsUiState> = mutableState.asStateFlow()
    private var previewJob: Job? = null
    private var dateRangeGeneration = 0L
    private var deleteImpactJob: Job? = null
    private var deleteGeneration = 0L

    init {
        viewModelScope.launch {
            repository.observeTrip(tripId).filterNotNull().collect { trip ->
                mutableState.value = mutableState.value.copy(
                    name = trip.name,
                    startDate = trip.startDate,
                    travelMode = trip.travelMode,
                    days = trip.days.map { DayUi(it.id, service.displayLabel(it, trip.startDate)) },
                    dateRange = mutableState.value.dateRange.takeIf { it.confirmation != null || it.error != null }
                        ?: DateRangeChangeUiState(
                            startDate = trip.startDate,
                            endDate = trip.startDate?.plusDays((trip.days.size - 1).toLong()),
                        ),
                )
            }
        }
    }

    fun rename(name: String) { viewModelScope.launch { service.renameTrip(tripId, name) } }
    fun setTravelMode(value: TravelMode) { viewModelScope.launch { service.setTravelMode(tripId, value) } }

    fun updateDateDraft(startDate: LocalDate?, endDate: LocalDate?) {
        dateRangeGeneration++
        previewJob?.cancel()
        mutableState.value = mutableState.value.copy(
            dateRange = DateRangeChangeUiState(startDate = startDate, endDate = endDate),
        )
    }

    fun requestDateRangeChange() {
        val draft = mutableState.value.dateRange
        if (draft.submitting || previewJob?.isActive == true) return
        val generation = ++dateRangeGeneration
        previewJob = viewModelScope.launch {
            try {
                val impact = dateRanges.preview(tripId, draft.startDate, draft.endDate)
                if (generation != dateRangeGeneration) return@launch
                if (impact.deletedDayIds.isEmpty()) {
                    mutableState.value = mutableState.value.copy(dateRange = draft.copy(submitting = true, error = null))
                    applyDateRange(impact, generation)
                } else {
                    mutableState.value = mutableState.value.copy(
                        dateRange = draft.copy(confirmation = impact, error = null),
                    )
                }
            } catch (_: CancellationException) {
                throw CancellationException()
            } catch (_: IllegalArgumentException) {
                if (generation == dateRangeGeneration) {
                    val error = if (draft.startDate != null && draft.endDate != null) {
                        "结束日期不能早于开始日期"
                    } else {
                        "开始和结束日期必须同时填写"
                    }
                    mutableState.value = mutableState.value.copy(dateRange = draft.copy(error = error))
                }
            } catch (_: Throwable) {
                if (generation == dateRangeGeneration) {
                    mutableState.value = mutableState.value.copy(
                        dateRange = draft.copy(error = "无法检查日期范围，请重试"),
                    )
                }
            }
        }
    }

    fun cancelDateRangeChange() {
        if (mutableState.value.dateRange.submitting) return
        mutableState.value = mutableState.value.copy(
            dateRange = mutableState.value.dateRange.copy(confirmation = null, error = null),
        )
    }

    fun confirmDateRangeChange() {
        val range = mutableState.value.dateRange
        val impact = range.confirmation ?: return
        if (range.submitting) return
        mutableState.value = mutableState.value.copy(dateRange = range.copy(submitting = true, error = null))
        val generation = ++dateRangeGeneration
        viewModelScope.launch {
            if (range.error == null) {
                applyDateRange(impact, generation)
                return@launch
            }
            try {
                val refreshed = dateRanges.preview(tripId, range.startDate, range.endDate)
                if (generation != dateRangeGeneration) return@launch
                if (refreshed.deletedDayIds.isEmpty()) {
                    applyDateRange(refreshed, generation)
                } else {
                    mutableState.value = mutableState.value.copy(
                        dateRange = mutableState.value.dateRange.copy(
                            confirmation = refreshed,
                            submitting = false,
                            error = null,
                        ),
                    )
                }
            } catch (_: Throwable) {
                if (generation == dateRangeGeneration) {
                    mutableState.value = mutableState.value.copy(
                        dateRange = mutableState.value.dateRange.copy(submitting = false, error = "无法重新计算影响，请重试"),
                    )
                }
            }
        }
    }

    private suspend fun applyDateRange(
        impact: com.yangchengwei.easytrip.trip.domain.DateRangeChangeImpact,
        generation: Long,
    ) {
        try {
            dateRanges.apply(impact, tripId)
            if (generation == dateRangeGeneration) {
                mutableState.value = mutableState.value.copy(
                    dateRange = DateRangeChangeUiState(impact.newStartDate, impact.newEndDate),
                )
            }
        } catch (_: Throwable) {
            if (generation == dateRangeGeneration) {
                mutableState.value = mutableState.value.copy(
                    dateRange = mutableState.value.dateRange.copy(submitting = false, error = "保存失败，请重新计算影响"),
                )
            }
        }
    }

    fun requestDelete(day: DayUi) {
        if (mutableState.value.days.size <= 1) return
        val generation = ++deleteGeneration
        deleteImpactJob?.cancel()
        deleteImpactJob = viewModelScope.launch {
            try {
                val impact = impacts.day(day.id)
                if (generation == deleteGeneration) {
                    mutableState.value = mutableState.value.copy(
                        pendingDayDeletion = PendingDayDeletion(day, impact),
                        dayDeleteError = null,
                    )
                }
            } catch (_: CancellationException) {
                throw CancellationException()
            } catch (_: Throwable) {
                if (generation == deleteGeneration) {
                    mutableState.value = mutableState.value.copy(dayDeleteError = "无法检查删除影响，请重试")
                }
            }
        }
    }
    fun cancelDelete() {
        if (!mutableState.value.dayDeleteInProgress) {
            deleteGeneration++
            deleteImpactJob?.cancel()
            mutableState.value = mutableState.value.copy(pendingDayDeletion = null, dayDeleteError = null)
        }
    }
    fun confirmDelete() {
        val state = mutableState.value
        val pending = state.pendingDayDeletion ?: return
        val day = pending.day
        if (state.dayDeleteInProgress) return
        mutableState.value = state.copy(dayDeleteInProgress = true, dayDeleteError = null)
        viewModelScope.launch {
            try {
                service.deleteDay(
                    DayDeletion(
                        day.id,
                        pending.impact.itineraryItems,
                        pending.impact.routeLegs,
                    ),
                )
                mutableState.value = mutableState.value.copy(
                    pendingDayDeletion = null,
                    dayDeleteInProgress = false,
                    dayDeleteError = null,
                )
            } catch (_: Throwable) {
                mutableState.value = mutableState.value.copy(
                    dayDeleteInProgress = false,
                    dayDeleteError = "删除失败，请重试",
                )
            }
        }
    }

    class Factory(
        private val service: TripService,
        private val repository: TripRepository,
        private val impacts: DeleteImpactProvider,
        private val dateRanges: TripDateRangeService = TripDateRangeService(repository),
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: androidx.lifecycle.viewmodel.CreationExtras): T =
            TripSettingsViewModel(extras.createSavedStateHandle(), service, repository, impacts, dateRanges) as T
    }
}
