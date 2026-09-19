package com.yangchengwei.easytrip.workspace

import android.location.Location
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import com.amap.api.maps.LocationSource
import com.amap.api.maps.MapView
import com.amap.api.maps.MapsInitializer
import com.amap.api.maps.model.LatLng
import com.yangchengwei.easytrip.amap.TestConsentGate
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import com.yangchengwei.easytrip.itinerary.ui.DayItineraryUiState
import com.yangchengwei.easytrip.place.ui.PlacePoolUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LocateVisibleViewportTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val sheetLevel = mutableStateOf(WorkspaceSheetLevel.HALF)
    private val request = mutableStateOf(0)
    private var locationListener: LocationSource.OnLocationChangedListener? = null
    private lateinit var mapView: MapView

    @Test fun locateCentersInVisibleMapForHalfAndCollapsedSheet() {
        showWorkspace()
        listOf(WorkspaceSheetLevel.HALF, WorkspaceSheetLevel.COLLAPSED).forEach { level ->
            compose.runOnIdle { sheetLevel.value = level }
            compose.waitForIdle()
            clickLocate()
            emitFix()
            assertLocationInVisibleCenter(level.name)
        }
    }

    @Test fun delayedFixUsesCurrentDrawerHeightAndLaterChangesDoNotRecenter() {
        showWorkspace()
        clickLocate()
        compose.runOnIdle { sheetLevel.value = WorkspaceSheetLevel.COLLAPSED }
        compose.waitForIdle()
        emitFix()
        assertLocationInVisibleCenter("Drawer collapsed while awaiting first fix")

        val locatedCamera = compose.runOnIdle { mapView.map.cameraPosition }
        compose.runOnIdle { sheetLevel.value = WorkspaceSheetLevel.HALF }
        compose.waitForIdle()
        emitFix(latitude = 30.26, longitude = 120.16)
        SystemClock.sleep(700)
        compose.runOnIdle {
            val currentCamera = mapView.map.cameraPosition
            assertEquals("Drawer changes and later fixes must not recenter", locatedCamera.target.latitude, currentCamera.target.latitude, .000001)
            assertEquals(locatedCamera.target.longitude, currentCamera.target.longitude, .000001)
            assertEquals(locatedCamera.zoom, currentCamera.zoom, .001f)
        }
        // A new click still uses the latest fix and the latest visible region.
        clickLocate()
        assertLocationInVisibleCenter("Locate again after changing drawer", latitude = 30.26, longitude = 120.16)
    }

    private fun showWorkspace() {
        MapsInitializer.updatePrivacyShow(compose.activity, true, true)
        MapsInitializer.updatePrivacyAgree(compose.activity, true)
        val consent = TestConsentGate().let { it.show(); requireNotNull(it.decide(true)) }
        compose.setContent {
            EasyTripTheme {
                TripWorkspaceScreen(
                    pageState = TripWorkspacePageState.Ready(
                        TripWorkspaceUiState(tripName = "定位居中测试", sheetLevel = sheetLevel.value).toReadyState(),
                    ),
                    consent = consent,
                    onAction = { if (it == TripWorkspaceAction.Locate) request.value++ },
                    onMarkerClick = {}, onMapPoiClick = {},
                    placeState = PlacePoolUiState(), onPlaceAction = {},
                    itineraryState = DayItineraryUiState(), onItineraryAction = {},
                    onCloseOverlay = {}, onDismissMapPlace = {},
                    locateRequest = request.value,
                    mapHostFactory = { context -> RealAmapMapHost(context).also { host ->
                        mapView = host.view as MapView
                        mapView.map.setLocationSource(object : LocationSource {
                            override fun activate(listener: LocationSource.OnLocationChangedListener) { locationListener = listener }
                            override fun deactivate() { locationListener = null }
                        })
                    } },
                )
            }
        }
        compose.waitUntil(20_000) { compose.onNodeWithTag("workspace-locate").runCatching { fetchSemanticsNode() }.isSuccess }
    }

    private fun clickLocate() {
        compose.onNodeWithTag("workspace-locate").performClick()
        compose.waitUntil(5_000) { locationListener != null }
    }

    private fun emitFix(latitude: Double = 30.25, longitude: Double = 120.15) {
        compose.runOnIdle {
            checkNotNull(locationListener).onLocationChanged(Location("test").apply {
                this.latitude = latitude
                this.longitude = longitude
                accuracy = 5f
                time = System.currentTimeMillis()
            })
        }
    }

    private fun assertLocationInVisibleCenter(label: String, latitude: Double = 30.25, longitude: Double = 120.15) {
        SystemClock.sleep(1_200)
        val sheet = compose.onNodeWithTag("workspace-sheet").getUnclippedBoundsInRoot()
        val mapBounds = compose.onNodeWithTag("workspace-map").getUnclippedBoundsInRoot()
        compose.runOnIdle {
            // Both bounds are relative to the Compose root, which can start below the status bar.
            val sheetTopPx = with(compose.density) { (sheet.top - mapBounds.top).toPx() }
            val point = mapView.map.projection.toScreenLocation(LatLng(latitude, longitude))
            assertEquals("Compose map bounds must match the native MapView", with(compose.density) { (mapBounds.bottom - mapBounds.top).toPx() }, mapView.height.toFloat(), 1f)
            assertTrue("Visible map must be shorter than full MapView", sheetTopPx < mapView.height)
            assertEquals("$label location must be horizontally centered", mapView.width / 2f, point.x.toFloat(), 6f)
            assertEquals("$label location must center above the drawer", sheetTopPx / 2f, point.y.toFloat(), 6f)
        }
    }
}
