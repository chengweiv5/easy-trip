package com.yangchengwei.easytrip.core.ui.component

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test

class FeedbackStateTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun errorWithoutActionFailsClearly() {
        assertThrows(IllegalArgumentException::class.java) {
            compose.setContent {
                EasyTripTheme {
                    FeedbackState(FeedbackKind.ERROR, "加载失败")
                }
            }
        }
    }

    @Test
    fun errorWithActionShowsPersistentAction() {
        var retryCount = 0
        compose.setContent {
            EasyTripTheme {
                FeedbackState(
                    kind = FeedbackKind.ERROR,
                    title = "加载失败",
                    actionLabel = "重试",
                    onAction = { retryCount++ },
                )
            }
        }

        compose.onNodeWithText("加载失败").assertIsDisplayed()
        compose.onNodeWithText("重试").assertIsDisplayed().performClick()
        assertEquals(1, retryCount)
    }
}
