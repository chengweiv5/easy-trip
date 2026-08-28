package com.yangchengwei.easytrip.itinerary.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.core.model.RouteStatus
import com.yangchengwei.easytrip.core.model.TransportMode
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import com.yangchengwei.easytrip.place.domain.SavedPlace
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ItineraryTimelineContentTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun dayTimelineInterleavesPreviewItemsAndAdjacentLegs() {
        val state = DayItineraryUiState(
            items = listOf(
                itineraryItem("i1", "灵隐寺", "法云弄1号", "09:30", 120),
                itineraryItem("i2", "知味观", "仁和路83号", "12:00", 60),
                itineraryItem("i3", "西湖", "龙井路1号", "15:00", 90),
            ),
            legs = listOf(
                routeLeg("first-adjacent", RouteStatus.SUCCESS, fromItemId = "i3", toItemId = "i1"),
                routeLeg("second-adjacent", RouteStatus.SUCCESS, fromItemId = "i1", toItemId = "i2"),
                routeLeg("non-adjacent", RouteStatus.SUCCESS, fromItemId = "i3", toItemId = "i2"),
            ),
            previewOrder = listOf("i3", "i1", "i2"),
        )
        compose.setContent { EasyTripTheme { DayItineraryContent(state, onAction = {}) } }

        val timelineTags = compose.onRoot().fetchSemanticsNode().timelineTags()
        assertEquals(
            listOf("item-i3", "leg-first-adjacent", "item-i1", "leg-second-adjacent", "item-i2"),
            timelineTags,
        )
    }

    @Test
    fun emptyDayShowsIllustrationTitleMessageAndAddAction() {
        val state = DayItineraryUiState(
            days = listOf(com.yangchengwei.easytrip.trip.domain.TripDay("day-1", 0)),
            selectedDayId = "day-1",
        )
        compose.setContent { EasyTripTheme { DayItineraryContent(state, onAction = {}) } }

        compose.onNodeWithTag("empty-illustration-itinerary").assertIsDisplayed()
        compose.onNodeWithText("第1天 · 暂无行程").assertIsDisplayed()
        compose.onNodeWithText("从地点池添加地点，开始安排这一天").assertIsDisplayed()
        compose.onNodeWithTag("add-places-to-selected-day").assertIsDisplayed()
    }

    @Test
    fun emptyDayAddDispatchesAddPlacesOnly() {
        val actions = mutableListOf<DayItineraryAction>()
        val state = DayItineraryUiState(
            days = listOf(com.yangchengwei.easytrip.trip.domain.TripDay("day-1", 0)),
            selectedDayId = "day-1",
        )
        compose.setContent { EasyTripTheme { DayItineraryContent(state, onAction = actions::add) } }

        compose.onNodeWithTag("add-places-to-selected-day").performClick()
        compose.runOnIdle { assertEquals(listOf(DayItineraryAction.AddPlaces), actions) }
    }

    @Test
    fun emptyDayWithSavedPlacesOffersOnlyAddPlacesFlow() {
        val actions = mutableListOf<DayItineraryAction>()
        val state = DayItineraryUiState(
            days = listOf(com.yangchengwei.easytrip.trip.domain.TripDay("day-1", 0)),
            selectedDayId = "day-1",
            savedPlaces = listOf(
                SavedPlace("saved-1", "trip", "poi", "灵隐寺", "", GeoPoint(30.2, 120.1), "", emptyList()),
            ),
        )
        compose.setContent { EasyTripTheme { DayItineraryContent(state, onAction = actions::add) } }

        compose.onAllNodesWithTag("add-place-saved-1").assertCountEquals(0)
        compose.onNodeWithTag("add-places-to-selected-day").performClick()
        compose.runOnIdle { assertEquals(listOf(DayItineraryAction.AddPlaces), actions) }
    }

    @Test
    fun noSelectedDayDoesNotOfferImplicitDayCreation() {
        compose.setContent {
            EasyTripTheme { DayItineraryContent(DayItineraryUiState(), onAction = {}) }
        }

        compose.onNodeWithTag("add-places-to-selected-day").assertDoesNotExist()
        compose.onAllNodesWithText("新增旅行日").assertCountEquals(0)
        compose.onAllNodesWithText("暂无旅行日").assertCountEquals(0)
    }

    @Test
    fun longTimelineScrollsLastItemAboveBottomPadding() {
        val items = (1..8).map {
            itineraryItem("i$it", "地点$it", "地址$it", "09:30", 60)
        }
        val state = DayItineraryUiState(items = items, previewOrder = items.map { it.id })
        compose.setContent {
            EasyTripTheme {
                DayItineraryContent(state, Modifier.height(260.dp), onAction = {})
            }
        }

        compose.onNodeWithTag("day-itinerary-timeline").performScrollToIndex(7)
        compose.onNodeWithTag("item-i8").assertIsDisplayed()
        val listBottom = compose.onNodeWithTag("day-itinerary-timeline").getUnclippedBoundsInRoot().bottom
        val itemBottom = compose.onNodeWithTag("item-i8").getUnclippedBoundsInRoot().bottom
        assertTrue("listBottom=$listBottom itemBottom=$itemBottom", itemBottom < listBottom)
    }

    @Test
    fun compactPlaceRowShowsArrivalNameStayAndAddress() {
        compose.setContent {
            EasyTripTheme {
                ItineraryPlaceRow(
                    item = itineraryItem(
                        id = "i1",
                        name = "灵隐寺",
                        address = "浙江省杭州市西湖区法云弄1号",
                        arrivalTime = "09:30",
                        stayMinutes = 120,
                    ),
                    displayOrder = 1,
                )
            }
        }

        compose.onNodeWithText("09:30").assertIsDisplayed()
        compose.onNodeWithText("灵隐寺").assertIsDisplayed()
        compose.onNodeWithText("停留 120 分钟").assertIsDisplayed()
        compose.onNodeWithText("浙江省杭州市西湖区法云弄1号").assertIsDisplayed()
    }

    @Test
    fun longTextDoesNotPushTrailingActionOutsideRow() {
        compose.setContent {
            EasyTripTheme {
                ItineraryPlaceContent(
                    item = itineraryItem(
                        id = "long",
                        name = "一段很长很长很长很长很长很长的地点名称",
                        address = "一段很长很长很长很长很长很长很长很长的地点地址",
                        arrivalTime = "09:30",
                        stayMinutes = 120,
                    ),
                    displayOrder = 1,
                    modifier = Modifier.width(320.dp),
                    trailingAction = { Box(Modifier.size(40.dp).testTag("trailing-long")) },
                )
            }
        }

        val row = compose.onNodeWithTag("item-long").getUnclippedBoundsInRoot()
        val trailing = compose.onNodeWithTag("trailing-long").getUnclippedBoundsInRoot()
        assertTrue("row=$row trailing=$trailing", trailing.right <= row.right)
    }

    @Test
    fun editableAndReadOnlyRowsSharePlaceContentGeometry() {
        compose.setContent {
            EasyTripTheme {
                Row {
                    ItineraryPlaceRow(
                        item = itineraryItem("read-only", "灵隐寺", "法云弄1号", "09:30", 120),
                        displayOrder = 1,
                        modifier = Modifier.width(180.dp),
                    )
                    ItineraryPlaceContent(
                        item = itineraryItem("editable", "灵隐寺", "法云弄1号", "09:30", 120),
                        displayOrder = 1,
                        modifier = Modifier.width(180.dp),
                    )
                }
            }
        }

        val readOnly = compose.onNodeWithTag("item-read-only").getUnclippedBoundsInRoot()
        val editable = compose.onNodeWithTag("item-editable").getUnclippedBoundsInRoot()
        assertEquals(readOnly.right - readOnly.left, editable.right - editable.left)
        assertEquals(readOnly.bottom - readOnly.top, editable.bottom - editable.top)
    }

    @Test
    fun itemShowsOnlyHandleAndMoreAsPermanentActions() {
        compose.setContent {
            EasyTripTheme {
                ItineraryItemRow(
                    item = itineraryItem("i1", "灵隐寺", "法云弄1号", "09:30", 120),
                    index = 0,
                    count = 1,
                    onPreview = {},
                    onCommit = {},
                    onMenuAction = {},
                )
            }
        }

        compose.onNodeWithTag("drag-handle-i1", useUnmergedTree = true).assertIsDisplayed()
        val handleBounds = compose.onNodeWithTag("drag-handle-i1", useUnmergedTree = true)
            .assertIsDisplayed()
            .getUnclippedBoundsInRoot()
        val menuBounds = compose.onNodeWithTag("more-i1", useUnmergedTree = true)
            .assertIsDisplayed()
            .getUnclippedBoundsInRoot()
        assertEquals(40f, (menuBounds.right - menuBounds.left).value, 0.5f)
        assertEquals(40f, (menuBounds.bottom - menuBounds.top).value, 0.5f)
        assertTrue("handle=$handleBounds menu=$menuBounds", handleBounds.right <= menuBounds.left)
        compose.onNodeWithTag("more-icon-i1", useUnmergedTree = true)
            .assertIsDisplayed()
            .let { icon ->
                val iconBounds = icon.getUnclippedBoundsInRoot()
                assertEquals(22f, (iconBounds.right - iconBounds.left).value, 0.5f)
                assertEquals(22f, (iconBounds.bottom - iconBounds.top).value, 0.5f)
            }
        compose.onAllNodesWithTag("timing-i1", useUnmergedTree = true).assertCountEquals(0)
        compose.onAllNodesWithTag("move-i1", useUnmergedTree = true).assertCountEquals(0)
        compose.onAllNodesWithTag("delete-i1", useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun itemMenuDispatchesTimingMoveAndDeleteForCurrentItem() {
        val actions = mutableListOf<ItineraryItemMenuAction>()
        compose.setContent {
            EasyTripTheme {
                ItineraryItemRow(
                    item = itineraryItem("i1", "灵隐寺", "法云弄1号", "09:30", 120),
                    index = 0,
                    count = 1,
                    onPreview = {},
                    onCommit = {},
                    onMenuAction = actions::add,
                )
            }
        }

        listOf(
            "menu-timing-i1" to ItineraryItemMenuAction.EditTiming,
            "menu-move-i1" to ItineraryItemMenuAction.MoveToOtherDay,
            "menu-delete-i1" to ItineraryItemMenuAction.Delete,
        ).forEach { (tag, expected) ->
            compose.onNodeWithTag("more-i1", useUnmergedTree = true).performClick()
            compose.onNodeWithTag(tag, useUnmergedTree = true).performClick()
            compose.runOnIdle { assertEquals(expected, actions.last()) }
        }
    }

    @Test
    fun dismissingMenuDoesNotDispatchBusinessAction() {
        val actions = mutableListOf<ItineraryItemMenuAction>()
        compose.setContent {
            EasyTripTheme {
                ItineraryItemRow(
                    item = itineraryItem("i1", "灵隐寺", "法云弄1号", "09:30", 120),
                    index = 0,
                    count = 1,
                    onPreview = {},
                    onCommit = {},
                    onMenuAction = actions::add,
                )
            }
        }

        compose.onNodeWithTag("more-i1", useUnmergedTree = true).performClick()
        compose.onNodeWithText("编辑时间与停留时长").assertIsDisplayed()
        compose.onNodeWithTag("more-i1", useUnmergedTree = true).performClick()
        compose.onAllNodesWithText("编辑时间与停留时长").assertCountEquals(0)
        compose.runOnIdle { assertTrue(actions.isEmpty()) }
    }

    @Test
    fun moreMenuDescriptionContainsPlaceName() {
        compose.setContent {
            EasyTripTheme {
                ItineraryItemRow(
                    item = itineraryItem("i1", "灵隐寺", "法云弄1号", "09:30", 120),
                    index = 0,
                    count = 1,
                    onPreview = {},
                    onCommit = {},
                    onMenuAction = {},
                )
            }
        }

        compose.onNodeWithTag("more-i1", useUnmergedTree = true)
            .assertContentDescriptionEquals("灵隐寺，更多行程项操作")
    }

    @Test
    fun longPressOnPlaceBodyDoesNotReorder() {
        val previews = mutableListOf<Int>()
        val commits = mutableListOf<Int>()
        compose.setContent {
            EasyTripTheme {
                ItineraryItemRow(
                    item = itineraryItem("i1", "灵隐寺", "法云弄1号", "09:30", 120),
                    index = 1,
                    count = 3,
                    onPreview = previews::add,
                    onCommit = commits::add,
                    onMenuAction = {},
                )
            }
        }

        compose.onNodeWithTag("item-i1").performTouchInput { longPressDragBy(-300f) }

        compose.runOnIdle {
            assertTrue(previews.isEmpty())
            assertTrue(commits.isEmpty())
        }
    }

    @Test
    fun longPressOnMoreDoesNotReorder() {
        val previews = mutableListOf<Int>()
        val commits = mutableListOf<Int>()
        compose.setContent {
            EasyTripTheme {
                ItineraryItemRow(
                    item = itineraryItem("i1", "灵隐寺", "法云弄1号", "09:30", 120),
                    index = 1,
                    count = 3,
                    onPreview = previews::add,
                    onCommit = commits::add,
                    onMenuAction = {},
                )
            }
        }

        compose.onNodeWithTag("more-i1", useUnmergedTree = true).performTouchInput { longPressDragBy(-300f) }

        compose.runOnIdle {
            assertTrue(previews.isEmpty())
            assertTrue(commits.isEmpty())
        }
    }

    @Test
    fun longPressAndDragOnHandleCommitsReorder() {
        val previews = mutableListOf<Int>()
        val commits = mutableListOf<Int>()
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 1f)) {
                EasyTripTheme {
                    ItineraryItemRow(
                        item = itineraryItem("i1", "灵隐寺", "法云弄1号", "09:30", 120),
                        index = 1,
                        count = 3,
                        onPreview = previews::add,
                        onCommit = commits::add,
                        onMenuAction = {},
                    )
                }
            }
        }

        compose.onNodeWithTag("drag-handle-i1", useUnmergedTree = true).performTouchInput { longPressDragBy(-130f) }

        compose.runOnIdle {
            assertEquals(0, previews.last())
            assertEquals(listOf(0), commits)
        }
    }

    @Test
    fun activeDragUsesLatestCallbacksAfterRecomposition() {
        val oldPreviews = mutableListOf<Int>()
        val newPreviews = mutableListOf<Int>()
        val oldCommits = mutableListOf<Int>()
        val newCommits = mutableListOf<Int>()
        lateinit var replaceCallbacks: () -> Unit
        compose.setContent {
            val callbackVersion = remember { mutableIntStateOf(0) }
            replaceCallbacks = { callbackVersion.intValue = 1 }
            EasyTripTheme {
                ItineraryItemRow(
                    item = itineraryItem("i1", "灵隐寺", "法云弄1号", "09:30", 120),
                    index = 1,
                    count = 3,
                    onPreview = if (callbackVersion.intValue == 0) {
                        { oldPreviews.add(it) }
                    } else {
                        { newPreviews.add(it) }
                    },
                    onCommit = if (callbackVersion.intValue == 0) {
                        { oldCommits.add(it) }
                    } else {
                        { newCommits.add(it) }
                    },
                    onMenuAction = {},
                )
            }
        }

        compose.onNodeWithTag("drag-handle-i1", useUnmergedTree = true).performTouchInput {
            val start = center
            down(start)
            advanceEventTime(700)
            compose.runOnIdle { replaceCallbacks() }
            moveTo(Offset(start.x, start.y - 500f), 500)
            up()
        }

        compose.runOnIdle {
            assertTrue(oldPreviews.isEmpty())
            assertTrue(oldCommits.isEmpty())
            assertEquals(0, newPreviews.last())
            assertEquals(listOf(0), newCommits)
        }
    }

    @Test
    fun cancelRestoresStartIndexWithoutCommit() {
        val previews = mutableListOf<Int>()
        val commits = mutableListOf<Int>()
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 1f)) {
                EasyTripTheme {
                    ItineraryItemRow(
                        item = itineraryItem("i1", "灵隐寺", "法云弄1号", "09:30", 120),
                        index = 1,
                        count = 3,
                        onPreview = previews::add,
                        onCommit = commits::add,
                        onMenuAction = {},
                    )
                }
            }
        }

        compose.onNodeWithTag("drag-handle-i1", useUnmergedTree = true).performTouchInput {
            val start = center
            down(start)
            advanceEventTime(700)
            moveTo(Offset(start.x, start.y - 130f), 500)
            cancel()
        }

        compose.runOnIdle {
            assertEquals(listOf(0, 1), previews)
            assertTrue(commits.isEmpty())
        }
    }

    @Test
    fun reorderStepUsesDpAtTwoXDensity() {
        val previews = mutableListOf<Int>()
        val commits = mutableListOf<Int>()
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(2f, 1f)) {
                EasyTripTheme {
                    ItineraryItemRow(
                        item = itineraryItem("i1", "灵隐寺", "法云弄1号", "09:30", 120),
                        index = 1,
                        count = 3,
                        onPreview = previews::add,
                        onCommit = commits::add,
                        onMenuAction = {},
                    )
                }
            }
        }

        compose.onNodeWithTag("drag-handle-i1", useUnmergedTree = true).performTouchInput { longPressDragBy(-130f) }
        compose.runOnIdle {
            assertEquals(1, previews.last())
            assertEquals(listOf(1), commits)
        }

        previews.clear()
        commits.clear()
        compose.onNodeWithTag("drag-handle-i1", useUnmergedTree = true).performTouchInput { longPressDragBy(-250f) }
        compose.runOnIdle {
            assertEquals(0, previews.last())
            assertEquals(listOf(0), commits)
        }
    }

    @Test
    fun middleItemKeepsMoveUpAndMoveDownActions() {
        val commits = mutableListOf<Int>()
        compose.setContent {
            EasyTripTheme {
                ItineraryItemRow(
                    item = itineraryItem("i1", "灵隐寺", "法云弄1号", "09:30", 120),
                    index = 1,
                    count = 3,
                    onPreview = {},
                    onCommit = commits::add,
                    onMenuAction = {},
                )
            }
        }

        val node = compose.onNodeWithTag("item-i1")
            .assert(SemanticsMatcher("has both reorder actions") { semanticsNode ->
                semanticsNode.config[SemanticsActions.CustomActions].map { it.label } == listOf("上移", "下移")
            })
            .fetchSemanticsNode()
        node.config[SemanticsActions.CustomActions].first { it.label == "上移" }.action()
        node.config[SemanticsActions.CustomActions].first { it.label == "下移" }.action()

        compose.runOnIdle { assertEquals(listOf(0, 2), commits) }
    }

    @Test
    fun routeLegShowsReadyCalculatingWaitingAndFailedText() {
        compose.setContent {
            EasyTripTheme {
                androidx.compose.foundation.layout.Column {
                    routeLeg("ready", RouteStatus.SUCCESS, distance = 1050, duration = 300).let {
                        RouteLegContent(it, Modifier.testTag("ready"))
                    }
                    routeLeg("calculating", RouteStatus.CALCULATING).let {
                        RouteLegContent(it, Modifier.testTag("calculating"))
                    }
                    routeLeg("waiting", RouteStatus.WAITING_NETWORK).let {
                        RouteLegContent(it, Modifier.testTag("waiting"))
                    }
                    routeLeg("failed", RouteStatus.FAILED, error = "路线暂时不可用").let {
                        RouteLegContent(it, Modifier.testTag("failed"))
                    }
                }
            }
        }

        compose.onNodeWithText("步行").assertIsDisplayed()
        compose.onNodeWithText("1.1 公里 · 5 分钟").assertIsDisplayed()
        compose.onNodeWithTag("calculating").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite),
        )
        compose.onNodeWithText("正在计算路线").assertIsDisplayed()
        compose.onNodeWithText("联网后计算路线").assertIsDisplayed()
        compose.onNodeWithText("路线暂时不可用").assertIsDisplayed()
        compose.onNodeWithContentDescription("路线计算中").assertIsDisplayed()
        compose.onNodeWithContentDescription("离线，联网后计算路线").assertIsDisplayed()
    }

    @Test
    fun routeLegKeepsPendingCalculatingAndOfflineStatesDistinctWithAccessibleIcons() {
        compose.setContent {
            EasyTripTheme {
                androidx.compose.foundation.layout.Column {
                    listOf(
                        routeLeg("pending", RouteStatus.PENDING),
                        routeLeg("calculating", RouteStatus.CALCULATING),
                        routeLeg("offline", RouteStatus.WAITING_NETWORK),
                    ).forEach { leg -> RouteLegContent(leg, Modifier.testTag("state-${leg.id}")) }
                }
            }
        }

        compose.onNodeWithText("等待计算路线").assertIsDisplayed()
        compose.onNodeWithText("正在计算路线").assertIsDisplayed()
        compose.onNodeWithText("联网后计算路线").assertIsDisplayed()
        compose.onNodeWithContentDescription("等待计算路线").assertIsDisplayed()
        compose.onNodeWithContentDescription("路线计算中").assertIsDisplayed()
        compose.onNodeWithContentDescription("离线，联网后计算路线").assertIsDisplayed()
        compose.onNodeWithTag("state-calculating").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite),
        )
    }

    @Test
    fun longPlaceNameAndAddressKeepReadableTypographyAtNarrowWidth() {
        val name = "一段很长很长很长很长很长很长的地点名称"
        val address = "一段很长很长很长很长很长很长很长很长的地点地址"
        compose.setContent {
            EasyTripTheme {
                ItineraryPlaceContent(
                    item = itineraryItem("narrow", name, address, "09:30", 120),
                    displayOrder = 1,
                    modifier = Modifier.width(220.dp),
                )
            }
        }

        val nameLayout = compose.onNodeWithText(name).fetchSemanticsNode().textLayout()
        val addressLayout = compose.onNodeWithText(address).fetchSemanticsNode().textLayout()
        assertTrue(nameLayout.lineCount >= 1)
        assertTrue(addressLayout.lineCount >= 1)
    }

    @Test
    fun onlyReadyLegOpensModeEditor() {
        val opened = mutableListOf<String>()
        compose.setContent {
            EasyTripTheme {
                androidx.compose.foundation.layout.Column {
                    listOf(
                        routeLeg("ready", RouteStatus.SUCCESS),
                        routeLeg("calculating", RouteStatus.CALCULATING),
                        routeLeg("waiting", RouteStatus.WAITING_NETWORK),
                        routeLeg("failed", RouteStatus.FAILED, error = "失败"),
                    ).forEach { leg ->
                        RouteLegContent(
                            leg = leg,
                            modifier = Modifier.testTag("leg-${leg.id}"),
                            onMode = { opened += leg.id },
                        )
                    }
                }
            }
        }

        compose.onNodeWithTag("mode-ready").assertHasClickAction().performClick()
        compose.onAllNodesWithTag("mode-calculating").assertCountEquals(0)
        compose.onAllNodesWithTag("mode-waiting").assertCountEquals(0)
        compose.onAllNodesWithTag("mode-failed").assertCountEquals(0)
        compose.runOnIdle { assertEquals(listOf("ready"), opened) }
    }

    @Test
    fun failedRetryDispatchesOnlyCurrentLeg() {
        val retried = mutableListOf<String>()
        compose.setContent {
            EasyTripTheme {
                androidx.compose.foundation.layout.Column {
                    listOf("first", "second").forEach { id ->
                        val leg = routeLeg(id, RouteStatus.FAILED, error = "失败")
                        RouteLegContent(leg, onRetry = { retried += id })
                    }
                }
            }
        }

        compose.onNodeWithTag("retry-second").performClick()
        compose.runOnIdle { assertEquals(listOf("second"), retried) }
    }

    @Test
    fun calculatingAndWaitingExposeNeitherModeNorRetry() {
        compose.setContent {
            EasyTripTheme {
                androidx.compose.foundation.layout.Column {
                    listOf(
                        routeLeg("calculating", RouteStatus.CALCULATING),
                        routeLeg("waiting", RouteStatus.WAITING_NETWORK),
                    ).forEach { leg ->
                        RouteLegContent(leg, onMode = {}, onRetry = {})
                    }
                }
            }
        }

        listOf("calculating", "waiting").forEach { id ->
            compose.onAllNodesWithTag("mode-$id").assertCountEquals(0)
            compose.onAllNodesWithTag("retry-$id").assertCountEquals(0)
        }
    }

    @Test
    fun twoXFontScaleKeepsFailureTextRetryAndConnectorReachable() {
        val error = "这是一段用于验证大字号下路线失败信息保持可读且操作仍可触达的两行错误文案"
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                EasyTripTheme {
                    RouteLegContent(
                        routeLeg("large-font", RouteStatus.FAILED, error = error),
                        Modifier.width(320.dp).testTag("large-font-leg"),
                        onRetry = {},
                    )
                }
            }
        }

        val leg = compose.onNodeWithTag("large-font-leg").getUnclippedBoundsInRoot()
        val text = compose.onNodeWithText(error).getUnclippedBoundsInRoot()
        val retry = compose.onNodeWithTag("retry-large-font").assertIsDisplayed().getUnclippedBoundsInRoot()
        val connector = compose.onNodeWithTag("route-connector-large-font", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        assertTrue("leg=$leg text=$text retry=$retry", text.right <= retry.left && retry.right <= leg.right)
        assertEquals(leg.bottom - leg.top, connector.bottom - connector.top)
    }

    @Test
    fun longFailureMessageExpandsConnectorToMatchItsActualRowHeight() {
        val message = "这是一段用于验证路线失败信息不会无限撑高连接段并且仍然完整参与自适应布局的很长错误文案"
        compose.setContent {
            EasyTripTheme {
                RouteLegContent(
                    routeLeg("connector", RouteStatus.FAILED, error = message),
                    Modifier.width(220.dp).testTag("connector-leg"),
                )
            }
        }

        val leg = compose.onNodeWithTag("connector-leg").getUnclippedBoundsInRoot()
        val connector = compose.onNodeWithTag("route-connector-connector", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        assertEquals(leg.bottom - leg.top, connector.bottom - connector.top)
        assertTrue("leg=$leg connector=$connector", connector.bottom > connector.top)
    }

    @Test
    fun longFailureMessageIsAtMostTwoLinesWithoutFixedHeight() {
        val message = "这是一段用于验证路线失败信息不会无限撑高连接段并且仍然完整参与自适应布局的很长错误文案"
        compose.setContent {
            EasyTripTheme {
                RouteLegContent(
                    routeLeg("long", RouteStatus.FAILED, error = message),
                    Modifier.width(220.dp).testTag("long-leg"),
                )
            }
        }

        val textNode = compose.onNodeWithText(message).assertIsDisplayed().fetchSemanticsNode()
        val results = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        checkNotNull(textNode.config[SemanticsActions.GetTextLayoutResult].action).invoke(results)
        assertTrue(results.single().lineCount <= 2)
        val bounds = compose.onNodeWithTag("long-leg").getUnclippedBoundsInRoot()
        assertTrue(bounds.bottom > bounds.top)
    }

    @Test
    fun normalPlaceRowHasZeroElevationAndNoPermanentDeleteAction() {
        compose.setContent {
            EasyTripTheme {
                ItineraryPlaceRow(
                    item = itineraryItem("plain", "灵隐寺", "法云弄1号", "09:30", 120),
                    displayOrder = 1,
                )
            }
        }

        compose.onNodeWithTag("item-plain").assertIsDisplayed()
        compose.onAllNodesWithTag("delete-plain").assertCountEquals(0)
        compose.onAllNodesWithText("删除").assertCountEquals(0)
    }

    private fun SemanticsNode.textLayout(): androidx.compose.ui.text.TextLayoutResult {
        val results = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        checkNotNull(config[SemanticsActions.GetTextLayoutResult].action).invoke(results)
        return results.single()
    }

    private fun SemanticsNode.timelineTags(): List<String> =
        listOfNotNull(
            if (config.contains(SemanticsProperties.TestTag)) {
                config[SemanticsProperties.TestTag].takeIf { it.startsWith("item-") || it.startsWith("leg-") }
            } else {
                null
            },
        ) + children.flatMap { it.timelineTags() }

    private fun routeLeg(
        id: String,
        status: RouteStatus,
        distance: Int? = null,
        duration: Int? = null,
        error: String? = null,
        fromItemId: String = "from-$id",
        toItemId: String = "to-$id",
    ) = RouteLegUi(
        id = id,
        fromItemId = fromItemId,
        toItemId = toItemId,
        mode = TransportMode.WALK,
        status = status,
        distanceMeters = distance,
        durationSeconds = duration,
        error = error,
    )

    private fun androidx.compose.ui.test.TouchInjectionScope.longPressDragBy(deltaY: Float) {
        val start = center
        down(start)
        advanceEventTime(700)
        moveTo(Offset(start.x, start.y + deltaY), 500)
        up()
    }

    private fun itineraryItem(
        id: String,
        name: String,
        address: String,
        arrivalTime: String,
        stayMinutes: Int,
    ) = ItineraryItemUi(id, name, address, LocalTime.parse(arrivalTime), stayMinutes)
}
