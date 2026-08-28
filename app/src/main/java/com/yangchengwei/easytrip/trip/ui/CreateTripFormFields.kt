package com.yangchengwei.easytrip.trip.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.core.ui.component.CompactSecondaryButton
import com.yangchengwei.easytrip.core.ui.component.SelectablePill
import com.yangchengwei.easytrip.core.ui.theme.EasyTripBorder
import com.yangchengwei.easytrip.core.ui.theme.EasyTripPrimary
import com.yangchengwei.easytrip.core.ui.theme.EasyTripPrimaryDark
import com.yangchengwei.easytrip.core.ui.theme.EasyTripSecondary
import com.yangchengwei.easytrip.core.ui.theme.EasyTripSurface
import com.yangchengwei.easytrip.core.ui.theme.EasyTripSurfaceSoft
import java.time.Instant
import java.time.ZoneOffset

internal data class CreateTripDatePickerPalette(
    val container: Color,
    val content: Color,
    val secondaryContent: Color,
    val primary: Color,
    val onPrimary: Color,
    val outline: Color,
)

internal fun createTripDatePickerPalette() = CreateTripDatePickerPalette(
    container = EasyTripSurface,
    content = EasyTripPrimaryDark,
    secondaryContent = EasyTripSecondary,
    primary = EasyTripPrimary,
    onPrimary = EasyTripSurface,
    outline = EasyTripBorder,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EasyTripDatePicker(
    state: DatePickerState,
    modifier: Modifier = Modifier,
) {
    val palette = createTripDatePickerPalette()
    DatePicker(
        state = state,
        modifier = modifier,
        colors = DatePickerDefaults.colors(
            containerColor = palette.container,
            titleContentColor = palette.content,
            headlineContentColor = palette.content,
            weekdayContentColor = palette.secondaryContent,
            subheadContentColor = palette.secondaryContent,
            navigationContentColor = palette.primary,
            yearContentColor = palette.content,
            currentYearContentColor = palette.primary,
            selectedYearContentColor = palette.onPrimary,
            selectedYearContainerColor = palette.primary,
            dayContentColor = palette.content,
            selectedDayContentColor = palette.onPrimary,
            selectedDayContainerColor = palette.primary,
            todayContentColor = palette.primary,
            todayDateBorderColor = palette.outline,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CreateTripFormFields(
    state: CreateTripUiState,
    onAction: (CreateTripAction) -> Unit,
    initialDateMillis: Long?,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }
    val enabled = !state.isSubmitting

    Column(modifier, verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Column(
            Modifier.testTag("create-name-container"),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("旅行名称", style = MaterialTheme.typography.labelLarge)
            BrandedField(
                value = state.name,
                onValueChange = { onAction(CreateTripAction.NameChanged(it)) },
                placeholder = "例如：杭州 · 春日慢游",
                enabled = enabled,
                isError = state.nameError != null,
                errorMessage = state.nameError,
                fieldTag = "create-name",
                modifier = Modifier,
            )
            Text(
                state.nameError ?: "一个容易辨认的名称，稍后可以修改",
                color = if (state.nameError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Column(
            Modifier.testTag("create-date-control-container"),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("出行日期", style = MaterialTheme.typography.labelLarge)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(66.dp)
                    .testTag("create-date-control")
                    .testTag("create-date-row")
                    .errorSemantics(state.dateError)
                    .semantics {
                        contentDescription = dateSelectionDescription(state)
                        role = Role.Button
                    }
                    .clickable(enabled = enabled) {
                        if (state.timeMode == CreateTimeMode.DRAFT) {
                            onAction(CreateTripAction.TimeModeChanged(CreateTimeMode.DATED))
                        }
                        showPicker = true
                    },
                shape = RoundedCornerShape(10.dp),
                color = EasyTripSurface,
                border = BorderStroke(if (state.dateError == null) 1.dp else 2.dp, if (state.dateError == null) EasyTripBorder else MaterialTheme.colorScheme.error),
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DateFieldIcon(Modifier.size(36.dp).testTag("create-date-leading-icon"))
                    Column(Modifier.weight(1f).testTag("create-date-text")) {
                        Text(
                            if (state.timeMode == CreateTimeMode.DRAFT) "日期待定" else state.startDate?.toString() ?: "选择出行日期",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        if (state.timeMode == CreateTimeMode.DATED) {
                            Text(state.endDate?.toString() ?: "选择日期后会自动计算行程天数", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    DateChevron(Modifier.size(20.dp))
                }
            }
            Row(Modifier.selectableGroup()) {
                SelectablePill(
                    selected = state.timeMode == CreateTimeMode.DRAFT,
                    onClick = { onAction(CreateTripAction.TimeModeChanged(CreateTimeMode.DRAFT)) },
                    label = { Text("日期待定") },
                    modifier = Modifier.testTag("create-time-DRAFT"),
                    enabled = enabled,
                    role = Role.RadioButton,
                )
                SelectablePill(
                    selected = state.timeMode == CreateTimeMode.DATED,
                    onClick = { onAction(CreateTripAction.TimeModeChanged(CreateTimeMode.DATED)); showPicker = true },
                    label = { Text("指定日期") },
                    modifier = Modifier.testTag("create-time-DATED"),
                    enabled = enabled,
                    role = Role.RadioButton,
                )
            }
            state.dateError?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("create-date-error"),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Column(
            Modifier.testTag("create-day-count-container"),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("旅行天数", style = MaterialTheme.typography.labelLarge)
            BrandedField(
                value = state.dayCount,
                onValueChange = { onAction(CreateTripAction.DayCountChanged(it)) },
                placeholder = "至少 1 天",
                enabled = enabled,
                isError = state.dayCountError != null,
                errorMessage = state.dayCountError,
                fieldTag = "create-day-count",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier,
            )
            state.dayCountError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("出行方式", style = MaterialTheme.typography.labelLarge)
            Row(
                Modifier.fillMaxWidth().height(82.dp).selectableGroup().testTag("create-mode-options"),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TravelModeCard(
                    mode = TravelMode.FLEXIBLE,
                    selected = state.travelMode == TravelMode.FLEXIBLE,
                    onClick = { onAction(CreateTripAction.TravelModeChanged(TravelMode.FLEXIBLE)) },
                    modifier = Modifier.weight(1f).testTag("create-mode-FLEXIBLE"),
                    enabled = enabled,
                )
                TravelModeCard(
                    mode = TravelMode.SELF_DRIVE,
                    selected = state.travelMode == TravelMode.SELF_DRIVE,
                    onClick = { onAction(CreateTripAction.TravelModeChanged(TravelMode.SELF_DRIVE)) },
                    modifier = Modifier.weight(1f).testTag("create-mode-SELF_DRIVE"),
                    enabled = enabled,
                )
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth().testTag("create-planning-tip"),
            color = EasyTripSurfaceSoft,
            shape = RoundedCornerShape(10.dp),
        ) {
            Box(Modifier.padding(horizontal = 12.dp, vertical = 11.dp), contentAlignment = Alignment.CenterStart) {
                Text(
                    "创建后先收藏感兴趣的地点，再从地点池添加到每天的行程。",
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }

    if (showPicker && enabled) {
        val millis = state.startDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli() ?: initialDateMillis
        val picker = rememberDatePickerState(initialSelectedDateMillis = millis, initialDisplayedMonthMillis = millis)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                CompactSecondaryButton(
                    onClick = {
                        picker.selectedDateMillis?.let {
                            onAction(CreateTripAction.StartDateChanged(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()))
                        }
                        showPicker = false
                    },
                    modifier = Modifier.testTag("create-date-confirm"),
                ) { Text("确定日期") }
            },
        ) {
            EasyTripDatePicker(
                state = picker,
                modifier = Modifier.testTag("create-date-picker"),
            )
        }
    }
}

@Composable
private fun BrandedField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    enabled: Boolean,
    isError: Boolean,
    errorMessage: String?,
    fieldTag: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val border = when {
        isError -> MaterialTheme.colorScheme.error
        focused -> MaterialTheme.colorScheme.primary
        else -> EasyTripBorder
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .border(BorderStroke(if (isError || focused) 2.dp else 1.dp, border), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.CenterStart,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(fieldTag)
                .errorSemantics(errorMessage),
            placeholder = { Text(placeholder) },
            leadingIcon = {
                NameFieldIcon(Modifier.size(20.dp).testTag("create-name-leading-icon"))
            },
            enabled = enabled,
            isError = isError,
            singleLine = true,
            keyboardOptions = keyboardOptions,
            interactionSource = interactionSource,
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                errorBorderColor = Color.Transparent,
                disabledBorderColor = Color.Transparent,
            ),
        )
    }
}

@Composable
private fun NameFieldIcon(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        val stroke = Stroke(1.8.dp.toPx())
        drawCircle(color, radius = size.minDimension * .2f, center = Offset(size.width / 2, size.height * .34f), style = stroke)
        drawArc(color, 200f, 140f, false, Offset(size.width * .18f, size.height * .43f), androidx.compose.ui.geometry.Size(size.width * .64f, size.height * .48f), style = stroke)
    }
}

@Composable
private fun DateFieldIcon(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        val stroke = Stroke(1.8.dp.toPx())
        drawRoundRect(color, Offset(size.width * .15f, size.height * .2f), androidx.compose.ui.geometry.Size(size.width * .7f, size.height * .65f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()), style = stroke)
        drawLine(color, Offset(size.width * .15f, size.height * .42f), Offset(size.width * .85f, size.height * .42f), 1.8.dp.toPx())
    }
}

@Composable
private fun DateChevron(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier) {
        drawLine(color, Offset(size.width * .28f, size.height * .4f), Offset(size.width * .5f, size.height * .62f), 1.8.dp.toPx())
        drawLine(color, Offset(size.width * .5f, size.height * .62f), Offset(size.width * .72f, size.height * .4f), 1.8.dp.toPx())
    }
}

@Composable
fun TravelModeCard(
    mode: TravelMode,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        modifier = modifier
            .height(82.dp)
            .selectable(
                selected = selected,
                onClick = onClick,
                enabled = enabled,
                role = Role.RadioButton,
                interactionSource = interactionSource,
            ),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        border = if (selected) null else BorderStroke(1.dp, EasyTripBorder),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(if (mode == TravelMode.FLEXIBLE) "灵活" else "自驾", style = MaterialTheme.typography.titleMedium)
        }
    }
}

private fun dateSelectionDescription(state: CreateTripUiState): String =
    "选择出行日期，当前范围：" + when (state.timeMode) {
        CreateTimeMode.DRAFT -> "日期待定"
        CreateTimeMode.DATED -> "${state.startDate ?: "未选择开始日期"} 至 ${state.endDate ?: "未选择结束日期"}"
    }

private fun Modifier.errorSemantics(message: String?): Modifier =
    if (message == null) this else semantics { error(message) }
