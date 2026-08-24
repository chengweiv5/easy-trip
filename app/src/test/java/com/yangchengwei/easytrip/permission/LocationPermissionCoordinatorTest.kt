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
    @Test fun snapshotFactoryCombinesPermissionResultAndRationale() {
        val snapshot = LocationPermissionSnapshot.from(
            permissions = setOf("fine", "coarse"),
            isGranted = { it == "coarse" },
            shouldShowRationale = { it == "fine" },
        )

        assertEquals(
            LocationPermissionSnapshot(granted = true, shouldShowRationale = true),
            snapshot,
        )
    }

    @Test fun grantedLocateClickEmitsShowCurrentLocation() = runTest {
        val coordinator = LocationPermissionCoordinator(SavedStateHandle())

        coordinator.onLocateClick(LocationPermissionSnapshot(granted = true, shouldShowRationale = false))

        assertEquals(WorkspaceEffect.ShowCurrentLocation, coordinator.effects.receive())
    }

    @Test fun successfulPermissionResultEmitsShowCurrentLocation() = runTest {
        val coordinator = LocationPermissionCoordinator(SavedStateHandle())

        coordinator.onPermissionResult(LocationPermissionSnapshot(granted = true, shouldShowRationale = false))

        assertEquals(WorkspaceEffect.ShowCurrentLocation, coordinator.effects.receive())
    }

    @Test fun requestedStatePersistsAcrossCoordinatorInstances() {
        val store = InMemoryLocationPermissionRequestStore()
        val first = LocationPermissionCoordinator(SavedStateHandle(), store)
        first.onLocateClick(LocationPermissionSnapshot(granted = false, shouldShowRationale = false))
        first.confirmExplanation()

        val second = LocationPermissionCoordinator(SavedStateHandle(), store)
        second.onLocateClick(LocationPermissionSnapshot(granted = false, shouldShowRationale = false))

        assertEquals(PermissionKind.DEVICE_LOCATION_SETTINGS, second.explanation.value)
    }

    @Test fun enteringWorkspaceDoesNotRequestLocation() {
        val coordinator = LocationPermissionCoordinator(SavedStateHandle())

        assertNull(coordinator.explanation.value)
        assertFalse(coordinator.effects.tryReceive().isSuccess)
    }

    @Test fun locateClickShowsExplanationBeforeSystemRequest() {
        val coordinator = LocationPermissionCoordinator(SavedStateHandle())

        coordinator.onLocateClick(LocationPermissionSnapshot(granted = false, shouldShowRationale = false))

        assertEquals(PermissionKind.DEVICE_LOCATION, coordinator.explanation.value)
        assertFalse(coordinator.effects.tryReceive().isSuccess)
    }

    @Test fun confirmationEmitsOnePermissionEffect() = runTest {
        val coordinator = LocationPermissionCoordinator(SavedStateHandle())
        coordinator.onLocateClick(LocationPermissionSnapshot(granted = false, shouldShowRationale = false))

        coordinator.confirmExplanation()
        coordinator.confirmExplanation()

        assertEquals(WorkspaceEffect.RequestLocationPermission, coordinator.effects.receive())
        assertFalse(coordinator.effects.tryReceive().isSuccess)
        assertNull(coordinator.explanation.value)
    }

    @Test fun ordinaryDenialCanRetry() {
        val saved = SavedStateHandle()
        val coordinator = LocationPermissionCoordinator(saved)
        coordinator.onLocateClick(LocationPermissionSnapshot(granted = false, shouldShowRationale = false))
        coordinator.confirmExplanation()

        coordinator.onPermissionResult(LocationPermissionSnapshot(granted = false, shouldShowRationale = true))
        coordinator.onLocateClick(LocationPermissionSnapshot(granted = false, shouldShowRationale = true))

        assertEquals(PermissionKind.DEVICE_LOCATION, coordinator.explanation.value)
        assertTrue(saved.get<Boolean>("location.permission.hasRequested") == true)
    }

    @Test fun requestedPermissionWithoutRationaleIsPermanentDenial() {
        val coordinator = LocationPermissionCoordinator(
            SavedStateHandle(mapOf("location.permission.hasRequested" to true)),
        )

        coordinator.onLocateClick(LocationPermissionSnapshot(granted = false, shouldShowRationale = false))

        assertEquals(PermissionKind.DEVICE_LOCATION_SETTINGS, coordinator.explanation.value)
    }

    @Test fun permanentDenialConfirmationOpensApplicationSettingsOnce() = runTest {
        val coordinator = LocationPermissionCoordinator(
            SavedStateHandle(mapOf("location.permission.hasRequested" to true)),
        )
        coordinator.onLocateClick(LocationPermissionSnapshot(granted = false, shouldShowRationale = false))

        coordinator.confirmExplanation()
        coordinator.confirmExplanation()

        assertEquals(WorkspaceEffect.OpenApplicationSettings, coordinator.effects.receive())
        assertFalse(coordinator.effects.tryReceive().isSuccess)
    }
}
