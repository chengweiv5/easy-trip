package com.yangchengwei.easytrip

import android.content.Context
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.itinerary.data.RoomItineraryRepository
import com.yangchengwei.easytrip.place.data.RoomSavedPlaceRepository
import com.yangchengwei.easytrip.route.data.RoomRouteLegRepository
import com.yangchengwei.easytrip.trip.data.RoomTripRepository
import com.yangchengwei.easytrip.trip.domain.TripService
import com.yangchengwei.easytrip.trip.ui.RoomDeleteImpactProvider
import com.yangchengwei.easytrip.workspace.AmapMapHost
import com.yangchengwei.easytrip.workspace.InMemoryMapPreferences
import com.yangchengwei.easytrip.workspace.MapLayer
import com.yangchengwei.easytrip.workspace.MapPoiUi
import com.yangchengwei.easytrip.workspace.MapUiModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class V1PencilFlowTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private lateinit var database: EasyTripDatabase
    private lateinit var repository: RoomTripRepository
    private val ids = AtomicInteger()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, EasyTripDatabase::class.java).build()
        repository = RoomTripRepository(database.tripDao(), idFactory = { "id-${ids.getAndIncrement()}" })
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun createBackAndReopenUsesRoomAndNavigatesExactlyOncePerAction() {
        val routes = mutableListOf<String>()
        val initialDate = LocalDate.of(2027, 3, 15)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
        compose.setContent {
            AppNavigation(
                service = TripService(repository),
                repository = repository,
                impacts = RoomDeleteImpactProvider(database.deleteImpactDao()),
                initialDateMillis = initialDate,
                dependencies = AppNavigationDependencies(
                    savedPlaceRepository = RoomSavedPlaceRepository(database),
                    itineraryRepository = RoomItineraryRepository(
                        database,
                        database.itineraryEditingDao(),
                        database.routeLegDao(),
                    ),
                    routeLegRepository = RoomRouteLegRepository(database.routeLegDao()),
                    mapPreferences = InMemoryMapPreferences(),
                    locationPermissionRequestStore = com.yangchengwei.easytrip.permission.InMemoryLocationPermissionRequestStore(),
                ),
                navigationObserver = AppNavigationObserver(routes::add),
                mapHostFactory = ::TestMapHost,
            )
        }

        compose.onNodeWithText("还没有旅行计划").assertIsDisplayed()
        compose.onNodeWithTag("create-trip").performClick()
        compose.onNodeWithTag("create-name").performTextInput("杭州周末")
        compose.onNodeWithTag("create-day-count").performTextInput("2")
        compose.onNodeWithTag("create-time-DATED").performClick()
        compose.onNodeWithTag("create-date-confirm").performClick()
        compose.onNodeWithTag("create-submit").performClick()

        lateinit var tripId: String
        compose.waitUntil(5_000) {
            runBlocking {
                repository.observeTrips().first().singleOrNull()?.also { tripId = it.id } != null
            }
        }
        compose.onNodeWithTag("workspace-top-bar").assertIsDisplayed()
        compose.runOnIdle { assertEquals(listOf("trips/create", "trips/$tripId"), routes) }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(listOf("trips/create", "trips/$tripId"), routes) }

        val saved = runBlocking { repository.observeTrip(tripId).first() }
        assertNotNull(saved)
        assertEquals(Instant.ofEpochMilli(initialDate).atZone(ZoneOffset.UTC).toLocalDate(), saved!!.startDate)
        assertEquals(2, saved.days.size)

        compose.onNodeWithText("返回").performClick()
        compose.onNodeWithTag("trip-$tripId").assertIsDisplayed().performClick()
        compose.onNodeWithTag("workspace-top-bar").assertIsDisplayed()
        compose.runOnIdle { assertEquals(listOf("trips/create", "trips/$tripId", "trips/$tripId"), routes) }
    }

    private class TestMapHost(context: Context) : AmapMapHost {
        override val view = View(context)
        override fun onCreate() = Unit
        override fun onResume() = Unit
        override fun onPause() = Unit
        override fun onDestroy() = Unit
        override fun render(
            model: MapUiModel,
            layer: MapLayer,
            onMarkerClick: (String) -> Unit,
            onMapPoiClick: (MapPoiUi) -> Unit,
            onLayerError: (Throwable, MapLayer) -> Unit,
        ) = Unit
    }
}
