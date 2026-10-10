package com.yangchengwei.easytrip.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yangchengwei.easytrip.place.amap.PlaceSearchDataSource
import com.yangchengwei.easytrip.place.domain.SavedPlaceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlaceAssistantViewModel(
    tripId: String,
    val config: AssistantConfigStore,
    importer: PlaceImport,
    places: SavedPlaceRepository,
    parser: PlaceIntentParser = DeepSeekPlaceParser(config::read),
) : ViewModel() {
    val controller = PlaceAssistantController(tripId, viewModelScope, parser, null, importer)
    private val mutableOpen = MutableStateFlow(false)
    val open = mutableOpen.asStateFlow()
    private val mutableFocus = MutableStateFlow<String?>(null)
    val focus = mutableFocus.asStateFlow()
    fun show() { mutableOpen.value = true }
    fun hide() { if (!controller.state.value.saving) mutableOpen.value = false }
    fun focus(id: String?) { mutableFocus.value = id; if (id != null) show() }
    init {
        viewModelScope.launch { controller.recoverReceipt() }
        viewModelScope.launch { places.observeSavedPoiIds(tripId).collect(controller::updateSaved) }
        viewModelScope.launch {
            config.revision.collect { controller.configurationChanged() }
        }
    }
    class Factory(
        private val tripId: String, private val config: AssistantConfigStore,
        private val importer: PlaceImport, private val places: SavedPlaceRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PlaceAssistantViewModel(tripId, config, importer, places) as T
    }
}
