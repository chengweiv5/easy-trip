package com.yangchengwei.easytrip.place.ui

import androidx.compose.runtime.Composable
import com.yangchengwei.easytrip.place.amap.PlaceCandidate

@Deprecated("Use PlaceSearchRoute and PlaceSearchContent")
@Composable
fun PlaceSearchScreen(
    state: PlacePoolUiState,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onSelect: (PlaceCandidate) -> Unit,
    onToggleCollection: (PlaceCandidate) -> Unit,
) {
    PlaceSearchContent(
        state = PlaceSearchUiState(
            search = state.search,
            savedPoiIds = state.savedPoiIds,
            collectionBusyPoiIds = state.collectionBusyPoiIds,
            pendingCollectionRemoval = state.pendingCollectionRemoval,
            collectionError = state.collectionError,
        ),
        onAction = { action ->
            when (action) {
                PlaceSearchAction.Back -> onBack()
                is PlaceSearchAction.QueryChanged -> onQueryChange(action.value)
                is PlaceSearchAction.ToggleCollection -> state.search.results
                    .firstOrNull { it.poiId == action.poiId }
                    ?.let(onToggleCollection)
                PlaceSearchAction.Submit,
                PlaceSearchAction.Retry,
                PlaceSearchAction.DismissRemovalConfirmation,
                PlaceSearchAction.ConfirmRemoval -> Unit
            }
        },
    )
}
