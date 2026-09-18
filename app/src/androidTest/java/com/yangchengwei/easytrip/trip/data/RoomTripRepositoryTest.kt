package com.yangchengwei.easytrip.trip.data

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.yangchengwei.easytrip.AppNavigationObserver
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.core.model.RouteStatus
import com.yangchengwei.easytrip.core.model.TransportMode
import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.itinerary.data.ItineraryItemEntity
import com.yangchengwei.easytrip.place.data.SavedPlaceEntity
import com.yangchengwei.easytrip.route.data.RouteLegEntity
import com.yangchengwei.easytrip.trip.domain.CreateTrip
import com.yangchengwei.easytrip.trip.domain.InsertSide
import com.yangchengwei.easytrip.trip.domain.TripDay
import com.yangchengwei.easytrip.trip.domain.TripService
import com.yangchengwei.easytrip.trip.domain.TripSummary
import com.yangchengwei.easytrip.trip.ui.CreateTripAction
import com.yangchengwei.easytrip.trip.ui.CreateTripEffect
import com.yangchengwei.easytrip.trip.ui.CreateTripViewModel
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomTripRepositoryTest {
    private lateinit var database: EasyTripDatabase
    private lateinit var repository: RoomTripRepository
    private lateinit var clock: MutableClock

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            EasyTripDatabase::class.java,
        ).build()
        clock = MutableClock(Instant.parse("2026-08-22T00:00:00Z"))
        repository = RoomTripRepository(
            database.tripDao(),
            clock,
            idFactory = IdFactory(),
        )
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun datedCreatePersistsTripAndDaysAtomically() = runTest {
        val date = LocalDate.parse("2026-10-01")
        val tripId = repository.createTrip(CreateTrip("Dated", 3, TravelMode.FLEXIBLE, date))

        val created = repository.observeTrip(tripId).first()!!
        assertEquals(date, created.startDate)
        assertEquals(3, created.days.size)
        assertEquals(com.yangchengwei.easytrip.core.model.TimeMode.DATED, database.tripDao().trip(tripId)!!.timeMode)
    }

    @Test
    fun createRejectsMoreThanThirtyDaysWithoutWriting() = runTest {
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { repository.createTrip(CreateTrip("Too long", 31)) }
        }

        assertEquals(emptyList<TripSummary>(), repository.observeTrips().first())
    }

    @Test
    fun datedCreateAcceptsRepresentableEndAndRejectsOverflowWithoutWriting() = runTest {
        val maximumTripId = repository.createTrip(CreateTrip("Maximum", 1, startDate = LocalDate.MAX))
        val beforeTrips = repository.observeTrips().first()

        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking {
                repository.createTrip(CreateTrip("Overflow", 2, startDate = LocalDate.MAX))
            }
        }

        assertEquals(LocalDate.MAX, repository.observeTrip(maximumTripId).first()!!.startDate)
        assertEquals(beforeTrips, repository.observeTrips().first())
    }

    @Test
    fun applyDateRangeRejectsUnrepresentableEndBeforeWriting() = runTest {
        repository = RoomTripRepository(database.tripDao(), clock, IdFactory(), database)
        val tripId = repository.createTrip(CreateTrip("Maximum", 1, startDate = LocalDate.MAX))
        val beforeTrip = repository.observeTrip(tripId).first()!!
        val beforePositions = database.tripDao().dayPositions(tripId)
        val snapshot = com.yangchengwei.easytrip.trip.domain.DateRangeApply(
            tripId = tripId,
            expectedStartDate = LocalDate.MAX,
            startDate = LocalDate.MAX,
            dayCount = 1,
            expectedDayIds = beforeTrip.days.map { it.id },
            expectedDeletedDayIds = emptyList(),
            expectedDeletedItineraryItems = 0,
            expectedDeletedRouteLegs = 0,
        )

        repository.applyDateRange(snapshot)
        assertEquals(beforeTrip, repository.observeTrip(tripId).first())
        assertEquals(beforePositions, database.tripDao().dayPositions(tripId))

        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { repository.applyDateRange(snapshot.copy(dayCount = 2)) }
        }

        assertEquals(beforeTrip, repository.observeTrip(tripId).first())
        assertEquals(beforePositions, database.tripDao().dayPositions(tripId))
    }

    @Test
    fun datedInsertRejectsUnrepresentableEndWithoutWriting() = runTest {
        val tripId = repository.createTrip(CreateTrip("Maximum", 1, startDate = LocalDate.MAX))
        val beforeTrip = repository.observeTrip(tripId).first()!!
        val beforePositions = database.tripDao().dayPositions(tripId)

        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { repository.insertDay(tripId, null, InsertSide.AFTER) }
        }

        assertEquals(beforeTrip, repository.observeTrip(tripId).first())
        assertEquals(beforePositions, database.tripDao().dayPositions(tripId))
    }

    @Test
    fun setStartDateAcceptsMaximumForOneDayAndRejectsItForMultipleDaysWithoutWriting() = runTest {
        val singleDayId = repository.createTrip(CreateTrip("Single", 1))
        repository.setStartDate(singleDayId, LocalDate.MAX)
        assertEquals(LocalDate.MAX, repository.observeTrip(singleDayId).first()!!.startDate)

        val multipleDayId = repository.createTrip(CreateTrip("Multiple", 2))
        val beforeTrip = repository.observeTrip(multipleDayId).first()!!
        val beforePositions = database.tripDao().dayPositions(multipleDayId)

        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { repository.setStartDate(multipleDayId, LocalDate.MAX) }
        }

        assertEquals(beforeTrip, repository.observeTrip(multipleDayId).first())
        assertEquals(beforePositions, database.tripDao().dayPositions(multipleDayId))
    }

    @Test
    fun createConstraintFailureRollsBackTripAndDays() = runTest {
        repository = RoomTripRepository(database.tripDao(), clock, idFactory = { "duplicate" })

        assertThrows(SQLiteConstraintException::class.java) {
            kotlinx.coroutines.runBlocking { repository.createTrip(CreateTrip("Rollback", 2)) }
        }

        assertEquals(emptyList<TripSummary>(), repository.observeTrips().first())
        assertEquals(emptyList<TripDayEntity>(), database.tripDao().days("duplicate"))
    }

    @Test
    fun replayingRequestIdDoesNotGenerateUnusedDayIds() = runTest {
        var generatedIds = 0
        repository = RoomTripRepository(database.tripDao(), clock, idFactory = { "generated-${++generatedIds}" })
        val command = CreateTrip("Replay", 2, requestId = "request-1")

        repository.createTrip(command)
        assertEquals(2, generatedIds)
        repository.createTrip(command)
        assertEquals(2, generatedIds)
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { repository.createTrip(command.copy(name = "Conflict")) }
        }
        assertEquals(2, generatedIds)
    }

    @Test
    fun createWithoutRequestIdGeneratesTripAndDayIds() = runTest {
        var generatedIds = 0
        repository = RoomTripRepository(database.tripDao(), clock, idFactory = { "generated-${++generatedIds}" })

        repository.createTrip(CreateTrip("Fresh", 3))

        assertEquals(4, generatedIds)
    }

    @Test
    fun requestIdReplayStillCreatesOneTripAndOneNavigationTarget() = runTest {
        val date = LocalDate.parse("2026-10-01")
        val command = CreateTrip(
            name = "Replay",
            dayCount = 2,
            travelMode = TravelMode.FLEXIBLE,
            startDate = date,
            requestId = "request-1",
        )
        val tripId = repository.createTrip(command)
        val savedState = SavedStateHandle(
            mapOf(
                "trip.create.name" to command.name,
                "trip.create.startDate" to date.toString(),
                "trip.create.endDate" to date.plusDays(1).toString(),
                "trip.create.travelMode" to command.travelMode.name,
                "trip.create.requestId" to command.requestId,
            ),
        )
        val navigationTargets = mutableListOf<String>()
        val observer = AppNavigationObserver(navigationTargets::add)
        val replayViewModel = CreateTripViewModel(TripService(repository), savedState) { "unexpected-request" }
        val effectJob = launch {
            when (val effect = replayViewModel.effects.first()) {
                is CreateTripEffect.OpenWorkspace -> observer.onNavigate("trips/${effect.tripId}")
                CreateTripEffect.NavigateBack -> error("Unexpected back navigation")
            }
        }

        replayViewModel.onAction(CreateTripAction.Submit)
        effectJob.join()

        assertEquals(listOf("trips/$tripId"), navigationTargets)
        assertEquals(1, repository.observeTrips().first().size)
        assertEquals(2, repository.observeTrip(tripId).first()!!.days.size)
    }

    @Test
    fun createObserveAndUpdateTrip() = runTest {
        val tripId = repository.createTrip(CreateTrip("Kyoto", 3, TravelMode.FLEXIBLE))

        val created = repository.observeTrip(tripId).first()!!
        assertEquals("Kyoto", created.name)
        assertEquals(listOf(0, 1, 2), created.days.map { it.index })
        assertEquals(1, repository.observeTrips().first().size)

        repository.renameTrip(tripId, "Tokyo")
        repository.setStartDate(tripId, LocalDate.parse("2026-10-01"))
        repository.setTravelMode(tripId, TravelMode.SELF_DRIVE)

        val updated = repository.observeTrip(tripId).first()!!
        assertEquals("Tokyo", updated.name)
        assertEquals(LocalDate.parse("2026-10-01"), updated.startDate)
        assertEquals(TravelMode.SELF_DRIVE, updated.travelMode)
    }

    @Test
    fun switchingFromSelfDriveToFlexibleRecommendsByDistanceAndClearsCache() = runTest {
        repository = RoomTripRepository(database.tripDao(), clock, IdFactory(), database) { true }
        val tripId = repository.createTrip(CreateTrip("Modes", 1, TravelMode.SELF_DRIVE))
        val dayId = repository.observeTrip(tripId).first()!!.days.single().id
        database.savedPlaceDao().insertPlace(SavedPlaceEntity("near-a", tripId, "a", "A", "", 0.0, 0.0))
        database.savedPlaceDao().insertPlace(SavedPlaceEntity("near-b", tripId, "b", "B", "", 0.0, 0.001))
        database.itineraryEditingDao().insertItem(ItineraryItemEntity("item-a", dayId, tripId, "near-a", 0))
        database.itineraryEditingDao().insertItem(ItineraryItemEntity("item-b", dayId, tripId, "near-b", 1000))
        database.routeLegDao().insert(RouteLegEntity("leg", dayId, "item-a", "item-b", TransportMode.DRIVE, status = RouteStatus.SUCCESS, distanceMeters = 100, durationSeconds = 60, polyline = "v1|0.0,0.0;0.0,0.001", version = 1, updatedAt = clock.instant()))

        repository.setTravelMode(tripId, TravelMode.FLEXIBLE)

        val leg = database.routeLegDao().legs(dayId).single()
        assertEquals(TransportMode.WALK, leg.recommendedMode)
        assertEquals(RouteStatus.PENDING, leg.status)
        assertEquals(2L, leg.version)
        assertEquals(null, leg.polyline)
    }

    @Test
    fun insertMoveAndDeleteReorderToCanonicalPositionsWithoutReplacingDays() = runTest {
        val tripId = repository.createTrip(CreateTrip("Kyoto", 3))
        val originalIds = repository.observeTrip(tripId).first()!!.days.map { it.id }

        val insertedId = repository.insertDay(tripId, originalIds[1], InsertSide.BEFORE)
        assertEquals(
            listOf(originalIds[0], insertedId, originalIds[1], originalIds[2]),
            repository.observeTrip(tripId).first()!!.days.map { it.id },
        )
        assertEquals(listOf(0L, 1_000L, 2_000L, 3_000L), database.tripDao().dayPositions(tripId))

        repository.moveDay(tripId, originalIds[0], 3)
        assertEquals(
            listOf(insertedId, originalIds[1], originalIds[2], originalIds[0]),
            repository.observeTrip(tripId).first()!!.days.map { it.id },
        )

        repository.deleteDay(com.yangchengwei.easytrip.trip.domain.DayDeletion(originalIds[1], 0, 0))
        val remaining = repository.observeTrip(tripId).first()!!.days.map { it.id }
        assertEquals(listOf(insertedId, originalIds[2], originalIds[0]), remaining)
        assertNotEquals(originalIds[1], remaining[1])
        assertEquals(listOf(0L, 1_000L, 2_000L), database.tripDao().dayPositions(tripId))
    }

    @Test
    fun deleteTripCascadesDays() = runTest {
        val tripId = repository.createTrip(CreateTrip("Kyoto", 2))

        repository.deleteTrip(tripId)

        assertEquals(null, repository.observeTrip(tripId).first())
        assertEquals(emptyList<Long>(), database.tripDao().dayPositions(tripId))
    }


    @Test
    fun insertSupportsBeforeAfterAppendAndRejectsInvalidAnchorsWithoutWriting() = runTest {
        val firstTrip = repository.createTrip(CreateTrip("First", 2))
        val secondTrip = repository.createTrip(CreateTrip("Second", 1))
        val original = repository.observeTrip(firstTrip).first()!!.days.map { it.id }
        val otherDay = repository.observeTrip(secondTrip).first()!!.days.single().id

        val before = repository.insertDay(firstTrip, original[0], InsertSide.BEFORE)
        val after = repository.insertDay(firstTrip, original[1], InsertSide.AFTER)
        val appended = repository.insertDay(firstTrip, null, InsertSide.BEFORE)
        assertEquals(listOf(before, original[0], original[1], after, appended), repository.observeTrip(firstTrip).first()!!.days.map { it.id })

        val snapshot = database.tripDao().dayPositions(firstTrip)
        assertThrows(IllegalArgumentException::class.java) { kotlinx.coroutines.runBlocking { repository.insertDay(firstTrip, "missing", InsertSide.BEFORE) } }
        assertThrows(IllegalArgumentException::class.java) { kotlinx.coroutines.runBlocking { repository.insertDay(firstTrip, otherDay, InsertSide.BEFORE) } }
        assertThrows(IllegalArgumentException::class.java) { kotlinx.coroutines.runBlocking { repository.insertDay("missing", null, InsertSide.BEFORE) } }
        assertEquals(snapshot, database.tripDao().dayPositions(firstTrip))
    }

    @Test
    fun mutationsRejectMissingOrCrossTripIdsAndInvalidMoveIndices() = runTest {
        val first = repository.createTrip(CreateTrip("First", 2))
        val second = repository.createTrip(CreateTrip("Second", 1))
        val foreignDay = repository.observeTrip(second).first()!!.days.single().id
        val positions = database.tripDao().dayPositions(first)

        assertThrows(IllegalArgumentException::class.java) { kotlinx.coroutines.runBlocking { repository.moveDay(first, foreignDay, 0) } }
        assertThrows(IllegalArgumentException::class.java) { kotlinx.coroutines.runBlocking { repository.moveDay(first, "missing", 0) } }
        assertThrows(IllegalArgumentException::class.java) { kotlinx.coroutines.runBlocking { repository.moveDay(first, repository.observeTrip(first).first()!!.days[0].id, -1) } }
        assertThrows(IllegalArgumentException::class.java) { kotlinx.coroutines.runBlocking { repository.moveDay(first, repository.observeTrip(first).first()!!.days[0].id, 2) } }
        assertThrows(IllegalArgumentException::class.java) { kotlinx.coroutines.runBlocking { repository.deleteDay(com.yangchengwei.easytrip.trip.domain.DayDeletion("missing", 0, 0)) } }
        assertThrows(IllegalArgumentException::class.java) { kotlinx.coroutines.runBlocking { repository.renameTrip("missing", "X") } }
        assertThrows(IllegalArgumentException::class.java) { kotlinx.coroutines.runBlocking { repository.setStartDate("missing", null) } }
        assertThrows(IllegalArgumentException::class.java) { kotlinx.coroutines.runBlocking { repository.setTravelMode("missing", TravelMode.FLEXIBLE) } }
        assertThrows(IllegalArgumentException::class.java) { kotlinx.coroutines.runBlocking { repository.deleteTrip("missing") } }
        assertEquals(positions, database.tripDao().dayPositions(first))
    }

    @Test
    fun maximumTripInsertionUsesDisjointParkingRange() = runTest {
        val tripId = repository.createTrip(CreateTrip("Large", 29))
        val anchor = repository.observeTrip(tripId).first()!!.days[15].id

        repository.insertDay(tripId, anchor, InsertSide.BEFORE)

        assertEquals(30, repository.observeTrip(tripId).first()!!.days.size)
        assertEquals(List(30) { it * 1_000L }, database.tripDao().dayPositions(tripId))
    }

    @Test
    fun insertingThirtyFirstDayIsRejectedWithoutChangingTrip() = runTest {
        val tripId = repository.createTrip(CreateTrip("Maximum", 30))
        val beforeTrip = repository.observeTrip(tripId).first()!!
        val beforePositions = database.tripDao().dayPositions(tripId)

        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking {
                repository.insertDay(tripId, null, InsertSide.AFTER)
            }
        }

        assertEquals(beforeTrip, repository.observeTrip(tripId).first())
        assertEquals(beforePositions, database.tripDao().dayPositions(tripId))
    }

    @Test
    fun existingFlowSubscriptionsEmitMutationsAndListDayCount() = runTest {
        val tripId = repository.createTrip(CreateTrip("Trip", 2))
        coroutineScope {
            val tripEvents = repository.observeTrip(tripId).produceIn(this)
            val listEvents = repository.observeTrips().produceIn(this)
            assertEquals("Trip", tripEvents.receiveUntil("initial trip") { it?.name == "Trip" }!!.name)
            assertEquals(2, listEvents.receive().single().dayCount)

            repository.renameTrip(tripId, "Renamed")
            assertEquals("Renamed", tripEvents.receiveUntil("renamed trip") { it?.name == "Renamed" }!!.name)
            clock.advance()
            repository.setStartDate(tripId, LocalDate.parse("2026-10-01"))
            assertEquals(
                LocalDate.parse("2026-10-01"),
                tripEvents.receiveUntil("dated trip") { it?.startDate == LocalDate.parse("2026-10-01") }!!.startDate,
            )
            val dayId = repository.insertDay(tripId, null, InsertSide.AFTER)
            assertEquals(3, tripEvents.receiveUntil("inserted day") { it?.days?.size == 3 }!!.days.size)
            assertEquals(3, listEvents.receiveUntil("trip list count 3") { it.single().dayCount == 3 }.single().dayCount)
            repository.moveDay(tripId, dayId, 0)
            assertEquals(dayId, tripEvents.receiveUntil("moved day") { it?.days?.firstOrNull()?.id == dayId }!!.days.first().id)
            repository.deleteDay(com.yangchengwei.easytrip.trip.domain.DayDeletion(dayId, 0, 0))
            assertEquals(2, tripEvents.receiveUntil("deleted day") { it?.days?.size == 2 && it.days.none { day -> day.id == dayId } }!!.days.size)
            assertEquals(2, listEvents.receiveUntil("trip list count 2") { it.single().dayCount == 2 }.single().dayCount)
            tripEvents.cancel(); listEvents.cancel()
        }
    }

    @Test
    fun observeTripsProjectsSavedPlacesAndDistinctScheduledDaysAndReemitsOnChanges() = runTest {
        val tripId = repository.createTrip(CreateTrip("Stats", 3))
        val dayIds = repository.observeTrip(tripId).first()!!.days.map(TripDay::id)
        database.savedPlaceDao().insertPlace(SavedPlaceEntity("place-a", tripId, "a", "A", "", 0.0, 0.0))
        database.savedPlaceDao().insertPlace(SavedPlaceEntity("place-b", tripId, "b", "B", "", 0.0, 0.0))
        database.savedPlaceDao().insertPlace(SavedPlaceEntity("place-c", tripId, "c", "C", "", 0.0, 0.0))
        database.itineraryEditingDao().insertItem(ItineraryItemEntity("item-a-0", dayIds[0], tripId, "place-a", 0))
        database.itineraryEditingDao().insertItem(ItineraryItemEntity("item-a-1", dayIds[1], tripId, "place-a", 0))
        database.itineraryEditingDao().insertItem(ItineraryItemEntity("item-b-2", dayIds[2], tripId, "place-b", 0))

        coroutineScope {
            val listEvents = repository.observeTrips().produceIn(this)
            val initial = listEvents.receiveUntil("initial list statistics") { it.singleOrNull()?.placeCount == 3 }
                .single()
            assertEquals(3, initial.dayCount)
            assertEquals(3, initial.placeCount)
            assertEquals(3, initial.scheduledDayCount)

            database.itineraryEditingDao().deleteRow("item-a-0")
            assertEquals(
                2,
                listEvents.receiveUntil("two scheduled days after first day becomes empty") {
                    it.singleOrNull()?.scheduledDayCount == 2
                }.single().scheduledDayCount,
            )

            database.itineraryEditingDao().deleteRow("item-a-1")
            assertEquals(
                1,
                listEvents.receiveUntil("one scheduled day after second day becomes empty") {
                    it.singleOrNull()?.scheduledDayCount == 1
                }.single().scheduledDayCount,
            )

            database.savedPlaceDao().insertPlace(SavedPlaceEntity("place-d", tripId, "d", "D", "", 0.0, 0.0))
            assertEquals(
                4,
                listEvents.receiveUntil("place count after saving D") { it.singleOrNull()?.placeCount == 4 }
                    .single().placeCount,
            )

            database.savedPlaceDao().deletePlace("place-d")
            assertEquals(
                3,
                listEvents.receiveUntil("place count after deleting D") { it.singleOrNull()?.placeCount == 3 }
                    .single().placeCount,
            )
            listEvents.cancel()
        }
    }

    @Test
    fun roomFlowReflectsDeleteWithoutManualUiMutation() = runTest {
        val tripId = repository.createTrip(CreateTrip("Flow", 2))
        coroutineScope {
            val listEvents = repository.observeTrips().produceIn(this)
            assertEquals(listOf(tripId), listEvents.receiveUntil("created trip") { it.size == 1 }.map { it.id })

            repository.deleteTrip(tripId)

            assertEquals(emptyList<TripSummary>(), listEvents.receiveUntil("empty list after delete") { it.isEmpty() })
            listEvents.cancel()
        }
    }

    @Test
    fun deletingLastDayIsRejectedAndTravelModeUpdatesWithoutChangingDateOrdering() = runTest {
        val first = repository.createTrip(CreateTrip("First", 1, startDate = LocalDate.of(2026, 8, 24)))
        clock.advance()
        val second = repository.createTrip(CreateTrip("Second", 1, startDate = LocalDate.of(2026, 8, 25)))
        assertEquals(listOf(first, second), repository.observeTrips().first().map { it.id })

        clock.advance()
        repository.setTravelMode(second, TravelMode.SELF_DRIVE)
        assertEquals(listOf(first, second), repository.observeTrips().first().map { it.id })
        assertEquals(TravelMode.SELF_DRIVE, repository.observeTrip(second).first()!!.travelMode)
        val onlyDay = repository.observeTrip(first).first()!!.days.single().id
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking {
                repository.deleteDay(com.yangchengwei.easytrip.trip.domain.DayDeletion(onlyDay, 0, 0))
            }
        }
        assertEquals(listOf(onlyDay), repository.observeTrip(first).first()!!.days.map { it.id })
        repository.deleteTrip(first)
        assertEquals(null, repository.observeTrip(first).first())
    }

    @Test
    fun observeTripsOrdersByDatesOnEveryRoomEmission() = runTest {
        val endedOlder = repository.createTrip(CreateTrip("Ended older", 2, startDate = LocalDate.of(2026, 8, 10)))
        clock.advance()
        val undated = repository.createTrip(CreateTrip("Undated", 1))
        clock.advance()
        val futureLater = repository.createTrip(CreateTrip("Future later", 1, startDate = LocalDate.of(2026, 9, 1)))
        clock.advance()
        val ongoingEarlierUpdate = repository.createTrip(CreateTrip("Ongoing earlier update", 4, startDate = LocalDate.of(2026, 8, 20)))
        clock.advance()
        val ongoingLaterUpdate = repository.createTrip(CreateTrip("Ongoing later update", 3, startDate = LocalDate.of(2026, 8, 21)))
        clock.advance()
        val endedRecent = repository.createTrip(CreateTrip("Ended recent", 2, startDate = LocalDate.of(2026, 8, 18)))
        clock.advance()
        val futureSooner = repository.createTrip(CreateTrip("Future sooner", 1, startDate = LocalDate.of(2026, 8, 25)))

        val expected = listOf(ongoingLaterUpdate, ongoingEarlierUpdate, futureSooner, futureLater, endedRecent, endedOlder, undated)
        assertEquals(expected, repository.observeTrips().first().map { it.id })

        clock.advance()
        repository.renameTrip(ongoingEarlierUpdate, "Edited ongoing earlier update")
        assertEquals(expected, repository.observeTrips().first().map { it.id })

        clock.advance()
        repository.setStartDate(ongoingLaterUpdate, LocalDate.of(2026, 9, 10))
        assertEquals(
            listOf(ongoingEarlierUpdate, futureSooner, futureLater, ongoingLaterUpdate, endedRecent, endedOlder, undated),
            repository.observeTrips().first().map { it.id },
        )
    }

    @Test
    fun observeTripsCountsDistinctScheduledDaysAndReemitsOnItineraryChanges() = runTest {
        val tripId = repository.createTrip(CreateTrip("Readiness", 3))
        val dayIds = repository.observeTrip(tripId).first()!!.days.map(TripDay::id)
        database.savedPlaceDao().insertPlace(SavedPlaceEntity("place", tripId, "p", "Place", "", 0.0, 0.0))
        database.itineraryEditingDao().insertItem(ItineraryItemEntity("item-0-a", dayIds[0], tripId, "place", 0))
        database.itineraryEditingDao().insertItem(ItineraryItemEntity("item-0-b", dayIds[0], tripId, "place", 1_000))

        coroutineScope {
            val events = repository.observeTrips().produceIn(this)
            assertEquals(1, events.receiveUntil("one scheduled day") { it.singleOrNull()?.scheduledDayCount == 1 }.single().scheduledDayCount)

            database.itineraryEditingDao().insertItem(ItineraryItemEntity("item-2", dayIds[2], tripId, "place", 0))
            assertEquals(2, events.receiveUntil("two scheduled days") { it.singleOrNull()?.scheduledDayCount == 2 }.single().scheduledDayCount)
            events.cancel()
        }
    }


    @Test
    fun insertConstraintFailureAfterParkingRollsBackRowsAndTimestamp() = runTest {
        val tripId = repository.createTrip(CreateTrip("Rollback", 3))
        val beforeDays = database.tripDao().days(tripId)
        val beforeUpdatedAt = database.tripDao().trip(tripId)!!.updatedAt
        clock.advance()

        assertThrows(SQLiteConstraintException::class.java) {
            kotlinx.coroutines.runBlocking {
                database.tripDao().insertAndReorderDay(
                    tripId = tripId,
                    anchorDayId = beforeDays[1].id,
                    after = false,
                    newId = beforeDays[0].id,
                    now = clock.instant(),
                    maxDays = com.yangchengwei.easytrip.trip.domain.MAX_TRIP_DAYS,
                )
            }
        }

        val afterDays = database.tripDao().days(tripId)
        assertEquals(beforeDays.map { it.id }, afterDays.map { it.id })
        assertEquals(beforeDays.map { it.position }, afterDays.map { it.position })
        assertEquals(beforeUpdatedAt, database.tripDao().trip(tripId)!!.updatedAt)
        assertEquals(emptyList<Long>(), afterDays.map { it.position }.filter { it < 0 })
    }

    @Test
    fun concurrentInsertsRemainContinuousAndUnique() = runTest {
        val tripId = repository.createTrip(CreateTrip("Concurrent", 2))

        coroutineScope {
            List(8) { async { repository.insertDay(tripId, null, InsertSide.AFTER) } }.awaitAll()
        }

        assertEquals(List(10) { it * 1_000L }, database.tripDao().dayPositions(tripId))
        assertEquals(10, repository.observeTrip(tripId).first()!!.days.map { it.id }.distinct().size)
    }

    private suspend fun <T> ReceiveChannel<T>.receiveUntil(
        description: String,
        maxEmissions: Int = 20,
        predicate: (T) -> Boolean,
    ): T = withContext(Dispatchers.Default.limitedParallelism(1)) {
        withTimeout(5_000) {
            repeat(maxEmissions) {
                val value = receive()
                if (predicate(value)) return@withTimeout value
            }
            throw AssertionError("Did not receive $description within $maxEmissions emissions")
        }
    }

    private class MutableClock(private var value: Instant) : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = this
        override fun instant(): Instant = value
        fun advance() { value = value.plusSeconds(1) }
    }

    @Test fun appendKeepsExistingDayPrefixAndAddsReturnedDayAtEnd() = runTest {
        repository = RoomTripRepository(database.tripDao(), clock, IdFactory(), database)
        val tripId = repository.createTrip(CreateTrip("Trip", 2))
        val before = repository.observeTrip(tripId).first()!!.days.map(TripDay::id)

        val returnedDayId = repository.insertDay(tripId, null, InsertSide.AFTER)
        val after = repository.observeTrip(tripId).first()!!.days.map(TripDay::id)

        assertEquals(before, after.dropLast(1))
        assertEquals(returnedDayId, after.last())
    }

    private class IdFactory : () -> String {
        private var next = 0
        override fun invoke() = "generated-${next++}"
    }
}
