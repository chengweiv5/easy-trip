package com.yangchengwei.easytrip.place.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.runtime.mutableStateOf
import androidx.test.espresso.Espresso.pressBack
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.domain.PlaceTag
import com.yangchengwei.easytrip.place.domain.SavedPlace
import org.junit.Assert.assertEquals
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

    @Test fun placePoolShowsEditAndDeleteButNoCollectionOrAdd() {
        setContent(source = PlaceDetailSource.PlacePool, savedPlace = savedPlace())

        compose.onNodeWithText("编辑").assertIsDisplayed()
        compose.onNodeWithText("删除").assertIsDisplayed()
        compose.onAllNodesWithText("收藏").assertCountEquals(0)
        compose.onAllNodesWithText("取消收藏").assertCountEquals(0)
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

        compose.runOnIdle { assert(actions == listOf("dismiss", "dismiss")) }
    }

    @Test fun compatibilityWrapperUsesPlacePoolCapabilities() {
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

        compose.onNodeWithText("取消").assertIsDisplayed()
        compose.onNodeWithText("删除").assertIsDisplayed()
        compose.onAllNodesWithText("取消收藏").assertCountEquals(0)
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

        pressBack()

        compose.runOnIdle { assert(!dismissed.value) }
        compose.onNodeWithText("保存中").assertIsDisplayed()
    }

    @Test fun panelTitleHasHeadingSemantics() {
        setContent(source = PlaceDetailSource.Search)

        compose.onNodeWithTag("place-detail-title")
            .assertTextEquals("故宫博物院")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }

    private fun setContent(
        candidate: PlaceCandidate = candidate(),
        savedPlace: SavedPlace? = null,
        source: PlaceDetailSource,
        editState: PlaceDetailEditState? = null,
    ) {
        compose.setContent {
            EasyTripTheme {
                PlaceDetailPanel(
                    candidate = candidate,
                    savedPlace = savedPlace,
                    editState = editState,
                    source = source,
                    collectionBusy = false,
                    collectionError = null,
                    onAction = {},
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
