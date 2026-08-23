package com.yangchengwei.easytrip.workspace

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import com.yangchengwei.easytrip.amap.AmapConsentToken
import com.yangchengwei.easytrip.place.amap.PlaceCandidate

@Composable
fun TripWorkspaceRoute(
    viewModel: TripWorkspaceViewModel,
    consent: AmapConsentToken?,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    onPrivacySettings: () -> Unit = {},
    onOpenSearch: () -> Unit = {},
    placeContent: @Composable () -> Unit,
    dayItineraryContent: @Composable () -> Unit,
    isPoiSaved: Boolean = false,
    collectionBusyPoiIds: Set<String> = emptySet(),
    collectionError: String? = null,
    onTogglePoiCollection: (PlaceCandidate) -> Unit = {},
    mapHostFactory: (android.content.Context) -> AmapMapHost = { RealAmapMapHost.create(it) },
) {
    BackHandler {
        if (!viewModel.handleBack()) onBack()
    }
    TripWorkspaceScreen(
        viewModel = viewModel,
        consent = consent,
        onBack = onBack,
        onSettings = onSettings,
        onPrivacySettings = onPrivacySettings,
        onOpenSearch = onOpenSearch,
        placeContent = placeContent,
        dayItineraryContent = dayItineraryContent,
        isPoiSaved = isPoiSaved,
        collectionBusyPoiIds = collectionBusyPoiIds,
        collectionError = collectionError,
        onTogglePoiCollection = onTogglePoiCollection,
        mapHostFactory = mapHostFactory,
    )
}
