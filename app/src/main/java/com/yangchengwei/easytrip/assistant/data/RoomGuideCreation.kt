package com.yangchengwei.easytrip.assistant.data

import androidx.room.withTransaction
import com.yangchengwei.easytrip.assistant.AssistantCityScope
import com.yangchengwei.easytrip.assistant.cityfirst.*
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.trip.data.RoomTripRepository
import com.yangchengwei.easytrip.trip.domain.CreateTrip

class RoomGuideCreation(private val db: EasyTripDatabase) : GuideTripCreation, GuideHandoffStore {
    private val dao get() = db.guideCreationDao()
    override suspend fun recover(command: CreateGuideTripCommand): GuideCreationRecovery = db.withTransaction {
        command.validated()
        val r = dao.receipt(command.operationId) ?: return@withTransaction GuideCreationRecovery.NotSubmitted
        require(r.tripId == command.tripId && r.digest == command.digest) { "创建回执与内容不一致" }
        if (db.tripDao().trip(r.tripId) == null) GuideCreationRecovery.Deleted
        else GuideCreationRecovery.Committed(GuideCreationReceipt(r.operationId, r.tripId, r.digest))
    }
    override suspend fun commit(command: CreateGuideTripCommand): GuideCreationReceipt = db.withTransaction {
        when (val r = recover(command)) {
            GuideCreationRecovery.Deleted -> error("旅行已删除，不会重复创建")
            is GuideCreationRecovery.Committed -> return@withTransaction r.receipt
            GuideCreationRecovery.NotSubmitted -> Unit
        }
        require(db.tripDao().trip(command.tripId) == null) { "旅行标识已存在" }
        RoomTripRepository(db.tripDao(), database = db).createTrip(CreateTrip(
            command.name, command.dayCount, command.travelMode, command.startDate, command.tripId))
        dao.insertDestination(GuideDestinationEntity(command.tripId, command.scope.encode()))
        dao.insertHandoff(GuideHandoffEntity(command.operationId, command.tripId, command.digest,
            command.material.id, command.material.digest, GuideHandoffStatus.PENDING.name, 0, null))
        dao.insertReceipt(GuideCreationReceiptEntity(command.operationId, command.tripId, command.digest))
        GuideCreationReceipt(command.operationId, command.tripId, command.digest)
    }
    override suspend fun scope(tripId: String) = dao.destination(tripId)?.let { AssistantCityScope.decode(it.cities) }
    private suspend fun GuideHandoffEntity.decode() = GuideHandoff(operationId, tripId, digest,
        GuideMaterialRef(materialId, materialDigest), requireNotNull(scope(tripId)),
        GuideHandoffStatus.valueOf(status), attempt, resultRef)
    override suspend fun find(tripId: String): GuideHandoff? = db.withTransaction { dao.forTrip(tripId)?.decode() }
    override suspend fun claim(id: String, digest: String, attempt: Int): GuideHandoff? = db.withTransaction {
        if (dao.claim(id, digest, attempt) == 1) dao.handoff(id)?.decode() else null
    }
    override suspend fun requestRetry(id: String, attempt: Int): GuideHandoff? = db.withTransaction {
        if (dao.retry(id, attempt) == 1) dao.handoff(id)?.decode() else null
    }
    override suspend fun complete(id: String, attempt: Int, status: GuideHandoffStatus, resultRef: String?): Boolean = db.withTransaction {
        require(status !in setOf(GuideHandoffStatus.PENDING, GuideHandoffStatus.CLAIMED))
        require((status == GuideHandoffStatus.IMPORTED) == (resultRef != null))
        val existing = dao.handoff(id) ?: return@withTransaction false
        if (existing.attempt != attempt) return@withTransaction false
        if (existing.status == GuideHandoffStatus.FINISHED.name)
            return@withTransaction status == GuideHandoffStatus.FINISHED
        dao.complete(id, attempt, status.name, resultRef) == 1
    }
}
