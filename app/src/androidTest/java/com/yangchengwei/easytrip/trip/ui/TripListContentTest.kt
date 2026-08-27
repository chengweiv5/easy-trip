package com.yangchengwei.easytrip.trip.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test fun primaryTripUsesSingleEntryActionAndFortyDpMenu() {
        val actions = mutableListOf<TripListAction>()
        setContent(content(), actions::add)

        compose.onNodeWithTag("continue-trip-trip-1").assertHeightIsEqualTo(48.dp).performClick()
        assertEquals(listOf(TripListAction.OpenTrip("trip-1")), actions)
        compose.onNodeWithTag("trip-menu-trip-1").assertWidthIsEqualTo(40.dp).assertHeightIsEqualTo(40.dp)
        compose.onAllNodesWithTag("trip-trip-1").assertCountEquals(0)
        compose.onAllNodesWithTag("trip-settings-trip-1").assertCountEquals(0)
        compose.onAllNodesWithTag("trip-delete-trip-1").assertCountEquals(0)
    }

    @Test fun primaryMenuContainsOnlySettingsAndDeleteForBoundTrip() {
        val actions = mutableListOf<TripListAction>()
        setContent(content(), actions::add)

        compose.onNodeWithTag("trip-menu-trip-1").performClick()
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
        var emptyHeight = 0
        compose.setContent {
            EasyTripTheme {
                Box(Modifier.requiredWidth(320.dp).height(480.dp)) {
                    TripListContent(
                        state = TripListUiState(page = TripListPageState.Empty),
                        onAction = {},
                        modifier = Modifier.fillMaxSize(),
                        emptyStateModifier = Modifier.onGloballyPositioned { emptyHeight = it.size.height },
                    )
                }
            }
        }

        compose.onNodeWithText("开始规划一次旅行").assertIsDisplayed()
        compose.onNodeWithText("创建旅行后，可以收藏地点并按天安排行程").assertIsDisplayed()
        compose.onNodeWithTag("create-trip").performScrollTo().assertIsDisplayed()
        assertFalse(emptyHeight == 647)
    }

    @Test fun longNamesAtNarrowWidthAndLargeFontKeepActionsReachable() {
        var overflowed = false
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = 2f)) {
                EasyTripTheme {
                    Box(Modifier.requiredWidth(280.dp).height(900.dp)) {
                        TripListContent(
                            modifier = Modifier.onGloballyPositioned { overflowed = it.size.width > 280 },
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

        compose.onNodeWithTag("continue-trip-trip-long").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("trip-menu-trip-long").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("other-trip-trip-other").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("trip-menu-trip-other").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("create-trip").performScrollTo().assertIsDisplayed()
        val menuBounds = compose.onNodeWithTag("trip-menu-trip-long").getUnclippedBoundsInRoot()
        assertEquals(true, menuBounds.left >= 0.dp && menuBounds.right <= 280.dp)
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

    private fun trip(id: String, name: String) = TripCardUiModel(id, name, "3 天", null, "灵活")

    private fun setContent(page: TripListPageState, onAction: (TripListAction) -> Unit = {}) {
        compose.setContent {
            EasyTripTheme { TripListContent(TripListUiState(page = page), onAction) }
        }
    }

    private fun <T> androidx.compose.ui.semantics.SemanticsPropertyKey<T>.keyNotDefined() =
        androidx.compose.ui.test.SemanticsMatcher("$name is not defined") { node ->
            node.config.getOrNull(this) == null
        }
}
