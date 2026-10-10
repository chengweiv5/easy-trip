package com.yangchengwei.easytrip.assistant

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import org.junit.Assert.*
import org.junit.Test

class AssistantMigrationTest {
    @Test fun tenToElevenKeepsEveryLegacyTableAndColumnUnchanged() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "v3-migration-10-11"
        val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), EasyTripDatabase::class.java,
            emptyList(), FrameworkSQLiteOpenHelperFactory())
        val before = mutableMapOf<String, List<List<String?>>>()
        val columns = mutableMapOf<String, String>()
        try {
            helper.createDatabase(name, 10).use { db ->
                db.execSQL("INSERT INTO trips (id,name,timeMode,startDate,travelMode,createdAt,updatedAt,hasTraveled) VALUES ('t','杭州','DRAFT',NULL,'FLEXIBLE',0,0,0)")
                db.execSQL("INSERT INTO saved_places (id,tripId,amapPoiId,name,address,latitude,longitude,note,cityMetadataVersion,category) VALUES ('p','t','poi','地点','地址',30,120,'旧备注',0,'food')")
                db.query("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name!='room_master_table'").use { tables ->
                    while (tables.moveToNext()) {
                        val table = tables.getString(0)
                        db.query("SELECT * FROM `$table`").use { rows ->
                            columns[table] = rows.columnNames.joinToString(",") { "`$it`" }
                            before[table] = buildList { while (rows.moveToNext()) add((0 until rows.columnCount).map { if (rows.isNull(it)) null else rows.getString(it) }) }
                        }
                    }
                }
            }
            helper.runMigrationsAndValidate(name, 11, true, EasyTripDatabase.MIGRATION_10_11).use { db ->
                columns.forEach { (table, cols) -> db.query("SELECT $cols FROM `$table`").use { rows ->
                    val after = buildList { while (rows.moveToNext()) add((0 until rows.columnCount).map { if (rows.isNull(it)) null else rows.getString(it) }) }
                    assertEquals(table, before[table], after)
                } }
                db.query("PRAGMA foreign_key_check").use { assertEquals(0, it.count) }
                db.query("SELECT * FROM assistant_import_receipts").use { assertEquals(0, it.count) }
            }
        } finally { context.deleteDatabase(name) }
    }
}
