package com.yangchengwei.easytrip

import android.content.Context
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.room.Room
import com.yangchengwei.easytrip.amap.TestConsentGate
import com.yangchengwei.easytrip.permission.InMemoryLocationPermissionRequestStore
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.itinerary.data.RoomItineraryRepository
import com.yangchengwei.easytrip.itinerary.domain.AddItineraryItemResult
import com.yangchengwei.easytrip.itinerary.domain.ItineraryRepository
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.amap.PlaceSearchDataSource
import com.yangchengwei.easytrip.place.data.RoomSavedPlaceRepository
import com.yangchengwei.easytrip.route.data.RoomRouteLegRepository
import com.yangchengwei.easytrip.trip.data.RoomTripRepository
import com.yangchengwei.easytrip.trip.domain.CreateTrip
import com.yangchengwei.easytrip.workspace.AmapMapHost
import com.yangchengwei.easytrip.workspace.MapLayer
import com.yangchengwei.easytrip.workspace.MapPoiUi
import com.yangchengwei.easytrip.workspace.MapUiModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference

class V2AcceptanceTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var database: EasyTripDatabase
    private lateinit var observationScope: CoroutineScope
    private var nextId = 0

    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder(compose.activity, EasyTripDatabase::class.java).build()
        observationScope = CoroutineScope(Dispatchers.Default)
    }

    @After fun tearDown() {
        compose.runOnIdle { compose.activity.setContent {} }
        observationScope.cancel()
        database.close()
    }

    @Test fun searchCollectionAndMapFlowUsesProductionNavigationState() {
        val trips = RoomTripRepository(database.tripDao(), idFactory = { "id-${nextId++}" })
        val places = RoomSavedPlaceRepository(database, idFactory = { "id-${nextId++}" })
        val itineraries = RoomItineraryRepository(database, database.itineraryEditingDao(), database.routeLegDao(), itemIdFactory = { "item-${nextId++}" }, legIdFactory = { "leg-${nextId++}" })
        val routes = RoomRouteLegRepository(database.routeLegDao())
        val tripId = runBlocking { trips.createTrip(CreateTrip("集成验收", 2)) }
        val savedPoiIds = places.observeSavedPoiIds(tripId).stateIn(
            observationScope,
            SharingStarted.Eagerly,
            emptySet(),
        )
        val museum = candidate("museum", "博物馆", 39.91, 116.41)
        val park = candidate("park", "公园", 39.92, 116.42)
        val source = object : PlaceSearchDataSource {
            override suspend fun search(keyword: String, city: String?) = listOf(museum, park)
        }
        val consentStore = com.yangchengwei.easytrip.amap.AmapConsentStore(
            persistence = object : com.yangchengwei.easytrip.amap.AmapConsentPersistence {
                override fun readDecision(): Boolean? = null
                override fun writeDecision(accepted: Boolean) = Unit
            },
            reporter = object : com.yangchengwei.easytrip.amap.AmapPrivacyReporter {
                override suspend fun reportShown() = Unit
                override suspend fun reportDecision(accepted: Boolean) = Unit
            },
            registry = com.yangchengwei.easytrip.amap.ConsentRegistry(),
        )
        runBlocking {
            consentStore.reportShown().getOrThrow()
            consentStore.decide(true).getOrThrow()
        }
        val consent = (consentStore.state.value.fact as com.yangchengwei.easytrip.amap.AmapConsentFact.Accepted).token
        val navigationRoutes = java.util.concurrent.CopyOnWriteArrayList<String>()
        val host = AtomicReference<RecordingHost?>()
        fun waitFor(stage: String, condition: () -> Boolean) {
            try {
                compose.waitUntil(10_000, condition)
            } catch (failure: Throwable) {
                throw AssertionError("$stage failed; routes=$navigationRoutes saved=${savedPoiIds.value}", failure)
            }
        }
        fun hasTag(tag: String) = compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

        compose.setContent {
            AppNavigation(
                service = com.yangchengwei.easytrip.trip.domain.TripService(trips),
                repository = trips,
                impacts = com.yangchengwei.easytrip.trip.ui.RoomDeleteImpactProvider(database.deleteImpactDao()),
                dependencies = AppNavigationDependencies(
                    savedPlaceRepository = places,
                    itineraryRepository = itineraries,
                    routeLegRepository = routes,
                    mapPreferences = com.yangchengwei.easytrip.workspace.InMemoryMapPreferences(),
                    locationPermissionRequestStore = InMemoryLocationPermissionRequestStore(),
                    consentStore = consentStore,
                    runtimeSessionFactory = { fact ->
                        com.yangchengwei.easytrip.AmapRuntimeSession(
                            fact.generation,
                            fact.token,
                            source,
                            object : com.yangchengwei.easytrip.route.domain.RouteRefreshCoordinator {
                                override fun start(scope: CoroutineScope) = Unit
                                override suspend fun retry(legId: String) = false
                                override suspend fun updateDetails(legId: String, selectedModeOverride: com.yangchengwei.easytrip.core.model.TransportMode?, durationOverrideSeconds: Int?, note: String?) = false
                            },
                        )
                    },
                    mapConsentToken = consent,
                ),
                navigationObserver = AppNavigationObserver(navigationRoutes::add),
                mapHostFactory = { RecordingHost(it).also(host::set) },
            )
        }

        waitFor("trip list entry") { hasTag("continue-trip-$tripId") }
        compose.onNodeWithTag("continue-trip-$tripId").performClick()
        waitFor("workspace UI") { hasTag("workspace-top-bar") }
        compose.onNodeWithTag("workspace-top-bar").assertIsDisplayed()
        compose.onNodeWithTag("layer-menu").assertHasClickAction()
        compose.onNodeWithTag("section-PLACE_POOL").assertIsSelected()
        compose.onNodeWithTag("section-ITINERARY").assertExists()
        compose.onNodeWithTag("itinerary-scope-rail").assertDoesNotExist()

        compose.onNodeWithTag("workspace-search-launcher").assertHasClickAction().performClick()
        waitFor("place search UI") { hasTag("place-search-field") }
        compose.onNodeWithTag("place-search-field").assertIsDisplayed().performTextInput("博物馆")
        waitFor("place search results") {
            hasTag("place-search-bookmark-touch-museum") && hasTag("place-search-bookmark-touch-park")
        }
        compose.onAllNodesWithText("加入行程").assertCountEquals(0)

        compose.onNodeWithTag("place-search-bookmark-touch-museum").performClick()
        waitFor("museum collection") { "museum" in savedPoiIds.value }
        compose.onNodeWithContentDescription("取消收藏博物馆").assertIsDisplayed()
        compose.onNodeWithTag("place-search-bookmark-touch-park").performClick()
        waitFor("park collection") { "park" in savedPoiIds.value }
        compose.onNodeWithContentDescription("取消收藏公园").assertIsDisplayed()
        compose.onNodeWithTag("place-search-back").performClick()
        waitFor("workspace UI after search return") { hasTag("workspace-top-bar") }
        waitFor("map host initialization") { host.get() != null }
        val mapHost = requireNotNull(host.get())

        compose.runOnIdle { mapHost.emit(MapPoiUi("poi-card", "故宫", "北京市东城区", GeoPoint(39.916, 116.397))) }
        compose.onNodeWithText("故宫").assertIsDisplayed()
        assertFalse("poi-card" in savedPoiIds.value)
        compose.onNodeWithTag("place-card-collection").performClick()
        waitFor("map POI collection") { "poi-card" in savedPoiIds.value }

        compose.onNodeWithTag("section-ITINERARY").performClick()
        compose.onNodeWithTag("section-ITINERARY").assertIsSelected()
        waitFor("itinerary all-empty state") { hasTag("itinerary-all-empty") }
        compose.onNodeWithTag("itinerary-all-empty").assertIsDisplayed()
        compose.onNodeWithTag("itinerary-scope-rail").assertDoesNotExist()
    }

    internal fun executeBatch5ProductionNavigationRoomMainFlow(): Set<Batch5FrameCheckpoint> =
        runBatch5ProductionNavigationRoomMainFlow()

    @Test fun batch5ProductionNavigationRoomMainFlow() {
        Batch5ExecutableEvidence.ProductionNavigationMainFlow.verifyCheckpoints(this)
    }

    @Test fun visualBatch3ProductionWorkspaceFixtureCoversThreeDaysEightStopsAndRouteStates() = runBlocking {
        val fixture = createVisualBatch3WorkspaceFixture()

        assertEquals(listOf(4, 0, 4), fixture.days.map { day ->
            database.itineraryEditingDao().items(day.id).size
        })
        assertEquals(8, fixture.itemIds.size)
        assertEquals(
            setOf(
                com.yangchengwei.easytrip.core.model.RouteStatus.SUCCESS,
                com.yangchengwei.easytrip.core.model.RouteStatus.FAILED,
                com.yangchengwei.easytrip.core.model.RouteStatus.WAITING_NETWORK,
            ),
            fixture.days.flatMap { database.routeLegDao().legs(it.id) }.map { it.status }.toSet(),
        )
        assertEquals("visual-stop-1", database.itineraryEditingDao().item(fixture.itemIds.first())?.note)
    }

    @Test fun visualBatch3FixtureRunsInProductionNavigationWithSynchronizedDayAndWholeTripViews() {
        lateinit var fixture: VisualBatch3WorkspaceFixture
        val trips = RoomTripRepository(database.tripDao(), idFactory = { "visual-trip-${nextId++}" })
        val places = RoomSavedPlaceRepository(database, idFactory = { "visual-place-${nextId++}" })
        val itineraries = RoomItineraryRepository(
            database,
            database.itineraryEditingDao(),
            database.routeLegDao(),
            itemIdFactory = { "visual-item-${nextId++}" },
            legIdFactory = { "visual-leg-${nextId++}" },
            isOnline = { false },
        )
        val routes = RoomRouteLegRepository(database.routeLegDao())
        runBlocking { fixture = createVisualBatch3WorkspaceFixture(trips, places, itineraries, routes) }
        val hosts = java.util.concurrent.CopyOnWriteArrayList<RecordingHost>()
        setProductionNavigation(trips, places, itineraries, routes, mutableListOf(), hosts)

        compose.onNodeWithTag("continue-trip-${fixture.tripId}").performClick()
        waitForTag("workspace-top-bar")
        compose.onNodeWithTag("section-ITINERARY").performClick()
        waitForTag("item-${fixture.itemIds.first()}")
        compose.onNodeWithText("第 1 天 · 4 站").assertIsDisplayed()
        waitFor("single-day map scope") {
            hosts.lastOrNull()?.lastModel?.viewportRequest?.scope == com.yangchengwei.easytrip.workspace.MapScope.SINGLE_DAY
        }
        val dayModel = requireNotNull(hosts.lastOrNull()?.lastModel)
        assertEquals(4, dayModel.markers.size)
        assertEquals(1, dayModel.polylines.size)
        assertEquals(com.yangchengwei.easytrip.workspace.MapScope.SINGLE_DAY, dayModel.viewportRequest?.scope)
        assertEquals(fixture.days.first().id, dayModel.viewportRequest?.selectedDayId)
        compose.onNodeWithTag("itinerary-scope-WHOLE_TRIP").performClick()
        compose.onNodeWithText("全程 · 3 天 · 8 站").assertIsDisplayed()
        waitFor("whole-trip map scope") {
            hosts.lastOrNull()?.lastModel?.viewportRequest?.scope == com.yangchengwei.easytrip.workspace.MapScope.WHOLE_TRIP
        }
        val wholeModel = requireNotNull(hosts.lastOrNull()?.lastModel)
        assertEquals(8, wholeModel.markers.size)
        assertEquals(1, wholeModel.polylines.size)
        assertEquals(com.yangchengwei.easytrip.workspace.MapScope.WHOLE_TRIP, wholeModel.viewportRequest?.scope)
        assertEquals(null, wholeModel.viewportRequest?.selectedDayId)
        fixture.days.forEach { day ->
            compose.onNodeWithTag("whole-trip-timeline").performScrollToNode(hasTestTag("whole-trip-day-${day.id}"))
            compose.onNodeWithTag("whole-trip-day-${day.id}").assertIsDisplayed()
        }
        compose.onAllNodesWithTag("more-${fixture.itemIds.first()}", useUnmergedTree = true).assertCountEquals(0)

        compose.onNodeWithTag("itinerary-scope-${fixture.days.first().id}").performClick()
        waitFor("return to single-day map scope") {
            hosts.lastOrNull()?.lastModel?.viewportRequest?.scope == com.yangchengwei.easytrip.workspace.MapScope.SINGLE_DAY &&
                hosts.lastOrNull()?.lastModel?.viewportRequest?.selectedDayId == fixture.days.first().id
        }
        val returnedDayModel = requireNotNull(hosts.lastOrNull()?.lastModel)
        assertEquals(4, returnedDayModel.markers.size)
        assertEquals(1, returnedDayModel.polylines.size)
    }

    @Test fun visualBatch3ProductionNavigationEditsAndDeletesRoomItemThenReopensPersistedValue() {
        lateinit var fixture: VisualBatch3WorkspaceFixture
        val trips = RoomTripRepository(database.tripDao(), idFactory = { "visual-trip-${nextId++}" })
        val places = RoomSavedPlaceRepository(database, idFactory = { "visual-place-${nextId++}" })
        val itineraries = RoomItineraryRepository(
            database,
            database.itineraryEditingDao(),
            database.routeLegDao(),
            itemIdFactory = { "visual-item-${nextId++}" },
            legIdFactory = { "visual-leg-${nextId++}" },
            isOnline = { false },
        )
        val routes = RoomRouteLegRepository(database.routeLegDao())
        runBlocking { fixture = createVisualBatch3WorkspaceFixture(trips, places, itineraries, routes) }
        setProductionNavigation(trips, places, itineraries, routes, mutableListOf(), mutableListOf())

        compose.onNodeWithTag("continue-trip-${fixture.tripId}").performClick()
        waitForTag("workspace-top-bar")
        compose.onNodeWithTag("section-ITINERARY").performClick()
        val editedItemId = fixture.itemIds[1]
        compose.onNodeWithTag("more-$editedItemId", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("menu-timing-$editedItemId", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("arrival-time-input").performTextClearance()
        compose.onNodeWithTag("arrival-time-input").performTextInput("14:20")
        compose.onNodeWithTag("stay-minutes-input").performTextClearance()
        compose.onNodeWithTag("stay-minutes-input").performTextInput("75")
        compose.onNodeWithTag("itinerary-note-input").performTextClearance()
        compose.onNodeWithTag("itinerary-note-input").performTextInput("Visual Batch 3 持久备注")
        compose.onNodeWithText("保存时间").performClick()
        waitFor("edited Room item") {
            runBlocking { database.itineraryEditingDao().item(editedItemId) }?.let {
                it.arrivalTime == java.time.LocalTime.of(14, 20) &&
                    it.stayDurationMinutes == 75 &&
                    it.note == "Visual Batch 3 持久备注"
            } == true
        }
        compose.onNodeWithTag("more-$editedItemId", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("menu-timing-$editedItemId", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("arrival-time-input").assertTextContains("14:20")
        compose.onNodeWithTag("stay-minutes-input").assertTextContains("75")
        compose.onNodeWithTag("itinerary-note-input").assertTextContains("Visual Batch 3 持久备注")
        compose.onNodeWithText("取消").performClick()

        compose.onNodeWithTag("more-$editedItemId", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("menu-delete-$editedItemId", useUnmergedTree = true).performClick()
        compose.onNodeWithText("确认移出").performClick()
        waitFor("deleted Room item") { runBlocking { database.itineraryEditingDao().item(editedItemId) } == null }
        assertTrue(runBlocking { database.itineraryEditingDao().savedPlace(fixture.placeIds[1]) } != null)
        assertEquals(2, runBlocking { database.routeLegDao().legs(fixture.days.first().id) }.size)
    }

    private fun runBatch5ProductionNavigationRoomMainFlow(): Set<Batch5FrameCheckpoint> {
        val checkpoints = linkedSetOf<Batch5FrameCheckpoint>()
        val trips = RoomTripRepository(
            database.tripDao(),
            idFactory = { "e2e-${nextId++}" },
            database = database,
            isOnline = { false },
        )
        val places = RoomSavedPlaceRepository(database, idFactory = { "e2e-place-${nextId++}" })
        val itineraries = RoomItineraryRepository(
            database,
            database.itineraryEditingDao(),
            database.routeLegDao(),
            itemIdFactory = { "e2e-item-${nextId++}" },
            legIdFactory = { "e2e-leg-${nextId++}" },
            isOnline = { false },
        )
        val routes = RoomRouteLegRepository(database.routeLegDao())
        val service = com.yangchengwei.easytrip.trip.domain.TripService(trips)
        val tripId: String
        val originalDays: List<com.yangchengwei.easytrip.trip.domain.TripDay>
        val firstPlaceId: String
        val middlePlaceId: String
        val firstItem: String
        val middleItem: String
        val lastItem: String
        val secondDayItem: String
        val originalLegIds: List<String>
        runBlocking {
            tripId = trips.createTrip(CreateTrip("旅程 C", 2, startDate = java.time.LocalDate.parse("2026-09-01")))
            originalDays = requireNotNull(trips.observeTrip(tripId).first()).days
            firstPlaceId = (places.save(tripId, candidate("e2e-west-lake", "西湖", 30.25, 120.15)) as com.yangchengwei.easytrip.place.domain.SavePlaceResult.Saved).id
            middlePlaceId = (places.save(tripId, candidate("e2e-museum", "博物馆", 30.26, 120.16)) as com.yangchengwei.easytrip.place.domain.SavePlaceResult.Saved).id
            val lastPlace = (places.save(tripId, candidate("e2e-park", "公园", 30.27, 120.17)) as com.yangchengwei.easytrip.place.domain.SavePlaceResult.Saved).id
            firstItem = itineraries.addItem(originalDays[0].id, firstPlaceId, 0)
            middleItem = itineraries.addItem(originalDays[0].id, middlePlaceId, 1)
            lastItem = itineraries.addItem(originalDays[0].id, lastPlace, 2)
            secondDayItem = itineraries.addItem(originalDays[1].id, firstPlaceId, 0)
            originalLegIds = database.routeLegDao().legs(originalDays[0].id).map { it.id }
            val editableLeg = database.routeLegDao().legs(originalDays[0].id).last()
            assertTrue(routes.claimIfVersionMatches(editableLeg.id, editableLeg.version))
            assertTrue(
                routes.completeIfVersionMatches(
                    editableLeg.id,
                    editableLeg.version,
                    com.yangchengwei.easytrip.route.domain.RouteResult(
                        distanceMeters = 1_500,
                        durationSeconds = 900,
                        polyline = listOf(GeoPoint(30.26, 120.16), GeoPoint(30.27, 120.17)),
                    ),
                ),
            )
        }
        val navigationRoutes = java.util.concurrent.CopyOnWriteArrayList<String>()
        val hosts = java.util.concurrent.CopyOnWriteArrayList<RecordingHost>()
        setProductionNavigation(trips, places, itineraries, routes, navigationRoutes, hosts)

        compose.onNodeWithTag("continue-trip-$tripId").performClick()
        waitForTag("workspace-top-bar")
        compose.onNodeWithTag("section-ITINERARY").performClick()
        waitForTag("item-$middleItem")
        waitFor("recording map host") { hosts.isNotEmpty() }
        checkpoint(checkpoints, Batch5FrameCheckpoint.ItineraryPage)
        checkpoint(checkpoints, Batch5FrameCheckpoint.ItemEditor)

        compose.onNodeWithTag("more-$middleItem", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("menu-timing-$middleItem", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("arrival-time-input").performTextInput("09:30")
        compose.onNodeWithTag("stay-minutes-input").performTextInput("60")
        compose.onNodeWithTag("itinerary-note-input").performTextInput("二层入口集合")
        compose.onNodeWithText("保存时间").performClick()
        compose.waitUntil(5_000) {
            runBlocking { database.itineraryEditingDao().item(middleItem) }?.let {
                it.arrivalTime == java.time.LocalTime.of(9, 30) &&
                    it.stayDurationMinutes == 60 &&
                    it.note == "二层入口集合"
            } == true
        }
        compose.onNodeWithText("09:30").assertIsDisplayed()
        compose.onNodeWithText("停留 60 分钟").assertIsDisplayed()
        compose.onNodeWithTag("more-$middleItem", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("menu-timing-$middleItem", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("arrival-time-input").assertTextContains("09:30")
        compose.onNodeWithTag("stay-minutes-input").assertTextContains("60")
        compose.onNodeWithTag("itinerary-note-input").assertTextContains("二层入口集合")
        compose.onNodeWithText("取消").performClick()
        checkpoint(checkpoints, Batch5FrameCheckpoint.ItemEditComplete)

        val editedLegId = originalLegIds.last()
        compose.onNodeWithTag("edit-route-$editedLegId").performScrollTo().performClick()
        compose.onNodeWithTag("route-mode-option-DRIVE").performClick()
        compose.onNodeWithTag("route-duration-minutes-input").performTextInput("20")
        compose.onNodeWithTag("route-note-input").performTextInput("避开拥堵")
        compose.onNodeWithText("保存路段").performClick()
        compose.waitUntil(5_000) {
            runBlocking { database.routeLegDao().leg(editedLegId) }?.let {
                it.selectedMode == com.yangchengwei.easytrip.core.model.TransportMode.DRIVE &&
                    it.durationOverrideSeconds == 1_200 &&
                    it.note == "避开拥堵"
            } == true
        }
        runBlocking {
            val pending = requireNotNull(routes.get(editedLegId))
            assertTrue(routes.claimIfVersionMatches(editedLegId, pending.version))
            assertTrue(
                routes.completeIfVersionMatches(
                    editedLegId,
                    pending.version,
                    com.yangchengwei.easytrip.route.domain.RouteResult(
                        distanceMeters = 1_500,
                        durationSeconds = 900,
                        polyline = listOf(GeoPoint(30.26, 120.16), GeoPoint(30.27, 120.17)),
                    ),
                ),
            )
        }
        waitForTag("edit-route-$editedLegId")
        compose.onNodeWithTag("edit-route-$editedLegId").performScrollTo().performClick()
        compose.onNodeWithTag("route-mode-option-DRIVE").assertIsSelected()
        compose.onNodeWithTag("route-duration-minutes-input").assertTextContains("20")
        compose.onNodeWithTag("route-note-input").assertTextContains("避开拥堵")
        compose.onNodeWithText("取消").performClick()
        checkpoint(checkpoints, Batch5FrameCheckpoint.RouteEditor)

        waitFor("recorded same-day route") {
            hosts.lastOrNull()?.lastModel?.polylines?.isNotEmpty() == true
        }
        checkpoint(checkpoints, Batch5FrameCheckpoint.SingleDayMap)

        compose.onNodeWithTag("more-$middleItem", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("menu-delete-$middleItem", useUnmergedTree = true).performClick()
        compose.onNodeWithText("仅移除本次安排；收藏仍保留；相邻路线将重新计算。").assertIsDisplayed()
        compose.onNodeWithText("确认移出").performClick()
        compose.waitUntil(5_000) {
            runBlocking { database.itineraryEditingDao().items(originalDays[0].id) }.map { it.id } == listOf(firstItem, lastItem)
        }
        assertTrue(runBlocking { database.itineraryEditingDao().savedPlace(middlePlaceId) } != null)
        val bridge = runBlocking { database.routeLegDao().legs(originalDays[0].id) }.single()
        assertEquals(firstItem, bridge.fromItemId)
        assertEquals(lastItem, bridge.toItemId)
        assertFalse(bridge.id in originalLegIds)
        checkpoint(checkpoints, Batch5FrameCheckpoint.ItemDelete)

        compose.onNodeWithTag("itinerary-add-day").performClick()
        compose.onNodeWithText("添加旅行日").assertIsDisplayed()
        compose.onNodeWithText("添加一天").performClick()
        compose.waitUntil(5_000) { runBlocking { trips.observeTrip(tripId).first() }?.days?.size == 3 }
        val appendedDay = requireNotNull(runBlocking { trips.observeTrip(tripId).first() }).days.last()
        checkpoint(checkpoints, Batch5FrameCheckpoint.AppendDay)
        compose.onNodeWithTag("itinerary-scope-${appendedDay.id}").performScrollTo().performClick()
        compose.onNodeWithText("第3天 · 暂无行程").assertIsDisplayed()
        compose.onNodeWithTag("add-places-to-selected-day").performClick()
        compose.onNodeWithTag("select-places-continue").assertIsDisplayed()
        compose.onNodeWithText("取消").performClick()
        checkpoint(checkpoints, Batch5FrameCheckpoint.EmptyDay)

        compose.onNodeWithTag("itinerary-scope-WHOLE_TRIP").performClick()
        compose.onNodeWithTag("whole-trip-content").assertIsDisplayed()
        compose.onNodeWithTag("whole-trip-day-${originalDays[0].id}").assertIsDisplayed()
        compose.onNodeWithTag("whole-trip-timeline").performScrollToNode(hasTestTag("whole-trip-day-${originalDays[1].id}"))
        compose.onNodeWithTag("whole-trip-day-${originalDays[1].id}").assertIsDisplayed()
        compose.onNodeWithTag("whole-trip-timeline").performScrollToNode(hasTestTag("whole-trip-day-${appendedDay.id}"))
        compose.onNodeWithTag("whole-trip-day-${appendedDay.id}").assertIsDisplayed()
        compose.onNodeWithTag("leg-${runBlocking { database.routeLegDao().legs(originalDays[0].id) }.single().id}").performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithTag("more-$firstItem", useUnmergedTree = true).assertCountEquals(0)
        compose.onAllNodesWithTag("more-$lastItem", useUnmergedTree = true).assertCountEquals(0)
        assertTrue(runBlocking { database.routeLegDao().legsForTrip(tripId) }.all { leg ->
            val from = runBlocking { database.itineraryEditingDao().item(leg.fromItemId) }
            val to = runBlocking { database.itineraryEditingDao().item(leg.toItemId) }
            from?.tripDayId == to?.tripDayId && from?.tripDayId == leg.tripDayId
        })
        checkpoint(checkpoints, Batch5FrameCheckpoint.WholeTrip)

        compose.onNodeWithTag("itinerary-scope-${originalDays[1].id}").performClick()
        compose.onNodeWithTag("workspace-more").performClick()
        compose.onNodeWithTag("more-menu-settings").performClick()
        waitForTag("settings-date-row")
        compose.onNodeWithTag("delete-day-${originalDays[0].id}").performScrollTo().performClick()
        compose.onNodeWithText("取消").performClick()
        assertEquals(3, requireNotNull(runBlocking { trips.observeTrip(tripId).first() }).days.size)
        checkpoint(checkpoints, Batch5FrameCheckpoint.DeleteDay)
        compose.onNodeWithTag("delete-day-${originalDays[0].id}").performScrollTo().performClick()
        compose.onNodeWithText("确认删除").performClick()
        compose.waitUntil(5_000) {
            runBlocking { trips.observeTrip(tripId).first() }?.days?.map { it.id } == listOf(originalDays[1].id, appendedDay.id)
        }
        val remainingDays = requireNotNull(runBlocking { trips.observeTrip(tripId).first() }).days
        assertEquals(listOf(0, 1), remainingDays.map { it.index })
        compose.onNodeWithTag("settings-back").performClick()
        waitForTag("workspace-top-bar")
        compose.onNodeWithTag("section-ITINERARY").assertIsSelected()
        compose.onNodeWithTag("itinerary-scope-${originalDays[1].id}").assertIsSelected()
        compose.onNodeWithTag("item-$secondDayItem").assertIsDisplayed()
        assertEquals("trips/$tripId/settings", navigationRoutes.last())
        return checkpoints
    }

    private fun checkpoint(
        checkpoints: MutableSet<Batch5FrameCheckpoint>,
        checkpoint: Batch5FrameCheckpoint,
    ) {
        checkpoints += checkpoint
    }

    @Test fun productionNavigationSaveFailurePreservesDraftAndRetriesOnceAgainstRoom() {
        val trips = RoomTripRepository(
            database.tripDao(),
            idFactory = { "save-failure-${nextId++}" },
            database = database,
            isOnline = { false },
        )
        val places = RoomSavedPlaceRepository(database, idFactory = { "save-failure-place-${nextId++}" })
        val roomItineraries = RoomItineraryRepository(
            database,
            database.itineraryEditingDao(),
            database.routeLegDao(),
            itemIdFactory = { "save-failure-item-${nextId++}" },
            legIdFactory = { "save-failure-leg-${nextId++}" },
            isOnline = { false },
        )
        val failingItineraries = FailOnceUpdateDetailsRepository(roomItineraries)
        val routes = RoomRouteLegRepository(database.routeLegDao())
        val tripId: String
        val dayId: String
        val editableItemId: String
        runBlocking {
            tripId = trips.createTrip(CreateTrip("保存失败根链", 1))
            dayId = requireNotNull(trips.observeTrip(tripId).first()).days.single().id
            val museumId = (places.save(tripId, candidate("save-failure-museum", "博物馆", 39.91, 116.41)) as com.yangchengwei.easytrip.place.domain.SavePlaceResult.Saved).id
            val parkId = (places.save(tripId, candidate("save-failure-park", "公园", 39.92, 116.42)) as com.yangchengwei.easytrip.place.domain.SavePlaceResult.Saved).id
            editableItemId = roomItineraries.addItem(dayId, museumId, 0)
            roomItineraries.addItem(dayId, parkId, 1)
            val leg = database.routeLegDao().legs(dayId).single()
            assertTrue(routes.claimIfVersionMatches(leg.id, leg.version))
            assertTrue(
                routes.completeIfVersionMatches(
                    leg.id,
                    leg.version,
                    com.yangchengwei.easytrip.route.domain.RouteResult(
                        distanceMeters = 1_000,
                        durationSeconds = 600,
                        polyline = listOf(GeoPoint(39.91, 116.41), GeoPoint(39.92, 116.42)),
                    ),
                ),
            )
        }
        val hosts = java.util.concurrent.CopyOnWriteArrayList<RecordingHost>()
        setProductionNavigation(trips, places, failingItineraries, routes, mutableListOf(), hosts)

        compose.onNodeWithTag("continue-trip-$tripId").performClick()
        waitForTag("workspace-top-bar")
        compose.onNodeWithTag("section-ITINERARY").performClick()
        waitForTag("item-$editableItemId")
        compose.onNodeWithTag("more-$editableItemId", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("menu-timing-$editableItemId", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("arrival-time-input").performTextInput("09:30")
        compose.onNodeWithTag("stay-minutes-input").performTextInput("60")
        compose.onNodeWithTag("itinerary-note-input").performTextInput("东门集合")
        compose.onNodeWithText("保存时间").performClick()

        waitForTag("itinerary-save-failure")
        compose.onNodeWithText("修改尚未保存").assertIsDisplayed()
        assertEquals(1, failingItineraries.updateDetailsCalls)
        assertEquals(null, runBlocking { database.itineraryEditingDao().item(editableItemId) }?.arrivalTime)
        assertEquals(null, runBlocking { database.itineraryEditingDao().item(editableItemId) }?.stayDurationMinutes)
        assertEquals(null, runBlocking { database.itineraryEditingDao().item(editableItemId) }?.note)

        compose.onNodeWithTag("itinerary-save-failure-retry").performClick()
        waitFor("retry saves Room value") {
            assertTrue(failingItineraries.updateDetailsCalls <= 2)
            runBlocking { database.itineraryEditingDao().item(editableItemId) }?.let {
                it.arrivalTime == java.time.LocalTime.of(9, 30) &&
                    it.stayDurationMinutes == 60 &&
                    it.note == "东门集合"
            } == true
        }
        assertEquals(2, failingItineraries.updateDetailsCalls)
        compose.onAllNodesWithTag("itinerary-save-failure").assertCountEquals(0)
        compose.onAllNodesWithTag("arrival-time-input").assertCountEquals(0)
        compose.onNodeWithText("09:30").assertIsDisplayed()
        compose.onNodeWithText("停留 60 分钟").assertIsDisplayed()
        compose.onNodeWithTag("more-$editableItemId", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("menu-timing-$editableItemId", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("arrival-time-input").assertTextContains("09:30")
        compose.onNodeWithTag("stay-minutes-input").assertTextContains("60")
        compose.onNodeWithTag("itinerary-note-input").assertTextContains("东门集合")
        compose.onNodeWithText("取消").performClick()
        assertEquals("东门集合", runBlocking { roomItineraries.observeDay(dayId).first() }.items.single { it.id == editableItemId }.note)
    }

    @Test fun batch5RoomRepositoriesPreserveItemRouteDeleteAppendAndWholeTripContracts() = runBlocking {
        val trips = RoomTripRepository(database.tripDao(), idFactory = { "batch5-${nextId++}" })
        val places = RoomSavedPlaceRepository(database, idFactory = { "batch5-place-${nextId++}" })
        val itineraries = RoomItineraryRepository(
            database,
            database.itineraryEditingDao(),
            database.routeLegDao(),
            itemIdFactory = { "batch5-item-${nextId++}" },
            legIdFactory = { "batch5-leg-${nextId++}" },
        )
        val routes = RoomRouteLegRepository(database.routeLegDao())
        val tripId = trips.createTrip(CreateTrip("Batch5", 2))
        val days = requireNotNull(trips.observeTrip(tripId).first()).days
        val firstDay = days[0]
        val secondDay = days[1]
        val museum = (places.save(tripId, candidate("batch5-museum", "博物馆", 39.91, 116.41)) as com.yangchengwei.easytrip.place.domain.SavePlaceResult.Saved).id
        val park = (places.save(tripId, candidate("batch5-park", "公园", 39.92, 116.42)) as com.yangchengwei.easytrip.place.domain.SavePlaceResult.Saved).id
        val firstItem = itineraries.addItem(firstDay.id, museum, 0)
        val secondItem = itineraries.addItem(firstDay.id, park, 1)
        val bridge = database.routeLegDao().legs(firstDay.id).single()

        itineraries.updateDetails(firstItem, java.time.LocalTime.of(9, 30), 60, "早到")
        routes.updateDetails(bridge.id, com.yangchengwei.easytrip.core.model.TransportMode.DRIVE, 1_200, "收费路段", online = true)
        val savedRoute = database.routeLegDao().legs(firstDay.id).single()
        assertEquals(com.yangchengwei.easytrip.core.model.TransportMode.DRIVE, savedRoute.selectedMode)
        assertEquals(1_200, savedRoute.durationOverrideSeconds)
        assertEquals("收费路段", savedRoute.note)
        val reopened = itineraries.observeDay(firstDay.id).first().items.first { it.id == firstItem }
        assertEquals(java.time.LocalTime.of(9, 30), reopened.arrivalTime)
        assertEquals(60, reopened.stayMinutes)
        assertEquals("早到", reopened.note)

        itineraries.deleteItem(firstItem)
        assertEquals(museum, database.itineraryEditingDao().savedPlace(museum)?.id)
        assertTrue(database.routeLegDao().legs(firstDay.id).isEmpty())
        assertEquals(listOf(secondItem), itineraries.observeDay(firstDay.id).first().items.map { it.id })

        val beforeAppend = requireNotNull(trips.observeTrip(tripId).first()).days.map { it.id }
        val appendedDay = trips.insertDay(tripId, null, com.yangchengwei.easytrip.trip.domain.InsertSide.AFTER)
        val afterAppend = requireNotNull(trips.observeTrip(tripId).first()).days
        assertEquals(beforeAppend, afterAppend.dropLast(1).map { it.id })
        assertEquals(appendedDay, afterAppend.last().id)
        assertTrue(itineraries.observeDay(secondDay.id).first().items.isEmpty())
    }

    @Test fun savedOnlyPlaceDetailAddsToTravelDayThroughProductionNavigationAndReopensScheduled() {
        val trips = RoomTripRepository(database.tripDao(), idFactory = { "detail-trip-${nextId++}" })
        val places = RoomSavedPlaceRepository(database, idFactory = { "detail-place-${nextId++}" })
        val itineraries = RoomItineraryRepository(
            database,
            database.itineraryEditingDao(),
            database.routeLegDao(),
            itemIdFactory = { "detail-item-${nextId++}" },
            legIdFactory = { "detail-leg-${nextId++}" },
        )
        val routes = RoomRouteLegRepository(database.routeLegDao())
        val tripId: String
        val dayId: String
        val savedPlaceId: String
        runBlocking {
            tripId = trips.createTrip(CreateTrip("地点详情闭环", 1))
            dayId = requireNotNull(trips.observeTrip(tripId).first()).days.single().id
            savedPlaceId = (places.save(tripId, candidate("detail-only", "西湖天地", 30.24, 120.15))
                as com.yangchengwei.easytrip.place.domain.SavePlaceResult.Saved).id
        }
        val hosts = java.util.concurrent.CopyOnWriteArrayList<RecordingHost>()
        setProductionNavigation(trips, places, itineraries, routes, mutableListOf(), hosts)

        compose.onNodeWithTag("continue-trip-$tripId").performClick()
        waitForTag("workspace-top-bar")
        waitFor("recording fake map host") { hosts.isNotEmpty() }
        compose.onNodeWithTag("open-place-detail-$savedPlaceId").performClick()
        waitForTag("place-detail-bottom-sheet")
        compose.onNodeWithTag("place-detail-bookmark-outline").assertIsDisplayed()
        compose.onAllNodesWithText("已加入行程").assertCountEquals(0)
        compose.onNodeWithTag("place-detail-start-add").assertHasClickAction().performClick()
        waitForTag("target-day-$dayId")
        compose.onNodeWithTag("target-day-$dayId").performClick()
        compose.onNodeWithTag("select-target-day-submit").performClick()

        waitFor("Room itinerary item") {
            runBlocking { database.itineraryEditingDao().items(dayId) }
                .singleOrNull()?.savedPlaceId == savedPlaceId
        }
        compose.onNodeWithText("关闭").performClick()
        compose.onNodeWithTag("open-place-detail-$savedPlaceId").performClick()
        waitFor("scheduled place detail") {
            compose.onAllNodesWithTag("place-detail-bookmark-filled").fetchSemanticsNodes().isNotEmpty() &&
                compose.onAllNodesWithText("第 1 天 · 1 次").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("place-detail-bookmark-filled").assertIsDisplayed()
        compose.onNodeWithText("第 1 天 · 1 次").assertIsDisplayed()
        assertEquals(1, runBlocking { database.itineraryEditingDao().items(dayId) }.size)
        assertTrue("map evidence is the recording fake host, not RealAmap", hosts.isNotEmpty())
    }

    private data class VisualBatch3WorkspaceFixture(
        val tripId: String,
        val days: List<com.yangchengwei.easytrip.trip.domain.TripDay>,
        val itemIds: List<String>,
        val placeIds: List<String>,
    )

    private suspend fun createVisualBatch3WorkspaceFixture(
        trips: RoomTripRepository = RoomTripRepository(database.tripDao(), idFactory = { "visual-trip-${nextId++}" }),
        places: RoomSavedPlaceRepository = RoomSavedPlaceRepository(database, idFactory = { "visual-place-${nextId++}" }),
        itineraries: RoomItineraryRepository = RoomItineraryRepository(
            database,
            database.itineraryEditingDao(),
            database.routeLegDao(),
            itemIdFactory = { "visual-item-${nextId++}" },
            legIdFactory = { "visual-leg-${nextId++}" },
            isOnline = { false },
        ),
        routes: RoomRouteLegRepository = RoomRouteLegRepository(database.routeLegDao()),
    ): VisualBatch3WorkspaceFixture {
        val tripId = trips.createTrip(CreateTrip("Visual Batch 3", 3, startDate = java.time.LocalDate.parse("2026-09-06")))
        val days = requireNotNull(trips.observeTrip(tripId).first()).days
        val placeIds = (1..8).map { index ->
            (places.save(
                tripId,
                candidate("visual-poi-$index", "Visual Stop $index", 30.0 + index / 100.0, 120.0 + index / 100.0),
            ) as com.yangchengwei.easytrip.place.domain.SavePlaceResult.Saved).id
        }
        val itemIds = placeIds.mapIndexed { index, placeId ->
            val day = if (index < 4) days.first() else days.last()
            itineraries.addItem(day.id, placeId, if (index < 4) index else index - 4).also { itemId ->
                itineraries.updateDetails(itemId, java.time.LocalTime.of(9 + index, 0), 46 + index, "visual-stop-${index + 1}")
            }
        }
        val legs = database.routeLegDao().legs(days.first().id)
        if (legs.isNotEmpty()) {
            val success = legs[0]
            check(routes.claimIfVersionMatches(success.id, success.version))
            check(
                routes.completeIfVersionMatches(
                    success.id,
                    success.version,
                    com.yangchengwei.easytrip.route.domain.RouteResult(
                        1_000,
                        600,
                        listOf(GeoPoint(30.01, 120.01), GeoPoint(30.02, 120.02)),
                    ),
                ),
            )
        }
        if (legs.size > 1) {
            val failed = legs[1]
            check(routes.claimIfVersionMatches(failed.id, failed.version))
            check(routes.failIfVersionMatches(failed.id, failed.version, com.yangchengwei.easytrip.route.domain.RoutePlanOutcome.Failure(com.yangchengwei.easytrip.route.domain.RouteErrorKind.TRANSIENT, "fixture")))
        }
        return VisualBatch3WorkspaceFixture(tripId, days, itemIds, placeIds)
    }

    private fun setProductionNavigation(
        trips: RoomTripRepository,
        places: RoomSavedPlaceRepository,
        itineraries: ItineraryRepository,
        routes: RoomRouteLegRepository,
        navigationRoutes: MutableList<String>,
        hosts: MutableList<RecordingHost>,
    ) {
        compose.setContent {
            AppNavigation(
                service = com.yangchengwei.easytrip.trip.domain.TripService(trips),
                repository = trips,
                impacts = com.yangchengwei.easytrip.trip.ui.RoomDeleteImpactProvider(database.deleteImpactDao()),
                dependencies = AppNavigationDependencies(
                    savedPlaceRepository = places,
                    itineraryRepository = itineraries,
                    routeLegRepository = routes,
                    mapPreferences = com.yangchengwei.easytrip.workspace.InMemoryMapPreferences(),
                    locationPermissionRequestStore = InMemoryLocationPermissionRequestStore(),
                    routeCoordinator = object : com.yangchengwei.easytrip.route.domain.RouteRefreshCoordinator {
                        override fun start(scope: CoroutineScope) = Unit
                        override suspend fun retry(legId: String) = false
                        override suspend fun updateDetails(
                            legId: String,
                            selectedModeOverride: com.yangchengwei.easytrip.core.model.TransportMode?,
                            durationOverrideSeconds: Int?,
                            note: String?,
                        ) = routes.updateDetails(legId, selectedModeOverride, durationOverrideSeconds, note, online = false)
                    },
                    consentStore = acceptedConsentStore(),
                    runtimeSessionFactory = { fact ->
                        com.yangchengwei.easytrip.AmapRuntimeSession(
                            fact.generation,
                            fact.token,
                            object : PlaceSearchDataSource {
                                override suspend fun search(keyword: String, city: String?) = emptyList<PlaceCandidate>()
                            },
                            object : com.yangchengwei.easytrip.route.domain.RouteRefreshCoordinator {
                                override fun start(scope: CoroutineScope) = Unit
                                override suspend fun retry(legId: String) = false
                                override suspend fun updateDetails(
                                    legId: String,
                                    selectedModeOverride: com.yangchengwei.easytrip.core.model.TransportMode?,
                                    durationOverrideSeconds: Int?,
                                    note: String?,
                                ) = routes.updateDetails(legId, selectedModeOverride, durationOverrideSeconds, note, online = false)
                            },
                        )
                    },
                ),
                navigationObserver = AppNavigationObserver(navigationRoutes::add),
                mapHostFactory = { RecordingHost(it).also(hosts::add) },
            )
        }
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("continue-trip-${runBlocking { trips.observeTrips().first().single().id }}")
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun acceptedConsentStore() = com.yangchengwei.easytrip.amap.AmapConsentStore(
        persistence = object : com.yangchengwei.easytrip.amap.AmapConsentPersistence {
            override fun readDecision(): Boolean? = true
            override fun writeDecision(accepted: Boolean) = Unit
        },
        reporter = object : com.yangchengwei.easytrip.amap.AmapPrivacyReporter {
            override suspend fun reportShown() = Unit
            override suspend fun reportDecision(accepted: Boolean) = Unit
        },
        registry = com.yangchengwei.easytrip.amap.ConsentRegistry(),
    ).also { store ->
        runBlocking {
            store.reportShown().getOrThrow()
            store.decide(true).getOrThrow()
        }
    }

    private fun waitForTag(tag: String) {
        compose.waitUntil(5_000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun waitFor(stage: String, condition: () -> Boolean) {
        try {
            compose.waitUntil(5_000, condition)
        } catch (failure: Throwable) {
            throw AssertionError("$stage failed", failure)
        }
    }

    private fun candidate(id: String, name: String, latitude: Double, longitude: Double) =
        PlaceCandidate(id, name, "地址", GeoPoint(latitude, longitude), "010")

    private class FailOnceUpdateDetailsRepository(
        private val delegate: ItineraryRepository,
    ) : ItineraryRepository {
        var updateDetailsCalls: Int = 0
            private set

        override fun observeDay(dayId: String) = delegate.observeDay(dayId)
        override suspend fun addItem(dayId: String, savedPlaceId: String, targetIndex: Int) =
            delegate.addItem(dayId, savedPlaceId, targetIndex)
        override suspend fun addItemIdempotently(
            dayId: String,
            savedPlaceId: String,
            targetIndex: Int,
            idempotencyKey: String,
        ): AddItineraryItemResult = delegate.addItemIdempotently(dayId, savedPlaceId, targetIndex, idempotencyKey)
        override suspend fun moveItem(itemId: String, targetDayId: String, targetIndex: Int) =
            delegate.moveItem(itemId, targetDayId, targetIndex)
        override suspend fun deleteItem(itemId: String) = delegate.deleteItem(itemId)
        override suspend fun updateTiming(itemId: String, arrivalTime: java.time.LocalTime?, stayMinutes: Int?) =
            delegate.updateTiming(itemId, arrivalTime, stayMinutes)
        override suspend fun updateDetails(itemId: String, arrivalTime: java.time.LocalTime?, stayMinutes: Int?, note: String?) {
            updateDetailsCalls++
            if (updateDetailsCalls == 1) throw IllegalStateException("模拟一次性保存失败")
            delegate.updateDetails(itemId, arrivalTime, stayMinutes, note)
        }
        override suspend fun removePlaceOccurrences(placeId: String) = delegate.removePlaceOccurrences(placeId)
    }

    private class RecordingHost(context: Context) : AmapMapHost {
        override val view = View(context)
        override fun canRenderBeforeReady() = true
        private var poiCallback: (MapPoiUi) -> Unit = {}
        var lastModel: MapUiModel? = null
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
            lastModel = model
            poiCallback = onMapPoiClick
        }
        fun emit(poi: MapPoiUi) = poiCallback(poi)
    }
}
