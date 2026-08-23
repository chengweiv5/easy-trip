package com.yangchengwei.easytrip.trip.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import org.junit.Rule
import org.junit.Test

class CreateTripContentTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun showsFullPageTitleFieldsPlanningTipAndSubmit() {
        compose.setContent {
            EasyTripTheme { CreateTripContent(CreateTripUiState(), {}) }
        }

        compose.onNodeWithText("创建旅行", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("基本信息").assertIsDisplayed()
        compose.onNodeWithText("旅行名称").assertIsDisplayed()
        compose.onNodeWithText("出行日期").assertIsDisplayed()
        compose.onNodeWithText("出行方式").assertIsDisplayed()
        compose.onNodeWithText("创建后先收藏感兴趣的地点，再从地点池添加到每天的行程。").assertIsDisplayed()
        compose.onNodeWithTag("create-submit").assertIsDisplayed()
    }

    @Test fun validationErrorsRemainVisibleUntilCorrected() {
        compose.setContent {
            EasyTripTheme {
                CreateTripContent(
                    CreateTripUiState(nameError = "请输入旅行名称", dayCountError = "请输入至少 1 天", dateError = "请选择开始日期"),
                    {},
                )
            }
        }

        compose.onNodeWithText("请输入旅行名称").assertIsDisplayed()
        compose.onNodeWithText("请输入至少 1 天").assertIsDisplayed()
        compose.onNodeWithText("请选择开始日期").assertIsDisplayed()
    }

    @Test fun keyboardDoesNotHideSubmitAction() {
        compose.setContent {
            EasyTripTheme {
                Box(Modifier.height(500.dp).testTag("small-window")) {
                    CreateTripContent(CreateTripUiState(), {})
                }
            }
        }

        compose.onNodeWithTag("create-submit").assertIsDisplayed().performClick()
    }
}
