package com.yangchengwei.easytrip.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.yangchengwei.easytrip.core.ui.theme.EasyTripBackground
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme

@Composable
fun ConfirmationDialog(
    model: ConfirmationUiModel,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    busy: Boolean = false,
    dismissible: Boolean = !busy,
    dismissEnabled: Boolean = !busy,
    showDismissAction: Boolean = true,
    errorMessage: String? = null,
    confirmLabel: String = model.confirmLabel,
    deletedItemTags: List<String>? = null,
    retainedItemTags: List<String>? = null,
) {
    EasyTripDialogSurface(
        onDismiss = onDismiss,
        modifier = modifier,
        dismissible = dismissible,
        width = EasyTripTheme.sizes.dialogWidth,
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = EasyTripTheme.spacing.dialogContentHorizontal,
                vertical = EasyTripTheme.spacing.dialogContentVertical,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(EasyTripTheme.spacing.dialogSectionGap),
        ) {
                Column(
                    modifier = Modifier,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(EasyTripTheme.spacing.dialogSectionGap),
                ) {
                    Surface(
                        modifier = Modifier.size(36.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                        contentColor = MaterialTheme.colorScheme.error,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("!", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(model.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text(model.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    if (model.deletedItems.isNotEmpty() || model.retainedItems.isNotEmpty()) {
                        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(EasyTripTheme.sizes.settingsCardCornerRadius), color = EasyTripBackground) {
                            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (model.deletedItems.isNotEmpty()) ConfirmationSection("将删除", model.deletedItems, MaterialTheme.colorScheme.error, deletedItemTags)
                                if (model.retainedItems.isNotEmpty()) ConfirmationSection("将保留", model.retainedItems, MaterialTheme.colorScheme.primary, retainedItemTags)
                            }
                        }
                    }
                    errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    if (busy) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            CircularProgressIndicator(Modifier.size(18.dp).testTag("confirmation-progress"))
                            Text("处理中…", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (showDismissAction) EasyTripSecondaryButton(
                        onClick = onDismiss,
                        enabled = dismissEnabled,
                        modifier = Modifier.weight(1f).height(48.dp).testTag("confirmation-dismiss"),
                    ) { Text(model.dismissLabel) }
                    if (model.destructive) {
                        EasyTripDangerButton(
                            onClick = onConfirm,
                            enabled = !busy,
                            modifier = Modifier.weight(1f).height(48.dp).testTag("confirmation-confirm"),
                        ) { Text(confirmLabel) }
                    } else {
                        EasyTripPrimaryButton(
                            onClick = onConfirm,
                            enabled = !busy,
                            modifier = Modifier.weight(1f).height(48.dp).testTag("confirmation-confirm"),
                        ) { Text(confirmLabel) }
                    }
                }
            }
        }
    }

@Composable
private fun ConfirmationSection(
    title: String,
    items: List<String>,
    color: Color,
    itemTags: List<String>?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.SemiBold)
        items.forEachIndexed { index, item ->
            Text(
                item,
                modifier = itemTags?.getOrNull(index)?.let(Modifier::testTag) ?: Modifier,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
