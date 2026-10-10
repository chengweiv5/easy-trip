package com.yangchengwei.easytrip.core.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class NumberFormattingTest {
    @Test fun displayUsesCommaGroupingIndependentlyOfDeviceLocaleAndKeepsPrecision() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            assertEquals("0", formatCount(0))
            assertEquals("999", formatCount(999))
            assertEquals("1,000", formatCount(1000))
            assertEquals("2,147,483,647", formatCount(Int.MAX_VALUE))
            assertEquals("-9,223,372,036,854,775,808", formatCount(Long.MIN_VALUE))
            assertEquals("12,345.60", groupDecimalDigits("12345.60"))
            assertEquals("0.01", groupDecimalDigits("0.01"))
        } finally {
            Locale.setDefault(previous)
        }
    }
}
