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

data class PlaceSearchState(val query: String = "", val results: List<PlaceCandidate> = emptyList(), val savedPlaces: List<SavedPlace> = emptyList(), val searching: Boolean = false, val error: String? = null)

class PlaceSearchReducer(private var source: PlaceSearchDataSource?, private val scope: CoroutineScope, private val dispatcher: CoroutineDispatcher) {
    private val mutableState = MutableStateFlow(PlaceSearchState())
    val state: StateFlow<PlaceSearchState> = mutableState.asStateFlow()
    private var searchJob: Job? = null
    private var generation = 0L

    fun setSource(value: PlaceSearchDataSource?) {
        if (source === value) return
        source = value
        searchJob?.cancel()
        generation++
        val query = mutableState.value.query
        mutableState.value = mutableState.value.copy(searching = false, error = null)
        if (query.isNotBlank()) setQuery(query)
    }
    fun setSavedPlaces(value: List<SavedPlace>) { mutableState.value = mutableState.value.copy(savedPlaces = value) }
    fun clear() {
        generation++
        searchJob?.cancel()
        searchJob = null
        mutableState.value = PlaceSearchState()
    }
    fun setQuery(value: String) {
        val current = ++generation
        searchJob?.cancel()
        mutableState.value = mutableState.value.copy(query = value, searching = false, error = null, results = if (value.isBlank()) emptyList() else mutableState.value.results)
        if (value.isBlank()) { mutableState.value = mutableState.value.copy(searching = false); return }
        searchJob = scope.launch(dispatcher) {
            delay(300)
            val active = source ?: run {
                if (current == generation) mutableState.value = mutableState.value.copy(searching = false, error = "请先阅读并同意高德隐私政策")
                return@launch
            }
            if (current == generation) mutableState.value = mutableState.value.copy(searching = true)
            try {
                val result = active.search(value.trim(), null)
                if (current == generation) mutableState.value = mutableState.value.copy(results = result, searching = false, error = null)
            } catch (error: CancellationException) { throw error } catch (error: Throwable) {
                if (current == generation) mutableState.value = mutableState.value.copy(results = emptyList(), searching = false, error = error.message ?: "搜索失败")
            }
        }
    }
}
