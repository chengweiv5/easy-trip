package com.yangchengwei.easytrip.workspace

import org.junit.Assert.assertEquals
import org.junit.Test

class OccurrenceBadgeFormatterTest {
    @Test fun `one occurrence uses its order`() {
        assertEquals("1", formatOccurrenceBadge(listOf(1)))
    }

    @Test fun `two occurrences join their orders`() {
        assertEquals("1·4", formatOccurrenceBadge(listOf(1, 4)))
    }

    @Test fun `three occurrences join their orders`() {
        assertEquals("1·4·7", formatOccurrenceBadge(listOf(1, 4, 7)))
    }

    @Test fun `more than three occurrences compress after first order`() {
        assertEquals("1 +3", formatOccurrenceBadge(listOf(1, 4, 7, 9)))
    }
}
