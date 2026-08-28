package com.yangchengwei.easytrip.core.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.R
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme

enum class EmptyIllustration(@DrawableRes val resource: Int) {
    Trips(R.drawable.empty_trips),
    Places(R.drawable.empty_places),
    Itinerary(R.drawable.empty_itinerary),
    Search(R.drawable.empty_search),
    Failure(R.drawable.empty_failure),
}

@Composable
fun EmptyIllustrationImage(
    illustration: EmptyIllustration,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(illustration.resource),
        contentDescription = null,
        modifier = modifier.size(96.dp).testTag("empty-illustration-${illustration.name.lowercase()}"),
    )
}

@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    emptyIllustration: EmptyIllustration? = null,
    illustration: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(EasyTripTheme.spacing.small),
    ) {
        emptyIllustration?.let { EmptyIllustrationImage(it) }
        illustration?.invoke()
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        action?.invoke()
    }
}
