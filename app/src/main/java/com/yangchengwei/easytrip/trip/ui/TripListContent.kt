package com.yangchengwei.easytrip.trip.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.ui.component.EasyTripPrimaryButton
import com.yangchengwei.easytrip.core.ui.component.EasyTripSecondaryButton
import com.yangchengwei.easytrip.core.ui.component.FeedbackKind
import com.yangchengwei.easytrip.core.ui.component.FeedbackState
import com.yangchengwei.easytrip.core.ui.theme.EasyTripBorder
import com.yangchengwei.easytrip.core.ui.theme.EasyTripSurfaceSoft
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme

@Composable
fun TripListContent(
    state: TripListUiState,
    onAction: (TripListAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            EasyTripPrimaryButton(
                onClick = { onAction(TripListAction.CreateTrip) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .testTag("create-trip"),
            ) { Text("创建旅行") }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            Text("我的旅行", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(EasyTripTheme.spacing.large))
            when (val page = state.page) {
                TripListPageState.Loading -> FeedbackState(FeedbackKind.LOADING, "正在加载旅行")
                TripListPageState.Empty -> FeedbackState(
                    kind = FeedbackKind.EMPTY,
                    title = "还没有旅行",
                    message = "创建一次旅行，从地图和地点开始规划。",
                )
                is TripListPageState.Error -> FeedbackState(
                    kind = FeedbackKind.ERROR,
                    title = page.message,
                    actionLabel = "重试",
                    onAction = { onAction(TripListAction.Retry) },
                )
                is TripListPageState.Content -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(EasyTripTheme.spacing.medium),
                ) {
                    items(page.trips, key = TripCardUiModel::id) { trip ->
                        TripCard(trip, onAction)
                    }
                }
            }
        }
    }
}

@Composable
private fun TripCard(trip: TripCardUiModel, onAction: (TripListAction) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, EasyTripBorder),
        shadowElevation = EasyTripTheme.elevation.card,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(EasyTripTheme.spacing.medium),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("trip-${trip.id}")
                    .semantics { contentDescription = "打开旅行 ${trip.name}" }
                    .clickable(role = Role.Button) { onAction(TripListAction.OpenTrip(trip.id)) },
                verticalArrangement = Arrangement.spacedBy(EasyTripTheme.spacing.medium),
            ) {
                Text(
                    trip.name,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(EasyTripTheme.spacing.small)) {
                    TripMetadata(trip.dayCountLabel)
                    TripMetadata(trip.travelModeLabel)
                    trip.dateLabel?.let { TripMetadata(it) }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(EasyTripTheme.spacing.small, Alignment.End),
            ) {
                EasyTripSecondaryButton(
                    onClick = { onAction(TripListAction.OpenSettings(trip.id)) },
                    modifier = Modifier
                        .testTag("trip-settings-${trip.id}")
                        .semantics { contentDescription = "设置 ${trip.name}" },
                ) { Text("设置") }
                EasyTripSecondaryButton(
                    onClick = { onAction(TripListAction.RequestDelete(trip.id)) },
                    modifier = Modifier
                        .testTag("trip-delete-${trip.id}")
                        .semantics { contentDescription = "删除 ${trip.name}" },
                ) { Text("删除") }
            }
        }
    }
}

@Composable
private fun TripMetadata(label: String) {
    Surface(shape = RoundedCornerShape(18.dp), color = EasyTripSurfaceSoft) {
        Text(label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), style = MaterialTheme.typography.labelMedium)
    }
}
