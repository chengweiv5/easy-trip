package com.yangchengwei.easytrip.trip.data

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
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
import com.yangchengwei.easytrip.trip.domain.TripSummary
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
    fun replayingRequestIdIsIdempotent_andConflictingCommandFails() = runTest {
        val command = CreateTrip(
            name = "Replay",
            dayCount = 2,
            travelMode = TravelMode.FLEXIBLE,
            startDate = LocalDate.parse("2026-10-01"),
            requestId = "request-1",
        )
        val first = repository.createTrip(command)
        val replay = repository.createTrip(command)

        assertEquals(first, replay)
        assertEquals(1, repository.observeTrips().first().size)
        assertEquals(2, repository.observeTrip(first).first()!!.days.size)
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { repository.createTrip(command.copy(name = "Conflict")) }
        }
        assertEquals(1, repository.observeTrips().first().size)
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

        repository.deleteDay(originalIds[1])
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
        assertThrows(IllegalArgumentException::class.java) { kotlinx.coroutines.runBlocking { repository.deleteDay("missing") } }
        assertThrows(IllegalArgumentException::class.java) { kotlinx.coroutines.runBlocking { repository.renameTrip("missing", "X") } }
        assertThrows(IllegalArgumentException::class.java) { kotlinx.coroutines.runBlocking { repository.setStartDate("missing", null) } }
        assertThrows(IllegalArgumentException::class.java) { kotlinx.coroutines.runBlocking { repository.setTravelMode("missing", TravelMode.FLEXIBLE) } }
        assertThrows(IllegalArgumentException::class.java) { kotlinx.coroutines.runBlocking { repository.deleteTrip("missing") } }
        assertEquals(positions, database.tripDao().dayPositions(first))
    }

    @Test
    fun largeTripInsertionUsesDisjointParkingRange() = runTest {
        val tripId = repository.createTrip(CreateTrip("Large", 1_000))
        val anchor = repository.observeTrip(tripId).first()!!.days[500].id

        repository.insertDay(tripId, anchor, InsertSide.BEFORE)

        assertEquals(1_001, repository.observeTrip(tripId).first()!!.days.size)
        assertEquals(List(1_001) { it * 1_000L }, database.tripDao().dayPositions(tripId))
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
            repository.deleteDay(dayId)
            assertEquals(2, tripEvents.receiveUntil("deleted day") { it?.days?.size == 2 && it.days.none { day -> day.id == dayId } }!!.days.size)
            assertEquals(2, listEvents.receiveUntil("trip list count 2") { it.single().dayCount == 2 }.single().dayCount)
            tripEvents.cancel(); listEvents.cancel()
        }
    }

    @Test
    fun deletingLastDayIsAllowedAndTravelModeAndRecentOrderingUpdate() = runTest {
        val first = repository.createTrip(CreateTrip("First", 1))
        clock.advance()
        val second = repository.createTrip(CreateTrip("Second", 1))
        assertEquals(listOf(second, first), repository.observeTrips().first().map { it.id })

        clock.advance()
        repository.setTravelMode(first, TravelMode.SELF_DRIVE)
        assertEquals(first, repository.observeTrips().first().first().id)
        assertEquals(TravelMode.SELF_DRIVE, repository.observeTrip(first).first()!!.travelMode)
        repository.deleteDay(repository.observeTrip(first).first()!!.days.single().id)
        assertEquals(emptyList<String>(), repository.observeTrip(first).first()!!.days.map { it.id })
        repository.deleteTrip(first)
        assertEquals(null, repository.observeTrip(first).first())
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

    private class IdFactory : () -> String {
        private var next = 0
        override fun invoke() = "generated-${next++}"
    }
}
