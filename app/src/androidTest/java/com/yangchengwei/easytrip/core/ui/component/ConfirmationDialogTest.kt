package com.yangchengwei.easytrip.core.ui.component

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.test.espresso.Espresso.pressBack
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ConfirmationDialogTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun busyDialogBlocksConfirmDismissBackAndOutsideDismiss() {
        var confirmed = 0
        var dismissed = 0
        compose.setContent {
            EasyTripTheme {
                ConfirmationDialog(
                    model = confirmation(),
                    onConfirm = { confirmed++ },
                    onDismiss = { dismissed++ },
                    busy = true,
                )
            }
        }

        compose.onNodeWithTag("confirmation-confirm").assertIsNotEnabled().performClick()
        compose.onNodeWithTag("confirmation-dismiss").assertIsNotEnabled().performClick()
        compose.onNodeWithText("处理中…").assertIsDisplayed()
        compose.onNodeWithTag("confirmation-progress").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo.Indeterminate),
        )
        pressBack()
        compose.waitForIdle()
        compose.onAllNodes(isRoot())[1].performTouchInput { click(Offset(1f, 1f)) }
        compose.onNodeWithText(confirmation().title).assertIsDisplayed()
        assertEquals(0, confirmed)
        assertEquals(0, dismissed)
    }

    @Test fun failedDialogKeepsImpactAndAllowsRetry() {
        var confirmed = 0
        compose.setContent {
            EasyTripTheme {
                ConfirmationDialog(
                    model = confirmation(),
                    onConfirm = { confirmed++ },
                    onDismiss = {},
                    errorMessage = "删除失败，请重试",
                )
            }
        }

        compose.onNodeWithText("5 个路线段").assertIsDisplayed()
        compose.onNodeWithText("删除失败，请重试").assertIsDisplayed()
        compose.onNodeWithTag("confirmation-confirm").performClick()
        assertEquals(1, confirmed)
    }

    @Test fun syncFailureShowsResyncAndBusyLocksRetryDismissBackAndOutsideDismiss() {
        var retried = 0
        var dismissed = 0
        var busy by mutableStateOf(false)
        compose.setContent {
            EasyTripTheme {
                ConfirmationDialog(
                    model = confirmation(),
                    onConfirm = { retried++; busy = true },
                    onDismiss = { dismissed++ },
                    busy = busy,
                    errorMessage = if (busy) null else "删除成功，但同步确认失败，请重新同步",
                    confirmLabel = "重新同步",
                )
            }
        }

        compose.onNodeWithText("删除成功，但同步确认失败，请重新同步").assertIsDisplayed()
        compose.onNodeWithText("重新同步").assertIsDisplayed().performClick()
        compose.onNodeWithTag("confirmation-confirm").assertIsNotEnabled().performClick()
        compose.onNodeWithTag("confirmation-dismiss").assertIsNotEnabled().performClick()
        pressBack()
        compose.waitForIdle()
        compose.onAllNodes(isRoot())[1].performTouchInput { click(Offset(1f, 1f)) }
        compose.onNodeWithText(confirmation().title).assertIsDisplayed()
        assertEquals(1, retried)
        assertEquals(0, dismissed)
    }

    @Test fun narrowLargeFontDialogKeepsActionsReachableWithFullImpactList() {
        var confirmed = 0
        var dismissed = 0
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = 2f)) {
                EasyTripTheme {
                    Box(Modifier.requiredWidth(280.dp).fillMaxHeight()) {
                        ConfirmationDialog(
                            model = ConfirmationUiModel(
                                title = "删除杭州春日慢游？",
                                message = "此操作将永久删除旅行及其中的所有内容，无法撤销。",
                                confirmLabel = "确认删除",
                                dismissLabel = "取消",
                                destructive = true,
                                reversible = false,
                                deletedItems = listOf("10 个旅行日", "99 个收藏地点", "20 个标签", "88 个行程项", "42 个路线段"),
                                retainedItems = listOf("其他旅行及其内容"),
                            ),
                            onConfirm = { confirmed++ },
                            onDismiss = { dismissed++ },
                        )
                    }
                }
            }
        }

        compose.onNodeWithText("42 个路线段").assertIsDisplayed()
        compose.onNodeWithTag("confirmation-dismiss").assertIsDisplayed().performClick()
        compose.onNodeWithTag("confirmation-confirm").assertIsDisplayed().performClick()
        assertEquals(1, dismissed)
        assertEquals(1, confirmed)
    }

    private fun confirmation() = ConfirmationUiModel(
        title = "删除杭州春日慢游？",
        message = "此操作将永久删除旅行及其中的所有内容，无法撤销。",
        confirmLabel = "确认删除",
        dismissLabel = "取消",
        destructive = true,
        reversible = false,
        deletedItems = listOf("3 个旅行日", "2 个收藏地点", "1 个标签", "4 个行程项", "5 个路线段"),
        retainedItems = listOf("其他旅行及其内容"),
    )
}
