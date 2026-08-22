package com.yangchengwei.easytrip
import org.junit.Assert.assertEquals
import org.junit.Test
class SanityTest {
    @Test fun packageName_isStable() {
        assertEquals("com.yangchengwei.easytrip", BuildConfig.APPLICATION_ID)
    }
}
