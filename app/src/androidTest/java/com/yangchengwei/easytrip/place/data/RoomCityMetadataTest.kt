package com.yangchengwei.easytrip.place.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.domain.PlaceCity
import com.yangchengwei.easytrip.place.domain.SavePlaceResult
import com.yangchengwei.easytrip.trip.data.RoomTripRepository
import com.yangchengwei.easytrip.trip.domain.CreateTrip
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class RoomCityMetadataTest {
    @Test fun legacyParentCityIsRefinedOnceWithoutChangingUserData() = runTest {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), EasyTripDatabase::class.java).build()
        try {
            val tripId = RoomTripRepository(db.tripDao()).createTrip(CreateTrip("旧城市", 1))
            val repo = RoomSavedPlaceRepository(db)
            val id = (repo.save(tripId, PlaceCandidate("poi", "地点", "地址", GeoPoint(34.5,113.0), null, "郑州", null)) as SavePlaceResult.Saved).id
            repo.updateCityMetadata(id, PlaceCity("登封市", "410185", "0371"))
            repo.updateCityMetadata(id, PlaceCity("错误城市", "999999"))
            val place = repo.observePlaces(tripId, emptySet()).first().single()
            assertEquals("登封市", place.cityName)
            assertEquals("410185", place.cityAdCode)
            assertEquals("0371", place.cityCode)
            assertEquals(1, place.cityMetadataVersion)
            assertEquals("地点", place.name)
            assertEquals("地址", place.address)
        } finally { db.close() }
    }

    @Test fun newCitiesRoundTripAndLegacyRepairPreservesOtherFieldsAndIsConditional() = runTest {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), EasyTripDatabase::class.java).build()
        try {
            val tripId = RoomTripRepository(db.tripDao()).createTrip(CreateTrip("城市测试", 1))
            val repo = RoomSavedPlaceRepository(db)
            val point = GeoPoint(30.25, 120.15)
            val id = (repo.save(tripId, PlaceCandidate("legacy", "原名称", "原地址", point, "0571")) as SavePlaceResult.Saved).id
            repo.updateDetails(id, "原备注", setOf("原标签"))
            repo.updateCityIfMissing(id, PlaceCity("杭州市", "330100"))
            repo.updateCityIfMissing(id, PlaceCity("错误城市", "999900"))
            repo.updateCityIfMissing("already-deleted", PlaceCity("杭州市", "330100"))
            val old = repo.observePlaces(tripId, emptySet()).first().single()
            assertEquals("杭州市", old.cityName)
            assertEquals("330100", old.cityAdCode)
            assertEquals("0571", old.cityCode)
            assertEquals("原名称", old.name)
            assertEquals("原地址", old.address)
            assertEquals(point, old.point)
            assertEquals("原备注", old.note)
            assertEquals("原标签", old.tags.single().name)
            repo.save(tripId, PlaceCandidate("new", "外滩", "黄浦区", GeoPoint(31.2, 121.5), "021", "上海市", "310000"))
            val fresh = repo.observePlaces(tripId, emptySet()).first().first { it.amapPoiId == "new" }
            assertEquals("上海市", fresh.cityName)
            assertEquals("310000", fresh.cityAdCode)
            assertEquals("021", fresh.cityCode)
        } finally { db.close() }
    }
}
