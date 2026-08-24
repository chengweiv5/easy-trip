package com.yangchengwei.easytrip.permission

import androidx.lifecycle.SavedStateHandle
import com.yangchengwei.easytrip.workspace.PermissionKind
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationPermissionCoordinatorTest {
    @Test fun enteringWorkspaceDoesNotRequestLocation() {
        val coordinator = LocationPermissionCoordinator(SavedStateHandle())

        assertNull(coordinator.explanation.value)
        assertFalse(coordinator.effects.tryReceive().isSuccess)
    }

    @Test fun locateClickShowsExplanationBeforeSystemRequest() {
        val coordinator = LocationPermissionCoordinator(SavedStateHandle())

        coordinator.onLocateClick(isGranted = false, shouldShowRationale = false)

        assertEquals(PermissionKind.DEVICE_LOCATION, coordinator.explanation.value)
        assertFalse(coordinator.effects.tryReceive().isSuccess)
    }

    @Test fun confirmationEmitsOnePermissionEffect() = runTest {
        val coordinator = LocationPermissionCoordinator(SavedStateHandle())
        coordinator.onLocateClick(isGranted = false, shouldShowRationale = false)

        coordinator.confirmExplanation()
        coordinator.confirmExplanation()

        assertEquals(WorkspaceEffect.RequestLocationPermission, coordinator.effects.receive())
        assertFalse(coordinator.effects.tryReceive().isSuccess)
        assertNull(coordinator.explanation.value)
    }

    @Test fun ordinaryDenialCanRetry() {
        val saved = SavedStateHandle()
        val coordinator = LocationPermissionCoordinator(saved)
        coordinator.onLocateClick(isGranted = false, shouldShowRationale = false)
        coordinator.confirmExplanation()

        coordinator.onPermissionResult(isGranted = false, shouldShowRationale = true)
        coordinator.onLocateClick(isGranted = false, shouldShowRationale = true)

        assertEquals(PermissionKind.DEVICE_LOCATION, coordinator.explanation.value)
        assertTrue(saved.get<Boolean>("location.permission.hasRequested") == true)
    }

    @Test fun requestedPermissionWithoutRationaleIsPermanentDenial() {
        val coordinator = LocationPermissionCoordinator(
            SavedStateHandle(mapOf("location.permission.hasRequested" to true)),
        )

        coordinator.onLocateClick(isGranted = false, shouldShowRationale = false)

        assertEquals(PermissionKind.DEVICE_LOCATION_SETTINGS, coordinator.explanation.value)
    }

    @Test fun permanentDenialConfirmationOpensApplicationSettingsOnce() = runTest {
        val coordinator = LocationPermissionCoordinator(
            SavedStateHandle(mapOf("location.permission.hasRequested" to true)),
        )
        coordinator.onLocateClick(isGranted = false, shouldShowRationale = false)

        coordinator.confirmExplanation()
        coordinator.confirmExplanation()

        assertEquals(WorkspaceEffect.OpenApplicationSettings, coordinator.effects.receive())
        assertFalse(coordinator.effects.tryReceive().isSuccess)
    }
}
