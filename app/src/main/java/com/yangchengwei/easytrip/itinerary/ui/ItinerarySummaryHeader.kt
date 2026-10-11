package com.yangchengwei.easytrip.itinerary.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun ItinerarySummaryHeader(
    text: String,
    modifier: Modifier = Modifier,
    date: String? = null,
    expenseLabel: String? = null,
    onExpenseClick: (() -> Unit)? = null,
    trailingAction: (@Composable () -> Unit)? = null,
    trailingInset: Dp = 0.dp,
    inlineDate: Boolean = true,
) {
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = 44.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            if (inlineDate) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val lineStyle = MaterialTheme.typography.bodySmall.copy(
                        lineHeight = 24.sp,
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Center,
                            trim = LineHeightStyle.Trim.None,
                        ),
                    )
                    Text(
                        text = text,
                        style = lineStyle.copy(
                            fontSize = 16.sp,
                            fontWeight = MaterialTheme.typography.titleMedium.fontWeight,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false).semantics { heading() },
                    )
                    date?.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.size(8.dp))
                        Text(
                            text = it,
                            style = lineStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            } else {
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() },
                )
            }
            expenseLabel?.let {
                Text(
                    text = if (onExpenseClick == null) it else "$it ›",
                    modifier = if (onExpenseClick == null) Modifier else
                        Modifier.clickable(onClick = onExpenseClick).padding(vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (inlineDate) 1 else Int.MAX_VALUE,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (!inlineDate) {
                date?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        trailingAction?.let {
            Box(
                Modifier.padding(start = 4.dp),
                contentAlignment = Alignment.Center,
            ) { it() }
            if (trailingInset > 0.dp) Spacer(Modifier.size(trailingInset))
        }
    }
}
