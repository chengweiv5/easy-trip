package com.yangchengwei.easytrip.workspace

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CompletableDeferred
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.amap.PlaceSearchDataSource
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class MapPlaceDialogTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val point = GeoPoint(30.2, 120.1)

    @Test fun resolvedAddressIsDisplayedAndCollectedWithOneCompactAction() {
        var collected: PlaceCandidate? = null
        var closed = false
        compose.setContent {
            EasyTripTheme {
                MapPlaceDialog(
                    poi = MapPoiUi("poi", "西湖天地", "", point),
                    source = object : PlaceSearchDataSource {
                        override suspend fun search(keyword: String, city: String?) = emptyList<PlaceCandidate>()
                        override suspend fun details(poiId: String) =
                            PlaceCandidate(poiId, "西湖天地", "上城区南山路147号", point, "0571")
                    },
                    isSaved = false, collectionBusy = false, collectionError = null,
                    onToggleCollection = { collected = it }, onDismiss = { closed = true },
                )
            }
        }
        compose.onNodeWithText("上城区南山路147号").assertIsDisplayed()
        compose.onNodeWithText("关闭").assertDoesNotExist()
        compose.onNodeWithTag("place-card-collection").assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(112.dp)
        compose.onNodeWithText("收藏地点").performClick()
        assertEquals("上城区南山路147号", collected?.address)
        compose.onNodeWithContentDescription("关闭地点详情").assertWidthIsAtLeast(48.dp).performClick()
        assertTrue(closed)
    }

    @Test fun loadingDisablesCollectionAndRetryResolvesAddress() {
        val response = CompletableDeferred<PlaceCandidate?>()
        var attempts = 0
        val source = source {
            if (++attempts == 1) response.await() else PlaceCandidate(it, "西湖天地", "成功地址", point, null)
        }
        compose.setContent { EasyTripTheme {
            MapPlaceDialog(MapPoiUi("poi", "西湖天地", "", point), source, false, false, null, {}, {})
        } }
        compose.onNodeWithText("正在获取地址…").assertIsDisplayed()
        compose.onNodeWithTag("place-card-collection").assertIsNotEnabled()
        response.completeExceptionally(IllegalStateException("secret diagnostic"))
        compose.waitUntil(5_000) { compose.onAllNodesWithText("地址获取失败").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("重试").performClick()
        compose.onNodeWithText("成功地址").assertIsDisplayed()
        compose.onNodeWithTag("place-card-collection").assertIsEnabled()
        compose.onNodeWithText("secret diagnostic").assertDoesNotExist()
    }

    @Test fun savedPlaceHasCancelActionAndBusyPreventsDuplicateClicks() {
        val busy = mutableStateOf(false)
        var calls = 0
        compose.setContent { EasyTripTheme {
            MapPlaceDialog(MapPoiUi("poi", "西湖天地", "已有地址", point), null, true,
                busy.value, null, { calls++; busy.value = true }, {})
        } }
        screenshot("saved")
        compose.onNodeWithText("取消收藏").performClick()
        compose.onNodeWithText("正在取消…").assertIsDisplayed()
        compose.onNodeWithTag("place-card-collection").assertIsNotEnabled().performClick()
        assertEquals(1, calls)
        compose.onNodeWithContentDescription("关闭地点详情").assertIsEnabled()
    }

    @Test fun noIdDisablesCollectionAndKnownPlaceWithNoAddressCanStillBeCollected() {
        val selected = mutableStateOf(MapPoiUi(null, "无编号", "", point))
        var collected: PlaceCandidate? = null
        val source = source { null }
        compose.setContent { EasyTripTheme {
            MapPlaceDialog(selected.value, source, false, false, null, { collected = it }, {})
        } }
        compose.onNodeWithText("无法收藏").assertIsNotEnabled()
        compose.runOnIdle { selected.value = MapPoiUi("poi", "西湖天地", "", point) }
        compose.onNodeWithText("该地点未提供地址").assertIsDisplayed()
        compose.onNodeWithText("收藏地点").performClick()
        assertEquals("poi", collected?.poiId)
        assertEquals("", collected?.address)
    }

    @Test fun longTextKeepsCloseAtTopAndCollectionReachable() {
        var collected = false
        compose.setContent { EasyTripTheme {
            MapPlaceDialog(
                MapPoiUi("poi", "杭州西湖风景名胜区西湖天地游客服务中心", "杭州市上城区南山路147号西湖天地入口附近", point),
                null, false, false, "收藏失败，请重试", { collected = true }, {},
            )
        } }
        compose.onNodeWithContentDescription("关闭地点详情").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        val close = compose.onNodeWithContentDescription("关闭地点详情").getUnclippedBoundsInRoot()
        val title = compose.onNodeWithText("杭州西湖风景名胜区西湖天地游客服务中心").getUnclippedBoundsInRoot()
        assertTrue("Close must stay at the top, not the middle of a multiline title", close.top <= title.top + 2.dp)
        compose.onNodeWithTag("place-card-collection").performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        screenshot("large-font")
        compose.onNodeWithTag("place-card-collection").performClick()
        assertTrue(collected)
    }

    @Test fun compactActionIsRightAlignedWith36DpVisualAnd48DpTouchTarget() {
        var collected = false
        compose.setContent { EasyTripTheme {
            MapPlaceDialog(MapPoiUi("poi", "西湖天地", "上城区南山路147号", point),
                null, false, false, null, { collected = true }, {})
        } }
        val touch = compose.onNodeWithTag("place-card-collection")
        val visual = compose.onNodeWithTag("place-card-button-visual", useUnmergedTree = true)
        val actionBounds = touch.getUnclippedBoundsInRoot()
        val contentBounds = compose.onNodeWithTag("place-card-address").getUnclippedBoundsInRoot()
        val actionWidth = actionBounds.right - actionBounds.left
        assertEquals(contentBounds.right.value, actionBounds.right.value, 0.5f)
        assertTrue("The action must not fill the content width", actionWidth < contentBounds.right - contentBounds.left)
        val fontScale = compose.activity.resources.configuration.fontScale
        if (fontScale <= 1f) {
            touch.assertHeightIsEqualTo(48.dp)
            visual.assertHeightIsEqualTo(36.dp)
            visual.assertWidthIsAtLeast(112.dp)
            assertTrue("Normal action must stay compact", actionWidth <= 128.dp)
        } else {
            touch.assertHeightIsAtLeast(48.dp)
            visual.assertHeightIsAtLeast(36.dp)
            if (fontScale >= 2f) {
                val visualBounds = visual.getUnclippedBoundsInRoot()
                assertTrue("Large text must be allowed to grow the button",
                    visualBounds.bottom - visualBounds.top > 36.dp)
            }
        }
        screenshot("normal")
        touch.performTouchInput { click(androidx.compose.ui.geometry.Offset(center.x, 1f)) }
        assertTrue("The transparent top edge must remain clickable", collected)
    }

    private fun screenshot(name: String) {
        val file = java.io.File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "v301-$name.png")
        file.outputStream().use { compose.onNodeWithTag("map-place-card").captureToImage().asAndroidBitmap()
            .compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun source(lookup: suspend (String) -> PlaceCandidate?) = object : PlaceSearchDataSource {
        override suspend fun search(keyword: String, city: String?) = emptyList<PlaceCandidate>()
        override suspend fun details(poiId: String) = lookup(poiId)
    }
}
