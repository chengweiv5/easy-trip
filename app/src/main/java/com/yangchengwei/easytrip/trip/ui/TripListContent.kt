package com.yangchengwei.easytrip.trip.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.ui.component.EasyTripPrimaryButton
import com.yangchengwei.easytrip.core.ui.theme.EasyTripSurfaceSoft

@Composable
fun TripListContent(
    state: TripListUiState,
    onAction: (TripListAction) -> Unit,
    modifier: Modifier = Modifier,
    emptyStateModifier: Modifier = Modifier,
) {
    var expandedMenuTripId by rememberSaveable { mutableStateOf<String?>(null) }
    Scaffold(modifier = modifier, containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TripListHeader(Modifier.padding(start = 20.dp, top = 18.dp, end = 20.dp))
            Spacer(Modifier.height(24.dp))
            when (val page = state.page) {
                TripListPageState.Loading -> TripListLoadingState(Modifier.weight(1f))
                TripListPageState.Empty -> EmptyTrips(
                    onCreate = { onAction(TripListAction.CreateTrip) },
                    modifier = Modifier.weight(1f).then(emptyStateModifier),
                )
                is TripListPageState.Error -> TripListErrorState(
                    message = page.message,
                    onRetry = { onAction(TripListAction.Retry) },
                    onCreate = { onAction(TripListAction.CreateTrip) },
                    modifier = Modifier.weight(1f),
                )
                is TripListPageState.Content -> BoxWithConstraints(Modifier.fillMaxSize()) {
                    val fontScale = LocalDensity.current.fontScale
                    val compactHeight = maxHeight < 500.dp ||
                        (fontScale >= 1.5f && maxWidth <= 320.dp && maxHeight <= 700.dp)
                    val singleTripScrollable = page.otherTrips.isEmpty() &&
                        (maxHeight < 620.dp || fontScale >= 1.5f)
                    Box(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 24.dp)) {
                        val contentModifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 72.dp)
                        if (compactHeight || singleTripScrollable) {
                            LazyColumn(
                                modifier = contentModifier.testTag("other-trips-list"),
                                verticalArrangement = Arrangement.spacedBy(20.dp),
                            ) {
                                item(key = "primary-${page.primaryTrip.id}") {
                                    PrimaryTripCard(
                                        trip = page.primaryTrip,
                                        menuExpanded = expandedMenuTripId == page.primaryTrip.id,
                                        onMenuExpandedChange = { expanded ->
                                            expandedMenuTripId = page.primaryTrip.id.takeIf { expanded }
                                        },
                                        onAction = onAction,
                                    )
                                }
                                if (page.otherTrips.isNotEmpty()) {
                                    item(key = "other-trips-heading") {
                                        Text("其他旅行", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    }
                                    items(page.otherTrips, key = TripCardUiModel::id) { trip ->
                                        OtherTripRow(
                                            trip = trip,
                                            menuExpanded = expandedMenuTripId == trip.id,
                                            onMenuExpandedChange = { expanded ->
                                                expandedMenuTripId = trip.id.takeIf { expanded }
                                            },
                                            onAction = onAction,
                                        )
                                    }
                                }
                            }
                        } else {
                            Column(
                                modifier = contentModifier.testTag("trip-content-list"),
                                verticalArrangement = Arrangement.spacedBy(20.dp),
                            ) {
                                PrimaryTripCard(
                                    trip = page.primaryTrip,
                                    menuExpanded = expandedMenuTripId == page.primaryTrip.id,
                                    onMenuExpandedChange = { expanded ->
                                        expandedMenuTripId = page.primaryTrip.id.takeIf { expanded }
                                    },
                                    onAction = onAction,
                                )
                                if (page.otherTrips.isNotEmpty()) {
                                    Text("其他旅行", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    LazyColumn(
                                        modifier = Modifier.weight(1f).testTag("other-trips-list"),
                                        verticalArrangement = Arrangement.spacedBy(20.dp),
                                    ) {
                                        items(page.otherTrips, key = TripCardUiModel::id) { trip ->
                                            OtherTripRow(
                                                trip = trip,
                                                menuExpanded = expandedMenuTripId == trip.id,
                                                onMenuExpandedChange = { expanded ->
                                                    expandedMenuTripId = trip.id.takeIf { expanded }
                                                },
                                                onAction = onAction,
                                            )
                                        }
                                    }
                                } else {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                        EasyTripPrimaryButton(
                            onClick = { onAction(TripListAction.CreateTrip) },
                            modifier = Modifier.align(Alignment.BottomEnd).width(142.dp).height(52.dp).testTag("create-trip"),
                        ) { Text("创建旅行") }
                    }
                }
            }
        }
    }
}

@Composable
private fun TripListHeader(modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().heightIn(min = 69.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("周末，去远一点", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            Text("我的旅行", modifier = Modifier.testTag("trip-list-title"), style = MaterialTheme.typography.headlineLarge)
        }
        Surface(
            modifier = Modifier.size(48.dp).testTag("profile-avatar"),
            shape = CircleShape,
            color = EasyTripSurfaceSoft,
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
            androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                Text("我", fontWeight = FontWeight.Bold)
            }
        }
    }
}
