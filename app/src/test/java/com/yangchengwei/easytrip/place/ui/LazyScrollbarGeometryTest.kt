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

    @Test fun `partial boundary item does not resize thumb while scrolling`() {
        val atTop = calculateScrollbarThumb(20, 5, 0, 0, 40, viewportCapacity = 5)!!
        val afterCrossingItemBoundary = calculateScrollbarThumb(
            20,
            6,
            1,
            1,
            40,
            viewportCapacity = 5,
        )!!

        assertEquals(atTop.sizeFraction, afterCrossingItemBoundary.sizeFraction)
        assertTrue(afterCrossingItemBoundary.offsetFraction > atTop.offsetFraction)
    }

    @Test fun `reported overflow without scrollable items keeps a finite degraded thumb`() {
        listOf(5, 6).forEach { capacity ->
            val thumb = calculateScrollbarThumb(
                totalItems = 5,
                visibleItems = 5,
                viewportCapacity = capacity,
                firstVisibleItemIndex = 0,
                firstVisibleItemScrollOffsetPx = 0,
                firstVisibleItemSizePx = 40,
                contentOverflows = true,
            )!!

            assertEquals(0f, thumb.offsetFraction)
            assertTrue(thumb.offsetFraction.isFinite())
            assertTrue(thumb.sizeFraction.isFinite())
            assertTrue(thumb.sizeFraction in 0f..1f)
            assertTrue(thumb.sizeFraction < 1f)
        }
    }

    @Test fun `scrollable thumb geometry is finite at both bounds`() {
        listOf(
            calculateScrollbarThumb(20, 5, 0, 0, 40, viewportCapacity = 5)!!,
            calculateScrollbarThumb(20, 5, 15, 40, 40, viewportCapacity = 5)!!,
        ).forEach { thumb ->
            assertTrue(thumb.offsetFraction.isFinite())
            assertTrue(thumb.sizeFraction.isFinite())
            assertTrue(thumb.offsetFraction in 0f..1f)
            assertTrue(thumb.sizeFraction in 0f..1f)
        }
    }

    @Test fun `largest initially visible item defines stable scrollbar capacity`() {
        val representativeItemSize = estimateScrollbarItemSizePx(listOf(28, 36, 60, 60))
        val capacity = calculateScrollbarViewportCapacity(
            viewportHeightPx = 300,
            representativeItemSizePx = representativeItemSize,
            fallbackItemCount = 4,
        )
        val beforeScroll = calculateScrollbarThumb(
            totalItems = 20,
            visibleItems = 5,
            viewportCapacity = capacity,
            firstVisibleItemIndex = 0,
            firstVisibleItemScrollOffsetPx = 0,
            firstVisibleItemSizePx = 28,
        )!!
        val afterLeadingHeaderScrollsOut = calculateScrollbarThumb(
            totalItems = 20,
            visibleItems = 5,
            viewportCapacity = capacity,
            firstVisibleItemIndex = 1,
            firstVisibleItemScrollOffsetPx = 0,
            firstVisibleItemSizePx = 60,
        )!!

        assertEquals(60, representativeItemSize)
        assertEquals(5, capacity)
        assertEquals(beforeScroll.sizeFraction, afterLeadingHeaderScrollsOut.sizeFraction)
        assertTrue(afterLeadingHeaderScrollsOut.offsetFraction > beforeScroll.offsetFraction)
    }
}
