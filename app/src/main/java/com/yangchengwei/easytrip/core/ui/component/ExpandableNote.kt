package com.yangchengwei.easytrip.core.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun ExpandableNote(note: String?, identity: String, modifier: Modifier = Modifier) {
    val text = note?.trim()?.takeIf(String::isNotEmpty) ?: return
    var expanded by rememberSaveable(identity, text) { mutableStateOf(false) }
    var overflows by remember(identity, text) { mutableStateOf(false) }
    Column(modifier.fillMaxWidth().testTag("note-$identity")) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (expanded) Int.MAX_VALUE else 2,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { if (!expanded) overflows = it.hasVisualOverflow },
            modifier = Modifier.fillMaxWidth().testTag("note-text-$identity"),
        )
        if (expanded || overflows) {
            Text(
                text = if (expanded) "收起" else "展开",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .testTag("note-toggle-$identity")
                    .clickable(role = Role.Button, onClickLabel = if (expanded) "收起备注" else "展开备注") {
                        expanded = !expanded
                    }
                    .padding(vertical = 6.dp, horizontal = 2.dp),
            )
        }
    }
}
