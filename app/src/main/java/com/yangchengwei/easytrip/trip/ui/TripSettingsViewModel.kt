package com.yangchengwei.easytrip.trip.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.trip.domain.DateRangeChangeImpact
import com.yangchengwei.easytrip.trip.domain.DateRangeSnapshotChangedException
import com.yangchengwei.easytrip.trip.domain.DayDeletion
import com.yangchengwei.easytrip.trip.domain.TripDateRangeService
import com.yangchengwei.easytrip.trip.domain.TripDay
import com.yangchengwei.easytrip.trip.domain.TripRepository
import com.yangchengwei.easytrip.trip.domain.TripService
import com.yangchengwei.easytrip.trip.domain.TripWithDays
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class TripDeleteImpact(val days: Int, val places: Int, val tags: Int, val itineraryItems: Int, val routeLegs: Int)
data class DayDeleteImpact(val itineraryItems: Int, val routeLegs: Int, val retainedSavedPlaces: Int)
interface DeleteImpactProvider {
    suspend fun trip(tripId: String): TripDeleteImpact
    suspend fun day(dayId: String): DayDeleteImpact
}

data class DayUi(val id: String, val label: String)
data class PendingDayDeletion(val day: DayUi, val impact: DayDeleteImpact)

internal fun tripMatchesDateRangeRequest(
    trip: TripWithDays,
    request: DateRangeChangeRequest,
    impact: DateRangeChangeImpact?,
): Boolean {
    if (trip.startDate != request.targetStartDate || impact == null) return false
    val actualDayIds = trip.days.map(TripDay::id)
    val targetDayCount = ChronoUnit.DAYS.between(request.targetStartDate, request.targetEndDate).toInt() + 1
    return when {
        targetDayCount < request.baselineDayIds.size -> actualDayIds == impact.retainedDayIds
        targetDayCount > request.baselineDayIds.size ->
            actualDayIds.size == targetDayCount && actualDayIds.take(request.baselineDayIds.size) == request.baselineDayIds
        else -> actualDayIds == request.baselineDayIds
    }
}
sealed interface TripSettingsEffect {
    data object ReturnToTripList : TripSettingsEffect
}
data class TripSettingsUiState(
    val tripId: String,
    val hasAuthoritativeTrip: Boolean = true,
    val name: String = "",
    val startDate: LocalDate? = null,
    val travelMode: TravelMode = TravelMode.FLEXIBLE,
    val days: List<DayUi> = emptyList(),
    val dateRange: DateRangeChangeUiState = DateRangeChangeUiState(),
    val tripDeletion: TripDeletionUiState = TripDeletionUiState.Idle,
    val pendingDayDeletion: PendingDayDeletion? = null,
    val dayDeletionRetry: DayUi? = null,
    val dayDeleteInProgress: Boolean = false,
    val dayDeleteError: String? = null,
    val dayManagementInProgress: Boolean = false,
    val dayManagementError: String? = null,
    val observationError: String? = null,
)

private data class DateRangeCommitProgress(
    val requestGeneration: Long,
    val minimumCollectorGeneration: Long,
    val minimumEmissionVersion: Long,
    val serviceCompleted: Boolean = false,
    val roomConfirmed: Boolean = false,
    val unknownApplyResult: Boolean = false,
)

private data class TripDeletionProgress(
    val generation: Long,
    val minimumCollectorGeneration: Long,
    val minimumEmissionVersion: Long,
    val tripId: String,
    val serviceCompleted: Boolean = false,
    val roomConfirmed: Boolean = false,
)

private const val TRIP_DELETE_IMPACT_FAILURE_MESSAGE = "无法加载删除影响，请重试"
private const val TRIP_DELETE_FAILURE_MESSAGE = "删除失败，请重试"
private const val TRIP_DELETE_SYNC_FAILURE_MESSAGE = "删除成功，但同步确认失败，请重新同步"

private fun TripDeletionUiState.tripIdOrNull(): String? = when (this) {
    TripDeletionUiState.Idle -> null
    is TripDeletionUiState.LoadingImpact -> tripId
    is TripDeletionUiState.ImpactFailure -> tripId
    is TripDeletionUiState.Ready -> tripId
}

class TripSettingsViewModel(
    savedStateHandle: SavedStateHandle,
    private val service: TripService,
    private val repository: TripRepository,
    private val impacts: DeleteImpactProvider,
    private val dateRanges: TripDateRangeService,
) : ViewModel() {
    private val tripId: String = checkNotNull(savedStateHandle["tripId"])
    private val mutableState = MutableStateFlow(TripSettingsUiState(tripId, hasAuthoritativeTrip = false))
    val state: StateFlow<TripSettingsUiState> = mutableState.asStateFlow()
    private val effectsChannel = Channel<TripSettingsEffect>(Channel.BUFFERED)
    val effects: Flow<TripSettingsEffect> = effectsChannel.receiveAsFlow()
    private var previewJob: Job? = null
    private var dateRangeGeneration = 0L
    private var dateRangeCommitProgress: DateRangeCommitProgress? = null
    private var tripObservationJob: Job? = null
    private var tripObservationGeneration = 0L
    private var tripEmissionVersion = 0L
    private var terminatedTripObservationGeneration: Long? = null
    private var deleteImpactJob: Job? = null
    private var deleteGeneration = 0L
    private var tripDeletionJob: Job? = null
    private var tripDeletionGeneration = 0L
    private var tripDeletionProgress: TripDeletionProgress? = null
    private var dayManagementJob: Job? = null
    private var dayManagementGeneration = 0L
    private var returnToTripListSent = false

    init {
        startTripObservation()
    }

    private fun startTripObservation() {
        val collectorGeneration = ++tripObservationGeneration
        terminatedTripObservationGeneration = null
        val previousJob = tripObservationJob
        tripObservationJob = viewModelScope.launch {
            previousJob?.cancelAndJoin()
            try {
                repository.observeTrip(tripId).collect { trip ->
                    acceptTripObservation(trip, collectorGeneration)
                }
                handleTripObservationFailure(collectorGeneration)
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Throwable) {
                handleTripObservationFailure(collectorGeneration)
            }
        }
    }

    fun retryTripObservation() {
        if (dateRangeCommitProgress != null || mutableState.value.observationError == null) return
        mutableState.value = mutableState.value.copy(observationError = null)
        startTripObservation()
    }

    internal suspend fun acceptTripObservation(trip: TripWithDays?, collectorGeneration: Long) {
        if (collectorGeneration != tripObservationGeneration) return
        tripEmissionVersion++
        handleTripEmission(trip, collectorGeneration)
    }

    internal fun currentTripObservationGeneration(): Long = tripObservationGeneration

    private suspend fun handleTripEmission(trip: TripWithDays?, collectorGeneration: Long) {
        if (trip == null) {
            dateRangeGeneration++
            dateRangeCommitProgress = null
            previewJob?.cancel()
            val progress = tripDeletionProgress
            val isCurrentDeletion = progress != null &&
                progress.tripId == tripId &&
                collectorGeneration >= progress.minimumCollectorGeneration &&
                tripEmissionVersion > progress.minimumEmissionVersion
            val updatedProgress = if (isCurrentDeletion) {
                progress!!.copy(roomConfirmed = true)
            } else {
                progress
            }
            val deletion = mutableState.value.tripDeletion
            val deletionCompleted = updatedProgress?.serviceCompleted == true && updatedProgress.roomConfirmed
            tripDeletionProgress = if (deletionCompleted) null else updatedProgress
            mutableState.value = mutableState.value.copy(
                dateRange = mutableState.value.dateRange.copy(
                    phase = DateRangeChangePhase.Idle,
                    error = null,
                ),
                tripDeletion = if (deletionCompleted) TripDeletionUiState.Idle else deletion,
            )
            if (progress == null || deletionCompleted) {
                if (!returnToTripListSent) {
                    returnToTripListSent = true
                    effectsChannel.send(TripSettingsEffect.ReturnToTripList)
                }
            }
            return
        }
        val baselineEnd = trip.startDate?.plusDays((trip.days.size - 1).toLong())
        val current = mutableState.value
        val range = current.dateRange
        val nextRange = if (range.isDirty || range.phase !is DateRangeChangePhase.Idle) {
            range.copy(baselineStartDate = trip.startDate, baselineEndDate = baselineEnd)
        } else {
            range.copy(
                startDate = trip.startDate,
                baselineStartDate = trip.startDate,
                baselineEndDate = baselineEnd,
                endDate = baselineEnd,
                draftStartDate = trip.startDate,
                draftEndDate = baselineEnd,
            )
        }
        mutableState.value = current.copy(
            hasAuthoritativeTrip = true,
            name = trip.name,
            startDate = trip.startDate,
            travelMode = trip.travelMode,
            days = trip.days.map { DayUi(it.id, service.displayLabel(it, trip.startDate)) },
            dateRange = nextRange,
            observationError = null,
        )
        val progress = dateRangeCommitProgress ?: return
        val activeRequest = activeDateRangeRequest() ?: return
        if (
            activeRequest.generation != progress.requestGeneration ||
            collectorGeneration < progress.minimumCollectorGeneration ||
            tripEmissionVersion <= progress.minimumEmissionVersion
        ) return
        val matches = tripMatchesDateRangeRequest(trip, activeRequest, activeDateRangeImpact())
        dateRangeCommitProgress = progress.copy(roomConfirmed = matches)
        if (matches) {
            reconcileDateRangeCompletion()
        } else if (progress.unknownApplyResult) {
            failUnknownDateRangeApply(activeRequest)
        }
    }

    private fun handleTripObservationFailure(collectorGeneration: Long) {
        if (collectorGeneration != tripObservationGeneration) return
        terminatedTripObservationGeneration = collectorGeneration
        val tripDeletion = tripDeletionProgress
        val currentTripDeletion = mutableState.value.tripDeletion as? TripDeletionUiState.Ready
        if (tripDeletion != null && currentTripDeletion?.tripId == tripDeletion.tripId) {
            if (tripDeletion.serviceCompleted) {
                mutableState.value = mutableState.value.copy(
                    tripDeletion = currentTripDeletion.copy(
                        isDeleting = false,
                        errorMessage = TRIP_DELETE_SYNC_FAILURE_MESSAGE,
                        confirmationSyncFailed = true,
                    ),
                    observationError = null,
                )
            }
            return
        }
        val progress = dateRangeCommitProgress
        if (progress == null) {
            mutableState.value = mutableState.value.copy(observationError = "无法加载旅行设置，请重试")
            return
        }
        if (!progress.serviceCompleted) return
        val phase = mutableState.value.dateRange.phase
        val request = activeDateRangeRequest() ?: return
        val impact = when (phase) {
            is DateRangeChangePhase.Applying -> phase.impact
            is DateRangeChangePhase.AwaitingRoom -> phase.impact
            is DateRangeChangePhase.SyncFailed -> phase.impact
            else -> return
        }
        if (request.generation != progress.requestGeneration) return
        showDateRangeSyncFailed(request, impact)
    }

    private fun showDateRangeSyncFailed(request: DateRangeChangeRequest, impact: DateRangeChangeImpact) {
        mutableState.value = mutableState.value.copy(
            dateRange = mutableState.value.dateRange.copy(
                phase = DateRangeChangePhase.SyncFailed(request, impact),
                error = "保存结果待同步确认，请重新同步",
            ),
        )
    }

    private fun tripDeletionWriteLocked(): Boolean = when (val deletion = mutableState.value.tripDeletion) {
        TripDeletionUiState.Idle -> false
        is TripDeletionUiState.LoadingImpact,
        is TripDeletionUiState.ImpactFailure,
        is TripDeletionUiState.Ready -> true
    }

    private fun dateMutationLocked(): Boolean =
        mutableState.value.dateRange.phase !is DateRangeChangePhase.Idle ||
            tripDeletionWriteLocked() || mutableState.value.dayManagementInProgress

    private fun dayDeletionWriteLocked(): Boolean =
        dateMutationLocked() || mutableState.value.dayDeleteInProgress

    private fun dateRangeWriteLocked(): Boolean =
        dateMutationLocked() || mutableState.value.dayDeleteInProgress

    private fun dayManagementWriteLocked(): Boolean =
        dateMutationLocked() || mutableState.value.dayDeleteInProgress ||
            mutableState.value.pendingDayDeletion != null || mutableState.value.dayDeletionRetry != null

    private fun invalidateDayDeletionForDateMutation() {
        deleteGeneration++
        deleteImpactJob?.cancel()
        deleteImpactJob = null
        mutableState.value = mutableState.value.copy(
            pendingDayDeletion = null,
            dayDeletionRetry = null,
            dayDeleteError = null,
        )
    }

    fun rename(name: String) {
        if (dateMutationLocked()) return
        viewModelScope.launch { service.renameTrip(tripId, name) }
    }

    fun setTravelMode(value: TravelMode) {
        if (dateMutationLocked()) return
        viewModelScope.launch { service.setTravelMode(tripId, value) }
    }

    fun updateDateRangeDraft(startDate: LocalDate?, endDate: LocalDate?) {
        val range = mutableState.value.dateRange
        if (tripDeletionWriteLocked() || range.submitting || range.phase is DateRangeChangePhase.SyncFailed) return
        dateRangeGeneration++
        previewJob?.cancel()
        mutableState.value = mutableState.value.copy(
            dateRange = range.copy(
                startDate = startDate,
                endDate = endDate,
                draftStartDate = startDate,
                draftEndDate = endDate,
                isDirty = startDate != range.baselineStartDate || endDate != range.baselineEndDate,
                phase = DateRangeChangePhase.Idle,
                error = null,
            ),
        )
    }


    fun requestDateRangeChange() {
        val range = mutableState.value.dateRange
        if (dateRangeWriteLocked()) return
        if (!range.isDirty || range.draftStartDate == null || range.draftEndDate == null) return
        val request = DateRangeChangeRequest(
            generation = ++dateRangeGeneration,
            tripId = tripId,
            baselineStartDate = range.baselineStartDate,
            baselineDayIds = mutableState.value.days.map(DayUi::id),
            targetStartDate = range.draftStartDate,
            targetEndDate = range.draftEndDate,
        )
        invalidateDayDeletionForDateMutation()
        mutableState.value = mutableState.value.copy(
            dateRange = range.copy(phase = DateRangeChangePhase.Previewing(request), error = null),
        )
        previewJob = viewModelScope.launch {
            try {
                val impact = dateRanges.preview(request)
                if (request.generation != dateRangeGeneration ||
                    (mutableState.value.dateRange.phase as? DateRangeChangePhase.Previewing)?.request != request
                ) return@launch
                if (impact.deletedDayIds.isEmpty()) {
                    beginDateRangeApply(request, impact)
                    applyDateRange(request, impact)
                } else {
                    mutableState.value = mutableState.value.copy(
                        dateRange = mutableState.value.dateRange.copy(
                            phase = DateRangeChangePhase.AwaitingConfirmation(request, impact),
                            error = null,
                        ),
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: DateRangeSnapshotChangedException) {
                previewFailed(request, "旅行内容已变化，请重新确认")
            } catch (_: com.yangchengwei.easytrip.trip.domain.TripDateRangeTargetNotFoundException) {
                previewFailed(request, "旅行已不存在，请返回旅行列表")
            } catch (exception: IllegalArgumentException) {
                previewFailed(request, exception.message ?: "日期范围无效")
            } catch (_: Throwable) {
                previewFailed(request, "无法检查日期范围，请重试")
            }
        }
    }

    fun cancelDateRangeChange() {
        val range = mutableState.value.dateRange
        if (range.phase !is DateRangeChangePhase.AwaitingConfirmation) return
        dateRangeGeneration++
        mutableState.value = mutableState.value.copy(
            dateRange = range.copy(
                startDate = range.baselineStartDate,
                endDate = range.baselineEndDate,
                draftStartDate = range.baselineStartDate,
                draftEndDate = range.baselineEndDate,
                isDirty = false,
                phase = DateRangeChangePhase.Idle,
                error = null,
            ),
        )
    }

    fun confirmDateRangeChange() {
        val range = mutableState.value.dateRange
        val phase = range.phase as? DateRangeChangePhase.AwaitingConfirmation ?: return
        if (range.error != null) {
            mutableState.value = mutableState.value.copy(
                dateRange = range.copy(phase = DateRangeChangePhase.Idle, error = null),
            )
            requestDateRangeChange()
            return
        }
        beginDateRangeApply(phase.request, phase.impact)
        viewModelScope.launch { applyDateRange(phase.request, phase.impact) }
    }

    fun retryDateRangeSync() {
        val phase = mutableState.value.dateRange.phase as? DateRangeChangePhase.SyncFailed ?: return
        val progress = dateRangeCommitProgress ?: return
        if (progress.requestGeneration != phase.request.generation || !progress.serviceCompleted) return
        dateRangeCommitProgress = progress.copy(
            minimumCollectorGeneration = tripObservationGeneration + 1,
            minimumEmissionVersion = tripEmissionVersion,
            roomConfirmed = false,
        )
        mutableState.value = mutableState.value.copy(
            dateRange = mutableState.value.dateRange.copy(
                phase = DateRangeChangePhase.AwaitingRoom(phase.request, phase.impact),
                error = null,
            ),
        )
        startTripObservation()
    }

    private fun beginDateRangeApply(request: DateRangeChangeRequest, impact: DateRangeChangeImpact) {
        dateRangeCommitProgress = DateRangeCommitProgress(
            requestGeneration = request.generation,
            minimumCollectorGeneration = tripObservationGeneration,
            minimumEmissionVersion = tripEmissionVersion,
        )
        mutableState.value = mutableState.value.copy(
            dateRange = mutableState.value.dateRange.copy(
                phase = DateRangeChangePhase.Applying(request, impact),
                error = null,
            ),
        )
    }

    private fun previewFailed(request: DateRangeChangeRequest, message: String) {
        if (request.generation != dateRangeGeneration ||
            (mutableState.value.dateRange.phase as? DateRangeChangePhase.Previewing)?.request != request
        ) return
        mutableState.value = mutableState.value.copy(
            dateRange = mutableState.value.dateRange.copy(phase = DateRangeChangePhase.Idle, error = message),
        )
    }

    private suspend fun applyDateRange(
        request: DateRangeChangeRequest,
        impact: DateRangeChangeImpact,
    ) {
        try {
            dateRanges.apply(impact)
            val progress = dateRangeCommitProgress
            if (
                request.generation != dateRangeGeneration ||
                progress?.requestGeneration != request.generation ||
                (mutableState.value.dateRange.phase as? DateRangeChangePhase.Applying)?.request != request
            ) return
            dateRangeCommitProgress = progress.copy(serviceCompleted = true)
            if (terminatedTripObservationGeneration == tripObservationGeneration) {
                showDateRangeSyncFailed(request, impact)
            } else {
                mutableState.value = mutableState.value.copy(
                    dateRange = mutableState.value.dateRange.copy(
                        phase = DateRangeChangePhase.AwaitingRoom(request, impact),
                        error = null,
                    ),
                )
                reconcileDateRangeCompletion()
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: DateRangeSnapshotChangedException) {
            failDateRangeApply(request, "旅行内容已变化，请重新确认")
        } catch (_: com.yangchengwei.easytrip.trip.domain.TripDateRangeTargetNotFoundException) {
            completeMissingTripDateRangeApply(request)
        } catch (_: Throwable) {
            val progress = dateRangeCommitProgress
            if (
                request.generation != dateRangeGeneration ||
                progress?.requestGeneration != request.generation ||
                (mutableState.value.dateRange.phase as? DateRangeChangePhase.Applying)?.request != request
            ) return
            dateRangeCommitProgress = progress.copy(
                minimumCollectorGeneration = tripObservationGeneration + 1,
                minimumEmissionVersion = tripEmissionVersion,
                serviceCompleted = true,
                roomConfirmed = false,
                unknownApplyResult = true,
            )
            mutableState.value = mutableState.value.copy(
                dateRange = mutableState.value.dateRange.copy(
                    phase = DateRangeChangePhase.AwaitingRoom(request, impact),
                    error = null,
                ),
            )
            startTripObservation()
        }
    }

    private suspend fun completeMissingTripDateRangeApply(request: DateRangeChangeRequest) {
        if (
            request.generation != dateRangeGeneration ||
            dateRangeCommitProgress?.requestGeneration != request.generation ||
            (mutableState.value.dateRange.phase as? DateRangeChangePhase.Applying)?.request != request
        ) return
        dateRangeGeneration++
        dateRangeCommitProgress = null
        mutableState.value = mutableState.value.copy(
            hasAuthoritativeTrip = false,
            dateRange = mutableState.value.dateRange.copy(
                phase = DateRangeChangePhase.Idle,
                error = null,
            ),
        )
        if (!returnToTripListSent) {
            returnToTripListSent = true
            effectsChannel.send(TripSettingsEffect.ReturnToTripList)
        }
    }

    private fun failDateRangeApply(request: DateRangeChangeRequest, message: String) {
        if (
            request.generation != dateRangeGeneration ||
            dateRangeCommitProgress?.requestGeneration != request.generation ||
            (mutableState.value.dateRange.phase as? DateRangeChangePhase.Applying)?.request != request
        ) return
        dateRangeCommitProgress = null
        mutableState.value = mutableState.value.copy(
            dateRange = mutableState.value.dateRange.copy(
                phase = DateRangeChangePhase.Idle,
                error = message,
            ),
        )
    }

    private fun activeDateRangeRequest(): DateRangeChangeRequest? = when (val phase = mutableState.value.dateRange.phase) {
        is DateRangeChangePhase.Applying -> phase.request
        is DateRangeChangePhase.AwaitingRoom -> phase.request
        is DateRangeChangePhase.SyncFailed -> phase.request
        else -> null
    }

    private fun activeDateRangeImpact(): DateRangeChangeImpact? = when (val phase = mutableState.value.dateRange.phase) {
        is DateRangeChangePhase.Applying -> phase.impact
        is DateRangeChangePhase.AwaitingRoom -> phase.impact
        is DateRangeChangePhase.SyncFailed -> phase.impact
        else -> null
    }

    private fun failUnknownDateRangeApply(request: DateRangeChangeRequest) {
        if (request.generation != dateRangeGeneration) return
        dateRangeCommitProgress = null
        mutableState.value = mutableState.value.copy(
            dateRange = mutableState.value.dateRange.copy(
                phase = DateRangeChangePhase.Idle,
                error = "保存结果未生效，请重新检查影响",
            ),
        )
    }

    private fun reconcileDateRangeCompletion() {
        val progress = dateRangeCommitProgress ?: return
        if (!progress.serviceCompleted || !progress.roomConfirmed) return
        val request = activeDateRangeRequest() ?: return
        if (request.generation != progress.requestGeneration || request.generation != dateRangeGeneration) return
        val current = mutableState.value
        dateRangeCommitProgress = null
        mutableState.value = current.copy(
            dateRange = current.dateRange.copy(
                startDate = request.targetStartDate,
                baselineStartDate = request.targetStartDate,
                baselineEndDate = request.targetEndDate,
                endDate = request.targetEndDate,
                draftStartDate = request.targetStartDate,
                draftEndDate = request.targetEndDate,
                isDirty = false,
                phase = DateRangeChangePhase.Idle,
                error = null,
            ),
        )
    }

    fun requestTripDeletion() {
        if (
            !mutableState.value.hasAuthoritativeTrip ||
            dateMutationLocked() ||
            mutableState.value.dayDeleteInProgress ||
            mutableState.value.pendingDayDeletion != null ||
            mutableState.value.dayDeletionRetry != null
        ) return
        loadTripDeletionImpact(mutableState.value.name)
    }

    private fun loadTripDeletionImpact(tripName: String) {
        val generation = ++tripDeletionGeneration
        tripDeletionProgress = null
        tripDeletionJob?.cancel()
        mutableState.value = mutableState.value.copy(
            tripDeletion = TripDeletionUiState.LoadingImpact(tripId, tripName),
        )
        tripDeletionJob = viewModelScope.launch {
            try {
                val impact = impacts.trip(tripId)
                if (generation == tripDeletionGeneration && mutableState.value.tripDeletion.tripIdOrNull() == tripId) {
                    mutableState.value = mutableState.value.copy(
                        tripDeletion = TripDeletionUiState.Ready(tripId, tripName, impact.toConfirmationUiModel(tripName)),
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Throwable) {
                if (generation == tripDeletionGeneration && mutableState.value.tripDeletion.tripIdOrNull() == tripId) {
                    mutableState.value = mutableState.value.copy(
                        tripDeletion = TripDeletionUiState.ImpactFailure(tripId, tripName, TRIP_DELETE_IMPACT_FAILURE_MESSAGE),
                    )
                }
            }
        }
    }

    fun retryTripDeletionImpact() {
        val deletion = mutableState.value.tripDeletion as? TripDeletionUiState.ImpactFailure ?: return
        requestTripDeletionFor(deletion.tripName)
    }

    private fun requestTripDeletionFor(tripName: String) {
        loadTripDeletionImpact(tripName)
    }

    fun cancelTripDeletion() {
        val deletion = mutableState.value.tripDeletion
        if (deletion is TripDeletionUiState.Ready && (deletion.isDeleting || deletion.confirmationSyncFailed)) return
        tripDeletionGeneration++
        tripDeletionProgress = null
        tripDeletionJob?.cancel()
        tripDeletionJob = null
        mutableState.value = mutableState.value.copy(tripDeletion = TripDeletionUiState.Idle)
    }

    fun confirmTripDeletion() {
        val deletion = mutableState.value.tripDeletion as? TripDeletionUiState.Ready ?: return
        if (deletion.isDeleting || deletion.confirmationSyncFailed) return
        val generation = ++tripDeletionGeneration
        val progress = TripDeletionProgress(
            generation = generation,
            minimumCollectorGeneration = tripObservationGeneration,
            minimumEmissionVersion = tripEmissionVersion,
            tripId = deletion.tripId,
        )
        tripDeletionProgress = progress
        mutableState.value = mutableState.value.copy(
            tripDeletion = deletion.copy(isDeleting = true, errorMessage = null),
        )
        tripDeletionJob = viewModelScope.launch {
            try {
                service.deleteTrip(deletion.tripId)
                val currentProgress = tripDeletionProgress
                if (
                    generation != tripDeletionGeneration ||
                    currentProgress?.generation != generation ||
                    mutableState.value.tripDeletion.tripIdOrNull() != deletion.tripId
                ) return@launch
                tripDeletionProgress = currentProgress.copy(serviceCompleted = true)
                if (completeTripDeletionIfRoomConfirmed(generation, deletion.tripId)) {
                    return@launch
                } else if (terminatedTripObservationGeneration == tripObservationGeneration) {
                    mutableState.value = mutableState.value.copy(
                        tripDeletion = deletion.copy(
                            isDeleting = false,
                            errorMessage = TRIP_DELETE_SYNC_FAILURE_MESSAGE,
                            confirmationSyncFailed = true,
                        ),
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Throwable) {
                if (generation == tripDeletionGeneration && mutableState.value.tripDeletion.tripIdOrNull() == deletion.tripId) {
                    if (!completeTripDeletionIfRoomConfirmed(generation, deletion.tripId)) {
                        tripDeletionProgress = null
                        mutableState.value = mutableState.value.copy(
                            tripDeletion = deletion.copy(
                                isDeleting = false,
                                errorMessage = TRIP_DELETE_FAILURE_MESSAGE,
                            ),
                        )
                    }
                }
            }
        }
    }

    private suspend fun completeTripDeletionIfRoomConfirmed(generation: Long, deletedTripId: String): Boolean {
        val progress = tripDeletionProgress
        if (
            progress?.generation != generation ||
            progress.tripId != deletedTripId ||
            !progress.roomConfirmed
        ) return false
        tripDeletionProgress = null
        mutableState.value = mutableState.value.copy(tripDeletion = TripDeletionUiState.Idle)
        if (!returnToTripListSent) {
            returnToTripListSent = true
            effectsChannel.send(TripSettingsEffect.ReturnToTripList)
        }
        return true
    }

    fun retryTripDeletionSync() {
        val deletion = mutableState.value.tripDeletion as? TripDeletionUiState.Ready ?: return
        val progress = tripDeletionProgress ?: return
        if (
            deletion.isDeleting ||
            !deletion.confirmationSyncFailed ||
            !progress.serviceCompleted ||
            progress.tripId != deletion.tripId
        ) return
        tripDeletionProgress = progress.copy(
            minimumCollectorGeneration = tripObservationGeneration + 1,
            minimumEmissionVersion = tripEmissionVersion,
            roomConfirmed = false,
        )
        mutableState.value = mutableState.value.copy(
            tripDeletion = deletion.copy(
                isDeleting = true,
                errorMessage = null,
                confirmationSyncFailed = false,
            ),
        )
        startTripObservation()
    }

    fun appendDay() {
        if (dayManagementWriteLocked() || mutableState.value.days.size >= com.yangchengwei.easytrip.trip.domain.MAX_TRIP_DAYS) return
        runDayManagement { service.appendTripDay(tripId) }
    }

    fun moveDay(day: DayUi, targetIndex: Int) {
        val current = mutableState.value
        val currentIndex = current.days.indexOfFirst { it.id == day.id }
        if (
            current.startDate != null ||
            dayManagementWriteLocked() ||
            currentIndex < 0 ||
            targetIndex !in current.days.indices ||
            targetIndex == currentIndex
        ) return
        runDayManagement { service.moveDay(tripId, day.id, targetIndex) }
    }

    private fun runDayManagement(block: suspend () -> Unit) {
        val generation = ++dayManagementGeneration
        mutableState.value = mutableState.value.copy(dayManagementInProgress = true, dayManagementError = null)
        dayManagementJob = viewModelScope.launch {
            try {
                block()
                if (generation == dayManagementGeneration) {
                    mutableState.value = mutableState.value.copy(dayManagementInProgress = false, dayManagementError = null)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Throwable) {
                if (generation == dayManagementGeneration) {
                    mutableState.value = mutableState.value.copy(
                        dayManagementInProgress = false,
                        dayManagementError = exception.message ?: "旅行日操作失败",
                    )
                }
            }
        }
    }

    fun requestDelete(day: DayUi) {
        if (dayDeletionWriteLocked() || mutableState.value.days.size <= 1) return
        val generation = ++deleteGeneration
        deleteImpactJob?.cancel()
        mutableState.value = mutableState.value.copy(
            pendingDayDeletion = null,
            dayDeletionRetry = day,
            dayDeleteError = null,
        )
        deleteImpactJob = viewModelScope.launch {
            try {
                val impact = impacts.day(day.id)
                if (generation == deleteGeneration && !dayDeletionWriteLocked()) {
                    mutableState.value = mutableState.value.copy(
                        pendingDayDeletion = PendingDayDeletion(day, impact),
                        dayDeletionRetry = null,
                        dayDeleteError = null,
                    )
                }
            } catch (_: CancellationException) {
                throw CancellationException()
            } catch (_: Throwable) {
                if (generation == deleteGeneration && !dayDeletionWriteLocked()) {
                    mutableState.value = mutableState.value.copy(dayDeleteError = "无法检查删除影响，请重试")
                }
            }
        }
    }
    fun retryDelete() {
        if (dayDeletionWriteLocked()) return
        mutableState.value.dayDeletionRetry?.let(::requestDelete)
    }
    fun cancelDelete() {
        if (dayDeletionWriteLocked()) return
        if (!mutableState.value.dayDeleteInProgress) {
            deleteGeneration++
            deleteImpactJob?.cancel()
            mutableState.value = mutableState.value.copy(
                pendingDayDeletion = null,
                dayDeletionRetry = null,
                dayDeleteError = null,
            )
        }
    }
    fun confirmDelete() {
        if (dayDeletionWriteLocked()) return
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
                    dayDeletionRetry = null,
                    dayDeleteInProgress = false,
                    dayDeleteError = null,
                )
            } catch (_: Throwable) {
                mutableState.value = mutableState.value.copy(
                    pendingDayDeletion = null,
                    dayDeletionRetry = day,
                    dayDeleteInProgress = false,
                    dayDeleteError = "删除失败，请重新检查影响",
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
