package com.yangchengwei.easytrip.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme

enum class FeedbackKind { LOADING, EMPTY, ERROR }

@Composable
fun FeedbackState(
    kind: FeedbackKind,
    title: String,
    message: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    if (kind == FeedbackKind.ERROR) {
        requireNotNull(actionLabel) { "Error feedback requires an action label" }
        requireNotNull(onAction) { "Error feedback requires an action callback" }
    }
    FeedbackContent(
        title = title,
        message = message,
        modifier = modifier,
        loading = kind == FeedbackKind.LOADING,
        actionLabel = actionLabel,
        onAction = onAction,
    )
}

@Composable
fun LoadingFeedbackState(
    title: String,
    message: String? = null,
    modifier: Modifier = Modifier,
) = FeedbackState(FeedbackKind.LOADING, title, message, modifier = modifier)

@Composable
fun EmptyFeedbackState(
    title: String,
    message: String? = null,
    modifier: Modifier = Modifier,
) = FeedbackState(FeedbackKind.EMPTY, title, message, modifier = modifier)

@Composable
fun ErrorFeedbackState(
    title: String,
    message: String? = null,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) = FeedbackState(FeedbackKind.ERROR, title, message, actionLabel, onAction, modifier)

@Composable
private fun FeedbackContent(
    title: String,
    message: String?,
    modifier: Modifier,
    loading: Boolean,
    actionLabel: String?,
    onAction: (() -> Unit)?,
) {
    Column(
        modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(EasyTripTheme.spacing.medium),
    ) {
        if (loading) CircularProgressIndicator()
        Text(title, style = MaterialTheme.typography.titleLarge)
        message?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (actionLabel != null && onAction != null) {
            EasyTripPrimaryButton(onAction) { Text(actionLabel) }
        }
    }
}
