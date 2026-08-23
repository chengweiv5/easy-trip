package com.yangchengwei.easytrip.trip.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewModelScope
import com.yangchengwei.easytrip.core.ui.component.ConfirmationUiModel
import com.yangchengwei.easytrip.trip.domain.TripRepository
import com.yangchengwei.easytrip.trip.domain.TripService
import com.yangchengwei.easytrip.trip.domain.TripSummary
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class TripListUiState(
    val page: TripListPageState = TripListPageState.Loading,
    val trips: List<TripSummary> = emptyList(),
    val pendingDelete: TripSummary? = null,
    val pendingDeleteImpact: TripDeleteImpact? = null,
    val deleteConfirmation: ConfirmationUiModel? = null,
    val deleteInProgress: Boolean = false,
    val deleteError: String? = null,
)

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
                    page = if (trips.isEmpty()) {
                        TripListPageState.Empty
                    } else {
                        val cards = trips.map(TripSummary::toTripCardUiModel)
                        TripListPageState.Content(cards.first(), cards.drop(1))
                    },
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
            is TripListAction.RequestDelete -> mutableState.value.trips.firstOrNull { it.id == action.tripId }?.let(::requestDelete)
        }
    }

    fun openWorkspace(id: String) { viewModelScope.launch { navigationChannel.send(TripListNavigation.OpenWorkspace(id)) } }
    fun openSettings(id: String) { viewModelScope.launch { navigationChannel.send(TripListNavigation.OpenSettings(id)) } }

    fun requestDelete(value: TripSummary) {
        viewModelScope.launch {
            val impact = impacts.trip(value.id)
            mutableState.value = mutableState.value.copy(
                pendingDelete = value,
                pendingDeleteImpact = impact,
                deleteConfirmation = impact.toConfirmation(value.name),
                deleteError = null,
            )
        }
    }

    fun cancelDelete() {
        if (mutableState.value.deleteInProgress) return
        mutableState.value = mutableState.value.copy(
            pendingDelete = null,
            pendingDeleteImpact = null,
            deleteConfirmation = null,
            deleteError = null,
        )
    }

    fun confirmDelete() {
        val current = mutableState.value
        val value = current.pendingDelete ?: return
        if (current.deleteInProgress) return
        mutableState.value = current.copy(deleteInProgress = true, deleteError = null)
        viewModelScope.launch {
            runCatching { service.deleteTrip(value.id) }
                .onSuccess {
                    mutableState.value = mutableState.value.copy(
                        pendingDelete = null,
                        pendingDeleteImpact = null,
                        deleteConfirmation = null,
                        deleteInProgress = false,
                        deleteError = null,
                    )
                }
                .onFailure {
                    mutableState.value = mutableState.value.copy(
                        deleteInProgress = false,
                        deleteError = "删除失败，请重试",
                    )
                }
        }
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
