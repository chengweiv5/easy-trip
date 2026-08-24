package com.yangchengwei.easytrip.itinerary.ui

import java.time.LocalTime

data class ItineraryEditDraft(
    val itemId: String,
    val arrivalTimeText: String,
    val stayMinutesText: String,
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val generation: Long = 0,
) {
    val arrivalTime: LocalTime?
        get() = arrivalTimeText.takeIf(String::isNotBlank)?.let { runCatching { LocalTime.parse(it) }.getOrNull() }

    val stayMinutes: Int?
        get() = stayMinutesText.takeIf(String::isNotBlank)?.toIntOrNull()

    val isValid: Boolean
        get() {
            val parsedMinutes = stayMinutes
            return (arrivalTimeText.isBlank() || arrivalTime != null) &&
                (stayMinutesText.isBlank() || parsedMinutes != null && parsedMinutes >= 0)
        }
}

data class ItineraryDeleteConfirmation(
    val itemId: String,
    val placeName: String,
    val isDeleting: Boolean = false,
    val deleteError: String? = null,
    val generation: Long = 0,
)
