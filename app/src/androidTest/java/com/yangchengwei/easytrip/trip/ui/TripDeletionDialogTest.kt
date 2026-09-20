package com.yangchengwei.easytrip.trip.ui

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TripDeletionDialogTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun leavingWhileImpactLoadsCancelsPendingConfirmationWithoutShowingAnOverlay() {
        var visible by mutableStateOf(true)
        var deletion: TripDeletionUiState by mutableStateOf(TripDeletionUiState.LoadingImpact("trip", "杭州"))
        var cancellations = 0
        compose.setContent {
            EasyTripTheme {
                Text("我的旅行")
                if (visible) TripDeletionDialog(
                    deletion = deletion,
                    onCancel = { cancellations++; deletion = TripDeletionUiState.Idle },
                    onRetryImpact = {}, onConfirm = {}, onRetrySync = {},
                )
            }
        }
        compose.onNodeWithText("我的旅行").assertIsDisplayed()
        compose.onNodeWithText("删除杭州？").assertDoesNotExist()
        compose.runOnIdle { visible = false }
        compose.runOnIdle {
            assertEquals(1, cancellations)
            assertEquals(TripDeletionUiState.Idle, deletion)
            visible = true
        }
        compose.onNodeWithText("删除杭州？").assertDoesNotExist()
    }
}
