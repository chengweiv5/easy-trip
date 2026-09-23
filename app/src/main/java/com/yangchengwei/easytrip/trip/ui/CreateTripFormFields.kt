package com.yangchengwei.easytrip.trip.ui

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.model.TravelMode

@Composable
internal fun CreateTripFormFields(
    state: CreateTripUiState,
    onAction: (CreateTripAction) -> Unit,
    onOpenDateRangePicker: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabled = !state.isSubmitting

    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
                    .height(56.dp)
                    .testTag("create-date-control")
                    .testTag("create-date-row")
                    .errorSemantics(state.dateError)
                    .semantics {
                        contentDescription = dateSelectionDescription(state)
                        role = Role.Button
                    }
                    .clickable(enabled = enabled) { onOpenDateRangePicker() },
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(if (state.dateError == null) 1.dp else 2.dp, if (state.dateError == null) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.error),
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DateFieldIcon(Modifier.size(36.dp).testTag("create-date-leading-icon"))
                    Column(Modifier.weight(1f).testTag("create-date-text")) {
                        Text(
                            state.startDate?.toString() ?: "选择开始和结束日期",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            state.endDate?.let { endDate ->
                                val duration = state.dayCount?.let { days -> "${days}天${days - 1}晚" } ?: "范围无效"
                                "至 $endDate · $duration"
                            } ?: "完整范围后自动计算行程天数",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    DateChevron(Modifier.size(20.dp))
                }
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
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("出行方式", style = MaterialTheme.typography.labelLarge)
            Row(
                Modifier.fillMaxWidth().height(72.dp).selectableGroup().testTag("create-mode-options"),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
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
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(8.dp),
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
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val border = when {
        isError -> MaterialTheme.colorScheme.error
        focused -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .border(BorderStroke(if (isError || focused) 2.dp else 1.dp, border), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag(fieldTag).errorSemantics(errorMessage),
            enabled = enabled,
            singleLine = true,
            interactionSource = interactionSource,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
            decorationBox = { inner ->
                Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NameFieldIcon(Modifier.size(20.dp).testTag("create-name-leading-icon"))
                    Box(Modifier.weight(1f)) {
                        if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        inner()
                    }
                }
            },
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
            .height(72.dp)
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
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            TravelModeSymbol(mode, Modifier.size(18.dp))
            Text(if (mode == TravelMode.FLEXIBLE) "灵活出行" else "自驾", style = MaterialTheme.typography.labelMedium)
            Text(if (mode == TravelMode.FLEXIBLE) "步行与公共交通" else "以驾车为主", style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun dateSelectionDescription(state: CreateTripUiState): String =
    "选择出行日期，当前范围：${state.startDate ?: "未选择开始日期"} 至 ${state.endDate ?: "未选择结束日期"}"

private fun Modifier.errorSemantics(message: String?): Modifier =
    if (message == null) this else semantics { error(message) }

@Composable
internal fun TravelModeSymbol(mode: TravelMode, modifier: Modifier = Modifier) {
    val color = androidx.compose.material3.LocalContentColor.current
    Canvas(modifier) {
        val stroke = Stroke(1.3.dp.toPx())
        if (mode == TravelMode.FLEXIBLE) {
            drawCircle(color, radius = size.minDimension * .4f, style = stroke)
            drawLine(color, Offset(size.width * .65f, size.height * .3f), Offset(size.width * .35f, size.height * .7f), 1.5.dp.toPx())
        } else {
            drawRoundRect(color, Offset(size.width * .1f, size.height * .35f), androidx.compose.ui.geometry.Size(size.width * .8f, size.height * .4f), androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()), style = stroke)
            drawLine(color, Offset(size.width * .22f, size.height * .35f), Offset(size.width * .32f, size.height * .16f), 1.3.dp.toPx())
            drawLine(color, Offset(size.width * .32f, size.height * .16f), Offset(size.width * .68f, size.height * .16f), 1.3.dp.toPx())
            drawLine(color, Offset(size.width * .68f, size.height * .16f), Offset(size.width * .78f, size.height * .35f), 1.3.dp.toPx())
            drawCircle(color, size.width * .06f, Offset(size.width * .25f, size.height * .6f))
            drawCircle(color, size.width * .06f, Offset(size.width * .75f, size.height * .6f))
        }
    }
}
