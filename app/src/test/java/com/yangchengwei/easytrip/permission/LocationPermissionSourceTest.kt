package com.yangchengwei.easytrip.permission

import java.nio.file.Files
import java.nio.file.Paths
import com.yangchengwei.easytrip.workspace.PermissionKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationPermissionSourceTest {
    @Test fun manifestDeclaresCoarseAndFineLocation() {
        val manifest = source("src/main/AndroidManifest.xml")
        assertTrue(manifest.contains("android.permission.ACCESS_COARSE_LOCATION"))
        assertTrue(manifest.contains("android.permission.ACCESS_FINE_LOCATION"))
    }

    @Test fun coordinatorHasNoSavedStateRequestStoreOrAndroidStateOwner() {
        val coordinator = source("src/main/java/com/yangchengwei/easytrip/permission/LocationPermissionCoordinator.kt")
        assertFalse(coordinator.contains("SavedStateLocationPermissionRequestStore"))
        assertFalse(coordinator.contains("SavedStateHandle"))
        assertFalse(coordinator.contains("LifecycleOwner"))
        assertFalse(coordinator.contains("Context"))
    }

    @Test fun permissionKindsModelOnlyTheTwoDeviceLocationPrompts() {
        assertEquals(
            setOf(PermissionKind.DEVICE_LOCATION, PermissionKind.DEVICE_LOCATION_SETTINGS),
            PermissionKind.entries.toSet(),
        )
    }

    private fun source(path: String) = String(Files.readAllBytes(Paths.get(path)))
}
