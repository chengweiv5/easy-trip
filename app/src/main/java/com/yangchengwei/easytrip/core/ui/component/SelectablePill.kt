package com.yangchengwei.easytrip.core.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme

data class SelectablePillStyle(
    val filled: Boolean,
    val border: BorderStroke?,
)

fun selectablePillStyle(selected: Boolean) = SelectablePillStyle(
    filled = selected,
    border = null,
)

@Composable
fun SelectablePill(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    role: Role? = null,
) {
    val style = selectablePillStyle(selected)
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = EasyTripTheme.sizes.pillHeight)
            .selectable(
                selected = selected,
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ripple(),
                role = role,
                onClick = onClick,
            ),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.height(EasyTripTheme.sizes.pillHeight),
            shape = RoundedCornerShape(18.dp),
            color = if (style.filled) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
            contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            border = style.border,
        ) {
            Box(Modifier.padding(horizontal = 16.dp), contentAlignment = androidx.compose.ui.Alignment.Center) { label() }
        }
    }
}
