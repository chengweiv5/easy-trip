package com.yangchengwei.easytrip.trip.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsProperties.IsDialog
import androidx.compose.ui.semantics.SemanticsProperties.PaneTitle
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.test.espresso.Espresso.pressBack
import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.trip.domain.DateRangeChangeImpact
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
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

    @Test fun dateEditorUsesBottomSheetWithScrimAndNoDialogSemantics() {
        compose.setContent {
            Box(Modifier.width(390.dp).height(900.dp)) {
                content(state = datedState().copy(dateRange = datedState().dateRange.copy(phase = DateRangeChangePhase.Idle)))
            }
        }

        compose.onNodeWithTag("settings-date-row").performClick()

        compose.onNodeWithTag("settings-date-scrim").assertIsDisplayed()
        val sheet = compose.onNodeWithTag("settings-date-bottom-sheet").assertIsDisplayed().getUnclippedBoundsInRoot()
        val root = compose.onRoot().getUnclippedBoundsInRoot()
        val expectedHeight = minOf(660f, (root.bottom.value - root.top.value) * (660f / 782f))
        assertEquals(expectedHeight, sheet.bottom.value - sheet.top.value, .5f)
        assertEquals(root.bottom.value, sheet.bottom.value, .5f)
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(IsDialog)).assertCountEquals(0)
    }

    @Test fun dateEditorIsAnAccessibilityPaneAndHidesUnderlyingSettingsSemantics() {
        compose.setContent {
            content(state = datedState().copy(dateRange = datedState().dateRange.copy(phase = DateRangeChangePhase.Idle)))
        }

        compose.onNodeWithTag("settings-date-row").performClick()

        compose.onNodeWithTag("settings-date-bottom-sheet").assert(
            SemanticsMatcher.expectValue(PaneTitle, "修改出行日期"),
        )
        compose.onNodeWithTag("settings-back").assertDoesNotExist()
        compose.onNodeWithTag("settings-delete-trip").assertDoesNotExist()
        compose.onNodeWithTag("settings-date-row").assertDoesNotExist()
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(IsDialog)).assertCountEquals(0)
        compose.onAllNodes(
            SemanticsMatcher("anonymous clickable") { node ->
                node.config.contains(SemanticsActions.OnClick) &&
                    !node.config.contains(SemanticsProperties.ContentDescription) &&
                    !node.config.contains(SemanticsProperties.Text)
            },
        ).assertCountEquals(0)
    }

    @Test fun dateEditorCancelRestoresBaselineAfterValidAndInvalidLocalEdits() {
        val drafts = mutableListOf<LocalDate?>()
        var submissions = 0
        compose.setContent {
            content(
                state = datedState().copy(dateRange = datedState().dateRange.copy(phase = DateRangeChangePhase.Idle)),
                onDateEndDraft = drafts::add,
                onSubmitDateRange = { submissions++ },
            )
        }

        fun openAndAssertBaseline() {
            compose.onNodeWithTag("settings-date-row").performClick()
            compose.onNodeWithTag("settings-end-date").assertTextEquals("2026-10-03")
            compose.onNodeWithTag("settings-date-input-error").assertDoesNotExist()
        }

        openAndAssertBaseline()
        compose.onNodeWithTag("settings-end-date").performTextReplacement("2026-10-05")
        compose.onNodeWithTag("settings-date-cancel").performClick()
        openAndAssertBaseline()
        compose.onNodeWithTag("settings-end-date").performTextReplacement("invalid")
        compose.onNodeWithTag("settings-apply-date-range").performClick()
        compose.onNodeWithTag("settings-date-input-error").assertIsDisplayed()
        compose.onNodeWithTag("settings-date-cancel").performClick()
        openAndAssertBaseline()

        assertEquals(emptyList<LocalDate?>(), drafts)
        assertEquals(0, submissions)
    }

    @Test fun dateEditorBackAndScrimRestoreBaselineWithoutDraftOrSubmissionCallbacks() {
        val drafts = mutableListOf<LocalDate?>()
        var submissions = 0
        compose.setContent {
            content(
                state = datedState().copy(dateRange = datedState().dateRange.copy(phase = DateRangeChangePhase.Idle)),
                onDateEndDraft = drafts::add,
                onSubmitDateRange = { submissions++ },
            )
        }

        compose.onNodeWithTag("settings-date-row").performClick()
        compose.onNodeWithTag("settings-end-date").performTextReplacement("2026-10-05")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        pressBack()
        compose.waitForIdle()
        compose.onNodeWithTag("settings-date-row", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("settings-end-date").assertTextEquals("2026-10-03")
        compose.onNodeWithTag("settings-end-date").performTextReplacement("invalid")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.onRoot().performTouchInput { click(Offset(1f, 1f)) }
        compose.onNodeWithTag("settings-date-row", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("settings-end-date").assertTextEquals("2026-10-03")
        compose.onNodeWithTag("settings-date-input-error").assertDoesNotExist()

        assertEquals(emptyList<LocalDate?>(), drafts)
        assertEquals(0, submissions)
    }

    @Test fun dateEditorApplyDispatchesDraftThenSubmitsExactlyOnce() {
        val callbacks = mutableListOf<String>()
        compose.setContent {
            content(
                state = datedState().copy(dateRange = datedState().dateRange.copy(phase = DateRangeChangePhase.Idle)),
                onDateEndDraft = { callbacks += "draft:$it" },
                onSubmitDateRange = { callbacks += "submit" },
            )
        }

        compose.onNodeWithTag("settings-date-row").performClick()
        compose.onNodeWithTag("settings-end-date").performTextReplacement("2026-10-05")
        compose.onNodeWithTag("settings-apply-date-range").performClick()

        assertEquals(listOf("draft:2026-10-05", "submit"), callbacks)
    }

    @Test fun dateEditorKeepsActionsInsideTinyHeightAtTwoTimesFontScale() {
        compose.setContent {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(density = 1f, fontScale = 2f),
            ) {
                Box(Modifier.width(280.dp).height(220.dp).testTag("date-editor-container")) {
                    content(state = datedState().copy(dateRange = datedState().dateRange.copy(phase = DateRangeChangePhase.Idle)))
                }
            }
        }

        compose.onNodeWithTag("settings-date-row").performClick()

        val container = compose.onNodeWithTag("date-editor-container").getUnclippedBoundsInRoot()
        val sheet = compose.onNodeWithTag("settings-date-bottom-sheet").assertIsDisplayed().getUnclippedBoundsInRoot()
        val cancel = compose.onNodeWithTag("settings-date-cancel").assertIsDisplayed().assertHeightIsAtLeast(48.dp).getUnclippedBoundsInRoot()
        val confirm = compose.onNodeWithTag("settings-apply-date-range").assertIsDisplayed().assertHeightIsAtLeast(48.dp).getUnclippedBoundsInRoot()
        assertEquals(container.top.value, sheet.top.value, .5f)
        assertEquals(container.bottom.value, sheet.bottom.value, .5f)
        check(cancel.top.value >= container.top.value && cancel.bottom.value <= container.bottom.value)
        check(confirm.top.value >= container.top.value && confirm.bottom.value <= container.bottom.value)
    }

    @Test fun dateEditorActionsStayAboveInjectedBottomSafeInset() {
        val bottomInset = 36.dp
        compose.setContent {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(density = 1f, fontScale = 2f),
            ) {
                Box(Modifier.width(280.dp).height(220.dp).testTag("date-editor-safe-container")) {
                    content(
                        state = datedState().copy(dateRange = datedState().dateRange.copy(phase = DateRangeChangePhase.Idle)),
                        dateEditorBottomInset = { WindowInsets(bottom = bottomInset) },
                    )
                }
            }
        }

        compose.onNodeWithTag("settings-date-row").performClick()

        val container = compose.onNodeWithTag("date-editor-safe-container").getUnclippedBoundsInRoot()
        val cancel = compose.onNodeWithTag("settings-date-cancel")
            .assertHeightIsAtLeast(48.dp)
            .assertIsDisplayed()
            .getUnclippedBoundsInRoot()
        val confirm = compose.onNodeWithTag("settings-apply-date-range")
            .assertHeightIsAtLeast(48.dp)
            .assertIsDisplayed()
            .getUnclippedBoundsInRoot()
        check(cancel.bottom <= container.bottom - bottomInset)
        check(confirm.bottom <= container.bottom - bottomInset)
    }

    @Test fun dateEditorSheetHeightNeverExceedsAvailableHeight() {
        val method = Class.forName("com.yangchengwei.easytrip.trip.ui.TripSettingsContentKt")
            .declaredMethods
            .single { it.name.startsWith("dateEditorSheetHeight-") }
            .apply { isAccessible = true }

        assertEquals(220f, method.invoke(null, 220f) as Float, .001f)
        assertEquals(240f, method.invoke(null, 284f) as Float, .001f)
        assertEquals(660f, method.invoke(null, 900f) as Float, .001f)
    }

    @Test fun dateEditorBackClosesSheetWithoutLeavingSettingsOrSubmitting() {
        var backs = 0
        var submissions = 0
        compose.setContent {
            TripSettingsContent(
                state = datedState().copy(dateRange = datedState().dateRange.copy(phase = DateRangeChangePhase.Idle)),
                onBack = { backs++ }, onRename = {}, onTravelMode = {}, onDateEndDraft = {},
                onSubmitDateRange = { submissions++ }, onCancelDateRange = {}, onConfirmDateRange = {},
                onRetryDateRangeSync = {}, onRequestDeleteDay = {}, onRetryDeleteDay = {},
                onCancelDeleteDay = {}, onConfirmDeleteDay = {},
            )
        }

        compose.onNodeWithTag("settings-date-row").performClick()
        pressBack()
        compose.waitForIdle()

        compose.onNodeWithTag("settings-date-bottom-sheet").assertDoesNotExist()
        compose.onNodeWithTag("settings-header").assertIsDisplayed()
        assertEquals(0, backs)
        assertEquals(0, submissions)
    }

    @Test fun dateEditorContentTapDoesNotDismissOrSubmit() {
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

        compose.onNodeWithTag("settings-date-row").performClick()
        compose.onNodeWithTag("settings-date-sheet-handle").performClick()

        compose.onNodeWithTag("settings-date-bottom-sheet").assertIsDisplayed()
        assertEquals(0, submissions)
    }

    @Test fun invalidDateKeepsBottomSheetOpenAndDoesNotSubmit() {
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

        compose.onNodeWithTag("settings-date-row").performClick()
        compose.onNodeWithTag("settings-end-date").performTextReplacement("invalid")
        compose.onNodeWithTag("settings-apply-date-range").performClick()

        compose.onNodeWithTag("settings-date-bottom-sheet").assertIsDisplayed()
        compose.onNodeWithTag("settings-date-input-error").assertIsDisplayed()
        assertEquals(0, submissions)
    }

    @Test fun dateEditorCancelDoesNotSubmitAndClosesSheet() {
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

        compose.onNodeWithTag("settings-date-row").performClick()
        compose.onNodeWithTag("settings-date-cancel").performClick()

        compose.onNodeWithTag("settings-date-bottom-sheet").assertDoesNotExist()
        assertEquals(0, submissions)
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

    @Test fun deleteTripIsDisabledUntilAuthoritativeSettingsObservationArrives() {
        var requests = 0
        compose.setContent {
            content(
                state = TripSettingsUiState(tripId = "trip", name = "暂未加载", hasAuthoritativeTrip = false),
                onRequestTripDeletion = { requests++ },
            )
        }

        compose.onNodeWithTag("settings-delete-trip").assertIsNotEnabled().performClick()
        assertEquals(0, requests)
    }

    @Test fun dangerZoneDeletesThisTripFromSettingsWithAccessibleAction() {
        var deletionRequests = 0
        compose.setContent {
            content(
                state = datedState().copy(
                    hasAuthoritativeTrip = true,
                    dateRange = datedState().dateRange.copy(phase = DateRangeChangePhase.Idle),
                ),
                onRequestTripDeletion = { deletionRequests++ },
            )
        }

        compose.onNodeWithText("删除旅行请返回旅行列表操作").assertDoesNotExist()
        compose.onNodeWithTag("settings-delete-trip").assertIsDisplayed().assertHeightIsAtLeast(48.dp).performClick()
        assertEquals(1, deletionRequests)
    }

    @Test fun tripDeletionStatesExposeOneRecoveryPathAtATime() {
        var retryImpact = 0
        var confirmations = 0
        var syncRetries = 0
        val confirmation = TripDeleteImpact(3, 8, 0, 8, 5).toConfirmationUiModel("Kyoto")
        var state by androidx.compose.runtime.mutableStateOf(
            TripSettingsUiState(
                tripId = "trip",
                name = "Kyoto",
                tripDeletion = TripDeletionUiState.LoadingImpact("trip", "Kyoto"),
            ),
        )
        compose.setContent {
            content(
                state = state,
                onRetryTripDeletionImpact = { retryImpact++ },
                onConfirmTripDeletion = { confirmations++ },
                onRetryTripDeletionSync = { syncRetries++ },
            )
        }

        compose.onNodeWithText("正在查询删除影响…").assertIsDisplayed()
        compose.onNodeWithTag("trip-delete-impact-loading").assertIsDisplayed()
        compose.onNodeWithText("确认删除旅行").assertDoesNotExist()

        compose.runOnIdle {
            state = state.copy(
                tripDeletion = TripDeletionUiState.ImpactFailure("trip", "Kyoto", "无法加载删除影响，请重试"),
            )
        }
        compose.onNodeWithText("未删除旅行").assertIsDisplayed()
        compose.onNodeWithTag("trip-delete-impact-retry").assertIsDisplayed().performClick()
        assertEquals(1, retryImpact)
        compose.onNodeWithText("确认删除旅行").assertDoesNotExist()

        compose.runOnIdle {
            state = state.copy(
                tripDeletion = TripDeletionUiState.Ready("trip", "Kyoto", confirmation),
            )
        }
        compose.onNodeWithText("删除Kyoto？").assertIsDisplayed()
        compose.onNodeWithText("确认删除旅行").assertIsDisplayed().performClick()
        assertEquals(1, confirmations)

        compose.runOnIdle {
            state = state.copy(
                tripDeletion = TripDeletionUiState.Ready("trip", "Kyoto", confirmation, isDeleting = true),
            )
        }
        compose.onNodeWithText("处理中…").assertIsDisplayed()
        compose.onNodeWithTag("confirmation-confirm").assertIsNotEnabled()
        compose.onNodeWithTag("confirmation-dismiss").assertIsNotEnabled()

        compose.runOnIdle {
            state = state.copy(
                tripDeletion = TripDeletionUiState.Ready(
                    "trip",
                    "Kyoto",
                    confirmation,
                    errorMessage = "删除成功，但同步确认失败，请重新同步",
                    confirmationSyncFailed = true,
                ),
            )
        }
        compose.onNodeWithText("删除成功，但同步确认失败，请重新同步").assertIsDisplayed()
        compose.onNodeWithText("重新同步").assertIsDisplayed().performClick()
        assertEquals(1, syncRetries)
    }

    @Test fun loadingImpactFailureAndReadyCancellationDispatchWithoutChangingTripContent() {
        var cancellations = 0
        val confirmation = TripDeleteImpact(3, 8, 0, 8, 5).toConfirmationUiModel("Kyoto")
        val initial = TripSettingsUiState(
            tripId = "trip",
            name = "Kyoto",
            days = listOf(DayUi("day-1", "2026-10-01")),
            tripDeletion = TripDeletionUiState.LoadingImpact("trip", "Kyoto"),
        )
        var state by androidx.compose.runtime.mutableStateOf(initial)
        compose.setContent {
            content(
                state = state,
                onCancelTripDeletion = {
                    cancellations++
                    state = state.copy(tripDeletion = TripDeletionUiState.Idle)
                },
            )
        }

        fun assertLoadingCannotCancel() {
            compose.onNodeWithTag("trip-delete-impact-cancel").assertIsNotEnabled().performClick()
            pressBack()
            compose.waitForIdle()
            compose.onAllNodes(isRoot())[1].performTouchInput { click(Offset(1f, 1f)) }
            compose.onNodeWithText("正在查询删除影响…").assertIsDisplayed()
            assertEquals(0, cancellations)
            assertEquals(TripDeletionUiState.LoadingImpact("trip", "Kyoto"), state.tripDeletion)
        }

        fun cancelAndAssertUnchanged() {
            compose.onNodeWithTag("trip-delete-impact-cancel").performClick()
            compose.onNodeWithTag("delete-day-day-1").assertIsDisplayed()
            assertEquals(listOf(DayUi("day-1", "2026-10-01")), state.days)
            assertEquals(TripDeletionUiState.Idle, state.tripDeletion)
        }

        assertLoadingCannotCancel()
        compose.runOnIdle {
            state = initial.copy(
                tripDeletion = TripDeletionUiState.ImpactFailure("trip", "Kyoto", "无法加载删除影响，请重试"),
            )
        }
        cancelAndAssertUnchanged()
        compose.runOnIdle {
            state = initial.copy(
                tripDeletion = TripDeletionUiState.Ready("trip", "Kyoto", confirmation),
            )
        }
        compose.onNodeWithTag("confirmation-dismiss").performClick()
        compose.onNodeWithTag("delete-day-day-1").assertIsDisplayed()
        assertEquals(listOf(DayUi("day-1", "2026-10-01")), state.days)
        assertEquals(TripDeletionUiState.Idle, state.tripDeletion)
        assertEquals(2, cancellations)
    }

    @Test fun syncFailureAllowsOnlyResyncAndBlocksDismissBackOutsideAndSettingsWrites() {
        var cancels = 0
        var resyncs = 0
        var backs = 0
        val confirmation = TripDeleteImpact(3, 8, 0, 8, 5).toConfirmationUiModel("Kyoto")
        val syncFailure = datedState().copy(
            dateRange = datedState().dateRange.copy(phase = DateRangeChangePhase.Idle),
            tripDeletion = TripDeletionUiState.Ready(
                tripId = "trip",
                tripName = "Kyoto",
                confirmation = confirmation,
                errorMessage = "删除成功，但同步确认失败，请重新同步",
                confirmationSyncFailed = true,
            ),
        )
        compose.setContent {
            content(
                state = syncFailure,
                onBack = { backs++ },
                onCancelTripDeletion = { cancels++ },
                onRetryTripDeletionSync = { resyncs++ },
            )
        }

        compose.onAllNodesWithTag("confirmation-dismiss").assertCountEquals(0)
        compose.onNodeWithTag("settings-back").assertIsNotEnabled().performClick()
        compose.onNodeWithTag("settings-rename").assertIsNotEnabled()
        compose.onNodeWithTag("settings-date-row").assertIsNotEnabled()
        compose.onNodeWithTag("delete-day-day-3").assertIsNotEnabled()
        compose.onNodeWithTag("settings-delete-trip").assertIsNotEnabled()
        pressBack()
        compose.waitForIdle()
        compose.onNodeWithText("删除成功，但同步确认失败，请重新同步").assertIsDisplayed()
        compose.onNodeWithText("重新同步").performClick()

        assertEquals(0, cancels)
        assertEquals(0, backs)
        assertEquals(1, resyncs)
    }

    @Test fun deletingTripBlocksBackAndOutsideDismiss() {
        var cancels = 0
        var backs = 0
        val confirmation = TripDeleteImpact(3, 8, 0, 8, 5).toConfirmationUiModel("Kyoto")
        compose.setContent {
            content(
                state = TripSettingsUiState(
                    tripId = "trip",
                    name = "Kyoto",
                    tripDeletion = TripDeletionUiState.Ready("trip", "Kyoto", confirmation, isDeleting = true),
                ),
                onBack = { backs++ },
                onCancelTripDeletion = { cancels++ },
            )
        }

        compose.onNodeWithTag("confirmation-dismiss").assertIsNotEnabled().performClick()
        pressBack()
        compose.waitForIdle()
        compose.onAllNodes(isRoot())[1].performTouchInput { click(Offset(1f, 1f)) }
        compose.onNodeWithText("处理中…").assertIsDisplayed()
        assertEquals(0, cancels)
        assertEquals(0, backs)
    }

    @Test fun tripDeletionEntryAndConfirmationRemainReachableAt280DpAndTwoTimesFontScale() {
        val tripName = "一段特别特别长而且需要完整换行展示的旅行名称"
        val confirmation = TripDeleteImpact(3, 8, 0, 8, 5).toConfirmationUiModel(tripName)
        var deleteRequests = 0
        var state by androidx.compose.runtime.mutableStateOf(
            TripSettingsUiState(tripId = "trip", name = tripName),
        )
        compose.setContent {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(density = 1f, fontScale = 2f),
            ) {
                androidx.compose.foundation.layout.Box(Modifier.width(280.dp).height(900.dp)) {
                    content(
                        state = state,
                        onRequestTripDeletion = {
                            deleteRequests++
                            state = state.copy(tripDeletion = TripDeletionUiState.Ready("trip", tripName, confirmation))
                        },
                    )
                }
            }
        }

        compose.onNodeWithTag("settings-delete-trip").performScrollTo().assertHeightIsAtLeast(48.dp).performClick()
        compose.onNodeWithTag("confirmation-confirm").performScrollTo().assertHeightIsAtLeast(48.dp).assertIsDisplayed()
        compose.onNodeWithTag("confirmation-dismiss").performScrollTo().assertHeightIsAtLeast(48.dp).assertIsDisplayed()
        assertEquals(1, deleteRequests)
    }

    @androidx.compose.runtime.Composable
    private fun content(
        state: TripSettingsUiState,
        onBack: () -> Unit = {},
        onDateEndDraft: (LocalDate?) -> Unit = {},
        onSubmitDateRange: () -> Unit = {},
        onCancelDateRange: () -> Unit = {},
        onRetryDateRangeSync: () -> Unit = {},
        onRetryDeleteDay: () -> Unit = {},
        onRetryTripObservation: () -> Unit = {},
        onRequestTripDeletion: () -> Unit = {},
        onRetryTripDeletionImpact: () -> Unit = {},
        onCancelTripDeletion: () -> Unit = {},
        onConfirmTripDeletion: () -> Unit = {},
        onRetryTripDeletionSync: () -> Unit = {},
        dateEditorBottomInset: @androidx.compose.runtime.Composable () -> WindowInsets = { WindowInsets(0) },
    ) = TripSettingsContent(
        state = state,
        onBack = onBack,
        onRename = {},
        onTravelMode = {},
        onDateEndDraft = onDateEndDraft,
        onSubmitDateRange = onSubmitDateRange,
        onCancelDateRange = onCancelDateRange,
        onConfirmDateRange = {},
        onRetryDateRangeSync = onRetryDateRangeSync,
        onRetryTripObservation = onRetryTripObservation,
        onRequestDeleteDay = {},
        onRetryDeleteDay = onRetryDeleteDay,
        onCancelDeleteDay = {},
        onConfirmDeleteDay = {},
        onRequestTripDeletion = onRequestTripDeletion,
        onRetryTripDeletionImpact = onRetryTripDeletionImpact,
        onCancelTripDeletion = onCancelTripDeletion,
        onConfirmTripDeletion = onConfirmTripDeletion,
        onRetryTripDeletionSync = onRetryTripDeletionSync,
        dateEditorBottomInset = dateEditorBottomInset,
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
