package com.yangchengwei.easytrip.itinerary.domain

import com.yangchengwei.easytrip.core.model.GeoPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class AddPlacesToDayUseCaseTest {
    @Test fun `fake itinerary repository preserves explicit detail fields`() = runTest {
        val repository = FakeItineraryRepository(null)

        repository.updateDetails("item", java.time.LocalTime.of(8, 30), 45, "note")

        assertEquals(listOf(ItineraryDetailCall("item", java.time.LocalTime.of(8, 30), 45, "note")), repository.detailCalls)
    }

    @Test fun `appends duplicate places in selection order after existing items`() = runTest {
        val repository = FakeItineraryRepository(day(items = listOf(item("old", "old-place"))))

        val outcome = AddPlacesToDayUseCase(repository)(
            AddPlacesRequest("trip", "day", listOf("hotel", "hotel", "museum")),
        )

        assertEquals(
            AddPlacesOutcome.Success("day", listOf("created-1", "created-2", "created-3")),
            outcome,
        )
        assertEquals(
            listOf(
                AddCall("day", "hotel", 1),
                AddCall("day", "hotel", 2),
                AddCall("day", "museum", 3),
            ),
            repository.addCalls,
        )
    }

    @Test fun `continues after a failed place without leaving an index gap`() = runTest {
        val repository = FakeItineraryRepository(day(), failedPlaceIds = setOf("missing"))

        val outcome = AddPlacesToDayUseCase(repository)(
            AddPlacesRequest("trip", "day", listOf("first", "missing", "last")),
        )

        assertEquals(
            AddPlacesOutcome.PartialSuccess("day", listOf("created-1", "created-2"), listOf("missing")),
            outcome,
        )
        assertEquals(listOf(0, 1, 1), repository.addCalls.map(AddCall::targetIndex))
    }

    @Test fun `missing target day retains every selected place and performs no writes`() = runTest {
        val repository = FakeItineraryRepository(null)
        val selected = listOf("first", "first", "second")

        val outcome = AddPlacesToDayUseCase(repository)(AddPlacesRequest("trip", "missing", selected))

        assertEquals(AddPlacesOutcome.TargetDayMissing(selected), outcome)
        assertTrue(repository.addCalls.isEmpty())
    }

    @Test fun `trip mismatch is treated as missing target day`() = runTest {
        val repository = FakeItineraryRepository(day(tripId = "other-trip"))

        val outcome = AddPlacesToDayUseCase(repository)(AddPlacesRequest("trip", "day", listOf("place")))

        assertEquals(AddPlacesOutcome.TargetDayMissing(listOf("place")), outcome)
        assertTrue(repository.addCalls.isEmpty())
    }

    @Test fun `target day invalidated after failure and success restores the complete selection`() = runTest {
        val selected = listOf("missing", "first", "first", "last")
        val repository = FakeItineraryRepository(
            day(),
            failedPlaceIds = setOf("missing"),
            invalidatesAfterAdds = 1,
        )

        val outcome = AddPlacesToDayUseCase(repository)(AddPlacesRequest("trip", "day", selected))

        assertEquals(
            AddPlacesOutcome.TargetDayMissing(
                retainedPlaceIds = selected,
                createdItemIds = listOf("created-1"),
            ),
            outcome,
        )
        assertEquals(listOf("missing", "first", "first"), repository.addCalls.map(AddCall::placeId))
    }

    @Test fun `unknown infrastructure failure is propagated`() = runTest {
        val failure = IllegalStateException("route leg write failed")
        val repository = FakeItineraryRepository(day(), infrastructureFailure = failure)

        try {
            AddPlacesToDayUseCase(repository)(AddPlacesRequest("trip", "day", listOf("place")))
            fail("Expected infrastructure failure")
        } catch (thrown: IllegalStateException) {
            assertSame(failure, thrown)
        }
    }

    @Test fun `empty selection succeeds without writes`() = runTest {
        val repository = FakeItineraryRepository(day())

        val outcome = AddPlacesToDayUseCase(repository)(AddPlacesRequest("trip", "day", emptyList()))

        assertEquals(AddPlacesOutcome.Success("day", emptyList()), outcome)
        assertTrue(repository.addCalls.isEmpty())
    }

    private fun day(tripId: String = "trip", items: List<ItineraryItem> = emptyList()) =
        DayItinerary("day", tripId, items)

    private fun item(id: String, placeId: String) = ItineraryItem(
        id,
        ItineraryPlace(placeId, placeId, "", GeoPoint(0.0, 0.0)),
        null,
        null,
    )
}

internal data class AddCall(val dayId: String, val placeId: String, val targetIndex: Int)
internal data class ItineraryDetailCall(val itemId: String, val arrivalTime: java.time.LocalTime?, val stayMinutes: Int?, val note: String?)


internal class FakeItineraryRepository(
    private var day: DayItinerary?,
    private val failedPlaceIds: Set<String> = emptySet(),
    private val invalidatesAfterAdds: Int? = null,
    private val infrastructureFailure: RuntimeException? = null,
) : ItineraryRepository {
    val addCalls = mutableListOf<AddCall>()
    val deletedItemIds = mutableListOf<String>()
    val missingDeleteIds = mutableSetOf<String>()
    val detailCalls = mutableListOf<ItineraryDetailCall>()
    private var createdCount = 0

    override fun observeDay(dayId: String): Flow<DayItinerary> = day?.let(::flowOf) ?: throw TargetDayNotFoundException(dayId)

    override suspend fun addItem(dayId: String, savedPlaceId: String, targetIndex: Int): String {
        addCalls += AddCall(dayId, savedPlaceId, targetIndex)
        if (invalidatesAfterAdds != null && createdCount >= invalidatesAfterAdds) {
            day = null
            throw TargetDayNotFoundException(dayId)
        }
        infrastructureFailure?.let { throw it }
        if (savedPlaceId in failedPlaceIds) throw RecoverablePlaceAddException(savedPlaceId)
        createdCount += 1
        return "created-$createdCount"
    }

    override suspend fun moveItem(itemId: String, targetDayId: String, targetIndex: Int) = Unit
    override suspend fun deleteItem(itemId: String) {
        if (itemId in missingDeleteIds) throw ItineraryItemNotFoundException(itemId)
        deletedItemIds += itemId
    }
    override suspend fun updateTiming(itemId: String, arrivalTime: java.time.LocalTime?, stayMinutes: Int?) = Unit
    override suspend fun updateDetails(itemId: String, arrivalTime: java.time.LocalTime?, stayMinutes: Int?, note: String?) {
        detailCalls += ItineraryDetailCall(itemId, arrivalTime, stayMinutes, note)
    }
    override suspend fun removePlaceOccurrences(placeId: String) = error("Undo must not delete by place")
}
