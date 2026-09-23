package com.yangchengwei.easytrip.core.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class ThemePickerState(
    val visible: Boolean = false,
    val preview: ThemePalette = ThemePalette.LAKE,
    val saving: Boolean = false,
    val failed: Boolean = false,
)

class ThemePickerViewModel(private val store: ThemePreferenceStore) : ViewModel() {
    private val mutableState = MutableStateFlow(ThemePickerState(preview = store.theme.value))
    val state = mutableState.asStateFlow()
    private val appliedEvents = Channel<ThemePalette>(Channel.BUFFERED)
    val applied = appliedEvents.receiveAsFlow()

    fun open() {
        if (!mutableState.value.visible) mutableState.value = ThemePickerState(visible = true, preview = store.theme.value)
    }

    fun select(palette: ThemePalette) {
        val state = mutableState.value
        if (state.visible && !state.saving) mutableState.value = state.copy(preview = palette, failed = false)
    }

    fun dismiss() {
        if (!mutableState.value.saving) mutableState.value = mutableState.value.copy(visible = false)
    }

    fun apply() {
        val state = mutableState.value
        if (!state.visible || state.saving || state.preview == store.theme.value) return
        mutableState.value = state.copy(saving = true, failed = false)
        viewModelScope.launch {
            val result = store.apply(state.preview)
            mutableState.value = state.copy(visible = result.isFailure, failed = result.isFailure)
            if (result.isSuccess) appliedEvents.send(state.preview)
        }
    }

    class Factory(private val store: ThemePreferenceStore) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ThemePickerViewModel(store) as T
    }
}
