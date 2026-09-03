package com.yangchengwei.easytrip.itinerary

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.core.model.TimeMode
import com.yangchengwei.easytrip.core.model.TransportMode
import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.itinerary.data.RoomItineraryRepository
import com.yangchengwei.easytrip.itinerary.domain.AddPlacesOutcome
import com.yangchengwei.easytrip.itinerary.domain.AddPlacesRequest
import com.yangchengwei.easytrip.itinerary.domain.AddPlacesToDayUseCase
import com.yangchengwei.easytrip.itinerary.domain.ItineraryRepository
import com.yangchengwei.easytrip.itinerary.domain.UndoAddedItemsRequest
import com.yangchengwei.easytrip.itinerary.domain.UndoAddedItemsUseCase
import com.yangchengwei.easytrip.place.data.SavedPlaceEntity
import com.yangchengwei.easytrip.trip.data.TripDayEntity
import com.yangchengwei.easytrip.trip.data.TripEntity
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AddPlacesRoomIntegrationTest {
    private lateinit var database: EasyTripDatabase
    private lateinit var repository: RoomItineraryRepository
    private lateinit var addPlaces: AddPlacesToDayUseCase
    private lateinit var undoAddedItems: UndoAddedItemsUseCase
    private val now = Instant.parse("2026-08-24T00:00:00Z")
    private val itemIds = SequenceIds("item")
    private val legIds = SequenceIds("leg")

    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            EasyTripDatabase::class.java,
        ).build()
        repository = newRepository()
        addPlaces = AddPlacesToDayUseCase(repository)
        undoAddedItems = UndoAddedItemsUseCase(repository)
    }

    @After fun tearDown() = database.close()

    @Test fun duplicatePlacesAppendAfterExistingItemsAndCreateAdjacentLegs() = runTest {
        seedTrip("trip", "day")
        listOf("a", "hotel", "museum").forEach { seedPlace(it) }
        val existing = repository.addItem("day", "a", 0)

        val outcome = addPlaces(AddPlacesRequest("trip", "day", listOf("hotel", "hotel", "museum")))
        val success = outcome as AddPlacesOutcome.Success
        val items = database.itineraryEditingDao().items("day")

        assertEquals(listOf(existing) + success.createdItemIds, items.map { it.id })
        assertEquals(listOf("a", "hotel", "hotel", "museum"), items.map { it.savedPlaceId })
        assertEquals(3, success.createdItemIds.distinct().size)
        assertEquals(listOf(0L, 1_000L, 2_000L, 3_000L), items.map { it.position })
        assertEquals(items.zipWithNext { from, to -> from.id to to.id }, database.routeLegDao().legs("day").map { it.fromItemId to it.toItemId })
    }

    @Test fun resumedRequestUsesRoomIdempotencyKeyWithoutDuplicatingCommittedOccurrence() = runTest {
        seedTrip("trip", "day")
        listOf("first", "second").forEach { seedPlace(it) }
        val request = AddPlacesRequest(
            tripId = "trip",
            dayId = "day",
            savedPlaceIds = listOf("first", "second"),
            operationId = "operation",
        )
        val interrupted = InterruptAfterCommittedAddRepository(repository)

        assertThrows(CancellationException::class.java) {
            runBlocking { AddPlacesToDayUseCase(interrupted)(request) }
        }

        val outcome = AddPlacesToDayUseCase(newRepository())(request) as AddPlacesOutcome.Success
        val items = database.itineraryEditingDao().items("day")

        assertEquals(listOf("first", "second"), items.map { it.savedPlaceId })
        assertEquals(2, items.size)
        assertEquals(items.map { it.id }, outcome.createdItemIds)
        assertEquals(outcome.createdItemIds, outcome.createdItemIds.distinct())
    }

    @Test fun undoAfterRestartSkipsAlreadyDeletedItemAndDeletesRemainingRoomRows() = runTest {
        seedTrip("trip", "day")
        listOf("first", "second").forEach { seedPlace(it) }
        val added = addPlaces(
            AddPlacesRequest("trip", "day", listOf("first", "second"), operationId = "operation"),
        ) as AddPlacesOutcome.Success
        repository.deleteItem(added.createdItemIds.first())

        val outcome = UndoAddedItemsUseCase(newRepository())(
            UndoAddedItemsRequest(added.createdItemIds),
        )

        assertEquals(added.createdItemIds, outcome.deletedItemIds)
        assertTrue(outcome.remainingItemIds.isEmpty())
        assertNull(outcome.failure)
        assertTrue(database.itineraryEditingDao().items("day").isEmpty())
    }

    @Test fun recoverableSinglePlaceFailureContinuesWithoutPositionGap() = runTest {
        seedTrip("trip", "day")
        seedPlace("first")
        seedPlace("last")

        val outcome = addPlaces(AddPlacesRequest("trip", "day", listOf("first", "missing", "last")))
        val partial = outcome as AddPlacesOutcome.PartialSuccess
        val items = database.itineraryEditingDao().items("day")

        assertEquals(listOf("missing"), partial.failedPlaceIds)
        assertEquals(partial.createdItemIds, items.map { it.id })
        assertEquals(listOf("first", "last"), items.map { it.savedPlaceId })
        assertEquals(listOf(0L, 1_000L), items.map { it.position })
        assertEquals(listOf(items[0].id to items[1].id), database.routeLegDao().legs("day").map { it.fromItemId to it.toItemId })
    }

    @Test fun unknownRouteLegInsertFailurePropagatesAndRollsBackCurrentPlace() = runTest {
        seedTrip("trip", "day")
        listOf("a", "b", "c").forEach { seedPlace(it) }
        val a = repository.addItem("day", "a", 0)
        val b = repository.addItem("day", "b", 1)
        val existingLeg = database.routeLegDao().legs("day").single()
        val beforeItems = database.itineraryEditingDao().items("day")
        val beforeLegs = database.routeLegDao().legs("day")
        repository = newRepository { existingLeg.id }
        addPlaces = AddPlacesToDayUseCase(repository)

        assertThrows(SQLiteConstraintException::class.java) {
            runBlocking { addPlaces(AddPlacesRequest("trip", "day", listOf("c"))) }
        }

        assertEquals(listOf(a, b), database.itineraryEditingDao().items("day").map { it.id })
        assertEquals(beforeItems, database.itineraryEditingDao().items("day"))
        assertEquals(beforeLegs, database.routeLegDao().legs("day"))
    }

    @Test fun targetDayMissingMapsWithoutMessageParsing() = runTest {
        seedTrip("trip", "other")
        val selected = listOf("first", "first", "second")

        val outcome = addPlaces(AddPlacesRequest("trip", "missing", selected))

        assertEquals(AddPlacesOutcome.TargetDayMissing(selected), outcome)
        assertTrue(database.itineraryEditingDao().items("other").isEmpty())
    }

    @Test fun targetDayDeletedMidBatchRestoresCompleteRequestAndStops() = runTest {
        seedTrip("trip", "target", "other")
        listOf("first", "second", "third").forEach { seedPlace(it) }
        val deletingRepository = DeleteTargetDayAfterFirstAddRepository(repository) {
            database.tripDao().deleteDayRow("target")
        }

        val outcome = AddPlacesToDayUseCase(deletingRepository)(
            AddPlacesRequest("trip", "target", listOf("first", "second", "third")),
        ) as AddPlacesOutcome.TargetDayMissing

        assertEquals(listOf("first", "second", "third"), outcome.retainedPlaceIds)
        assertEquals(listOf("item-0"), outcome.createdItemIds)
        assertTrue(database.itineraryEditingDao().items("target").isEmpty())
        assertTrue(database.routeLegDao().legs("target").isEmpty())
        assertTrue(database.itineraryEditingDao().items("other").isEmpty())
    }

    @Test fun undoDeletesOnlyCreatedOccurrencesAndPreservesSavedPlaceAndHistory() = runTest {
        seedTrip("trip", "day")
        seedPlace("hotel", note = "keep-note")
        seedPlace("museum")
        val historicalHotel = repository.addItem("day", "hotel", 0)
        val historicalMuseum = repository.addItem("day", "museum", 1)
        val outcome = addPlaces(AddPlacesRequest("trip", "day", listOf("hotel", "hotel"))) as AddPlacesOutcome.Success

        undoAddedItems(UndoAddedItemsRequest(outcome.createdItemIds))

        val items = database.itineraryEditingDao().items("day")
        assertEquals(listOf(historicalHotel, historicalMuseum), items.map { it.id })
        assertEquals(listOf("hotel", "museum"), items.map { it.savedPlaceId })
        assertEquals(listOf(0L, 1_000L), items.map { it.position })
        assertEquals("keep-note", database.savedPlaceDao().place("hotel")?.note)
        assertEquals(listOf(historicalHotel to historicalMuseum), database.routeLegDao().legs("day").map { it.fromItemId to it.toItemId })
        outcome.createdItemIds.forEach { assertNull(database.itineraryEditingDao().item(it)) }
    }

    @Test fun batchesOnDifferentDaysNeverCreateCrossDayRouteLegs() = runTest {
        seedTrip("trip", "day-1", "day-2")
        listOf("a", "b", "c", "d").forEach { seedPlace(it) }
        addPlaces(AddPlacesRequest("trip", "day-1", listOf("a", "b")))
        addPlaces(AddPlacesRequest("trip", "day-2", listOf("c", "d")))

        val day1Items = database.itineraryEditingDao().items("day-1")
        val day2Items = database.itineraryEditingDao().items("day-2")
        val day1Ids = day1Items.mapTo(mutableSetOf()) { it.id }
        val day2Ids = day2Items.mapTo(mutableSetOf()) { it.id }
        val day1Legs = database.routeLegDao().legs("day-1")
        val day2Legs = database.routeLegDao().legs("day-2")

        assertEquals(listOf(day1Items[0].id to day1Items[1].id), day1Legs.map { it.fromItemId to it.toItemId })
        assertEquals(listOf(day2Items[0].id to day2Items[1].id), day2Legs.map { it.fromItemId to it.toItemId })
        assertTrue(day1Legs.all { it.fromItemId in day1Ids && it.toItemId in day1Ids })
        assertTrue(day2Legs.all { it.fromItemId in day2Ids && it.toItemId in day2Ids })
        assertTrue(day1Legs.none { it.fromItemId in day2Ids || it.toItemId in day2Ids })
        assertTrue(day2Legs.none { it.fromItemId in day1Ids || it.toItemId in day1Ids })
    }

    private fun newRepository(legIdFactory: () -> String = legIds) = RoomItineraryRepository(
        database,
        database.itineraryEditingDao(),
        database.routeLegDao(),
        Clock.fixed(now, ZoneOffset.UTC),
        itemIds,
        legIdFactory,
        { true },
        { _, _, _ -> TransportMode.WALK },
    )

    private suspend fun seedTrip(id: String, vararg days: String) {
        database.tripDao().insertTrip(TripEntity(id, id, TimeMode.DRAFT, null, TravelMode.FLEXIBLE, now, now))
        days.forEachIndexed { index, dayId ->
            database.tripDao().insertDay(TripDayEntity(dayId, id, index * 1_000L))
        }
    }

    private suspend fun seedPlace(id: String, tripId: String = "trip", note: String? = null) {
        database.savedPlaceDao().insertPlace(SavedPlaceEntity(id, tripId, "poi-$id", id, "address-$id", 0.0, 0.0, note))
    }

    private class SequenceIds(private val prefix: String) : () -> String {
        private val next = AtomicInteger()
        override fun invoke(): String = "$prefix-${next.getAndIncrement()}"
    }

    private class InterruptAfterCommittedAddRepository(
        private val delegate: ItineraryRepository,
    ) : ItineraryRepository by delegate {
        private var interrupted = false

        override suspend fun addItemIdempotently(
            dayId: String,
            savedPlaceId: String,
            targetIndex: Int,
            idempotencyKey: String,
        ): com.yangchengwei.easytrip.itinerary.domain.AddItineraryItemResult {
            val result = delegate.addItemIdempotently(dayId, savedPlaceId, targetIndex, idempotencyKey)
            if (!interrupted) {
                interrupted = true
                throw CancellationException("process stopped after commit")
            }
            return result
        }
    }

    private class DeleteTargetDayAfterFirstAddRepository(
        private val delegate: ItineraryRepository,
        private val deleteDay: suspend () -> Unit,
    ) : ItineraryRepository by delegate {
        private var successfulAdds = 0

        override suspend fun addItem(dayId: String, savedPlaceId: String, targetIndex: Int): String {
            val itemId = delegate.addItem(dayId, savedPlaceId, targetIndex)
            successfulAdds += 1
            if (successfulAdds == 1) deleteDay()
            return itemId
        }
    }
}
