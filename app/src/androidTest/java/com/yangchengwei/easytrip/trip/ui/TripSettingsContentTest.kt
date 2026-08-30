package com.yangchengwei.easytrip.trip.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.test.espresso.Espresso.pressBack
import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.trip.domain.DateRangeChangeImpact
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TripSettingsContentTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun settingsExposesRangeAndNoAddInsertReorderOrSingleDayDateActions() {
        compose.setContent { content(state = datedState()) }

        compose.onNodeWithText("整体出行日期").assertIsDisplayed()
        compose.onNodeWithText("添加一天").assertDoesNotExist()
        compose.onNodeWithText("插入").assertDoesNotExist()
        compose.onNodeWithText("拖动排序").assertDoesNotExist()
        compose.onNodeWithText("设置单日日期").assertDoesNotExist()
    }

    @Test fun settingsRowsExposeSingleClickActionAndDayDeleteHas48DpTarget() {
        compose.setContent { content(state = datedState().copy(dateRange = datedState().dateRange.copy(phase = DateRangeChangePhase.Idle))) }

        compose.onNodeWithTag("settings-rename").assert(
            androidx.compose.ui.test.SemanticsMatcher("has one click action") { node ->
                node.config.contains(SemanticsActions.OnClick)
            },
        )
        compose.onNodeWithTag("settings-date-row").assert(
            androidx.compose.ui.test.SemanticsMatcher("has one click action") { node ->
                node.config.contains(SemanticsActions.OnClick)
            },
        )
        compose.onNodeWithTag("delete-day-day-2").assertHeightIsAtLeast(48.dp)
    }

    @Test fun settingsUsesDesignHeaderAndDoneReturnsThroughBackCallback() {
        var backs = 0
        compose.setContent {
            content(
                state = datedState().copy(dateRange = datedState().dateRange.copy(phase = DateRangeChangePhase.Idle)),
                onBack = { backs++ },
            )
        }

        compose.onNodeWithTag("settings-header").assertHeightIsEqualTo(62.dp)
        compose.onNodeWithTag("settings-done").assertIsEnabled().performClick()
        compose.onNodeWithTag("delete-day-day-2").assertHeightIsAtLeast(48.dp)
        assertEquals(1, backs)
    }

    @Test fun dateRowOpensLocalEditorAndAppliesOnlyFromTheDialog() {
        var submissions = 0
        compose.setContent {
            TripSettingsContent(
                state = datedState().copy(dateRange = datedState().dateRange.copy(phase = DateRangeChangePhase.Idle)),
                onBack = {}, onRename = {}, onTravelMode = {}, onDateEndDraft = {},
                onSubmitDateRange = { submissions++ }, onCancelDateRange = {}, onConfirmDateRange = {},
                onRetryDateRangeSync = {}, onRequestDeleteDay = {}, onRetryDeleteDay = {},
                onCancelDeleteDay = {}, onConfirmDeleteDay = {},
            )
        }

        compose.onNodeWithText("修改出行日期").assertDoesNotExist()
        compose.onNodeWithTag("settings-date-row").performClick()
        compose.onNodeWithText("修改出行日期").assertIsDisplayed()
        compose.onNodeWithTag("settings-start-date").assert(
            androidx.compose.ui.test.SemanticsMatcher("has no SetText action") {
                !it.config.contains(SemanticsActions.SetText)
            },
        )
        compose.onNodeWithTag("settings-end-date").performTextReplacement("2026-10-02")
        compose.onNodeWithTag("settings-apply-date-range").performClick()
        assertEquals(1, submissions)
    }

    @Test fun startDateIsReadOnlyAndOnlyEndDateDispatchesDraft() {
        val drafts = mutableListOf<LocalDate?>()
        compose.setContent {
            content(
                state = datedState().let { state ->
                    state.copy(dateRange = state.dateRange.copy(phase = DateRangeChangePhase.Idle))
                },
                onDateEndDraft = drafts::add,
            )
        }

        compose.onNodeWithTag("settings-date-row").performClick()
        compose.onNodeWithTag("settings-start-date", useUnmergedTree = true).assert(
            androidx.compose.ui.test.SemanticsMatcher("has no SetText action") {
                !it.config.contains(SemanticsActions.SetText)
            },
        )
        compose.onNodeWithTag("settings-end-date").assert(
            androidx.compose.ui.test.SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("结束日期")),
        ).assert(
            androidx.compose.ui.test.SemanticsMatcher("has SetText action") {
                it.config.contains(SemanticsActions.SetText)
            },
        ).performTextReplacement("2026-10-02")
        compose.onNodeWithTag("settings-apply-date-range").assertIsEnabled().performClick()

        assertEquals(listOf(LocalDate.parse("2026-10-02")), drafts)
    }

    @Test fun applyingLocksDateInputBackDeleteAndDialogDismiss() {
        var backs = 0
        var cancellations = 0
        val state = datedState(
            days = listOf(DayUi("day-1", "2026-10-01"), DayUi("day-2", "2026-10-02")),
            phaseFactory = { request, impact -> DateRangeChangePhase.Applying(request, impact) },
        )
        compose.setContent {
            content(
                state = state,
                onBack = { backs++ },
                onCancelDateRange = { cancellations++ },
            )
        }

        compose.onNodeWithTag("settings-date-row").assertIsNotEnabled()
        compose.onNodeWithText("修改出行日期").assertDoesNotExist()
        compose.onNodeWithTag("delete-day-day-2").assertIsNotEnabled()
        compose.onNodeWithTag("settings-back").assertIsNotEnabled().performClick()
        compose.onNodeWithTag("confirmation-dismiss").assertIsNotEnabled().performClick()
        pressBack()
        compose.waitForIdle()
        compose.onAllNodes(isRoot())[1].performTouchInput { click(Offset(1f, 1f)) }
        compose.onNodeWithText("确认修改日期范围？").assertIsDisplayed()
        assertEquals(0, backs)
        assertEquals(0, cancellations)
    }

    @Test fun growthApplyingAndAwaitingRoomUsePageProgressWithoutDangerConfirmation() {
        var state by androidx.compose.runtime.mutableStateOf(
            growthState { request, impact -> DateRangeChangePhase.Applying(request, impact) },
        )
        compose.setContent { content(state = state) }

        fun assertPageProgressWithoutDangerConfirmation() {
            compose.onNodeWithText("正在保存日期范围…").assertIsDisplayed()
            compose.onNodeWithText("确认修改日期范围？").assertDoesNotExist()
            compose.onNodeWithText("缩短日期会删除超出范围的旅行内容。").assertDoesNotExist()
            compose.onNodeWithTag("settings-back").assertIsNotEnabled()
            compose.onNodeWithTag("settings-date-row").assertIsNotEnabled()
            compose.onNodeWithText("修改出行日期").assertDoesNotExist()
            compose.onNodeWithTag("delete-day-day-2").assertIsNotEnabled()
        }

        assertPageProgressWithoutDangerConfirmation()
        compose.runOnIdle {
            state = growthState { request, impact -> DateRangeChangePhase.AwaitingRoom(request, impact) }
        }
        assertPageProgressWithoutDangerConfirmation()
    }

    @Test fun awaitingRoomLocksBackInputDeleteApplyAndConfirmationDismiss() {
        var backs = 0
        var cancellations = 0
        val state = datedState(
            days = listOf(DayUi("day-1", "2026-10-01"), DayUi("day-2", "2026-10-02")),
            phaseFactory = { request, impact -> DateRangeChangePhase.AwaitingRoom(request, impact) },
        )
        compose.setContent {
            content(state, onBack = { backs++ }, onCancelDateRange = { cancellations++ })
        }

        compose.onNodeWithTag("settings-back").assertIsNotEnabled().performClick()
        compose.onNodeWithTag("settings-date-row").assertIsNotEnabled()
        compose.onNodeWithText("修改出行日期").assertDoesNotExist()
        compose.onNodeWithTag("delete-day-day-2").assertIsNotEnabled()
        compose.onNodeWithTag("confirmation-confirm").assertIsNotEnabled()
        compose.onNodeWithTag("confirmation-dismiss").assertIsNotEnabled().performClick()
        pressBack()
        compose.waitForIdle()
        compose.onAllNodes(isRoot())[1].performTouchInput { click(Offset(1f, 1f)) }
        compose.onNodeWithText("确认修改日期范围？").assertIsDisplayed()
        assertEquals(0, backs)
        assertEquals(0, cancellations)
    }

    @Test fun dayDeleteConfirmationIncludesCompleteDangerImpact() {
        compose.setContent {
            content(
                state = TripSettingsUiState(
                    tripId = "trip",
                    days = listOf(DayUi("day-1", "2026-10-01"), DayUi("day-2", "2026-10-02")),
                    pendingDayDeletion = PendingDayDeletion(
                        DayUi("day-1", "2026-10-01"),
                        DayDeleteImpact(2, 1, 7),
                    ),
                ),
            )
        }

        compose.onNodeWithText("将删除 2 个行程项和 1 个路线段；7 个收藏地点会保留。此操作不可撤销，后续旅行日日期编号和路线将变化。").assertIsDisplayed()
    }

    @Test fun activeDatePhaseHidesConflictingDayDeleteConfirmation() {
        val pending = PendingDayDeletion(
            DayUi("day-2", "2026-10-02"),
            DayDeleteImpact(2, 1, 7),
        )
        val phases = listOf<(DateRangeChangeRequest, DateRangeChangeImpact) -> DateRangeChangePhase>(
            { request, _ -> DateRangeChangePhase.Previewing(request) },
            { request, impact -> DateRangeChangePhase.AwaitingConfirmation(request, impact) },
            { request, impact -> DateRangeChangePhase.Applying(request, impact) },
            { request, impact -> DateRangeChangePhase.AwaitingRoom(request, impact) },
            { request, impact -> DateRangeChangePhase.SyncFailed(request, impact) },
        )

        var state by androidx.compose.runtime.mutableStateOf(
            datedState(phaseFactory = phases.first()).copy(pendingDayDeletion = pending),
        )
        compose.setContent { content(state = state) }

        phases.forEach { phase ->
            compose.runOnIdle {
                state = datedState(phaseFactory = phase).copy(pendingDayDeletion = pending)
            }
            compose.onNodeWithText("删除 2026-10-02？").assertDoesNotExist()
            compose.onNodeWithText("确认删除").assertDoesNotExist()
        }
    }

    @Test fun dateRangeMaximumErrorIsVisible() {
        compose.setContent {
            content(
                state = datedState().copy(
                    dateRange = datedState().dateRange.copy(
                        phase = DateRangeChangePhase.Idle,
                        error = "旅行最多 30 天",
                    ),
                ),
            )
        }

        compose.onNodeWithText("旅行最多 30 天").assertIsDisplayed()
    }

    @Test fun dayDeleteInProgressDisablesDateRangeApply() {
        compose.setContent {
            content(
                state = datedState().copy(
                    dayDeleteInProgress = true,
                    dateRange = datedState().dateRange.copy(phase = DateRangeChangePhase.Idle),
                ),
            )
        }

        compose.onNodeWithTag("settings-date-row").assertIsNotEnabled()
    }

    @Test fun failedDayPreviewShowsBodyErrorAndExplicitRetry() {
        var retries = 0
        compose.setContent {
            content(
                state = TripSettingsUiState(
                    tripId = "trip",
                    days = listOf(DayUi("day-1", "2026-10-01"), DayUi("day-2", "2026-10-02")),
                    dayDeleteError = "无法检查删除影响，请重试",
                    dayDeletionRetry = DayUi("day-2", "2026-10-02"),
                ),
                onRetryDeleteDay = { retries++ },
            )
        }

        compose.onNodeWithText("无法检查删除影响，请重试").assertIsDisplayed()
        compose.onNodeWithText("重试检查").assertIsDisplayed().performClick()
        assertEquals(1, retries)
    }

    @Test fun shrinkConfirmationExposesStructuredImpactCounts() {
        compose.setContent { content(state = datedState()) }

        compose.onNode(hasTestTag("settings-date-impact-days")).assertIsDisplayed()
        compose.onNodeWithTag("settings-date-impact-days").assertIsDisplayed()
        compose.onNodeWithTag("settings-date-impact-items").assertIsDisplayed()
        compose.onNodeWithTag("settings-date-impact-legs").assertIsDisplayed()
        compose.onNodeWithTag("settings-date-impact-saved-places").assertIsDisplayed()
        compose.onNodeWithText("2 个尾部旅行日").assertIsDisplayed()
        compose.onNodeWithText("4 个行程项").assertIsDisplayed()
        compose.onNodeWithText("2 个路线段").assertIsDisplayed()
        compose.onNodeWithText("7 个收藏地点").assertIsDisplayed()
        compose.onNodeWithText("确认修改").assertIsDisplayed()
    }

    @Test fun syncFailureLocksWritesButAllowsRetryAndBack() {
        var retries = 0
        var backs = 0
        val state = datedState(
            phaseFactory = { request, impact -> DateRangeChangePhase.SyncFailed(request, impact) },
            error = "保存结果待同步确认，请重新同步",
        )
        compose.setContent {
            content(
                state = state,
                onBack = { backs++ },
                onRetryDateRangeSync = { retries++ },
            )
        }

        compose.onNodeWithText("修改旅行名称").assertIsNotEnabled()
        compose.onNodeWithTag("settings-mode-FLEXIBLE").assertIsNotEnabled()
        compose.onNodeWithTag("settings-mode-SELF_DRIVE").assertIsNotEnabled()
        compose.onNodeWithTag("delete-day-day-3").assertIsNotEnabled()
        compose.onNodeWithTag("settings-date-sync-retry").assertIsDisplayed().performClick()
        compose.onNodeWithTag("settings-back").assertIsEnabled().performClick()
        compose.onNodeWithTag("confirmation-confirm").assertDoesNotExist()
        compose.onNodeWithTag("confirmation-dismiss").assertDoesNotExist()
        assertEquals(1, retries)
        assertEquals(1, backs)
    }

    @Test fun observationErrorShowsRetryAndHidesEmptySettingsBody() {
        var retries = 0
        compose.setContent {
            content(
                state = TripSettingsUiState(
                    tripId = "trip",
                    observationError = "无法加载旅行设置，请重试",
                ),
                onRetryTripObservation = { retries++ },
            )
        }

        compose.onNodeWithText("无法加载旅行设置，请重试").assertIsDisplayed()
        compose.onNodeWithText("重试").assertIsDisplayed().performClick()
        compose.onNodeWithText("整体出行日期").assertDoesNotExist()
        assertEquals(1, retries)
    }

    @Test fun undatedTripHasNoDateMutationAction() {
        compose.setContent {
            content(
                state = TripSettingsUiState(
                    tripId = "trip",
                    name = "Kyoto",
                    days = listOf(DayUi("day-1", "Day 1")),
                ),
            )
        }

        compose.onNodeWithTag("settings-start-date").assertIsDisplayed()
        compose.onNodeWithText("未设置日期").assertIsDisplayed()
        compose.onNodeWithTag("settings-end-date").assertDoesNotExist()
        compose.onNodeWithTag("settings-apply-date-range").assertDoesNotExist()
        compose.onNodeWithText("无日期").assertDoesNotExist()
    }

    @androidx.compose.runtime.Composable
    private fun content(
        state: TripSettingsUiState,
        onBack: () -> Unit = {},
        onDateEndDraft: (LocalDate?) -> Unit = {},
        onCancelDateRange: () -> Unit = {},
        onRetryDateRangeSync: () -> Unit = {},
        onRetryDeleteDay: () -> Unit = {},
        onRetryTripObservation: () -> Unit = {},
    ) = TripSettingsContent(
        state = state,
        onBack = onBack,
        onRename = {},
        onTravelMode = {},
        onDateEndDraft = onDateEndDraft,
        onSubmitDateRange = {},
        onCancelDateRange = onCancelDateRange,
        onConfirmDateRange = {},
        onRetryDateRangeSync = onRetryDateRangeSync,
        onRetryTripObservation = onRetryTripObservation,
        onRequestDeleteDay = {},
        onRetryDeleteDay = onRetryDeleteDay,
        onCancelDeleteDay = {},
        onConfirmDeleteDay = {},
    )

    private fun growthState(
        phaseFactory: (DateRangeChangeRequest, DateRangeChangeImpact) -> DateRangeChangePhase,
    ): TripSettingsUiState {
        val request = DateRangeChangeRequest(
            generation = 1,
            tripId = "trip",
            baselineStartDate = LocalDate.parse("2026-10-01"),
            baselineDayIds = listOf("day-1", "day-2"),
            targetEndDate = LocalDate.parse("2026-10-04"),
        )
        val impact = DateRangeChangeImpact(
            request = request,
            retainedDayIds = listOf("day-1", "day-2"),
            deletedDayIds = emptyList(),
            deletedItineraryItems = 0,
            deletedRouteLegs = 0,
            retainedSavedPlaces = 1,
        )
        return TripSettingsUiState(
            tripId = "trip",
            name = "Kyoto",
            days = listOf(DayUi("day-1", "2026-10-01"), DayUi("day-2", "2026-10-02")),
            dateRange = DateRangeChangeUiState(
                startDate = LocalDate.parse("2026-10-01"),
                baselineEndDate = LocalDate.parse("2026-10-02"),
                endDate = LocalDate.parse("2026-10-04"),
                isDirty = true,
                phase = phaseFactory(request, impact),
            ),
        )
    }

    private fun datedState(
        days: List<DayUi> = listOf(
            DayUi("day-1", "2026-10-01"),
            DayUi("day-2", "2026-10-02"),
            DayUi("day-3", "2026-10-03"),
        ),
        phaseFactory: (DateRangeChangeRequest, DateRangeChangeImpact) -> DateRangeChangePhase =
            { request, impact -> DateRangeChangePhase.AwaitingConfirmation(request, impact) },
        error: String? = null,
    ): TripSettingsUiState {
        val request = DateRangeChangeRequest(
            generation = 1,
            tripId = "trip",
            baselineStartDate = LocalDate.parse("2026-10-01"),
            baselineDayIds = listOf("day-1", "day-2", "day-3"),
            targetEndDate = LocalDate.parse("2026-10-01"),
        )
        val impact = DateRangeChangeImpact(
            request = request,
            retainedDayIds = listOf("day-1"),
            deletedDayIds = listOf("day-2", "day-3"),
            deletedItineraryItems = 4,
            deletedRouteLegs = 2,
            retainedSavedPlaces = 7,
        )
        return TripSettingsUiState(
            tripId = "trip",
            name = "Kyoto",
            travelMode = TravelMode.FLEXIBLE,
            days = days,
            dateRange = DateRangeChangeUiState(
                startDate = LocalDate.parse("2026-10-01"),
                baselineEndDate = LocalDate.parse("2026-10-03"),
                endDate = LocalDate.parse("2026-10-01"),
                isDirty = true,
                phase = phaseFactory(request, impact),
                error = error,
            ),
        )
    }
}
