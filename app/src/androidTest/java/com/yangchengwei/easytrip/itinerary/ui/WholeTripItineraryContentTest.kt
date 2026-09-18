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
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import com.yangchengwei.easytrip.core.model.RouteStatus
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.model.TransportMode
import com.yangchengwei.easytrip.trip.domain.TripDay
import com.yangchengwei.easytrip.workspace.ItineraryScope
import java.time.LocalDate
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

        compose.onNodeWithTag("empty-illustration-itinerary").assertIsDisplayed()
        compose.onNodeWithText("暂无旅行日").assertIsDisplayed()
        compose.onNodeWithTag("whole-trip-add-day").assertHasClickAction().performClick()
        assertEquals(1, addClicks)
    }

    @Test
    fun singleEmptyDayKeepsScopeRailAndAddsFromCurrentDayInsteadOfWholeTripEmptyState() {
        var selected: ItineraryScope = ItineraryScope.Day("day-2")
        var addClicks = 0
        compose.setContent {
            WorkspaceItineraryContent(
                days = listOf(TripDay("day-1", 0), TripDay("day-2", 1)),
                selected = selected,
                wholeTripDays = listOf(
                    WholeTripDayUi("day-1", 1, listOf(item("first", "早餐店")), emptyList()),
                    WholeTripDayUi("day-2", 2, emptyList(), emptyList()),
                ),
                onSelect = { selected = it },
                onAddDay = {},
                dayContent = {
                    DayItineraryContent(
                        state = DayItineraryUiState(
                            days = listOf(TripDay("day-1", 0), TripDay("day-2", 1)),
                            selectedDayId = "day-2",
                        ),
                        onAction = { if (it == DayItineraryAction.AddPlaces) addClicks++ },
                        showDialogs = false,
                    )
                },
            )
        }

        compose.onNodeWithTag("itinerary-scope-rail").assertIsDisplayed()
        compose.onNodeWithTag("itinerary-scope-day-2").assertIsDisplayed()
        compose.onNodeWithText("第 2 天 · 暂无行程").assertIsDisplayed()
        compose.onNodeWithTag("add-places-to-selected-day").performClick()
        assertEquals(1, addClicks)
        compose.onAllNodesWithTag("itinerary-all-empty").assertCountEquals(0)
    }

    @Test
    fun wholeTripHeaderSummarizesDaysAndStopsWhileDayGroupsShowContinuousDates() {
        compose.setContent {
            WholeTripItineraryContent(
                days = listOf(
                    WholeTripDayUi("day-1", 1, listOf(item("first", "早餐店")), emptyList()),
                    WholeTripDayUi("day-2", 2, emptyList(), emptyList()),
                    WholeTripDayUi("day-3", 3, listOf(item("last", "晚餐店")), emptyList()),
                ),
                startDate = LocalDate.parse("2026-09-06"),
            )
        }

        compose.onNodeWithTag("whole-trip-summary").assertIsDisplayed()
            .assertHeightIsEqualTo(36.dp)
        compose.onNodeWithText("全程 · 3 天 · 2 站").assertIsDisplayed()
        compose.onNodeWithText("第 1 天 · 1 站").assertIsDisplayed()
        compose.onNodeWithText("第 2 天 · 0 站").assertIsDisplayed()
        compose.onNodeWithText("第 3 天 · 1 站").assertIsDisplayed()
        compose.onNodeWithText("9 月 6 日").assertIsDisplayed()
        compose.onNodeWithText("9 月 7 日").assertIsDisplayed()
        compose.onNodeWithText("9 月 8 日").assertIsDisplayed()
        listOf("day-1", "day-2", "day-3").forEachIndexed { index, dayId ->
            val title = compose.onNodeWithText("第 ${index + 1} 天 · ${if (index == 1) 0 else 1} 站", useUnmergedTree = true).getUnclippedBoundsInRoot()
            val date = compose.onNodeWithText("9 月 ${index + 6} 日", useUnmergedTree = true).getUnclippedBoundsInRoot()
            val titleCenter = (title.top + title.bottom) / 2f
            val dateCenter = (date.top + date.bottom) / 2f
            assertTrue("title=$title date=$date", kotlin.math.abs(titleCenter.value - dateCenter.value) < 0.01f)
            val gap = date.left - title.right
            assertTrue("title=$title date=$date", gap in 0.dp..8.5.dp)
        }
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
        compose.onNodeWithText("第 1 天 · 2 站").assertIsDisplayed()
        compose.onNodeWithText("第 2 天 · 0 站").assertIsDisplayed()
        compose.onNodeWithText("暂无行程").assertIsDisplayed()
        assertEquals(
            listOf("item-first", "leg-route", "item-second"),
            compose.onRoot(useUnmergedTree = true).fetchSemanticsNode().timelineTags(),
        )
        compose.onNodeWithTag("item-first").assert(SemanticsMatcher.keyNotDefined(SemanticsActions.CustomActions))
        compose.onNodeWithTag("item-second").assert(SemanticsMatcher.keyNotDefined(SemanticsActions.CustomActions))
        compose.onNodeWithTag("route-leg-action-route")
            .assertContentDescriptionEquals("从早餐店到博物馆的路段")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
        compose.onAllNodesWithText("早餐店 → 博物馆").assertCountEquals(0)
        val forbiddenPrefixes = listOf("timing-", "move-", "delete-", "mode-", "retry-", "more-", "drag-handle-")
        val allTags = compose.onRoot(useUnmergedTree = true).fetchSemanticsNode().allTags()
        assertTrue(allTags.none { tag -> forbiddenPrefixes.any(tag::startsWith) })
    }

    @Test
    fun wholeTripTimingSummaryStaysOnOneLineAtNormalWidth() {
        compose.setContent {
            Box(Modifier.width(320.dp)) {
                WholeTripItineraryContent(
                    days = listOf(
                        WholeTripDayUi(
                            "day-1",
                            1,
                            listOf(ItineraryItemUi("place", "灵隐寺", "法云弄1号", java.time.LocalTime.parse("09:30"), 120)),
                            emptyList(),
                        ),
                    ),
                )
            }
        }

        val node = compose.onNodeWithText("09:30 到达 · 停留 120 分钟").fetchSemanticsNode()
        val results = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        checkNotNull(node.config[SemanticsActions.GetTextLayoutResult].action).invoke(results)
        assertEquals(1, results.single().lineCount)
    }

    @Test
    fun wholeTripUsesEightDpGapBetweenAdjacentPlaceAndRouteBlocks() {
        val first = item("first", "早餐店")
        val second = item("second", "博物馆")
        compose.setContent {
            WholeTripItineraryContent(
                days = listOf(
                    WholeTripDayUi(
                        "day-1",
                        1,
                        listOf(first, second),
                        listOf(
                            RouteLegUi(
                                id = "route",
                                fromItemId = first.id,
                                toItemId = second.id,
                                mode = TransportMode.WALK,
                                status = RouteStatus.SUCCESS,
                                distanceMeters = 800,
                                durationSeconds = 600,
                                error = null,
                            ),
                        ),
                    ),
                ),
            )
        }

        val firstPlace = compose.onNodeWithTag("item-first").getUnclippedBoundsInRoot()
        val route = compose.onNodeWithTag("leg-route").getUnclippedBoundsInRoot()
        val secondPlace = compose.onNodeWithTag("item-second").getUnclippedBoundsInRoot()
        assertEquals(8.dp, route.top - firstPlace.bottom)
        assertEquals(8.dp, secondPlace.top - route.bottom)
    }

    @Test
    fun wholeTripAxisFollowsPlaceTextAndRouteTextKeepsTwentyFiveDpOffset() {
        val first = item("first", "早餐店")
        val second = item("second", "博物馆")
        compose.setContent {
            WholeTripItineraryContent(
                days = listOf(
                    WholeTripDayUi(
                        "day-1",
                        1,
                        listOf(first, second),
                        listOf(
                            RouteLegUi(
                                id = "route",
                                fromItemId = first.id,
                                toItemId = second.id,
                                mode = TransportMode.WALK,
                                status = RouteStatus.SUCCESS,
                                distanceMeters = 800,
                                durationSeconds = 600,
                                error = null,
                            ),
                        ),
                    ),
                ),
            )
        }

        val placeName = compose.onNodeWithText("早餐店").getUnclippedBoundsInRoot()
        val connector = compose.onNodeWithTag("route-connector-route", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val routeLabel = compose.onNodeWithText("步行").getUnclippedBoundsInRoot()
        assertEquals(placeName.left, connector.left + 6.dp)
        val route = compose.onNodeWithTag("leg-route").getUnclippedBoundsInRoot()
        assertEquals(route.left + 35.dp, routeLabel.left)
        assertEquals(25.dp, routeLabel.left - placeName.left)
    }

    @Test
    fun wholeTripLongFailureKeepsConnectorContinuousAndReadOnly() {
        val first = item("first", "早餐店")
        val second = item("second", "博物馆")
        val error = "这是一段很长很长的路线错误信息，用于验证全程行程中的连接线会随两行错误文本一起伸缩"
        val leg = RouteLegUi(
            id = "long-route",
            fromItemId = first.id,
            toItemId = second.id,
            mode = TransportMode.WALK,
            status = RouteStatus.FAILED,
            distanceMeters = null,
            durationSeconds = null,
            error = error,
        )
        compose.setContent {
            Box(Modifier.width(220.dp)) {
                WholeTripItineraryContent(listOf(WholeTripDayUi("day-1", 1, listOf(first, second), listOf(leg))))
            }
        }

        val legBounds = compose.onNodeWithTag("leg-long-route").getUnclippedBoundsInRoot()
        val connectorBounds = compose.onNodeWithTag("route-connector-long-route", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        assertEquals(legBounds.bottom - legBounds.top, connectorBounds.bottom - connectorBounds.top)
        compose.onAllNodesWithTag("retry-long-route").assertCountEquals(0)
        compose.onAllNodesWithTag("mode-long-route").assertCountEquals(0)
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

        compose.onNodeWithTag("whole-trip-timeline").performScrollToNode(hasTestTag("whole-trip-bottom-spacer"))
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

        compose.onNodeWithTag("whole-trip-timeline").performScrollToNode(hasTestTag("whole-trip-day-day-10"))
        compose.onNodeWithText("第 10 天 · 0 站").assertIsDisplayed()
        compose.onNodeWithTag("whole-trip-timeline").performScrollToNode(hasTestTag("whole-trip-day-day-11"))
        compose.onNodeWithText("第 11 天 · 0 站").assertIsDisplayed()
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
