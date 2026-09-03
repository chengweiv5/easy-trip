package com.yangchengwei.easytrip.itinerary.domain

data class UndoAddedItemsRequest(val createdItemIds: List<String>)

data class UndoAddedItemsOutcome(
    val deletedItemIds: List<String>,
    val remainingItemIds: List<String>,
    val failure: Throwable? = null,
)

class UndoAddedItemsUseCase(private val repository: ItineraryRepository) {
    suspend operator fun invoke(
        request: UndoAddedItemsRequest,
        onItemCompleted: suspend (String) -> Unit = {},
    ): UndoAddedItemsOutcome {
        val deleted = mutableListOf<String>()
        request.createdItemIds.forEachIndexed { index, itemId ->
            try {
                repository.deleteItem(itemId)
                deleted += itemId
                onItemCompleted(itemId)
            } catch (failure: kotlinx.coroutines.CancellationException) {
                throw failure
            } catch (_: ItineraryItemNotFoundException) {
                deleted += itemId
                onItemCompleted(itemId)
            } catch (failure: Throwable) {
                return UndoAddedItemsOutcome(deleted, request.createdItemIds.drop(index), failure)
            }
        }
        return UndoAddedItemsOutcome(deleted, emptyList())
    }
}
