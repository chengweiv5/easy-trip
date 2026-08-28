package com.yangchengwei.easytrip.workspace

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceSheetSyncTest {
    @Test fun `sheet accepts only three levels and restores valid values`() {
        assertEquals(WorkspaceSheetLevel.COLLAPSED, restoreWorkspaceSheetLevel("COLLAPSED"))
        assertEquals(WorkspaceSheetLevel.HALF, restoreWorkspaceSheetLevel("HALF"))
        assertEquals(WorkspaceSheetLevel.EXPANDED, restoreWorkspaceSheetLevel("EXPANDED"))
    }

    @Test fun `invalid stored sheet level safely falls back to half`() {
        assertEquals(WorkspaceSheetLevel.HALF, restoreWorkspaceSheetLevel(null))
        assertEquals(WorkspaceSheetLevel.HALF, restoreWorkspaceSheetLevel("hidden"))
        assertEquals(WorkspaceSheetLevel.HALF, restoreWorkspaceSheetLevel("FULL"))
    }

    @Test fun `regular window produces ordered anchors and search return lift`() {
        assertEquals(
            WorkspaceSheetAnchors(34.dp, 396.dp, 578.dp),
            workspaceSheetAnchors(792.dp, searchReturn = false),
        )
        assertEquals(412.dp, workspaceSheetAnchors(792.dp, searchReturn = true).half)
    }

    @Test fun `expanded anchor remains continuous across the 395 to 396dp boundary`() {
        val before = workspaceSheetAnchors(395.dp, searchReturn = false)
        val boundary = workspaceSheetAnchors(396.dp, searchReturn = false)
        val after = workspaceSheetAnchors(397.dp, searchReturn = false)

        assertEquals(355.5f, before.expanded.value, 0.01f)
        assertEquals(356.4f, boundary.expanded.value, 0.01f)
        assertEquals(357.3f, after.expanded.value, 0.01f)
        assertTrue(boundary.expanded > before.expanded)
        assertTrue(after.expanded > boundary.expanded)
        assertTrue(boundary.expanded.value - before.expanded.value < 2f)
        assertTrue(after.expanded.value - boundary.expanded.value < 2f)
    }

    @Test fun `expanded anchor joins continuously at 636dp`() {
        val before = workspaceSheetAnchors(635.9.dp, searchReturn = false).expanded
        val boundary = workspaceSheetAnchors(636.dp, searchReturn = false).expanded
        val after = workspaceSheetAnchors(636.1.dp, searchReturn = false).expanded

        assertTrue("before=$before boundary=$boundary", boundary >= before)
        assertTrue("boundary=$boundary after=$after", after >= boundary)
        assertTrue("before=$before boundary=$boundary", boundary - before < 1.dp)
        assertTrue("boundary=$boundary after=$after", after - boundary < 1.dp)
    }

    @Test fun `search return lift is bounded by the same ordered anchors`() {
        listOf(48.dp, 280.dp, 396.dp, 636.dp, 792.dp).forEach { height ->
            val regular = workspaceSheetAnchors(height, searchReturn = false)
            val returned = workspaceSheetAnchors(height, searchReturn = true)
            assertTrue("height=$height regular=$regular returned=$returned", returned.collapsed == regular.collapsed)
            assertTrue("height=$height regular=$regular returned=$returned", returned.expanded == regular.expanded)
            assertTrue("height=$height regular=$regular returned=$returned", returned.half >= regular.half)
            assertTrue("height=$height returned=$returned", returned.collapsed < returned.half && returned.half < returned.expanded)
        }
    }

    @Test fun `anchors stay ordered monotonic and bounded from tiny to regular heights`() {
        val heights = listOf(48.dp, 280.dp, 395.dp, 396.dp, 397.dp, 782.dp, 844.dp)
        var previous: WorkspaceSheetAnchors? = null

        heights.forEach { height ->
            val anchors = workspaceSheetAnchors(height, searchReturn = false)
            assertTrue("height=$height anchors=$anchors", anchors.collapsed < anchors.half)
            assertTrue("height=$height anchors=$anchors", anchors.half < anchors.expanded)
            assertTrue("height=$height anchors=$anchors", anchors.expanded <= height)
            previous?.let { prior ->
                assertTrue("prior=$prior anchors=$anchors", anchors.collapsed >= prior.collapsed)
                assertTrue("prior=$prior anchors=$anchors", anchors.half >= prior.half)
                assertTrue("prior=$prior anchors=$anchors", anchors.expanded > prior.expanded)
            }
            previous = anchors
        }
    }

    @Test fun `small window produces ordered anchors within available height`() {
        val anchors = workspaceSheetAnchors(280.dp, searchReturn = false)

        assertTrue(anchors.collapsed < anchors.half)
        assertTrue(anchors.half < anchors.expanded)
        assertTrue(anchors.expanded <= 280.dp)
    }

    @Test fun `tiny window degrades within maximum available sheet height`() {
        val anchors = workspaceSheetAnchors(48.dp, searchReturn = true)

        assertTrue(anchors.collapsed < anchors.half)
        assertTrue(anchors.half < anchors.expanded)
        assertTrue(anchors.expanded <= 48.dp)
    }

    @Test fun `drag threshold is interpreted in pixels supplied by density`() {
        assertEquals(
            WorkspaceSheetLevel.HALF,
            resolveWorkspaceSheetDrag(WorkspaceSheetLevel.COLLAPSED, -49f, 48f),
        )
        assertEquals(
            WorkspaceSheetLevel.COLLAPSED,
            resolveWorkspaceSheetDrag(WorkspaceSheetLevel.COLLAPSED, -47f, 48f),
        )
    }

    @Test fun `drag moves only one adjacent level`() {
        assertEquals(
            WorkspaceSheetLevel.HALF,
            resolveWorkspaceSheetDrag(WorkspaceSheetLevel.COLLAPSED, -500f, 24f),
        )
        assertEquals(
            WorkspaceSheetLevel.HALF,
            resolveWorkspaceSheetDrag(WorkspaceSheetLevel.EXPANDED, 500f, 24f),
        )
    }

    @Test fun `drag offset is clamped to legal anchor range`() {
        assertEquals(-300f, clampWorkspaceSheetDragOffsetPx(400f, 40f, 700f, -999f))
        assertEquals(360f, clampWorkspaceSheetDragOffsetPx(400f, 40f, 700f, 999f))
    }
}
