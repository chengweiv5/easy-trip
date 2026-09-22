package com.yangchengwei.easytrip.itinerary.data

import androidx.room.RoomDatabase
import androidx.room.withTransaction
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.core.model.RouteStatus
import com.yangchengwei.easytrip.core.model.TransportMode
import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.itinerary.domain.AddItineraryItemResult
import com.yangchengwei.easytrip.itinerary.domain.DayItinerary
import com.yangchengwei.easytrip.itinerary.domain.Edge
import com.yangchengwei.easytrip.itinerary.domain.ItineraryItem
import com.yangchengwei.easytrip.itinerary.domain.ItineraryItemNotFoundException
import com.yangchengwei.easytrip.itinerary.domain.ItineraryPlace
import com.yangchengwei.easytrip.itinerary.domain.ItineraryRepository
import com.yangchengwei.easytrip.itinerary.domain.ItineraryTimingChange
import com.yangchengwei.easytrip.itinerary.domain.RecoverablePlaceAddException
import com.yangchengwei.easytrip.itinerary.domain.TargetDayNotFoundException
import com.yangchengwei.easytrip.itinerary.domain.adjacencyDiff
import com.yangchengwei.easytrip.place.data.SavedPlaceEntity
import com.yangchengwei.easytrip.route.data.RouteLegDao
import com.yangchengwei.easytrip.route.data.RouteLegEntity
import com.yangchengwei.easytrip.route.domain.TransportModeRecommender
import java.time.Clock
import java.time.LocalTime
import java.util.UUID
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomItineraryRepository(
    private val database: RoomDatabase,
    private val itineraryDao: ItineraryDao,
    private val routeLegDao: RouteLegDao,
    private val clock: Clock = Clock.systemUTC(),
    private val itemIdFactory: () -> String = { UUID.randomUUID().toString() },
    private val legIdFactory: () -> String = { UUID.randomUUID().toString() },
    private val isOnline: () -> Boolean = { true },
    private val recommendMode: (SavedPlaceEntity, SavedPlaceEntity, TravelMode) -> TransportMode = ::defaultRecommendMode,
) : ItineraryRepository {
    override fun observeDay(dayId: String): Flow<DayItinerary> = itineraryDao.observeDayRows(dayId).map { rows ->
        if (rows.isEmpty()) throw TargetDayNotFoundException(dayId)
        DayItinerary(dayId, rows.first().tripId, rows.mapNotNull { row ->
            val itemId = row.itemId ?: return@mapNotNull null
            ItineraryItem(
                itemId,
                ItineraryPlace(
                    requireNotNull(row.placeId),
                    requireNotNull(row.placeName),
                    requireNotNull(row.placeAddress),
                    GeoPoint(requireNotNull(row.latitude), requireNotNull(row.longitude)),
                ),
                row.arrivalTime,
                row.stayDurationMinutes,
                row.note,
            )
        })
    }

    override suspend fun addItem(dayId: String, savedPlaceId: String, targetIndex: Int): String =
        addItemInternal(dayId, savedPlaceId, targetIndex, idempotencyKey = null).itemId

    override suspend fun addItemIdempotently(
        dayId: String,
        savedPlaceId: String,
        targetIndex: Int,
        idempotencyKey: String,
    ): AddItineraryItemResult = addItemInternal(dayId, savedPlaceId, targetIndex, idempotencyKey)

    private suspend fun addItemInternal(
        dayId: String,
        savedPlaceId: String,
        targetIndex: Int,
        idempotencyKey: String?,
    ): AddItineraryItemResult = database.withTransaction {
        idempotencyKey?.let { key ->
            itineraryDao.itemByIdempotencyKey(key)?.let { existing ->
                require(existing.tripDayId == dayId && existing.savedPlaceId == savedPlaceId) {
                    "Idempotency key reused for another occurrence: $key"
                }
                return@withTransaction AddItineraryItemResult(existing.id, created = false)
            }
        }
        val tripId = itineraryDao.tripIdForDay(dayId) ?: throw TargetDayNotFoundException(dayId)
        val place = itineraryDao.savedPlace(savedPlaceId) ?: throw RecoverablePlaceAddException(savedPlaceId)
        if (place.tripId != tripId) throw RecoverablePlaceAddException(savedPlaceId)
        val old = itineraryDao.items(dayId)
        require(targetIndex in 0..old.size) { "Invalid target index: $targetIndex" }
        park(old)
        val id = itemIdFactory()
        val item = ItineraryItemEntity(
            id = id,
            tripDayId = dayId,
            tripId = tripId,
            savedPlaceId = savedPlaceId,
            position = NEW_ITEM_POSITION,
            idempotencyKey = idempotencyKey,
        )
        itineraryDao.insertItem(item)
        val new = old.toMutableList().apply { add(targetIndex, item) }
        reorder(new)
        syncLegs(dayId, old.map { it.id }, new.map { it.id })
        AddItineraryItemResult(id, created = true)
    }

    override suspend fun moveItem(itemId: String, targetDayId: String, targetIndex: Int) = database.withTransaction {
        val item = itineraryDao.item(itemId) ?: throw ItineraryItemNotFoundException(itemId)
        val targetTripId = requireNotNull(itineraryDao.tripIdForDay(targetDayId)) { "Unknown day: $targetDayId" }
        require(item.tripId == targetTripId) { "Cannot move item across trips" }
        val sourceOld = itineraryDao.items(item.tripDayId)
        val targetOld = if (targetDayId == item.tripDayId) sourceOld else itineraryDao.items(targetDayId)
        val maxIndex = if (targetDayId == item.tripDayId) sourceOld.lastIndex else targetOld.size
        require(targetIndex in 0..maxIndex) { "Invalid target index: $targetIndex" }
        val sourceNew = sourceOld.filterNot { it.id == itemId }.toMutableList()
        val targetNew = if (targetDayId == item.tripDayId) sourceNew else targetOld.toMutableList()
        targetNew.add(targetIndex, item.copy(tripDayId = targetDayId, tripId = targetTripId))
        if (targetDayId == item.tripDayId) {
            park(sourceOld)
            require(itineraryDao.moveRow(itemId, targetDayId, targetTripId, MOVING_ITEM_POSITION) == 1)
            reorder(targetNew)
            syncLegs(targetDayId, sourceOld.map { it.id }, targetNew.map { it.id })
        } else {
            val sourceDiff = adjacencyDiff(sourceOld.map { it.id }, sourceNew.map { it.id })
            val targetDiff = adjacencyDiff(targetOld.map { it.id }, targetNew.map { it.id })
            deleteLegs(item.tripDayId, sourceDiff.deleted)
            deleteLegs(targetDayId, targetDiff.deleted)
            park(sourceOld)
            park(targetOld)
            require(itineraryDao.moveRow(itemId, targetDayId, targetTripId, MOVING_ITEM_POSITION) == 1)
            reorder(sourceNew)
            reorder(targetNew)
            createLegs(item.tripDayId, sourceDiff.created)
            createLegs(targetDayId, targetDiff.created)
        }
    }

    override suspend fun deleteItem(itemId: String) = database.withTransaction {
        val item = itineraryDao.item(itemId) ?: throw ItineraryItemNotFoundException(itemId)
        val old = itineraryDao.items(item.tripDayId)
        park(old)
        require(itineraryDao.deleteRow(itemId) == 1)
        val new = old.filterNot { it.id == itemId }
        reorder(new)
        syncLegs(item.tripDayId, old.map { it.id }, new.map { it.id })
    }

    override suspend fun compareAndSetTiming(change: ItineraryTimingChange): ItineraryTimingChange? = database.withTransaction {
        require(change.after.stayMinutes == null || change.after.stayMinutes >= 0)
        val old = itineraryDao.items(change.dayId)
        val oldIds = old.map { it.id }
        if (change.beforeOrder != null && change.beforeOrder != oldIds) return@withTransaction null
        val item = old.firstOrNull { it.id == change.itemId } ?: return@withTransaction null
        val newIds = change.afterOrder?.also { requested ->
            require(change.beforeOrder != null)
            require(requested.size == oldIds.size && requested.toSet() == oldIds.toSet())
            require(requested.filterNot { it == item.id } == oldIds.filterNot { it == item.id })
        } ?: run {
            val arrival = change.after.arrivalTime
            if (arrival == null || arrival == change.before.arrivalTime) oldIds else {
                // Move only this occurrence. Untimed visits and other manual ordering stay intact;
                // equal arrivals retain their original relative order.
                val remaining = old.filterNot { it.id == item.id }
                val next = remaining.indexOfFirst { other ->
                    other.arrivalTime?.let { it > arrival || it == arrival && other.position > item.position } == true
                }
                val index = if (next >= 0) next else {
                    val lastTimed = remaining.indexOfLast { it.arrivalTime != null }
                    if (lastTimed >= 0) lastTimed + 1 else oldIds.indexOf(item.id)
                }
                remaining.map { it.id }.toMutableList().apply { add(index, item.id) }
            }
        }
        if (itineraryDao.conditionalTiming(change.tripId, change.dayId, change.itemId,
            change.before.arrivalTime, change.before.stayMinutes,
            change.after.arrivalTime, change.after.stayMinutes) != 1) return@withTransaction null
        if (newIds != oldIds) {
            park(old)
            val byId = old.associateBy { it.id }
            reorder(newIds.map { byId.getValue(it) })
            syncLegs(change.dayId, oldIds, newIds)
        }
        change.copy(beforeOrder = oldIds, afterOrder = newIds)
    }

    override suspend fun updateTiming(itemId: String, arrivalTime: LocalTime?, stayMinutes: Int?) {
        require(stayMinutes == null || stayMinutes >= 0)
        require(itineraryDao.timing(itemId, arrivalTime, stayMinutes) == 1) { "Unknown item: $itemId" }
    }

    override suspend fun updateDetails(itemId: String, arrivalTime: LocalTime?, stayMinutes: Int?, note: String?) {
        require(stayMinutes == null || stayMinutes >= 0)
        val normalizedNote = note?.trim()?.ifEmpty { null }
        require(itineraryDao.details(itemId, arrivalTime, stayMinutes, normalizedNote) == 1) { "Unknown item: $itemId" }
    }

    override suspend fun removePlaceOccurrences(placeId: String) = database.withTransaction {
        val affected = itineraryDao.itemsForPlace(placeId)
        if (affected.isEmpty()) return@withTransaction
        val changes = affected.groupBy(ItineraryItemEntity::tripDayId).map { (dayId, removed) ->
            val old = itineraryDao.items(dayId)
            val removedIds = removed.mapTo(mutableSetOf(), ItineraryItemEntity::id)
            Triple(dayId, old, old.filterNot { it.id in removedIds })
        }
        require(itineraryDao.deleteRowsForPlace(placeId) == affected.size)
        changes.forEach { (dayId, old, new) ->
            reorder(new)
            syncLegs(dayId, old.map { it.id }, new.map { it.id })
        }
    }

    private suspend fun park(items: List<ItineraryItemEntity>) {
        items.forEachIndexed { index, item -> require(itineraryDao.position(item.id, Long.MIN_VALUE + index) == 1) }
    }

    private suspend fun reorder(items: List<ItineraryItemEntity>) {
        items.forEachIndexed { index, item -> require(itineraryDao.position(item.id, index * POSITION_STEP) == 1) }
    }

    private suspend fun syncLegs(dayId: String, old: List<String>, new: List<String>) {
        val diff = adjacencyDiff(old, new)
        deleteLegs(dayId, diff.deleted)
        createLegs(dayId, diff.created)
    }

    private suspend fun deleteLegs(dayId: String, edges: Set<Edge>) {
        edges.forEach { routeLegDao.deleteEdge(dayId, it.fromItemId, it.toItemId) }
    }

    private suspend fun createLegs(dayId: String, edges: Set<Edge>) {
        val travelMode = requireNotNull(itineraryDao.travelModeForDay(dayId))
        edges.forEach { createLeg(dayId, it, travelMode) }
    }

    private suspend fun createLeg(dayId: String, edge: Edge, travelMode: TravelMode) {
        val fromItem = requireNotNull(itineraryDao.item(edge.fromItemId))
        val toItem = requireNotNull(itineraryDao.item(edge.toItemId))
        val fromPlace = requireNotNull(itineraryDao.savedPlace(fromItem.savedPlaceId))
        val toPlace = requireNotNull(itineraryDao.savedPlace(toItem.savedPlaceId))
        routeLegDao.insert(RouteLegEntity(
            id = legIdFactory(), tripDayId = dayId, fromItemId = edge.fromItemId, toItemId = edge.toItemId,
            recommendedMode = recommendMode(fromPlace, toPlace, travelMode),
            status = if (isOnline()) RouteStatus.PENDING else RouteStatus.WAITING_NETWORK,
            version = 1, updatedAt = clock.instant(),
        ))
    }

    companion object {
        const val POSITION_STEP = 1_000L
        const val NEW_ITEM_POSITION = Long.MAX_VALUE
        const val MOVING_ITEM_POSITION = Long.MAX_VALUE - 1
    }
}

fun haversineMeters(fromLatitude: Double, fromLongitude: Double, toLatitude: Double, toLongitude: Double): Double {
    val lat1 = Math.toRadians(fromLatitude); val lat2 = Math.toRadians(toLatitude)
    val deltaLat = lat2 - lat1; val deltaLon = Math.toRadians(toLongitude - fromLongitude)
    val a = sin(deltaLat / 2) * sin(deltaLat / 2) + cos(lat1) * cos(lat2) * sin(deltaLon / 2) * sin(deltaLon / 2)
    return 2.0 * 6_371_000.0 * asin(sqrt(a.coerceIn(0.0, 1.0)))
}

fun defaultRecommendMode(from: SavedPlaceEntity, to: SavedPlaceEntity, travelMode: TravelMode): TransportMode {
    if (travelMode == TravelMode.SELF_DRIVE) return TransportMode.DRIVE
    return TransportModeRecommender().recommend(travelMode, haversineMeters(from.latitude, from.longitude, to.latitude, to.longitude))
}
