package com.yangchengwei.easytrip.itinerary.domain

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class UndoAddedItemsUseCaseTest {
    @Test fun `deletes only item ids created by this submission`() = runTest {
        val repository = FakeItineraryRepository(null)

        UndoAddedItemsUseCase(repository)(UndoAddedItemsRequest(listOf("new-1", "new-2")))

        assertEquals(listOf("new-1", "new-2"), repository.deletedItemIds)
    }

    @Test fun `each completed deletion is checkpointed before cancellation`() = runTest {
        val repository = FakeItineraryRepository(null)
        val checkpoints = mutableListOf<String>()

        try {
            UndoAddedItemsUseCase(repository)(
                UndoAddedItemsRequest(listOf("new-1", "new-2")),
            ) { itemId ->
                checkpoints += itemId
                if (itemId == "new-1") throw CancellationException("process stopped")
            }
        } catch (_: CancellationException) {
        }

        assertEquals(listOf("new-1"), checkpoints)
        assertEquals(listOf("new-1"), repository.deletedItemIds)
    }

    @Test fun `already missing item is treated as completed and later items are still deleted`() = runTest {
        val repository = FakeItineraryRepository(null).apply {
            missingDeleteIds += "new-1"
        }

        val outcome = UndoAddedItemsUseCase(repository)(
            UndoAddedItemsRequest(listOf("new-1", "new-2")),
        )

        assertEquals(listOf("new-1", "new-2"), outcome.deletedItemIds)
        assertEquals(emptyList<String>(), outcome.remainingItemIds)
        assertEquals(listOf("new-2"), repository.deletedItemIds)
    }

    @Test fun `empty undo performs no writes`() = runTest {
        val repository = FakeItineraryRepository(null)

        UndoAddedItemsUseCase(repository)(UndoAddedItemsRequest(emptyList()))

        assertEquals(emptyList<String>(), repository.deletedItemIds)
    }
}
