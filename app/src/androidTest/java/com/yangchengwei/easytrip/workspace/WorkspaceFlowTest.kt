package com.yangchengwei.easytrip.workspace

import android.content.Context
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.room.Room
import com.yangchengwei.easytrip.AppNavigation
import com.yangchengwei.easytrip.AppNavigationDependencies
import com.yangchengwei.easytrip.AppNavigationObserver
import com.yangchengwei.easytrip.AmapRuntimeSession
import com.yangchengwei.easytrip.amap.AmapConsentPersistence
import com.yangchengwei.easytrip.amap.AmapConsentStore
import com.yangchengwei.easytrip.amap.AmapPrivacyReporter
import com.yangchengwei.easytrip.amap.ConsentRegistry
import com.yangchengwei.easytrip.route.domain.RouteRefreshCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.itinerary.data.RoomItineraryRepository
import com.yangchengwei.easytrip.permission.InMemoryLocationPermissionRequestStore
import com.yangchengwei.easytrip.permission.LocationPermissionSnapshot
import com.yangchengwei.easytrip.place.amap.PlaceSearchDataSource
import androidx.lifecycle.Lifecycle
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import com.yangchengwei.easytrip.place.data.RoomSavedPlaceRepository
import com.yangchengwei.easytrip.route.data.RoomRouteLegRepository
import com.yangchengwei.easytrip.trip.data.RoomTripRepository
import com.yangchengwei.easytrip.trip.domain.TripService
import com.yangchengwei.easytrip.trip.ui.RoomDeleteImpactProvider
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso.pressBack
import androidx.lifecycle.SavedStateHandle
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.core.model.RouteStatus
import com.yangchengwei.easytrip.core.model.TransportMode
import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.itinerary.ui.CrossDayMoveDraft
import com.yangchengwei.easytrip.itinerary.ui.DayItineraryAction
import com.yangchengwei.easytrip.itinerary.ui.DayItineraryUiState
import com.yangchengwei.easytrip.itinerary.ui.ItineraryDeleteConfirmation
import com.yangchengwei.easytrip.itinerary.ui.ItineraryEditDraft
import com.yangchengwei.easytrip.itinerary.ui.ItineraryItemUi
import com.yangchengwei.easytrip.itinerary.ui.RouteLegUi
import com.yangchengwei.easytrip.itinerary.ui.RouteModeEditDraft
import com.yangchengwei.easytrip.itinerary.domain.AddPlacesToDayUseCase
import com.yangchengwei.easytrip.itinerary.domain.DayItinerary
import com.yangchengwei.easytrip.itinerary.domain.UndoAddedItemsUseCase
import com.yangchengwei.easytrip.itinerary.ui.AddToItineraryStep
import com.yangchengwei.easytrip.itinerary.ui.AddToItinerarySubmissionResult
import com.yangchengwei.easytrip.itinerary.ui.AddToItineraryViewModel
import com.yangchengwei.easytrip.itinerary.ui.FailedItineraryAddition
import com.yangchengwei.easytrip.itinerary.ui.UndoCreatedItemsBatch
import org.junit.Assert.assertTrue
import com.yangchengwei.easytrip.itinerary.domain.ItineraryItem
import com.yangchengwei.easytrip.itinerary.domain.ItineraryPlace
import com.yangchengwei.easytrip.itinerary.domain.ItineraryRepository
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.permission.LocationPermissionCoordinator
import com.yangchengwei.easytrip.place.domain.PlaceDeletionImpact
import com.yangchengwei.easytrip.place.domain.PlaceTag
import com.yangchengwei.easytrip.place.domain.SavePlaceResult
import com.yangchengwei.easytrip.place.domain.SavedPlace
import com.yangchengwei.easytrip.place.domain.SavedPlaceRepository
import com.yangchengwei.easytrip.route.domain.RouteLegRepository
import com.yangchengwei.easytrip.route.domain.RouteLegWithEndpoints
import com.yangchengwei.easytrip.route.domain.RoutePlanOutcome
import com.yangchengwei.easytrip.route.domain.RouteResult
import com.yangchengwei.easytrip.trip.domain.CreateTrip
import com.yangchengwei.easytrip.trip.domain.InsertSide
import com.yangchengwei.easytrip.trip.domain.TripDay
import com.yangchengwei.easytrip.trip.domain.TripRepository
import com.yangchengwei.easytrip.trip.domain.TripSummary
import com.yangchengwei.easytrip.trip.domain.TripWithDays
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class WorkspaceFlowTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun realNavigationEntersWorkspaceSearchesReturnsAndSwitchesSections() {
        val database = Room.inMemoryDatabaseBuilder(compose.activity, EasyTripDatabase::class.java).build()
        try {
            var nextId = 0
            val trips = RoomTripRepository(database.tripDao(), idFactory = { "nav-${nextId++}" })
            val places = RoomSavedPlaceRepository(database, idFactory = { "nav-${nextId++}" })
            val itineraries = RoomItineraryRepository(database, database.itineraryEditingDao(), database.routeLegDao())
            val routes = RoomRouteLegRepository(database.routeLegDao())
            val tripId = runBlocking { trips.createTrip(CreateTrip("真实导航", 1)) }
            val museum = PlaceCandidate("museum", "博物馆", "北京市东城区", GeoPoint(39.91, 116.41), "010")
            val navigated = mutableListOf<String>()
            compose.setContent {
                AppNavigation(
                    service = TripService(trips),
                    repository = trips,
                    impacts = RoomDeleteImpactProvider(database.deleteImpactDao()),
                    dependencies = AppNavigationDependencies(
                        savedPlaceRepository = places,
                        itineraryRepository = itineraries,
                        routeLegRepository = routes,
                        mapPreferences = InMemoryMapPreferences(),
                        locationPermissionRequestStore = InMemoryLocationPermissionRequestStore(),
                        placeSearchDataSource = object : PlaceSearchDataSource {
                            override suspend fun search(keyword: String, city: String?) = listOf(museum)
                        },
                        mapConsentToken = consentToken(),
                    ),
                    navigationObserver = AppNavigationObserver(navigated::add),
                    mapHostFactory = ::TestMapHost,
                )
            }

            compose.onNodeWithTag("continue-trip-$tripId").performClick()
            compose.onNodeWithTag("section-PLACE_POOL").assertIsSelected()
            compose.onNodeWithTag("workspace-search-launcher").assertHasClickAction().performClick()
            compose.onNodeWithTag("place-search-field").performTextInput("博物馆")
            compose.waitUntil(5_000) {
                compose.onAllNodesWithTag("place-search-bookmark-touch-museum").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("place-search-bookmark-touch-museum").performClick()
            compose.waitUntil(5_000) { runBlocking { places.observeSavedPoiIds(tripId).first() } == setOf("museum") }
            compose.onNodeWithTag("place-search-back").performClick()

            compose.onNodeWithText("刚刚收藏 · 待安排行程").assertIsDisplayed()
            compose.onNodeWithTag("section-ITINERARY").performClick().assertIsSelected()
            compose.onNodeWithTag("section-PLACE_POOL").performClick().assertIsSelected()
            assertEquals(listOf("trips/$tripId", "trips/$tripId/search"), navigated)
        } finally {
            database.close()
        }
    }

    @Test fun undecidedWorkspaceEntryShowsConsentExplanationUntilDecision() {
        val fixture = consentNavigationFixture(null)
        compose.setContent { fixture.render() }
        enterWorkspace()

        compose.onNodeWithText("允许 Easy Trip 使用地图").assertIsDisplayed()
        compose.onNodeWithText("地图用于展示收藏地点、每天的路线和交通距离。暂不允许也可以继续编辑地点池和行程。").assertIsDisplayed()
        compose.onNodeWithText("允许使用地图").assertIsDisplayed()
        compose.onNodeWithText("暂不允许").assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, fixture.reporter.shownCalls) }
        compose.onNodeWithTag("workspace-back").performClick()
        compose.onNodeWithTag("continue-trip-trip").performClick()
        compose.onNodeWithText("允许 Easy Trip 使用地图").assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, fixture.reporter.shownCalls) }
    }

    @Test fun mapConsentDialogExposesOrderedPolicyGateAndAcceptsOnlyAfterConfirmation() {
        val fixture = consentNavigationFixture(null)
        compose.setContent { fixture.render() }
        enterWorkspace()

        compose.onNodeWithText("允许 Easy Trip 使用地图", useUnmergedTree = true)
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        compose.onAllNodesWithTag("map-consent-policy-confirmation", useUnmergedTree = true)
            .assertCountEquals(1)
        compose.onNodeWithTag("map-consent-policy-confirmation", useUnmergedTree = true)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, androidx.compose.ui.semantics.Role.Checkbox))
        val title = compose.onNodeWithText("允许 Easy Trip 使用地图").getUnclippedBoundsInRoot()
        val purpose = compose.onNodeWithText("地图用于展示收藏地点、每天的路线和交通距离。暂不允许也可以继续编辑地点池和行程。")
            .getUnclippedBoundsInRoot()
        val policy = compose.onNodeWithText("阅读高德隐私权政策").assertHasClickAction().getUnclippedBoundsInRoot()
        val checkbox = compose.onNodeWithTag("map-consent-policy-confirmation").assertIsOff()
        val checkboxBounds = checkbox.getUnclippedBoundsInRoot()
        val allow = compose.onNodeWithText("允许使用地图").assertIsNotEnabled().getUnclippedBoundsInRoot()
        val decline = compose.onNodeWithText("暂不允许").assertIsEnabled().getUnclippedBoundsInRoot()
        assertTrue("title=$title purpose=$purpose", title.top < purpose.top)
        assertTrue("purpose=$purpose policy=$policy", purpose.top < policy.top)
        assertTrue("policy=$policy checkbox=$checkboxBounds", policy.top < checkboxBounds.top)
        assertTrue("checkbox=$checkboxBounds allow=$allow decline=$decline", checkboxBounds.bottom <= allow.top && checkboxBounds.bottom <= decline.top)

        compose.onNodeWithText("我已阅读高德隐私权政策").performClick()
        compose.onNodeWithTag("map-consent-policy-confirmation").assertIsOn()
        compose.onAllNodesWithContentDescription("我已阅读高德隐私权政策").assertCountEquals(0)
        compose.onNodeWithText("允许使用地图").assertIsEnabled().performClick()
        compose.waitUntil(5_000) { fixture.reporter.decisions == listOf(true) }
        compose.onNodeWithText("允许 Easy Trip 使用地图").assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, fixture.runtimeSessions) }
    }

    @Test fun alreadyReportedUndecidedWorkspaceStillShowsConsentExplanation() {
        val fixture = consentNavigationFixture(null)
        runBlocking { fixture.store.reportShown().getOrThrow() }
        compose.setContent { fixture.render() }

        enterWorkspace()

        compose.onNodeWithText("允许 Easy Trip 使用地图").assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, fixture.reporter.shownCalls) }
    }

    @Test fun failedShownReportOffersRetryAndKeepsDecisionsDisabledUntilShown() {
        val fixture = consentNavigationFixture(null)
        fixture.reporter.failShown = true
        compose.setContent { fixture.render() }
        enterWorkspace()

        compose.onNodeWithText("隐私说明展示失败，请重试").assertIsDisplayed()
        compose.onNodeWithText("暂不允许").assertIsNotEnabled()
        compose.onNodeWithText("重试").performClick()
        compose.waitUntil(5_000) { fixture.reporter.shownCalls == 2 }
        compose.onNodeWithText("暂不允许").assertIsEnabled().performClick()
        compose.onNodeWithText("允许 Easy Trip 使用地图").assertDoesNotExist()
    }

    @Test fun persistedDeclineDoesNotAutoPromptOnReentry() {
        val fixture = consentNavigationFixture(false)
        compose.setContent { fixture.render() }
        enterWorkspace()

        compose.onNodeWithText("允许 Easy Trip 使用地图").assertDoesNotExist()
        compose.onNodeWithText("地点池").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, fixture.reporter.shownCalls) }
    }

    @Test fun explicitAuthorizeActionReopensConsentExplanation() {
        val fixture = consentNavigationFixture(false)
        compose.setContent { fixture.render() }
        enterWorkspace()

        compose.onNodeWithTag("map-consent-open").performClick()
        compose.onNodeWithText("允许 Easy Trip 使用地图").assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, fixture.reporter.shownCalls) }
    }

    @Test fun appNavigationBindsPermissionCallbackToRequestThenSettingsResumeGrantShowsLocationOnce() {
        val requestStore = InMemoryLocationPermissionRequestStore()
        val permissionSnapshot = AtomicReference(LocationPermissionSnapshot(granted = false, shouldShowRationale = true))
        val launches = mutableListOf<Array<String>>()
        var permissionResult: ((LocationPermissionSnapshot) -> Unit)? = null
        var settingsLaunches = 0
        val locationCalls = AtomicInteger()
        compose.setContent {
            AppNavigation(
                service = TripService(Trips()),
                repository = Trips(),
                impacts = EmptyDeleteImpactProvider,
                dependencies = productionLocationDependencies(requestStore),
                mapHostFactory = { context -> LocationRecordingHost(context, locationCalls) },
                onLaunchLocationPermission = { permissions, onResult ->
                    launches += permissions
                    permissionResult = onResult
                    Result.success(Unit)
                },
                onOpenApplicationSettings = {
                    settingsLaunches++
                    Result.success(Unit)
                },
                locationPermissionSnapshot = permissionSnapshot::get,
            )
        }

        enterWorkspace()
        compose.onNodeWithTag("workspace-locate").performClick()
        compose.onNodeWithText("允许 Easy Trip 获取你的位置").assertIsDisplayed()
        compose.onNodeWithText("继续").performClick()
        compose.waitUntil(5_000) { launches.size == 1 && requestStore.hasRequested && permissionResult != null }
        compose.runOnIdle { assertEquals(2, launches.single().size) }

        permissionResult!!.invoke(LocationPermissionSnapshot(granted = false, shouldShowRationale = false))
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("定位权限未开启").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("前往设置").performClick()
        compose.waitUntil(5_000) { settingsLaunches == 1 }

        permissionSnapshot.set(LocationPermissionSnapshot(granted = true, shouldShowRationale = false))
        compose.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.waitUntil(5_000) { locationCalls.get() == 1 }
        compose.runOnIdle { assertEquals(1, locationCalls.get()) }
    }

    @Test fun appNavigationIgnoresPermissionCallbackAfterWorkspaceDisposes() {
        val requestStore = InMemoryLocationPermissionRequestStore()
        var permissionResult: ((LocationPermissionSnapshot) -> Unit)? = null
        val locationCalls = AtomicInteger()
        compose.setContent {
            AppNavigation(
                service = TripService(Trips()),
                repository = Trips(),
                impacts = EmptyDeleteImpactProvider,
                dependencies = productionLocationDependencies(requestStore),
                mapHostFactory = { context -> LocationRecordingHost(context, locationCalls) },
                onLaunchLocationPermission = { _, onResult ->
                    permissionResult = onResult
                    Result.success(Unit)
                },
            )
        }

        enterWorkspace()
        compose.onNodeWithTag("workspace-locate").performClick()
        compose.onNodeWithText("继续").performClick()
        compose.waitUntil(5_000) { requestStore.hasRequested && permissionResult != null }
        compose.onNodeWithTag("workspace-back").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("workspace-search-launcher").fetchSemanticsNodes().isEmpty()
        }

        permissionResult!!.invoke(LocationPermissionSnapshot(granted = true, shouldShowRationale = false))
        compose.waitForIdle()

        assertEquals(0, locationCalls.get())
    }

    @Test fun appNavigationIgnoresOlderPermissionCallbackAfterNewerRequest() {
        val requestStore = InMemoryLocationPermissionRequestStore()
        val callbacks = mutableListOf<(LocationPermissionSnapshot) -> Unit>()
        val locationCalls = AtomicInteger()
        val permissionSnapshot = AtomicReference(LocationPermissionSnapshot(granted = false, shouldShowRationale = true))
        compose.setContent {
            AppNavigation(
                service = TripService(Trips()),
                repository = Trips(),
                impacts = EmptyDeleteImpactProvider,
                dependencies = productionLocationDependencies(requestStore),
                mapHostFactory = { context -> LocationRecordingHost(context, locationCalls) },
                onLaunchLocationPermission = { _, onResult ->
                    callbacks += onResult
                    Result.success(Unit)
                },
                locationPermissionSnapshot = permissionSnapshot::get,
            )
        }

        enterWorkspace()
        compose.onNodeWithTag("workspace-locate").performClick()
        compose.onNodeWithText("继续").performClick()
        compose.waitUntil(5_000) { callbacks.size == 1 && requestStore.hasRequested }
        callbacks.single().invoke(LocationPermissionSnapshot(granted = false, shouldShowRationale = true))
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("允许 Easy Trip 获取你的位置").fetchSemanticsNodes().isEmpty()
        }

        compose.onNodeWithTag("workspace-locate").performClick()
        compose.onNodeWithText("继续").performClick()
        compose.waitUntil(5_000) { callbacks.size == 2 }

        callbacks.first().invoke(LocationPermissionSnapshot(granted = true, shouldShowRationale = false))
        compose.waitForIdle()
        assertEquals(0, locationCalls.get())
    }

    @Test fun synchronousPermissionCallbackWaitsForLaunchRecordingBeforePermanentDenial() {
        val requestStore = InMemoryLocationPermissionRequestStore()
        compose.setContent {
            AppNavigation(
                service = TripService(Trips()),
                repository = Trips(),
                impacts = EmptyDeleteImpactProvider,
                dependencies = productionLocationDependencies(requestStore),
                mapHostFactory = ::TestMapHost,
                onLaunchLocationPermission = { _, onResult ->
                    onResult(LocationPermissionSnapshot(granted = false, shouldShowRationale = false))
                    Result.success(Unit)
                },
            )
        }

        enterWorkspace()
        compose.onNodeWithTag("workspace-locate").performClick()
        compose.onNodeWithText("继续").performClick()

        compose.waitUntil(5_000) {
            requestStore.hasRequested &&
                compose.onAllNodesWithText("定位权限未开启").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test fun withdrawalStopsSearchAndMapWithoutResettingWorkspace() {
        val fixture = consentNavigationFixture(true)
        compose.setContent { fixture.render() }
        enterWorkspace()
        compose.waitUntil(5_000) { fixture.runtimeSessions == 1 }
        compose.onNodeWithTag("section-ITINERARY").performClick().assertIsSelected()

        compose.runOnUiThread { runBlocking { fixture.store.decide(false) } }

        compose.onNodeWithTag("map-consent-required").assertIsDisplayed()
        compose.onNodeWithTag("section-ITINERARY").assertIsSelected()
        compose.runOnIdle { assertEquals(1, fixture.stoppedSessions) }
    }

    @Test fun longTripNameKeepsBackMoreAndSearchPhysicallyClickable() {
        val longName = "这是一段足够长以验证标题省略不会挤压两侧操作按钮的旅行名称"
        val model = TripWorkspaceViewModel("trip", LongNameTrips(longName), Places(), Itineraries(), Legs(), SavedStateHandle())
        var backCount = 0
        var settingsCount = 0
        var searchCount = 0
        compose.setContent {
            TripWorkspaceScreen(
                viewModel = model,
                consent = null,
                onBack = { backCount++ },
                onSettings = { settingsCount++ },
                onOpenSearch = { searchCount++ },
                placeContent = { Text("地点内容") },
                dayItineraryContent = { Text("行程内容") },
            )
        }

        val backNode = compose.onNodeWithTag("workspace-back").assertIsDisplayed().assertHasClickAction()
        val moreNode = compose.onNodeWithTag("workspace-more").assertIsDisplayed().assertHasClickAction()
        val searchNode = compose.onNodeWithTag("workspace-search-launcher").assertIsDisplayed().assertHasClickAction()
        val back = backNode.getUnclippedBoundsInRoot()
        val more = moreNode.getUnclippedBoundsInRoot()
        val search = searchNode.getUnclippedBoundsInRoot()
        assert(back.right - back.left >= 44.dp && back.bottom - back.top >= 44.dp)
        assert(more.right - more.left >= 44.dp && more.bottom - more.top >= 44.dp)
        assert(search.right - search.left >= 44.dp && search.bottom - search.top >= 44.dp)
        assert(back.right <= more.left)

        backNode.performClick()
        moreNode.performClick()
        searchNode.performClick()
        compose.runOnIdle {
            assertEquals(1, backCount)
            assertEquals(1, settingsCount)
            assertEquals(1, searchCount)
        }
    }

    @Test fun newCompositionStartsFreshMapAttemptWithoutReusingFailureOverlay() {
        val model = TripWorkspaceViewModel("trip", Trips(), Places(), Itineraries(), Legs(), SavedStateHandle())
        var composition by mutableIntStateOf(0)
        val hosts = mutableListOf<AmapMapHost>()
        compose.setContent {
            key(composition) {
                TripWorkspaceScreen(
                    viewModel = model,
                    consent = consentToken(),
                    onBack = {},
                    onSettings = {},
                    placeContent = { Text("地点内容") },
                    dayItineraryContent = { Text("行程内容") },
                    mapHostFactory = { context ->
                        val host: AmapMapHost = if (hosts.isEmpty()) {
                            FailingMapHost(context)
                        } else {
                            object : AmapMapHost {
                                override val view = View(context)
                                override fun setOnReadyListener(listener: (() -> Unit)?) = Unit
                                override fun onCreate() = Unit
                                override fun onResume() = Unit
                                override fun onPause() = Unit
                                override fun onDestroy() = Unit
                            }
                        }
                        hosts.add(host)
                        host
                    },
                    mapReadyTimeoutMillis = 60_000,
                )
            }
        }

        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("map-load-failed").fetchSemanticsNodes().isNotEmpty()
        }
        compose.runOnUiThread { composition++ }
        compose.waitUntil(5_000) { hosts.size == 2 }

        compose.onNodeWithTag("workspace-map-loading").assertIsDisplayed()
        compose.onAllNodesWithTag("map-load-failed").assertCountEquals(0)
        compose.onAllNodesWithTag("map-retry").assertCountEquals(0)
    }

    @Test fun mapRetryRecreatesOnlyMapHostAndKeepsWorkspaceContext() {
        val model = TripWorkspaceViewModel("trip", Trips(), Places(), Itineraries(), Legs(), SavedStateHandle())
        var attempt by mutableIntStateOf(0)
        var hosts = 0
        compose.setContent {
            key(attempt) {
                TripWorkspaceScreen(
                    viewModel = model,
                    consent = consentToken(),
                    onBack = {},
                    onSettings = {},
                    placeContent = { Text("地点内容") },
                    dayItineraryContent = { Text("行程内容") },
                    mapHostFactory = { context -> FailingMapHost(context).also { hosts++ } },
                )
            }
        }

        compose.waitUntil(5_000) { compose.onAllNodesWithTag("map-retry").fetchSemanticsNodes().isNotEmpty() }
        model.selectSection(WorkspaceSection.ITINERARY)
        model.setSheetLevel(WorkspaceSheetLevel.EXPANDED)
        compose.waitUntil(5_000) {
            model.state.value.section == WorkspaceSection.ITINERARY &&
                model.state.value.sheetLevel == WorkspaceSheetLevel.EXPANDED
        }
        val identity = model
        compose.onNodeWithText("行程内容").assertIsDisplayed()
        val retry = compose.onNodeWithTag("map-retry").assertIsDisplayed().assertHasClickAction()
        retry.assertHeightIsAtLeast(48.dp)
        retry.performClick()
        compose.waitUntil(5_000) { hosts == 2 }

        assertEquals(identity, model)
        assertEquals(WorkspaceSection.ITINERARY, model.state.value.section)
        assertEquals(WorkspaceSheetLevel.EXPANDED, model.state.value.sheetLevel)
        compose.onNodeWithText("行程内容").assertIsDisplayed()
    }

    @Test fun locateRequestedWhileLoadingIsForwardedToReplacementAttempt() {
        val token = consentToken()
        var locateRequest by mutableIntStateOf(0)
        val hosts = AtomicInteger()
        val locationCalls = AtomicInteger()
        compose.setContent {
            TripWorkspaceScreen(
                pageState = TripWorkspacePageState.Ready(
                    TripWorkspaceUiState(tripName = "测试旅行").toReadyState(),
                ),
                consent = token,
                onAction = {},
                onMarkerClick = {},
                onMapPoiClick = {},
                placeState = com.yangchengwei.easytrip.place.ui.PlacePoolUiState(),
                onPlaceAction = {},
                itineraryState = DayItineraryUiState(),
                onItineraryAction = {},
                onCloseOverlay = {},
                onDismissMapPlace = {},
                locateRequest = locateRequest,
                mapReadyTimeoutMillis = 400,
                mapHostFactory = { context ->
                    val attempt = hosts.getAndIncrement()
                    object : AmapMapHost {
                        private var readyListener: (() -> Unit)? = null
                        override val view = View(context)
                        override fun canRenderBeforeReady() = attempt == 1
                        override fun setOnReadyListener(listener: (() -> Unit)?) {
                            readyListener = listener
                        }
                        override fun onCreate() = Unit
                        override fun onResume() = Unit
                        override fun onPause() = Unit
                        override fun onDestroy() = Unit
                        override fun showCurrentLocation() {
                            locationCalls.incrementAndGet()
                        }
                    }
                },
            )
        }

        compose.waitUntil(5_000) { hosts.get() == 1 }
        compose.runOnUiThread { locateRequest = 1 }
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("map-retry").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("map-retry").performClick()
        compose.waitUntil(5_000) { hosts.get() == 2 && locationCalls.get() == 1 }
        compose.runOnIdle { assertEquals(1, locationCalls.get()) }
    }

    @Test fun neverReadyMapShowsApprovedFallbackKeepsContentAndRetryRecreatesOnlyHost() {
        val model = TripWorkspaceViewModel("trip", Trips(), Places(), Itineraries(), Legs(), SavedStateHandle())
        val created = mutableListOf<Int>()
        val destroyed = mutableListOf<Int>()
        compose.setContent {
            TripWorkspaceScreen(
                viewModel = model,
                consent = consentToken(),
                onBack = {},
                onSettings = {},
                placeContent = { Text("地点内容保持可见") },
                dayItineraryContent = { Text("行程内容保持可见") },
                mapReadyTimeoutMillis = 400,
                mapHostFactory = { context ->
                    val attempt = created.size
                    created += attempt
                    object : AmapMapHost {
                        override val view = View(context)
                        override fun canRenderBeforeReady() = attempt == 1
                        override fun setOnReadyListener(listener: (() -> Unit)?) = Unit
                        override fun onCreate() = Unit
                        override fun onResume() = Unit
                        override fun onPause() = Unit
                        override fun onDestroy() { destroyed += attempt }
                    }
                },
            )
        }

        compose.waitUntil(5_000) { compose.onAllNodesWithText("地图暂时无法加载").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("地点和行程仍可查看，请稍后重试").assertIsDisplayed()
        compose.onNodeWithText("地点内容保持可见").assertIsDisplayed()
        compose.onNodeWithTag("section-ITINERARY").performClick()
        compose.onNodeWithText("行程内容保持可见").assertIsDisplayed()
        compose.onNodeWithTag("map-retry").performClick()
        compose.waitUntil(5_000) { created.size == 2 && destroyed.contains(0) }
        compose.onNodeWithTag("workspace-map-fallback").assertDoesNotExist()

        assertEquals(WorkspaceSection.ITINERARY, model.state.value.section)
    }

    @Test fun zoomButtonsReportExactlyOneViewportOperationBeforeHostZoom() {
        val events = mutableListOf<String>()
        compose.setContent {
            AmapComposeMap(
                model = MapUiModel(),
                onMarkerClick = {},
                consent = consentToken(),
                hostFactory = { context -> object : AmapMapHost {
                    override val view = View(context)
                    override fun canRenderBeforeReady() = true
                    override fun onCreate() = Unit
                    override fun onResume() = Unit
                    override fun onPause() = Unit
                    override fun onDestroy() = Unit
                    override fun zoomIn() { events += "zoom-in" }
                    override fun zoomOut() { events += "zoom-out" }
                } },
                onUserGesture = { events += "viewport" },
            )
        }

        compose.onNodeWithTag("zoom-in").performClick()
        compose.onNodeWithTag("zoom-out").performClick()
        compose.runOnIdle {
            assertEquals(listOf("viewport", "zoom-in", "viewport", "zoom-out"), events)
        }
    }

    @Test fun zoomButtonClearsViewportAndSuppressesSectionFits() {
        val workspace = TripWorkspaceViewModel("trip", Trips(), Places(), Itineraries(), Legs(), SavedStateHandle())
        lateinit var host: ZoomRecordingHost
        compose.setContent {
            TripWorkspaceRoute(
                viewModel = workspace,
                consent = consentToken(),
                onBack = {},
                onSettings = {},
                locationPermissionCoordinator = LocationPermissionCoordinator(InMemoryLocationPermissionRequestStore()),
                locationPermissionSnapshot = { LocationPermissionSnapshot(false, false) },
                onWorkspaceEffect = {},
                mapHostFactory = { context -> ZoomRecordingHost(context).also { host = it } },
            )
        }
        compose.waitUntil(5_000) {
            workspace.pageState.value is TripWorkspacePageState.Ready &&
                runCatching { host.viewportCalls == 1 }.getOrDefault(false)
        }

        compose.onNodeWithTag("zoom-in").performClick()
        compose.waitUntil(5_000) { host.zoomInCalls == 1 && workspace.state.value.map.viewportRequest == null }
        compose.onNodeWithTag("section-ITINERARY").performClick()
        compose.onNodeWithTag("section-PLACE_POOL").performClick()
        compose.waitForIdle()

        compose.runOnIdle {
            assertEquals(1, host.zoomInCalls)
            assertEquals(1, host.viewportCalls)
            assertEquals(null, workspace.state.value.map.viewportRequest)
        }
    }

    @Test fun grantedLocateBeforeFirstConsentMountRunsExactlyOnceWhenMapBecomesReady() {
        val workspace = TripWorkspaceViewModel("trip", Trips(), Places(), Itineraries(), Legs(), SavedStateHandle())
        val coordinator = LocationPermissionCoordinator(InMemoryLocationPermissionRequestStore())
        coordinator.attachWorkspace("trip")
        var consent by androidx.compose.runtime.mutableStateOf<com.yangchengwei.easytrip.amap.AmapConsentToken?>(null)
        val locationCalls = AtomicInteger()
        var hosts = 0
        compose.setContent {
            TripWorkspaceRoute(
                viewModel = workspace,
                consent = consent,
                onBack = {},
                onSettings = {},
                locationPermissionCoordinator = coordinator,
                locationPermissionSnapshot = { LocationPermissionSnapshot(granted = true, shouldShowRationale = false) },
                onWorkspaceEffect = {},
                mapHostFactory = { context ->
                    LocationRecordingHost(context, locationCalls).also { hosts++ }
                },
            )
        }
        compose.waitUntil(5_000) {
            workspace.pageState.value is TripWorkspacePageState.Ready &&
                compose.onAllNodesWithTag("map-consent-required").fetchSemanticsNodes().isNotEmpty()
        }
        compose.runOnIdle {
            assertEquals(0, hosts)
            assertEquals(0, locationCalls.get())
        }

        compose.onNodeWithTag("workspace-locate").performClick()
        compose.waitForIdle()
        compose.runOnUiThread { consent = consentToken() }

        compose.waitUntil(5_000) { hosts > 0 && locationCalls.get() == 1 }
        compose.onNodeWithTag("section-ITINERARY").performClick()
        compose.onNodeWithTag("section-PLACE_POOL").performClick()
        compose.runOnIdle {
            assertEquals(1, hosts)
            assertEquals(1, locationCalls.get())
        }
    }

    @Test fun permanentLocationDenialKeepsReadyMapAndSettingsOverlayCancelOrBackPreservesFact() {
        val workspace = TripWorkspaceViewModel("trip", Trips(), Places(), Itineraries(), Legs(), SavedStateHandle())
        val coordinator = LocationPermissionCoordinator(InMemoryLocationPermissionRequestStore(hasRequested = true))
        coordinator.attachWorkspace("trip")
        var rendered = 0
        var destroyed = 0
        var settingsEffects = 0
        compose.setContent {
            TripWorkspaceRoute(
                viewModel = workspace,
                consent = consentToken(),
                onBack = {},
                onSettings = {},
                locationPermissionCoordinator = coordinator,
                locationPermissionSnapshot = { com.yangchengwei.easytrip.permission.LocationPermissionSnapshot(false, false) },
                onWorkspaceEffect = { effect ->
                    if (effect is com.yangchengwei.easytrip.permission.WorkspaceEffect.OpenApplicationSettings) settingsEffects++
                },
                mapHostFactory = { context ->
                    object : AmapMapHost {
                        override val view = View(context)
                        override fun canRenderBeforeReady() = true
                        override fun onCreate() = Unit
                        override fun onResume() = Unit
                        override fun onPause() = Unit
                        override fun onDestroy() { destroyed++ }
                        override fun render(
                            model: MapUiModel,
                            layer: MapLayer,
                            onMarkerClick: (String) -> Unit,
                            onMapPoiClick: (MapPoiUi) -> Unit,
                            onLayerError: (Throwable, MapLayer) -> Unit,
                        ) { rendered++ }
                    }
                },
            )
        }
        compose.waitUntil(5_000) { rendered > 0 }

        compose.onNodeWithTag("workspace-locate").performClick()
        compose.onNodeWithText("定位权限未开启").assertIsDisplayed()
        compose.onNodeWithTag("workspace-map").assertIsDisplayed()
        compose.onAllNodesWithTag("workspace-map-fallback").assertCountEquals(0)
        compose.runOnIdle { assertEquals(0, destroyed) }

        compose.onNodeWithText("取消").performClick()
        compose.waitUntil(5_000) { workspace.state.value.overlay == WorkspaceOverlay.None }
        compose.runOnIdle { assertTrue(coordinator.uiState.value.permanentlyDenied) }

        compose.onNodeWithTag("workspace-locate").performClick()
        compose.onNodeWithText("前往设置").performClick()
        compose.waitUntil(5_000) { settingsEffects == 1 }
        compose.onNodeWithTag("location-settings-open").assertIsNotEnabled().performClick()
        compose.runOnIdle { assertEquals(1, settingsEffects) }
        pressBack()
        compose.waitUntil(5_000) { workspace.state.value.overlay == WorkspaceOverlay.None }
        compose.runOnIdle {
            assertTrue(coordinator.uiState.value.permanentlyDenied)
            assertEquals(0, destroyed)
        }
    }

    @Test fun permissionLauncherFailureKeepsSingleOverlayShowsErrorAndAllowsRetry() {
        val workspace = TripWorkspaceViewModel("trip", Trips(), Places(), Itineraries(), Legs(), SavedStateHandle())
        val coordinator = LocationPermissionCoordinator(InMemoryLocationPermissionRequestStore())
        coordinator.attachWorkspace("trip")
        var permissionEffects = 0
        compose.setContent {
            TripWorkspaceRoute(
                viewModel = workspace,
                consent = consentToken(),
                onBack = {},
                onSettings = {},
                locationPermissionCoordinator = coordinator,
                locationPermissionSnapshot = { LocationPermissionSnapshot(false, true) },
                onWorkspaceEffect = { effect ->
                    if (effect is com.yangchengwei.easytrip.permission.WorkspaceEffect.RequestLocationPermission) {
                        permissionEffects++
                        coordinator.onPermissionLaunchFailed(effect.generation)
                    }
                },
                mapHostFactory = ::TestMapHost,
            )
        }

        compose.onNodeWithTag("workspace-locate").performClick()
        compose.onNodeWithText("继续").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("无法打开系统权限请求，请重试").fetchSemanticsNodes().isNotEmpty()
        }

        compose.onNodeWithText("无法打开系统权限请求，请重试")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, androidx.compose.ui.semantics.LiveRegionMode.Polite))
        compose.onAllNodesWithText("允许 Easy Trip 获取你的位置").assertCountEquals(1)
        compose.onNodeWithTag("permission-explanation-confirm").assertIsEnabled().performClick()
        compose.waitUntil(5_000) { permissionEffects == 2 }
    }

    @Test fun settingsLauncherFailureKeepsSingleOverlayShowsErrorAndAllowsRetry() {
        val workspace = TripWorkspaceViewModel("trip", Trips(), Places(), Itineraries(), Legs(), SavedStateHandle())
        val coordinator = LocationPermissionCoordinator(InMemoryLocationPermissionRequestStore(hasRequested = true))
        coordinator.attachWorkspace("trip")
        var settingsEffects = 0
        compose.setContent {
            TripWorkspaceRoute(
                viewModel = workspace,
                consent = consentToken(),
                onBack = {},
                onSettings = {},
                locationPermissionCoordinator = coordinator,
                locationPermissionSnapshot = { LocationPermissionSnapshot(false, false) },
                onWorkspaceEffect = { effect ->
                    if (effect is com.yangchengwei.easytrip.permission.WorkspaceEffect.OpenApplicationSettings) {
                        settingsEffects++
                        coordinator.onSettingsLaunchFailed(effect.generation)
                    }
                },
                mapHostFactory = ::TestMapHost,
            )
        }

        compose.onNodeWithTag("workspace-locate").performClick()
        compose.onNodeWithText("前往设置").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("无法打开应用设置，请重试").fetchSemanticsNodes().isNotEmpty()
        }

        compose.onNodeWithText("无法打开应用设置，请重试")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, androidx.compose.ui.semantics.LiveRegionMode.Polite))
        compose.onAllNodesWithText("定位权限未开启").assertCountEquals(1)
        compose.onNodeWithTag("location-settings-open").assertIsEnabled().performClick()
        compose.waitUntil(5_000) { settingsEffects == 2 }
    }

    @Test fun mapFailureKeepsLocalTabsAndActionsReachable() {
        val model = TripWorkspaceViewModel("trip", Trips(), Places(), Itineraries(), Legs(), SavedStateHandle())
        compose.setContent {
            TripWorkspaceScreen(
                viewModel = model,
                consent = consentToken(),
                onBack = {},
                onSettings = {},
                placeContent = { Text("地点内容") },
                dayItineraryContent = { Text("行程内容") },
                mapHostFactory = ::FailingMapHost,
            )
        }

        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("workspace-map-fallback").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("workspace-search-launcher").assertIsDisplayed().assertHasClickAction()
        compose.onNodeWithTag("section-ITINERARY").assertIsDisplayed().assertHasClickAction().performClick().assertIsSelected()
        compose.onNodeWithText("行程内容").assertIsDisplayed()
        compose.onNodeWithTag("section-PLACE_POOL").assertIsDisplayed().assertHasClickAction().performClick().assertIsSelected()
        compose.onNodeWithText("地点内容").assertIsDisplayed()
    }

    @Test fun persistedSatelliteFailureRemainsVisibleAfterMapReadyThenAutoDismisses() {
        compose.mainClock.autoAdvance = false
        try {
            val preferences = InMemoryMapPreferences(MapLayer.SATELLITE)
            val model = TripWorkspaceViewModel(
                "trip", Trips(), Places(), Itineraries(), Legs(), SavedStateHandle(), mapPreferences = preferences,
            )
            compose.setContent {
                TripWorkspaceScreen(
                    viewModel = model,
                    consent = consentToken(),
                    onBack = {},
                    onSettings = {},
                    placeContent = { Text("地点内容") },
                    dayItineraryContent = { Text("行程内容") },
                    mapHostFactory = { context -> FirstSatelliteFailingMapHost(context) },
                )
            }

            compose.mainClock.advanceTimeByFrame()
            compose.onNodeWithTag("map-layer-failure").assertIsDisplayed()
            compose.onNodeWithTag("workspace-map-fallback").assertDoesNotExist()
            compose.mainClock.advanceTimeBy(4_001)
            compose.onNodeWithTag("map-layer-failure").assertDoesNotExist()
        } finally {
            compose.mainClock.autoAdvance = true
        }
    }

    @Test fun layerFailureRestoresPreferenceThenNextSuccessfulSelectionDismissesLocalFeedback() {
        val preferences = InMemoryMapPreferences()
        val renderedLayers = mutableListOf<MapLayer>()
        val model = TripWorkspaceViewModel(
            "trip", Trips(), Places(), Itineraries(), Legs(), SavedStateHandle(), mapPreferences = preferences,
        )
        compose.setContent {
            TripWorkspaceScreen(
                viewModel = model,
                consent = consentToken(),
                onBack = {},
                onSettings = {},
                placeContent = { Text("地点内容") },
                dayItineraryContent = { Text("行程内容") },
                mapHostFactory = { context -> LayerFailingMapHost(context, renderedLayers) },
            )
        }

        compose.waitUntil(5_000) { compose.onAllNodesWithTag("layer-menu").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("layer-menu").performClick()
        compose.onNodeWithTag("layer-SATELLITE").performClick()
        compose.waitUntil(5_000) {
            renderedLayers.contains(MapLayer.SATELLITE) &&
                preferences.layer.value == MapLayer.STANDARD &&
                renderedLayers.lastOrNull() == MapLayer.STANDARD
        }

        compose.onNodeWithText("图层切换失败，已保留当前图层").assertIsDisplayed()
        compose.onNodeWithTag("workspace-map-fallback").assertDoesNotExist()
        compose.onNodeWithTag("map-retry").assertDoesNotExist()

        compose.onNodeWithTag("layer-menu").performClick()
        compose.onNodeWithTag("layer-SATELLITE_ROAD").performClick()
        compose.waitUntil(5_000) { preferences.layer.value == MapLayer.SATELLITE_ROAD }
        compose.onNodeWithTag("map-layer-failure").assertDoesNotExist()
    }

    @Test fun routeBackClosesOverlayBeforeLeavingAndTopBackUsesSamePriority() {
        val model = TripWorkspaceViewModel("trip", Trips(), Places(), Itineraries(), Legs(), SavedStateHandle())
        var backCount = 0
        compose.setContent {
            TripWorkspaceRoute(
                viewModel = model,
                consent = null,
                onBack = { backCount++ },
                onSettings = {},
                locationPermissionCoordinator = LocationPermissionCoordinator(InMemoryLocationPermissionRequestStore()),
                locationPermissionSnapshot = { com.yangchengwei.easytrip.permission.LocationPermissionSnapshot(false, false) },
                onWorkspaceEffect = {},
                placeState = com.yangchengwei.easytrip.place.ui.PlacePoolUiState(),
                onPlaceAction = {},
                itineraryState = com.yangchengwei.easytrip.itinerary.ui.DayItineraryUiState(),
                onItineraryAction = {},
            )
        }
        compose.waitUntil(5_000) { model.pageState.value is TripWorkspacePageState.Ready }

        compose.onNodeWithTag("layer-menu").performClick()
        compose.waitUntil(5_000) {
            (model.pageState.value as? TripWorkspacePageState.Ready)?.content?.overlay == WorkspaceOverlay.LayerMenu
        }
        compose.onNodeWithTag("layer-menu-panel").assertIsDisplayed()
        compose.runOnIdle {
            assert(compose.activity.onBackPressedDispatcher.hasEnabledCallbacks())
        }
        pressBack()
        compose.waitUntil(5_000) {
            (model.pageState.value as? TripWorkspacePageState.Ready)?.content?.overlay == WorkspaceOverlay.None
        }
        assertEquals(0, backCount)
        pressBack()
        compose.waitUntil(5_000) { backCount == 1 }

        compose.onNodeWithTag("layer-menu").performClick()
        compose.waitUntil(5_000) {
            (model.pageState.value as? TripWorkspacePageState.Ready)?.content?.overlay == WorkspaceOverlay.LayerMenu
        }
        compose.onNodeWithTag("workspace-back").performClick()
        compose.waitUntil(5_000) {
            (model.pageState.value as? TripWorkspacePageState.Ready)?.content?.overlay == WorkspaceOverlay.None
        }
        assertEquals(1, backCount)
        compose.onNodeWithTag("workspace-back").performClick()
        compose.waitUntil(5_000) { backCount == 2 }
    }

    @Test fun savedPlaceRowOpensBottomSheetWithoutRecreatingMapOrViewport() {
        val workspace = TripWorkspaceViewModel("trip", Trips(), Places(), Itineraries(), Legs(), SavedStateHandle())
        val placeModel = com.yangchengwei.easytrip.place.ui.PlacePoolViewModel("trip", Places(), null)
        val hostCreations = AtomicInteger()
        lateinit var host: TestMapHost
        compose.setContent {
            TripWorkspaceRoute(
                viewModel = workspace,
                consent = consentToken(),
                onBack = {},
                onSettings = {},
                locationPermissionCoordinator = LocationPermissionCoordinator(InMemoryLocationPermissionRequestStore()),
                locationPermissionSnapshot = { com.yangchengwei.easytrip.permission.LocationPermissionSnapshot(false, false) },
                onWorkspaceEffect = {},
                placeViewModel = placeModel,
                mapHostFactory = { context ->
                    hostCreations.incrementAndGet()
                    TestMapHost(context).also { host = it }
                },
            )
        }
        compose.waitUntil(5_000) {
            placeModel.state.value.rows.isNotEmpty() &&
                hostCreations.get() == 1 &&
                runCatching { host.viewportCalls }.getOrDefault(0) == 1
        }
        val viewportBeforeOpen = workspace.state.value.map.viewportRequest
        val viewportCallsBeforeOpen = host.viewportCalls

        compose.onNodeWithTag("open-place-detail-p").performClick()
        compose.waitUntil(5_000) {
            workspace.state.value.overlay is WorkspaceOverlay.PlaceDetail &&
                placeModel.state.value.selectedDetailPlaceId == "p" &&
                compose.onAllNodesWithTag("place-detail-bottom-sheet").fetchSemanticsNodes().size == 1
        }

        compose.onNodeWithTag("place-detail-bottom-sheet").assertIsDisplayed()
        compose.onNodeWithTag("place-detail-bottom-sheet")
            .assert(hasAnyAncestor(hasTestTag("workspace-screen-root")))
        compose.onNodeWithTag("workspace-map").assertIsDisplayed()
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.IsDialog)).assertCountEquals(0)
        val sheetBounds = compose.onNodeWithTag("place-detail-bottom-sheet").getUnclippedBoundsInRoot()
        val mapBounds = compose.onNodeWithTag("workspace-map").getUnclippedBoundsInRoot()
        compose.runOnIdle {
            assertTrue("map=$mapBounds sheet=$sheetBounds", sheetBounds.top > mapBounds.top)
            assertEquals(mapBounds.bottom, sheetBounds.bottom)
            assertEquals(1, hostCreations.get())
            assertEquals(viewportCallsBeforeOpen, host.viewportCalls)
            assertEquals(viewportBeforeOpen, workspace.state.value.map.viewportRequest)
        }

        pressBack()
        compose.waitUntil(5_000) { placeModel.state.value.selectedDetailPlaceId == null }
        compose.runOnIdle {
            assertEquals(1, hostCreations.get())
            assertEquals(viewportCallsBeforeOpen, host.viewportCalls)
            assertEquals(viewportBeforeOpen, workspace.state.value.map.viewportRequest)
        }
    }

    @Test fun savedMarkerOpensTheSameBottomSheetWithoutNewViewportRequest() {
        val workspace = TripWorkspaceViewModel("trip", Trips(), Places(), Itineraries(), Legs(), SavedStateHandle())
        val placeModel = com.yangchengwei.easytrip.place.ui.PlacePoolViewModel("trip", Places(), null)
        lateinit var host: MarkerClickHost
        compose.setContent {
            TripWorkspaceRoute(
                viewModel = workspace,
                consent = consentToken(),
                onBack = {},
                onSettings = {},
                locationPermissionCoordinator = LocationPermissionCoordinator(InMemoryLocationPermissionRequestStore()),
                locationPermissionSnapshot = { com.yangchengwei.easytrip.permission.LocationPermissionSnapshot(false, false) },
                onWorkspaceEffect = {},
                placeViewModel = placeModel,
                mapHostFactory = { context -> MarkerClickHost(context).also { host = it } },
            )
        }
        compose.waitUntil(5_000) {
            placeModel.state.value.rows.isNotEmpty() &&
                runCatching { host.hasSavedPlaceMarker() && host.viewportCalls == 1 }.getOrDefault(false)
        }
        val viewportBeforeOpen = workspace.state.value.map.viewportRequest
        val viewportCallsBeforeOpen = host.viewportCalls

        compose.runOnIdle { host.clickSavedPlaceMarker() }

        compose.waitUntil(5_000) {
            placeModel.state.value.selectedDetailPlaceId == "p" &&
                compose.onAllNodesWithTag("place-detail-bottom-sheet").fetchSemanticsNodes().size == 1
        }
        compose.runOnIdle {
            assertEquals(viewportCallsBeforeOpen, host.viewportCalls)
            assertEquals(viewportBeforeOpen, workspace.state.value.map.viewportRequest)
        }
    }

    @Test fun editingStaysInTheSameSheetAndSavingBlocksBackUntilCompletion() {
        val workspace = TripWorkspaceViewModel("trip", Trips(), Places(), Itineraries(), Legs(), SavedStateHandle())
        val repository = DelayedUpdatePlaces()
        val placeModel = com.yangchengwei.easytrip.place.ui.PlacePoolViewModel("trip", repository, null)
        val hostCreations = AtomicInteger()
        lateinit var host: TestMapHost
        var leaveCalls = 0
        compose.setContent {
            TripWorkspaceRoute(
                viewModel = workspace,
                consent = consentToken(),
                onBack = { leaveCalls++ },
                onSettings = {},
                locationPermissionCoordinator = LocationPermissionCoordinator(InMemoryLocationPermissionRequestStore()),
                locationPermissionSnapshot = { com.yangchengwei.easytrip.permission.LocationPermissionSnapshot(false, false) },
                onWorkspaceEffect = {},
                placeViewModel = placeModel,
                mapHostFactory = { context ->
                    hostCreations.incrementAndGet()
                    TestMapHost(context).also { host = it }
                },
            )
        }
        compose.waitUntil(5_000) {
            placeModel.state.value.rows.isNotEmpty() &&
                hostCreations.get() == 1 &&
                runCatching { host.viewportCalls }.getOrDefault(0) == 1
        }
        val viewportCallsBeforeEdit = host.viewportCalls
        compose.onNodeWithTag("open-place-detail-saved").performClick()
        compose.onNodeWithText("编辑").performClick()

        compose.waitUntil(5_000) { placeModel.state.value.editing?.id == "saved" }
        compose.onAllNodesWithTag("place-detail-bottom-sheet").assertCountEquals(1)
        compose.onNodeWithTag("place-detail-note-input").performTextInput("保存中的备注")
        compose.onNodeWithTag("place-detail-save").performClick()
        compose.waitUntil(5_000) { placeModel.state.value.detailSaving }

        pressBack()
        compose.waitForIdle()
        compose.onNodeWithTag("workspace-back").performClick()
        compose.waitForIdle()

        compose.onNodeWithTag("place-detail-bottom-sheet").assertIsDisplayed()
        compose.onNodeWithTag("place-detail-dismiss").assertIsNotEnabled()
        compose.runOnIdle {
            assertEquals(0, leaveCalls)
            assertEquals(1, hostCreations.get())
            assertEquals(viewportCallsBeforeEdit, host.viewportCalls)
            assertEquals(1, repository.updateCalls)
        }

        compose.runOnIdle { repository.updateGate.complete(Unit) }
        compose.waitUntil(5_000) {
            placeModel.state.value.editing == null && workspace.state.value.overlay == WorkspaceOverlay.None
        }
        compose.runOnIdle {
            assertEquals(1, hostCreations.get())
            assertEquals(viewportCallsBeforeEdit, host.viewportCalls)
        }
    }

    @Test fun detailAddOpensTargetDayForExistingTravelDays() {
        val workspace = TripWorkspaceViewModel("trip", Trips(), Places(), Itineraries(), Legs(), SavedStateHandle())
        val add = AddToItineraryViewModel("trip", AddPlacesToDayUseCase(Itineraries()), UndoAddedItemsUseCase(Itineraries()), SavedStateHandle())
        val placeModel = com.yangchengwei.easytrip.place.ui.PlacePoolViewModel("trip", Places(), null)
        compose.setContent {
            TripWorkspaceRoute(
                viewModel = workspace, consent = null, onBack = {}, onSettings = {},
                locationPermissionCoordinator = LocationPermissionCoordinator(InMemoryLocationPermissionRequestStore()),
                locationPermissionSnapshot = { com.yangchengwei.easytrip.permission.LocationPermissionSnapshot(false, false) },
                onWorkspaceEffect = {}, placeViewModel = placeModel, addToItineraryViewModel = add,
            )
        }
        compose.waitUntil(5_000) { placeModel.state.value.rows.isNotEmpty() }
        compose.onNodeWithTag("open-place-detail-p").performClick()
        compose.onNodeWithTag("place-detail-start-add").performClick()

        compose.waitUntil(5_000) {
            workspace.state.value.overlay == WorkspaceOverlay.SelectAddTargetDay &&
                add.state.value.selectedPlaceIds == listOf("p") &&
                placeModel.state.value.selectedDetailPlaceId == null
        }
        compose.onNodeWithTag("place-detail-bottom-sheet").assertDoesNotExist()
    }

    @Test fun detailAddOpensNoDayGuidanceWhenTripHasNoDays() {
        val workspace = TripWorkspaceViewModel("trip", NoDayTrips(), Places(), Itineraries(), Legs(), SavedStateHandle())
        val placeModel = com.yangchengwei.easytrip.place.ui.PlacePoolViewModel("trip", Places(), null)
        val repository = Itineraries()
        val add = AddToItineraryViewModel("trip", AddPlacesToDayUseCase(repository), UndoAddedItemsUseCase(repository), SavedStateHandle())
        compose.setContent {
            TripWorkspaceRoute(
                viewModel = workspace, consent = null, onBack = {}, onSettings = {},
                locationPermissionCoordinator = LocationPermissionCoordinator(InMemoryLocationPermissionRequestStore()),
                locationPermissionSnapshot = { com.yangchengwei.easytrip.permission.LocationPermissionSnapshot(false, false) },
                onWorkspaceEffect = {}, placeViewModel = placeModel, addToItineraryViewModel = add,
            )
        }
        compose.waitUntil(5_000) { placeModel.state.value.rows.isNotEmpty() }
        compose.onNodeWithTag("open-place-detail-p").performClick()
        compose.onNodeWithTag("place-detail-start-add").performClick()

        compose.waitUntil(5_000) {
            workspace.state.value.overlay == WorkspaceOverlay.SelectAddTargetDay &&
                compose.onAllNodesWithTag("go-to-itinerary-add-day").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test fun placeQuickAddOpensTargetDayOnlyForValidPlaceWithAvailableDay() {
        val workspace = TripWorkspaceViewModel("trip", Trips(), Places(), Itineraries(), Legs(), SavedStateHandle())
        val repository = Itineraries()
        val add = AddToItineraryViewModel(
            "trip",
            AddPlacesToDayUseCase(repository),
            UndoAddedItemsUseCase(repository),
            SavedStateHandle(),
        )
        compose.setContent {
            TripWorkspaceRoute(
                viewModel = workspace,
                consent = null,
                onBack = {},
                onSettings = {},
                locationPermissionCoordinator = LocationPermissionCoordinator(InMemoryLocationPermissionRequestStore()),
                locationPermissionSnapshot = { com.yangchengwei.easytrip.permission.LocationPermissionSnapshot(false, false) },
                onWorkspaceEffect = {},
                placeState = com.yangchengwei.easytrip.place.ui.PlacePoolUiState(
                    rows = listOf(com.yangchengwei.easytrip.place.ui.SavedPlaceRowUi(SavedPlace("p", "trip", "poi", "酒店", "地址", GeoPoint(1.0, 2.0), "", emptyList()), 0, false)),
                ),
                addToItineraryViewModel = add,
            )
        }
        compose.waitUntil(5_000) { workspace.pageState.value is TripWorkspacePageState.Ready }

        compose.onNodeWithTag("quick-add-place-p").performClick()

        compose.waitUntil(5_000) {
            add.state.value.step == AddToItineraryStep.SELECT_TARGET_DAY &&
                workspace.state.value.overlay == WorkspaceOverlay.SelectAddTargetDay
        }
        assertEquals(listOf("p"), add.state.value.selectedPlaceIds)
    }

    @Test fun selectedDayAddOpensPlaceSelection() {
        val workspace = TripWorkspaceViewModel("trip", Trips(), Places(), Itineraries(), Legs(), SavedStateHandle())
        val repository = Itineraries()
        val add = AddToItineraryViewModel("trip", AddPlacesToDayUseCase(repository), UndoAddedItemsUseCase(repository), SavedStateHandle())
        compose.setContent {
            TripWorkspaceRoute(
                viewModel = workspace,
                consent = null,
                onBack = {},
                onSettings = {},
                locationPermissionCoordinator = LocationPermissionCoordinator(InMemoryLocationPermissionRequestStore()),
                locationPermissionSnapshot = { com.yangchengwei.easytrip.permission.LocationPermissionSnapshot(false, false) },
                onWorkspaceEffect = {},
                itineraryState = DayItineraryUiState(
                    days = listOf(TripDay("day-1", 1)),
                    selectedDayId = "day-1",
                ),
                addToItineraryViewModel = add,
            )
        }
        compose.waitUntil(5_000) { workspace.pageState.value is TripWorkspacePageState.Ready }
        compose.onNodeWithTag("section-ITINERARY").performClick()

        compose.onNodeWithTag("add-places-to-selected-day").performClick()

        compose.waitUntil(5_000) { workspace.state.value.overlay == WorkspaceOverlay.SelectAddPlaces }
        assertEquals(com.yangchengwei.easytrip.itinerary.ui.AddToItineraryEditingTarget.ForDay("day-1"), add.state.value.editingTarget)
    }

    @Test fun selectedDayProductionRouteShowsResultAndRestoredUndo() {
        val workspace = TripWorkspaceViewModel("trip", Trips(), Places(), Itineraries(), Legs(), SavedStateHandle())
        val repository = Itineraries()
        val saved = SavedStateHandle()
        val add = androidx.compose.runtime.mutableStateOf(
            AddToItineraryViewModel("trip", AddPlacesToDayUseCase(repository), UndoAddedItemsUseCase(repository), saved),
        )
        val placeState = com.yangchengwei.easytrip.place.ui.PlacePoolUiState(
            rows = listOf(
                com.yangchengwei.easytrip.place.ui.SavedPlaceRowUi(
                    SavedPlace("p", "trip", "poi", "酒店", "地址", GeoPoint(1.0, 2.0), "", emptyList()),
                    0,
                    false,
                ),
            ),
        )
        compose.setContent {
            TripWorkspaceRoute(
                viewModel = workspace,
                consent = null,
                onBack = {},
                onSettings = {},
                locationPermissionCoordinator = LocationPermissionCoordinator(InMemoryLocationPermissionRequestStore()),
                locationPermissionSnapshot = { com.yangchengwei.easytrip.permission.LocationPermissionSnapshot(false, false) },
                onWorkspaceEffect = {},
                itineraryState = DayItineraryUiState(days = listOf(TripDay("day-1", 0)), selectedDayId = "day-1"),
                placeState = placeState,
                addToItineraryViewModel = add.value,
            )
        }
        compose.waitUntil(5_000) { workspace.pageState.value is TripWorkspacePageState.Ready }
        compose.onNodeWithTag("section-ITINERARY").performClick()
        compose.onNodeWithTag("add-places-to-selected-day").performClick()
        compose.onNodeWithText("酒店").performClick()
        compose.onNodeWithText("继续").performClick()

        compose.onNodeWithText("已加入第 1 天").assertIsDisplayed()
        compose.onNodeWithText("撤销").assertIsDisplayed()

        compose.runOnIdle {
            add.value = AddToItineraryViewModel(
                "trip",
                AddPlacesToDayUseCase(repository),
                UndoAddedItemsUseCase(repository),
                SavedStateHandle(),
            )
            workspace.closeOverlay()
        }
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("已加入第 1 天").fetchSemanticsNodes().isEmpty()
        }
        val restored = AddToItineraryViewModel(
            "trip",
            AddPlacesToDayUseCase(repository),
            UndoAddedItemsUseCase(repository),
            saved,
        )
        compose.runOnIdle { add.value = restored }
        compose.waitUntil(5_000) {
            restored.state.value.step == AddToItineraryStep.COMPLETED &&
                compose.onAllNodesWithText("已加入第 1 天").fetchSemanticsNodes().isNotEmpty()
        }

        compose.onNodeWithText("已加入第 1 天").assertIsDisplayed()
        compose.onNodeWithText("撤销").assertIsDisplayed()
    }

    @Test fun loadingWorkspaceDoesNotDestructivelyReconcileRestoredAddDraft() {
        val trips = DeferredTrips()
        val workspace = TripWorkspaceViewModel("trip", trips, Places(), Itineraries(), Legs(), SavedStateHandle())
        val add = AddToItineraryViewModel(
            "trip",
            AddPlacesToDayUseCase(Itineraries()),
            UndoAddedItemsUseCase(Itineraries()),
            SavedStateHandle(),
        )
        add.reconcile(listOf("day-1"), setOf("p"))
        add.startForPlace("p")
        add.toggleTargetDay("day-1")
        compose.setContent {
            TripWorkspaceRoute(
                viewModel = workspace,
                consent = null,
                onBack = {},
                onSettings = {},
                locationPermissionCoordinator = LocationPermissionCoordinator(InMemoryLocationPermissionRequestStore()),
                locationPermissionSnapshot = { com.yangchengwei.easytrip.permission.LocationPermissionSnapshot(false, false) },
                onWorkspaceEffect = {},
                placeState = com.yangchengwei.easytrip.place.ui.PlacePoolUiState(),
                addToItineraryViewModel = add,
            )
        }
        compose.onNodeWithText("旅行加载中").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(AddToItineraryStep.SELECT_TARGET_DAY, add.state.value.step)
            assertEquals(listOf("p"), add.state.value.selectedPlaceIds)
            assertEquals(listOf("day-1"), add.state.value.selectedTargetDayIds)
        }
    }

    @Test fun readyWorkspaceWaitsForAuthoritativePlacesBeforeDestructiveReconcile() {
        val workspace = TripWorkspaceViewModel("trip", Trips(), Places(), Itineraries(), Legs(), SavedStateHandle())
        val placeViewModel = com.yangchengwei.easytrip.place.ui.PlacePoolViewModel("trip", DeferredPlaces(), null)
        val add = AddToItineraryViewModel(
            "trip",
            AddPlacesToDayUseCase(Itineraries()),
            UndoAddedItemsUseCase(Itineraries()),
            SavedStateHandle(),
        )
        add.reconcile(listOf("day-1", "day-2"), setOf("p"))
        add.startForPlace("p")
        add.toggleTargetDay("day-1")
        compose.setContent {
            TripWorkspaceRoute(
                viewModel = workspace,
                consent = null,
                onBack = {},
                onSettings = {},
                locationPermissionCoordinator = LocationPermissionCoordinator(InMemoryLocationPermissionRequestStore()),
                locationPermissionSnapshot = { com.yangchengwei.easytrip.permission.LocationPermissionSnapshot(false, false) },
                onWorkspaceEffect = {},
                placeViewModel = placeViewModel,
                addToItineraryViewModel = add,
            )
        }
        compose.waitUntil(5_000) { workspace.pageState.value is TripWorkspacePageState.Ready }

        compose.runOnIdle {
            assertEquals(AddToItineraryStep.SELECT_TARGET_DAY, add.state.value.step)
            assertEquals(listOf("p"), add.state.value.selectedPlaceIds)
            assertEquals(listOf("day-1"), add.state.value.selectedTargetDayIds)
        }
    }

    @Test fun failedQuickAddDoesNotExposeOldBulkTargetDayDraft() {
        val workspace = TripWorkspaceViewModel("trip", Trips(), Places(), Itineraries(), Legs(), SavedStateHandle())
        val repository = Itineraries()
        val add = AddToItineraryViewModel("trip", AddPlacesToDayUseCase(repository), UndoAddedItemsUseCase(repository), SavedStateHandle())
        add.reconcile(listOf("day-1"), setOf("invalid"))
        add.startFromPool()
        add.togglePlace("invalid")
        add.continueToTargetDay()
        compose.setContent {
            TripWorkspaceRoute(
                viewModel = workspace,
                consent = null,
                onBack = {},
                onSettings = {},
                locationPermissionCoordinator = LocationPermissionCoordinator(InMemoryLocationPermissionRequestStore()),
                locationPermissionSnapshot = { com.yangchengwei.easytrip.permission.LocationPermissionSnapshot(false, false) },
                onWorkspaceEffect = {},
                placeState = com.yangchengwei.easytrip.place.ui.PlacePoolUiState(
                    rows = listOf(com.yangchengwei.easytrip.place.ui.SavedPlaceRowUi(SavedPlace("invalid", "trip", "poi", "无效地点", "地址", GeoPoint(1.0, 2.0), "", emptyList()), 0, false)),
                ),
                addToItineraryViewModel = add,
            )
        }
        compose.waitUntil(5_000) { workspace.pageState.value is TripWorkspacePageState.Ready }
        compose.onNodeWithTag("quick-add-place-invalid").performClick()
        compose.waitForIdle()

        assertEquals(WorkspaceOverlay.None, workspace.state.value.overlay)
        assertEquals(listOf("invalid"), add.state.value.selectedPlaceIds)
    }

    @Test fun invalidPlaceDoesNotOpenTargetDayAndNoDayQuickAddGuidesToAddDay() {
        fun render(trips: TripRepository, rowId: String) : Pair<TripWorkspaceViewModel, AddToItineraryViewModel> {
            val workspace = TripWorkspaceViewModel("trip", trips, Places(), Itineraries(), Legs(), SavedStateHandle())
            val repository = Itineraries()
            val add = AddToItineraryViewModel("trip", AddPlacesToDayUseCase(repository), UndoAddedItemsUseCase(repository), SavedStateHandle())
            compose.setContent {
                TripWorkspaceRoute(
                    viewModel = workspace,
                    consent = null,
                    onBack = {},
                    onSettings = {},
                    locationPermissionCoordinator = LocationPermissionCoordinator(InMemoryLocationPermissionRequestStore()),
                    locationPermissionSnapshot = { com.yangchengwei.easytrip.permission.LocationPermissionSnapshot(false, false) },
                    onWorkspaceEffect = {},
                    placeState = com.yangchengwei.easytrip.place.ui.PlacePoolUiState(
                        rows = listOf(com.yangchengwei.easytrip.place.ui.SavedPlaceRowUi(SavedPlace(rowId, "trip", "poi-$rowId", "地点$rowId", "地址", GeoPoint(1.0, 2.0), "", emptyList()), 0, false)),
                    ),
                    addToItineraryViewModel = add,
                )
            }
            compose.waitUntil(5_000) { workspace.pageState.value is TripWorkspacePageState.Ready }
            return workspace to add
        }

        val invalidRepository = Itineraries()
        val invalidAdd = AddToItineraryViewModel("trip", AddPlacesToDayUseCase(invalidRepository), UndoAddedItemsUseCase(invalidRepository), SavedStateHandle())
        invalidAdd.reconcile(listOf("day-1"), setOf("p"))
        invalidAdd.startForPlace("invalid")
        assertEquals(AddToItineraryStep.IDLE, invalidAdd.state.value.step)
        assertEquals(null, addOverlayToPresent(WorkspaceOverlay.None, invalidAdd.state.value))

        val (noDayWorkspace, noDayAdd) = render(NoDayTrips(), "p")
        compose.onNodeWithTag("quick-add-place-p").performClick()
        compose.waitUntil(5_000) {
            noDayWorkspace.state.value.overlay == WorkspaceOverlay.SelectAddTargetDay &&
                noDayAdd.state.value.selectedPlaceIds == listOf("p")
        }
        compose.onNodeWithTag("go-to-itinerary-add-day").assertIsDisplayed()
    }

    @Test fun singlePlaceWithoutDaysKeepsIntentAndGuidesToItineraryAddDay() {
        val workspace = TripWorkspaceViewModel("trip", NoDayTrips(), Places(), Itineraries(), Legs(), SavedStateHandle())
        val repository = Itineraries()
        val add = AddToItineraryViewModel("trip", AddPlacesToDayUseCase(repository), UndoAddedItemsUseCase(repository), SavedStateHandle())
        compose.setContent {
            TripWorkspaceRoute(
                viewModel = workspace, consent = null, onBack = {}, onSettings = {},
                locationPermissionCoordinator = LocationPermissionCoordinator(InMemoryLocationPermissionRequestStore()),
                locationPermissionSnapshot = { com.yangchengwei.easytrip.permission.LocationPermissionSnapshot(false, false) },
                onWorkspaceEffect = {},
                placeState = com.yangchengwei.easytrip.place.ui.PlacePoolUiState(
                    rows = listOf(com.yangchengwei.easytrip.place.ui.SavedPlaceRowUi(SavedPlace("p", "trip", "poi", "地点", "地址", GeoPoint(1.0, 2.0), "", emptyList()), 0, false)),
                ),
                addToItineraryViewModel = add,
            )
        }
        compose.waitUntil(5_000) { workspace.pageState.value is TripWorkspacePageState.Ready }

        compose.onNodeWithTag("quick-add-place-p").performClick()

        compose.waitUntil(5_000) {
            workspace.state.value.overlay == WorkspaceOverlay.SelectAddTargetDay &&
                add.state.value.selectedPlaceIds == listOf("p")
        }
        compose.onNodeWithTag("go-to-itinerary-add-day").performClick()
        compose.waitUntil(5_000) { workspace.state.value.overlay == WorkspaceOverlay.AddTripDay }
        assertEquals(WorkspaceSection.ITINERARY, workspace.state.value.section)
        assertEquals(listOf("p"), add.state.value.selectedPlaceIds)
        compose.onNodeWithText("取消").performClick()
        compose.waitUntil(5_000) { workspace.state.value.overlay == WorkspaceOverlay.None }
        assertEquals(AddToItineraryStep.IDLE, add.state.value.step)
    }

    @Test fun singlePlaceTogglesMultipleTargetDaysWithoutReplacingSelection() {
        val workspace = TripWorkspaceViewModel("trip", Trips(), Places(), Itineraries(), Legs(), SavedStateHandle())
        val repository = Itineraries()
        val add = AddToItineraryViewModel("trip", AddPlacesToDayUseCase(repository), UndoAddedItemsUseCase(repository), SavedStateHandle())
        compose.setContent {
            TripWorkspaceRoute(
                viewModel = workspace, consent = null, onBack = {}, onSettings = {},
                locationPermissionCoordinator = LocationPermissionCoordinator(InMemoryLocationPermissionRequestStore()),
                locationPermissionSnapshot = { com.yangchengwei.easytrip.permission.LocationPermissionSnapshot(false, false) },
                onWorkspaceEffect = {},
                placeState = com.yangchengwei.easytrip.place.ui.PlacePoolUiState(
                    rows = listOf(com.yangchengwei.easytrip.place.ui.SavedPlaceRowUi(SavedPlace("p", "trip", "poi", "地点", "地址", GeoPoint(1.0, 2.0), "", emptyList()), 0, false)),
                ),
                addToItineraryViewModel = add,
            )
        }
        compose.waitUntil(5_000) { workspace.pageState.value is TripWorkspacePageState.Ready }
        compose.onNodeWithTag("quick-add-place-p").performClick()
        compose.waitUntil(5_000) { workspace.state.value.overlay == WorkspaceOverlay.SelectAddTargetDay }
        compose.onNodeWithTag("target-day-day-1").performClick()
        compose.onNodeWithTag("target-day-day-2").performClick()

        compose.waitUntil(5_000) { add.state.value.selectedTargetDayIds == listOf("day-1", "day-2") }
    }

    @Test fun singlePlaceTargetDaysShowNameDateOccurrenceAndAccurateConfirmLabel() {
        val itineraryRepository = SchedulingItineraries()
        val workspace = TripWorkspaceViewModel("trip", DatedTrips(), Places(), itineraryRepository, Legs(), SavedStateHandle())
        val repository = itineraryRepository
        val add = AddToItineraryViewModel("trip", AddPlacesToDayUseCase(repository), UndoAddedItemsUseCase(repository), SavedStateHandle())
        compose.setContent {
            TripWorkspaceRoute(
                viewModel = workspace, consent = null, onBack = {}, onSettings = {},
                locationPermissionCoordinator = LocationPermissionCoordinator(InMemoryLocationPermissionRequestStore()),
                locationPermissionSnapshot = { com.yangchengwei.easytrip.permission.LocationPermissionSnapshot(false, false) },
                onWorkspaceEffect = {},
                placeState = com.yangchengwei.easytrip.place.ui.PlacePoolUiState(
                    rows = listOf(com.yangchengwei.easytrip.place.ui.SavedPlaceRowUi(SavedPlace("p", "trip", "poi", "地点", "地址", GeoPoint(1.0, 2.0), "", emptyList()), 0, false)),
                ),
                addToItineraryViewModel = add,
            )
        }
        compose.waitUntil(5_000) { workspace.pageState.value is TripWorkspacePageState.Ready }
        compose.onNodeWithTag("quick-add-place-p").performClick()
        compose.waitUntil(5_000) { workspace.state.value.overlay == WorkspaceOverlay.SelectAddTargetDay }

        compose.onAllNodesWithText("地点")[0].assertIsDisplayed()
        compose.onNodeWithText("第 1 天 · 8月25日").assertIsDisplayed()
        compose.onNodeWithText("已安排 1 次").assertIsDisplayed()
        compose.onNodeWithTag("select-target-day-submit").assertIsNotEnabled()
        compose.onNodeWithTag("target-day-day-1").performClick()
        compose.onNodeWithText("加入第 1 天").assertIsDisplayed()
        compose.onNodeWithTag("target-day-day-2").performClick()
        compose.onNodeWithText("加入 2 天").assertIsDisplayed()
    }

    @Test fun longTargetDayListKeepsConfirmReachableAndSelectsFinalDay() {
        val workspace = TripWorkspaceViewModel("trip", LongTrips(), Places(), Itineraries(), Legs(), SavedStateHandle())
        val repository = Itineraries()
        val add = AddToItineraryViewModel("trip", AddPlacesToDayUseCase(repository), UndoAddedItemsUseCase(repository), SavedStateHandle())
        compose.setContent {
            TripWorkspaceRoute(
                viewModel = workspace, consent = null, onBack = {}, onSettings = {},
                locationPermissionCoordinator = LocationPermissionCoordinator(InMemoryLocationPermissionRequestStore()),
                locationPermissionSnapshot = { com.yangchengwei.easytrip.permission.LocationPermissionSnapshot(false, false) },
                onWorkspaceEffect = {},
                placeState = com.yangchengwei.easytrip.place.ui.PlacePoolUiState(
                    rows = listOf(com.yangchengwei.easytrip.place.ui.SavedPlaceRowUi(SavedPlace("p", "trip", "poi", "地点", "地址", GeoPoint(1.0, 2.0), "", emptyList()), 0, false)),
                ),
                addToItineraryViewModel = add,
            )
        }
        compose.waitUntil(5_000) { workspace.pageState.value is TripWorkspacePageState.Ready }
        compose.onNodeWithTag("quick-add-place-p").performClick()
        compose.waitUntil(5_000) { workspace.state.value.overlay == WorkspaceOverlay.SelectAddTargetDay }

        compose.onNodeWithTag("select-target-day-list").performTouchInput { swipeUp() }
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("target-day-day-8").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("target-day-day-8").performClick()
        compose.waitUntil(5_000) { add.state.value.selectedTargetDayIds == listOf("day-8") }
        compose.onNodeWithText("加入第 8 天").assertIsDisplayed()
        compose.onNodeWithText("地点将添加到当天行程末尾，可稍后调整顺序").assertIsDisplayed()
    }

    @Test fun noDayTargetSelectionLabelsCancelAsNotAdding() {
        compose.setContent {
            com.yangchengwei.easytrip.itinerary.ui.SelectTargetDayContent(
                days = emptyList(),
                state = com.yangchengwei.easytrip.itinerary.ui.AddToItineraryUiState(),
                onSelectDay = {},
                onSubmit = {},
                onClose = {},
            )
        }

        compose.onNodeWithText("暂不添加").assertIsDisplayed()
        assertEquals(0, compose.onAllNodesWithText("取消").fetchSemanticsNodes().size)
    }

    @Test fun unknownScheduleDoesNotPresentKnownZeroOrEnableCancelWhileUndoing() {
        compose.setContent {
            com.yangchengwei.easytrip.itinerary.ui.SelectTargetDayContent(
                days = listOf(TripDay("day-1", 0)),
                state = com.yangchengwei.easytrip.itinerary.ui.AddToItineraryUiState(isUndoing = true),
                onSelectDay = {},
                onSubmit = {},
                onClose = {},
                schedule = PlaceScheduleSummaryUi(isKnown = false),
            )
        }

        compose.onNodeWithText("安排信息加载中").assertIsDisplayed()
        compose.onNodeWithText("尚未安排").assertDoesNotExist()
        compose.onNodeWithText("取消").assertIsNotEnabled()
    }

    @Test fun fixedDayPlaceSelectionContinuesBySubmittingWithoutTargetDayOverlay() {
        val workspace = TripWorkspaceViewModel("trip", Trips(), Places(), Itineraries(), Legs(), SavedStateHandle())
        val repository = Itineraries()
        val add = AddToItineraryViewModel("trip", AddPlacesToDayUseCase(repository), UndoAddedItemsUseCase(repository), SavedStateHandle())
        compose.setContent {
            TripWorkspaceRoute(
                viewModel = workspace, consent = null, onBack = {}, onSettings = {},
                locationPermissionCoordinator = LocationPermissionCoordinator(InMemoryLocationPermissionRequestStore()),
                locationPermissionSnapshot = { com.yangchengwei.easytrip.permission.LocationPermissionSnapshot(false, false) },
                onWorkspaceEffect = {},
                itineraryState = DayItineraryUiState(days = listOf(TripDay("day-1", 1)), selectedDayId = "day-1"),
                placeState = com.yangchengwei.easytrip.place.ui.PlacePoolUiState(
                    rows = listOf(com.yangchengwei.easytrip.place.ui.SavedPlaceRowUi(SavedPlace("p", "trip", "poi", "地点", "地址", GeoPoint(1.0, 2.0), "", emptyList()), 0, false)),
                ),
                addToItineraryViewModel = add,
            )
        }
        compose.waitUntil(5_000) { workspace.pageState.value is TripWorkspacePageState.Ready }
        compose.onNodeWithTag("section-ITINERARY").performClick()
        compose.onNodeWithTag("add-places-to-selected-day").performClick()
        compose.waitUntil(5_000) { workspace.state.value.overlay == WorkspaceOverlay.SelectAddPlaces }
        compose.onNodeWithTag("select-place-p").performClick()
        compose.onNodeWithTag("select-places-continue").performClick()

        compose.waitUntil(5_000) { workspace.state.value.overlay == WorkspaceOverlay.AddToItineraryResult }
        assertEquals(AddToItineraryStep.COMPLETED, add.state.value.step)
    }

    @Test fun addingDayFromNoDayGuidanceResumesPlaceTargetSelection() {
        val trips = AppendableNoDayTrips()
        val workspace = TripWorkspaceViewModel("trip", trips, Places(), Itineraries(), Legs(), SavedStateHandle())
        val itinerary = com.yangchengwei.easytrip.itinerary.ui.DayItineraryViewModel(
            "trip", trips, Itineraries(), Legs(), null,
        )
        val repository = Itineraries()
        val add = AddToItineraryViewModel("trip", AddPlacesToDayUseCase(repository), UndoAddedItemsUseCase(repository), SavedStateHandle())
        compose.setContent {
            TripWorkspaceRoute(
                viewModel = workspace, consent = null, onBack = {}, onSettings = {},
                locationPermissionCoordinator = LocationPermissionCoordinator(InMemoryLocationPermissionRequestStore()),
                locationPermissionSnapshot = { com.yangchengwei.easytrip.permission.LocationPermissionSnapshot(false, false) },
                onWorkspaceEffect = {}, itineraryViewModel = itinerary,
                placeState = com.yangchengwei.easytrip.place.ui.PlacePoolUiState(
                    rows = listOf(com.yangchengwei.easytrip.place.ui.SavedPlaceRowUi(SavedPlace("p", "trip", "poi", "地点", "地址", GeoPoint(1.0, 2.0), "", emptyList()), 0, false)),
                ),
                addToItineraryViewModel = add,
            )
        }
        compose.waitUntil(5_000) { workspace.pageState.value is TripWorkspacePageState.Ready }
        compose.onNodeWithTag("quick-add-place-p").performClick()
        compose.waitUntil(5_000) { workspace.state.value.overlay == WorkspaceOverlay.SelectAddTargetDay }
        compose.onNodeWithTag("go-to-itinerary-add-day").performClick()
        compose.waitUntil(5_000) { workspace.state.value.overlay == WorkspaceOverlay.AddTripDay }
        compose.onNodeWithText("添加一天").performClick()

        compose.waitUntil(5_000) {
            workspace.state.value.overlay == WorkspaceOverlay.SelectAddTargetDay &&
                add.state.value.selectedPlaceIds == listOf("p") &&
                compose.onAllNodesWithTag("target-day-day-1").fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(AddToItineraryStep.SELECT_TARGET_DAY, add.state.value.step)
    }

    @Test fun activeAddFlowDoesNotOverwriteNonAddOverlays() {
        val state = com.yangchengwei.easytrip.itinerary.ui.AddToItineraryUiState(
            selectedPlaceIds = listOf("p"),
            editingTarget = com.yangchengwei.easytrip.itinerary.ui.AddToItineraryEditingTarget.ForPlace("p"),
            step = AddToItineraryStep.SELECT_TARGET_DAY,
        )
        listOf<WorkspaceOverlay>(
            WorkspaceOverlay.PermissionExplanation(PermissionKind.DEVICE_LOCATION),
            WorkspaceOverlay.Confirmation(com.yangchengwei.easytrip.core.ui.component.ConfirmationUiModel("确认", "内容", emptyList(), emptyList(), "继续", "取消", false, false)),
            WorkspaceOverlay.EditItineraryItem("item"),
            WorkspaceOverlay.PlaceDetail(1),
        ).forEach { overlay ->
            assertEquals(null, addOverlayToPresent(overlay, state))
        }
    }

    @Test fun resultOverlayPrioritizesUndoFailureAndExplainsMissingTargetCleanup() {
        val ready = TripWorkspaceUiState(tripName = "测试旅行").toReadyState()
        compose.setContent {
            TripWorkspaceScreen(
                pageState = TripWorkspacePageState.Ready(ready.copy(overlay = WorkspaceOverlay.AddToItineraryResult)),
                consent = null,
                onAction = {},
                onMarkerClick = {},
                onMapPoiClick = {},
                placeState = com.yangchengwei.easytrip.place.ui.PlacePoolUiState(),
                onPlaceAction = {},
                itineraryState = com.yangchengwei.easytrip.itinerary.ui.DayItineraryUiState(),
                addToItineraryState = com.yangchengwei.easytrip.itinerary.ui.AddToItineraryUiState(
                    result = com.yangchengwei.easytrip.itinerary.domain.AddPlacesOutcome.TargetDayMissing(
                        retainedPlaceIds = listOf("place"),
                        createdItemIds = listOf("stale-item"),
                    ),
                    undoBatches = listOf(
                        com.yangchengwei.easytrip.itinerary.ui.UndoCreatedItemsBatch(
                            dayId = "other-day",
                            itemIds = listOf("older-live-item"),
                        ),
                    ),
                    errorMessage = "撤销失败，请重试",
                ),
                onItineraryAction = {},
                onCloseOverlay = {},
                onDismissMapPlace = {},
            )
        }

        compose.onNodeWithText("撤销失败，请重试").assertIsDisplayed()
        compose.onNodeWithText("目标日已删除，该日期新增项已随日期移除；其他日期仍有可撤销项。请重新选择日期。").assertDoesNotExist()
        compose.onNodeWithText("撤销").assertIsDisplayed()
    }

    @Test fun addResultSuccessShowsDayAndNavigatesToItBeforeClosing() {
        val workspace = TripWorkspaceViewModel("trip", Trips(), Places(), Itineraries(), Legs(), SavedStateHandle())
        val resultPageState = TripWorkspacePageState.Ready(
            workspace.state.value.toReadyState().copy(
                days = listOf(TripDay("day-2", 1)),
                overlay = WorkspaceOverlay.AddToItineraryResult,
            ),
        )
        var closed = 0
        compose.setContent {
            TripWorkspaceScreen(
                pageState = resultPageState,
                consent = null,
                onAction = { action ->
                    when (action) {
                        is TripWorkspaceAction.SelectSection -> workspace.selectSection(action.section)
                        is TripWorkspaceAction.SelectItineraryScope -> workspace.selectItineraryScope(action.scope)
                        else -> Unit
                    }
                },
                onMarkerClick = {}, onMapPoiClick = {},
                placeState = com.yangchengwei.easytrip.place.ui.PlacePoolUiState(), onPlaceAction = {},
                itineraryState = DayItineraryUiState(),
                addToItineraryState = com.yangchengwei.easytrip.itinerary.ui.AddToItineraryUiState(
                    submissionResult = AddToItinerarySubmissionResult(
                        createdItemsByDay = listOf(UndoCreatedItemsBatch("day-2", listOf("item-1"))),
                    ),
                    undoBatches = listOf(UndoCreatedItemsBatch("day-2", listOf("item-1"))),
                ),
                onViewAddResult = { dayIds ->
                    workspace.selectSection(WorkspaceSection.ITINERARY)
                    workspace.selectItineraryScope(dayIds.singleOrNull()?.let(ItineraryScope::Day) ?: ItineraryScope.WholeTrip)
                    closed++
                },
                onItineraryAction = {}, onCloseOverlay = { closed++ }, onDismissMapPlace = {},
            )
        }

        compose.onNodeWithText("已加入第 2 天").assertIsDisplayed()
        compose.onNodeWithText("查看当天").performClick()

        compose.runOnIdle {
            assertEquals(WorkspaceSection.ITINERARY, workspace.state.value.section)
            assertEquals(ItineraryScope.Day("day-2"), workspace.state.value.itineraryScope)
            assertEquals(1, closed)
        }
    }

    @Test fun missingResultReselectOpensTargetDayWithoutCancellingDraft() {
        val workspace = TripWorkspaceViewModel("trip", Trips(), Places(), Itineraries(), Legs(), SavedStateHandle())
        val repository = MissingTargetItineraries()
        val restored = AddToItineraryViewModel(
            "trip",
            AddPlacesToDayUseCase(repository),
            UndoAddedItemsUseCase(repository),
            SavedStateHandle(),
        )
        restored.reconcile(listOf("day-1", "deleted-day"), setOf("p"))
        restored.startForPlace("p")
        restored.toggleTargetDay("deleted-day")
        restored.submit()
        compose.waitUntil(5_000) { restored.state.value.submissionResult?.missingTargetDayIds == listOf("deleted-day") }
        compose.setContent {
            TripWorkspaceRoute(
                viewModel = workspace,
                consent = null,
                onBack = {},
                onSettings = {},
                locationPermissionCoordinator = LocationPermissionCoordinator(InMemoryLocationPermissionRequestStore()),
                locationPermissionSnapshot = { com.yangchengwei.easytrip.permission.LocationPermissionSnapshot(false, false) },
                onWorkspaceEffect = {},
                placeState = com.yangchengwei.easytrip.place.ui.PlacePoolUiState(
                    rows = listOf(com.yangchengwei.easytrip.place.ui.SavedPlaceRowUi(SavedPlace("p", "trip", "poi", "地点", "地址", GeoPoint(1.0, 2.0), "", emptyList()), 0, false)),
                ),
                addToItineraryViewModel = restored,
            )
        }
        compose.waitUntil(5_000) {
            workspace.pageState.value is TripWorkspacePageState.Ready &&
                compose.onAllNodesWithText("所选旅行日已不存在").fetchSemanticsNodes().isNotEmpty()
        }

        compose.onNodeWithText("请重新选择旅行日").performClick()

        compose.waitUntil(5_000) {
            workspace.state.value.overlay == WorkspaceOverlay.SelectAddTargetDay &&
                restored.state.value.step == AddToItineraryStep.SELECT_TARGET_DAY
        }
        assertEquals(listOf("p"), restored.state.value.selectedPlaceIds)
        compose.onNodeWithTag("target-day-day-1").assertIsDisplayed()
    }

    @Test fun addResultDistinguishesPartialMissingAndUndoStates() {
        fun state(result: AddToItinerarySubmissionResult, undoBatches: List<UndoCreatedItemsBatch> = emptyList()) =
            com.yangchengwei.easytrip.itinerary.ui.AddToItineraryUiState(
                submissionResult = result,
                undoBatches = undoBatches,
            )
        var current by androidx.compose.runtime.mutableStateOf(
            state(
                AddToItinerarySubmissionResult(
                    createdItemsByDay = listOf(UndoCreatedItemsBatch("day-1", listOf("i"))),
                    failedAdditions = listOf(FailedItineraryAddition("day-2", "p")),
                    retryTargetDayIds = listOf("day-2"),
                ),
                listOf(UndoCreatedItemsBatch("day-1", listOf("i"))),
            ),
        )
        compose.setContent {
            TripWorkspaceScreen(
                pageState = TripWorkspacePageState.Ready(
                    TripWorkspaceUiState(days = listOf(TripDay("day-1", 0), TripDay("day-2", 1))).toReadyState()
                        .copy(overlay = WorkspaceOverlay.AddToItineraryResult),
                ),
                consent = null, onAction = {}, onMarkerClick = {}, onMapPoiClick = {},
                placeState = com.yangchengwei.easytrip.place.ui.PlacePoolUiState(
                    rows = listOf(com.yangchengwei.easytrip.place.ui.SavedPlaceRowUi(SavedPlace("p", "trip", "poi", "西湖天地", "", GeoPoint(1.0, 2.0), "", emptyList()), 0, false)),
                ),
                onPlaceAction = {}, itineraryState = DayItineraryUiState(), addToItineraryState = current,
                onItineraryAction = {}, onCloseOverlay = {}, onDismissMapPlace = {},
            )
        }

        compose.onNodeWithText("部分地点已加入行程").assertIsDisplayed()
        compose.onNodeWithText("第 1 天：已加入").assertIsDisplayed()
        compose.onNodeWithText("第 2 天 · 西湖天地：未加入").assertIsDisplayed()
        compose.onNodeWithText("重试失败地点").assertIsDisplayed()
        compose.runOnUiThread {
            current = state(AddToItinerarySubmissionResult(missingTargetDayIds = listOf("day-1")))
        }
        compose.onNodeWithText("所选旅行日已不存在").assertIsDisplayed()
        compose.onNodeWithText("请重新选择旅行日").assertIsDisplayed()
        compose.onNodeWithText("已加入行程").assertDoesNotExist()
        compose.runOnUiThread {
            current = state(AddToItinerarySubmissionResult(createdItemsByDay = listOf(UndoCreatedItemsBatch("day-1", listOf("i")))))
        }
        compose.onNodeWithText("已从第 1 天移除，收藏地点仍保留").assertIsDisplayed()
        compose.onNodeWithText("撤销").assertDoesNotExist()
        compose.onNodeWithText("查看地点池").assertIsDisplayed()
    }

    @Test fun missingResultKeepsReselectAndGatesRetryByRetryableTargets() {
        var reselectCalls = 0
        var retryCalls = 0
        var closeCalls = 0
        compose.setContent {
            com.yangchengwei.easytrip.itinerary.ui.AddToItineraryResultContent(
                state = com.yangchengwei.easytrip.itinerary.ui.AddToItineraryUiState(
                    submissionResult = AddToItinerarySubmissionResult(
                        failedAdditions = listOf(FailedItineraryAddition("deleted-day", "p")),
                        missingTargetDayIds = listOf("deleted-day"),
                    ),
                ),
                days = emptyList(), placeNameForId = { "地点" },
                onUndo = {}, onRetryFailed = { retryCalls++ }, onReselectDates = { reselectCalls++ },
                onViewResult = {}, onViewPlacePool = {}, onClose = { closeCalls++ },
            )
        }

        compose.onNodeWithText("请重新选择旅行日").assertIsEnabled().performClick()
        compose.onNodeWithText("重试失败地点").assertDoesNotExist()
        compose.onNodeWithText("仅保留收藏").performClick()
        compose.runOnIdle {
            assertEquals(1, reselectCalls)
            assertEquals(0, retryCalls)
            assertEquals(1, closeCalls)
        }
    }

    @Test fun addResultWithMissingAndSuccessRetainsUndoAndViewActions() {
        var undoCalls = 0
        var viewedDays = emptyList<String>()
        compose.setContent {
            com.yangchengwei.easytrip.itinerary.ui.AddToItineraryResultContent(
                state = com.yangchengwei.easytrip.itinerary.ui.AddToItineraryUiState(
                    submissionResult = AddToItinerarySubmissionResult(
                        createdItemsByDay = listOf(UndoCreatedItemsBatch("day-1", listOf("i-1"))),
                        missingTargetDayIds = listOf("deleted-day"),
                        missingTargetDayLabels = mapOf("deleted-day" to "已删除的旅行日（deleted-day）"),
                    ),
                    undoBatches = listOf(UndoCreatedItemsBatch("day-1", listOf("i-1"))),
                ),
                days = listOf(TripDay("day-1", 0)), placeNameForId = { null },
                onUndo = { undoCalls++ }, onRetryFailed = {}, onReselectDates = {},
                onViewResult = { viewedDays = it }, onViewPlacePool = {}, onClose = {},
            )
        }

        compose.onNodeWithText("撤销").assertIsEnabled().performClick()
        compose.onNodeWithText("查看当天").performClick()
        compose.runOnIdle {
            assertEquals(1, undoCalls)
            assertEquals(listOf("day-1"), viewedDays)
        }
    }

    @Test fun addResultRendersAllMixedOutcomeSectionsAndConcreteMultiDayLabels() {
        var retryCalls = 0
        var reselectCalls = 0
        var closeCalls = 0
        compose.setContent {
            com.yangchengwei.easytrip.itinerary.ui.AddToItineraryResultContent(
                state = com.yangchengwei.easytrip.itinerary.ui.AddToItineraryUiState(
                    submissionResult = AddToItinerarySubmissionResult(
                        createdItemsByDay = listOf(
                            UndoCreatedItemsBatch("day-1", listOf("i-1")),
                            UndoCreatedItemsBatch("day-2", listOf("i-2")),
                        ),
                        failedAdditions = listOf(FailedItineraryAddition("day-3", "p")),
                        missingTargetDayIds = listOf("deleted-day"),
                        missingTargetDayLabels = mapOf("deleted-day" to "已删除的旅行日（deleted-day）"),
                        retryTargetDayIds = listOf("day-3"),
                    ),
                    undoBatches = listOf(UndoCreatedItemsBatch("day-1", listOf("i-1"))),
                    errorMessage = "撤销失败，请重试",
                ),
                days = listOf(TripDay("day-1", 0), TripDay("day-2", 1), TripDay("day-3", 2)),
                placeNameForId = { "西湖天地" },
                onUndo = {}, onRetryFailed = { retryCalls++ }, onReselectDates = { reselectCalls++ }, onViewResult = {}, onViewPlacePool = {}, onClose = { closeCalls++ },
            )
        }

        compose.onNodeWithText("已加入第 1 天、第 2 天").assertIsDisplayed()
        compose.onNodeWithText("第 3 天 · 西湖天地：未加入").assertIsDisplayed()
        compose.onNodeWithText("已删除的旅行日（deleted-day）已被删除，未将地点改投其他日期。").assertIsDisplayed()
        compose.onNodeWithText("撤销失败，请重试").assertIsDisplayed()
        compose.onNodeWithText("重试失败地点").performClick()
        compose.onNodeWithText("请重新选择旅行日").performClick()
        compose.onNodeWithText("仅保留收藏").performClick()
        compose.runOnIdle {
            assertEquals(1, retryCalls)
            assertEquals(1, reselectCalls)
            assertEquals(1, closeCalls)
        }
    }

    @Test fun ordinaryFailedResultHidesRetryWithoutRetryableTargets() {
        compose.setContent {
            com.yangchengwei.easytrip.itinerary.ui.AddToItineraryResultContent(
                state = com.yangchengwei.easytrip.itinerary.ui.AddToItineraryUiState(
                    submissionResult = AddToItinerarySubmissionResult(
                        failedAdditions = listOf(FailedItineraryAddition("day-1", "p")),
                    ),
                ),
                days = listOf(TripDay("day-1", 0)), placeNameForId = { "地点" },
                onUndo = {}, onRetryFailed = {}, onReselectDates = {}, onViewResult = {}, onViewPlacePool = {}, onClose = {},
            )
        }

        compose.onNodeWithText("重试失败地点").assertDoesNotExist()
    }

    @Test fun mapDetailBackThenPlaceEditRendersNewTarget() {
        val workspace = TripWorkspaceViewModel("trip", Trips(), Places(), Itineraries(), Legs(), SavedStateHandle())
        val placeModel = com.yangchengwei.easytrip.place.ui.PlacePoolViewModel("trip", EditingPlaces(), null)
        compose.setContent {
            TripWorkspaceRoute(
                viewModel = workspace,
                consent = null,
                onBack = {},
                onSettings = {},
                locationPermissionCoordinator = LocationPermissionCoordinator(InMemoryLocationPermissionRequestStore()),
                locationPermissionSnapshot = { com.yangchengwei.easytrip.permission.LocationPermissionSnapshot(false, false) },
                onWorkspaceEffect = {},
                placeViewModel = placeModel,
            )
        }
        compose.waitUntil(5_000) { workspace.pageState.value is TripWorkspacePageState.Ready }

        compose.runOnIdle {
            workspace.selectMapPoi(MapPoiUi("old-poi", "旧地图地点", "旧地址", GeoPoint(39.9, 116.4)))
        }
        compose.onNodeWithText("旧地图地点").assertIsDisplayed()
        pressBack()
        compose.waitUntil { workspace.state.value.selectedMapPoi == null }

        compose.onNodeWithTag("more-place-saved").performClick()
        compose.onNodeWithTag("menu-edit-place-saved", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("place-detail-title").assertIsDisplayed()
        compose.onNodeWithText("取消").assertIsDisplayed()
        compose.onNodeWithText("取消收藏").assertDoesNotExist()
        compose.onNodeWithText("旧地图地点").assertDoesNotExist()
    }

    @Test fun backDuringDeletePreparationPreventsConfirmationReopening() {
        val workspace = TripWorkspaceViewModel("trip", Trips(), Places(), Itineraries(), Legs(), SavedStateHandle())
        val repository = DelayedDeletePlaces()
        val placeModel = com.yangchengwei.easytrip.place.ui.PlacePoolViewModel("trip", repository, null)
        var backCount = 0
        compose.setContent {
            TripWorkspaceRoute(
                viewModel = workspace,
                consent = null,
                onBack = { backCount++ },
                onSettings = {},
                locationPermissionCoordinator = LocationPermissionCoordinator(InMemoryLocationPermissionRequestStore()),
                locationPermissionSnapshot = { com.yangchengwei.easytrip.permission.LocationPermissionSnapshot(false, false) },
                onWorkspaceEffect = {},
                placeViewModel = placeModel,
            )
        }
        compose.waitUntil(5_000) { placeModel.state.value.rows.isNotEmpty() }
        compose.onNodeWithTag("more-place-saved").performClick()
        compose.onNodeWithTag("menu-delete-place-saved", useUnmergedTree = true).performClick()
        pressBack()
        compose.waitUntil(5_000) { backCount == 1 }

        compose.runOnIdle { repository.usage.complete(2) }
        compose.waitForIdle()

        assertEquals(null, placeModel.state.value.deleting)
        assertEquals(WorkspaceOverlay.None, workspace.state.value.overlay)
        compose.onNodeWithTag("confirmation-confirm").assertDoesNotExist()
    }

    @Test fun deleteConfirmationWaitsForReadyTargetAndConfirmsOnce() {
        val workspace = TripWorkspaceViewModel("trip", Trips(), Places(), Itineraries(), Legs(), SavedStateHandle())
        val repository = DelayedDeletePlaces()
        val placeModel = com.yangchengwei.easytrip.place.ui.PlacePoolViewModel("trip", repository, null)
        compose.setContent {
            TripWorkspaceRoute(
                viewModel = workspace,
                consent = null,
                onBack = {},
                onSettings = {},
                locationPermissionCoordinator = LocationPermissionCoordinator(InMemoryLocationPermissionRequestStore()),
                locationPermissionSnapshot = { com.yangchengwei.easytrip.permission.LocationPermissionSnapshot(false, false) },
                onWorkspaceEffect = {},
                placeViewModel = placeModel,
            )
        }
        compose.waitUntil(5_000) { placeModel.state.value.rows.isNotEmpty() }
        compose.onNodeWithTag("more-place-saved").performClick()
        compose.onNodeWithTag("menu-delete-place-saved", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("confirmation-confirm").assertDoesNotExist()

        compose.runOnIdle { repository.usage.complete(2) }
        compose.waitUntil(5_000) { placeModel.state.value.deleting != null }
        compose.onNodeWithTag("confirmation-confirm").assertIsDisplayed().performClick()
        compose.waitUntil(5_000) { repository.deleteCalls == 1 }
        assertEquals(1, repository.deleteCalls)
        compose.onNodeWithTag("confirmation-confirm").assertIsDisplayed()
        compose.runOnIdle { repository.deleteGate.complete(Unit) }
        compose.waitUntil(5_000) { placeModel.state.value.deleting == null }
        compose.onNodeWithTag("confirmation-confirm").assertDoesNotExist()
    }

    @Test fun failedDeleteImpactHidesUnknownCountsAndRetriesInPlace() {
        val workspace = TripWorkspaceViewModel("trip", Trips(), Places(), Itineraries(), Legs(), SavedStateHandle())
        val repository = RetryImpactPlaces()
        val placeModel = com.yangchengwei.easytrip.place.ui.PlacePoolViewModel("trip", repository, null)
        compose.setContent {
            TripWorkspaceRoute(
                viewModel = workspace,
                consent = null,
                onBack = {},
                onSettings = {},
                locationPermissionCoordinator = LocationPermissionCoordinator(InMemoryLocationPermissionRequestStore()),
                locationPermissionSnapshot = { com.yangchengwei.easytrip.permission.LocationPermissionSnapshot(false, false) },
                onWorkspaceEffect = {},
                placeViewModel = placeModel,
            )
        }
        compose.waitUntil(5_000) { placeModel.state.value.rows.isNotEmpty() }

        compose.onNodeWithTag("more-place-saved").performClick()
        compose.onNodeWithTag("menu-delete-place-saved", useUnmergedTree = true).performClick()
        compose.waitUntil(5_000) { placeModel.state.value.deletionError != null }

        compose.onNodeWithText("影响查询失败").assertIsDisplayed()
        compose.onNodeWithText("将同时删除 null 次行程安排和 null 段路线。").assertDoesNotExist()
        compose.onNodeWithTag("confirmation-confirm").performClick()
        compose.waitUntil(5_000) { repository.impactCalls == 2 }
        compose.onNodeWithTag("confirmation-confirm").assertIsDisplayed().assertIsNotEnabled()
        compose.onNodeWithTag("confirmation-dismiss").assertIsNotEnabled()

        compose.runOnIdle { repository.impactGate.complete(PlaceDeletionImpact(2, 1)) }
        compose.waitUntil(5_000) { placeModel.state.value.deletionImpact != null }
        compose.onNodeWithText("将同时删除 2 次行程安排和 1 段路线。").assertIsDisplayed()
        compose.onNodeWithTag("confirmation-confirm").assertIsEnabled()
    }

    @Test fun backCannotDismissPlaceConfirmationWhileDeleteRuns() {
        val workspace = TripWorkspaceViewModel("trip", Trips(), Places(), Itineraries(), Legs(), SavedStateHandle())
        val repository = DelayedDeletePlaces()
        val placeModel = com.yangchengwei.easytrip.place.ui.PlacePoolViewModel("trip", repository, null)
        var backCount = 0
        compose.setContent {
            TripWorkspaceRoute(
                viewModel = workspace,
                consent = null,
                onBack = { backCount++ },
                onSettings = {},
                locationPermissionCoordinator = LocationPermissionCoordinator(InMemoryLocationPermissionRequestStore()),
                locationPermissionSnapshot = { com.yangchengwei.easytrip.permission.LocationPermissionSnapshot(false, false) },
                onWorkspaceEffect = {},
                placeViewModel = placeModel,
            )
        }
        compose.waitUntil(5_000) { placeModel.state.value.rows.isNotEmpty() }
        compose.onNodeWithTag("more-place-saved").performClick()
        compose.onNodeWithTag("menu-delete-place-saved", useUnmergedTree = true).performClick()
        compose.runOnIdle { repository.usage.complete(2) }
        compose.waitUntil(5_000) { placeModel.state.value.deleting != null }
        compose.onNodeWithTag("confirmation-confirm").performClick()
        compose.waitUntil(5_000) { placeModel.state.value.deletionBusy }
        compose.onNodeWithTag("confirmation-confirm").assertIsNotEnabled()
        compose.onNodeWithTag("confirmation-dismiss").assertIsNotEnabled()

        pressBack()
        compose.waitForIdle()

        assertEquals(0, backCount)
        assertEquals("saved", placeModel.state.value.deleting?.id)
        compose.onNodeWithTag("confirmation-confirm").assertIsDisplayed()
        compose.runOnIdle { repository.deleteGate.complete(Unit) }
        compose.waitUntil(5_000) { placeModel.state.value.deleting == null }
    }

    @Test fun itemMenuActionsOpenBusinessOverlaysForSameItem() {
        val workspace = TripWorkspaceViewModel("trip", Trips(), Places(), Itineraries(), Legs(), SavedStateHandle())
        val receivedActions = mutableListOf<DayItineraryAction>()
        var itineraryState by androidx.compose.runtime.mutableStateOf(
            DayItineraryUiState(
                days = listOf(TripDay("day-1", 0), TripDay("day-2", 1)),
                selectedDayId = "day-1",
                items = listOf(ItineraryItemUi("item-1", "酒店", "", null, null)),
                previewOrder = listOf("item-1"),
            ),
        )
        compose.setContent {
            TripWorkspaceRoute(
                viewModel = workspace,
                consent = null,
                onBack = {},
                onSettings = {},
                locationPermissionCoordinator = LocationPermissionCoordinator(InMemoryLocationPermissionRequestStore()),
                locationPermissionSnapshot = { com.yangchengwei.easytrip.permission.LocationPermissionSnapshot(false, false) },
                onWorkspaceEffect = {},
                itineraryState = itineraryState,
                onItineraryAction = { action ->
                    receivedActions += action
                    itineraryState = when (action) {
                        is DayItineraryAction.RequestTiming -> itineraryState.copy(
                            editDraft = ItineraryEditDraft(action.itemId, "", ""),
                        )
                        is DayItineraryAction.RequestCrossDay -> itineraryState.copy(
                            crossDayMove = CrossDayMoveDraft(action.itemId),
                        )
                        is DayItineraryAction.RequestDelete -> itineraryState.copy(
                            deleteConfirmation = ItineraryDeleteConfirmation(action.itemId, "酒店"),
                        )
                        DayItineraryAction.DismissDialogs -> itineraryState.copy(
                            editDraft = null,
                            crossDayMove = null,
                            deleteConfirmation = null,
                        )
                        else -> itineraryState
                    }
                },
            )
        }
        compose.waitUntil(5_000) { workspace.pageState.value is TripWorkspacePageState.Ready }
        compose.onNodeWithTag("section-ITINERARY").performClick()

        compose.onNodeWithTag("more-item-1").performClick()
        pressBack()
        compose.runOnIdle {
            assertEquals(null, itineraryState.editDraft)
            assertEquals(null, itineraryState.crossDayMove)
            assertEquals(null, itineraryState.deleteConfirmation)
            assertTrue(receivedActions.none { it == DayItineraryAction.DismissDialogs })
        }

        compose.onNodeWithTag("more-item-1").performClick()
        compose.onNodeWithTag("menu-timing-item-1", useUnmergedTree = true).performClick()
        compose.waitUntil { itineraryState.editDraft?.itemId == "item-1" }
        compose.onNodeWithTag("arrival-time-input").assertIsDisplayed()
        compose.onNodeWithText("取消").performClick()
        compose.runOnIdle { itineraryState = itineraryState.copy(editDraft = null) }
        compose.waitUntil { workspace.state.value.overlay == WorkspaceOverlay.None }

        compose.onNodeWithTag("more-item-1").performClick()
        compose.onNodeWithTag("menu-move-item-1", useUnmergedTree = true).performClick()
        compose.waitUntil {
            itineraryState.crossDayMove?.itemId == "item-1" &&
                workspace.state.value.overlay == WorkspaceOverlay.SelectMoveTargetDay("item-1")
        }
        compose.onNodeWithText("移动到…").assertIsDisplayed()
        pressBack()
        compose.runOnIdle { itineraryState = itineraryState.copy(crossDayMove = null) }
        compose.waitUntil { workspace.state.value.overlay == WorkspaceOverlay.None }

        compose.onNodeWithTag("more-item-1").performClick()
        compose.onNodeWithTag("menu-delete-item-1", useUnmergedTree = true).performClick()
        compose.waitUntil { itineraryState.deleteConfirmation?.itemId == "item-1" }
        compose.onNodeWithText("移出酒店？").assertIsDisplayed()
    }

    @Test fun saveFailureUsesSharedRecoveryContentInsideTheExistingItineraryOverlay() {
        val ready = TripWorkspaceUiState(tripName = "测试旅行").toReadyState()
        var itineraryState by androidx.compose.runtime.mutableStateOf(
            DayItineraryUiState(
                editDraft = ItineraryEditDraft(
                    itemId = "item-1",
                    arrivalTimeText = "09:30",
                    stayMinutesText = "60",
                    noteText = "仍在编辑的备注",
                    saveError = "保存失败",
                ),
            ),
        )
        val actions = mutableListOf<DayItineraryAction>()
        var overlayCloseCalls = 0
        compose.setContent {
            TripWorkspaceScreen(
                pageState = TripWorkspacePageState.Ready(ready.copy(overlay = WorkspaceOverlay.EditItineraryItem("item-1"))),
                consent = null,
                onAction = {},
                onMarkerClick = {},
                onMapPoiClick = {},
                placeState = com.yangchengwei.easytrip.place.ui.PlacePoolUiState(),
                onPlaceAction = {},
                itineraryState = itineraryState,
                onItineraryAction = { action ->
                    actions += action
                    if (action::class.java.simpleName == "DismissEditSaveError") {
                        itineraryState = itineraryState.copy(editDraft = itineraryState.editDraft?.copy(saveError = null))
                    }
                },
                onCloseOverlay = { overlayCloseCalls++ },
                onDismissMapPlace = {},
            )
        }

        compose.onNodeWithTag("itinerary-save-failure").assertIsDisplayed()
        compose.onAllNodesWithTag("arrival-time-input").assertCountEquals(0)
        compose.onNodeWithTag("itinerary-save-failure-retry").performClick()
        compose.runOnIdle { assertEquals(1, actions.count { it == DayItineraryAction.SaveEdit }) }
        compose.onNodeWithTag("itinerary-save-failure-keep-editing").performClick()
        compose.onNodeWithTag("arrival-time-input").assertIsDisplayed()
        compose.onNodeWithTag("stay-minutes-input").assertIsDisplayed()
        compose.onNodeWithTag("itinerary-note-input").assertIsDisplayed()
        compose.onNodeWithText("仍在编辑的备注").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(0, overlayCloseCalls)
            assertEquals(1, actions.count { it == DayItineraryAction.SaveEdit })
        }
    }

    @Test fun routeBackFromItinerarySaveFailureOnlyClearsErrorAndKeepsOverlay() {
        val workspace = TripWorkspaceViewModel("trip", Trips(), Places(), Itineraries(), Legs(), SavedStateHandle())
        var itineraryState by androidx.compose.runtime.mutableStateOf(
            DayItineraryUiState(
                editDraft = ItineraryEditDraft(
                    itemId = "item-1",
                    arrivalTimeText = "09:30",
                    stayMinutesText = "60",
                    noteText = "仍在编辑的备注",
                    saveError = "保存失败",
                ),
            ),
        )
        var leaveCalls = 0
        compose.setContent {
            TripWorkspaceRoute(
                viewModel = workspace,
                consent = null,
                onBack = { leaveCalls++ },
                onSettings = {},
                locationPermissionCoordinator = LocationPermissionCoordinator(InMemoryLocationPermissionRequestStore()),
                locationPermissionSnapshot = { com.yangchengwei.easytrip.permission.LocationPermissionSnapshot(false, false) },
                onWorkspaceEffect = {},
                itineraryState = itineraryState,
                onItineraryAction = { action ->
                    if (action == DayItineraryAction.DismissEditSaveError) {
                        itineraryState = itineraryState.copy(editDraft = itineraryState.editDraft?.copy(saveError = null))
                    }
                },
            )
        }
        compose.waitUntil(5_000) {
            workspace.state.value.overlay == WorkspaceOverlay.EditItineraryItem("item-1")
        }
        compose.onNodeWithTag("itinerary-save-failure").assertIsDisplayed()

        pressBack()

        compose.waitUntil(5_000) { itineraryState.editDraft?.saveError == null }
        compose.onNodeWithTag("arrival-time-input").assertIsDisplayed()
        compose.onNodeWithTag("stay-minutes-input").assertIsDisplayed()
        compose.onNodeWithTag("itinerary-note-input").assertIsDisplayed()
        compose.onNodeWithText("仍在编辑的备注").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(WorkspaceOverlay.EditItineraryItem("item-1"), workspace.state.value.overlay)
            assertEquals(0, leaveCalls)
        }
    }

    @Test fun readyLegOpensModeOverlayForSameLeg() {
        val workspace = TripWorkspaceViewModel("trip", Trips(), Places(), Itineraries(), Legs(), SavedStateHandle())
        val items = listOf(
            ItineraryItemUi("item-1", "酒店", "", null, null),
            ItineraryItemUi("item-2", "景点", "", null, null),
        )
        var state by androidx.compose.runtime.mutableStateOf(
            DayItineraryUiState(
                days = listOf(TripDay("day-1", 0)),
                selectedDayId = "day-1",
                items = items,
                previewOrder = items.map(ItineraryItemUi::id),
                legs = listOf(
                    RouteLegUi("ready", "item-1", "item-2", TransportMode.WALK, RouteStatus.SUCCESS, 800, 600, null),
                ),
            ),
        )
        compose.setContent {
            TripWorkspaceRoute(
                viewModel = workspace,
                consent = null,
                onBack = {},
                onSettings = {},
                locationPermissionCoordinator = LocationPermissionCoordinator(InMemoryLocationPermissionRequestStore()),
                locationPermissionSnapshot = { com.yangchengwei.easytrip.permission.LocationPermissionSnapshot(false, false) },
                onWorkspaceEffect = {},
                itineraryState = state,
                onItineraryAction = { action ->
                    if (action is DayItineraryAction.RequestMode) {
                        state = state.copy(modeEditor = RouteModeEditDraft(action.legId, TransportMode.WALK))
                    }
                },
            )
        }
        compose.waitUntil(5_000) { workspace.pageState.value is TripWorkspacePageState.Ready }
        compose.onNodeWithTag("section-ITINERARY").performClick()

        compose.onNodeWithTag("mode-ready").performClick()

        compose.waitUntil {
            state.modeEditor?.legId == "ready" &&
                workspace.state.value.overlay == WorkspaceOverlay.EditRouteLeg("ready")
        }
        compose.onNodeWithText("交通路段编辑").assertIsDisplayed()
        compose.onNodeWithTag("route-duration-minutes-input").assertIsDisplayed()
    }

    @Test fun failedLegRetryDoesNotOpenModeOverlay() {
        val workspace = TripWorkspaceViewModel("trip", Trips(), Places(), Itineraries(), Legs(), SavedStateHandle())
        val items = listOf(
            ItineraryItemUi("item-1", "酒店", "", null, null),
            ItineraryItemUi("item-2", "景点", "", null, null),
        )
        var actions = emptyList<DayItineraryAction>()
        val state = DayItineraryUiState(
            days = listOf(TripDay("day-1", 0)),
            selectedDayId = "day-1",
            items = items,
            previewOrder = items.map(ItineraryItemUi::id),
            legs = listOf(
                RouteLegUi("failed", "item-1", "item-2", TransportMode.WALK, RouteStatus.FAILED, null, null, "路线失败"),
            ),
        )
        compose.setContent {
            TripWorkspaceRoute(
                viewModel = workspace,
                consent = null,
                onBack = {},
                onSettings = {},
                locationPermissionCoordinator = LocationPermissionCoordinator(InMemoryLocationPermissionRequestStore()),
                locationPermissionSnapshot = { com.yangchengwei.easytrip.permission.LocationPermissionSnapshot(false, false) },
                onWorkspaceEffect = {},
                itineraryState = state,
                onItineraryAction = { actions = actions + it },
            )
        }
        compose.waitUntil(5_000) { workspace.pageState.value is TripWorkspacePageState.Ready }
        compose.onNodeWithTag("section-ITINERARY").performClick()

        compose.onNodeWithTag("retry-failed").performClick()

        assertEquals(listOf(DayItineraryAction.Retry("failed", 0)), actions)
        assertEquals(WorkspaceOverlay.None, workspace.state.value.overlay)
    }

    @Test fun mapPoiClickOpensCardBeforeCollectionAction() {
        val saved = SavedStateHandle()
        val model = TripWorkspaceViewModel("trip", Trips(), Places(), Itineraries(), Legs(), saved)
        val poi = MapPoiUi("poi-card", "故宫", "北京市东城区", GeoPoint(39.916, 116.397))
        lateinit var host: PoiHost
        var toggles = 0
        compose.setContent {
            TripWorkspaceScreen(
                viewModel = model,
                consent = consentToken(),
                onBack = {},
                onSettings = {},
                isPoiSaved = false,
                onTogglePoiCollection = { toggles++ },
                placeContent = { Text("地点内容") },
                dayItineraryContent = { Text("行程内容") },
                mapHostFactory = { PoiHost(it).also { created -> host = created } },
            )
        }
        compose.waitUntil(5_000) { runCatching { host }.isSuccess }

        compose.runOnIdle { host.emit(poi) }

        compose.onNodeWithText("故宫").assertIsDisplayed()
        compose.onNodeWithText("北京市东城区").assertIsDisplayed()
        val viewportBeforeClose = model.state.value.map.viewportRequest
        compose.onNodeWithText("关闭").performClick()
        compose.waitForIdle()
        assertEquals(viewportBeforeClose, model.state.value.map.viewportRequest)
        assertEquals(0, toggles)
    }

    @Test fun mapPoiWithoutAddressShowsUnavailableMessageAndCanBeCollected() {
        val model = TripWorkspaceViewModel("trip", Trips(), Places(), Itineraries(), Legs(), SavedStateHandle())
        lateinit var host: PoiHost
        var collected: PlaceCandidate? = null
        compose.setContent {
            TripWorkspaceScreen(
                viewModel = model,
                consent = consentToken(),
                onBack = {},
                onSettings = {},
                isPoiSaved = false,
                onTogglePoiCollection = { collected = it },
                placeContent = { Text("地点内容") },
                dayItineraryContent = { Text("行程内容") },
                mapHostFactory = { PoiHost(it).also { created -> host = created } },
            )
        }
        compose.waitUntil(5_000) { runCatching { host }.isSuccess }
        compose.runOnIdle { host.emit(MapPoiUi("poi-empty-address", "故宫", "", GeoPoint(39.9, 116.4))) }

        compose.onNodeWithText("地址暂不可用").assertIsDisplayed()
        compose.onNodeWithTag("place-card-collection").performClick()
        assertEquals("", collected?.address)
    }

    @Test fun mapPoiWithoutStableIdCannotBeCollected() {
        val model = TripWorkspaceViewModel("trip", Trips(), Places(), Itineraries(), Legs(), SavedStateHandle())
        lateinit var host: PoiHost
        compose.setContent {
            TripWorkspaceScreen(
                viewModel = model,
                consent = consentToken(),
                onBack = {},
                onSettings = {},
                isPoiSaved = false,
                onTogglePoiCollection = {},
                placeContent = { Text("地点内容") },
                dayItineraryContent = { Text("行程内容") },
                mapHostFactory = { PoiHost(it).also { created -> host = created } },
            )
        }
        compose.waitUntil(5_000) { runCatching { host }.isSuccess }
        compose.runOnIdle { host.emit(MapPoiUi(null, "无编号地点", "地址未知", GeoPoint(39.9, 116.4))) }

        compose.onNodeWithText("无法收藏").assertIsNotEnabled()
    }

    @Test fun bottomNavigationKeepsItineraryAndMapScopeSynchronized() {
        val model = TripWorkspaceViewModel("trip", Trips(), Places(), Itineraries(), Legs(), SavedStateHandle())
        compose.setContent {
            TripWorkspaceScreen(
                viewModel = model,
                consent = null,
                onBack = {},
                onSettings = {},
                placeContent = { Text("地点内容") },
                dayItineraryContent = { Text("可编辑日行程") },
            )
        }
        compose.waitUntil(5_000) { model.state.value.map.viewportRequest != null }
        compose.onNodeWithTag("workspace-top-bar").assertHeightIsEqualTo(52.dp)
        compose.onNodeWithTag("workspace-search-launcher").assertHeightIsAtLeast(46.dp)
        compose.onNodeWithTag("workspace-sheet-handle").assertIsDisplayed()
        assertEquals(0, compose.onAllNodesWithText("收起").fetchSemanticsNodes().size)
        assertEquals(0, compose.onAllNodesWithText("半屏").fetchSemanticsNodes().size)
        assertEquals(0, compose.onAllNodesWithText("展开").fetchSemanticsNodes().size)
        compose.onNodeWithTag("workspace-map").assertIsDisplayed()
        val mapBottom = compose.onNodeWithTag("workspace-map").getUnclippedBoundsInRoot().bottom
        val searchBottom = compose.onNodeWithTag("workspace-search-launcher").getUnclippedBoundsInRoot().bottom
        val sheetTop = compose.onNodeWithTag("workspace-sheet").getUnclippedBoundsInRoot().top
        val navigationTop = compose.onNodeWithTag("workspace-tabs").getUnclippedBoundsInRoot().top
        assert(searchBottom <= sheetTop)
        assert(sheetTop < navigationTop)
        assert(navigationTop <= mapBottom)
        compose.onNodeWithTag("section-PLACE_POOL").assertExists()
        compose.onNodeWithTag("section-ITINERARY").assertExists()
        compose.onNodeWithTag("scope-PLACE_POOL").assertDoesNotExist()
        compose.onNodeWithTag("scope-SINGLE_DAY").assertDoesNotExist()
        compose.onNodeWithTag("scope-WHOLE_TRIP").assertDoesNotExist()
        compose.onNodeWithTag("tab-PLACES").assertDoesNotExist()
        compose.onNodeWithTag("tab-ITINERARY").assertDoesNotExist()
        compose.onNodeWithText("地点内容").assertIsDisplayed()

        val placeRequestId = model.state.value.map.viewportRequest?.id
        compose.onNodeWithTag("section-ITINERARY").performClick()
        compose.waitUntil(5_000) { model.state.value.mapScope == MapScope.SINGLE_DAY }
        compose.onNodeWithTag("itinerary-scope-rail").assertIsDisplayed()
        compose.onNodeWithText("第一天").assertIsDisplayed()
        compose.onNodeWithText("可编辑日行程").assertIsDisplayed()
        assertEquals(ItineraryScope.Day("day-1"), model.state.value.itineraryScope)
        assertEquals(placeRequestId?.plus(1), model.state.value.map.viewportRequest?.id)

        val dayOneRequestId = model.state.value.map.viewportRequest?.id
        compose.onNodeWithTag("itinerary-scope-WHOLE_TRIP").performClick()
        compose.waitUntil(5_000) { model.state.value.mapScope == MapScope.WHOLE_TRIP }
        compose.onNodeWithText("可编辑日行程").assertDoesNotExist()
        compose.onNodeWithTag("whole-trip-day-day-1").assertExists()
        compose.onNodeWithTag("whole-trip-day-day-2").assertExists()
        assertEquals(dayOneRequestId?.plus(1), model.state.value.map.viewportRequest?.id)

        val wholeTripRequestId = model.state.value.map.viewportRequest?.id
        compose.onNodeWithTag("itinerary-scope-day-2").performClick()
        compose.waitUntil(5_000) { model.state.value.selectedDayId == "day-2" }
        compose.onNodeWithText("可编辑日行程").assertIsDisplayed()
        assertEquals(MapScope.SINGLE_DAY, model.state.value.mapScope)
        assertEquals(wholeTripRequestId?.plus(1), model.state.value.map.viewportRequest?.id)

        compose.onNodeWithTag("section-PLACE_POOL").performClick()
        compose.waitUntil(5_000) { model.state.value.section == WorkspaceSection.PLACE_POOL }
        val returnedPlaceRequestId = model.state.value.map.viewportRequest?.id
        compose.onNodeWithTag("section-ITINERARY").performClick()
        compose.waitUntil(5_000) { model.state.value.section == WorkspaceSection.ITINERARY }
        assertEquals(ItineraryScope.Day("day-2"), model.state.value.itineraryScope)
        assertEquals(returnedPlaceRequestId?.plus(1), model.state.value.map.viewportRequest?.id)

        compose.onNodeWithTag("workspace-sheet-handle").performTouchInput {
            down(center)
            moveTo(Offset(center.x, center.y + 200f))
            advanceEventTime(100)
            up()
        }
        compose.waitUntil(5_000) { model.state.value.sheetLevel == WorkspaceSheetLevel.COLLAPSED }
        compose.onNodeWithTag("workspace-sheet-handle").assertIsDisplayed().performTouchInput {
            down(center)
            moveTo(Offset(center.x, center.y - 200f))
            advanceEventTime(100)
            up()
        }
        compose.waitUntil(5_000) { model.state.value.sheetLevel == WorkspaceSheetLevel.HALF }
    }

    private fun enterWorkspace() {
        compose.onNodeWithTag("continue-trip-trip").performClick()
        compose.onNodeWithTag("workspace-search-launcher").assertIsDisplayed()
    }

    private fun productionLocationDependencies(
        requestStore: InMemoryLocationPermissionRequestStore,
    ): AppNavigationDependencies {
        val source = object : PlaceSearchDataSource {
            override suspend fun search(keyword: String, city: String?) = emptyList<PlaceCandidate>()
        }
        val routeCoordinator = object : RouteRefreshCoordinator {
            override fun start(scope: CoroutineScope) = Unit
            override suspend fun retry(legId: String) = false
            override suspend fun updateDetails(
                legId: String,
                selectedModeOverride: TransportMode?,
                durationOverrideSeconds: Int?,
                note: String?,
            ) = false
        }
        val consentStore = AmapConsentStore(
            MemoryConsentPersistence(true),
            RecordingConsentReporter(),
            ConsentRegistry(),
        )
        return AppNavigationDependencies(
            savedPlaceRepository = Places(),
            itineraryRepository = Itineraries(),
            routeLegRepository = Legs(),
            mapPreferences = InMemoryMapPreferences(),
            locationPermissionRequestStore = requestStore,
            consentStore = consentStore,
            runtimeSessionFactory = { fact ->
                AmapRuntimeSession(fact.generation, fact.token, source, routeCoordinator)
            },
        )
    }

    private val EmptyDeleteImpactProvider = object : com.yangchengwei.easytrip.trip.ui.DeleteImpactProvider {
        override suspend fun trip(tripId: String) = com.yangchengwei.easytrip.trip.ui.TripDeleteImpact(0, 0, 0, 0, 0)
        override suspend fun day(dayId: String) = com.yangchengwei.easytrip.trip.ui.DayDeleteImpact(0, 0, 0)
    }

    private fun consentNavigationFixture(decision: Boolean?): ConsentNavigationFixture {
        val persistence = MemoryConsentPersistence(decision)
        val reporter = RecordingConsentReporter()
        val store = AmapConsentStore(persistence, reporter, ConsentRegistry())
        return ConsentNavigationFixture(store, reporter) { dependencies ->
            AppNavigation(
                service = TripService(Trips()),
                repository = Trips(),
                impacts = object : com.yangchengwei.easytrip.trip.ui.DeleteImpactProvider {
                    override suspend fun trip(tripId: String) = com.yangchengwei.easytrip.trip.ui.TripDeleteImpact(0, 0, 0, 0, 0)
                    override suspend fun day(dayId: String) = com.yangchengwei.easytrip.trip.ui.DayDeleteImpact(0, 0, 0)
                },
                dependencies = dependencies,
                mapHostFactory = ::TestMapHost,
            )
        }
    }

    private class ConsentNavigationFixture(
        val store: AmapConsentStore,
        val reporter: RecordingConsentReporter,
        private val renderNavigation: @androidx.compose.runtime.Composable (AppNavigationDependencies) -> Unit,
    ) {
        var runtimeSessions = 0
        var stoppedSessions = 0
        private val source = object : PlaceSearchDataSource {
            override suspend fun search(keyword: String, city: String?) = emptyList<PlaceCandidate>()
        }
        private val coordinator = object : RouteRefreshCoordinator {
            override fun start(scope: CoroutineScope) = Unit
            override suspend fun retry(legId: String) = false
            override suspend fun updateDetails(legId: String, selectedModeOverride: TransportMode?, durationOverrideSeconds: Int?, note: String?) = false
        }
        @androidx.compose.runtime.Composable fun render() = renderNavigation(
            AppNavigationDependencies(
                savedPlaceRepository = Places(),
                itineraryRepository = Itineraries(),
                routeLegRepository = Legs(),
                mapPreferences = InMemoryMapPreferences(),
                locationPermissionRequestStore = InMemoryLocationPermissionRequestStore(),
                consentStore = store,
                runtimeSessionFactory = { fact ->
                    runtimeSessions++
                    AmapRuntimeSession(fact.generation, fact.token, source, coordinator)
                },
                stopRuntimeSession = { stoppedSessions++ },
            ),
        )
    }

    private class MemoryConsentPersistence(private var decision: Boolean?) : AmapConsentPersistence {
        override fun readDecision() = decision
        override fun writeDecision(accepted: Boolean) { decision = accepted }
    }

    private class RecordingConsentReporter : AmapPrivacyReporter {
        var shownCalls = 0
        var failShown = false
        val decisions = mutableListOf<Boolean>()
        override suspend fun reportShown() {
            shownCalls++
            if (failShown) {
                failShown = false
                throw IllegalStateException("show failed")
            }
        }
        override suspend fun reportDecision(accepted: Boolean) { decisions += accepted }
    }

    private class DelayedUpdatePlaces : SavedPlaceRepository {
        val updateGate = CompletableDeferred<Unit>()
        var updateCalls = 0
        private val place = SavedPlace("saved", "trip", "saved-poi", "保存地点", "地址", GeoPoint(39.8, 116.3), "", emptyList())
        override fun observePlaces(tripId: String, tagIds: Set<String>) = flowOf(listOf(place))
        override fun observeTags(tripId: String) = flowOf(emptyList<PlaceTag>())
        override fun observeSavedPoiIds(tripId: String) = flowOf(setOf("saved-poi"))
        override suspend fun save(tripId: String, candidate: PlaceCandidate) = SavePlaceResult.Saved("saved")
        override suspend fun updateDetails(placeId: String, note: String, tagNames: Set<String>) {
            updateCalls++
            updateGate.await()
        }
        override suspend fun usageCount(placeId: String) = 0
        override suspend fun deletionImpact(placeId: String) = PlaceDeletionImpact(0, 0)
        override suspend fun deletePlaceAndReferences(placeId: String) = Unit
    }

    private class EditingPlaces : SavedPlaceRepository {
        private val place = SavedPlace("saved", "trip", "saved-poi", "新编辑地点", "新地址", GeoPoint(39.8, 116.3), "", emptyList())
        override fun observePlaces(tripId: String, tagIds: Set<String>) = flowOf(listOf(place))
        override fun observeTags(tripId: String) = flowOf(emptyList<PlaceTag>())
        override fun observeSavedPoiIds(tripId: String) = flowOf(setOf("saved-poi"))
        override suspend fun save(tripId: String, candidate: PlaceCandidate) = SavePlaceResult.Saved("saved")
        override suspend fun updateDetails(placeId: String, note: String, tagNames: Set<String>) = Unit
        override suspend fun usageCount(placeId: String) = 0
        override suspend fun deletionImpact(placeId: String) = com.yangchengwei.easytrip.place.domain.PlaceDeletionImpact(usageCount(placeId), 0)
        override suspend fun deletePlaceAndReferences(placeId: String) = Unit
    }

    private class RetryImpactPlaces : SavedPlaceRepository {
        val impactGate = CompletableDeferred<PlaceDeletionImpact>()
        var impactCalls = 0
        private val place = SavedPlace("saved", "trip", "poi", "待删除地点", "地址", GeoPoint(39.9, 116.4), "", emptyList())
        override fun observePlaces(tripId: String, tagIds: Set<String>) = flowOf(listOf(place))
        override fun observeTags(tripId: String) = flowOf(emptyList<PlaceTag>())
        override fun observeSavedPoiIds(tripId: String) = flowOf(setOf("poi"))
        override fun observeUsageCounts(tripId: String) = flowOf(mapOf("saved" to 0))
        override suspend fun save(tripId: String, candidate: PlaceCandidate) = SavePlaceResult.Saved("saved")
        override suspend fun updateDetails(placeId: String, note: String, tagNames: Set<String>) = Unit
        override suspend fun usageCount(placeId: String) = 0
        override suspend fun deletionImpact(placeId: String): PlaceDeletionImpact {
            impactCalls++
            if (impactCalls == 1) error("影响查询失败")
            return impactGate.await()
        }
        override suspend fun deletePlaceAndReferences(placeId: String) = Unit
    }

    private class DelayedDeletePlaces : SavedPlaceRepository {
        val usage = CompletableDeferred<Int>()
        val deleteGate = CompletableDeferred<Unit>()
        var deleteCalls = 0
        private val place = SavedPlace("saved", "trip", "poi", "待删除地点", "地址", GeoPoint(39.9, 116.4), "", emptyList())
        override fun observePlaces(tripId: String, tagIds: Set<String>) = flowOf(listOf(place))
        override fun observeTags(tripId: String) = flowOf(emptyList<PlaceTag>())
        override fun observeSavedPoiIds(tripId: String) = flowOf(setOf("poi"))
        override fun observeUsageCounts(tripId: String) = flowOf(mapOf("saved" to 0))
        override suspend fun save(tripId: String, candidate: PlaceCandidate) = SavePlaceResult.Saved("saved")
        override suspend fun updateDetails(placeId: String, note: String, tagNames: Set<String>) = Unit
        override suspend fun usageCount(placeId: String) = usage.await()
        override suspend fun deletionImpact(placeId: String) = com.yangchengwei.easytrip.place.domain.PlaceDeletionImpact(usageCount(placeId), 0)
        override suspend fun deletePlaceAndReferences(placeId: String) {
            deleteCalls++
            deleteGate.await()
        }
    }

    private fun consentToken(): com.yangchengwei.easytrip.amap.AmapConsentToken {
        val gate = com.yangchengwei.easytrip.amap.TestConsentGate()
        gate.show()
        return requireNotNull(gate.decide(true))
    }

    private class ZoomRecordingHost(context: Context) : AmapMapHost {
        override val view = View(context)
        override fun canRenderBeforeReady() = true
        private var consumedViewportId: Long? = null
        private var consumedViewportInsets = MapViewportInsets()
        var viewportCalls = 0
            private set
        var zoomInCalls = 0
            private set
        override fun onCreate() = Unit
        override fun onResume() = Unit
        override fun onPause() = Unit
        override fun onDestroy() = Unit
        override fun zoomIn() { zoomInCalls++ }
        override fun render(
            model: MapUiModel,
            layer: MapLayer,
            onMarkerClick: (String) -> Unit,
            onMapPoiClick: (MapPoiUi) -> Unit,
            onLayerError: (Throwable, MapLayer) -> Unit,
        ) {
            val rendering = viewportRendering(
                consumedViewportId,
                model.viewportRequest,
                consumedViewportInsets,
            )
            consumedViewportId = rendering.consumedRequestId
            consumedViewportInsets = rendering.consumedSafeInsets
            if (rendering.command != null) viewportCalls++
        }
    }

    private class LocationRecordingHost(
        context: Context,
        private val locationCalls: AtomicInteger,
    ) : AmapMapHost {
        override val view = View(context)
        override fun canRenderBeforeReady() = true
        override fun onCreate() = Unit
        override fun onResume() = Unit
        override fun onPause() = Unit
        override fun onDestroy() = Unit
        override fun showCurrentLocation() { locationCalls.incrementAndGet() }
        override fun render(
            model: MapUiModel,
            layer: MapLayer,
            onMarkerClick: (String) -> Unit,
            onMapPoiClick: (MapPoiUi) -> Unit,
            onLayerError: (Throwable, MapLayer) -> Unit,
        ) = Unit
    }

    private class TestMapHost(context: Context) : AmapMapHost {
        override val view = View(context)
        override fun canRenderBeforeReady() = true
        private var consumedViewportId: Long? = null
        private var consumedViewportInsets = MapViewportInsets()
        var viewportCalls = 0
            private set
        override fun onCreate() = Unit
        override fun onResume() = Unit
        override fun onPause() = Unit
        override fun onDestroy() = Unit
        override fun render(
            model: MapUiModel,
            layer: MapLayer,
            onMarkerClick: (String) -> Unit,
            onMapPoiClick: (MapPoiUi) -> Unit,
            onLayerError: (Throwable, MapLayer) -> Unit,
        ) {
            val rendering = viewportRendering(
                consumedViewportId,
                model.viewportRequest,
                consumedViewportInsets,
            )
            consumedViewportId = rendering.consumedRequestId
            consumedViewportInsets = rendering.consumedSafeInsets
            if (rendering.command != null) viewportCalls++
        }
    }

    private class MarkerClickHost(context: Context) : AmapMapHost {
        override val view = View(context)
        override fun canRenderBeforeReady() = true
        private var savedPlaceMarkerKey: String? = null
        private var markerCallback: (String) -> Unit = {}
        private var consumedViewportId: Long? = null
        private var consumedViewportInsets = MapViewportInsets()
        var viewportCalls = 0
            private set
        override fun onCreate() = Unit
        override fun onResume() = Unit
        override fun onPause() = Unit
        override fun onDestroy() = Unit
        override fun render(
            model: MapUiModel,
            layer: MapLayer,
            onMarkerClick: (String) -> Unit,
            onMapPoiClick: (MapPoiUi) -> Unit,
            onLayerError: (Throwable, MapLayer) -> Unit,
        ) {
            val rendering = viewportRendering(
                consumedViewportId,
                model.viewportRequest,
                consumedViewportInsets,
            )
            consumedViewportId = rendering.consumedRequestId
            consumedViewportInsets = rendering.consumedSafeInsets
            if (rendering.command != null) viewportCalls++
            savedPlaceMarkerKey = model.markers.firstOrNull { it.savedPlaceId == "p" }?.key
            markerCallback = onMarkerClick
        }
        fun hasSavedPlaceMarker() = savedPlaceMarkerKey != null
        fun clickSavedPlaceMarker() = markerCallback(requireNotNull(savedPlaceMarkerKey))
    }

    private class FailingMapHost(context: Context) : AmapMapHost {
        override val view = View(context)
        override fun canRenderBeforeReady() = true
        override fun onCreate() = Unit
        override fun onResume() = Unit
        override fun onPause() = Unit
        override fun onDestroy() = Unit
        override fun render(
            model: MapUiModel,
            layer: MapLayer,
            onMarkerClick: (String) -> Unit,
            onMapPoiClick: (MapPoiUi) -> Unit,
            onLayerError: (Throwable, MapLayer) -> Unit,
        ) = error("map unavailable")
    }

    private class FirstSatelliteFailingMapHost(context: Context) : AmapMapHost {
        override val view = View(context)
        override fun canRenderBeforeReady() = true
        override fun onCreate() = Unit
        override fun onResume() = Unit
        override fun onPause() = Unit
        override fun onDestroy() = Unit
        override fun render(
            model: MapUiModel,
            layer: MapLayer,
            onMarkerClick: (String) -> Unit,
            onMapPoiClick: (MapPoiUi) -> Unit,
            onLayerError: (Throwable, MapLayer) -> Unit,
        ) {
            if (layer == MapLayer.SATELLITE) onLayerError(IllegalStateException("layer unavailable"), MapLayer.STANDARD)
        }
    }

    private class LayerFailingMapHost(context: Context, private val renderedLayers: MutableList<MapLayer>) : AmapMapHost {
        override val view = View(context)
        override fun canRenderBeforeReady() = true
        override fun onCreate() = Unit
        override fun onResume() = Unit
        override fun onPause() = Unit
        override fun onDestroy() = Unit
        override fun render(
            model: MapUiModel,
            layer: MapLayer,
            onMarkerClick: (String) -> Unit,
            onMapPoiClick: (MapPoiUi) -> Unit,
            onLayerError: (Throwable, MapLayer) -> Unit,
        ) {
            renderedLayers += layer
            if (layer == MapLayer.SATELLITE) onLayerError(IllegalStateException("layer unavailable"), MapLayer.STANDARD)
        }
    }

    private class PoiHost(context: android.content.Context) : AmapMapHost {
        override val view = android.view.View(context)
        override fun canRenderBeforeReady() = true
        private var callback: (MapPoiUi) -> Unit = {}
        override fun onCreate() = Unit
        override fun onResume() = Unit
        override fun onPause() = Unit
        override fun onDestroy() = Unit
        override fun render(
            model: MapUiModel,
            layer: MapLayer,
            onMarkerClick: (String) -> Unit,
            onMapPoiClick: (MapPoiUi) -> Unit,
            onLayerError: (Throwable, MapLayer) -> Unit,
        ) {
            callback = onMapPoiClick
        }
        fun emit(poi: MapPoiUi) = callback(poi)
    }

    private class DeferredPlaces : SavedPlaceRepository {
        override fun observePlaces(tripId: String, tagIds: Set<String>) = kotlinx.coroutines.flow.emptyFlow<List<SavedPlace>>()
        override fun observeTags(tripId: String) = kotlinx.coroutines.flow.emptyFlow<List<PlaceTag>>()
        override fun observeSavedPoiIds(tripId: String) = kotlinx.coroutines.flow.emptyFlow<Set<String>>()
        override suspend fun save(tripId: String, candidate: PlaceCandidate) = SavePlaceResult.Saved("p")
        override suspend fun updateDetails(placeId: String, note: String, tagNames: Set<String>) = Unit
        override suspend fun usageCount(placeId: String) = 0
        override suspend fun deletionImpact(placeId: String) = PlaceDeletionImpact(0, 0)
        override suspend fun deletePlaceAndReferences(placeId: String) = Unit
    }

    private class DeferredTrips : TripRepository {
        override fun observeTrip(tripId: String) = kotlinx.coroutines.flow.emptyFlow<TripWithDays?>()
        override fun observeTrips() = flowOf(emptyList<TripSummary>())
        override suspend fun createTrip(command: CreateTrip) = "trip"
        override suspend fun renameTrip(tripId: String, name: String) = Unit
        override suspend fun setStartDate(tripId: String, startDate: LocalDate?) = Unit
        override suspend fun dateRangeDeletionCounts(tripId: String, dayIds: List<String>) = com.yangchengwei.easytrip.trip.domain.DateRangeDeletionCounts(0, 0, 0)
        override suspend fun applyDateRange(command: com.yangchengwei.easytrip.trip.domain.DateRangeApply) = Unit
        override suspend fun setTravelMode(tripId: String, mode: TravelMode) = Unit
        override suspend fun insertDay(tripId: String, anchorDayId: String?, side: InsertSide) = "day"
        override suspend fun moveDay(tripId: String, dayId: String, targetIndex: Int) = Unit
        override suspend fun deleteDay(command: com.yangchengwei.easytrip.trip.domain.DayDeletion) = Unit
        override suspend fun deleteTrip(tripId: String) = Unit
    }

    private class LongNameTrips(private val name: String) : TripRepository {
        override fun observeTrip(tripId: String) = flowOf(TripWithDays("trip", name, LocalDate.of(2026, 8, 25), TravelMode.FLEXIBLE, listOf(TripDay("day-1", 0))))
        override fun observeTrips() = flowOf(emptyList<TripSummary>())
        override suspend fun createTrip(command: CreateTrip) = "trip"
        override suspend fun renameTrip(tripId: String, name: String) = Unit
        override suspend fun setStartDate(tripId: String, startDate: LocalDate?) = Unit
        override suspend fun dateRangeDeletionCounts(tripId: String, dayIds: List<String>) = com.yangchengwei.easytrip.trip.domain.DateRangeDeletionCounts(0, 0, 0)
        override suspend fun applyDateRange(command: com.yangchengwei.easytrip.trip.domain.DateRangeApply) = Unit
        override suspend fun setTravelMode(tripId: String, mode: TravelMode) = Unit
        override suspend fun insertDay(tripId: String, anchorDayId: String?, side: InsertSide) = "day"
        override suspend fun moveDay(tripId: String, dayId: String, targetIndex: Int) = Unit
        override suspend fun deleteDay(command: com.yangchengwei.easytrip.trip.domain.DayDeletion) = Unit
        override suspend fun deleteTrip(tripId: String) = Unit
    }

    private class NoDayTrips : TripRepository {
        override fun observeTrip(tripId: String) = flowOf(TripWithDays("trip", "空旅行", null, TravelMode.FLEXIBLE, emptyList()))
        override fun observeTrips() = flowOf(emptyList<TripSummary>())
        override suspend fun createTrip(command: CreateTrip) = "trip"
        override suspend fun renameTrip(tripId: String, name: String) = Unit
        override suspend fun setStartDate(tripId: String, startDate: LocalDate?) = Unit
        override suspend fun dateRangeDeletionCounts(tripId: String, dayIds: List<String>) = com.yangchengwei.easytrip.trip.domain.DateRangeDeletionCounts(0, 0, 0)
        override suspend fun applyDateRange(command: com.yangchengwei.easytrip.trip.domain.DateRangeApply) = Unit
        override suspend fun setTravelMode(tripId: String, mode: TravelMode) = Unit
        override suspend fun insertDay(tripId: String, anchorDayId: String?, side: InsertSide) = "day"
        override suspend fun moveDay(tripId: String, dayId: String, targetIndex: Int) = Unit
        override suspend fun deleteDay(command: com.yangchengwei.easytrip.trip.domain.DayDeletion) = Unit
        override suspend fun deleteTrip(tripId: String) = Unit
    }

    private class AppendableNoDayTrips : TripRepository {
        private val trip = MutableStateFlow(TripWithDays("trip", "空旅行", null, TravelMode.FLEXIBLE, emptyList()))
        override fun observeTrip(tripId: String) = trip
        override fun observeTrips() = flowOf(emptyList<TripSummary>())
        override suspend fun createTrip(command: CreateTrip) = "trip"
        override suspend fun renameTrip(tripId: String, name: String) = Unit
        override suspend fun setStartDate(tripId: String, startDate: LocalDate?) = Unit
        override suspend fun dateRangeDeletionCounts(tripId: String, dayIds: List<String>) = com.yangchengwei.easytrip.trip.domain.DateRangeDeletionCounts(0, 0, 0)
        override suspend fun applyDateRange(command: com.yangchengwei.easytrip.trip.domain.DateRangeApply) = Unit
        override suspend fun setTravelMode(tripId: String, mode: TravelMode) = Unit
        override suspend fun insertDay(tripId: String, anchorDayId: String?, side: InsertSide): String {
            trip.value = trip.value.copy(days = listOf(TripDay("day-1", 0)))
            return "day-1"
        }
        override suspend fun moveDay(tripId: String, dayId: String, targetIndex: Int) = Unit
        override suspend fun deleteDay(command: com.yangchengwei.easytrip.trip.domain.DayDeletion) = Unit
        override suspend fun deleteTrip(tripId: String) = Unit
    }

    private class DatedTrips : TripRepository {
        override fun observeTrip(tripId: String) = flowOf(
            TripWithDays(
                "trip",
                "川西",
                LocalDate.of(2026, 8, 25),
                TravelMode.FLEXIBLE,
                listOf(TripDay("day-1", 0), TripDay("day-2", 1)),
            ),
        )
        override fun observeTrips() = flowOf(emptyList<TripSummary>())
        override suspend fun createTrip(command: CreateTrip) = "trip"
        override suspend fun renameTrip(tripId: String, name: String) = Unit
        override suspend fun setStartDate(tripId: String, startDate: LocalDate?) = Unit
        override suspend fun dateRangeDeletionCounts(tripId: String, dayIds: List<String>) = com.yangchengwei.easytrip.trip.domain.DateRangeDeletionCounts(0, 0, 0)
        override suspend fun applyDateRange(command: com.yangchengwei.easytrip.trip.domain.DateRangeApply) = Unit
        override suspend fun setTravelMode(tripId: String, mode: TravelMode) = Unit
        override suspend fun insertDay(tripId: String, anchorDayId: String?, side: InsertSide) = "day"
        override suspend fun moveDay(tripId: String, dayId: String, targetIndex: Int) = Unit
        override suspend fun deleteDay(command: com.yangchengwei.easytrip.trip.domain.DayDeletion) = Unit
        override suspend fun deleteTrip(tripId: String) = Unit
    }

    private class LongTrips : TripRepository {
        override fun observeTrip(tripId: String) = flowOf(
            TripWithDays(
                "trip",
                "长旅行",
                LocalDate.of(2026, 8, 25),
                TravelMode.FLEXIBLE,
                (0..7).map { TripDay("day-${it + 1}", it) },
            ),
        )
        override fun observeTrips() = flowOf(emptyList<TripSummary>())
        override suspend fun createTrip(command: CreateTrip) = "trip"
        override suspend fun renameTrip(tripId: String, name: String) = Unit
        override suspend fun setStartDate(tripId: String, startDate: LocalDate?) = Unit
        override suspend fun dateRangeDeletionCounts(tripId: String, dayIds: List<String>) = com.yangchengwei.easytrip.trip.domain.DateRangeDeletionCounts(0, 0, 0)
        override suspend fun applyDateRange(command: com.yangchengwei.easytrip.trip.domain.DateRangeApply) = Unit
        override suspend fun setTravelMode(tripId: String, mode: TravelMode) = Unit
        override suspend fun insertDay(tripId: String, anchorDayId: String?, side: InsertSide) = "day"
        override suspend fun moveDay(tripId: String, dayId: String, targetIndex: Int) = Unit
        override suspend fun deleteDay(command: com.yangchengwei.easytrip.trip.domain.DayDeletion) = Unit
        override suspend fun deleteTrip(tripId: String) = Unit
    }

    private class Trips : TripRepository {
        override fun observeTrip(tripId: String) = flowOf(
            TripWithDays(
                "trip",
                "川西",
                null,
                TravelMode.FLEXIBLE,
                listOf(TripDay("day-1", 0), TripDay("day-2", 1)),
            ),
        )
        override fun observeTrips() = flowOf(
            listOf(TripSummary("trip", "川西", null, TravelMode.FLEXIBLE, 2)),
        )
        override suspend fun createTrip(command: CreateTrip) = "trip"
        override suspend fun renameTrip(tripId: String, name: String) = Unit
        override suspend fun setStartDate(tripId: String, startDate: LocalDate?) = Unit
        override suspend fun dateRangeDeletionCounts(tripId: String, dayIds: List<String>) = com.yangchengwei.easytrip.trip.domain.DateRangeDeletionCounts(0, 0, 0)
        override suspend fun applyDateRange(command: com.yangchengwei.easytrip.trip.domain.DateRangeApply) = Unit
        override suspend fun setTravelMode(tripId: String, mode: TravelMode) = Unit
        override suspend fun insertDay(tripId: String, anchorDayId: String?, side: InsertSide) = "day"
        override suspend fun moveDay(tripId: String, dayId: String, targetIndex: Int) = Unit
        override suspend fun deleteDay(command: com.yangchengwei.easytrip.trip.domain.DayDeletion) = Unit
        override suspend fun deleteTrip(tripId: String) = Unit
    }

    private class Places : SavedPlaceRepository {
        override fun observePlaces(tripId: String, tagIds: Set<String>) = flowOf(listOf(SavedPlace("p", "trip", "poi", "酒店", "", GeoPoint(1.0, 2.0), "", emptyList())))
        override fun observeTags(tripId: String) = flowOf(emptyList<PlaceTag>())
        override fun observeSavedPoiIds(tripId: String) = flowOf(emptySet<String>())
        override suspend fun save(tripId: String, candidate: PlaceCandidate) = SavePlaceResult.Saved("p")
        override suspend fun updateDetails(placeId: String, note: String, tagNames: Set<String>) = Unit
        override suspend fun usageCount(placeId: String) = 0
        override suspend fun deletionImpact(placeId: String) = com.yangchengwei.easytrip.place.domain.PlaceDeletionImpact(usageCount(placeId), 0)
        override suspend fun deletePlaceAndReferences(placeId: String) = Unit
    }

    private class MissingTargetItineraries : ItineraryRepository {
        override fun observeDay(dayId: String) = flowOf(DayItinerary(dayId, "trip", emptyList()))
        override suspend fun addItem(dayId: String, savedPlaceId: String, targetIndex: Int): String {
            throw com.yangchengwei.easytrip.itinerary.domain.TargetDayNotFoundException(dayId)
        }
        override suspend fun moveItem(itemId: String, targetDayId: String, targetIndex: Int) = Unit
        override suspend fun deleteItem(itemId: String) = Unit
        override suspend fun updateTiming(itemId: String, arrivalTime: LocalTime?, stayMinutes: Int?) = Unit
        override suspend fun updateDetails(itemId: String, arrivalTime: LocalTime?, stayMinutes: Int?, note: String?) = Unit
        override suspend fun removePlaceOccurrences(placeId: String) = Unit
    }

    private class SchedulingItineraries : ItineraryRepository {
        override fun observeDay(dayId: String) = flowOf(
            DayItinerary(
                dayId,
                "trip",
                if (dayId == "day-1") listOf(
                    ItineraryItem("scheduled", ItineraryPlace("p", "地点", "", GeoPoint(1.0, 2.0)), null, null),
                ) else emptyList(),
            ),
        )
        override suspend fun addItem(dayId: String, savedPlaceId: String, targetIndex: Int) = "i"
        override suspend fun moveItem(itemId: String, targetDayId: String, targetIndex: Int) = Unit
        override suspend fun deleteItem(itemId: String) = Unit
        override suspend fun updateTiming(itemId: String, arrivalTime: LocalTime?, stayMinutes: Int?) = Unit
        override suspend fun updateDetails(itemId: String, arrivalTime: LocalTime?, stayMinutes: Int?, note: String?) = error("Fake itinerary details are not modeled")
        override suspend fun removePlaceOccurrences(placeId: String) = Unit
    }

    private class Itineraries : ItineraryRepository {
        override fun observeDay(dayId: String) = flowOf(
            DayItinerary(
                dayId,
                "trip",
                listOf(
                    ItineraryItem(
                        "i-$dayId",
                        ItineraryPlace("p-$dayId", if (dayId == "day-1") "酒店" else "景点", "", GeoPoint(1.0, if (dayId == "day-1") 2.0 else 3.0)),
                        null,
                        null,
                    ),
                ),
            ),
        )
        override suspend fun addItem(dayId: String, savedPlaceId: String, targetIndex: Int) = "i"
        override suspend fun moveItem(itemId: String, targetDayId: String, targetIndex: Int) = Unit
        override suspend fun deleteItem(itemId: String) = Unit
        override suspend fun updateTiming(itemId: String, arrivalTime: LocalTime?, stayMinutes: Int?) = Unit
        override suspend fun updateDetails(itemId: String, arrivalTime: LocalTime?, stayMinutes: Int?, note: String?) = error("Fake itinerary details are not modeled")
        override suspend fun removePlaceOccurrences(placeId: String) = Unit
    }

    private class Legs : RouteLegRepository {
        override fun observeDay(dayId: String) = flowOf(emptyList<com.yangchengwei.easytrip.route.data.RouteLegEntity>())
        override fun observePending(): Flow<List<RouteLegWithEndpoints>> = flowOf(emptyList())
        override suspend fun get(legId: String) = null
        override suspend fun requeueTransientFailures() = 0
        override suspend fun recoverInterruptedCalculations(online: Boolean) = 0
        override suspend fun repairCorruptPolyline(legId: String, version: Long) = false
        override suspend fun claimIfVersionMatches(legId: String, version: Long) = false
        override suspend fun waitForNetworkIfVersionMatches(legId: String, version: Long) = false
        override suspend fun releaseClaimIfVersionMatches(legId: String, version: Long, online: Boolean) = false
        override suspend fun completeIfVersionMatches(legId: String, version: Long, result: RouteResult) = false
        override suspend fun failIfVersionMatches(legId: String, version: Long, failure: RoutePlanOutcome.Failure) = false
        override suspend fun updateDetails(legId: String, selectedModeOverride: com.yangchengwei.easytrip.core.model.TransportMode?, durationOverrideSeconds: Int?, note: String?, online: Boolean): Boolean = error("Fake route details are not modeled")
        override suspend fun retry(legId: String, online: Boolean) = false
    }
}
