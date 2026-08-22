package com.yangchengwei.easytrip.core.ui.component

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SelectablePillTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun selectedAndUnselectedPillsExposeSelectionAndClickSemantics() {
        compose.setContent {
            Column {
                SelectablePill(selected = true, onClick = {}, label = { Text("已选") })
                SelectablePill(selected = false, onClick = {}, label = { Text("未选") })
            }
        }

        compose.onNodeWithText("已选").assertIsSelected().assertHasClickAction()
        compose.onNodeWithText("未选").assertIsNotSelected().assertHasClickAction()
    }

    @Test fun pillSupportsTabRoleAndCompactHeight() {
        compose.setContent {
            SelectablePill(
                selected = true,
                onClick = {},
                label = { Text("标签页") },
                role = Role.Tab,
            )
        }

        compose.onNodeWithText("标签页")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(32.dp)
    }

    @Test fun selectedPillIsFilledAndUnselectedPillHasNoFillOrOutline() {
        assertEquals(SelectablePillStyle(filled = true, border = null), selectablePillStyle(true))
        assertEquals(SelectablePillStyle(filled = false, border = null), selectablePillStyle(false))
    }
}
