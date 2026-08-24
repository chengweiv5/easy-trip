package com.yangchengwei.easytrip.itinerary.domain

data class UndoAddedItemsRequest(val createdItemIds: List<String>)

class UndoAddedItemsUseCase(private val repository: ItineraryRepository) {
    suspend operator fun invoke(request: UndoAddedItemsRequest) {
        request.createdItemIds.forEach { repository.deleteItem(it) }
    }
}
