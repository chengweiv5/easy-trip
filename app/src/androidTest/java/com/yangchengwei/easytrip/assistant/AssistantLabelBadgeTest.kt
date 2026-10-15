package com.yangchengwei.easytrip.assistant

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class AssistantLabelBadgeTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun originalAndCandidateLabelsFitCirclesAtNormalFont() = checkBadges(false)
    @Test fun originalAndCandidateLabelsFitCirclesAtSystemFont() = checkBadges(true)

    private fun checkBadges(systemFont: Boolean) {
        val labels = listOf("A", "AA", "CV", "A1", "AA1", "CV1")
        compose.setContent {
            EasyTripTheme {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, if (systemFont) density.fontScale else 1f)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        labels.forEach { AssistantLabelBadge(it) }
                    }
                }
            }
        }
        labels.forEach { label ->
            compose.onNodeWithTag("assistant-badge-$label", useUnmergedTree = true)
                .assertWidthIsEqualTo(28.dp).assertHeightIsEqualTo(28.dp)
            val layouts = mutableListOf<TextLayoutResult>()
            compose.onNodeWithText(label, useUnmergedTree = true)
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            val layout = layouts.single()
            assertEquals(1, layout.lineCount)
            // TextLayout's integer size can round below its fractional intrinsic width.
            assertFalse("$label height", layout.didOverflowHeight)
            assertTrue("$label right edge", layout.getLineRight(0) <= layout.size.width + 1f)
            assertTrue("$label left edge", layout.getLineLeft(0) >= -1f)
            with(layout.layoutInput.density) {
                val size = layout.layoutInput.style.fontSize.toPx() / density
                if (label.length <= 2) assertEquals(if (label.length == 1) 14f else 11f, size, .1f)
                else assertTrue(size in 1f..11f)
                assertTrue(layout.size.width / density <= 20.1f)
            }
        }
    }
}
