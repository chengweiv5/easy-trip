package com.yangchengwei.easytrip.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope

class AppUpdateViewModel(version: String, service: UpdateService) : ViewModel() {
    val controller = UpdateController(version, service, viewModelScope)
    override fun onCleared() { controller.close() }
    class Factory(private val version: String, private val service: UpdateService) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = AppUpdateViewModel(version, service) as T
    }
}
