package com.yangchengwei.easytrip.route.data

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

/** Updates old automatic defaults once, while retaining every explicit user choice. */
internal object FlexibleRouteDefaultsUpdate : RoomDatabase.Callback() {
    override fun onOpen(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            UPDATE route_legs SET
                recommendedMode = 'TAXI',
                version = CASE WHEN selectedMode IS NULL THEN version + 1 ELSE version END,
                status = CASE WHEN selectedMode IS NULL THEN 'PENDING' ELSE status END,
                distanceMeters = CASE WHEN selectedMode IS NULL THEN NULL ELSE distanceMeters END,
                durationSeconds = CASE WHEN selectedMode IS NULL THEN NULL ELSE durationSeconds END,
                polyline = CASE WHEN selectedMode IS NULL THEN NULL ELSE polyline END,
                errorKind = CASE WHEN selectedMode IS NULL THEN NULL ELSE errorKind END,
                errorCode = CASE WHEN selectedMode IS NULL THEN NULL ELSE errorCode END
            WHERE recommendedMode = 'TRANSIT'
                AND tripDayId IN (
                    SELECT d.id FROM trip_days d JOIN trips t ON t.id = d.tripId
                    WHERE t.travelMode = 'FLEXIBLE'
                )
            """.trimIndent(),
        )
    }
}
