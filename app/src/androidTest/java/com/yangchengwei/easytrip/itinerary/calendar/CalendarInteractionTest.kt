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
            down(from); moveTo(from+Offset(0f,delta), 300); up()
        }
    }
    @Test fun resizeCuesFollowActiveEdgeAndDisappearAfterReleaseOrCancel() {
        setup()
        compose.onNodeWithTag("calendar-resize-start").assertDoesNotExist()
        compose.onNodeWithTag("calendar-resize-end").assertDoesNotExist()
        event().performTouchInput { down(Offset(width * .05f, 2f)) }
        val startCue = compose.onNodeWithTag("calendar-resize-start", useUnmergedTree = true)
        startCue.assertIsDisplayed()
        compose.onNodeWithTag("calendar-resize-end").assertDoesNotExist()
        compose.onNodeWithTag("calendar-draft-label").assertTextContains("调整开始", substring = true)
        val first = startCue.fetchSemanticsNode().boundsInRoot
        val originalDraft = compose.onNodeWithTag("calendar-draft").fetchSemanticsNode().boundsInRoot
        assertEquals(originalDraft.center.x, first.center.x, 1f)
        assertEquals(originalDraft.top, first.bottom, 1f)
        saveResizeEvidence("calendar-resize-start.jpg")
        event().performTouchInput { moveBy(Offset(0f, -26f), 300) }
        assertEquals(first.top - 26f, startCue.fetchSemanticsNode().boundsInRoot.top, 1f)
        event().performTouchInput { up() }
        startCue.assertDoesNotExist()
        assertEquals(1, changes.size)

        event().performTouchInput { down(Offset(width * .95f, height - 2f)) }
        val endCue = compose.onNodeWithTag("calendar-resize-end", useUnmergedTree = true)
        endCue.assertIsDisplayed()
        compose.onNodeWithTag("calendar-resize-start").assertDoesNotExist()
        compose.onNodeWithTag("calendar-draft-label").assertTextContains("调整结束", substring = true)
        val lower = endCue.fetchSemanticsNode().boundsInRoot
        val draftBounds = compose.onNodeWithTag("calendar-draft").fetchSemanticsNode().boundsInRoot
        assertEquals(first.center.x, lower.center.x, 1f)
        assertEquals(first.width, lower.width, 1f)
        assertEquals(first.height, lower.height, 1f)
        assertEquals(draftBounds.bottom, lower.top, 1f)
        saveResizeEvidence("calendar-resize-end.jpg")
        event().performTouchInput { moveBy(Offset(0f, 26f), 300); cancel() }
        endCue.assertDoesNotExist()
        assertEquals(1, changes.size)
    }

    @Test fun unsetStayShowsEdgeCueImmediatelyAndBodyMoveDoesNot() {
        setup(listOf(item(stay = null)))
        event().performTouchInput { down(Offset(width * .5f, height - 2f)) }
        compose.onNodeWithTag("calendar-resize-end", useUnmergedTree = true).assertIsDisplayed()
        event().performTouchInput { cancel() }
        compose.onNodeWithTag("calendar-resize-end").assertDoesNotExist()
        event().performTouchInput { down(center); advanceEventTime(500); moveBy(Offset(0f, 26f), 300) }
        compose.onNodeWithTag("calendar-draft").assertIsDisplayed()
        compose.onNodeWithTag("calendar-resize-start").assertDoesNotExist()
        compose.onNodeWithTag("calendar-resize-end").assertDoesNotExist()
        event().performTouchInput { cancel() }
        assertTrue(changes.isEmpty())
    }

    @Test fun midnightResizeCueRemainsVisibleInsideViewport() {
        setup(listOf(item(arrival = "00:00")))
        event().performTouchInput { down(Offset(width * .5f, 2f)) }
        val cue = compose.onNodeWithTag("calendar-resize-start", useUnmergedTree = true)
        cue.assertIsDisplayed()
        val viewport = compose.onNodeWithTag("calendar-viewport").fetchSemanticsNode().boundsInRoot
        val bounds = cue.fetchSemanticsNode().boundsInRoot
        assertTrue(bounds.top >= viewport.top)
        assertEquals(20f, bounds.height, 1f)
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
        event().performTouchInput { down(center); advanceEventTime(500); moveBy(Offset(0f,26f),300); up() }
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
        pending.performTouchInput { down(center); advanceEventTime(500); moveBy(delta,400); up() }
        assertEquals(90, changes.single().after.stayMinutes)
        assertNull(changes.single().before.arrivalTime)
        compose.onNodeWithTag("calendar-pending-pending").assertDoesNotExist()
    }
    @Test fun pendingOutsideDropAndCancelledGestureDoNotWrite() {
        setup(listOf(item(),item("pending",null,null)))
        compose.onNodeWithTag("calendar-pending-pending").performTouchInput { down(center); advanceEventTime(500); moveBy(Offset(-200f,100f),300); up() }
        assertTrue(changes.isEmpty())
        compose.onNodeWithTag("calendar-pending-pending").performTouchInput { down(center); advanceEventTime(500); moveBy(Offset(0f,120f),300); cancel() }
        assertTrue(changes.isEmpty())
    }
    @Test fun wholeTripIsReadOnlyAndTapFocusesSourceDay() {
        setup(whole=true,count=3)
        event().performTouchInput { down(center); advanceEventTime(500); moveBy(Offset(0f,26f),300); up() }
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
    @Test fun shortAndCrossMidnightEventsOpenPreciseDetails() {
        setup(listOf(item(stay=10),item("late","23:30",120)))
        compose.onNodeWithTag("calendar-short-a").assertIsDisplayed().performClick()
        compose.onNodeWithTag("calendar-detail-edit").assertIsDisplayed()
    }
    @Test fun movingAfterWholeToSingleUsesCurrentDayAndKeyboardCancelsOrCommits() {
        setup(whole=true,count=3)
        event().assertIsDisplayed().performClick()
        compose.waitForIdle()
        event().performTouchInput { down(center); advanceEventTime(500); moveBy(Offset(0f,26f),300); up() }
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
        event().performTouchInput { down(center); advanceEventTime(500); moveBy(Offset(0f,viewport.bottom-12-bounds.center.y),300) }
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
        event().performTouchInput { down(center); advanceEventTime(500); moveBy(Offset(0f,26f),300) }
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
    @Test fun unsetCardsNeverBecomeRealOverlapAggregateAndFortyMinuteCardHasPreciseTarget() {
        setup(listOf(item(stay=null),item("b",stay=null),item("c",stay=null),item("short","12:00",40)))
        compose.onNodeWithTag("calendar-overlap-day-1").assertDoesNotExist()
        event().assertIsDisplayed()
        compose.onNodeWithTag("calendar-short-short").assertHeightIsAtLeast(48.dp)
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
