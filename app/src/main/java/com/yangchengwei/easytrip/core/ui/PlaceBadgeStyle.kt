package com.yangchengwei.easytrip.core.ui

/** Map and result-list badges share fixed dp geometry; place names retain their own typography. */
internal object PlaceBadgeStyle {
    const val diameterDp = 28f
    private const val textWidthDp = 20f

    fun textSizeDp(label: String): Float = if (label.length == 1) 14f else 11f

    /** Candidate suffixes (e.g. CV1) must fit without widening the circle or truncating the label. */
    fun fittedTextSizeDp(label: String, measuredWidthDp: Float): Float =
        textSizeDp(label) * minOf(1f, textWidthDp / measuredWidthDp.coerceAtLeast(1f))
}
