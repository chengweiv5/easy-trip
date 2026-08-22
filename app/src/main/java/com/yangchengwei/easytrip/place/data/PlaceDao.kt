package com.yangchengwei.easytrip.place.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class PlaceSnapshotRow(
    val placeId: String,
    val tripId: String,
    val amapPoiId: String,
    val placeName: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val note: String?,
    val tagId: String?,
    val tagName: String?,
)

@Dao
interface PlaceDao {
    @Query("""
        SELECT p.id AS placeId, p.tripId, p.amapPoiId, p.name AS placeName,
               p.address, p.latitude, p.longitude, p.note,
               t.id AS tagId, t.name AS tagName
        FROM saved_places p
        LEFT JOIN saved_place_tags r ON r.savedPlaceId = p.id AND r.tripId = p.tripId
        LEFT JOIN tags t ON t.id = r.tagId AND t.tripId = r.tripId
        WHERE p.tripId = :tripId
        ORDER BY p.name COLLATE NOCASE, p.id, t.name COLLATE NOCASE, t.id
    """)
    fun observeSnapshot(tripId: String): Flow<List<PlaceSnapshotRow>>
    @Query("SELECT * FROM saved_places WHERE tripId=:tripId ORDER BY name COLLATE NOCASE, id")
    fun observePlaces(tripId: String): Flow<List<SavedPlaceEntity>>
    @Query("SELECT amapPoiId FROM saved_places WHERE tripId=:tripId")
    fun observeSavedPoiIds(tripId: String): Flow<List<String>>
    @Query("SELECT * FROM tags WHERE tripId=:tripId ORDER BY name COLLATE NOCASE, id")
    fun observeTags(tripId: String): Flow<List<TagEntity>>
    @Query("SELECT * FROM saved_place_tags WHERE tripId=:tripId")
    fun observeCrossRefs(tripId: String): Flow<List<SavedPlaceTagCrossRef>>
    @Query("SELECT * FROM saved_places WHERE id=:id") suspend fun place(id: String): SavedPlaceEntity?
    @Query("SELECT id FROM saved_places WHERE tripId=:tripId AND amapPoiId=:poiId") suspend fun placeId(tripId: String, poiId: String): String?
    @Insert(onConflict=OnConflictStrategy.IGNORE) suspend fun insertPlace(value: SavedPlaceEntity): Long
    @Query("UPDATE saved_places SET note=:note WHERE id=:placeId") suspend fun updateNote(placeId: String, note: String): Int
    @Query("SELECT * FROM tags WHERE tripId=:tripId AND tagNameNormalized=:normalized") suspend fun tag(tripId: String, normalized: String): TagEntity?
    @Insert(onConflict=OnConflictStrategy.IGNORE) suspend fun insertTag(value: TagEntity): Long
    @Query("DELETE FROM saved_place_tags WHERE savedPlaceId=:placeId") suspend fun deleteCrossRefs(placeId: String)
    @Insert suspend fun insertCrossRefs(values: List<SavedPlaceTagCrossRef>)
    @Query("DELETE FROM tags WHERE tripId=:tripId AND id NOT IN (SELECT tagId FROM saved_place_tags WHERE tripId=:tripId)") suspend fun deleteOrphanTags(tripId: String)
    @Query("SELECT COUNT(*) FROM itinerary_items WHERE savedPlaceId=:placeId") suspend fun usageCount(placeId: String): Int
    @Query("DELETE FROM saved_places WHERE id=:placeId") suspend fun deletePlace(placeId: String): Int
}
