package com.yangchengwei.easytrip.itinerary.calendar

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import com.yangchengwei.easytrip.itinerary.domain.ItineraryTimingChange
import com.yangchengwei.easytrip.itinerary.ui.ItineraryItemUi
import com.yangchengwei.easytrip.itinerary.ui.WholeTripDayUi
import com.yangchengwei.easytrip.workspace.ItineraryScope
import java.time.LocalTime
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
class CalendarInteractionTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val changes = mutableListOf<ItineraryTimingChange>()
    private val selected = mutableStateOf<ItineraryScope>(ItineraryScope.Day("day"))
    private val raw = mutableStateOf(listOf<WholeTripDayUi>())
    private var focused: Pair<String, String?>? = null
    private var editing: String? = null
    private var openedRoute: String? = null
    private fun item(id: String = "a", arrival: String? = "09:40", stay: Int? = 90) = ItineraryItemUi(id, if(id=="a") "西湖天地" else "湖滨步行街", "杭州", arrival?.let(LocalTime::parse), stay)
    private fun setup(items: List<ItineraryItemUi> = listOf(item()), whole: Boolean = false, width: Int = 278, scale: Float = 1f, count: Int = 1) {
        raw.value = (1..count).map { WholeTripDayUi(if(it==1) "day" else "day$it", it, items.map { item -> item.copy(id = if(it==1) item.id else "${item.id}$it") }, emptyList()) }
        if(whole) selected.value = ItineraryScope.WholeTrip
        compose.setContent { CompositionLocalProvider(LocalDensity provides Density(1f, scale)) { EasyTripTheme {
            CalendarContent(raw.value, selected.value, CalendarSaveState(), focused?.second,
                onToggle = {}, onFocus = { day, id -> focused = day to id; selected.value = ItineraryScope.Day(day) },
                onEdit = { _, id -> editing = id }, onAdd = {},
                onSave = { change -> changes += change; raw.value = raw.value.map { day -> if(day.dayId!=change.dayId) day else day.copy(items = day.items.map { if(it.id==change.itemId) it.copy(arrivalTime=change.after.arrivalTime, stayMinutes=change.after.stayMinutes) else it }) } },
                onUndo = {}, onRetry = {}, onDismissMessage = {}, onBusy = {}, modifier = Modifier.width(width.dp).height(660.dp),
                onRoute = { openedRoute = it })
        } } }
        compose.waitForIdle()
    }
    private fun event(id: String="a") = compose.onNodeWithTag("calendar-event-day:$id:day")
    private fun edge(xFraction: Float, top: Boolean, delta: Float) {
        event().performTouchInput {
            val from = Offset(width*xFraction, if(top) 2f else height-2f)
            down(from); advanceEventTime(600); moveTo(from+Offset(0f,delta), 300); up()
        }
    }
    @Test fun resizeCuesFollowFingerAboveBothEdgesAndDisappearAfterReleaseOrCancel() {
        setup(listOf(item("early", "07:30", 30), item()))
        val viewportBefore = compose.onNodeWithTag("calendar-viewport").fetchSemanticsNode().boundsInRoot
        listOf(.05f, .5f, .95f).forEach { x ->
            val original = event().fetchSemanticsNode().boundsInRoot
            event().performTouchInput { down(Offset(width * x, 2f)); advanceEventTime(600); moveBy(Offset.Zero) }
            val cue = compose.onNodeWithTag("calendar-resize-start", useUnmergedTree = true)
            cue.assertIsDisplayed().assertContentDescriptionContains("调整开始 09:40", substring = true)
            cue.assertContentDescriptionContains("停留 90 分钟", substring = true)
            cue.assert(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.StateDescription, "Floating"))
            val bounds = cue.fetchSemanticsNode().boundsInRoot
            assertEquals(original.top + 2f - 64f, bounds.bottom, 1f)
            assertTrue(bounds.left >= viewportBefore.left + 7f)
            assertTrue(bounds.right <= viewportBefore.right - 7f)
            compose.onNodeWithTag("calendar-draft-label").assertTextEquals("")
            event().performTouchInput { moveBy(Offset(0f, -26f), 300) }
            cue.assertContentDescriptionContains("调整开始 09:10", substring = true)
            cue.assertContentDescriptionContains("结束 11:10 不变", substring = true)
            cue.assertContentDescriptionContains("停留 120 分钟", substring = true)
            assertEquals(bounds.bottom - 26f, cue.fetchSemanticsNode().boundsInRoot.bottom, 1f)
            saveResizeEvidence("calendar-resize-start.jpg")
            event().performTouchInput { cancel() }
            cue.assertDoesNotExist()
            assertTrue(changes.isEmpty())
        }
        val original = event().fetchSemanticsNode().boundsInRoot
        event().performTouchInput { down(Offset(width * .95f, height - 2f)); advanceEventTime(600); moveBy(Offset.Zero) }
        val lower = compose.onNodeWithTag("calendar-resize-end", useUnmergedTree = true)
        assertEquals(original.bottom - 2f - 64f, lower.fetchSemanticsNode().boundsInRoot.bottom, 1f)
        event().performTouchInput { moveBy(Offset(0f, 26f), 300) }
        lower.assertContentDescriptionContains("调整结束 11:40", substring = true)
        lower.assertContentDescriptionContains("开始 09:40 不变", substring = true)
        saveResizeEvidence("calendar-resize-end.jpg")
        event().performTouchInput { up() }
        lower.assertDoesNotExist()
        assertEquals(1, changes.size)
        assertEquals(120, changes.single().after.stayMinutes)
        assertEquals(viewportBefore, compose.onNodeWithTag("calendar-viewport").fetchSemanticsNode().boundsInRoot)
    }

    @Test fun unsetStayShowsEdgeCueAfterLongPressAndBodyMoveDoesNot() {
        setup(listOf(item(stay = null)))
        event().performTouchInput { down(Offset(width * .5f, height - 2f)); advanceEventTime(600); moveBy(Offset.Zero) }
        compose.onNodeWithTag("calendar-resize-end", useUnmergedTree = true).assertIsDisplayed()
        event().performTouchInput { cancel() }
        compose.onNodeWithTag("calendar-resize-end").assertDoesNotExist()
        event().performTouchInput { down(center); advanceEventTime(600); moveBy(Offset(0f, 26f), 300) }
        compose.onNodeWithTag("calendar-draft").assertIsDisplayed()
        compose.onNodeWithTag("calendar-resize-start").assertDoesNotExist()
        compose.onNodeWithTag("calendar-resize-end").assertDoesNotExist()
        event().performTouchInput { cancel() }
        assertTrue(changes.isEmpty())
    }

    @Test fun midnightResizeUsesSummaryWithoutMovingTimeline() {
        setup(listOf(item(arrival = "00:00")))
        val viewport = compose.onNodeWithTag("calendar-viewport").fetchSemanticsNode().boundsInRoot
        val summary = compose.onNodeWithTag("calendar-summary").fetchSemanticsNode().boundsInRoot
        event().performTouchInput { down(Offset(width * .5f, 2f)); advanceEventTime(600); moveBy(Offset.Zero) }
        val cue = compose.onNodeWithTag("calendar-resize-start", useUnmergedTree = true)
        cue.assertIsDisplayed().assertContentDescriptionContains("调整开始 00:00", substring = true)
        cue.assert(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.StateDescription, "Summary"))
        assertEquals(summary, cue.fetchSemanticsNode().boundsInRoot)
        assertEquals(viewport, compose.onNodeWithTag("calendar-viewport").fetchSemanticsNode().boundsInRoot)
        compose.onNodeWithTag("calendar-toggle").assertDoesNotExist()
        saveResizeEvidence("calendar-resize-top.jpg")
        event().performTouchInput { cancel() }
        compose.onNodeWithTag("calendar-toggle").assertIsDisplayed()
        assertEquals(viewport, compose.onNodeWithTag("calendar-viewport").fetchSemanticsNode().boundsInRoot)
        assertTrue(changes.isEmpty())
    }

    @Test fun lateUnsetStayKeepsLowerHintVisibleAndDoesNotPretendStayWasSaved() {
        setup(listOf(item(arrival = "23:59", stay = null)))
        event().performScrollTo().performTouchInput { down(Offset(width * .5f, height - 2f)); advanceEventTime(600); moveBy(Offset.Zero) }
        val cue = compose.onNodeWithTag("calendar-resize-end", useUnmergedTree = true)
        cue.assertIsDisplayed().assertContentDescriptionContains("次日 00:59", substring = true)
        cue.assertContentDescriptionContains("停留待设", substring = true)
        event().performTouchInput { cancel() }
        assertTrue(changes.isEmpty())
        assertNull(raw.value.single().items.single().stayMinutes)
    }

    @Test fun largeFontHintMeasuresHeightWithoutMovingTimelineOrDroppingMainText() {
        setup(listOf(item(arrival = "00:00")), width = 208, scale = 2f)
        val viewport = compose.onNodeWithTag("calendar-viewport").fetchSemanticsNode().boundsInRoot
        event().performTouchInput { down(Offset(width * .5f, 2f)); advanceEventTime(600); moveBy(Offset.Zero) }
        val cue = compose.onNodeWithTag("calendar-resize-start", useUnmergedTree = true)
        cue.assertIsDisplayed().assertContentDescriptionContains("调整开始 00:00", substring = true)
        cue.assert(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.StateDescription, "Wide"))
        compose.onNodeWithText("↑ 调整开始", useUnmergedTree = true).assertIsDisplayed()
        compose.onNode(hasText("00:00") and hasAnyAncestor(hasTestTag("calendar-resize-start")), useUnmergedTree = true).assertIsDisplayed()
        assertEquals(viewport, compose.onNodeWithTag("calendar-viewport").fetchSemanticsNode().boundsInRoot)
        saveResizeEvidence("calendar-resize-large-font.jpg")
        event().performTouchInput { cancel() }
        assertTrue(changes.isEmpty())
    }

    @Test fun floatingAndFixedTransitionHasHysteresisAndKeepsGridPosition() {
        setup()
        val before = compose.onNodeWithTag("calendar-grid").fetchSemanticsNode().boundsInRoot.top
        event().performTouchInput { down(Offset(width * .5f, height - 2f)); advanceEventTime(600); moveBy(Offset.Zero) }
        val cue = compose.onNodeWithTag("calendar-resize-end", useUnmergedTree = true)
        fun position(value: String) = cue.assert(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.StateDescription, value))
        position("Floating")
        event().performTouchInput { moveBy(Offset(0f, -40f), 300) }
        position("Summary")
        event().performTouchInput { moveBy(Offset(0f, 8f), 100) }
        position("Summary")
        event().performTouchInput { moveBy(Offset(0f, 20f), 100) }
        position("Floating")
        assertEquals(before, compose.onNodeWithTag("calendar-grid").fetchSemanticsNode().boundsInRoot.top, 1f)
        event().performTouchInput { cancel() }
        assertTrue(changes.isEmpty())
    }

    @Test fun trafficWarningStaysOnCardAndDetailWithoutBottomStrip() {
        setup(listOf(item(arrival = "09:00", stay = 60), item("b", "10:15", 60)))
        compose.runOnIdle {
            raw.value = raw.value.map { it.copy(legs = listOf(route("traffic", 3600))) }
        }
        compose.onNodeWithTag("calendar-route-traffic").assertDoesNotExist()
        event("b").assertContentDescriptionContains("交通可能来不及", substring = true)
        event("b").performClick()
        compose.onNodeWithTag("calendar-detail").assertIsDisplayed()
        compose.onNodeWithText("交通可能来不及", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test fun pendingRouteRemainsReachableThroughDetail() {
        setup(listOf(item(arrival = null, stay = null), item("b", null, null)))
        compose.runOnIdle { raw.value = raw.value.map { it.copy(legs = listOf(route("unknown", null))) } }
        compose.onNodeWithTag("calendar-route-unknown").assertDoesNotExist()
        compose.onNodeWithTag("calendar-pending-a").performClick()
        compose.onNodeWithTag("calendar-detail-route-unknown").assertIsDisplayed().performClick()
        assertEquals("unknown", openedRoute)
        compose.onNodeWithTag("calendar-detail").assertDoesNotExist()
        assertTrue(changes.isEmpty())
    }

    @Test fun sourceDetailIncludesRouteWhoseTrafficStartsOnNextDay() {
        setup(listOf(item(arrival = "23:00", stay = 120), item("b", "23:45", 30)), count = 2)
        compose.runOnIdle { raw.value = raw.value.mapIndexed { index, day ->
            if (index == 0) day.copy(legs = listOf(route("next-day", 3600))) else day
        } }
        event().performClick()
        compose.onNodeWithTag("calendar-detail-route-next-day").assertIsDisplayed().performClick()
        assertEquals("next-day", openedRoute)
    }

    @Test fun sourceDetailPreservesTrafficConflictFromNextDayFragment() {
        setup(listOf(item(arrival = "23:00", stay = 30), item("b", null, null)), count = 2)
        compose.runOnIdle {
            raw.value = listOf(
                raw.value.first().copy(legs = listOf(route("overnight", 7200))),
                raw.value.last().copy(items = listOf(item("c", "00:30", 60))),
            )
        }
        event().performClick()
        compose.onNodeWithTag("calendar-detail-route-overnight")
            .assertIsDisplayed().assertTextContains("时间不足", substring = true)
    }


    private fun traffic() = compose.onNodeWithTag("calendar-traffic-day-traffic")

    @Test fun trafficBlockShowsEstimateAndTrueIntervalWithoutCoveringVisitEdges() {
        setup(listOf(item(arrival = "09:00", stay = 60), item("b", "12:00", 60)))
        compose.runOnIdle { raw.value = raw.value.map { it.copy(legs = listOf(route("traffic", 3600))) } }
        traffic().assertIsDisplayed().assertContentDescriptionContains("预计约 60 分钟", substring = true)
        val interval = compose.onNodeWithTag("calendar-traffic-interval-day-traffic", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertEquals(52f, interval.height, 1f)
        assertEquals(event().fetchSemanticsNode().boundsInRoot.left, interval.left, 1f)
        assertEquals(event().fetchSemanticsNode().boundsInRoot.right, interval.right, 1f)
        traffic().assertTextContains("步行 · 约60分钟")
        saveResizeEvidence("calendar-traffic-normal.jpg")
        traffic().performClick()
        assertEquals("traffic", openedRoute)
    }

    @Test fun squeezedAndFullyOccupiedTrafficRemainVisible() {
        setup(listOf(item(arrival = "09:00", stay = 60), item("b", "10:15", 60)))
        compose.runOnIdle { raw.value = raw.value.map { it.copy(legs = listOf(route("traffic", 3600))) } }
        traffic().assertIsDisplayed().assertContentDescriptionContains("预留 15 分钟", substring = true)
        assertEquals(13f, compose.onNodeWithTag("calendar-traffic-interval-day-traffic", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.height, 1f)
        saveResizeEvidence("calendar-traffic-squeezed.jpg")
        compose.runOnIdle { raw.value = raw.value.map { day -> day.copy(items = day.items.map { if (it.id == "b") it.copy(arrivalTime = LocalTime.of(10, 0)) else it }) } }
        traffic().assertDoesNotExist()
        event("b").assertContentDescriptionContains("预留 0 分钟", substring = true)
        event("b").assertContentDescriptionContains("交通可能来不及", substring = true)
        saveResizeEvidence("calendar-traffic-zero.jpg")
    }

    @Test fun visitResizeLiveSqueezesTrafficThenCancelRestoresAndSavePersistsOnlyVisit() {
        setup(listOf(item(arrival = "09:00", stay = 60), item("b", "10:30", 60)))
        compose.runOnIdle { raw.value = raw.value.map { it.copy(legs = listOf(route("traffic", 3600))) } }
        traffic().assertContentDescriptionContains("预留 30 分钟", substring = true)
        event().performTouchInput { down(Offset(width * .95f, height - 2f)); advanceEventTime(600); moveBy(Offset(0f, 26f), 300) }
        traffic().assertDoesNotExist()
        event("b").assertContentDescriptionContains("预留 0 分钟", substring = true)
        compose.onNodeWithTag("calendar-resize-end", useUnmergedTree = true).assertIsDisplayed()
        saveResizeEvidence("calendar-traffic-drag.jpg")
        event().performTouchInput { cancel() }
        traffic().assertContentDescriptionContains("预留 30 分钟", substring = true)
        assertTrue(changes.isEmpty())
        edge(.05f, false, 26f)
        event("b").assertContentDescriptionContains("预留 0 分钟", substring = true)
        assertEquals(90, changes.single().after.stayMinutes)
        assertEquals(3600, raw.value.single().legs.single().effectiveDurationSeconds)
    }

    @Test fun wholeTripShowsTrafficAndOpensSourceDayBeforeEditingRoute() {
        setup(listOf(item(arrival = "09:00", stay = 60), item("b", "10:15", 60)), whole = true, count = 2, width = 390)
        compose.runOnIdle { raw.value = raw.value.mapIndexed { index, day -> if (index == 0) day.copy(legs = listOf(route("traffic", 3600))) else day } }
        traffic().assertIsDisplayed()
        saveResizeEvidence("calendar-traffic-whole.jpg")
        traffic().performClick()
        assertEquals("day" to "a", focused)
        assertNull(openedRoute)
        assertTrue(changes.isEmpty())
    }

    @Test fun narrowWholeTripKeepsReadableVisitAndTrafficAndCanPage() {
        setup(listOf(item(arrival = "09:00", stay = 60), item("b", "10:15", 60)), whole = true, count = 2, width = 220)
        compose.runOnIdle { raw.value = raw.value.mapIndexed { index, day -> if (index == 0) day.copy(legs = listOf(route("traffic", 3600))) else day } }
        compose.onNodeWithTag("calendar-date-day2").assertDoesNotExist()
        assertTrue(event().fetchSemanticsNode().boundsInRoot.width >= 150f)
        traffic().assertIsDisplayed()
        saveResizeEvidence("calendar-traffic-narrow.jpg")
        compose.onNodeWithText("›").performClick()
        compose.onNodeWithTag("calendar-date-day2").assertIsDisplayed()
    }

    @Test fun midnightTrafficDisplaysContinuationAndUnknownDepartureStaysInDetail() {
        setup(listOf(item(arrival = "23:00", stay = 30), item("b", null)), whole = true, count = 2, width = 390)
        compose.runOnIdle { raw.value = raw.value.mapIndexed { index, day -> if (index == 0) day.copy(legs = listOf(route("traffic", 3600))) else day.copy(items = emptyList()) } }
        compose.onNodeWithTag("calendar-traffic-day2-traffic").performScrollTo().assertIsDisplayed()
            .assertContentDescriptionContains("本段预留 30 分钟", substring = true)
        compose.onNodeWithTag("calendar-traffic-day2-traffic").performClick()
        assertEquals("day" to "a", focused)
        compose.runOnIdle { raw.value = raw.value.map { day -> day.copy(items = day.items.map { it.copy(stayMinutes = null) }) } }
        traffic().assertDoesNotExist()
        event().performScrollTo().performClick()
        compose.onNodeWithTag("calendar-detail-route-traffic").performScrollTo().assertIsDisplayed()
    }

    @Test fun largeFontTrafficFallsBackWithoutInventingExtraHeight() {
        setup(listOf(item(arrival = "09:00", stay = 60), item("b", "10:15", 120)), whole = true, count = 2, scale = 1.5f)
        compose.runOnIdle { raw.value = raw.value.mapIndexed { index, day -> if (index == 0) day.copy(legs = listOf(route("traffic", 3600))) else day } }
        traffic().assertDoesNotExist()
        compose.onNodeWithTag("calendar-incoming-day-traffic", useUnmergedTree = true).assertIsDisplayed()
        assertEquals(104f, event("b").fetchSemanticsNode().boundsInRoot.height, 1f)
        saveResizeEvidence("calendar-traffic-large-font.jpg")
        event("b").performClick()
        assertEquals("day" to "b", focused)
        event("b").performClick()
        compose.onNodeWithTag("calendar-detail-incoming-traffic").performScrollTo().assertHeightIsAtLeast(48.dp)
            .assertTextContains("预留 15 分钟", substring = true)
    }

    @Test fun denseShortTrafficUsesDestinationCardsAndDetailsWithoutDisplacedLabels() {
        setup(listOf(item(arrival = "09:00", stay = 60), item("b", "10:05", 5), item("c", "10:15", 90)))
        compose.runOnIdle { raw.value = raw.value.map { it.copy(legs = listOf(route("traffic", 1800), route("second", 2400).copy(fromItemId = "b", toItemId = "c"))) } }
        traffic().assertDoesNotExist()
        compose.onNodeWithTag("calendar-traffic-day-second").assertDoesNotExist()
        compose.onNodeWithTag("calendar-incoming-day-traffic", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag("calendar-incoming-day-second", useUnmergedTree = true).assertIsDisplayed()
            .assertTextEquals("步行 · 约40分钟")
        event("b").performTouchInput { click(center) }
        compose.onNodeWithTag("calendar-detail-incoming-traffic").performScrollTo()
            .assertTextContains("预计约 30 分钟", substring = true).performClick()
        assertEquals("traffic", openedRoute)
    }

    @Test fun trafficDoesNotReduceTwoDayColumnWidth() {
        setup(listOf(item(arrival = "09:00", stay = 60), item("b", "10:15", 60)), whole = true, count = 2)
        compose.runOnIdle { raw.value = raw.value.mapIndexed { index, day -> if (index == 0) day.copy(legs = listOf(route("traffic", 1800))) else day } }
        compose.onNodeWithTag("calendar-date-day2").assertIsDisplayed()
        assertEquals(115f, event().fetchSemanticsNode().boundsInRoot.width, 1f)
        traffic().assertTextContains("步行 · 约30分")
        assertEquals(event().fetchSemanticsNode().boundsInRoot.width, traffic().fetchSemanticsNode().boundsInRoot.width, 1f)
    }

    @Test fun placeholderCannotHideTrafficAndUnknownEstimateStaysReachableAtDestination() {
        setup(listOf(item(arrival = "09:00", stay = 60), item("b", "11:00", 90), item("unset", "10:00", null)))
        compose.runOnIdle { raw.value = raw.value.map { it.copy(legs = listOf(route("traffic", 1800))) } }
        traffic().assertDoesNotExist()
        compose.onNodeWithTag("calendar-incoming-day-traffic", useUnmergedTree = true).assertIsDisplayed()
        compose.runOnIdle { raw.value = raw.value.map { it.copy(legs = listOf(route("traffic", null))) } }
        event("b").performClick()
        compose.onNodeWithTag("calendar-detail-incoming-traffic").performScrollTo().assertIsDisplayed()
            .assertTextContains("待", substring = true)
    }

    @Test fun zeroEstimateUsesDestinationAndIncomingRouteSurvivesDraftCancel() {
        setup(listOf(item(arrival = "09:00", stay = 60), item("b", "10:00", 60)))
        compose.runOnIdle { raw.value = raw.value.map { it.copy(legs = listOf(route("traffic", 0))) } }
        traffic().assertDoesNotExist()
        compose.onNodeWithTag("calendar-incoming-day-traffic", useUnmergedTree = true).assertTextEquals("步行 · 约0分钟")
        event("b").performTouchInput { down(center); advanceEventTime(600); moveBy(Offset(0f, 26f), 300) }
        compose.onNodeWithTag("calendar-draft-incoming", useUnmergedTree = true).assertIsDisplayed()
        event("b").performTouchInput { cancel() }
        compose.onNodeWithTag("calendar-incoming-day-traffic", useUnmergedTree = true).assertIsDisplayed()
        assertTrue(changes.isEmpty())
    }

    @Test fun shortTrafficAppearsOnPendingDestinationInSingleAndWholeTrip() {
        setup(listOf(item(arrival = "09:00", stay = 60), item("b", null, 60)), whole = true, count = 2)
        compose.runOnIdle { raw.value = raw.value.mapIndexed { i, day -> if (i == 0) day.copy(legs = listOf(route("traffic", 600))) else day } }
        compose.onNodeWithTag("calendar-pending-incoming-b", useUnmergedTree = true).assertIsDisplayed()
            .assertTextEquals("步行 · 约10分")
        compose.onNodeWithTag("calendar-pending-b").performClick()
        compose.onNodeWithTag("calendar-pending-incoming-b", useUnmergedTree = true).assertIsDisplayed()
            .assertTextEquals("步行 · 约10分钟")
    }

    private fun route(id: String, seconds: Int?) = com.yangchengwei.easytrip.itinerary.ui.RouteLegUi(
        id, "a", "b", com.yangchengwei.easytrip.core.model.TransportMode.WALK,
        if (seconds == null) com.yangchengwei.easytrip.core.model.RouteStatus.PENDING else com.yangchengwei.easytrip.core.model.RouteStatus.SUCCESS,
        100, seconds, null,
    )

    private fun saveResizeEvidence(name: String) {
        compose.onRoot().captureToImage().asAndroidBitmap().let { bitmap ->
            java.io.File(compose.activity.getExternalFilesDir(null), name).outputStream().use {
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 88, it)
            }
        }
    }

    @Test fun upperAndLowerEdgesWorkAcrossEntireWidthWithoutSelection() {
        setup()
        edge(.10f, false, 26f)
        assertEquals(120, changes.last().after.stayMinutes)
        edge(.90f, true, 26f)
        assertEquals(LocalTime.of(10,10), changes.last().after.arrivalTime)
        assertEquals(90, changes.last().after.stayMinutes)
        edge(.50f, false, 26f)
        assertEquals(120, changes.last().after.stayMinutes)
        assertEquals(3, changes.size)
    }
    @Test fun longPressBodyMovesWithoutChangingDuration() {
        setup()
        event().performTouchInput { down(center); advanceEventTime(600); moveBy(Offset(0f,26f),300); up() }
        assertEquals(1, changes.size)
        assertEquals(LocalTime.of(10,10), changes.single().after.arrivalTime)
        assertEquals(90, changes.single().after.stayMinutes)
    }
    @Test fun normalSwipeScrollsWithoutSavingAndCanReachMidnight() {
        setup(listOf(item(),item("late","23:00",60)))
        event().performTouchInput { down(center); moveBy(Offset(0f,-60f),100); up() }
        assertTrue(changes.isEmpty())
        compose.onNodeWithTag("calendar-viewport").performTouchInput { swipeUp(durationMillis=300) }
        compose.onNodeWithTag("calendar-viewport").performTouchInput { swipeUp(durationMillis=300) }
        compose.onNodeWithText("24:00", useUnmergedTree=true).assertExists()
    }
    @Test fun quickSwipeFromUpperEdgeScrollsCalendarWithoutChangingArrival() {
        setup()
        val before = event().fetchSemanticsNode().boundsInRoot.top
        event().performTouchInput {
            down(Offset(width * .5f, 2f)); moveBy(Offset(0f, -60f), 100); up()
        }
        assertTrue("quick upper-edge swipe must not save timing", changes.isEmpty())
        assertTrue("calendar should scroll", event().fetchSemanticsNode().boundsInRoot.top < before - 20f)
        compose.onNodeWithTag("calendar-resize-start").assertDoesNotExist()
        compose.onNodeWithTag("calendar-detail").assertDoesNotExist()
    }
    @Test fun quickSwipeFromLowerEdgeScrollsCalendarWithoutChangingStay() {
        setup(listOf(item(stay = null)))
        val before = event().fetchSemanticsNode().boundsInRoot.top
        event().performTouchInput {
            down(Offset(width * .5f, height - 2f)); moveBy(Offset(0f, -60f), 100); up()
        }
        assertTrue("quick lower-edge swipe must not save timing", changes.isEmpty())
        assertNull(raw.value.first().items.first().stayMinutes)
        assertTrue("calendar should scroll", event().fetchSemanticsNode().boundsInRoot.top < before - 20f)
        compose.onNodeWithTag("calendar-resize-end").assertDoesNotExist()
        compose.onNodeWithTag("calendar-detail").assertDoesNotExist()
    }
    @Test fun swipeThenPauseDoesNotTurnIntoResize() {
        setup()
        val before = event().fetchSemanticsNode().boundsInRoot.top
        event().performTouchInput {
            down(Offset(width * .5f, 2f))
            moveBy(Offset(0f, -24f), 80)
            advanceEventTime(700)
            moveBy(Offset(0f, -30f), 100); up()
        }
        assertTrue(changes.isEmpty())
        assertTrue(event().fetchSemanticsNode().boundsInRoot.top < before - 20f)
        compose.onNodeWithTag("calendar-draft").assertDoesNotExist()
    }
    @Test fun briefHoldThenSwipeStillScrollsWithoutSaving() {
        setup()
        val before = event().fetchSemanticsNode().boundsInRoot.top
        event().performTouchInput {
            down(Offset(width * .8f, height - 2f)); advanceEventTime(300)
            moveBy(Offset(0f, -60f), 100); up()
        }
        assertTrue(changes.isEmpty())
        assertTrue(event().fetchSemanticsNode().boundsInRoot.top < before - 20f)
    }
    @Test fun edgeTapOpensDetailAndLongHoldWithoutMovementPreservesUnsetStay() {
        setup(listOf(item(stay = null)))
        event().performTouchInput { down(Offset(width * .5f, 2f)); advanceEventTime(100); up() }
        compose.onNodeWithTag("calendar-detail").assertIsDisplayed()
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        event().performTouchInput { down(Offset(width * .5f, height - 2f)); advanceEventTime(600); up() }
        assertTrue(changes.isEmpty())
        assertNull(raw.value.first().items.first().stayMinutes)
        compose.onNodeWithTag("calendar-detail").assertDoesNotExist()
        compose.onNodeWithTag("calendar-resize-end").assertDoesNotExist()
    }
    @Test fun longPressResizeWorksAtLeftCenterAndRightOfBothEdges() {
        setup()
        listOf(.04f, .5f, .96f).forEach { x ->
            edge(x, false, 26f)
            edge(x, true, 26f)
        }
        assertEquals(6, changes.size)
        assertEquals(LocalTime.of(11, 10), changes.last().after.arrivalTime)
        assertEquals(90, changes.last().after.stayMinutes)
    }
    @Test fun unsetStayUsesFullCardAndNoOpKeepsNull() {
        setup(listOf(item(stay=null)))
        event().assertHeightIsAtLeast(52.dp)
        edge(.5f, false, 3f)
        assertTrue(changes.isEmpty())
        edge(.5f, false, 26f)
        assertNull(changes.single().before.stayMinutes)
        assertEquals(90, changes.single().after.stayMinutes)
    }
    @Test fun pendingDragIntoTimelinePreservesKnownStayAndClickOpensUnsetDetail() {
        setup(listOf(item(), item("pending",null,90)))
        val pending = compose.onNodeWithTag("calendar-pending-pending")
        pending.performClick()
        compose.onNodeWithTag("calendar-detail").assertIsDisplayed()
        compose.onNodeWithTag("calendar-detail-edit").performClick()
        assertEquals("pending",editing)
        assertNull(raw.value.first().items.first { it.id == "pending" }.arrivalTime)
        val source=pending.fetchSemanticsNode().boundsInRoot
        val grid=compose.onNodeWithTag("calendar-viewport").fetchSemanticsNode().boundsInRoot
        val delta=Offset(grid.left+110-source.center.x,grid.top+190-source.center.y)
        pending.performTouchInput { down(center); advanceEventTime(600); moveBy(delta,400); up() }
        assertEquals(90, changes.single().after.stayMinutes)
        assertNull(changes.single().before.arrivalTime)
        compose.onNodeWithTag("calendar-pending-pending").assertDoesNotExist()
    }
    @Test fun pendingOutsideDropAndCancelledGestureDoNotWrite() {
        setup(listOf(item(),item("pending",null,null)))
        compose.onNodeWithTag("calendar-pending-pending").performTouchInput { down(center); advanceEventTime(600); moveBy(Offset(-200f,100f),300); up() }
        assertTrue(changes.isEmpty())
        compose.onNodeWithTag("calendar-pending-pending").performTouchInput { down(center); advanceEventTime(600); moveBy(Offset(0f,120f),300); cancel() }
        assertTrue(changes.isEmpty())
    }
    @Test fun wholeTripIsReadOnlyAndTapFocusesSourceDay() {
        setup(whole=true,count=3)
        event().performTouchInput { down(center); advanceEventTime(600); moveBy(Offset(0f,26f),300); up() }
        assertTrue(changes.isEmpty())
        event().assertIsDisplayed().performClick()
        assertEquals("day" to "a", focused)
        compose.waitForIdle()
        event().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("calendar-detail").assertIsDisplayed()
    }
    @Test fun narrowLargeFontPagerContainsOnlyRealFinalDay() {
        setup(whole=true,count=3,width=220,scale=1.5f)
        compose.onNodeWithText("›").performClick()
        compose.onNodeWithText("›").performClick()
        compose.onNodeWithTag("calendar-date-day3").assertIsDisplayed()
        compose.onNodeWithTag("calendar-date-day4").assertDoesNotExist()
        compose.onNodeWithText("›").assertIsNotEnabled()
    }
    @Test fun shortAndZeroMinuteEventsOpenFromTimelineWithoutDuplicateFooter() {
        setup(listOf(item(stay=10),item("zero","10:30",0),item("late","23:30",120)))
        compose.onNodeWithTag("calendar-short-a").assertDoesNotExist()
        compose.onNodeWithTag("calendar-short-zero").assertDoesNotExist()
        val content = compose.onNodeWithTag("calendar-content").fetchSemanticsNode().boundsInRoot
        val viewport = compose.onNodeWithTag("calendar-viewport").fetchSemanticsNode().boundsInRoot
        assertEquals(content.bottom, viewport.bottom, 1f)
        assertEquals(10 * CALENDAR_MINUTE_DP, event().fetchSemanticsNode().boundsInRoot.height, 1f)
        saveResizeEvidence("calendar-without-short-footer.jpg")
        event().performTouchInput { click(center) }
        compose.onNodeWithTag("calendar-detail-edit").assertIsDisplayed()
        compose.onNodeWithText("停留：10 分钟").assertIsDisplayed()
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        event("zero").performScrollTo().performTouchInput { click(center) }
        compose.onNodeWithText("停留：0 分钟").assertIsDisplayed()
    }
    @Test fun movingAfterWholeToSingleUsesCurrentDayAndKeyboardCancelsOrCommits() {
        setup(whole=true,count=3)
        event().assertIsDisplayed().performClick()
        compose.waitForIdle()
        event().performTouchInput { down(center); advanceEventTime(600); moveBy(Offset(0f,26f),300); up() }
        assertEquals(1, changes.size)
        event().performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        event().performKeyInput { pressKey(androidx.compose.ui.input.key.Key.Spacebar); pressKey(androidx.compose.ui.input.key.Key.DirectionDown); pressKey(androidx.compose.ui.input.key.Key.Escape) }
        assertEquals(1, changes.size)
        event().performKeyInput { pressKey(androidx.compose.ui.input.key.Key.Spacebar); pressKey(androidx.compose.ui.input.key.Key.DirectionDown); pressKey(androidx.compose.ui.input.key.Key.Enter) }
        assertEquals(2, changes.size)
        assertEquals(LocalTime.of(10,40), changes.last().after.arrivalTime)
    }
    @Test fun activatedDragAtViewportEdgeAutoScrollsAndBackCancels() {
        setup()
        val bounds=event().fetchSemanticsNode().boundsInRoot
        val viewport=compose.onNodeWithTag("calendar-viewport").fetchSemanticsNode().boundsInRoot
        event().performTouchInput { down(center); advanceEventTime(600); moveBy(Offset(0f,viewport.bottom-12-bounds.center.y),300) }
        val grid=compose.onNodeWithTag("calendar-grid")
        val before=grid.fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.VerticalScrollAxisRange].value()
        compose.mainClock.advanceTimeBy(500)
        val after=grid.fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.VerticalScrollAxisRange].value()
        assertTrue("edge should scroll: $before -> $after",after>before)
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        event().performTouchInput { up() }
        assertTrue(changes.isEmpty())
    }
    @Test fun externalTimingChangeDuringDragCancelsTheDraft() {
        setup()
        event().performTouchInput { down(center); advanceEventTime(600); moveBy(Offset(0f,26f),300) }
        compose.runOnIdle { raw.value=raw.value.map { it.copy(items=listOf(item(arrival="11:00"))) } }
        event().performTouchInput { up() }
        assertTrue(changes.isEmpty())
    }
    @Test fun thirtyDayPagerAndHorizontalSwipeReachLastRealDay() {
        setup(whole=true,count=30)
        repeat(14) { compose.onNodeWithText("›").performClick() }
        compose.onNodeWithTag("calendar-date-day30").assertIsDisplayed()
        compose.onNodeWithText("›").assertIsNotEnabled()
        compose.onNodeWithTag("calendar-viewport").performTouchInput { swipeRight(durationMillis=300) }
        compose.onNodeWithTag("calendar-date-day27").assertIsDisplayed()
    }
    @Test fun moreThanTwoOverlapsOpenReadableIndependentEntries() {
        setup(listOf(item(),item("b"),item("c")))
        compose.onNodeWithTag("calendar-overlap-day-1").performClick()
        compose.onNodeWithText("3 项交叠日程").assertIsDisplayed()
        compose.onNodeWithText("1. 西湖天地 · 09:40–11:10").performClick()
        compose.onNodeWithTag("calendar-detail").assertIsDisplayed()
    }
    @Test fun unsetCardsNeverBecomeRealOverlapAggregateAndFortyMinuteCardOpensOnTimeline() {
        setup(listOf(item(stay=null),item("b",stay=null),item("c",stay=null),item("short","12:00",40)))
        compose.onNodeWithTag("calendar-overlap-day-1").assertDoesNotExist()
        event().assertIsDisplayed()
        compose.onNodeWithTag("calendar-short-short").assertDoesNotExist()
        assertEquals(40 * CALENDAR_MINUTE_DP, event("short").fetchSemanticsNode().boundsInRoot.height, 1f)
        event("short").performScrollTo().performTouchInput { click(center) }
        compose.onNodeWithText("停留：40 分钟").assertIsDisplayed()
    }
    @Test fun wholeTripOverlapsOpenDayAndReadableGroupInOneTap() {
        setup(listOf(item(),item("b")),whole=true,count=2)
        compose.onNodeWithTag("calendar-overlap-day-1").performClick()
        compose.onNodeWithText("2 项交叠日程").assertIsDisplayed()
    }
    @Test fun crossMidnightTrafficHasOneReferenceAcrossTwoColumns() {
        setup(listOf(item(arrival="23:00",stay=30),item("b","23:45",30)),whole=true,count=2)
        compose.runOnIdle {
            val leg=com.yangchengwei.easytrip.itinerary.ui.RouteLegUi("night-leg","a","b",com.yangchengwei.easytrip.core.model.TransportMode.WALK,
                com.yangchengwei.easytrip.core.model.RouteStatus.SUCCESS,100,7200,null)
            raw.value=raw.value.mapIndexed { index, day -> if(index==0) day.copy(legs=listOf(leg)) else day }
        }
        compose.onNodeWithTag("calendar-route-night-leg").assertDoesNotExist()
        event().performScrollTo().assertIsDisplayed().performClick()
        compose.waitForIdle()
        event().performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithTag("calendar-detail").assertIsDisplayed()
        compose.onNodeWithTag("calendar-detail-route-night-leg").performScrollTo().assertIsDisplayed().performClick()
        assertEquals("night-leg", openedRoute)
        compose.onNodeWithTag("calendar-detail").assertDoesNotExist()
    }
    @Test fun calendarVisualEvidence() {
        setup(listOf(item(stay=90), item("unset","12:00",null),item("pending",null,90),item("overlap","10:10",60)))
        compose.onRoot().captureToImage().asAndroidBitmap().let { bitmap ->
            val file=java.io.File(compose.activity.getExternalFilesDir(null),"calendar-native.png")
            file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }
        }
        assertTrue(changes.isEmpty())
    }
}
