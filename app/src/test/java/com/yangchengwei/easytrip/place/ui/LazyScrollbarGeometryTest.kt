package com.yangchengwei.easytrip.place.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LazyScrollbarGeometryTest {
    @Test fun `empty or fully visible content has no thumb`() {
        assertNull(calculateScrollbarThumb(0, 0, 0, 0, 1))
        assertNull(calculateScrollbarThumb(5, 5, 0, 0, 40, contentOverflows = false))
        assertNotNull(calculateScrollbarThumb(3, 3, 0, 0, 100, contentOverflows = true))
        assertNull(calculateScrollbarThumb(5, 6, 0, 0, 40))
    }

    @Test fun `overflowing content maps top and bottom to track bounds`() {
        val top = calculateScrollbarThumb(20, 5, 0, 0, 40)!!
        assertEquals(0f, top.offsetFraction)
        assertEquals(0.25f, top.sizeFraction)

        val bottom = calculateScrollbarThumb(20, 5, 15, 0, 40)!!
        assertEquals(1f, bottom.offsetFraction)
        assertEquals(0.25f, bottom.sizeFraction)
    }

    @Test fun `partial item progress stays normalized`() {
        val thumb = calculateScrollbarThumb(20, 5, 7, 20, 40)!!

        assertTrue(thumb.offsetFraction in 0f..1f)
        assertTrue(thumb.sizeFraction in 0f..1f)
        assertEquals(0.5f, thumb.offsetFraction)
    }
}
