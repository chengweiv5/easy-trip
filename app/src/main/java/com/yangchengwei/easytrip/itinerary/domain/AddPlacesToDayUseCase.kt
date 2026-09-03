package com.yangchengwei.easytrip.itinerary.domain

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

data class AddPlacesRequest(
    val tripId: String,
    val dayId: String,
    val savedPlaceIds: List<String>,
    val operationId: String? = null,
)

sealed interface AddPlacesOutcome {
    data class Success(
        val dayId: String,
        val createdItemIds: List<String>,
    ) : AddPlacesOutcome

    data class PartialSuccess(
        val dayId: String,
        val createdItemIds: List<String>,
        val failedPlaceIds: List<String>,
    ) : AddPlacesOutcome

    data class TargetDayMissing(
        val retainedPlaceIds: List<String>,
        val createdItemIds: List<String> = emptyList(),
    ) : AddPlacesOutcome
}

class AddPlacesToDayUseCase(private val repository: ItineraryRepository) {
    suspend operator fun invoke(request: AddPlacesRequest): AddPlacesOutcome {
        val day = try {
            repository.observeDay(request.dayId).first()
        } catch (_: TargetDayNotFoundException) {
            return AddPlacesOutcome.TargetDayMissing(request.savedPlaceIds)
        }
        if (day.tripId != request.tripId) {
            return AddPlacesOutcome.TargetDayMissing(request.savedPlaceIds)
        }

        val createdItemIds = mutableListOf<String>()
        val failedPlaceIds = mutableListOf<String>()
        var targetIndex = day.items.size
        request.savedPlaceIds.forEachIndexed { placeIndex, placeId ->
            try {
                val result = request.operationId?.let { operationId ->
                    repository.addItemIdempotently(
                        request.dayId,
                        placeId,
                        targetIndex,
                        "$operationId:${request.dayId}:$placeIndex",
                    )
                } ?: AddItineraryItemResult(
                    repository.addItem(request.dayId, placeId, targetIndex),
                    created = true,
                )
                createdItemIds += result.itemId
                if (result.created) targetIndex += 1
            } catch (failure: CancellationException) {
                throw failure
            } catch (_: TargetDayNotFoundException) {
                return AddPlacesOutcome.TargetDayMissing(
                    retainedPlaceIds = request.savedPlaceIds,
                    createdItemIds = createdItemIds,
                )
            } catch (_: RecoverablePlaceAddException) {
                failedPlaceIds += placeId
            }
        }
        return if (failedPlaceIds.isEmpty()) {
            AddPlacesOutcome.Success(request.dayId, createdItemIds)
        } else {
            AddPlacesOutcome.PartialSuccess(request.dayId, createdItemIds, failedPlaceIds)
        }
    }
}
