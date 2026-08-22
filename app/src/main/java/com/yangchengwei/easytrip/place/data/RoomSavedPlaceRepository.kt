package com.yangchengwei.easytrip.place.data

import android.icu.lang.UCharacter
import androidx.room.withTransaction
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.domain.PlaceTag
import com.yangchengwei.easytrip.place.domain.SavePlaceResult
import com.yangchengwei.easytrip.place.domain.SavedPlace
import com.yangchengwei.easytrip.place.domain.SavedPlaceRepository
import java.text.Normalizer
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomSavedPlaceRepository(
    private val database: EasyTripDatabase,
    private val idFactory: () -> String = { UUID.randomUUID().toString() },
    private val isOnline: () -> Boolean = { true },
) : SavedPlaceRepository {
    private val dao = database.savedPlaceDao()

    override fun observePlaces(tripId: String, tagIds: Set<String>): Flow<List<SavedPlace>> =
        dao.observeSnapshot(tripId).map { rows ->
            rows.groupBy(PlaceSnapshotRow::placeId).values.mapNotNull { group ->
                val first = group.first()
                val tags = group.mapNotNull { row -> row.tagId?.let { PlaceTag(it, requireNotNull(row.tagName)) } }
                if (!tagIds.all { selected -> tags.any { it.id == selected } }) return@mapNotNull null
                SavedPlace(first.placeId, first.tripId, first.amapPoiId, first.placeName, first.address, GeoPoint(first.latitude, first.longitude), first.note.orEmpty(), tags)
            }
        }

    override fun observeTags(tripId: String): Flow<List<PlaceTag>> = dao.observeSnapshot(tripId).map { rows ->
        rows.mapNotNull { row -> row.tagId?.let { PlaceTag(it, requireNotNull(row.tagName)) } }.distinctBy(PlaceTag::id)
    }

    override fun observeSavedPoiIds(tripId: String): Flow<Set<String>> =
        dao.observeSavedPoiIds(tripId).map(List<String>::toSet)

    override suspend fun save(tripId: String, candidate: PlaceCandidate): SavePlaceResult = database.withTransaction {
        dao.placeId(tripId, candidate.poiId)?.let { return@withTransaction SavePlaceResult.AlreadySaved(it) }
        val id = idFactory()
        val inserted = dao.insertPlace(SavedPlaceEntity(id, tripId, candidate.poiId, candidate.name, candidate.address, candidate.point.latitude, candidate.point.longitude, cityCode = candidate.cityCode))
        if (inserted != -1L) SavePlaceResult.Saved(id)
        else SavePlaceResult.AlreadySaved(requireNotNull(dao.placeId(tripId, candidate.poiId)))
    }

    override suspend fun updateDetails(placeId: String, note: String, tagNames: Set<String>) = database.withTransaction {
        val place = requireNotNull(dao.place(placeId)) { "Unknown place: $placeId" }
        require(dao.updateNote(placeId, note.trim()) == 1)
        val unique = linkedMapOf<String, String>()
        tagNames.forEach { raw -> raw.trim().takeIf(String::isNotEmpty)?.let { unique.putIfAbsent(normalize(it), it) } }
        val tags = unique.map { (normalized, display) ->
            dao.tag(place.tripId, normalized) ?: run {
                val created = TagEntity(idFactory(), place.tripId, display, normalized)
                dao.insertTag(created)
                requireNotNull(dao.tag(place.tripId, normalized))
            }
        }
        dao.deleteCrossRefs(placeId)
        if (tags.isNotEmpty()) dao.insertCrossRefs(tags.map { SavedPlaceTagCrossRef(placeId, it.id, place.tripId) })
        dao.deleteOrphanTags(place.tripId)
    }

    override suspend fun usageCount(placeId: String): Int { requireNotNull(dao.place(placeId)) { "Unknown place: $placeId" }; return dao.usageCount(placeId) }
    override suspend fun deletePlaceAndReferences(placeId: String) = database.withTransaction {
        requireNotNull(dao.place(placeId)) { "Unknown place: $placeId" }
        com.yangchengwei.easytrip.itinerary.data.RoomItineraryRepository(
            database,
            database.itineraryEditingDao(),
            database.routeLegDao(),
            isOnline = isOnline,
        ).removePlaceOccurrences(placeId)
        require(dao.deletePlace(placeId) == 1)
    }

    private fun normalize(value: String) =
        UCharacter.foldCase(Normalizer.normalize(value, Normalizer.Form.NFKC), true)
}
