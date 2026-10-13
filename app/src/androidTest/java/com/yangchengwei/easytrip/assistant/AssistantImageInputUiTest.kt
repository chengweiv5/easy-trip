package com.yangchengwei.easytrip.assistant

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class AssistantImageInputUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @Test fun selectedImagesCanBePreviewedAndRemovedWithoutSending() {
        var picked = 0
        var previewed = ""
        var removed = ""
        val images = (1..20).map { AssistantImage("i$it", it, "image/png", 100, 80, 40) }
        compose.setContent { EasyTripTheme {
            AssistantImageInput(PlaceAssistantState(images = images), { picked++ }, { previewed = it }, { removed = it })
        } }
        compose.onNodeWithText("添加图片 · 20/20").assertIsDisplayed()
        compose.onNodeWithTag("assistant-image-list").performScrollToNode(hasTestTag("assistant-preview-i20"))
        compose.onNodeWithTag("assistant-preview-i20").performClick()
        compose.runOnIdle { assertEquals("i20", previewed); assertEquals(0, picked) }
        compose.onNodeWithTag("assistant-remove-i20").performClick()
        compose.runOnIdle { assertEquals("i20", removed) }
    }
}
