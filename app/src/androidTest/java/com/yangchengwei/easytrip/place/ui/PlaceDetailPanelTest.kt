package com.yangchengwei.easytrip.place.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performTextInput
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.espresso.Espresso.pressBack
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.domain.PlaceTag
import com.yangchengwei.easytrip.place.domain.SavedPlace
import com.yangchengwei.easytrip.workspace.PlaceScheduleDayUi
import com.yangchengwei.easytrip.workspace.PlaceScheduleSummaryUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PlaceDetailPanelTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun searchUnsavedShowsCollectionButNoEditDeleteOrAdd() {
        setContent(source = PlaceDetailSource.Search, savedPlace = null)

        compose.onNodeWithText("收藏").assertIsDisplayed()
        compose.onAllNodesWithText("编辑").assertCountEquals(0)
        compose.onAllNodesWithText("删除").assertCountEquals(0)
        compose.onAllNodesWithText("加入行程").assertCountEquals(0)
    }

    @Test fun searchSavedShowsCollectionAndEditButNoAdd() {
        setContent(source = PlaceDetailSource.Search, savedPlace = savedPlace())

        compose.onNodeWithText("取消收藏").assertIsDisplayed()
        compose.onNodeWithText("编辑").assertIsDisplayed()
        compose.onAllNodesWithText("加入行程").assertCountEquals(0)
    }

    @Test fun placePoolScheduledPlaceShowsDailyScheduleFilledBookmarkWithoutDuplicateAdd() {
        setContent(
            source = PlaceDetailSource.PlacePool,
            savedPlace = savedPlace(),
            schedule = PlaceScheduleSummaryUi(
                isKnown = true,
                totalOccurrences = 3,
                days = listOf(PlaceScheduleDayUi("day-1", 0, 2), PlaceScheduleDayUi("day-3", 2, 1)),
            ),
        )

        compose.onNodeWithText("已加入行程").assertIsDisplayed()
        compose.onNodeWithText("第 1 天 · 2 次").assertIsDisplayed()
        compose.onNodeWithText("第 3 天 · 1 次").assertIsDisplayed()
        compose.onNodeWithTag("place-detail-bookmark-filled").assertIsDisplayed()
        compose.onAllNodesWithText("加入行程").assertCountEquals(0)
    }

    @Test fun placePoolOnlyCollectedPlaceShowsHollowBookmarkWithoutScheduleBlockAndStartsAdd() {
        val actions = mutableListOf<PlaceDetailPanelAction>()
        setContent(
            source = PlaceDetailSource.PlacePool,
            savedPlace = savedPlace(),
            schedule = PlaceScheduleSummaryUi(isKnown = true),
            onAction = actions::add,
        )

        compose.onAllNodesWithText("已加入行程").assertCountEquals(0)
        compose.onNodeWithTag("place-detail-bookmark-outline").assertIsDisplayed()
        compose.onNodeWithText("加入行程").performClick()

        compose.runOnIdle { assertEquals(listOf(PlaceDetailPanelAction.StartAddToItinerary), actions) }
    }

    @Test fun placePoolUnknownScheduleExplainsThatArrangementIsUnavailableWithoutAdd() {
        setContent(
            source = PlaceDetailSource.PlacePool,
            savedPlace = savedPlace(),
            schedule = PlaceScheduleSummaryUi(isKnown = false),
        )

        compose.onNodeWithText("行程安排暂不可用").assertIsDisplayed()
        compose.onAllNodesWithText("加入行程").assertCountEquals(0)
    }

    @Test fun placePoolKnownUnscheduledPlaceShowsHollowBookmarkAndStartsAdd() {
        val actions = mutableListOf<PlaceDetailPanelAction>()
        setContent(
            source = PlaceDetailSource.PlacePool,
            savedPlace = savedPlace(),
            schedule = PlaceScheduleSummaryUi(isKnown = true),
            onAction = actions::add,
        )

        compose.onNodeWithTag("place-detail-bookmark-outline").assertIsDisplayed()
        compose.onNodeWithText("加入行程").performClick()

        compose.runOnIdle { assertEquals(listOf(PlaceDetailPanelAction.StartAddToItinerary), actions) }
    }

    @Test fun placePoolRowUsesTwoLineAddressAndFixedActionRailAtNarrowWidth() {
        val place = savedPlace().copy(
            name = "这是一个非常长的地点名称用于验证最多两行",
            address = "这是一条非常长的地址信息用于验证最多两行并且不挤压右侧操作",
            note = "安排状态与备注应留在底部辅助信息区域",
        )
        compose.setContent {
            EasyTripTheme {
                androidx.compose.foundation.layout.Box(Modifier.requiredWidth(280.dp)) {
                    SavedPlaceRow(
                        place = SavedPlaceRowUi(place, 0, false),
                        onQuickAdd = {},
                        onOpenDetail = {},
                        onEdit = {},
                        onDelete = {},
                    )
                }
            }
        }

        compose.onNodeWithTag("quick-add-place-${place.id}").assertHeightIsEqualTo(28.dp)
        compose.onNodeWithTag("more-place-${place.id}").assertHeightIsAtLeast(48.dp)
        compose.onNodeWithTag("saved-place-${place.id}").assertIsDisplayed()
    }

    @Test fun placePoolScheduledPlaceDoesNotExposeDuplicateAddAction() {
        setContent(
            source = PlaceDetailSource.PlacePool,
            savedPlace = savedPlace(),
            schedule = PlaceScheduleSummaryUi(isKnown = true, totalOccurrences = 1),
        )

        compose.onAllNodesWithText("加入行程").assertCountEquals(0)
    }

    @Test fun readOnlyShowsAddressNoteAndTagFallbacks() {
        setContent(candidate = candidate(address = ""), source = PlaceDetailSource.Search)

        compose.onNodeWithText("地址暂不可用").assertIsDisplayed()
        compose.onNodeWithText("暂无备注").assertIsDisplayed()
        compose.onNodeWithText("暂无标签").assertIsDisplayed()
    }

    @Test fun savingDisablesInputsSaveCancelAndDismiss() {
        setContent(
            source = PlaceDetailSource.PlacePool,
            savedPlace = savedPlace(),
            editState = PlaceDetailEditState("saved-1", "备注", setOf("自然"), isSaving = true),
        )

        compose.onNodeWithTag("place-detail-note-input").assertIsNotEnabled()
        compose.onNodeWithTag("place-detail-tags-input").assertIsNotEnabled()
        compose.onNodeWithText("保存中").assertIsNotEnabled()
        compose.onNodeWithText("取消").assertIsNotEnabled()
        compose.onNodeWithTag("place-detail-dismiss").assertIsNotEnabled()
    }

    @Test fun availablePresetTagsCanBeAddedAndSelectedTagsRemainRemovableAtLimit() {
        val actions = mutableListOf<PlaceDetailPanelAction>()
        setContent(
            source = PlaceDetailSource.PlacePool,
            savedPlace = savedPlace(),
            editState = PlaceDetailEditState("saved-1", "", (1..8).mapTo(mutableSetOf()) { "已选$it" }),
            availableTagNames = listOf("已选1", "咖啡"),
            onAction = actions::add,
        )

        compose.onNodeWithTag("place-detail-preset-tag-咖啡").assertIsNotEnabled()
        compose.onNodeWithTag("place-detail-preset-tag-已选1").performClick()

        compose.runOnIdle { assertEquals(PlaceDetailPanelAction.RemoveTag("已选1"), actions.single()) }
    }

    @Test fun availableUnselectedPresetTagDispatchesAddSelection() {
        val actions = mutableListOf<PlaceDetailPanelAction>()
        setContent(
            source = PlaceDetailSource.Search,
            savedPlace = savedPlace(),
            editState = PlaceDetailEditState("saved-1", "", setOf("自然")),
            availableTagNames = listOf("自然", "咖啡"),
            onAction = actions::add,
        )

        compose.onNodeWithTag("place-detail-preset-tag-咖啡").performClick()

        compose.runOnIdle { assertEquals(PlaceDetailPanelAction.AddPresetTag("咖啡"), actions.single()) }
    }

    @Test fun typingTagInputPreservesEveryCharacterAndComma() {
        val input = mutableStateOf("")
        compose.setContent {
            EasyTripTheme {
                PlaceDetailPanel(
                    candidate = candidate(),
                    savedPlace = savedPlace(),
                    editState = PlaceDetailEditState("saved-1", "", emptySet(), input.value),
                    source = PlaceDetailSource.PlacePool,
                    collectionBusy = false,
                    collectionError = null,
                    onAction = { action ->
                        if (action is PlaceDetailPanelAction.NewTagInputChanged) input.value = action.value
                    },
                )
            }
        }

        compose.onNodeWithTag("place-detail-tags-input").performTextInput("自然,咖啡")

        compose.runOnIdle { assertEquals("自然,咖啡", input.value) }
        compose.onNodeWithTag("place-detail-tags-input").assertTextEquals("自然,咖啡", "新标签")
    }

    @Test fun compatibilityWrapperForwardsDismissAndCancel() {
        val actions = mutableListOf<String>()
        compose.setContent {
            EasyTripTheme {
                PlaceDetailContent(
                    place = savedPlace(),
                    draft = PlaceDetailDraft("备注", emptySet(), "saved-1"),
                    saving = false,
                    error = null,
                    source = PlaceDetailSource.PlacePool,
                    onNoteChange = {},
                    onTagsChange = {},
                    onToggleCollection = {},
                    onSave = {},
                    onDismiss = { actions += "dismiss" },
                    onDelete = {},
                )
            }
        }

        compose.onNodeWithTag("place-detail-dismiss").performClick()
        compose.onNodeWithText("取消").performClick()

        compose.runOnIdle { assertEquals(listOf("dismiss", "dismiss"), actions) }
    }

    @Test fun compatibilityWrapperHidesDeleteWhileEditingFromPlacePool() {
        compose.setContent {
            EasyTripTheme {
                PlaceDetailContent(
                    place = savedPlace(),
                    draft = PlaceDetailDraft("备注", emptySet(), "saved-1"),
                    saving = false,
                    error = null,
                    source = PlaceDetailSource.PlacePool,
                    onNoteChange = {},
                    onTagsChange = {},
                    onToggleCollection = {},
                    onSave = {},
                    onDismiss = {},
                    onDelete = {},
                )
            }
        }

        compose.onNodeWithTag("place-detail-cancel").assertIsDisplayed().assertHasClickAction()
        compose.onNodeWithTag("place-detail-delete").assertDoesNotExist()
        compose.onAllNodesWithText("取消收藏").assertCountEquals(0)
    }

    @Test fun placePoolReadOnlyKeepsDeleteAction() {
        setContent(source = PlaceDetailSource.PlacePool, savedPlace = savedPlace())

        compose.onNodeWithText("删除").assertIsDisplayed().assertHasClickAction()
    }

    @Test fun savingHostDialogIgnoresSystemBack() {
        val dismissed = mutableStateOf(false)
        compose.setContent {
            EasyTripTheme {
                PlaceDetailDialog(
                    place = savedPlace(),
                    draft = PlaceDetailDraft("备注", emptySet(), "saved-1"),
                    saving = true,
                    error = null,
                    source = PlaceDetailSource.PlacePool,
                    onNoteChange = {},
                    onTagsChange = {},
                    onDismiss = { dismissed.value = true },
                    onDelete = {},
                    onSave = {},
                )
            }
        }

        compose.onNodeWithText("保存中").assertIsDisplayed()
        pressBack()

        compose.runOnIdle { assertTrue(!dismissed.value) }
        compose.onNodeWithText("保存中").assertIsDisplayed()
    }

    @Test fun panelTitleHasHeadingSemantics() {
        setContent(source = PlaceDetailSource.Search)

        compose.onNodeWithTag("place-detail-title")
            .assertTextEquals("故宫博物院")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }

    @Test fun placePoolEditHidesDeleteButKeepsCancelAndSaveReachableAndCenteredAtNarrowWidthWithTwoTimesFontScale() {
        val state = PlacePoolUiState(
            editing = savedPlace(),
            detailDraft = PlaceDetailDraft(
                note = "很长的备注内容用于验证正文能够滚动到底部操作区",
                tags = (1..8).mapTo(mutableSetOf()) { "标签$it" },
                placeId = "saved-1",
            ),
            tags = (1..8).map { PlaceTag("tag-$it", "标签$it") },
        )
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                EasyTripTheme {
                    Box(Modifier.requiredWidth(280.dp).height(400.dp)) {
                        WorkspacePlaceDetailSheet(
                            state = state,
                            schedule = PlaceScheduleSummaryUi(isKnown = true),
                            onAction = {},
                        )
                    }
                }
            }
        }

        compose.onNodeWithTag("place-detail-delete").assertDoesNotExist()

        val container = compose.onNodeWithTag("place-detail-bottom-sheet").getUnclippedBoundsInRoot()
        listOf(
            "place-detail-cancel" to "取消",
            "place-detail-save" to "保存",
        ).forEach { (tag, label) ->
            val action = compose.onNodeWithTag(tag)
                .performScrollTo()
                .assertIsDisplayed()
                .assertHasClickAction()
                .assertHeightIsAtLeast(48.dp)
                .getUnclippedBoundsInRoot()
            val text = compose.onNodeWithText(label, useUnmergedTree = true).getUnclippedBoundsInRoot()
            assertTrue("action=$action container=$container", action.left >= container.left && action.right <= container.right)
            assertTrue("action=$action container=$container", action.top >= container.top && action.bottom <= container.bottom)
            assertTrue("label=$text action=$action", kotlin.math.abs(((text.left + text.right) / 2 - (action.left + action.right) / 2).value) < 0.5f)
            assertTrue("label=$text action=$action", kotlin.math.abs(((text.top + text.bottom) / 2 - (action.top + action.bottom) / 2).value) < 0.5f)
        }
    }

    @Test fun newTagInputKeepsUsableWidthAndVisibleTextWithoutImeAtNormalFontScale() {
        compose.setContent {
            EasyTripTheme {
                Box(Modifier.requiredWidth(220.dp).height(400.dp)) {
                    PlaceDetailPanel(
                        candidate = candidate(),
                        savedPlace = savedPlace(),
                        editState = PlaceDetailEditState("saved-1", "", emptySet(), "自然"),
                        source = PlaceDetailSource.PlacePool,
                        collectionBusy = false,
                        collectionError = null,
                        onAction = {},
                    )
                }
            }
        }

        val inputBounds = compose.onNodeWithTag("place-detail-tags-input")
            .assertTextEquals("自然", "新标签")
            .assertWidthIsAtLeast(100.dp)
            .getUnclippedBoundsInRoot()
        val inputTextBounds = compose.onNodeWithText("自然", useUnmergedTree = true)
            .assertIsDisplayed()
            .getUnclippedBoundsInRoot()
        val addBounds = compose.onNodeWithTag("place-detail-add-tag")
            .assertIsDisplayed()
            .assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
            .getUnclippedBoundsInRoot()

        assertTrue("input=$inputBounds add=$addBounds", inputBounds.right <= addBounds.left)
        assertTrue("input=$inputBounds add=$addBounds", inputBounds.right - inputBounds.left > addBounds.right - addBounds.left)
        assertTrue("input=$inputBounds text=$inputTextBounds", inputTextBounds.left >= inputBounds.left && inputTextBounds.right <= inputBounds.right)
    }

    @Test fun newTagInputAndAddButtonStayVerticallyCenteredAndReachableWithImeAtNarrowTwoTimesFontScale() {
        val input = mutableStateOf("")
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                EasyTripTheme {
                    Box(Modifier.requiredWidth(280.dp).height(400.dp)) {
                        PlaceDetailPanel(
                            candidate = candidate(),
                            savedPlace = savedPlace(),
                            editState = PlaceDetailEditState("saved-1", "", emptySet(), input.value),
                            source = PlaceDetailSource.PlacePool,
                            collectionBusy = false,
                            collectionError = null,
                            onAction = { action ->
                                if (action is PlaceDetailPanelAction.NewTagInputChanged) input.value = action.value
                            },
                        )
                    }
                }
            }
        }

        compose.onNodeWithTag("place-detail-tags-input").performScrollTo().performClick().performTextInput("自然")
        compose.waitUntil(5_000) {
            ViewCompat.getRootWindowInsets(compose.activity.window.decorView)
                ?.isVisible(WindowInsetsCompat.Type.ime()) == true
        }
        compose.onNodeWithTag("place-detail-tags-input").performScrollTo()
        compose.waitUntil(5_000) {
            runCatching {
                compose.onNodeWithTag("place-detail-tags-input").getUnclippedBoundsInRoot()
                compose.onNodeWithTag("place-detail-add-tag").getUnclippedBoundsInRoot()
                true
            }.getOrDefault(false)
        }

        val inputBounds = compose.onNodeWithTag("place-detail-tags-input")
            .assertWidthIsAtLeast(100.dp)
            .getUnclippedBoundsInRoot()
        val addBounds = compose.onNodeWithTag("place-detail-add-tag")
            .assertIsDisplayed()
            .assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
            .getUnclippedBoundsInRoot()
        val addLabelBounds = compose.onNodeWithText("添加", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val container = compose.onNodeWithTag("place-detail-scroll-content").getUnclippedBoundsInRoot()
        assertTrue("input=$inputBounds add=$addBounds", inputBounds.right <= addBounds.left)
        assertTrue("input=$inputBounds add=$addBounds", kotlin.math.abs(((inputBounds.top + inputBounds.bottom) / 2 - (addBounds.top + addBounds.bottom) / 2).value) < 0.5f)
        assertTrue("add=$addBounds label=$addLabelBounds", addLabelBounds.left >= addBounds.left && addLabelBounds.right <= addBounds.right)
        assertTrue("add=$addBounds container=$container", addBounds.left >= container.left && addBounds.right <= container.right)
        assertTrue("add=$addBounds container=$container", addBounds.top >= container.top && addBounds.bottom <= container.bottom)
    }

    @Test fun longestValidNewTagKeepsRemoveVisibleAndTouchableAtNarrowLargeText() {
        val tag = "这是十二个中文字的标签名"
        val actions = mutableListOf<PlaceDetailPanelAction>()
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                EasyTripTheme {
                    Box(Modifier.requiredWidth(280.dp).height(400.dp)) {
                        PlaceDetailPanel(
                            candidate = candidate(),
                            savedPlace = savedPlace(),
                            editState = PlaceDetailEditState("saved-1", "", setOf(tag)),
                            source = PlaceDetailSource.PlacePool,
                            collectionBusy = false,
                            collectionError = null,
                            onAction = actions::add,
                        )
                    }
                }
            }
        }

        val remove = compose.onNodeWithText("移除").performScrollTo().assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp)
        val removeBounds = remove.getUnclippedBoundsInRoot()
        val labelBounds = compose.onNodeWithText(tag, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val container = compose.onNodeWithTag("place-detail-scroll-content").getUnclippedBoundsInRoot()
        assertTrue("tag=$labelBounds remove=$removeBounds", labelBounds.right <= removeBounds.left)
        assertTrue("remove=$removeBounds container=$container", removeBounds.right <= container.right)
        remove.performTouchInput { click() }
        compose.runOnIdle { assertEquals(listOf(PlaceDetailPanelAction.RemoveTag(tag)), actions) }
    }

    private fun setContent(
        candidate: PlaceCandidate = candidate(),
        savedPlace: SavedPlace? = null,
        source: PlaceDetailSource,
        editState: PlaceDetailEditState? = null,
        schedule: PlaceScheduleSummaryUi = PlaceScheduleSummaryUi(isKnown = false),
        availableTagNames: List<String> = emptyList(),
        onAction: (PlaceDetailPanelAction) -> Unit = {},
    ) {
        compose.setContent {
            EasyTripTheme {
                PlaceDetailPanel(
                    candidate = candidate,
                    savedPlace = savedPlace,
                    editState = editState,
                    source = source,
                    schedule = schedule,
                    collectionBusy = false,
                    collectionError = null,
                    availableTagNames = availableTagNames,
                    onAction = onAction,
                )
            }
        }
    }

    private fun candidate(address: String = "北京市东城区") =
        PlaceCandidate("poi-1", "故宫博物院", address, GeoPoint(39.9, 116.4), "010")

    private fun savedPlace() = SavedPlace(
        "saved-1",
        "trip",
        "poi-1",
        "故宫博物院",
        "北京市东城区",
        GeoPoint(39.9, 116.4),
        "",
        emptyList<PlaceTag>(),
    )
}
