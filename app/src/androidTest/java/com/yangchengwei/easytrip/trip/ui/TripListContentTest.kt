package com.yangchengwei.easytrip.trip.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.ui.theme.EasyTripAccent
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TripListContentTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun profileAvatarIsDecorativeAndHasNoClickOrButtonSemantics() {
        setContent(TripListPageState.Empty)

        compose.onNodeWithTag("profile-avatar", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("profile-avatar", useUnmergedTree = true)
            .assert(SemanticsProperties.Role.keyNotDefined())
            .assert(SemanticsActions.OnClick.keyNotDefined())
        compose.onAllNodesWithContentDescription("个人中心").assertCountEquals(0)
        compose.onAllNodes(hasClickAction() and androidx.compose.ui.test.hasTestTag("profile-avatar")).assertCountEquals(0)
    }

    @Test fun emptyTripsUsesSharedIllustratedState() {
        setContent(TripListPageState.Empty)

        compose.onNodeWithTag("empty-illustration-trips").assertIsDisplayed()
        compose.onNodeWithText("开始规划一次旅行").assertIsDisplayed()
    }

    @Test fun primaryTripHasOneCardEntryAndIndependentCompactMenu() {
        val actions = mutableListOf<TripListAction>()
        setContent(content(), actions::add)

        compose.onNodeWithTag("primary-trip-trip-1").assert(hasClickAction()).performClick()
        assertEquals(listOf(TripListAction.OpenTrip("trip-1")), actions)
        compose.onAllNodesWithTag("continue-trip-trip-1").assertCountEquals(0)
        compose.onAllNodesWithText("继续规划", useUnmergedTree = true).assertCountEquals(0)
        compose.onNodeWithTag("trip-menu-trip-1").assertWidthIsEqualTo(28.dp).assertHeightIsEqualTo(28.dp)
        compose.onAllNodesWithTag("trip-trip-1").assertCountEquals(0)
        compose.onAllNodesWithTag("trip-settings-trip-1").assertCountEquals(0)
        compose.onAllNodesWithTag("trip-delete-trip-1").assertCountEquals(0)
    }

    @Test fun primaryTripMenuActionDoesNotOpenTheCard() {
        val actions = mutableListOf<TripListAction>()
        setContent(content(), actions::add)

        compose.onNodeWithTag("trip-menu-trip-1").performClick()
        compose.onNodeWithTag("trip-menu-settings-trip-1").performClick()

        assertEquals(listOf(TripListAction.OpenSettings("trip-1")), actions)
    }

    @Test fun primaryTripShowsCountdownPillAndAccessibleRealReadinessProgress() {
        setContent(TripListPageState.Content(listOf(nonZeroTrip("trip-primary", "杭州"))))

        compose.onNodeWithTag("primary-trip-trip-primary").assertIsDisplayed()
        compose.onNodeWithText("下一站 · 杭州").assertIsDisplayed()
        compose.onNodeWithTag("trip-countdown-trip-primary", useUnmergedTree = true).assertIsDisplayed()
        compose.onAllNodesWithText("行程准备度", useUnmergedTree = true).assertCountEquals(1)
        compose.onNodeWithText("67%", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("trip-readiness-trip-primary")
            .assert(SemanticsProperties.ProgressBarRangeInfo.keyIs(androidx.compose.ui.semantics.ProgressBarRangeInfo(0.67f, 0f..1f)))
            .assert(SemanticsProperties.ContentDescription.keyIs(listOf("已有行程内容 2/3 个旅行日")))
    }

    @Test fun quarterReadinessUsesOneContinuousTrackWithoutTrailingStopIndicator() {
        setContent(TripListPageState.Content(listOf(nonZeroTrip("trip-primary", "杭州").copy(
            scheduledDayCount = 1,
            readinessPercent = 25,
            readinessLabel = "25%",
        ))))

        val pixels = compose.onNodeWithTag("trip-readiness-trip-primary").captureToImage().toPixelMap()
        val midY = pixels.height / 2
        val orangeRuns = horizontalColorRuns(pixels, midY, EasyTripAccent)
        val splitX = orangeRuns.single().last + 1
        val cardBackground = pixels[0, 0]

        assertEquals("25% 准备度只能有一段连续橙色进度", 1, orangeRuns.size)
        assertTrue(
            "进度与剩余轨道的内部交界必须为直边并逐行贴合",
            (0 until pixels.height).all { y -> pixels[splitX, y] != cardBackground },
        )
    }

    @Test fun otherTripRowKeepsSingleEntrySemanticsWithSeparateMenuAndDecorativeArrow() {
        val actions = mutableListOf<TripListAction>()
        setContent(content(), actions::add)

        compose.onNodeWithTag("other-trip-trip-2")
            .assertHeightIsAtLeast(72.dp)
            .assert(hasClickAction())
            .performClick()
        assertEquals(listOf(TripListAction.OpenTrip("trip-2")), actions)
        compose.onNodeWithTag("other-trip-arrow-trip-2", useUnmergedTree = true)
            .assertWidthIsEqualTo(36.dp)
            .assertHeightIsEqualTo(36.dp)
            .assert(SemanticsActions.OnClick.keyNotDefined())
        compose.onNodeWithTag("trip-menu-trip-2").assertWidthIsEqualTo(28.dp).assertHeightIsEqualTo(28.dp)
        actions.clear()
        compose.onNodeWithTag("trip-menu-trip-2").performClick()
        compose.onNodeWithTag("trip-menu-settings-trip-2").performClick()
        assertEquals(listOf(TripListAction.OpenSettings("trip-2")), actions)
    }

    @Test fun contentCreateActionIsA132By48RightAlignedFixedButton() {
        compose.setContent {
            EasyTripTheme {
                Box(Modifier.requiredWidth(320.dp).height(700.dp).testTag("trip-list-container").consumeWindowInsets(androidx.compose.foundation.layout.WindowInsets.safeDrawing)) {
                    TripListContent(
                        state = TripListUiState(
                            page = TripListPageState.Content(
                                primaryTrip = trip("trip-primary", "京都"),
                                otherTrips = (1..12).map { index -> trip("trip-$index", "旅行 $index") },
                            ),
                        ),
                        onAction = {},
                    )
                }
            }
        }

        val containerBounds = compose.onNodeWithTag("trip-list-container").getUnclippedBoundsInRoot()
        val headerBounds = compose.onNodeWithTag("trip-list-title").assertIsDisplayed().getUnclippedBoundsInRoot()
        val primaryBounds = compose.onNodeWithTag("primary-trip-trip-primary").assertIsDisplayed().getUnclippedBoundsInRoot()
        check(headerBounds.bottom <= primaryBounds.top)
        val createBounds = compose.onNodeWithTag("create-trip")
            .assertWidthIsEqualTo(132.dp)
            .assertHeightIsEqualTo(48.dp)
            .assertIsDisplayed()
            .getUnclippedBoundsInRoot()
        assertEquals(containerBounds.right - 16.dp, createBounds.right)
        assertEquals(containerBounds.bottom - 20.dp, createBounds.bottom)

        compose.onNodeWithTag("other-trips-list").performScrollToNode(hasTestTag("other-trip-trip-12"))
        compose.onNodeWithTag("other-trip-trip-12").assertIsDisplayed()
        assertEquals(headerBounds, compose.onNodeWithTag("trip-list-title").getUnclippedBoundsInRoot())
        assertEquals(primaryBounds, compose.onNodeWithTag("primary-trip-trip-primary").getUnclippedBoundsInRoot())
        assertEquals(createBounds, compose.onNodeWithTag("create-trip").getUnclippedBoundsInRoot())
    }

    @Test fun singleTripKeepsPrimaryAndCreateReachableWithoutOtherTrips() {
        setContent(
            TripListPageState.Content(
                primaryTrip = trip("trip-1", "京都"),
                otherTrips = emptyList(),
            ),
        )

        compose.onNodeWithTag("primary-trip-trip-1").assertIsDisplayed()
        compose.onNodeWithTag("create-trip").assertWidthIsEqualTo(132.dp).assertHeightIsEqualTo(48.dp).assertIsDisplayed()
        compose.onAllNodesWithText("其他旅行").assertCountEquals(0)
    }

    @Test fun tallSingleTripAtLargeFontScrollsWithoutOverlappingTheFixedCreateAction() {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = LocalDensity.current.density, fontScale = 2f)) {
                EasyTripTheme {
                    Box(Modifier.requiredWidth(320.dp).height(480.dp).testTag("single-trip-container").consumeWindowInsets(androidx.compose.foundation.layout.WindowInsets.safeDrawing)) {
                        TripListContent(
                            state = TripListUiState(
                                page = TripListPageState.Content(
                                    primaryTrip = trip("trip-primary", "一段特别特别长而且需要完整换行展示的旅行名称"),
                                    otherTrips = emptyList(),
                                ),
                            ),
                            onAction = {},
                        )
                    }
                }
            }
        }

        val container = compose.onNodeWithTag("single-trip-container").getUnclippedBoundsInRoot()
        val create = compose.onNodeWithTag("create-trip").assertIsDisplayed().getUnclippedBoundsInRoot()
        val list = compose.onNodeWithTag("other-trips-list").assertIsDisplayed().getUnclippedBoundsInRoot()
        assertEquals(container.right - 16.dp, create.right)
        assertEquals(container.bottom - 20.dp, create.bottom)
        check(list.bottom > list.top)
        check(list.bottom <= create.top)
        compose.onNodeWithTag("other-trips-list").performScrollToNode(hasTestTag("primary-trip-trip-primary"))
        compose.onNodeWithTag("primary-trip-trip-primary").assertIsDisplayed()
        compose.onNodeWithTag("create-trip").assertIsDisplayed()
    }

    @Test fun narrowLargeFontContentUsesCompactScrollBeforePrimaryOverlapsOtherTrips() {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = LocalDensity.current.density, fontScale = 2f)) {
                EasyTripTheme {
                    Box(Modifier.requiredWidth(320.dp).height(700.dp).testTag("narrow-large-font-container").consumeWindowInsets(androidx.compose.foundation.layout.WindowInsets.safeDrawing)) {
                        TripListContent(
                            state = TripListUiState(
                                page = TripListPageState.Content(
                                    primaryTrip = trip("trip-primary", "一段特别特别长而且需要完整换行展示的旅行名称"),
                                    otherTrips = (1..12).map { index ->
                                        trip("trip-$index", "另一段同样很长而且需要换行的旅行名称 $index")
                                    },
                                ),
                            ),
                            onAction = {},
                        )
                    }
                }
            }
        }

        val container = compose.onNodeWithTag("narrow-large-font-container").getUnclippedBoundsInRoot()
        val create = compose.onNodeWithTag("create-trip").assertIsDisplayed().getUnclippedBoundsInRoot()
        val list = compose.onNodeWithTag("other-trips-list").assertIsDisplayed().getUnclippedBoundsInRoot()
        assertEquals(container.right - 16.dp, create.right)
        assertEquals(container.bottom - 20.dp, create.bottom)
        check(list.bottom > list.top)
        check(list.bottom <= create.top)
        compose.onNodeWithTag("other-trips-list").performScrollToNode(hasTestTag("other-trip-trip-12"))
        compose.onNodeWithTag("other-trip-trip-12").assertIsDisplayed()
        compose.onNodeWithTag("other-trips-list").performScrollToNode(hasTestTag("primary-trip-trip-primary"))
        compose.onNodeWithTag("primary-trip-trip-primary").assertIsDisplayed()
    }

    @Test fun scrollingOtherTripsKeepsHeaderPrimaryAndCreateBoundsFixed() {
        compose.setContent {
            EasyTripTheme {
                Box(Modifier.requiredWidth(320.dp).height(700.dp)) {
                    TripListContent(
                        state = TripListUiState(
                            page = TripListPageState.Content(
                                primaryTrip = trip("trip-primary", "京都"),
                                otherTrips = (1..12).map { index -> trip("trip-$index", "旅行 $index") },
                            ),
                        ),
                        onAction = {},
                    )
                }
            }
        }

        val headerBounds = compose.onNodeWithTag("trip-list-title").assertIsDisplayed().getUnclippedBoundsInRoot()
        val primaryBounds = compose.onNodeWithTag("primary-trip-trip-primary").assertIsDisplayed().getUnclippedBoundsInRoot()
        check(headerBounds.bottom <= primaryBounds.top)
        val createBounds = compose.onNodeWithTag("create-trip")
            .assertWidthIsEqualTo(132.dp)
            .assertHeightIsEqualTo(48.dp)
            .assertIsDisplayed()
            .getUnclippedBoundsInRoot()

        compose.onNodeWithTag("other-trips-list").performScrollToNode(hasTestTag("other-trip-trip-12"))
        compose.onNodeWithTag("other-trip-trip-12").assertIsDisplayed()

        assertEquals(headerBounds, compose.onNodeWithTag("trip-list-title").getUnclippedBoundsInRoot())
        assertEquals(primaryBounds, compose.onNodeWithTag("primary-trip-trip-primary").getUnclippedBoundsInRoot())
        assertEquals(createBounds, compose.onNodeWithTag("create-trip").getUnclippedBoundsInRoot())
    }

    @Test fun splitScreenLargeFontKeepsCreateAndAllTripsReachable() {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = LocalDensity.current.density, fontScale = 2f)) {
                EasyTripTheme {
                    Box(Modifier.requiredWidth(320.dp).height(480.dp).testTag("compact-trip-list-container").consumeWindowInsets(androidx.compose.foundation.layout.WindowInsets.safeDrawing)) {
                        TripListContent(
                            state = TripListUiState(
                                page = TripListPageState.Content(
                                    primaryTrip = trip("trip-primary", "一段特别特别长而且需要完整换行展示的旅行名称"),
                                    otherTrips = (1..12).map { index ->
                                        trip("trip-$index", "另一段同样很长而且需要换行的旅行名称 $index")
                                    },
                                ),
                            ),
                            onAction = {},
                        )
                    }
                }
            }
        }

        val container = compose.onNodeWithTag("compact-trip-list-container").getUnclippedBoundsInRoot()
        val create = compose.onNodeWithTag("create-trip")
            .assertWidthIsEqualTo(132.dp)
            .assertHeightIsEqualTo(48.dp)
            .assertIsDisplayed()
            .getUnclippedBoundsInRoot()
        assertEquals(container.right - 16.dp, create.right)
        assertEquals(container.bottom - 20.dp, create.bottom)
        val list = compose.onNodeWithTag("other-trips-list")
            .assertIsDisplayed()
            .getUnclippedBoundsInRoot()
        check(create.top >= container.top && create.bottom <= container.bottom)
        check(list.bottom > list.top)

        compose.onNodeWithTag("other-trips-list").performScrollToNode(hasTestTag("other-trip-trip-12"))
        compose.onNodeWithTag("other-trip-trip-12").assertIsDisplayed()
        compose.onNodeWithTag("other-trips-list").performScrollToNode(hasTestTag("primary-trip-trip-primary"))
        compose.onNodeWithTag("primary-trip-trip-primary").assertIsDisplayed()
        compose.onNodeWithTag("create-trip").assertIsDisplayed()
    }

    @Test fun narrowLargeFontLayoutKeepsFixedActionsAndOtherTripsReachable() {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = LocalDensity.current.density, fontScale = 2f)) {
                EasyTripTheme {
                    Box(Modifier.requiredWidth(280.dp).height(700.dp).consumeWindowInsets(androidx.compose.foundation.layout.WindowInsets.safeDrawing)) {
                        TripListContent(
                            state = TripListUiState(
                                page = TripListPageState.Content(
                                    primaryTrip = trip("trip-primary", "一段特别特别长而且需要完整换行展示的旅行名称"),
                                    otherTrips = (1..12).map { index ->
                                        trip("trip-$index", "另一段同样很长而且需要换行的旅行名称 $index")
                                    },
                                ),
                            ),
                            onAction = {},
                        )
                    }
                }
            }
        }

        compose.onNodeWithTag("primary-trip-trip-primary").assert(hasClickAction()).assertIsDisplayed()
        compose.onNodeWithTag("create-trip").assertWidthIsEqualTo(132.dp).assertHeightIsEqualTo(48.dp).assertIsDisplayed()
        compose.onNodeWithTag("other-trips-list").performScrollToNode(hasTestTag("other-trip-trip-12"))
        compose.onNodeWithTag("other-trip-trip-12").assertIsDisplayed()
    }

    @Test fun primaryMenuContainsOnlySettingsAndDeleteForBoundTrip() {
        val actions = mutableListOf<TripListAction>()
        setContent(content(), actions::add)

        compose.onNodeWithTag("trip-menu-trip-1").performClick()
        compose.onNodeWithTag("trip-menu-settings-trip-1").assertIsDisplayed()
        compose.onNodeWithTag("trip-menu-delete-trip-1").assertIsDisplayed()
        compose.onAllNodes(
            hasClickAction() and hasAnyAncestor(hasTestTag("trip-menu-popup-trip-1")),
            useUnmergedTree = true,
        ).assertCountEquals(2)
        compose.onNodeWithTag("trip-menu-delete-trip-1").assert(TripMenuTone.keyIs("danger"))
        compose.onNodeWithTag("trip-menu-settings-trip-1").performClick()
        compose.onNodeWithTag("trip-menu-trip-1").performClick()
        compose.onNodeWithTag("trip-menu-delete-trip-1").performClick()

        assertEquals(
            listOf(TripListAction.OpenSettings("trip-1"), TripListAction.RequestDelete("trip-1")),
            actions,
        )
        compose.onAllNodesWithTag("trip-menu-settings-trip-2").assertCountEquals(0)
    }

    @Test fun otherTripMenuIsBoundToStableTripId() {
        val page = mutableStateOf(content())
        val actions = mutableListOf<TripListAction>()
        compose.setContent {
            EasyTripTheme { TripListContent(TripListUiState(page = page.value), actions::add) }
        }

        compose.onNodeWithTag("trip-menu-trip-2").performClick()
        page.value = TripListPageState.Content(
            primaryTrip = trip("trip-1", "京都"),
            otherTrips = listOf(trip("trip-3", "首尔"), trip("trip-2", "东京")),
        )
        compose.waitForIdle()
        compose.onNodeWithTag("trip-menu-delete-trip-2").performClick()

        assertEquals(listOf(TripListAction.RequestDelete("trip-2")), actions)
    }

    @Test fun emptyStateUsesRemainingSpaceWithoutFixedHeight() {
        var emptyHeightPx = 0
        lateinit var density: Density
        compose.setContent {
            density = LocalDensity.current
            EasyTripTheme {
                Box(Modifier.requiredWidth(320.dp).height(300.dp).consumeWindowInsets(androidx.compose.foundation.layout.WindowInsets.safeDrawing)) {
                    TripListContent(
                        state = TripListUiState(page = TripListPageState.Empty),
                        onAction = {},
                        modifier = Modifier.fillMaxSize(),
                        emptyStateModifier = Modifier.onGloballyPositioned { emptyHeightPx = it.size.height },
                    )
                }
            }
        }

        compose.onNodeWithText("开始规划一次旅行").assertIsDisplayed()
        compose.onNodeWithTag("create-trip").assertIsNotDisplayed().performScrollTo().assertIsDisplayed()
        val emptyHeightDp = with(density) { emptyHeightPx.toDp() }
        assertEquals(true, emptyHeightDp > 0.dp && emptyHeightDp < 300.dp)
        assertFalse(emptyHeightDp == 647.dp)
    }

    @Test fun longNamesAtNarrowWidthAndLargeFontKeepActionsReachable() {
        var overflowed = false
        compose.setContent {
            val deviceDensity = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density = deviceDensity, fontScale = 2f)) {
                EasyTripTheme {
                    Box(Modifier.requiredWidth(280.dp).height(900.dp).consumeWindowInsets(androidx.compose.foundation.layout.WindowInsets.safeDrawing)) {
                        TripListContent(
                            modifier = Modifier.fillMaxSize().onGloballyPositioned {
                                overflowed = it.size.width > (280 * deviceDensity).toInt()
                            },
                            state = TripListUiState(
                                page = TripListPageState.Content(
                                    primaryTrip = trip("trip-long", "一段特别特别长而且需要完整换行展示的旅行名称"),
                                    otherTrips = listOf(trip("trip-other", "另一段同样很长而且需要换行的旅行名称")),
                                ),
                            ),
                            onAction = {},
                        )
                    }
                }
            }
        }

        compose.onNodeWithTag("primary-trip-trip-long").assert(hasClickAction()).assertIsDisplayed()
        compose.onNodeWithTag("trip-menu-trip-long").assertIsDisplayed()
        compose.onNodeWithTag("other-trip-trip-other").assertIsDisplayed()
        compose.onNodeWithTag("trip-menu-trip-other").assertIsDisplayed()
        compose.onNodeWithTag("create-trip").assertIsDisplayed()
        listOf(
            "primary-trip-name-trip-long",
            "trip-menu-trip-long",
            "other-trip-name-trip-other",
            "trip-menu-trip-other",
        ).forEach { tag ->
            val bounds = compose.onNodeWithTag(tag, useUnmergedTree = true).getUnclippedBoundsInRoot()
            assertEquals("$tag left bound", true, bounds.left >= 0.dp)
            assertEquals("$tag right bound", true, bounds.right <= 280.dp)
        }
        assertFalse(overflowed)
    }

    @Test fun errorStateKeepsCreateAndRetryActions() {
        val actions = mutableListOf<TripListAction>()
        setContent(TripListPageState.Error("无法加载旅行"), actions::add)

        compose.onNodeWithTag("retry-trips").performClick()
        compose.onNodeWithTag("create-trip").performClick()
        assertEquals(listOf(TripListAction.Retry, TripListAction.CreateTrip), actions)
    }

    private fun content() = TripListPageState.Content(
        primaryTrip = trip("trip-1", "京都"),
        otherTrips = listOf(trip("trip-2", "东京")),
    )

    private fun trip(id: String, name: String) = TripCardUiModel(
        id = id,
        name = name,
        dayCountLabel = "3天2晚",
        dateLabel = null,
        travelModeLabel = "灵活",
        countdownLabel = "待定日期",
        placeCount = 0,
        scheduledDayCount = 0,
        placeCountLabel = "0 个地点",
        tripDayCountLabel = "3 天行程",
        readinessPercent = 0,
        readinessLabel = "0%",
    )

    private fun nonZeroTrip(id: String, name: String) = trip(id, name).copy(
        placeCount = 3,
        scheduledDayCount = 2,
        placeCountLabel = "3 个地点",
        tripDayCountLabel = "3 天行程",
        readinessPercent = 67,
        readinessLabel = "67%",
    )

    @Test fun nonZeroTripFixtureExposesCountsAndReadiness() {
        setContent(TripListPageState.Content(listOf(nonZeroTrip("trip-nonzero", "杭州"))))
        compose.onNodeWithText("3 个地点").assertIsDisplayed()
        compose.onNodeWithText("3 天行程").assertIsDisplayed()
        compose.onAllNodesWithText("2 个已安排地点").assertCountEquals(0)
        compose.onNodeWithText("行程准备度", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("67%", useUnmergedTree = true).assertIsDisplayed()
    }

    private fun setContent(page: TripListPageState, onAction: (TripListAction) -> Unit = {}) {
        compose.setContent {
            EasyTripTheme { TripListContent(TripListUiState(page = page), onAction) }
        }
    }

    private fun horizontalColorRuns(
        pixels: androidx.compose.ui.graphics.PixelMap,
        y: Int,
        color: androidx.compose.ui.graphics.Color,
    ): List<IntRange> {
        val runs = mutableListOf<IntRange>()
        var start: Int? = null
        (0 until pixels.width).forEach { x ->
            val matches = pixels[x, y] == color
            if (matches && start == null) start = x
            if (!matches && start != null) {
                runs += start!! until x
                start = null
            }
        }
        start?.let { runs += it until pixels.width }
        return runs
    }

    private fun <T> androidx.compose.ui.semantics.SemanticsPropertyKey<T>.keyNotDefined() =
        androidx.compose.ui.test.SemanticsMatcher("$name is not defined") { node ->
            node.config.getOrNull(this) == null
        }

    private fun <T> androidx.compose.ui.semantics.SemanticsPropertyKey<T>.keyIs(value: T) =
        androidx.compose.ui.test.SemanticsMatcher("$name equals $value") { node ->
            node.config.getOrNull(this) == value
        }
}
