package com.yangchengwei.easytrip.itinerary.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assert
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.trip.domain.TripDay
import com.yangchengwei.easytrip.workspace.ItineraryScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ItineraryScopeRailTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun railHasFixedWidthLabelsAndSelectableScopes() {
        var selected: ItineraryScope by mutableStateOf(ItineraryScope.WholeTrip)
        compose.setContent {
            ItineraryScopeRail(
                days = days(),
                selected = selected,
                onSelect = { selected = it },
                modifier = Modifier.height(180.dp),
            )
        }

        compose.onNodeWithTag("itinerary-scope-rail").assertWidthIsEqualTo(88.dp)
        compose.onNodeWithTag("itinerary-scope-WHOLE_TRIP").assertIsDisplayed().assertIsSelected()
        compose.onNodeWithTag("itinerary-scope-day-1").assertExists()
        compose.onNodeWithText("全程").assertExists()
        compose.onNodeWithText("第一天").assertExists()
        compose.onNodeWithText("第二天").assertExists()

        compose.onNodeWithTag("itinerary-scope-rail").performScrollToNode(hasText("第二天"))
        compose.onNodeWithTag("itinerary-scope-day-2").performClick()
        compose.onNodeWithTag("itinerary-scope-WHOLE_TRIP").assertIsNotSelected()
        compose.onNodeWithTag("itinerary-scope-day-2").assertIsSelected()
        assertEquals(ItineraryScope.Day("day-2"), selected)
    }

    @Test
    fun railOrdersWholeTripIndexedDaysThenNonScopeAddAction() {
        var selected: ItineraryScope by mutableStateOf(ItineraryScope.WholeTrip)
        var addClicks = 0
        compose.setContent {
            ItineraryScopeRail(
                days = listOf(TripDay("day-2", 1), TripDay("day-1", 0)),
                selected = selected,
                onSelect = { selected = it },
                onAddDay = { addClicks++ },
                modifier = Modifier.height(220.dp),
            )
        }

        compose.onNodeWithTag("itinerary-scope-rail").performScrollToNode(hasTestTag("itinerary-add-day"))
        val wholeTop = compose.onNodeWithText("全程").fetchSemanticsNode().positionInRoot.y
        val firstTop = compose.onNodeWithText("第一天").fetchSemanticsNode().positionInRoot.y
        val secondTop = compose.onNodeWithText("第二天").fetchSemanticsNode().positionInRoot.y
        val addTop = compose.onNodeWithTag("itinerary-add-day").fetchSemanticsNode().positionInRoot.y
        assertTrue(wholeTop < firstTop && firstTop < secondTop && secondTop < addTop)

        compose.onNodeWithTag("itinerary-add-day")
            .assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Selected))
            .performClick()
        assertEquals(1, addClicks)
        assertEquals(ItineraryScope.WholeTrip, selected)
    }

    @Test
    fun tripWithoutDaysStillShowsAddAction() {
        compose.setContent {
            ItineraryScopeRail(
                days = emptyList(),
                selected = ItineraryScope.WholeTrip,
                onSelect = {},
                onAddDay = {},
            )
        }

        compose.onNodeWithTag("itinerary-add-day").assertIsDisplayed()
        compose.onAllNodesWithTag("itinerary-scope-day-1").assertCountEquals(0)
    }

    @Test
    fun addTripDayContentReportsBusyAndErrorWithoutOwningState() {
        var confirms = 0
        var closes = 0
        var isAppending by mutableStateOf(false)
        var error by mutableStateOf<String?>("新增失败")
        compose.setContent {
            AddTripDayContent(
                isAppending = isAppending,
                error = error,
                onConfirm = { confirms++ },
                onClose = { closes++ },
            )
        }

        compose.onNodeWithText("新增失败").assertIsDisplayed()
        compose.onNodeWithText("添加一天").performClick()
        compose.onNodeWithText("取消").performClick()
        assertEquals(1, confirms)
        assertEquals(1, closes)

        compose.runOnIdle {
            isAppending = true
            error = null
        }
        compose.onNodeWithText("添加中…").assertIsDisplayed()
    }

    @Test
    fun railAddDayActionInvokesCallbackOnce() {
        var addClicks = 0
        compose.setContent {
            WorkspaceItineraryContent(
                days = emptyList(),
                selected = ItineraryScope.WholeTrip,
                wholeTripDays = emptyList(),
                onSelect = {},
                dayContent = {},
                onAddDay = { addClicks++ },
                modifier = Modifier.height(300.dp),
            )
        }

        compose.onNodeWithTag("itinerary-add-day").performClick()
        assertEquals(1, addClicks)
    }

    @Test
    fun wholeTripEmptyStateAddDayActionInvokesCallbackOnce() {
        var addClicks = 0
        compose.setContent {
            WorkspaceItineraryContent(
                days = emptyList(),
                selected = ItineraryScope.WholeTrip,
                wholeTripDays = emptyList(),
                onSelect = {},
                dayContent = {},
                onAddDay = { addClicks++ },
                modifier = Modifier.height(300.dp),
            )
        }

        compose.onNodeWithTag("whole-trip-add-day").performClick()
        assertEquals(1, addClicks)
    }

    @Test
    fun scrollingRailRevealsLastDayWithoutMovingRightContent() {
        compose.setContent {
            WorkspaceItineraryContent(
                days = days(),
                selected = ItineraryScope.Day("day-1"),
                wholeTripDays = emptyList(),
                onSelect = {},
                dayContent = { Box(Modifier.fillMaxSize().testTag("right-sentinel")) },
                modifier = Modifier.height(180.dp),
            )
        }
        val before = compose.onNodeWithTag("right-sentinel").fetchSemanticsNode().positionInRoot

        compose.onNodeWithTag("itinerary-scope-rail").performScrollToNode(hasText("第七天"))
        compose.onNodeWithText("第七天").assertIsDisplayed()
        val after = compose.onNodeWithTag("right-sentinel").fetchSemanticsNode().positionInRoot

        assertEquals(before, after)
    }

    @Test
    fun scopeRailAndDayContentStayInsideSheetBounds() {
        compose.setContent {
            Box(Modifier.width(360.dp).height(220.dp).testTag("workspace-sheet")) {
                WorkspaceItineraryContent(
                    days = days(),
                    selected = ItineraryScope.Day("day-1"),
                    wholeTripDays = emptyList(),
                    onSelect = {},
                    dayContent = { Box(Modifier.fillMaxSize().testTag("day-content")) },
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                )
            }
        }

        val sheet = compose.onNodeWithTag("workspace-sheet").getUnclippedBoundsInRoot()
        val rail = compose.onNodeWithTag("itinerary-scope-rail").getUnclippedBoundsInRoot()
        val day = compose.onNodeWithTag("day-content").getUnclippedBoundsInRoot()
        assertTrue("sheet=$sheet rail=$rail", rail.left >= sheet.left && rail.right <= sheet.right)
        assertTrue("sheet=$sheet day=$day", day.left >= sheet.left && day.right <= sheet.right)
        assertEquals(sheet.left + 20.dp, rail.left)
        assertEquals(sheet.right - 20.dp, day.right)
    }

    @Test
    fun workspaceContentChoosesOnlyTheSelectedScopeRenderer() {
        var selected: ItineraryScope by mutableStateOf(ItineraryScope.WholeTrip)
        val wholeDay = WholeTripDayUi("day-1", 1, emptyList(), emptyList())
        compose.setContent {
            WorkspaceItineraryContent(
                days = days(),
                selected = selected,
                wholeTripDays = listOf(wholeDay),
                onSelect = { selected = it },
                dayContent = { Text("单日内容", Modifier.testTag("day-content")) },
                modifier = Modifier.height(180.dp),
            )
        }
        compose.onNodeWithTag("whole-trip-day-day-1").assertExists()
        compose.onNodeWithTag("day-content").assertDoesNotExist()

        compose.runOnIdle { selected = ItineraryScope.Day("day-1") }
        compose.onNodeWithTag("whole-trip-day-day-1").assertDoesNotExist()
        compose.onNodeWithTag("day-content").assertExists()
    }

    private fun days() = (1..7).map { TripDay("day-$it", it - 1) }
}
