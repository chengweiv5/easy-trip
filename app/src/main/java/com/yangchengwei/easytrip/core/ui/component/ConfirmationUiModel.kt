package com.yangchengwei.easytrip.core.ui.component

import androidx.compose.runtime.Immutable

@Immutable
data class ConfirmationUiModel(
    val title: String,
    val message: String,
    val deletedItems: List<String>,
    val retainedItems: List<String>,
    val confirmLabel: String,
    val dismissLabel: String,
    val destructive: Boolean,
    val reversible: Boolean,
)
