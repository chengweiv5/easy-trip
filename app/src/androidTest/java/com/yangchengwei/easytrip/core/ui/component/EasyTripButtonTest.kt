package com.yangchengwei.easytrip.core.ui.component

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class EasyTripButtonTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun fixedWidthAndWeightedButtonsCenterContentHorizontallyAndVertically() {
        compose.setContent {
            EasyTripTheme {
                Column(Modifier.width(300.dp)) {
                    EasyTripPrimaryButton({}, Modifier.width(180.dp).testTag("fixed")) { Text("固定宽度") }
                    EasyTripSecondaryButton({}, Modifier.fillMaxWidth().testTag("full")) { Text("全宽") }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        EasyTripSecondaryButton({}, Modifier.weight(1f).testTag("weighted-cancel")) { Text("取消") }
                        EasyTripPrimaryButton({}, Modifier.weight(1f).testTag("weighted-save")) { Text("保存") }
                    }
                    EasyTripDangerButton({}, Modifier.testTag("natural")) { Text("删除") }
                }
            }
        }

        compose.onNodeWithTag("fixed").assertWidthIsEqualTo(180.dp)
        compose.onNodeWithTag("full").assertWidthIsEqualTo(300.dp)
        compose.onNodeWithTag("weighted-cancel").assertWidthIsEqualTo(146.dp)
        compose.onNodeWithTag("weighted-save").assertWidthIsEqualTo(146.dp)
        listOf("fixed" to "固定宽度", "full" to "全宽", "weighted-cancel" to "取消", "weighted-save" to "保存", "natural" to "删除")
            .forEach { (tag, label) -> assertCentered(tag, label) }
    }

    @Test
    fun naturalWidthButtonsLeaveRoomForEachOtherAndAcceptPhysicalClicks() {
        var cancelled = 0
        var continued = 0
        compose.setContent {
            EasyTripTheme {
                Row(Modifier.width(280.dp).testTag("footer"), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CompactSecondaryButton({ cancelled++ }, Modifier.testTag("cancel")) { Text("取消") }
                    CompactPrimaryButton({ continued++ }, Modifier.testTag("continue")) { Text("继续") }
                }
            }
        }

        val footer = compose.onNodeWithTag("footer").getUnclippedBoundsInRoot()
        val cancel = compose.onNodeWithTag("cancel").assertIsDisplayed().getUnclippedBoundsInRoot()
        val proceed = compose.onNodeWithTag("continue").assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue("cancel=$cancel continue=$proceed", proceed.right - proceed.left >= 48.dp)
        assertTrue(cancel.left >= footer.left && proceed.right <= footer.right)
        assertTrue("cancel=$cancel continue=$proceed", cancel.right + 8.dp <= proceed.left + 0.5.dp)
        assertCentered("cancel", "取消")
        assertCentered("continue", "继续")
        compose.onNodeWithTag("cancel").performTouchInput { click() }
        compose.onNodeWithTag("continue").performTouchInput { click() }
        compose.runOnIdle {
            assertEquals(1, cancelled)
            assertEquals(1, continued)
        }
    }

    @Test
    fun naturalWidthAddButtonDoesNotConsumeWeightedTagInput() {
        compose.setContent {
            EasyTripTheme {
                Box(Modifier.width(280.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField("自然", {}, Modifier.weight(1f).testTag("input"))
                        EasyTripSecondaryButton({}, Modifier.testTag("add")) { Text("添加") }
                    }
                }
            }
        }

        val input = compose.onNodeWithTag("input").assertIsDisplayed().getUnclippedBoundsInRoot()
        val add = compose.onNodeWithTag("add").assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue("input=$input add=$add", input.right - input.left >= 100.dp)
        assertTrue(input.right + 8.dp <= add.left)
        compose.onNodeWithText("自然", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun primaryButtonExposesDisabledClickSemanticsAndDesignHeight() {
        compose.setContent {
            EasyTripTheme {
                EasyTripPrimaryButton(onClick = {}, enabled = false) { Text("创建") }
            }
        }

        compose.onNodeWithText("创建")
            .assertHasClickAction()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertIsNotEnabled()
            .assertHeightIsEqualTo(48.dp)
    }

    private fun assertCentered(tag: String, label: String) {
        val button = compose.onNodeWithTag(tag).getUnclippedBoundsInRoot()
        val content = compose.onNodeWithText(label, useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue("button=$button text=$content", kotlin.math.abs(((content.left + content.right) / 2 - (button.left + button.right) / 2).value) < 0.5f)
        assertTrue("button=$button text=$content", kotlin.math.abs(((content.top + content.bottom) / 2 - (button.top + button.bottom) / 2).value) < 0.5f)
    }
}
