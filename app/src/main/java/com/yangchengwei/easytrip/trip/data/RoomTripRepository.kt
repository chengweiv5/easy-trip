package com.yangchengwei.easytrip.trip.data

import androidx.room.withTransaction
import com.yangchengwei.easytrip.core.model.TimeMode
import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.trip.domain.CreateTrip
import com.yangchengwei.easytrip.trip.domain.InsertSide
import com.yangchengwei.easytrip.trip.domain.TripDay
import com.yangchengwei.easytrip.trip.domain.TripRepository
import com.yangchengwei.easytrip.trip.domain.TripSummary
import com.yangchengwei.easytrip.trip.domain.TripWithDays
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.core.model.RouteStatus
import com.yangchengwei.easytrip.core.model.TransportMode
import com.yangchengwei.easytrip.itinerary.data.haversineMeters
import com.yangchengwei.easytrip.route.domain.TransportModeRecommender
import java.time.Clock
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomTripRepository(
    private val dao: TripDao,
    private val clock: Clock = Clock.systemUTC(),
    private val idFactory: () -> String = { UUID.randomUUID().toString() },
    private val database: EasyTripDatabase? = null,
    private val isOnline: () -> Boolean = { true },
) : TripRepository {
    override fun observeTrips(): Flow<List<TripSummary>> = dao.observeTrips().map { trips ->
        trips.map { value ->
            val trip = value.trip
            TripSummary(trip.id, trip.name, trip.startDate, trip.travelMode, value.days.size)
        }
    }

    override fun observeTrip(tripId: String): Flow<TripWithDays?> = dao.observeTrip(tripId).map { value ->
        value?.let {
            TripWithDays(
                id = it.trip.id,
                name = it.trip.name,
                startDate = it.trip.startDate,
                travelMode = it.trip.travelMode,
                days = it.days.sortedBy(TripDayEntity::position).mapIndexed { index, day -> TripDay(day.id, index) },
            )
        }
    }

    override suspend fun createTrip(command: CreateTrip): String {
        require(command.name.isNotBlank())
        require(command.dayCount >= 1)
        val tripId = idFactory()
        val now = clock.instant()
        val trip = TripEntity(
            id = tripId,
            name = command.name.trim(),
            timeMode = TimeMode.DRAFT,
            travelMode = command.travelMode,
            createdAt = now,
            updatedAt = now,
        )
        val days = List(command.dayCount) { index ->
            TripDayEntity(idFactory(), tripId, index * TripDao.POSITION_STEP)
        }
        dao.createTripWithDays(trip, days)
        return tripId
    }

    override suspend fun renameTrip(tripId: String, name: String) {
        require(name.isNotBlank())
        dao.renameTrip(tripId, name.trim(), clock.instant())
    }

    override suspend fun setStartDate(tripId: String, startDate: LocalDate?) {
        dao.setStartDate(tripId, startDate, if (startDate == null) TimeMode.DRAFT else TimeMode.DATED, clock.instant())
    }

    override suspend fun setTravelMode(tripId: String, mode: TravelMode) {
        val db = database
        if (db == null) {
            dao.setTravelMode(tripId, mode, clock.instant())
            return
        }
        db.withTransaction {
            dao.setTravelMode(tripId, mode, clock.instant())
            val status = if (isOnline()) RouteStatus.PENDING else RouteStatus.WAITING_NETWORK
            db.routeLegDao().legsForTrip(tripId).forEach { leg ->
                val fromItem = requireNotNull(db.itineraryEditingDao().item(leg.fromItemId))
                val toItem = requireNotNull(db.itineraryEditingDao().item(leg.toItemId))
                val from = requireNotNull(db.itineraryEditingDao().savedPlace(fromItem.savedPlaceId))
                val to = requireNotNull(db.itineraryEditingDao().savedPlace(toItem.savedPlaceId))
                val recommendation = if (mode == TravelMode.SELF_DRIVE) TransportMode.DRIVE else TransportModeRecommender().recommend(mode, haversineMeters(from.latitude, from.longitude, to.latitude, to.longitude))
                require(db.routeLegDao().resetRecommendation(leg.id, recommendation, status) == 1)
            }
        }
    }

    override suspend fun insertDay(tripId: String, anchorDayId: String?, side: InsertSide): String {
        val dayId = idFactory()
        dao.insertAndReorderDay(tripId, anchorDayId, side == InsertSide.AFTER, dayId, clock.instant())
        return dayId
    }

    override suspend fun moveDay(tripId: String, dayId: String, targetIndex: Int) {
        dao.moveAndReorderDay(tripId, dayId, targetIndex, clock.instant())
    }

    override suspend fun deleteDay(dayId: String) {
        dao.deleteAndReorderDay(dayId, clock.instant())
    }

    override suspend fun deleteTrip(tripId: String) {
        dao.deleteTripChecked(tripId)
    }
}
