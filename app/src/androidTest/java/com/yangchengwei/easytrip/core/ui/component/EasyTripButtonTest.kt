package com.yangchengwei.easytrip.core.ui.component

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import org.junit.Rule
import org.junit.Test

class EasyTripButtonTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

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
}
