package com.yangchengwei.easytrip.trip.ui

import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.ui.component.EasyTripPrimaryButton

@Composable
fun TripListContent(
    state: TripListUiState,
    onAction: (TripListAction) -> Unit,
    modifier: Modifier = Modifier,
    emptyStateModifier: Modifier = Modifier,
) {
    var showTraveled by rememberSaveable { mutableStateOf(false) }
    var expandedMenuTripId by rememberSaveable { mutableStateOf<String?>(null) }
    Scaffold(modifier = modifier, containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TripListHeader(Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp))
            Spacer(Modifier.height(16.dp))
            state.statusError?.let { error ->
                Text(error, color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).testTag("trip-status-error"),
                    style = MaterialTheme.typography.bodySmall)
            }
            when (val page = state.page) {
                TripListPageState.Loading -> com.yangchengwei.easytrip.core.ui.component.DeferredLoading { TripListLoadingState(Modifier.weight(1f)) }
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
                is TripListPageState.Content -> Column(Modifier.fillMaxSize()) {
                    TripStatusFilters(
                        trips = page.trips,
                        showTraveled = showTraveled,
                        onSelected = { showTraveled = it; expandedMenuTripId = null },
                    )
                    Spacer(Modifier.height(12.dp))
                    val visibleTrips = page.trips.filter { it.hasTraveled == showTraveled }
                    if (visibleTrips.isEmpty()) {
                        Box(Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, bottom = 20.dp)) {
                            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(if (showTraveled) "暂无已出行的旅行" else "暂无待出行的旅行", style = MaterialTheme.typography.titleMedium)
                                Text(if (showTraveled) "出行后，可在旅行菜单中手动标记" else "创建旅行，开始下一次出发",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            }
                            EasyTripPrimaryButton(onClick = { onAction(TripListAction.CreateTrip) },
                                modifier = Modifier.align(Alignment.BottomEnd).width(132.dp).height(48.dp).testTag("create-trip")) {
                                Text("＋ 创建新旅行")
                            }
                        }
                    } else {
                        val page = TripListPageState.Content(visibleTrips)
                        BoxWithConstraints(Modifier.fillMaxSize()) {
                            val fontScale = LocalDensity.current.fontScale
                            val compactHeight = maxHeight < 500.dp ||
                                (fontScale >= 1.5f && maxWidth <= 320.dp && maxHeight <= 700.dp)
                            val singleTripScrollable = page.otherTrips.isEmpty() &&
                                (maxHeight < 620.dp || fontScale >= 1.5f)
                            Box(Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, bottom = 20.dp)) {
                                val contentModifier = Modifier
                                    .fillMaxSize()
                                    .padding(bottom = 72.dp)
                                if (compactHeight || singleTripScrollable) {
                                    LazyColumn(
                                        modifier = contentModifier.testTag("other-trips-list"),
                                        verticalArrangement = Arrangement.spacedBy(16.dp),
                                    ) {
                                        item(key = "primary-${page.primaryTrip.id}") {
                                            PrimaryTripCard(
                                                trip = page.primaryTrip,
                                                statusUpdating = page.primaryTrip.id in state.updatingTripIds,
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
                                                    statusUpdating = trip.id in state.updatingTripIds,
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
                                        verticalArrangement = Arrangement.spacedBy(16.dp),
                                    ) {
                                        PrimaryTripCard(
                                            trip = page.primaryTrip,
                                            statusUpdating = page.primaryTrip.id in state.updatingTripIds,
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
                                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                            ) {
                                                items(page.otherTrips, key = TripCardUiModel::id) { trip ->
                                                    OtherTripRow(
                                                        trip = trip,
                                                        statusUpdating = trip.id in state.updatingTripIds,
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
                                    modifier = Modifier.align(Alignment.BottomEnd).width(132.dp).height(48.dp).testTag("create-trip"),
                                ) { Text("＋ 创建新旅行") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TripListHeader(modifier: Modifier = Modifier) {
    val openTheme = com.yangchengwei.easytrip.core.ui.theme.LocalThemePicker.current
    Row(modifier.fillMaxWidth().heightIn(min = 64.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("周末，去远一点", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            Text("我的旅行", modifier = Modifier.testTag("trip-list-title"), style = MaterialTheme.typography.headlineLarge)
        }
        Surface(
            onClick = openTheme,
            modifier = Modifier.size(48.dp).testTag("theme-entry").semantics { contentDescription = "主题配色" },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
            androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                com.yangchengwei.easytrip.core.ui.theme.ThemePaletteIcon(Modifier.size(23.dp))
            }
        }
    }
}

@Composable
private fun TripStatusFilters(
    trips: List<TripCardUiModel>,
    showTraveled: Boolean,
    onSelected: (Boolean) -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(false, true).forEach { traveled ->
            val selected = showTraveled == traveled
            Surface(
                modifier = Modifier.weight(1f).heightIn(min = 40.dp)
                    .testTag(if (traveled) "trip-filter-traveled" else "trip-filter-pending")
                    .selectable(selected = selected, role = Role.Tab, onClick = { onSelected(traveled) }),
                shape = RoundedCornerShape(10.dp),
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                Box(Modifier.padding(horizontal = 8.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                    Text("${if (traveled) "已出行" else "待出行"} ${trips.count { it.hasTraveled == traveled }}",
                        style = MaterialTheme.typography.labelLarge, fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
