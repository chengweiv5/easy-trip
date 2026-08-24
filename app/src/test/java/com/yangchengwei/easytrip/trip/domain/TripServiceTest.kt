package com.yangchengwei.easytrip.trip.domain

import com.yangchengwei.easytrip.core.model.TravelMode
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TripServiceTest {
    @Test
    fun threeDayDraftDisplaysDayNumbersWithoutDates() = runTest {
        val repository = FakeTripRepository()
        val service = TripService(repository)

        service.createTrip(CreateTrip("Kyoto", 3, TravelMode.FLEXIBLE))

        val days = repository.trip!!.days
        assertEquals(listOf("Day 1", "Day 2", "Day 3"), days.map(service::displayLabel))
        assertEquals(listOf(null, null, null), days.map { service.displayDate(repository.trip!!.startDate, it.index) })
    }

    @Test
    fun settingStartDateDisplaysConsecutiveDates() = runTest {
        val repository = FakeTripRepository()
        val service = TripService(repository)
        val tripId = service.createTrip(CreateTrip("Kyoto", 3, TravelMode.FLEXIBLE))

        service.setStartDate(tripId, LocalDate.parse("2026-10-01"))

        assertEquals(
            listOf("2026-10-01", "2026-10-02", "2026-10-03"),
            repository.trip!!.days.map { service.displayLabel(it, repository.trip!!.startDate) },
        )
    }

    @Test
    fun insertingBeforeSecondDayKeepsExistingIds() = runTest {
        val repository = FakeTripRepository()
        val service = TripService(repository)
        val tripId = service.createTrip(CreateTrip("Kyoto", 3, TravelMode.FLEXIBLE))
        val originalIds = repository.trip!!.days.map { it.id }

        val insertedId = service.insertDay(tripId, originalIds[1], InsertSide.BEFORE)

        assertEquals(listOf(originalIds[0], insertedId, originalIds[1], originalIds[2]), repository.trip!!.days.map { it.id })
    }

    @Test
    fun appendingDayUsesNullAnchorAndAfterSide() = runTest {
        val repository = FakeTripRepository()
        val service = TripService(repository)
        val tripId = service.createTrip(CreateTrip("Kyoto", 2, TravelMode.FLEXIBLE))
        val originalIds = repository.trip!!.days.map(TripDay::id)

        val appendedId = service.appendTripDay(tripId)

        assertEquals(listOf(originalIds[0], originalIds[1], appendedId), repository.trip!!.days.map(TripDay::id))
        assertEquals(null, repository.lastInsertAnchor)
        assertEquals(InsertSide.AFTER, repository.lastInsertSide)
    }

    @Test
    fun appendingToTripWithoutDaysCreatesFirstDay() = runTest {
        val repository = FakeTripRepository().apply {
            trip = TripWithDays("trip", "Empty", null, TravelMode.FLEXIBLE, emptyList())
        }

        val appendedId = TripService(repository).appendTripDay("trip")

        assertEquals(listOf(appendedId), repository.trip!!.days.map(TripDay::id))
    }

    @Test
    fun movingFirstDayToEndKeepsDayIds() = runTest {
        val repository = FakeTripRepository()
        val service = TripService(repository)
        val tripId = service.createTrip(CreateTrip("Kyoto", 3, TravelMode.FLEXIBLE))
        val originalIds = repository.trip!!.days.map { it.id }

        service.moveDay(tripId, originalIds[0], 2)

        assertEquals(listOf(originalIds[1], originalIds[2], originalIds[0]), repository.trip!!.days.map { it.id })
    }

    @Test
    fun deletingMiddleDayKeepsRemainingContainerIds() = runTest {
        val repository = FakeTripRepository()
        val service = TripService(repository)
        service.createTrip(CreateTrip("Kyoto", 3, TravelMode.FLEXIBLE))
        val originalIds = repository.trip!!.days.map { it.id }

        service.deleteDay(DayDeletion(originalIds[1], 0, 0))

        assertEquals(listOf(originalIds[0], originalIds[2]), repository.trip!!.days.map { it.id })
    }

    @Test
    fun rejectsBlankNameAndZeroDays() = runTest {
        val service = TripService(FakeTripRepository())

        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { service.createTrip(CreateTrip("  ", 1, TravelMode.FLEXIBLE)) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { service.createTrip(CreateTrip("Kyoto", 0, TravelMode.FLEXIBLE)) }
        }
    }


    @Test
    fun mutationEntriesDelegateAndRenameValidates() = runTest {
        val repository = FakeTripRepository()
        val service = TripService(repository)
        val tripId = service.createTrip(CreateTrip("Kyoto", 2))

        service.renameTrip(tripId, "  Tokyo  ")
        service.setTravelMode(tripId, TravelMode.SELF_DRIVE)
        service.deleteTrip(tripId)

        assertEquals("Tokyo", repository.renamedTo)
        assertEquals(TravelMode.SELF_DRIVE, repository.travelModeSetTo)
        assertEquals(tripId, repository.deletedTripId)
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { service.renameTrip(tripId, "  ") }
        }
    }

    private class FakeTripRepository : TripRepository {
        var renamedTo: String? = null
        var travelModeSetTo: TravelMode? = null
        var deletedTripId: String? = null
        var lastInsertAnchor: String? = "unset"
        var lastInsertSide: InsertSide? = null
        var trip: TripWithDays? = null
        private var nextId = 1
        private val trips = MutableStateFlow<List<TripSummary>>(emptyList())
        private val selected = MutableStateFlow<TripWithDays?>(null)

        override fun observeTrips(): Flow<List<TripSummary>> = trips
        override fun observeTrip(tripId: String): Flow<TripWithDays?> = selected

        override suspend fun createTrip(command: CreateTrip): String {
            val tripId = id()
            trip = TripWithDays(
                id = tripId,
                name = command.name,
                startDate = null,
                travelMode = command.travelMode,
                days = List(command.dayCount) { TripDay(id(), it) },
            )
            selected.value = trip
            return tripId
        }

        override suspend fun renameTrip(tripId: String, name: String) {
            renamedTo = name
            trip = trip!!.copy(name = name)
        }

        override suspend fun dateRangeDeletionCounts(tripId: String, dayIds: List<String>) = DateRangeDeletionCounts(0, 0, 0)
        override suspend fun applyDateRange(command: DateRangeApply) = Unit

        override suspend fun setStartDate(tripId: String, startDate: LocalDate?) {
            trip = trip!!.copy(startDate = startDate)
            selected.value = trip
        }

        override suspend fun setTravelMode(tripId: String, mode: TravelMode) {
            travelModeSetTo = mode
            trip = trip!!.copy(travelMode = mode)
        }

        override suspend fun insertDay(tripId: String, anchorDayId: String?, side: InsertSide): String {
            lastInsertAnchor = anchorDayId
            lastInsertSide = side
            val newId = id()
            val days = trip!!.days.toMutableList()
            val anchorIndex = anchorDayId?.let { id -> days.indexOfFirst { it.id == id } } ?: days.size
            val index = if (side == InsertSide.BEFORE) anchorIndex else anchorIndex + 1
            days.add(index.coerceIn(0, days.size), TripDay(newId, 0))
            trip = trip!!.copy(days = days.mapIndexed { i, day -> day.copy(index = i) })
            return newId
        }

        override suspend fun moveDay(tripId: String, dayId: String, targetIndex: Int) {
            val days = trip!!.days.toMutableList()
            val day = days.removeAt(days.indexOfFirst { it.id == dayId })
            days.add(targetIndex, day)
            trip = trip!!.copy(days = days.mapIndexed { i, item -> item.copy(index = i) })
        }

        override suspend fun deleteDay(command: DayDeletion) {
            trip = trip!!.copy(days = trip!!.days.filterNot { it.id == command.dayId }.mapIndexed { i, day -> day.copy(index = i) })
        }

        override suspend fun deleteTrip(tripId: String) {
            deletedTripId = tripId
            trip = null
        }

        private fun id() = "id-${nextId++}"
    }
}
