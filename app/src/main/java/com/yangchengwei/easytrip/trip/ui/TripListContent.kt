package com.yangchengwei.easytrip.trip.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yangchengwei.easytrip.core.ui.component.EasyTripDangerButton
import com.yangchengwei.easytrip.core.ui.component.EasyTripPrimaryButton
import com.yangchengwei.easytrip.core.ui.component.EasyTripSecondaryButton
import com.yangchengwei.easytrip.core.ui.component.InlineStatus
import com.yangchengwei.easytrip.core.ui.theme.EasyTripBorder
import com.yangchengwei.easytrip.core.ui.theme.EasyTripPrimaryDark
import com.yangchengwei.easytrip.core.ui.theme.EasyTripSurfaceSoft

@Composable
fun TripListContent(
    state: TripListUiState,
    onAction: (TripListAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier, containerColor = MaterialTheme.colorScheme.background) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, top = 18.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item { TripListHeader() }
            when (val page = state.page) {
                TripListPageState.Loading -> item { InlineStatus("正在加载旅行") }
                TripListPageState.Empty -> item {
                    EmptyTrips { onAction(TripListAction.CreateTrip) }
                }
                is TripListPageState.Error -> item {
                    InlineStatus(
                        title = page.message,
                        actionLabel = "重试",
                        onAction = { onAction(TripListAction.Retry) },
                    )
                }
                is TripListPageState.Content -> {
                    item { TripCard(page.primaryTrip, onAction, primary = true) }
                    if (page.otherTrips.isNotEmpty()) {
                        item { Text("其他旅行", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
                        items(page.otherTrips, key = TripCardUiModel::id) { trip ->
                            TripCard(trip, onAction, primary = false)
                        }
                    }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            EasyTripPrimaryButton(
                                onClick = { onAction(TripListAction.CreateTrip) },
                                modifier = Modifier.width(138.dp).testTag("create-trip"),
                            ) { Text("创建旅行") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TripListHeader() {
    Row(Modifier.fillMaxWidth().height(69.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("我的旅行", modifier = Modifier.testTag("trip-list-title"), style = MaterialTheme.typography.headlineLarge)
            Text("把旅程整理好，再从容出发", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        Surface(
            modifier = Modifier.size(48.dp).semantics { contentDescription = "个人中心" },
            shape = CircleShape,
            color = EasyTripSurfaceSoft,
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
            Box(contentAlignment = Alignment.Center) { Text("我", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun EmptyTrips(onCreate: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().height(647.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(124.dp))
        Surface(
            modifier = Modifier.size(56.dp).semantics { contentDescription = "旅行行李插画" },
            shape = RoundedCornerShape(16.dp),
            color = EasyTripSurfaceSoft,
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
            Box(contentAlignment = Alignment.Center) { Text("▣", fontSize = 28.sp, fontWeight = FontWeight.Bold) }
        }
        Spacer(Modifier.height(20.dp))
        Text("还没有旅行计划", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "创建一次旅行，把想去的地点慢慢安排进每天的行程。",
            modifier = Modifier.width(280.dp),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(24.dp))
        EasyTripPrimaryButton(
            onClick = onCreate,
            modifier = Modifier.width(220.dp).testTag("create-trip"),
        ) { Text("创建旅行") }
    }
}

@Composable
private fun TripCard(trip: TripCardUiModel, onAction: (TripListAction) -> Unit, primary: Boolean) {
    val containerColor = if (primary) EasyTripPrimaryDark else MaterialTheme.colorScheme.surface
    val contentColor = if (primary) Color.White else MaterialTheme.colorScheme.onSurface
    Surface(
        modifier = Modifier.fillMaxWidth().then(if (primary) Modifier.testTag("primary-trip-${trip.id}") else Modifier),
        shape = RoundedCornerShape(if (primary) 20.dp else 12.dp),
        color = containerColor,
        contentColor = contentColor,
        border = if (primary) null else BorderStroke(1.dp, EasyTripBorder),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(
                modifier = Modifier.fillMaxWidth().testTag("trip-${trip.id}").semantics { contentDescription = "打开旅行 ${trip.name}" }
                    .clickable(role = Role.Button) { onAction(TripListAction.OpenTrip(trip.id)) },
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(trip.name, style = if (primary) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TripMetadata(trip.dayCountLabel, primary)
                    TripMetadata(trip.travelModeLabel, primary)
                }
                trip.dateLabel?.let { TripMetadata(it, primary) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EasyTripSecondaryButton(
                    onClick = { onAction(TripListAction.OpenSettings(trip.id)) },
                    modifier = Modifier.weight(1f).testTag("trip-settings-${trip.id}").semantics { contentDescription = "设置 ${trip.name}" },
                ) { Text("设置") }
                EasyTripDangerButton(
                    onClick = { onAction(TripListAction.RequestDelete(trip.id)) },
                    modifier = Modifier.weight(1f).testTag("trip-delete-${trip.id}").semantics { contentDescription = "删除 ${trip.name}" },
                ) { Text("删除") }
            }
        }
    }
}

@Composable
private fun TripMetadata(label: String, primary: Boolean) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (primary) Color.White.copy(alpha = 0.14f) else EasyTripSurfaceSoft,
        contentColor = if (primary) Color.White else MaterialTheme.colorScheme.onSurface,
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium)
    }
}
