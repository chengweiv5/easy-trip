package com.yangchengwei.easytrip.place.ui

import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.amap.PlaceSearchDataSource
import com.yangchengwei.easytrip.place.domain.SavedPlace
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface PlaceSearchPhase {
    data object Initial : PlaceSearchPhase
    data object Loading : PlaceSearchPhase
    data object Results : PlaceSearchPhase
    data object Empty : PlaceSearchPhase
    data class NetworkFailure(val message: String) : PlaceSearchPhase
}

data class PlaceSearchState(
    val query: String = "",
    val results: List<PlaceCandidate> = emptyList(),
    val savedPlaces: List<SavedPlace> = emptyList(),
    val phase: PlaceSearchPhase = PlaceSearchPhase.Initial,
) {
    val searching: Boolean get() = phase == PlaceSearchPhase.Loading
    val error: String? get() = (phase as? PlaceSearchPhase.NetworkFailure)?.message
}

class PlaceSearchReducer(
    private var source: PlaceSearchDataSource?,
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher,
    initialQuery: String = "",
) {
    private val mutableState = MutableStateFlow(PlaceSearchState(query = initialQuery))
    val state: StateFlow<PlaceSearchState> = mutableState.asStateFlow()
    private var searchJob: Job? = null
    private var generation = 0L

    init {
        if (initialQuery.isNotBlank()) search(initialQuery)
    }

    fun setSource(value: PlaceSearchDataSource?) {
        if (source === value) return
        source = value
        searchJob?.cancel()
        generation++
        val query = mutableState.value.query
        mutableState.value = mutableState.value.copy(phase = PlaceSearchPhase.Initial)
        if (query.isNotBlank()) search(query)
    }

    fun setSavedPlaces(value: List<SavedPlace>) {
        mutableState.value = mutableState.value.copy(savedPlaces = value)
    }

    fun clear() {
        generation++
        searchJob?.cancel()
        searchJob = null
        mutableState.value = PlaceSearchState()
    }

    fun setQuery(value: String) {
        searchJob?.cancel()
        mutableState.value = mutableState.value.copy(
            query = value,
            results = if (value.isBlank()) emptyList() else mutableState.value.results,
            phase = PlaceSearchPhase.Initial,
        )
        if (value.isNotBlank()) search(value)
    }

    fun submit() {
        val query = mutableState.value.query
        if (query.isNotBlank()) search(query, debounce = false)
    }

    fun retry() = submit()

    private fun search(query: String, debounce: Boolean = true) {
        val current = ++generation
        searchJob?.cancel()
        searchJob = scope.launch(dispatcher) {
            if (debounce) delay(300)
            val active = source ?: run {
                if (current == generation) {
                    mutableState.value = mutableState.value.copy(
                        results = emptyList(),
                        phase = PlaceSearchPhase.NetworkFailure("请先阅读并同意高德隐私政策"),
                    )
                }
                return@launch
            }
            if (current == generation) {
                mutableState.value = mutableState.value.copy(phase = PlaceSearchPhase.Loading)
            }
            try {
                val result = active.search(query.trim(), null)
                if (current == generation) {
                    mutableState.value = mutableState.value.copy(
                        results = result,
                        phase = if (result.isEmpty()) PlaceSearchPhase.Empty else PlaceSearchPhase.Results,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                if (current == generation) {
                    mutableState.value = mutableState.value.copy(
                        results = emptyList(),
                        phase = PlaceSearchPhase.NetworkFailure(error.message ?: "搜索失败"),
                    )
                }
            }
        }
    }
}
