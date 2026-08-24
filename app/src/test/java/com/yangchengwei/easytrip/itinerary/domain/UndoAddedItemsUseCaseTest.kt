package com.yangchengwei.easytrip.itinerary.domain

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class UndoAddedItemsUseCaseTest {
    @Test fun `deletes only item ids created by this submission`() = runTest {
        val repository = FakeItineraryRepository(null)

        UndoAddedItemsUseCase(repository)(UndoAddedItemsRequest(listOf("new-1", "new-2")))

        assertEquals(listOf("new-1", "new-2"), repository.deletedItemIds)
    }

    @Test fun `empty undo performs no writes`() = runTest {
        val repository = FakeItineraryRepository(null)

        UndoAddedItemsUseCase(repository)(UndoAddedItemsRequest(emptyList()))

        assertEquals(emptyList<String>(), repository.deletedItemIds)
    }
}
