package com.yangchengwei.easytrip.itinerary.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.model.RouteStatus
import com.yangchengwei.easytrip.core.model.TransportMode
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import com.yangchengwei.easytrip.trip.domain.TripDay
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class WorkspaceRefinementsTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private fun stop(id: String) = ItineraryItemUi(id, "地点 $id", "地址", null, 60)
    private fun leg(id: String, from: String, to: String, status: RouteStatus) = RouteLegUi(id, from, to, TransportMode.WALK, status, 500, 300, "网络不可用")

    @Test fun dragTracksFingerAcrossRouteRowsAndReordersOnce() {
        val state = mutableStateOf(DayItineraryUiState(
            days = listOf(TripDay("day", 0)), selectedDayId = "day",
            items = listOf(stop("a"), stop("b"), stop("c")),
            legs = listOf(leg("ab", "a", "b", RouteStatus.SUCCESS), leg("bc", "b", "c", RouteStatus.FAILED)),
        ))
        val commits = mutableListOf<DayItineraryAction.CommitMove>()
        compose.setContent { EasyTripTheme {
            DayItineraryContent(state.value, Modifier.width(360.dp).height(620.dp), onAction = { action ->
                when (action) {
                    is DayItineraryAction.PreviewMove -> {
                        val order = currentDisplayItems(state.value.items, state.value.previewOrder).map { it.id }.toMutableList()
                        order.remove(action.itemId); order.add(action.target, action.itemId)
                        state.value = state.value.copy(previewOrder = order)
                    }
                    is DayItineraryAction.CommitMove -> commits += action
                    else -> Unit
                }
            })
        } }
        val handle = compose.onNodeWithTag("drag-handle-a", true).fetchSemanticsNode().boundsInRoot.center
        val original = compose.onNodeWithTag("item-a", true).fetchSemanticsNode().boundsInRoot.top
        compose.mainClock.autoAdvance = false
        compose.onRoot().performTouchInput { down(handle); advanceEventTime(700) }
        compose.mainClock.advanceTimeBy(700)
        compose.onRoot().performTouchInput { moveBy(Offset(0f, 45f)) }
        compose.mainClock.advanceTimeBy(32)
        compose.onNodeWithTag("drag-overlay-a", true).assertExists()
        assertEquals(original + 45f, compose.onNodeWithTag("drag-overlay-a", true).fetchSemanticsNode().boundsInRoot.top, 2f)
        compose.onRoot().performTouchInput { moveBy(Offset(0f, 165f), 100) }
        compose.mainClock.advanceTimeBy(32)
        assertEquals(original + 210f, compose.onNodeWithTag("drag-overlay-a", true).fetchSemanticsNode().boundsInRoot.top, 2f)
        compose.onRoot().performTouchInput { moveBy(Offset(0f, 35f), 100) }
        compose.mainClock.advanceTimeBy(32)
        assertEquals(original + 245f, compose.onNodeWithTag("drag-overlay-a", true).fetchSemanticsNode().boundsInRoot.top, 2f)
        compose.onRoot().performTouchInput { up() }
        compose.mainClock.autoAdvance = true
        compose.onNodeWithTag("drag-overlay-a", true).assertDoesNotExist()
        assertEquals(1, commits.size)
        assertTrue(commits.single().target > 0)
    }

    @Test fun moveTargetDaysUseChineseAndCalendarDate() {
        assertEquals("第 2 天 · 9月20日 周日", moveTargetDayLabel(1, java.time.LocalDate.of(2026,9,19)))
        assertEquals("第 2 天 · 日期待定", moveTargetDayLabel(1, null))
    }

    @Test fun recoveryAndMarkerVisualEvidence() {
        compose.setContent { EasyTripTheme {
            androidx.compose.foundation.layout.Column(Modifier.width(360.dp)) {
                RouteLegRow(leg("visual", "a", "b", RouteStatus.FAILED).copy(error = "城市信息暂未获取，请重试或更改方式"), onMode = {}, onRetry = {})
                androidx.compose.ui.viewinterop.AndroidView(factory = { context ->
                    android.widget.LinearLayout(context).apply {
                        orientation = android.widget.LinearLayout.HORIZONTAL
                        setPadding(16, 20, 16, 20)
                        listOf("1" to "少林寺", "1·12·23" to "郑州东站", "1 +12" to "酒店").forEach { (badge, name) ->
                            addView(com.yangchengwei.easytrip.workspace.MarkerIconView(context,
                                com.yangchengwei.easytrip.workspace.MapMarkerUi("place-$badge", com.yangchengwei.easytrip.core.model.GeoPoint(30.25, 120.15), name, emptyList(),
                                    com.yangchengwei.easytrip.workspace.MapMarkerKind.SAVED_ITINERARY, badgeText = badge, scheduled = true)))
                        }
                    }
                })
            }
        } }
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        java.io.File(compose.activity.getExternalFilesDir(null), "inline-recovery-markers.png").outputStream().use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun failureRecoveryIsInlineWithoutSeparateButtons() {
        compose.setContent { EasyTripTheme {
            RouteLegRow(leg("inline", "a", "b", RouteStatus.FAILED).copy(error = "城市信息暂未获取，请重试或更改方式"), onMode = {}, onRetry = {})
        } }
        compose.onNodeWithTag("route-leg-action-inline", true).assert(SemanticsMatcher.keyNotDefined(androidx.compose.ui.semantics.SemanticsActions.OnClick))
        compose.onNodeWithTag("route-recovery-inline", true).assertExists()
        compose.onAllNodes(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.Role, androidx.compose.ui.semantics.Role.Button), true).assertCountEquals(0)
    }

    @Test fun inlineRecoveryWrapsAt280DpWithLargeFontAndKeepsActionsSeparate() {
        val events = mutableListOf<String>()
        compose.setContent {
            val density = androidx.compose.ui.platform.LocalDensity.current
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(density.density, 2f),
            ) { EasyTripTheme {
                androidx.compose.foundation.layout.Column(Modifier.width(280.dp)) {
                    RouteLegRow(leg("wrap", "a", "b", RouteStatus.FAILED).copy(error = "城市信息暂未获取，请重试或更改方式"), onMode = { events += "mode" }, onRetry = { events += "retry" })
                }
            } }
        }
        clickRecovery("wrap", "城市")
        assertTrue(events.isEmpty())
        clickRecovery("wrap", "重试")
        assertEquals(listOf("retry"), events)
        clickRecovery("wrap", "更改方式")
        assertEquals(listOf("retry", "mode"), events)
        val node = compose.onNodeWithTag("route-recovery-wrap", true)
        val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        node.performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue(layouts.single().lineCount >= 2)
        assertFalse(layouts.single().hasVisualOverflow)
    }

    @Test fun readonlyRecoveryTextHasNoLinks() {
        compose.setContent { EasyTripTheme {
            RouteLegRow(leg("readonly", "a", "b", RouteStatus.FAILED).copy(error = "城市信息暂未获取，请重试或更改方式"))
        } }
        val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        compose.onNodeWithTag("route-recovery-readonly", true).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val text = layouts.single().layoutInput.text
        assertTrue(text.getLinkAnnotations(0, text.length).isEmpty())
    }

    private fun clickRecovery(id: String, word: String) {
        val node = compose.onNodeWithTag("route-recovery-$id", true)
        val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        node.performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val layout = layouts.single()
        val offset = layout.layoutInput.text.text.indexOf(word)
        assertTrue("$word missing", offset >= 0)
        node.performTouchInput { click(layout.getBoundingBox(offset).center) }
        compose.waitForIdle()
    }

    @Test fun failedRouteCanChangeModeAfterRetryFails() {
        var edits = 0
        var retries = 0
        compose.setContent { EasyTripTheme {
            RouteLegRow(leg("ab", "a", "b", RouteStatus.FAILED), onMode = { edits++ }, onRetry = { retries++ })
        } }
        clickRecovery("ab", "重试")
        clickRecovery("ab", "更改方式")
        assertEquals(1, retries)
        assertEquals(1, edits)
    }
}
