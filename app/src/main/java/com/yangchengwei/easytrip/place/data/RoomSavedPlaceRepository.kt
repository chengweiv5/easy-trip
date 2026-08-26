package com.yangchengwei.easytrip.place.data

import android.icu.lang.UCharacter
import androidx.room.withTransaction
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.domain.PlaceDeletionImpact
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

    override fun observeUsageCounts(tripId: String): Flow<Map<String, Int>> =
        dao.observeUsageCounts(tripId).map { rows -> rows.associate { it.placeId to it.usageCount } }

    override suspend fun save(tripId: String, candidate: PlaceCandidate): SavePlaceResult = database.withTransaction {
        dao.placeId(tripId, candidate.poiId)?.let { return@withTransaction SavePlaceResult.AlreadySaved(it) }
        val point = requireNotNull(candidate.point) { "无法收藏缺少坐标的地点" }
        val id = idFactory()
        val inserted = dao.insertPlace(SavedPlaceEntity(id, tripId, candidate.poiId, candidate.name, candidate.address, point.latitude, point.longitude, cityCode = candidate.cityCode))
        if (inserted != -1L) SavePlaceResult.Saved(id)
        else SavePlaceResult.AlreadySaved(requireNotNull(dao.placeId(tripId, candidate.poiId)))
    }

    override suspend fun updateDetails(placeId: String, note: String, tagNames: Set<String>) = database.withTransaction {
        val place = requireNotNull(dao.place(placeId)) { "Unknown place: $placeId" }
        val unique = linkedMapOf<String, String>()
        tagNames.forEach { raw ->
            val display = raw.trim()
            require(display.isNotEmpty()) { "标签不能为空" }
            require(tagUnits(display) <= 24) { "标签不能超过 24 个单位" }
            unique.putIfAbsent(normalize(display), display)
        }
        require(unique.size <= 8) { "最多选择 8 个标签" }
        require(dao.updateNote(placeId, note.trim()) == 1)
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
    override suspend fun deletionImpact(placeId: String): PlaceDeletionImpact = database.withTransaction {
        requireNotNull(dao.place(placeId)) { "Unknown place: $placeId" }
        PlaceDeletionImpact(dao.usageCount(placeId), dao.incidentRouteLegCount(placeId))
    }
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

    private fun tagUnits(value: String): Int {
        var units = 0
        var offset = 0
        while (offset < value.length) {
            val codePoint = value.codePointAt(offset)
            units += if (Character.UnicodeScript.of(codePoint) in cjkScripts) 2 else 1
            offset += Character.charCount(codePoint)
        }
        return units
    }

    private companion object {
        val cjkScripts = setOf(
            Character.UnicodeScript.HAN,
            Character.UnicodeScript.HIRAGANA,
            Character.UnicodeScript.KATAKANA,
            Character.UnicodeScript.HANGUL,
        )
    }
}
