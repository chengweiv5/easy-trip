package com.yangchengwei.easytrip.place.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.ui.component.CompactSecondaryButton as TextButton
import com.yangchengwei.easytrip.place.amap.PlaceCandidate

@Composable
fun PlaceSearchScreen(
    state: PlacePoolUiState,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onSelect: (PlaceCandidate) -> Unit,
    onToggleCollection: (PlaceCandidate) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onBack) { Text("返回") }
            PlaceSearchField(state.search.query, onQueryChange)
        }
        PlaceSearchResults(
            state = state.search,
            savedPoiIds = state.savedPoiIds,
            onSelect = onSelect,
            onToggleCollection = onToggleCollection,
            modifier = Modifier.fillMaxWidth().weight(1f),
            collectionBusyPoiIds = state.collectionBusyPoiIds,
        )
    }
}
