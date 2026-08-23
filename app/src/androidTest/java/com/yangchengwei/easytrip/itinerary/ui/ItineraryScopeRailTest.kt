package com.yangchengwei.easytrip.itinerary.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasText
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.trip.domain.TripDay
import com.yangchengwei.easytrip.workspace.ItineraryScope
import org.junit.Assert.assertEquals
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
