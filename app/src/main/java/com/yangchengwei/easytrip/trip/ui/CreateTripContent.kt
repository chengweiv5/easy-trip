package com.yangchengwei.easytrip.trip.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.ui.component.EasyTripPrimaryButton
import com.yangchengwei.easytrip.core.ui.theme.EasyTripBorder
import com.yangchengwei.easytrip.core.ui.theme.EasyTripSurfaceSoft
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneOffset

private fun initialCreateTripMonth(initialDateMillis: Long?): YearMonth? = initialDateMillis?.let {
    Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().let(YearMonth::from)
}


@Composable
fun CreateTripContent(
    state: CreateTripUiState,
    onAction: (CreateTripAction) -> Unit,
    modifier: Modifier = Modifier,
    initialDateMillis: Long? = null,
) {
    val enabled = !state.isSubmitting
    var showRangePicker by remember { mutableStateOf(false) }
    BackHandler(enabled = state.isSubmitting) {}
    val scrollState = rememberScrollState()
    TripDateRangePickerModalHost(
        sheetVisible = showRangePicker && enabled,
        background = {
            Column(
                modifier = modifier.fillMaxSize().safeDrawingPadding().imePadding(),
            ) {
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(22.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth().height(58.dp).testTag("create-header"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("create-back")
                                .clickable(enabled = enabled, role = Role.Button) { onAction(CreateTripAction.Back) },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, EasyTripBorder),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("←", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                        Column {
                            Text("创建旅行", style = MaterialTheme.typography.headlineMedium)
                            Text(
                                "先确定基本信息，之后再慢慢规划",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StepNumber("1", true, Modifier.testTag("create-step-1"))
                        Text("基本信息", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.width(28.dp).height(1.dp).then(Modifier))
                        StepNumber("2", false)
                        Text("开始规划", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                    }
                    CreateTripFormFields(
                        state = state,
                        onAction = onAction,
                        onOpenDateRangePicker = { showRangePicker = true },
                    )
                    Column(
                        Modifier.fillMaxWidth().testTag("create-actions"),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        state.submitError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        EasyTripPrimaryButton(
                            onClick = { onAction(CreateTripAction.Submit) },
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("create-submit"),
                            enabled = enabled,
                        ) { Text(if (state.isSubmitting) "创建中…" else "创建旅行") }
                        Text(
                            if (state.nameError != null || state.dateError != null) "修正标红字段后即可创建旅行" else "地点与日期都可以稍后调整",
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            color = if (state.nameError != null || state.dateError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        },
        sheet = {
            TripDateRangePickerSheet(
                initialSelection = DateRangeSelection(state.startDate, state.endDate),
                initialDisplayedMonth = initialCreateTripMonth(initialDateMillis),
                onConfirm = { selection ->
                    onAction(CreateTripAction.DateRangeChanged(selection.startDate!!, selection.endDate!!))
                    showRangePicker = false
                },
                onDismiss = { showRangePicker = false },
                modifier = Modifier.testTag("create-date-range-picker"),
            )
        },
    )
}

@Composable
private fun StepNumber(value: String, active: Boolean, modifier: Modifier = Modifier) {
    Surface(
        modifier.size(22.dp),
        shape = CircleShape,
        color = if (active) MaterialTheme.colorScheme.primary else EasyTripSurfaceSoft,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                value,
                color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}
