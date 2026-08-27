package com.yangchengwei.easytrip.trip.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.ui.component.EasyTripPrimaryButton
import com.yangchengwei.easytrip.core.ui.component.EasyTripSecondaryButton
import com.yangchengwei.easytrip.core.ui.component.InlineStatus

@Composable
internal fun EmptyTrips(onCreate: () -> Unit, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val compact = maxHeight < 360.dp
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = if (compact) Arrangement.Top else Arrangement.Center,
        ) {
            if (compact) Spacer(Modifier.height(16.dp))
            LuggageIllustration()
            Spacer(Modifier.height(20.dp))
            Text("开始规划一次旅行", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "创建旅行后，可以收藏地点并按天安排行程",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(24.dp))
            EasyTripPrimaryButton(onClick = onCreate, modifier = Modifier.testTag("create-trip")) {
                Text("创建旅行")
            }
            if (compact) Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
internal fun TripListLoadingState(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(horizontal = 20.dp)) { InlineStatus("正在加载旅行") }
}

@Composable
internal fun TripListErrorState(
    message: String,
    onRetry: () -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
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

@Composable
private fun LuggageIllustration() {
    val color = MaterialTheme.colorScheme.primary
    Canvas(Modifier.size(108.dp).testTag("luggage-illustration")) {
        val stroke = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        drawRoundRect(
            color,
            topLeft = androidx.compose.ui.geometry.Offset(size.width * .2f, size.height * .28f),
            size = androidx.compose.ui.geometry.Size(size.width * .6f, size.height * .58f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()),
            style = stroke,
        )
        val handle = Path().apply {
            moveTo(size.width * .38f, size.height * .28f)
            lineTo(size.width * .38f, size.height * .16f)
            lineTo(size.width * .62f, size.height * .16f)
            lineTo(size.width * .62f, size.height * .28f)
        }
        drawPath(handle, color, style = stroke)
        drawLine(color, androidx.compose.ui.geometry.Offset(size.width * .38f, size.height * .38f), androidx.compose.ui.geometry.Offset(size.width * .38f, size.height * .74f), 3.dp.toPx(), StrokeCap.Round)
        drawLine(color, androidx.compose.ui.geometry.Offset(size.width * .62f, size.height * .38f), androidx.compose.ui.geometry.Offset(size.width * .62f, size.height * .74f), 3.dp.toPx(), StrokeCap.Round)
    }
}
