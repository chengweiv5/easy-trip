package com.yangchengwei.easytrip.itinerary.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import com.yangchengwei.easytrip.core.model.RouteStatus
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.model.TransportMode
import com.yangchengwei.easytrip.trip.domain.TripDay
import com.yangchengwei.easytrip.workspace.ItineraryScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class WholeTripItineraryContentTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun noTripDaysShowsIllustrationAndAddDayAction() {
        var addClicks = 0
        compose.setContent { WholeTripItineraryContent(emptyList(), onAddDay = { addClicks++ }) }

        compose.onNodeWithTag("itinerary-empty-illustration").assertIsDisplayed()
        compose.onNodeWithText("暂无旅行日").assertIsDisplayed()
        compose.onNodeWithTag("whole-trip-add-day").assertHasClickAction().performClick()
        assertEquals(1, addClicks)
    }

    @Test
    fun daysRenderInOrderWithEmptyDayAndReadOnlyTimeline() {
        val first = item("first", "早餐店")
        val second = item("second", "博物馆")
        val leg = RouteLegUi(
            id = "route",
            fromItemId = first.id,
            toItemId = second.id,
            mode = TransportMode.WALK,
            status = RouteStatus.SUCCESS,
            distanceMeters = 800,
            durationSeconds = 600,
            error = null,
        )
        compose.setContent {
            WholeTripItineraryContent(
                listOf(
                    WholeTripDayUi("day-1", 1, listOf(first, second), listOf(leg)),
                    WholeTripDayUi("day-2", 2, emptyList(), emptyList()),
                ),
            )
        }

        compose.onNodeWithTag("whole-trip-day-day-1").assertIsDisplayed()
        compose.onNodeWithTag("whole-trip-day-day-2").assertIsDisplayed()
        compose.onNodeWithText("第一天").assertIsDisplayed()
        compose.onNodeWithText("第二天").assertIsDisplayed()
        compose.onNodeWithText("暂无行程").assertIsDisplayed()
        assertEquals(
            listOf("item-first", "leg-route", "item-second"),
            compose.onRoot(useUnmergedTree = true).fetchSemanticsNode().timelineTags(),
        )
        compose.onNodeWithTag("item-first").assert(SemanticsMatcher.keyNotDefined(SemanticsActions.CustomActions))
        compose.onNodeWithTag("item-second").assert(SemanticsMatcher.keyNotDefined(SemanticsActions.CustomActions))
        val forbiddenPrefixes = listOf("timing-", "move-", "delete-", "mode-", "retry-", "more-", "drag-handle-")
        val allTags = compose.onRoot(useUnmergedTree = true).fetchSemanticsNode().allTags()
        assertTrue(allTags.none { tag -> forbiddenPrefixes.any(tag::startsWith) })
    }

    @Test
    fun wholeTripFitsWorkspaceWidthAndRemainsReadOnly() {
        val day = WholeTripDayUi("day-1", 1, listOf(item("first", "早餐店")), emptyList())
        compose.setContent {
            Box(Modifier.width(360.dp).height(220.dp).testTag("workspace-sheet")) {
                WorkspaceItineraryContent(
                    days = listOf(TripDay("day-1", 0)),
                    selected = ItineraryScope.WholeTrip,
                    wholeTripDays = listOf(day),
                    onSelect = {},
                    dayContent = {},
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                )
            }
        }

        val sheet = compose.onNodeWithTag("workspace-sheet").getUnclippedBoundsInRoot()
        val content = compose.onNodeWithTag("whole-trip-content").getUnclippedBoundsInRoot()
        assertTrue("sheet=$sheet content=$content", content.left >= sheet.left && content.right <= sheet.right)
        assertEquals(sheet.right - 20.dp, content.right)
        val forbiddenPrefixes = listOf("timing-", "move-", "delete-", "mode-", "retry-", "more-", "drag-handle-")
        val allTags = compose.onRoot(useUnmergedTree = true).fetchSemanticsNode().allTags()
        assertTrue(allTags.none { tag -> forbiddenPrefixes.any(tag::startsWith) })
    }

    @Test
    fun longWholeTripScrollsLastDayAboveBottomPadding() {
        val days = (1..12).map { day ->
            WholeTripDayUi("day-$day", day, listOf(item("item-$day", "地点 $day")), emptyList())
        }
        compose.setContent {
            Box(Modifier.width(360.dp).height(220.dp).testTag("whole-trip-viewport")) {
                WholeTripItineraryContent(days, modifier = Modifier.fillMaxSize())
            }
        }

        compose.onNodeWithTag("whole-trip-timeline").performScrollToNode(hasTestTag("item-item-12"))
        val viewport = compose.onNodeWithTag("whole-trip-viewport").getUnclippedBoundsInRoot()
        val lastItem = compose.onNodeWithTag("item-item-12").assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue("viewport=$viewport lastItem=$lastItem", lastItem.bottom <= viewport.bottom - 24.dp)
    }

    @Test
    fun usesChineseDayLabelsThroughTenAndNumericFallbackAfterwards() {
        compose.setContent {
            WholeTripItineraryContent(
                (1..11).map { WholeTripDayUi("day-$it", it, emptyList(), emptyList()) },
            )
        }

        compose.onAllNodesWithText("第十天").assertCountEquals(1)
        compose.onAllNodesWithText("第 11 天").assertCountEquals(1)
    }

    private fun item(id: String, name: String) = ItineraryItemUi(id, name, "", null, null)

    private fun SemanticsNode.timelineTags(): List<String> =
        listOfNotNull(
            if (config.contains(SemanticsProperties.TestTag)) {
                config[SemanticsProperties.TestTag].takeIf { it.startsWith("item-") || it.startsWith("leg-") }
            } else null,
        ) + children.flatMap { it.timelineTags() }

    private fun SemanticsNode.allTags(): List<String> =
        listOfNotNull(if (config.contains(SemanticsProperties.TestTag)) config[SemanticsProperties.TestTag] else null) +
            children.flatMap { it.allTags() }
}
