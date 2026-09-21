package com.yangchengwei.easytrip.share

import androidx.activity.ComponentActivity
import androidx.room.Room
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.yangchengwei.easytrip.AppNavigation
import com.yangchengwei.easytrip.AppNavigationDependencies
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import com.yangchengwei.easytrip.itinerary.data.RoomItineraryRepository
import com.yangchengwei.easytrip.permission.InMemoryLocationPermissionRequestStore
import com.yangchengwei.easytrip.place.data.RoomSavedPlaceRepository
import com.yangchengwei.easytrip.place.data.SavedPlaceEntity
import com.yangchengwei.easytrip.route.data.RoomRouteLegRepository
import com.yangchengwei.easytrip.trip.data.RoomTripRepository
import com.yangchengwei.easytrip.trip.domain.CreateTrip
import com.yangchengwei.easytrip.trip.domain.TripService
import com.yangchengwei.easytrip.trip.ui.RoomDeleteImpactProvider
import com.yangchengwei.easytrip.workspace.InMemoryMapPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class ShareNavigationTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    @Test fun tripListToWorkspaceToShareAndBack(): Unit = runBlocking {
        val db=Room.inMemoryDatabaseBuilder(compose.activity,EasyTripDatabase::class.java).build()
        val trips=RoomTripRepository(db.tripDao(),database=db,isOnline={false})
        val itinerary=RoomItineraryRepository(db,db.itineraryEditingDao(),db.routeLegDao(),isOnline={false})
        val id=trips.createTrip(CreateTrip("长图导航验证",1))
        val day=trips.observeTrip(id).first()!!.days.first()
        db.savedPlaceDao().insertPlace(SavedPlaceEntity("hotel",id,"hotel","酒店","西湖",30.25,120.15))
        itinerary.addItem(day.id,"hotel",0)
        compose.setContent { EasyTripTheme {
            AppNavigation(TripService(trips),trips,RoomDeleteImpactProvider(db.deleteImpactDao()),
                dependencies=AppNavigationDependencies(RoomSavedPlaceRepository(db),itinerary,RoomRouteLegRepository(db.routeLegDao()),InMemoryMapPreferences(),InMemoryLocationPermissionRequestStore()))
        } }
        try {
            compose.waitUntil(5_000) { compose.onAllNodesWithText("长图导航验证").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("长图导航验证").performClick()
            compose.onNodeWithTag("workspace-more").performClick()
            compose.onNodeWithTag("more-menu-share").performClick()
            compose.waitUntil(15_000) { compose.onAllNodesWithTag("share-image-preview").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("share-send").assertIsEnabled()
            compose.onNodeWithText("返回").performClick()
            compose.onNodeWithTag("workspace-more").assertIsDisplayed()
        } finally {
            compose.activityRule.scenario.close()
            db.close()
        }
    }
}
