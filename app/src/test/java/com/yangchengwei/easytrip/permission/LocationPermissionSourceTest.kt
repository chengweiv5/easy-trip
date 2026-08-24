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

    @Test fun routeUsesActivityResultBoundaryAndApplicationSettingsCallback() {
        val navigation = source("src/main/java/com/yangchengwei/easytrip/AppNavigation.kt")
        assertTrue(navigation.contains("ActivityResultContracts.RequestMultiplePermissions"))
        assertTrue(navigation.contains("onOpenApplicationSettings"))
        assertTrue(navigation.contains("Settings.ACTION_APPLICATION_DETAILS_SETTINGS"))
    }

    @Test fun mapRetryDoesNotInvokePageRetryOrRoom() {
        val screen = source("src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceScreen.kt")
        val localRetry = screen.substringAfter("onMapRetry = {").substringBefore("},\n        onAction")
        assertFalse(localRetry.contains("viewModel.retry"))
        assertFalse(localRetry.contains("repository"))
        assertFalse(localRetry.contains("Room"))
    }

    private fun source(path: String) = String(Files.readAllBytes(Paths.get(path)))
}
