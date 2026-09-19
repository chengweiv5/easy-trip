package com.yangchengwei.easytrip.place.ui

import androidx.activity.ComponentActivity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeUp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.workspace.PlaceScheduleDayUi
import com.yangchengwei.easytrip.workspace.PlaceScheduleSummaryUi
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.amap.PlaceSearchDataSource
import com.yangchengwei.easytrip.place.data.RoomSavedPlaceRepository
import com.yangchengwei.easytrip.itinerary.domain.AddPlacesOutcome
import com.yangchengwei.easytrip.itinerary.domain.AddPlacesToDayUseCase
import com.yangchengwei.easytrip.itinerary.domain.DayItinerary
import com.yangchengwei.easytrip.itinerary.domain.ItineraryRepository
import com.yangchengwei.easytrip.itinerary.domain.UndoAddedItemsUseCase
import com.yangchengwei.easytrip.itinerary.ui.AddToItineraryStep
import com.yangchengwei.easytrip.itinerary.ui.AddToItineraryViewModel
import com.yangchengwei.easytrip.itinerary.ui.AddToItineraryUiState
import com.yangchengwei.easytrip.itinerary.ui.SelectPlacesContent
import com.yangchengwei.easytrip.itinerary.ui.SelectTargetDayContent
import com.yangchengwei.easytrip.trip.domain.CreateTrip
import com.yangchengwei.easytrip.trip.domain.TripDay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PlacePoolFlowTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun selectPlacesKeepsScheduledSeparateAndDisablesEmptyContinue() {
        val place = com.yangchengwei.easytrip.place.domain.SavedPlace(
            "scheduled", "trip", "poi", "西湖天地", "上城区南山路", GeoPoint(39.9, 116.4), "", emptyList(),
        )
        var toggled: String? = null
        compose.setContent {
            EasyTripTheme {
                SelectPlacesContent(
                    rows = listOf(SavedPlaceRowUi(place, 1, scheduled = true)),
                    state = AddToItineraryUiState(step = AddToItineraryStep.SELECT_PLACES),
                    onTogglePlace = { toggled = it },
                    onContinue = {},
                    onClose = {},
                )
            }
        }

        compose.onNodeWithContentDescription("已安排 1 次，可重复添加").assertIsDisplayed()
        compose.onNodeWithTag("select-place-scheduled").performClick()
        assertEquals("scheduled", toggled)
        compose.onNodeWithTag("select-places-continue").assertIsNotEnabled()
    }

    @Test fun contentCallbacksReportClicksWithoutSimulatingViewModelState() {
        val first = com.yangchengwei.easytrip.place.domain.SavedPlace(
            "first", "trip", "poi-1", "西湖", "地址", GeoPoint(39.9, 116.4), "", emptyList(),
        )
        val second = first.copy(id = "second", amapPoiId = "poi-2", name = "灵隐寺")
        val toggled = mutableListOf<String>()
        compose.setContent {
            EasyTripTheme {
                SelectPlacesContent(
                    rows = listOf(SavedPlaceRowUi(first, 0, false), SavedPlaceRowUi(second, 0, false)),
                    state = AddToItineraryUiState(step = AddToItineraryStep.SELECT_PLACES),
                    onTogglePlace = toggled::add,
                    onContinue = {},
                    onClose = {},
                )
            }
        }

        compose.onNodeWithTag("select-places-continue").assertIsNotEnabled()
        compose.onNodeWithTag("select-place-second").performClick()
        compose.onNodeWithTag("select-place-first").performClick()
        assertEquals(listOf("second", "first"), toggled)
    }

    @Test fun realViewModelRecomposesOrderedSelectionAndContinueState() {
        val first = com.yangchengwei.easytrip.place.domain.SavedPlace(
            "first", "trip", "poi-1", "西湖", "地址", GeoPoint(39.9, 116.4), "", emptyList(),
        )
        val second = first.copy(id = "second", amapPoiId = "poi-2", name = "灵隐寺")
        val repository = object : ItineraryRepository {
            override fun observeDay(dayId: String): Flow<DayItinerary> = flowOf(DayItinerary(dayId, "trip", emptyList()))
            override suspend fun addItem(dayId: String, savedPlaceId: String, targetIndex: Int) = "created-$savedPlaceId"
            override suspend fun moveItem(itemId: String, targetDayId: String, targetIndex: Int) = Unit
            override suspend fun deleteItem(itemId: String) = Unit
            override suspend fun updateTiming(itemId: String, arrivalTime: java.time.LocalTime?, stayMinutes: Int?) = Unit
            override suspend fun updateDetails(itemId: String, arrivalTime: java.time.LocalTime?, stayMinutes: Int?, note: String?) = error("Fake itinerary details are not modeled")
            override suspend fun removePlaceOccurrences(placeId: String) = Unit
        }
        val viewModel = AddToItineraryViewModel(
            "trip",
            AddPlacesToDayUseCase(repository),
            UndoAddedItemsUseCase(repository),
            SavedStateHandle(),
        )
        viewModel.reconcile(listOf("day-1"), setOf("first", "second"))
        viewModel.startFromPool()
        compose.setContent {
            val state = viewModel.state.collectAsStateWithLifecycle().value
            EasyTripTheme {
                SelectPlacesContent(
                    rows = listOf(SavedPlaceRowUi(first, 0, false), SavedPlaceRowUi(second, 0, false)),
                    state = state,
                    onTogglePlace = viewModel::togglePlace,
                    onContinue = viewModel::continueToTargetDay,
                    onClose = {},
                )
            }
        }

        compose.onNodeWithTag("select-places-continue").assertIsNotEnabled()
        compose.onNodeWithTag("select-place-second").performClick()
        compose.onNodeWithTag("select-place-first").performClick()
        compose.onNodeWithTag("select-places-continue").assertIsEnabled().performClick()
        compose.runOnIdle {
            assertEquals(listOf("second", "first"), viewModel.state.value.selectedPlaceIds)
            assertEquals(AddToItineraryStep.SELECT_TARGET_DAY, viewModel.state.value.step)
        }
    }

    @Test fun selectedScheduledPlaceRemainsSelectedAndContinueIsEnabled() {
        val place = com.yangchengwei.easytrip.place.domain.SavedPlace(
            "scheduled", "trip", "poi", "西湖天地", "上城区南山路", GeoPoint(39.9, 116.4), "", emptyList(),
        )
        compose.setContent {
            EasyTripTheme {
                SelectPlacesContent(
                    rows = listOf(SavedPlaceRowUi(place, 1, scheduled = true)),
                    state = AddToItineraryUiState(
                        selectedPlaceIds = listOf("scheduled"),
                        step = AddToItineraryStep.SELECT_PLACES,
                    ),
                    onTogglePlace = {},
                    onContinue = {},
                    onClose = {},
                )
            }
        }

        compose.onNodeWithTag("select-place-scheduled").assertIsSelected()
        compose.onNodeWithTag("select-places-continue").assertIsEnabled()
        compose.onNodeWithText("已选 1 个地点").assertIsDisplayed()
    }

    @Test fun selectPlacesFooterStaysVisibleAt280DpTwoTimesFontAndTouchSelectionEnablesContinue() {
        val place = com.yangchengwei.easytrip.place.domain.SavedPlace(
            "place", "trip", "poi", "西湖", "杭州市西湖区", GeoPoint(30.2, 120.1), "", emptyList(),
        )
        val state = mutableStateOf(AddToItineraryUiState(step = AddToItineraryStep.SELECT_PLACES))
        var continues = 0
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                EasyTripTheme {
                    Box(androidx.compose.ui.Modifier.requiredWidth(280.dp).height(432.dp)) {
                        SelectPlacesContent(
                            rows = listOf(SavedPlaceRowUi(place, 0, false)),
                            state = state.value,
                            onTogglePlace = { id -> state.value = state.value.copy(selectedPlaceIds = listOf(id)) },
                            onContinue = { continues++ },
                            onClose = {},
                        )
                    }
                }
            }
        }

        val continueButton = compose.onNodeWithTag("select-places-continue")
        continueButton.assertIsDisplayed().assertIsNotEnabled()
        val disabledBounds = continueButton.getUnclippedBoundsInRoot()
        val cancelBounds = compose.onNodeWithTag("select-places-close").assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue("continue=$disabledBounds", disabledBounds.right - disabledBounds.left >= 48.dp)
        assertTrue("close=$cancelBounds continue=$disabledBounds", cancelBounds.bottom <= disabledBounds.top)
        assertTrue("continue=$disabledBounds", disabledBounds.left >= 0.dp && disabledBounds.right <= 280.dp)
        assertTrue("continue=$disabledBounds", disabledBounds.top >= 0.dp && disabledBounds.bottom <= 432.dp)

        compose.onNodeWithTag("select-place-place").performTouchInput { click() }
        compose.onNodeWithTag("select-places-continue").assertIsDisplayed().assertIsEnabled()
        val enabledBounds = compose.onNodeWithTag("select-places-continue").getUnclippedBoundsInRoot()
        assertTrue("continue=$enabledBounds", enabledBounds.left >= 0.dp && enabledBounds.right <= 280.dp)
        assertTrue("continue=$enabledBounds", enabledBounds.top >= 0.dp && enabledBounds.bottom <= 432.dp)
        compose.onNodeWithTag("select-places-continue").performTouchInput { click() }
        assertEquals(1, continues)
    }

    @Test fun selectTargetDayFooterStaysVisibleAt280DpTwoTimesFontAndTouchSelectionEnablesSubmit() {
        val state = mutableStateOf(
            AddToItineraryUiState(
                selectedPlaceIds = listOf("place"),
                validityInitialized = true,
                step = AddToItineraryStep.SELECT_TARGET_DAY,
            ),
        )
        var submits = 0
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                EasyTripTheme {
                    Box(androidx.compose.ui.Modifier.requiredWidth(280.dp).height(432.dp)) {
                        SelectTargetDayContent(
                            days = listOf(TripDay("day-1", 0)),
                            state = state.value,
                            onSelectDay = { id -> state.value = state.value.copy(targetDayId = id) },
                            onSubmit = { submits++ },
                            onClose = {},
                        )
                    }
                }
            }
        }

        val submitButton = compose.onNodeWithTag("select-target-day-submit")
        submitButton.assertIsDisplayed().assertIsNotEnabled()
        val disabledBounds = submitButton.getUnclippedBoundsInRoot()
        val cancelBounds = compose.onNodeWithText("取消").assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue("submit=$disabledBounds", disabledBounds.right - disabledBounds.left >= 48.dp)
        assertTrue("cancel=$cancelBounds submit=$disabledBounds", cancelBounds.right <= disabledBounds.left)
        assertTrue("submit=$disabledBounds", disabledBounds.left >= 0.dp && disabledBounds.right <= 280.dp)
        assertTrue("submit=$disabledBounds", disabledBounds.top >= 0.dp && disabledBounds.bottom <= 432.dp)

        compose.onNodeWithTag("target-day-day-1").performTouchInput { click() }
        compose.onNodeWithTag("select-target-day-submit").assertIsDisplayed().assertIsEnabled()
        val enabledBounds = compose.onNodeWithTag("select-target-day-submit").getUnclippedBoundsInRoot()
        assertTrue("submit=$enabledBounds", enabledBounds.left >= 0.dp && enabledBounds.right <= 280.dp)
        assertTrue("submit=$enabledBounds", enabledBounds.top >= 0.dp && enabledBounds.bottom <= 432.dp)
        compose.onNodeWithTag("select-target-day-submit").performTouchInput { click() }
        assertEquals(1, submits)
    }

    @Test fun longDayListScrollsAtNarrowLargeTextWhileSubmitStaysReachable() {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                EasyTripTheme {
                    Box(androidx.compose.ui.Modifier.requiredWidth(280.dp).height(550.dp)) {
                        SelectTargetDayContent(
                            days = (0..19).map { TripDay("day-$it", it) },
                            state = AddToItineraryUiState(
                                selectedPlaceIds = listOf("place"),
                                targetDayId = "day-1",
                                validityInitialized = true,
                                step = AddToItineraryStep.SELECT_TARGET_DAY,
                            ),
                            onSelectDay = {},
                            onSubmit = {},
                            onClose = {},
                        )
                    }
                }
            }
        }

        compose.onNodeWithTag("select-target-day-submit").assertIsDisplayed()
        compose.onNodeWithTag("target-day-day-19").assertDoesNotExist()
        compose.onNodeWithTag("select-target-day-list").performScrollToNode(hasTestTag("target-day-day-19"))
        compose.onNodeWithTag("target-day-day-19").assertIsDisplayed()
        compose.onNodeWithTag("select-target-day-submit").assertIsDisplayed()
    }

    @Test fun targetDaySubmissionIsLockedWhileSubmitting() {
        var submits = 0
        compose.setContent {
            EasyTripTheme {
                SelectTargetDayContent(
                    days = listOf(TripDay("day-1", 0)),
                    state = AddToItineraryUiState(
                        selectedPlaceIds = listOf("place"),
                        targetDayId = "day-1",
                        step = AddToItineraryStep.SELECT_TARGET_DAY,
                        validityInitialized = true,
                        isSubmitting = true,
                    ),
                    onSelectDay = {},
                    onSubmit = { submits++ },
                    onClose = {},
                )
            }
        }

        compose.onNodeWithText("正在创建行程项，请勿重复操作").assertIsDisplayed()
        compose.onNodeWithTag("select-target-day-submit").assertIsNotEnabled().performClick()
        assertEquals(0, submits)
    }

    @Test fun missingTargetDayKeepsSelectionAndRequiresReselection() {
        compose.setContent {
            EasyTripTheme {
                SelectTargetDayContent(
                    days = listOf(TripDay("day-2", 1)),
                    state = AddToItineraryUiState(
                        selectedPlaceIds = listOf("place-a", "place-b"),
                        validityInitialized = true,
                        step = AddToItineraryStep.SELECT_TARGET_DAY,
                        result = AddPlacesOutcome.TargetDayMissing(listOf("place-a", "place-b")),
                    ),
                    onSelectDay = {},
                    onSubmit = {},
                    onClose = {},
                )
            }
        }

        compose.onNodeWithText("所选旅行日已不存在，请重新选择").assertIsDisplayed()
        compose.onNodeWithText("已选 2 个地点").assertIsDisplayed()
        compose.onNodeWithTag("select-target-day-submit").assertIsNotEnabled()
    }

    @Test fun placeDetailAllowsCollectionNoteAndTagsOnly() {
        val place = com.yangchengwei.easytrip.place.domain.SavedPlace(
            "detail", "trip", "poi-detail", "西湖天地", "上城区南山路", GeoPoint(39.9, 116.4), "", emptyList(),
        )
        compose.setContent {
            EasyTripTheme {
                PlaceDetailContent(
                    place = place,
                    draft = PlaceDetailDraft("", emptySet(), place.id),
                    saving = false,
                    error = null,
                    onNoteChange = {},
                    onTagsChange = {},
                    onToggleCollection = {},
                    onSave = {},
                )
            }
        }

        compose.onNodeWithText("取消收藏").assertIsDisplayed()
        compose.onNodeWithText("备注").assertIsDisplayed()
        compose.onNodeWithText("新标签").assertIsDisplayed()
        compose.onNodeWithText("加入行程").assertDoesNotExist()
        compose.onNodeWithText("已加入行程").assertDoesNotExist()
    }

    @Test fun rowActionsUseFixedTargetsExposeNamedSemanticsAndDispatchCurrentIds() {
        val first = com.yangchengwei.easytrip.place.domain.SavedPlace(
            "first", "trip", "poi-first", "非常非常长的地点名称用于验证操作区不会被挤出屏幕", "非常非常长的地点地址用于验证地址最多两行且操作仍可点击", GeoPoint(39.9, 116.4), "", emptyList(),
        )
        val second = first.copy(id = "second", amapPoiId = "poi-second", name = "灵隐寺")
        val actions = mutableListOf<PlacePoolAction>()
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(3f, 2f)) {
                EasyTripTheme {
                    PlacePoolContent(
                        state = PlacePoolUiState(rows = listOf(SavedPlaceRowUi(first, 0, false), SavedPlaceRowUi(second, 0, false))),
                        showSearch = false,
                        onAction = actions::add,
                    )
                }
            }
        }

        val quickAdd = compose.onNodeWithContentDescription("添加非常非常长的地点名称用于验证操作区不会被挤出屏幕到行程")
        val more = compose.onNodeWithContentDescription("非常非常长的地点名称用于验证操作区不会被挤出屏幕，更多操作")
        quickAdd.assertIsDisplayed().assertHasClickAction()
        more.assertIsDisplayed().assertHasClickAction()
        val quickAddBounds = quickAdd.getUnclippedBoundsInRoot()
        val moreBounds = more.getUnclippedBoundsInRoot()
        assertTrue(quickAddBounds.right - quickAddBounds.left >= 27.5.dp)
        assertTrue(quickAddBounds.bottom - quickAddBounds.top >= 27.5.dp)
        assertTrue(moreBounds.right - moreBounds.left >= 47.5.dp)
        assertTrue(moreBounds.bottom - moreBounds.top >= 47.5.dp)

        quickAdd.performClick()
        compose.onNodeWithTag("more-place-second").performClick()
        compose.onNodeWithTag("menu-edit-place-second", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("more-place-second").performClick()
        compose.onNodeWithTag("menu-delete-place-second", useUnmergedTree = true).performClick()
        assertEquals(
            listOf(
                PlacePoolAction.StartAddSingle("first"),
                PlacePoolAction.Edit(second),
                PlacePoolAction.Delete(second),
            ),
            actions,
        )
        compose.onNodeWithText("编辑").assertDoesNotExist()
        compose.onNodeWithText("删除").assertDoesNotExist()
    }

    @Test fun menuStateStaysWithPlaceAcrossLazyRowReorder() {
        val first = com.yangchengwei.easytrip.place.domain.SavedPlace(
            "first", "trip", "poi-first", "第一个地点", "地址", GeoPoint(39.9, 116.4), "", emptyList(),
        )
        val second = first.copy(id = "second", amapPoiId = "poi-second", name = "第二个地点")
        val rows = mutableStateOf(listOf(SavedPlaceRowUi(first, 0, false), SavedPlaceRowUi(second, 0, false)))
        val actions = mutableListOf<PlacePoolAction>()
        compose.setContent {
            EasyTripTheme {
                PlacePoolContent(
                    state = PlacePoolUiState(rows = rows.value),
                    showSearch = false,
                    onAction = actions::add,
                )
            }
        }

        compose.onNodeWithTag("more-place-first").performClick()
        compose.runOnIdle { rows.value = rows.value.reversed() }
        compose.onNodeWithTag("menu-delete-place-first", useUnmergedTree = true).performClick()

        assertEquals(listOf(PlacePoolAction.Delete(first)), actions)
    }

    @Test fun standaloneDetailUsesSelectedFullPlaceWhenRowIsFilteredOut() {
        val hidden = com.yangchengwei.easytrip.place.domain.SavedPlace(
            "hidden", "trip", "poi-hidden", "被筛选地点", "地址", GeoPoint(39.9, 116.4), "", emptyList(),
        )
        compose.setContent {
            EasyTripTheme {
                PlacePoolContent(
                    state = PlacePoolUiState(selectedDetailPlaceId = hidden.id, selectedDetailPlace = hidden),
                    showSearch = false,
                    onAction = {},
                )
            }
        }

        compose.onNodeWithTag("place-detail-title").assertIsDisplayed()
        compose.onNodeWithText("被筛选地点").assertIsDisplayed()
    }

    @Test fun publicSheetWithoutCoordinatorHidesAddEntrypoints() {
        val model = PlacePoolViewModel("trip", object : com.yangchengwei.easytrip.place.domain.SavedPlaceRepository {
            private val place = com.yangchengwei.easytrip.place.domain.SavedPlace(
                "place", "trip", "poi", "西湖", "地址", GeoPoint(39.9, 116.4), "", emptyList(),
            )
            override fun observePlaces(tripId: String, tagIds: Set<String>) = flowOf(listOf(place))
            override fun observeTags(tripId: String) = flowOf(emptyList<com.yangchengwei.easytrip.place.domain.PlaceTag>())
            override fun observeSavedPoiIds(tripId: String) = flowOf(setOf("poi"))
            override suspend fun save(tripId: String, candidate: PlaceCandidate) = com.yangchengwei.easytrip.place.domain.SavePlaceResult.Saved("place")
            override suspend fun updateDetails(placeId: String, note: String, tagNames: Set<String>) = Unit
            override suspend fun usageCount(placeId: String) = 0
            override suspend fun deletionImpact(placeId: String) = com.yangchengwei.easytrip.place.domain.PlaceDeletionImpact(0, 0)
            override suspend fun deletePlaceAndReferences(placeId: String) = Unit
        }, null)
        compose.setContent { EasyTripTheme { PlacePoolSheet(model, showSearch = false) } }
        compose.waitUntil(5_000) { model.state.value.rows.isNotEmpty() }

        compose.onNodeWithTag("quick-add-place-place").assertDoesNotExist()
        compose.onNodeWithTag("start-add-to-itinerary").assertDoesNotExist()
    }

    @Test fun tagsRemainReachableAtNarrowWidthAndLargeFont() {
        val tags = (1..8).map { com.yangchengwei.easytrip.place.domain.PlaceTag("$it", "很长标签$it") }
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(3f, 2f)) {
                EasyTripTheme {
                    PlacePoolContent(
                        state = PlacePoolUiState(tags = tags, rows = emptyList()),
                        showSearch = true,
                        onAction = {},
                    )
                }
            }
        }

        repeat(4) {
            compose.onNodeWithTag("place-pool-tags").performTouchInput { swipeLeft() }
        }
        compose.onNodeWithTag("tag-8").assertIsDisplayed()
    }

    @Test fun eightRowsScrollWhileThreeRowsFitWithoutScroll() {
        fun place(index: Int) = com.yangchengwei.easytrip.place.domain.SavedPlace(
            "place-$index", "trip", "poi-$index", "地点$index", "地址", GeoPoint(39.9, 116.4), "", emptyList(),
        )
        val eightRows = (0 until 8).map { SavedPlaceRowUi(place(it), 0, false) }
        val visibleRows = mutableStateOf(eightRows)
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 1f)) {
                EasyTripTheme {
                    Box(androidx.compose.ui.Modifier.requiredWidth(280.dp).height(432.dp)) {
                        PlacePoolContent(
                            state = PlacePoolUiState(
                                rows = visibleRows.value,
                                savedPoiIds = visibleRows.value.mapTo(mutableSetOf()) { it.place.amapPoiId },
                            ),
                            showSearch = false,
                            onAction = {},
                        )
                    }
                }
            }
        }

        compose.onNodeWithTag("workspace-place-list").performScrollToNode(hasTestTag("saved-place-place-7"))
        compose.onNodeWithTag("saved-place-place-7").assertIsDisplayed()

        compose.runOnIdle { visibleRows.value = eightRows.take(3) }
        compose.onNodeWithTag("workspace-place-list").performScrollToNode(hasTestTag("saved-place-place-0"))
        compose.onNodeWithTag("saved-place-place-0").assertIsDisplayed()
        compose.onNodeWithTag("saved-place-place-1").assertIsDisplayed()
        compose.onNodeWithTag("saved-place-place-2").assertIsDisplayed()
    }

    @Test fun workspaceScrollbarAppearsOnlyForOverflowTracksScrollAndDisappearsAfterFiltering() {
        fun place(index: Int) = com.yangchengwei.easytrip.place.domain.SavedPlace(
            "place-$index", "trip", "poi-$index", "地点$index", "地址", GeoPoint(39.9, 116.4), "", emptyList(),
        )
        val allRows = (0 until 8).map { SavedPlaceRowUi(place(it), 0, false) }
        val visibleRows = mutableStateOf(allRows)
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 1f)) {
                EasyTripTheme {
                    Box(androidx.compose.ui.Modifier.requiredWidth(280.dp).height(432.dp)) {
                        PlacePoolContent(
                            state = PlacePoolUiState(
                                rows = visibleRows.value,
                                savedPoiIds = allRows.mapTo(mutableSetOf()) { it.place.amapPoiId },
                                savedPlaceIds = allRows.mapTo(mutableSetOf()) { it.place.id },
                            ),
                            showSearch = false,
                            onAction = {},
                        )
                    }
                }
            }
        }

        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("place-pool-scrollbar-thumb", useUnmergedTree = true)
                .fetchSemanticsNodes().size == 1
        }
        val before = compose.onNodeWithTag("place-pool-scrollbar-thumb", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        compose.onNodeWithTag("workspace-place-list").performTouchInput { swipeUp() }
        compose.waitUntil(5_000) {
            compose.onNodeWithTag("place-pool-scrollbar-thumb", useUnmergedTree = true)
                .getUnclippedBoundsInRoot().top > before.top
        }
        val after = compose.onNodeWithTag("place-pool-scrollbar-thumb", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        assertEquals(before.bottom - before.top, after.bottom - after.top)
        compose.onNodeWithTag("place-pool-scrollbar-thumb", useUnmergedTree = true)
            .assert(SemanticsMatcher("has no scroll action") { node ->
                !hasScrollAction().matches(node)
            })

        compose.runOnIdle { visibleRows.value = allRows.take(2) }
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("place-pool-scrollbar-thumb", useUnmergedTree = true)
                .fetchSemanticsNodes().isEmpty()
        }
        compose.onNodeWithText("已收藏 8 个").assertIsDisplayed()
    }

    @Test fun workspaceScrollbarDoesNotCoverQuickAddOrMoreActions() {
        fun place(index: Int) = com.yangchengwei.easytrip.place.domain.SavedPlace(
            "place-$index", "trip", "poi-$index", "地点$index", "地址", GeoPoint(39.9, 116.4), "", emptyList(),
        )
        compose.setContent {
            EasyTripTheme {
                Box(androidx.compose.ui.Modifier.requiredWidth(280.dp).height(432.dp)) {
                    PlacePoolContent(
                        state = PlacePoolUiState(
                            rows = (0 until 8).map { SavedPlaceRowUi(place(it), 0, false) },
                        ),
                        showSearch = false,
                        onAction = {},
                    )
                }
            }
        }

        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("place-pool-scrollbar-thumb", useUnmergedTree = true)
                .fetchSemanticsNodes().size == 1
        }
        val thumb = compose.onNodeWithTag("place-pool-scrollbar-thumb", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val quickAdd = compose.onNodeWithTag("quick-add-place-place-0").getUnclippedBoundsInRoot()
        val more = compose.onNodeWithTag("more-place-place-0").getUnclippedBoundsInRoot()
        assertTrue("thumb=$thumb quickAdd=$quickAdd", thumb.left >= quickAdd.right)
        assertTrue("thumb=$thumb more=$more", thumb.left >= more.right)
    }

    @Test fun collectionTotalIgnoresTagFilteredRows() {
        val visible = com.yangchengwei.easytrip.place.domain.SavedPlace(
            "visible", "trip", "poi-visible", "西湖", "地址", GeoPoint(39.9, 116.4), "", emptyList(),
        )
        compose.setContent {
            EasyTripTheme {
                PlacePoolContent(
                    state = PlacePoolUiState(
                        rows = listOf(SavedPlaceRowUi(visible, 0, false)),
                        savedPoiIds = setOf("poi-visible", "poi-hidden-a", "poi-hidden-b"),
                    ),
                    showSearch = false,
                    onAction = {},
                )
            }
        }

        compose.onNodeWithText("已收藏 3 个").assertIsDisplayed()
    }

    @Test fun placePoolShowsCollectionTotalLegendAndOpenDetailSeparateFromQuickAdd() {
        val first = com.yangchengwei.easytrip.place.domain.SavedPlace(
            "first", "trip", "poi-first", "西湖", "地址", GeoPoint(39.9, 116.4), "", emptyList(),
        )
        val second = first.copy(id = "second", amapPoiId = "poi-second", name = "灵隐寺")
        val actions = mutableListOf<PlacePoolAction>()
        compose.setContent {
            EasyTripTheme {
                PlacePoolContent(
                    state = PlacePoolUiState(
                        rows = listOf(SavedPlaceRowUi(first, 0, false), SavedPlaceRowUi(second, 1, true)),
                        savedPoiIds = setOf(first.amapPoiId, second.amapPoiId),
                    ),
                    showSearch = false,
                    onAction = actions::add,
                )
            }
        }

        compose.onNodeWithText("已收藏 2 个").assertIsDisplayed()
        compose.onAllNodesWithText("已排入 · 仅收藏").assertCountEquals(0)
        compose.onNodeWithContentDescription("批量添加到行程").assertIsDisplayed()
        compose.onNodeWithContentDescription("查看西湖详情").performClick()
        compose.onNodeWithContentDescription("添加西湖到行程").performClick()

        assertEquals(
            listOf(PlacePoolAction.OpenDetail("first"), PlacePoolAction.StartAddSingle("first")),
            actions,
        )
    }

    @Test fun workspaceDetailUsesCompleteScheduleWithoutDuplicateAddAction() {
        val place = com.yangchengwei.easytrip.place.domain.SavedPlace(
            "place", "trip", "poi-place", "西湖", "地址", GeoPoint(39.9, 116.4), "", emptyList(),
        )
        val actions = mutableListOf<PlacePoolAction>()
        compose.setContent {
            EasyTripTheme {
                PlacePoolContent(
                    state = PlacePoolUiState(
                        rows = listOf(SavedPlaceRowUi(place, 3, true)),
                        savedPoiIds = setOf(place.amapPoiId),
                        selectedDetailPlaceId = place.id,
                        selectedDetailPlace = place,
                    ),
                    showSearch = false,
                    schedulesByPlaceId = mapOf(
                        place.id to PlaceScheduleSummaryUi(
                            isKnown = true,
                            totalOccurrences = 3,
                            days = listOf(PlaceScheduleDayUi("day-1", 0, 2), PlaceScheduleDayUi("day-3", 2, 1)),
                        ),
                    ),
                    onAction = actions::add,
                )
            }
        }

        compose.onNodeWithText("已加入行程").assertIsDisplayed()
        compose.onNodeWithText("第 1 天 · 2 次").assertIsDisplayed()
        compose.onNodeWithText("第 3 天 · 1 次").assertIsDisplayed()
        compose.onAllNodesWithText("加入行程").assertCountEquals(0)

        assertTrue(actions.isEmpty())
    }

    @Test fun standaloneDetailWithoutAddCapabilityDoesNotShowAddOrCrash() {
        val place = com.yangchengwei.easytrip.place.domain.SavedPlace(
            "place", "trip", "poi-place", "西湖", "地址", GeoPoint(39.9, 116.4), "", emptyList(),
        )
        val model = PlacePoolViewModel("trip", object : com.yangchengwei.easytrip.place.domain.SavedPlaceRepository {
            override fun observePlaces(tripId: String, tagIds: Set<String>) = flowOf(listOf(place))
            override fun observeTags(tripId: String) = flowOf(emptyList<com.yangchengwei.easytrip.place.domain.PlaceTag>())
            override fun observeSavedPoiIds(tripId: String) = flowOf(setOf(place.amapPoiId))
            override suspend fun save(tripId: String, candidate: PlaceCandidate) = com.yangchengwei.easytrip.place.domain.SavePlaceResult.Saved(place.id)
            override suspend fun updateDetails(placeId: String, note: String, tagNames: Set<String>) = Unit
            override suspend fun usageCount(placeId: String) = 0
            override suspend fun deletionImpact(placeId: String) = com.yangchengwei.easytrip.place.domain.PlaceDeletionImpact(0, 0)
            override suspend fun deletePlaceAndReferences(placeId: String) = Unit
        }, null)
        compose.setContent { EasyTripTheme { PlacePoolSheet(model, showSearch = false) } }
        compose.waitUntil(5_000) { model.state.value.rows.isNotEmpty() }
        compose.runOnIdle { model.openDetail(place.id) }

        compose.onNodeWithTag("place-detail-title").assertIsDisplayed()
        compose.onAllNodesWithText("加入行程").assertCountEquals(0)
    }

    @Test fun workspaceDetailOnlyCollectedPlaceHasNoScheduleBlock() {
        val place = com.yangchengwei.easytrip.place.domain.SavedPlace(
            "place", "trip", "poi-place", "西湖", "地址", GeoPoint(39.9, 116.4), "", emptyList(),
        )
        compose.setContent {
            EasyTripTheme {
                PlacePoolContent(
                    state = PlacePoolUiState(
                        rows = listOf(SavedPlaceRowUi(place, 0, false)),
                        savedPoiIds = setOf(place.amapPoiId),
                        selectedDetailPlaceId = place.id,
                        selectedDetailPlace = place,
                    ),
                    showSearch = false,
                    schedulesByPlaceId = mapOf(place.id to PlaceScheduleSummaryUi(isKnown = true)),
                    onAction = {},
                )
            }
        }

        compose.onNodeWithTag("place-detail-bookmark-outline").assertIsDisplayed()
        compose.onAllNodesWithText("已加入行程").assertCountEquals(0)
        compose.onAllNodesWithText("第 1 天 ·").assertCountEquals(0)
    }

    @Test fun emptyPoolProvidesSearchAction() {
        var searchRequested = false
        compose.setContent {
            EasyTripTheme {
                PlacePoolContent(
                    state = PlacePoolUiState(),
                    showSearch = false,
                    onAction = {},
                    onSearch = { searchRequested = true },
                )
            }
        }

        compose.onNodeWithText("还没有收藏地点").assertIsDisplayed()
        compose.onNodeWithText("搜索地点").performClick()
        assertEquals(true, searchRequested)
    }

    @Test fun savedSearchResultWithNoUsageRemovesImmediately() {
        val app = compose.activity.application as com.yangchengwei.easytrip.EasyTripApplication
        val tripId = runBlocking { app.tripRepository.createTrip(CreateTrip("取消收藏旅行", 1)) }
        val repository = RoomSavedPlaceRepository(app.database, idFactory = sequence("place-${System.nanoTime()}"))
        val candidate = PlaceCandidate("poi-remove", "颐和园", "北京市海淀区", GeoPoint(39.999, 116.275), "010")
        runBlocking { repository.save(tripId, candidate) }
        val source = object : PlaceSearchDataSource {
            override suspend fun search(keyword: String, city: String?) = listOf(candidate)
        }
        val model = PlacePoolViewModel(tripId, repository, source)
        compose.setContent { PlacePoolSheet(model) }
        compose.onNodeWithText("搜索地点").performTextInput("颐和园")
        compose.waitUntil(5_000) {
            model.state.value.search.results.isNotEmpty() && "poi-remove" in model.state.value.savedPoiIds
        }

        compose.onNodeWithTag("save-result-poi-remove").performClick()

        compose.waitUntil(5_000) { "poi-remove" !in model.state.value.savedPoiIds }
        compose.onNodeWithText("收藏").assertExists()
        compose.onNodeWithText("确认取消收藏").assertDoesNotExist()
    }

    @Test fun referencedSavedResultRequiresConfirmationAndDismissPreservesData() {
        val app = compose.activity.application as com.yangchengwei.easytrip.EasyTripApplication
        val tripId = runBlocking { app.tripRepository.createTrip(CreateTrip("引用收藏旅行", 1)) }
        val dayId = runBlocking { app.tripRepository.observeTrip(tripId).first()!!.days.single().id }
        val repository = RoomSavedPlaceRepository(app.database, idFactory = sequence("place-${System.nanoTime()}"))
        val candidate = PlaceCandidate("poi-used", "天坛", "北京市东城区", GeoPoint(39.883, 116.407), "010")
        val placeId = runBlocking {
            (repository.save(tripId, candidate) as com.yangchengwei.easytrip.place.domain.SavePlaceResult.Saved).id
        }
        runBlocking { app.itineraryRepository.addItem(dayId, placeId, 0) }
        val source = object : PlaceSearchDataSource {
            override suspend fun search(keyword: String, city: String?) = listOf(candidate)
        }
        val model = PlacePoolViewModel(tripId, repository, source)
        compose.setContent { PlacePoolSheet(model) }
        compose.onNodeWithText("搜索地点").performTextInput("天坛")
        compose.waitUntil(5_000) {
            model.state.value.search.results.isNotEmpty() && "poi-used" in model.state.value.savedPoiIds
        }

        compose.onNodeWithTag("save-result-poi-used").performClick()
        compose.onNodeWithText("将同时删除 1 次行程安排和 0 段路线。").assertExists()
        compose.onNodeWithText("取消").performClick()
        compose.waitUntil(5_000) { model.state.value.pendingCollectionRemoval == null }
        assertEquals(1, runBlocking { repository.usageCount(placeId) })
        assertEquals(setOf("poi-used"), runBlocking { repository.observeSavedPoiIds(tripId).first() })

        compose.onNodeWithTag("save-result-poi-used").performClick()
        compose.onNodeWithText("确认取消收藏").performClick()
        compose.waitUntil(5_000) { "poi-used" !in model.state.value.savedPoiIds }
        assertEquals(0, runBlocking { app.database.itineraryEditingDao().items(dayId).size })
    }

    @Test fun deletingAfterCancellingEditClosesDetailAndRemovesReferencesAfterConfirmation() {
        val app = compose.activity.application as com.yangchengwei.easytrip.EasyTripApplication
        val tripId = runBlocking { app.tripRepository.createTrip(CreateTrip("编辑删除旅行", 1)) }
        val repository = RoomSavedPlaceRepository(app.database, idFactory = sequence("place-${System.nanoTime()}"))
        val candidate = PlaceCandidate("poi-edit-delete", "待删除地点", "北京市东城区", GeoPoint(39.9, 116.4), "010")
        val placeId = runBlocking {
            (repository.save(tripId, candidate) as com.yangchengwei.easytrip.place.domain.SavePlaceResult.Saved).id
        }
        val dayId = runBlocking { app.tripRepository.observeTrip(tripId).first()!!.days.single().id }
        runBlocking { app.itineraryRepository.addItem(dayId, placeId, 0) }
        val model = PlacePoolViewModel(tripId, repository, null)
        compose.setContent { PlacePoolSheet(model, showSearch = false) }
        compose.waitUntil(5_000) { model.state.value.rows.size == 1 }

        compose.onNodeWithTag("more-place-$placeId").performClick()
        compose.onNodeWithTag("menu-edit-place-$placeId", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("place-detail-title").assertIsDisplayed()
        compose.onNodeWithText("取消").performClick()
        compose.waitUntil(5_000) { model.state.value.editing == null }
        compose.onNodeWithTag("place-detail-title").assertDoesNotExist()

        compose.onNodeWithTag("more-place-$placeId").performClick()
        compose.onNodeWithTag("menu-delete-place-$placeId", useUnmergedTree = true).performClick()
        compose.waitUntil(5_000) { model.state.value.deleting != null }
        compose.onNodeWithText("删除 待删除地点？").assertIsDisplayed()
        compose.onNodeWithText("确认删除地点").performClick()
        compose.waitUntil(5_000) { model.state.value.deleting == null && model.state.value.rows.isEmpty() }

        compose.onNodeWithTag("place-detail-title").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(null, model.state.value.editing)
            assertEquals(null, model.state.value.detailDraft)
        }
        assertEquals(0, runBlocking { app.database.itineraryEditingDao().items(dayId).size })
        assertEquals(emptySet<String>(), runBlocking { repository.observeSavedPoiIds(tripId).first() })
    }

    @Test fun cancellingDeleteAfterCancellingEditKeepsPlaceAndReferencesWithoutReopeningDetail() {
        val app = compose.activity.application as com.yangchengwei.easytrip.EasyTripApplication
        val tripId = runBlocking { app.tripRepository.createTrip(CreateTrip("取消编辑删除旅行", 1)) }
        val repository = RoomSavedPlaceRepository(app.database, idFactory = sequence("place-${System.nanoTime()}"))
        val candidate = PlaceCandidate("poi-cancel-delete", "保留地点", "北京市东城区", GeoPoint(39.9, 116.4), "010")
        val placeId = runBlocking {
            (repository.save(tripId, candidate) as com.yangchengwei.easytrip.place.domain.SavePlaceResult.Saved).id
        }
        val dayId = runBlocking { app.tripRepository.observeTrip(tripId).first()!!.days.single().id }
        runBlocking { app.itineraryRepository.addItem(dayId, placeId, 0) }
        val model = PlacePoolViewModel(tripId, repository, null)
        compose.setContent { PlacePoolSheet(model, showSearch = false) }
        compose.waitUntil(5_000) { model.state.value.rows.size == 1 }

        compose.onNodeWithTag("more-place-$placeId").performClick()
        compose.onNodeWithTag("menu-edit-place-$placeId", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("place-detail-title").assertIsDisplayed()
        compose.onNodeWithText("取消").performClick()
        compose.waitUntil(5_000) { model.state.value.editing == null }
        compose.onNodeWithTag("place-detail-title").assertDoesNotExist()

        compose.onNodeWithTag("more-place-$placeId").performClick()
        compose.onNodeWithTag("menu-delete-place-$placeId", useUnmergedTree = true).performClick()
        compose.waitUntil(5_000) { model.state.value.deleting != null }
        compose.onNodeWithText("删除 保留地点？").assertIsDisplayed()
        compose.onNodeWithText("取消").performClick()
        compose.waitUntil(5_000) { model.state.value.deleting == null }

        compose.onNodeWithTag("place-detail-title").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(null, model.state.value.editing)
            assertEquals(null, model.state.value.detailDraft)
        }
        assertEquals(1, runBlocking { app.database.itineraryEditingDao().items(dayId).size })
        assertEquals(setOf("poi-cancel-delete"), runBlocking { repository.observeSavedPoiIds(tripId).first() })
    }

    @Test fun searchSaveEditAndFilterThroughPlacePool() {
        val app = compose.activity.application as com.yangchengwei.easytrip.EasyTripApplication
        val tripId = runBlocking { app.tripRepository.createTrip(CreateTrip("地点旅行", 1)) }
        val repository = RoomSavedPlaceRepository(app.database, idFactory = sequence("place", "tag"))
        val source = object : PlaceSearchDataSource {
            override suspend fun search(keyword: String, city: String?) = listOf(
                PlaceCandidate("poi-1", "故宫博物院", "北京市东城区", GeoPoint(39.916, 116.397), "010"),
                PlaceCandidate("poi-2", "国家博物馆", "北京市东城区", GeoPoint(39.905, 116.401), "010"),
            )
        }
        val model = PlacePoolViewModel(tripId, repository, source)
        compose.setContent { PlacePoolSheet(model) }
        compose.onNodeWithText("搜索地点").performTextInput("故宫")
        compose.waitUntil(5_000) { model.state.value.search.results.isNotEmpty() }
        compose.onNodeWithText("故宫博物院").assertExists()
        compose.onAllNodesWithText("收藏")[0].performClick()
        compose.waitUntil(5_000) { model.state.value.search.savedPlaces.size == 1 }
        compose.onAllNodesWithText("收藏")[0].performClick()
        compose.waitUntil(5_000) { model.state.value.savedPoiIds.size == 2 }
        val editedPlaceId = model.state.value.rows.first().id
        compose.onNodeWithTag("more-place-$editedPlaceId").performClick()
        compose.onNodeWithTag("menu-edit-place-$editedPlaceId", useUnmergedTree = true).performClick()
        compose.onNodeWithText("备注").performTextInput("必去")
        compose.onNodeWithTag("place-detail-tags-input").performScrollTo().assertIsDisplayed().performTextInput("文化")
        compose.onNodeWithTag("place-detail-add-tag").performScrollTo().assertIsDisplayed().performTouchInput { click() }
        compose.onNodeWithText("文化").assertIsDisplayed()
        compose.onNodeWithTag("place-detail-tags-input").performScrollTo().performTextInput("宫殿")
        compose.onNodeWithTag("place-detail-add-tag").performScrollTo().assertIsDisplayed().performTouchInput { click() }
        compose.onNodeWithTag("place-detail-save").performScrollTo().assertIsDisplayed().performTouchInput { click() }
        compose.waitUntil(5_000) { model.state.value.tags.size == 2 }
        val cultureTagId = model.state.value.tags.single { it.name == "文化" }.id
        compose.onNodeWithTag("tag-$cultureTagId").performClick()
        compose.waitUntil(5_000) { model.state.value.search.savedPlaces.size == 1 }
        assertEquals("必去", model.state.value.search.savedPlaces.single().note)
        assertEquals(2, compose.onAllNodesWithText("已收藏").fetchSemanticsNodes().size)

        compose.onNodeWithTag("more-place-$editedPlaceId").performTouchInput { click() }
        compose.onNodeWithTag("menu-edit-place-$editedPlaceId", useUnmergedTree = true).performTouchInput { click() }
        compose.waitUntil(5_000) { model.state.value.detailDraft != null }
        assertEquals(setOf("文化", "宫殿"), model.state.value.detailDraft?.tags)
        compose.onNodeWithTag("place-detail-tags-input").performScrollTo().performTextInput("未保存")
        compose.onNodeWithTag("place-detail-add-tag").performScrollTo().performTouchInput { click() }
        compose.onNodeWithTag("place-detail-cancel").performScrollTo().assertIsDisplayed().performTouchInput { click() }
        compose.waitUntil(5_000) { model.state.value.editing == null }
        assertEquals(
            setOf("文化", "宫殿"),
            runBlocking { repository.observePlaces(tripId, emptySet()).first() }
                .single { it.id == editedPlaceId }.tags.map { it.name }.toSet(),
        )
    }

    private fun sequence(vararg prefixes: String): () -> String {
        var index = 0
        return { "${prefixes[index.coerceAtMost(prefixes.lastIndex)]}-${index++}" }
    }
}
