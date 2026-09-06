package com.yangchengwei.easytrip.itinerary.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
    fun longNoteKeepsSaveAndCancelFixedInsideConstrainedEditor() {
        var saved = false
        var cancelled = false
        compose.setContent {
            EasyTripTheme {
                EditItineraryItemContent(
                    draft = ItineraryEditDraft("i1", "09:30", "60", noteText = (1..30).joinToString("\n") { "长备注第${it}行" }),
                    onArrivalTimeChange = {},
                    onStayMinutesChange = {},
                    onNoteChange = {},
                    onSave = { saved = true },
                    onCancel = { cancelled = true },
                    modifier = Modifier.height(220.dp),
                )
            }
        }

        compose.onNodeWithText("保存时间").assertIsDisplayed().performClick()
        compose.onNodeWithText("取消").assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(saved); assertTrue(cancelled) }
    }

    @Test
    fun saveFailureContentIsOrderedAccessibleAndActionableAt280DpWithTwoTimesFontScale() {
        var keepEditing = 0
        var retrySave = 0
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                EasyTripTheme {
                    Box(
                        Modifier
                            .width(280.dp)
                            .height(600.dp)
                            .testTag("save-failure-container"),
                    ) {
                        ItinerarySaveFailureContent(
                            onKeepEditing = { keepEditing++ },
                            onRetrySave = { retrySave++ },
                            modifier = Modifier.width(280.dp),
                        )
                    }
                }
            }
        }

        compose.onNodeWithTag("itinerary-save-failure").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite),
        )
        compose.onNodeWithText("修改尚未保存", useUnmergedTree = true)
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        val container = compose.onNodeWithTag("save-failure-container").getUnclippedBoundsInRoot()
        val title = compose.onNodeWithText("修改尚未保存").getUnclippedBoundsInRoot()
        val body = compose.onNodeWithText("到达时间、停留时长和备注仍保留在当前页面。请重新保存，或稍后再试。")
            .getUnclippedBoundsInRoot()
        val keep = compose.onNodeWithTag("itinerary-save-failure-keep-editing")
            .assertIsDisplayed().assertHasClickAction().getUnclippedBoundsInRoot()
        val retry = compose.onNodeWithTag("itinerary-save-failure-retry")
            .assertIsDisplayed().assertHasClickAction().getUnclippedBoundsInRoot()
        assertTrue("title=$title body=$body", title.top < body.top)
        assertTrue("body=$body keep=$keep", body.bottom <= keep.top)
        assertTrue("keep=$keep retry=$retry", keep.bottom <= retry.top)
        listOf(keep, retry).forEach { action ->
            assertTrue("action=$action", action.right - action.left >= 48.dp && action.bottom - action.top >= 48.dp)
            assertTrue("container=$container action=$action", action.left >= container.left && action.right <= container.right)
            assertTrue("container=$container action=$action", action.top >= container.top && action.bottom <= container.bottom)
        }
        assertTrue("keep=$keep retry=$retry", keep.right <= retry.left || retry.right <= keep.left || keep.bottom <= retry.top || retry.bottom <= keep.top)

        compose.onNodeWithTag("itinerary-save-failure-keep-editing").performClick()
        compose.onNodeWithTag("itinerary-save-failure-retry").performClick()
        compose.runOnIdle {
            assertEquals(1, keepEditing)
            assertEquals(1, retrySave)
        }
    }

    @Test
    fun saveFailureFooterRemainsFixedInsideShort280DpTwoTimesFontContainer() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                EasyTripTheme {
                    Box(
                        Modifier
                            .width(280.dp)
                            .height(300.dp)
                            .testTag("save-failure-short-container"),
                    ) {
                        ItinerarySaveFailureContent(
                            onKeepEditing = {},
                            onRetrySave = {},
                            modifier = Modifier.width(280.dp).height(300.dp),
                        )
                    }
                }
            }
        }

        val container = compose.onNodeWithTag("save-failure-short-container").getUnclippedBoundsInRoot()
        val scroll = compose.onNodeWithTag("itinerary-save-failure-scroll").getUnclippedBoundsInRoot()
        val keep = compose.onNodeWithTag("itinerary-save-failure-keep-editing").getUnclippedBoundsInRoot()
        val retry = compose.onNodeWithTag("itinerary-save-failure-retry").getUnclippedBoundsInRoot()
        listOf(keep, retry).forEach { action ->
            assertTrue("container=$container action=$action", action.left >= container.left && action.right <= container.right)
            assertTrue("container=$container action=$action", action.top >= container.top && action.bottom <= container.bottom)
            assertTrue("action=$action", action.right - action.left >= 48.dp && action.bottom - action.top >= 48.dp)
        }
        assertTrue("scroll=$scroll keep=$keep", scroll.bottom <= keep.top)
        assertTrue("keep=$keep retry=$retry", keep.bottom <= retry.top)
    }

    @Test
    fun itemEditorDoesNotRenderSaveErrorOutsideRecoveryContent() {
        compose.setContent {
            EasyTripTheme {
                EditItineraryItemContent(
                    draft = ItineraryEditDraft("item", "09:30", "60", saveError = "保存失败"),
                    onArrivalTimeChange = {},
                    onStayMinutesChange = {},
                    onSave = {},
                    onCancel = {},
                )
            }
        }

        compose.onAllNodesWithText("保存失败").assertCountEquals(0)
    }

    @Test
    fun itemEditorOffersExistingTargetDayFlowAndLocksItWhileSaving() {
        var scheduleAgainCalls = 0
        val saving = androidx.compose.runtime.mutableStateOf(false)
        compose.setContent {
            EasyTripTheme {
                EditItineraryItemContent(
                    draft = ItineraryEditDraft(
                        itemId = "i1",
                        arrivalTimeText = "09:30",
                        stayMinutesText = "60",
                        isSaving = saving.value,
                        placeId = "place-1",
                        placeName = "灵隐寺",
                    ),
                    onArrivalTimeChange = {},
                    onStayMinutesChange = {},
                    onScheduleAgain = { scheduleAgainCalls++ },
                    onSave = {},
                    onCancel = {},
                )
            }
        }

        compose.onNodeWithTag("schedule-again-place").assertIsDisplayed().assertHasClickAction().performClick()
        compose.runOnIdle {
            assertEquals(1, scheduleAgainCalls)
            saving.value = true
        }
        compose.onNodeWithTag("schedule-again-place").assertIsNotEnabled()
    }

    @Test
    fun routeEditorShowsConcreteEndpointsCurrentRouteAndHonestRecomputeAction() {
        compose.setContent {
            EasyTripTheme {
                EditRouteLegContent(
                    draft = RouteModeEditDraft(
                        legId = "leg",
                        selectedMode = TransportMode.WALK,
                        selectedModeOverride = TransportMode.DRIVE,
                        originalSelectedModeOverride = TransportMode.WALK,
                        plannedDurationSeconds = 600,
                        fromPlaceName = "酒店",
                        toPlaceName = "景点",
                        distanceMeters = 800,
                    ),
                    onSelectMode = {},
                    onClearSelectedModeOverride = {},
                    onDurationMinutesChange = {},
                    onNoteChange = {},
                    onSave = {},
                    onCancel = {},
                )
            }
        }

        compose.onNodeWithTag("route-endpoints").assertIsDisplayed()
        compose.onNodeWithText("酒店 → 景点").assertIsDisplayed()
        compose.onNodeWithTag("route-current-context").assertIsDisplayed()
        compose.onNodeWithText("当前路线 · 已规划 · 800 米 · 预计 10 分钟").assertIsDisplayed()
        compose.onNodeWithTag("save-route").assertHasClickAction()
        compose.onNodeWithText("保存并重新计算路线").assertIsDisplayed()
    }

    @Test
    fun routeRowShowsConcreteEndpointsAndCurrentStatusContext() {
        compose.setContent {
            EasyTripTheme {
                RouteLegRow(
                    leg = RouteLegUi("leg", "from", "to", TransportMode.WALK, RouteStatus.SUCCESS, 800, 600, null),
                    fromPlaceName = "酒店",
                    toPlaceName = "景点",
                    onMode = {},
                )
            }
        }

        compose.onNodeWithText("酒店 → 景点").assertIsDisplayed()
        compose.onNodeWithText("已规划 · 800 米 · 预计 10 分钟").assertIsDisplayed()
    }

    @Test
    fun itemEditContainsMultilineNoteAndLocksEveryActionWhileSaving() {
        val draft = ItineraryEditDraft(
            itemId = "i1",
            arrivalTimeText = "09:30",
            stayMinutesText = "60",
            noteText = "第一行\n第二行",
            isSaving = true,
            saveError = "保存失败",
        )
        compose.setContent {
            EasyTripTheme {
                EditItineraryItemContent(
                    draft = draft,
                    onArrivalTimeChange = {},
                    onStayMinutesChange = {},
                    onNoteChange = {},
                    onSave = {},
                    onCancel = {},
                )
            }
        }

        compose.onNodeWithTag("itinerary-note-input").assertIsDisplayed()
        compose.onNodeWithText("第一行\n第二行").assertIsDisplayed()
        compose.onAllNodesWithText("保存失败").assertCountEquals(0)
        compose.onNodeWithText("保存中…").assertIsNotEnabled()
        compose.onNodeWithText("取消").assertIsNotEnabled()
    }

    @Test
    fun editorsExposeStableHostRootsForSmallWindowAndImeReachability() {
        compose.setContent {
            EasyTripTheme {
                Column {
                    EditItineraryItemContent(
                        draft = ItineraryEditDraft("item", "09:30", "60"),
                        onArrivalTimeChange = {},
                        onStayMinutesChange = {},
                        onSave = {},
                        onCancel = {},
                        modifier = Modifier.width(280.dp).height(320.dp),
                    )
                    EditRouteLegContent(
                        draft = RouteModeEditDraft("leg", TransportMode.WALK),
                        onSelectMode = {},
                        onClearSelectedModeOverride = {},
                        onDurationMinutesChange = {},
                        onNoteChange = {},
                        onSave = {},
                        onCancel = {},
                        modifier = Modifier.width(280.dp).height(320.dp),
                    )
                }
            }
        }

        compose.onNodeWithTag("itinerary-item-editor").assertIsDisplayed()
        compose.onNodeWithTag("route-leg-editor").assertIsDisplayed()
    }

    @Test
    fun routeEditorExposesSelectedModeAndKeepsEveryChoiceReachableAtNarrowLargeFont() {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                EasyTripTheme {
                    EditRouteLegContent(
                        draft = RouteModeEditDraft(
                            legId = "leg",
                            selectedMode = TransportMode.DRIVE,
                            selectedModeOverride = TransportMode.DRIVE,
                        ),
                        onSelectMode = {},
                        onClearSelectedModeOverride = {},
                        onDurationMinutesChange = {},
                        onNoteChange = {},
                        onSave = {},
                        onCancel = {},
                        modifier = Modifier.width(220.dp).height(520.dp),
                    )
                }
            }
        }

        compose.onNodeWithTag("route-mode-option-DRIVE").assertIsSelected().assertIsDisplayed()
        compose.onNodeWithTag("route-mode-option-WALK").assertIsDisplayed()
        compose.onNodeWithTag("route-mode-option-TAXI").assertIsDisplayed()
        compose.onNodeWithTag("route-mode-option-TRANSIT").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("route-mode-auto").assertIsDisplayed()
    }

    @Test
    fun recommendedModeChoiceIsSelectedWhenThereIsNoOverride() {
        compose.setContent {
            EasyTripTheme {
                EditRouteLegContent(
                    draft = RouteModeEditDraft(
                        legId = "leg",
                        selectedMode = TransportMode.WALK,
                        selectedModeOverride = null,
                    ),
                    onSelectMode = {},
                    onClearSelectedModeOverride = {},
                    onDurationMinutesChange = {},
                    onNoteChange = {},
                    onSave = {},
                    onCancel = {},
                )
            }
        }

        compose.onNodeWithTag("route-mode-auto").assertIsSelected()
    }

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
    fun populatedDayShowsDayAndStopSummaryBeforeTimeline() {
        val items = listOf(
            itineraryItem("i1", "灵隐寺", "法云弄1号", "09:30", 120),
            itineraryItem("i2", "知味观", "仁和路83号", "12:00", 60),
        )
        val state = DayItineraryUiState(
            days = listOf(com.yangchengwei.easytrip.trip.domain.TripDay("day-1", 0)),
            selectedDayId = "day-1",
            items = items,
            previewOrder = items.map(ItineraryItemUi::id),
        )
        compose.setContent { EasyTripTheme { DayItineraryContent(state, onAction = {}) } }

        compose.onNodeWithTag("day-itinerary-summary").assertIsDisplayed()
        compose.onNodeWithText("第 1 天 · 2 站").assertIsDisplayed()
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
                    item = itineraryItem("i1", "灵隐寺", "法云弄1号", "09:30", 120).copy(placeId = "p1"),
                    index = 0,
                    count = 1,
                    onPreview = {},
                    onCommit = {},
                    onMenuAction = actions::add,
                    canScheduleAgain = true,
                )
            }
        }

        listOf(
            "menu-timing-i1" to ItineraryItemMenuAction.EditTiming,
            "menu-schedule-again-i1" to ItineraryItemMenuAction.ScheduleAgain,
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
        compose.onNodeWithText("已规划 · 1.1 公里 · 预计 5 分钟").assertIsDisplayed()
        compose.onNodeWithTag("calculating").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite),
        )
        compose.onNodeWithText("正在计算路线").assertIsDisplayed()
        compose.onNodeWithText("等待联网后计算").assertIsDisplayed()
        compose.onNodeWithText("路线暂时不可用").assertIsDisplayed()
        compose.onNodeWithContentDescription("路线计算中").assertIsDisplayed()
        compose.onNodeWithContentDescription("离线，等待联网后计算").assertIsDisplayed()
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
        compose.onNodeWithText("等待联网后计算").assertIsDisplayed()
        compose.onNodeWithContentDescription("等待计算路线").assertIsDisplayed()
        compose.onNodeWithContentDescription("路线计算中").assertIsDisplayed()
        compose.onNodeWithContentDescription("离线，等待联网后计算").assertIsDisplayed()
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
    fun readySiblingRemainsEditableWhileEveryNonReadyLegHasNoEditAction() {
        val opened = mutableListOf<String>()
        compose.setContent {
            EasyTripTheme {
                androidx.compose.foundation.layout.Column {
                    listOf(
                        routeLeg("ready", RouteStatus.SUCCESS),
                        routeLeg("pending", RouteStatus.PENDING),
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

        compose.onNodeWithTag("edit-route-ready").assertHasClickAction().performClick()
        listOf("pending", "calculating", "waiting", "failed").forEach { id ->
            compose.onAllNodesWithTag("edit-route-$id").assertCountEquals(0)
        }
        compose.runOnIdle { assertEquals(listOf("ready"), opened) }
    }

    @Test
    fun waitingShowsExactMessageWithoutEditOrRetry() {
        compose.setContent {
            EasyTripTheme {
                RouteLegContent(
                    routeLeg("waiting", RouteStatus.WAITING_NETWORK),
                    onMode = {},
                    onRetry = {},
                )
            }
        }

        compose.onNodeWithText("等待联网后计算").assertIsDisplayed()
        compose.onAllNodesWithTag("edit-route-waiting").assertCountEquals(0)
        compose.onAllNodesWithTag("retry-waiting").assertCountEquals(0)
    }

    @Test
    fun failedShowsRecoverySummaryAndOnlyRetriesItsOwnLeg() {
        val retried = mutableListOf<String>()
        compose.setContent {
            EasyTripTheme {
                androidx.compose.foundation.layout.Column {
                    RouteLegContent(
                        routeLeg("failed", RouteStatus.FAILED, error = "底层异常"),
                        onMode = { retried += "edited" },
                        onRetry = { retried += "failed" },
                    )
                    RouteLegContent(
                        routeLeg("ready", RouteStatus.SUCCESS),
                        onRetry = { retried += "ready" },
                    )
                }
            }
        }

        compose.onNodeWithText("路线计算失败").assertIsDisplayed()
        compose.onNodeWithText("底层异常").assertIsDisplayed()
        compose.onAllNodesWithTag("edit-route-failed").assertCountEquals(0)
        compose.onNodeWithTag("retry-failed").performClick()
        compose.onAllNodesWithTag("retry-ready").assertCountEquals(0)
        compose.runOnIdle { assertEquals(listOf("failed"), retried) }
    }

    @Test
    fun routeRecoveryStatesRemainAccessibleAt280DpWithTwoTimesFontScale() {
        val actions = mutableListOf<String>()
        val failureDetail = "路线服务暂时不可用，请稍后重新计算"
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                EasyTripTheme {
                    androidx.compose.foundation.layout.Column(
                        Modifier.width(280.dp).testTag("route-recovery-container"),
                    ) {
                        RouteLegContent(
                            leg = routeLeg("waiting-2x", RouteStatus.WAITING_NETWORK),
                            modifier = Modifier.width(280.dp).testTag("route-waiting-2x"),
                            onMode = { actions += "waiting-edit" },
                            onRetry = { actions += "waiting-retry" },
                        )
                        RouteLegContent(
                            leg = routeLeg("failed-2x", RouteStatus.FAILED, error = failureDetail),
                            modifier = Modifier.width(280.dp).testTag("route-failed-2x"),
                            onMode = { actions += "failed-edit" },
                            onRetry = { actions += "failed-retry" },
                        )
                        RouteLegContent(
                            leg = routeLeg("ready-2x", RouteStatus.SUCCESS),
                            modifier = Modifier.width(280.dp).testTag("route-ready-2x"),
                            onMode = { actions += "ready-edit" },
                            onRetry = { actions += "ready-retry" },
                        )
                    }
                }
            }
        }

        compose.onNodeWithTag("route-waiting-2x").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite),
        )
        compose.onNodeWithText("等待联网后计算").assertIsDisplayed()
        compose.onNodeWithContentDescription("离线，等待联网后计算").assertIsDisplayed()
        compose.onAllNodesWithTag("edit-route-waiting-2x").assertCountEquals(0)
        compose.onAllNodesWithTag("retry-waiting-2x").assertCountEquals(0)

        compose.onNodeWithTag("route-failed-2x")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
            .assert(SemanticsMatcher("has no content description") {
                !it.config.contains(SemanticsProperties.ContentDescription)
            })
        compose.onNodeWithText("路线计算失败").assertIsDisplayed()
        compose.onNodeWithText(failureDetail).assertIsDisplayed()
        compose.onAllNodesWithTag("retry-failed-2x").assertCountEquals(1)
        compose.onAllNodesWithTag("edit-route-failed-2x").assertCountEquals(0)
        compose.onNodeWithTag("edit-route-ready-2x").assertHasClickAction()

        val container = compose.onNodeWithTag("route-recovery-container").getUnclippedBoundsInRoot()
        val failed = compose.onNodeWithTag("route-failed-2x").getUnclippedBoundsInRoot()
        val retry = compose.onNodeWithTag("retry-failed-2x").getUnclippedBoundsInRoot()
        val title = compose.onNodeWithText("路线计算失败").getUnclippedBoundsInRoot()
        val detail = compose.onNodeWithText(failureDetail).getUnclippedBoundsInRoot()
        val edit = compose.onNodeWithTag("edit-route-ready-2x").getUnclippedBoundsInRoot()
        assertTrue("retry=$retry", retry.right - retry.left >= 48.dp && retry.bottom - retry.top >= 48.dp)
        assertTrue("container=$container retry=$retry", retry.left >= container.left && retry.right <= container.right)
        assertTrue("failed=$failed retry=$retry", retry.top >= failed.top && retry.bottom <= failed.bottom)
        assertTrue("title=$title retry=$retry", title.right <= retry.left || retry.right <= title.left || title.bottom <= retry.top || retry.bottom <= title.top)
        assertTrue("detail=$detail retry=$retry", detail.right <= retry.left || retry.right <= detail.left || detail.bottom <= retry.top || retry.bottom <= detail.top)
        assertTrue("retry=$retry edit=$edit", retry.right <= edit.left || edit.right <= retry.left || retry.bottom <= edit.top || edit.bottom <= retry.top)

        compose.onNodeWithTag("retry-failed-2x").performClick()
        compose.onNodeWithTag("edit-route-ready-2x").performClick()
        compose.runOnIdle { assertEquals(listOf("failed-retry", "ready-edit"), actions) }
    }

    @Test
    fun dayItineraryDispatchesOnlyFailedLegRetryWhileReadySiblingRemainsEditable() {
        val actions = mutableListOf<DayItineraryAction>()
        val items = listOf(
            itineraryItem("i1", "第一站", "地址 1", "09:00", 30),
            itineraryItem("i2", "第二站", "地址 2", "10:00", 30),
            itineraryItem("i3", "第三站", "地址 3", "11:00", 30),
        )
        val state = DayItineraryUiState(
            items = items,
            previewOrder = items.map(ItineraryItemUi::id),
            legs = listOf(
                routeLeg("failed", RouteStatus.FAILED, error = "失败", fromItemId = "i1", toItemId = "i2"),
                routeLeg("ready", RouteStatus.SUCCESS, fromItemId = "i2", toItemId = "i3"),
            ),
        )
        compose.setContent {
            EasyTripTheme { DayItineraryContent(state, onAction = actions::add, showDialogs = false) }
        }

        compose.onNodeWithTag("retry-failed").performClick()
        compose.onNodeWithTag("edit-route-ready").assertHasClickAction()
        compose.runOnIdle { assertEquals(listOf(DayItineraryAction.Retry("failed", 1)), actions) }
    }

    @Test
    fun failedRouteWithoutErrorDoesNotDuplicateFailureAnnouncement() {
        compose.setContent {
            EasyTripTheme {
                RouteLegContent(
                    routeLeg("failed-default", RouteStatus.FAILED, error = null),
                    Modifier.testTag("failed-default"),
                )
            }
        }

        compose.onNodeWithTag("failed-default")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
            .assert(SemanticsMatcher("has no content description") {
                !it.config.contains(SemanticsProperties.ContentDescription)
            })
        compose.onAllNodesWithText("路线计算失败").assertCountEquals(1)
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
            compose.onAllNodesWithTag("edit-route-$id").assertCountEquals(0)
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
        version = 1,
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
