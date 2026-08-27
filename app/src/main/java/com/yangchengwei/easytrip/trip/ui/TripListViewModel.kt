package com.yangchengwei.easytrip.trip.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewModelScope
import com.yangchengwei.easytrip.core.ui.component.ConfirmationUiModel
import com.yangchengwei.easytrip.trip.domain.TripRepository
import com.yangchengwei.easytrip.trip.domain.TripService
import com.yangchengwei.easytrip.trip.domain.TripSummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

private const val IMPACT_FAILURE_MESSAGE = "无法加载删除影响，请重试"
private const val DELETE_FAILURE_MESSAGE = "删除失败，请重试"

data class TripListUiState(
    val page: TripListPageState = TripListPageState.Loading,
    val trips: List<TripSummary> = emptyList(),
    val deletion: TripDeletionUiState = TripDeletionUiState.Idle,
) {
    val deleteConfirmation: ConfirmationUiModel?
        get() = (deletion as? TripDeletionUiState.Ready)?.confirmation
    val deleteInProgress: Boolean
        get() = (deletion as? TripDeletionUiState.Ready)?.isDeleting == true
    val deleteError: String?
        get() = (deletion as? TripDeletionUiState.Ready)?.errorMessage
}

sealed interface TripListNavigation {
    data class OpenWorkspace(val tripId: String) : TripListNavigation
    data class OpenSettings(val tripId: String) : TripListNavigation
}

class TripListViewModel(
    private val service: TripService,
    private val repository: TripRepository,
    private val impacts: DeleteImpactProvider,
) : ViewModel() {
    private val mutableState = MutableStateFlow(TripListUiState())
    val state: StateFlow<TripListUiState> = mutableState.asStateFlow()
    private val navigationChannel = Channel<TripListNavigation>(Channel.BUFFERED)
    val navigation = navigationChannel.receiveAsFlow()
    private var tripsJob: Job? = null
    private var deleteJob: Job? = null
    private var deleteGeneration = 0L
    private var awaitingDeletedTrip: Pair<Long, String>? = null

    init { observeTrips() }

    fun observeTrips() {
        tripsJob?.cancel()
        mutableState.value = mutableState.value.copy(
            page = TripListPageState.Loading,
            trips = emptyList(),
        )
        tripsJob = viewModelScope.launch {
            repository.observeTrips().catch {
                mutableState.value = mutableState.value.copy(
                    page = TripListPageState.Error("无法加载旅行"),
                    trips = emptyList(),
                )
            }.collect { trips ->
                val waiting = awaitingDeletedTrip
                val deletion = if (
                    waiting != null &&
                    waiting.first == deleteGeneration &&
                    trips.none { it.id == waiting.second } &&
                    mutableState.value.deletion.tripIdOrNull() == waiting.second
                ) {
                    awaitingDeletedTrip = null
                    TripDeletionUiState.Idle
                } else {
                    mutableState.value.deletion
                }
                mutableState.value = mutableState.value.copy(
                    trips = trips,
                    page = if (trips.isEmpty()) {
                        TripListPageState.Empty
                    } else {
                        val cards = trips.map(TripSummary::toTripCardUiModel)
                        TripListPageState.Content(cards.first(), cards.drop(1))
                    },
                    deletion = deletion,
                )
            }
        }
    }

    fun onAction(action: TripListAction) {
        when (action) {
            TripListAction.CreateTrip -> Unit
            TripListAction.Retry -> observeTrips()
            is TripListAction.OpenTrip -> openWorkspace(action.tripId)
            is TripListAction.OpenSettings -> openSettings(action.tripId)
            is TripListAction.RequestDelete -> requestDelete(action.tripId)
            TripListAction.RetryDeleteImpact -> retryDeleteImpact()
            TripListAction.ConfirmDelete -> confirmDelete()
            TripListAction.CancelDelete -> cancelDelete()
        }
    }

    fun openWorkspace(id: String) { viewModelScope.launch { navigationChannel.send(TripListNavigation.OpenWorkspace(id)) } }
    fun openSettings(id: String) { viewModelScope.launch { navigationChannel.send(TripListNavigation.OpenSettings(id)) } }

    private fun requestDelete(tripId: String) {
        val trip = mutableState.value.trips.firstOrNull { it.id == tripId } ?: return
        loadDeleteImpact(trip.id, trip.name)
    }

    private fun retryDeleteImpact() {
        val current = mutableState.value.deletion as? TripDeletionUiState.ImpactFailure ?: return
        loadDeleteImpact(current.tripId, current.tripName)
    }

    private fun loadDeleteImpact(tripId: String, tripName: String) {
        deleteJob?.cancel()
        awaitingDeletedTrip = null
        val generation = ++deleteGeneration
        mutableState.value = mutableState.value.copy(
            deletion = TripDeletionUiState.LoadingImpact(tripId, tripName),
        )
        deleteJob = viewModelScope.launch {
            try {
                val impact = impacts.trip(tripId)
                if (deleteGeneration == generation && mutableState.value.deletion.tripIdOrNull() == tripId) {
                    mutableState.value = mutableState.value.copy(
                        deletion = TripDeletionUiState.Ready(
                            tripId = tripId,
                            tripName = tripName,
                            confirmation = impact.toConfirmation(tripName),
                        ),
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                if (deleteGeneration == generation && mutableState.value.deletion.tripIdOrNull() == tripId) {
                    mutableState.value = mutableState.value.copy(
                        deletion = TripDeletionUiState.ImpactFailure(
                            tripId = tripId,
                            tripName = tripName,
                            message = IMPACT_FAILURE_MESSAGE,
                        ),
                    )
                }
            }
        }
    }

    fun cancelDelete() {
        val current = mutableState.value.deletion
        if (current is TripDeletionUiState.Ready && current.isDeleting) return
        deleteGeneration++
        awaitingDeletedTrip = null
        deleteJob?.cancel()
        deleteJob = null
        mutableState.value = mutableState.value.copy(deletion = TripDeletionUiState.Idle)
    }

    fun confirmDelete() {
        val current = mutableState.value.deletion as? TripDeletionUiState.Ready ?: return
        if (current.isDeleting) return
        val generation = ++deleteGeneration
        mutableState.value = mutableState.value.copy(
            deletion = current.copy(isDeleting = true, errorMessage = null),
        )
        deleteJob = viewModelScope.launch {
            try {
                service.deleteTrip(current.tripId)
                if (deleteGeneration == generation && mutableState.value.deletion.tripIdOrNull() == current.tripId) {
                    if (mutableState.value.trips.none { it.id == current.tripId }) {
                        mutableState.value = mutableState.value.copy(deletion = TripDeletionUiState.Idle)
                    } else {
                        awaitingDeletedTrip = generation to current.tripId
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                if (deleteGeneration == generation && mutableState.value.deletion.tripIdOrNull() == current.tripId) {
                    mutableState.value = mutableState.value.copy(
                        deletion = current.copy(
                            isDeleting = false,
                            errorMessage = DELETE_FAILURE_MESSAGE,
                        ),
                    )
                }
            }
        }
    }

    private fun TripDeletionUiState.tripIdOrNull(): String? = when (this) {
        TripDeletionUiState.Idle -> null
        is TripDeletionUiState.LoadingImpact -> tripId
        is TripDeletionUiState.ImpactFailure -> tripId
        is TripDeletionUiState.Ready -> tripId
    }

    private fun TripDeleteImpact.toConfirmation(tripName: String) = ConfirmationUiModel(
        title = "删除$tripName？",
        message = "此操作将永久删除旅行及其中的所有内容，无法撤销。",
        deletedItems = listOf(
            "$days 个旅行日",
            "$places 个收藏地点",
            "$tags 个标签",
            "$itineraryItems 个行程项",
            "$routeLegs 个路线段",
        ),
        retainedItems = listOf("其他旅行及其内容"),
        confirmLabel = "确认删除旅行",
        dismissLabel = "取消",
        destructive = true,
        reversible = false,
    )

    class Factory(
        private val service: TripService,
        private val repository: TripRepository,
        private val impacts: DeleteImpactProvider,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
            TripListViewModel(service, repository, impacts) as T
    }
}
