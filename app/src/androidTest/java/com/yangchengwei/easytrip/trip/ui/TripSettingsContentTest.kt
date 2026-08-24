package com.yangchengwei.easytrip.trip.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.yangchengwei.easytrip.core.model.TravelMode
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test

class TripSettingsContentTest {
    @get:Rule val compose = createComposeRule()

    @Test fun settingsExposesRangeAndNoAddInsertReorderOrSingleDayDateActions() {
        compose.setContent {
            TripSettingsContent(
                state = TripSettingsUiState(
                    tripId = "trip",
                    name = "Kyoto",
                    travelMode = TravelMode.FLEXIBLE,
                    days = listOf(DayUi("day-1", "2026-10-01")),
                    dateRange = DateRangeChangeUiState(LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-01")),
                ),
                onBack = {}, onRename = {}, onTravelMode = {}, onDateDraft = { _, _ -> },
                onSubmitDateRange = {}, onCancelDateRange = {}, onConfirmDateRange = {},
                onRequestDeleteDay = {}, onCancelDeleteDay = {}, onConfirmDeleteDay = {},
            )
        }

        compose.onNodeWithText("整体出行日期").assertIsDisplayed()
        compose.onNodeWithText("添加一天").assertDoesNotExist()
        compose.onNodeWithText("插入").assertDoesNotExist()
        compose.onNodeWithText("拖动排序").assertDoesNotExist()
        compose.onNodeWithText("设置单日日期").assertDoesNotExist()
    }

    @Test fun shrinkConfirmationListsCompleteDangerImpact() {
        compose.setContent {
            TripSettingsContent(
                state = TripSettingsUiState(
                    tripId = "trip",
                    dateRange = DateRangeChangeUiState(
                        startDate = LocalDate.parse("2026-10-01"),
                        endDate = LocalDate.parse("2026-10-01"),
                        confirmation = com.yangchengwei.easytrip.trip.domain.DateRangeChangeImpact(
                            LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-01"),
                            listOf("day-1"), listOf("day-2", "day-3"), 4, 2, 7,
                        ),
                    ),
                ),
                onBack = {}, onRename = {}, onTravelMode = {}, onDateDraft = { _, _ -> },
                onSubmitDateRange = {}, onCancelDateRange = {}, onConfirmDateRange = {},
                onRequestDeleteDay = {}, onCancelDeleteDay = {}, onConfirmDeleteDay = {},
            )
        }

        compose.onNodeWithText("将删除 2 个尾部旅行日、4 个行程项和 2 个路线段；7 个收藏地点会保留。").assertIsDisplayed()
        compose.onNodeWithText("确认修改").assertIsDisplayed()
    }
}
