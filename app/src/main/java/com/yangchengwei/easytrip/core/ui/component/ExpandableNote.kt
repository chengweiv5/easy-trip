package com.yangchengwei.easytrip.core.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.ui.theme.EasyTripNote

@Composable
fun ExpandableNote(
    note: String?,
    identity: String,
    modifier: Modifier = Modifier,
    textStartPadding: Dp = 0.dp,
    header: (@Composable RowScope.() -> Unit)? = null,
) {
    val text = note?.trim().orEmpty()
    if (text.isEmpty() && header == null) return
    var expanded by rememberSaveable(identity, text) { mutableStateOf(false) }
    val singleLinePreview = remember(text) { text.lineSequence().joinToString(" ") { it.trim() } }
    val textMeasurer = rememberTextMeasurer()
    val style = MaterialTheme.typography.bodySmall
    val textStartPaddingPx = with(LocalDensity.current) { textStartPadding.roundToPx() }
    BoxWithConstraints(modifier.fillMaxWidth()) {
        // Measure before reserving the action's width so it disappears when the full note fits.
        val overflows = text.isNotEmpty() && textMeasurer.measure(
            text = text,
            style = style,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            constraints = Constraints(maxWidth = (constraints.maxWidth - textStartPaddingPx).coerceAtLeast(0)),
        ).hasVisualOverflow
        if (header != null) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    header()
                    if (overflows) {
                        NoteToggle(identity, expanded, { expanded = !expanded }, Modifier.alignByBaseline())
                    }
                }
                if (text.isNotEmpty()) {
                    Text(
                        text = if (expanded) text else singleLinePreview,
                        style = style,
                        color = EasyTripNote,
                        maxLines = if (expanded) Int.MAX_VALUE else 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth().padding(start = textStartPadding)
                            .testTag("note-text-$identity"),
                    )
                }
            }
            return@BoxWithConstraints
        }
        Row(Modifier.fillMaxWidth().testTag("note-$identity"), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = text,
                style = style,
                color = EasyTripNote,
                maxLines = if (expanded) Int.MAX_VALUE else 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).alignByBaseline().testTag("note-text-$identity"),
            )
            if (overflows) {
                NoteToggle(
                    identity, expanded, { expanded = !expanded },
                    Modifier.alignByBaseline().padding(horizontal = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun NoteToggle(identity: String, expanded: Boolean, onToggle: () -> Unit, modifier: Modifier) {
    Text(
        text = if (expanded) "收起" else "展开",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
        softWrap = false,
        modifier = modifier.testTag("note-toggle-$identity")
            .clickable(role = Role.Button, onClickLabel = if (expanded) "收起备注" else "展开备注", onClick = onToggle),
    )
}
