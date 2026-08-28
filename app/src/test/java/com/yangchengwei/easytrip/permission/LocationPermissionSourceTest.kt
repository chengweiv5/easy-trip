package com.yangchengwei.easytrip.permission

import java.nio.file.Files
import java.nio.file.Paths
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

    @Test fun permissionKindsExcludeConsentAndSettingsDialogSemantics() {
        val overlay = source("src/main/java/com/yangchengwei/easytrip/workspace/WorkspaceOverlay.kt")
        val explanation = source("src/main/java/com/yangchengwei/easytrip/permission/PermissionExplanationContent.kt")
        assertFalse(overlay.contains("MAP_SERVICE_CONSENT"))
        assertFalse(overlay.contains("DEVICE_LOCATION_SETTINGS"))
        assertFalse(explanation.contains("打开应用设置"))
        assertTrue(explanation.contains("定位仅在你主动点击后用于在地图上显示当前位置"))
    }

    private fun source(path: String) = String(Files.readAllBytes(Paths.get(path)))
}
