package com.yangchengwei.easytrip.expense

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.expense.data.RoomExpenseRepository
import com.yangchengwei.easytrip.itinerary.data.RoomItineraryRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class ExpenseV2MigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "v200-expense-migration-test"
    private val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(), EasyTripDatabase::class.java,
        emptyList(), FrameworkSQLiteOpenHelperFactory(),
    )

    @After fun close() { context.deleteDatabase(name) }

    @Test fun legacyAmountsMigrateExactlyOnceIncludingZeroAndKeepRouteCategory() = runBlocking {
        helper.createDatabase(name, 8).apply {
            execSQL("INSERT INTO trips (id,name,timeMode,startDate,travelMode,createdAt,updatedAt,hasTraveled) VALUES ('trip','旅行','DRAFT',NULL,'FLEXIBLE',0,0,0)")
            execSQL("INSERT INTO trip_days (id,tripId,position) VALUES ('day','trip',9000)")
            listOf("empty" to null, "zero" to 0L, "hotel" to 42800L).forEachIndexed { index, (id, cents) ->
                execSQL("INSERT INTO saved_places (id,tripId,amapPoiId,name,address,latitude,longitude,cityMetadataVersion) VALUES ('$id','trip','$id','$id','地址',1,2,0)")
                execSQL("INSERT INTO itinerary_items (id,tripDayId,tripId,savedPlaceId,position,expenseCents,autoTimingPending) VALUES ('$id','day','trip','$id',${index * 1000},${cents ?: "NULL"},0)")
            }
            execSQL("INSERT INTO route_legs (id,tripDayId,fromItemId,toItemId,recommendedMode,status,version,updatedAt,expenseCents) VALUES ('leg','day','zero','hotel','WALK','SUCCESS',1,0,5850)")
            close()
        }
        helper.runMigrationsAndValidate(name, 9, true, EasyTripDatabase.MIGRATION_8_9).close()
        repeat(2) {
            val db = Room.databaseBuilder(context, EasyTripDatabase::class.java, name)
                .addMigrations(EasyTripDatabase.MIGRATION_8_9).build()
            try {
                val records = RoomExpenseRepository(db).observeRecords().first()
                assertEquals(3, records.size)
                assertEquals(0L, records.single { it.key.id == "legacy:zero" }.cents)
                assertEquals(42800L, records.single { it.key.id == "legacy:hotel" }.cents)
                assertTrue(records.filter { it.key.kind == ExpenseSourceKind.PLACE }.all { it.category == null })
                assertEquals(ExpenseCategory.TRANSPORT, records.single { it.key.id == "leg" }.category)
                assertTrue(records.all { it.date == null })
                val trip = db.tripDao().observeTrips().first().single()
                assertEquals(48650L, trip.expenseCents)
                assertEquals(3, trip.recordedExpenseCount)
                val items = RoomItineraryRepository(db, db.itineraryEditingDao(), db.routeLegDao())
                items.updateDetailsWithExpense("hotel", null, null, null, 42800)
                try {
                    items.updateDetailsWithExpense("hotel", null, null, "不应保存", 42801)
                    fail("修改待分类费用必须选择类别")
                } catch (_: IllegalArgumentException) { }
                assertEquals(records, RoomExpenseRepository(db).observeRecords().first())
            } finally { db.close() }
        }
    }
}
