package com.yangchengwei.easytrip.trip.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.ui.component.EasyTripPrimaryButton
import com.yangchengwei.easytrip.core.ui.component.EasyTripSecondaryButton
import com.yangchengwei.easytrip.core.ui.component.EmptyIllustration
import com.yangchengwei.easytrip.core.ui.component.EmptyState
import com.yangchengwei.easytrip.core.ui.component.InlineStatus

@Composable
internal fun EmptyTrips(onCreate: () -> Unit, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxWidth().testTag("empty-trips")) {
        val compact = maxHeight < 360.dp
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = if (compact) Arrangement.Top else Arrangement.Center,
        ) {
            if (compact) Spacer(Modifier.height(16.dp))
            EmptyState(
                title = "开始规划一次旅行",
                message = "创建旅行后，可以收藏地点并按天安排行程",
                emptyIllustration = EmptyIllustration.Trips,
                modifier = Modifier.padding(vertical = 0.dp),
                action = {
                    EasyTripPrimaryButton(onClick = onCreate, modifier = Modifier.testTag("create-trip")) {
                        Text("创建旅行")
                    }
                },
            )
            if (compact) Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
internal fun TripListLoadingState(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp)) { InlineStatus("正在加载旅行") }
}

@Composable
internal fun TripListErrorState(
    message: String,
    onRetry: () -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        EasyTripSecondaryButton(onClick = onRetry, modifier = Modifier.testTag("retry-trips")) { Text("重试") }
        Spacer(Modifier.height(12.dp))
        EasyTripPrimaryButton(onClick = onCreate, modifier = Modifier.testTag("create-trip")) { Text("创建旅行") }
    }
}
