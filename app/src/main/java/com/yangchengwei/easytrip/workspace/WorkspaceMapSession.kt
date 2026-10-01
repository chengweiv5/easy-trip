package com.yangchengwei.easytrip.workspace

import androidx.lifecycle.SavedStateHandle
import com.yangchengwei.easytrip.core.model.GeoPoint

/** Camera coordinates include the drawer offset: restore them without applying it a second time. */
data class MapCameraState(
    val target: GeoPoint,
    val zoom: Float,
    val tilt: Float,
    val bearing: Float,
    val consumedViewportId: Long? = null,
)

/** Owned by one workspace back-stack entry, never shared between trips or with the search map. */
class WorkspaceMapSession(private val savedState: SavedStateHandle) {
    val location: GeoPoint?
        get() = savedState.get<DoubleArray>(LOCATION)?.let { GeoPoint(it[0], it[1]) }

    val camera: MapCameraState?
        get() = savedState.get<DoubleArray>(CAMERA)?.let {
            MapCameraState(GeoPoint(it[0], it[1]), it[2].toFloat(), it[3].toFloat(), it[4].toFloat(), savedState[VIEWPORT])
        }

    fun onLocated(point: GeoPoint) {
        savedState[LOCATION] = doubleArrayOf(point.latitude, point.longitude)
    }

    fun onCameraChanged(camera: MapCameraState) {
        savedState[CAMERA] = doubleArrayOf(
            camera.target.latitude, camera.target.longitude,
            camera.zoom.toDouble(), camera.tilt.toDouble(), camera.bearing.toDouble(),
        )
        savedState[VIEWPORT] = camera.consumedViewportId
    }

    private companion object {
        const val LOCATION = "workspaceLocatedPoint"
        const val CAMERA = "workspaceCamera"
        const val VIEWPORT = "workspaceCameraViewport"
    }
}
