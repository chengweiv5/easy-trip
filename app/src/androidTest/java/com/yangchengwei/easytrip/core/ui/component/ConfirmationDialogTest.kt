package com.yangchengwei.easytrip.core.ui.component

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ConfirmationDialogTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

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
}
