package com.yangchengwei.easytrip.expense.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yangchengwei.easytrip.itinerary.ui.DayItineraryViewModel
import com.yangchengwei.easytrip.itinerary.ui.EditRouteLegContent

/** Reuses the itinerary route editor and its save path; route categories stay fixed to transport. */
@Composable
fun ExpenseRouteEditor(model: DayItineraryViewModel, legId: String, dayId: String, onBack: () -> Unit) {
    val state by model.state.collectAsStateWithLifecycle()
    var attempted by remember { mutableStateOf(false) }
    var opened by remember { mutableStateOf(false) }
    LaunchedEffect(state.isDayLoaded, state.selectedDayId) {
        if (!attempted && state.isDayLoaded && state.selectedDayId == dayId) {
            attempted = true
            opened = model.requestMode(legId)
        }
    }
    LaunchedEffect(opened, state.modeEditor) { if (opened && state.modeEditor == null) onBack() }
    BackHandler { if (state.modeEditor?.isSaving != true) onBack() }
    Surface(Modifier.fillMaxSize()) {
        val draft = state.modeEditor
        if (draft != null) EditRouteLegContent(draft, model::selectMode, model::clearSelectedModeOverride,
            model::updateRouteDurationMinutes, model::updateRouteNote, model::updateRouteExpense,
            model::saveRouteEditor, onBack, Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp))
        else Column(Modifier.safeDrawingPadding().padding(24.dp)) {
            Text(if (attempted) "路段已变更或当前不可编辑，请返回原旅行查看" else "正在读取路段…")
            TextButton(onBack) { Text("返回") }
        }
    }
}
