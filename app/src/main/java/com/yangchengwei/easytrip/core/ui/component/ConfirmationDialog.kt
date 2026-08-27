package com.yangchengwei.easytrip.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yangchengwei.easytrip.core.ui.theme.EasyTripBackground

@Composable
fun ConfirmationDialog(
    model: ConfirmationUiModel,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    busy: Boolean = false,
    errorMessage: String? = null,
) {
    Dialog(
        onDismissRequest = { if (!busy) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !busy,
            dismissOnClickOutside = !busy,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Surface(
            modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp).widthIn(max = 334.dp).heightIn(max = 640.dp),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            shadowElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Column(
                    modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Surface(
                        modifier = Modifier.size(52.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                        contentColor = MaterialTheme.colorScheme.error,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("!", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(model.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text(model.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), color = EasyTripBackground) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            ConfirmationSection("将删除", model.deletedItems, MaterialTheme.colorScheme.error)
                            ConfirmationSection("将保留", model.retainedItems, MaterialTheme.colorScheme.primary)
                        }
                    }
                    errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    if (busy) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            CircularProgressIndicator(Modifier.size(18.dp))
                            Text("处理中…", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    EasyTripSecondaryButton(
                        onClick = onDismiss,
                        enabled = !busy,
                        modifier = Modifier.weight(1f).height(44.dp).testTag("confirmation-dismiss"),
                    ) { Text(model.dismissLabel) }
                    if (model.destructive) {
                        EasyTripDangerButton(
                            onClick = onConfirm,
                            enabled = !busy,
                            modifier = Modifier.weight(1f).height(44.dp).testTag("confirmation-confirm"),
                        ) { Text(model.confirmLabel) }
                    } else {
                        EasyTripPrimaryButton(
                            onClick = onConfirm,
                            enabled = !busy,
                            modifier = Modifier.weight(1f).height(44.dp).testTag("confirmation-confirm"),
                        ) { Text(model.confirmLabel) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfirmationSection(title: String, items: List<String>, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.SemiBold)
        items.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
}
