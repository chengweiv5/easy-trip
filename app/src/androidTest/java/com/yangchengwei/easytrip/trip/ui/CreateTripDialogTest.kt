package com.yangchengwei.easytrip.trip.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CreateTripDialogTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun submitShowsFieldErrors() {
        compose.setContent {
            var latest by remember { mutableStateOf(CreateTripUiState(visible = true)) }
            EasyTripTheme {
                CreateTripDialog(latest, { action ->
                    if (action == CreateTripAction.Submit) {
                        latest = latest.copy(nameError = "请输入旅行名称", dayCountError = "请输入至少 1 天")
                    }
                })
            }
        }

        compose.onNodeWithTag("create-submit").performClick()
        compose.onNodeWithText("请输入旅行名称").assertIsDisplayed()
        compose.onNodeWithText("请输入至少 1 天").assertIsDisplayed()
    }

    @Test fun dateErrorHasErrorSemantics() {
        compose.setContent {
            EasyTripTheme {
                CreateTripDialog(
                    CreateTripUiState(
                        visible = true,
                        name = "东京",
                        dayCount = "3",
                        timeMode = CreateTimeMode.DATED,
                        dateError = "请选择开始日期",
                    ),
                    {},
                )
            }
        }

        compose.onNodeWithTag("create-date-control")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, "请选择开始日期"))
    }

    @Test fun dismissPickerPreservesExistingDateSelection() {
        val date = java.time.LocalDate.of(2026, 10, 1)
        val actions = mutableListOf<CreateTripAction>()
        compose.setContent {
            EasyTripTheme {
                CreateTripDialog(
                    CreateTripUiState(
                        visible = true,
                        name = "东京",
                        dayCount = "3",
                        timeMode = CreateTimeMode.DATED,
                        startDate = date,
                    ),
                    actions::add,
                )
            }
        }
        compose.onNodeWithText(date.toString()).performClick()
        androidx.test.espresso.Espresso.pressBack()
        assertEquals(emptyList<CreateTripAction>(), actions)
    }

    @Test fun pendingRequestLocksFields() {
        compose.setContent {
            EasyTripTheme {
                CreateTripDialog(
                    CreateTripUiState(
                        visible = true,
                        name = "东京",
                        dayCount = "3",
                        requestId = "request-1",
                        submitError = "创建旅行失败，请重试",
                    ),
                    {},
                )
            }
        }
        compose.onNodeWithText("旅行名称").assertIsNotEnabled()
        compose.onNodeWithText("天数").assertIsNotEnabled()
        compose.onNodeWithTag("create-submit").assertIsDisplayed()
    }

    @Test fun submittingDisablesSubmit() {
        compose.setContent {
            EasyTripTheme {
                CreateTripDialog(
                    CreateTripUiState(visible = true, name = "东京", dayCount = "3", isSubmitting = true),
                    {},
                )
            }
        }

        compose.onNodeWithTag("create-submit").assertIsNotEnabled()
        compose.onNodeWithText("创建中…").assertIsDisplayed()
    }
}
