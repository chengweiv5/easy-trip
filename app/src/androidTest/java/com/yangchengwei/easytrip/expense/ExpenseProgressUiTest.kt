package com.yangchengwei.easytrip.expense

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import com.yangchengwei.easytrip.core.ui.theme.ThemePalette
import com.yangchengwei.easytrip.expense.ui.ReviewRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ExpenseProgressUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun quarterExpenseShareHasOneContinuousFillAndNoTrailingDot() {
        var fill = Color.Unspecified
        var track = Color.Unspecified
        compose.setContent {
            EasyTripTheme {
                fill = MaterialTheme.colorScheme.primary
                track = MaterialTheme.colorScheme.primaryContainer
                Surface {
                    Box(Modifier.width(320.dp)) {
                        ReviewRow("住宿", ExpenseTotal(2500, 1, 1), "sample", fraction = .25f) {}
                    }
                }
            }
        }

        val pixels = compose.onNode(
            SemanticsMatcher.expectValue(
                SemanticsProperties.ProgressBarRangeInfo,
                ProgressBarRangeInfo(.25f, 0f..1f),
            ),
            useUnmergedTree = true,
        ).captureToImage().toPixelMap()
        val runs = colorRuns(pixels, fill)
        assertEquals("花费占比只能有一段连续填充，末端不能有独立圆点", 1, runs.size)
        val splitX = runs.single().last + 1
        assertTrue("填充长度应为轨道的25%", kotlin.math.abs(splitX - pixels.width / 4) <= 1)
        assertTrue("填充与底轨必须逐行贴合，不留断口", (0 until pixels.height).all { y ->
            pixels[splitX, y] == track
        })
    }

    @Test fun zeroSmallAndFullExpenseSharesKeepExactValuesInEveryTheme() {
        var fraction by mutableStateOf(0f)
        var palette by mutableStateOf(ThemePalette.LAKE)
        compose.setContent {
            EasyTripTheme(palette) {
                Surface {
                    Box(Modifier.width(320.dp)) {
                        ReviewRow("住宿", ExpenseTotal(if (fraction == 0f) 0 else 2500, 1, 1), "sample", fraction = fraction) {}
                    }
                }
            }
        }

        for (theme in ThemePalette.entries) {
            compose.runOnIdle { palette = theme }
            for (value in listOf(0f, .01f, .25f, 1f)) {
                compose.runOnIdle { fraction = value }
                val bar = compose.onNode(
                    SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo(value, 0f..1f)),
                    useUnmergedTree = true,
                ).assertHeightIsEqualTo(5.dp)
                val image = bar.captureToImage()
                val pixels = image.toPixelMap()
                val runs = colorRuns(pixels, theme.colors.primary)
                if (value == 0f) {
                    assertTrue("真实零元不能画假进度或末端圆点", runs.isEmpty())
                    compose.onNodeWithTag("sample").assertTextContains("¥0.00", substring = true)
                } else {
                    assertEquals("${theme.id} $value 只保留一段填充", 1, runs.size)
                    val expectedEnd = when (value) { .01f -> pixels.width / 100; .25f -> pixels.width / 4; else -> pixels.width }
                    assertTrue("填充宽度与轨道的真实比例一致", kotlin.math.abs(runs.single().last + 1 - expectedEnd) <= 1)
                }
                val expectedEndColor = if (value == 1f) theme.colors.primary else theme.colors.primaryContainer
                assertEquals("轨道右端不得存在额外圆点", expectedEndColor, pixels[pixels.width - 3, pixels.height / 2])
                if (theme == ThemePalette.VIOLET && value == .25f) {
                    java.io.File(compose.activity.getExternalFilesDir(null), "progress-violet-quarter.png").outputStream().use {
                        image.asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
                    }
                }
            }
        }
    }

    @Test fun rightToLeftExpenseShareFillsFromTheReadingStartWithoutADot() {
        val theme = ThemePalette.VIOLET
        compose.setContent {
            EasyTripTheme(theme) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface {
                        Box(Modifier.width(320.dp)) {
                            ReviewRow("住宿", ExpenseTotal(2500, 1, 1), "sample", fraction = .25f) {}
                        }
                    }
                }
            }
        }
        val pixels = compose.onNode(
            SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo(.25f, 0f..1f)),
            useUnmergedTree = true,
        ).captureToImage().toPixelMap()
        val runs = colorRuns(pixels, theme.colors.primary)
        assertEquals("RTL仍只有一段连续填充", 1, runs.size)
        assertTrue("25%占比从右端开始", kotlin.math.abs(pixels.width * 3 / 4 - runs.single().first) <= 1)
        assertEquals(theme.colors.primaryContainer, pixels[3, pixels.height / 2])
    }

    private fun colorRuns(pixels: PixelMap, color: Color): List<IntRange> {
        val runs = mutableListOf<IntRange>()
        var start: Int? = null
        (0 until pixels.width).forEach { x ->
            val matches = pixels[x, pixels.height / 2] == color
            if (matches && start == null) start = x
            if (!matches && start != null) {
                runs += start!! until x
                start = null
            }
        }
        start?.let { runs += it until pixels.width }
        return runs
    }
}
