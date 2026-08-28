package com.yangchengwei.easytrip.permission

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationPermissionCoordinatorTest {
    private val deniedFirstRequest = LocationPermissionSnapshot(false, false)
    private val ordinaryDenial = LocationPermissionSnapshot(false, true)
    private val granted = LocationPermissionSnapshot(true, false)
    private val store = RecordingRequestStore()
    private val coordinator = LocationPermissionCoordinator(store)

    @Test fun snapshotFactoryCombinesCoarseGrantAndFineRationale() {
        val snapshot = LocationPermissionSnapshot.from(
            permissions = setOf("fine", "coarse"),
            isGranted = { it == "coarse" },
            shouldShowRationale = { it == "fine" },
        )

        assertEquals(LocationPermissionSnapshot(true, true), snapshot)
    }

    @Test fun confirmExplanationEmitsLaunchEffectWithoutPersistingRequested() = runTest {
        coordinator.attachWorkspace("trip-1")
        coordinator.onLocateClick(deniedFirstRequest)
        coordinator.confirmExplanation()

        assertFalse(store.hasRequested)
        assertTrue(coordinator.effectFlow.first() is WorkspaceEffect.RequestLocationPermission)
    }

    @Test fun permissionLaunchStartedPersistsRequestedExactlyOnce() = runTest {
        val generation = requestPermissionAndReadGeneration()
        coordinator.onPermissionLaunchStarted(generation)
        coordinator.onPermissionLaunchStarted(generation)

        assertEquals(1, store.writeCount)
        assertTrue(store.hasRequested)
    }

    @Test fun permissionLaunchFailureClearsBusyAndKeepsUnrequestedFact() = runTest {
        val generation = requestPermissionAndReadGeneration()
        coordinator.onPermissionLaunchFailed(generation)

        assertFalse(store.hasRequested)
        assertTrue(coordinator.uiState.value.explanationVisible)
        assertFalse(coordinator.uiState.value.busy)
        assertEquals("无法打开系统权限请求，请重试", coordinator.uiState.value.error)
    }

    @Test fun permissionLaunchRemainsSingleFlightUntilResult() = runTest {
        val first = requestPermissionAndReadGeneration()
        coordinator.onLocateClick(ordinaryDenial)
        coordinator.confirmExplanation()

        assertNoEffect()
        coordinator.onPermissionResult(first, ordinaryDenial)
        coordinator.confirmExplanation()

        assertTrue(coordinator.effectFlow.first() is WorkspaceEffect.RequestLocationPermission)
    }

    @Test fun coarseGrantEmitsLocateOnce() = runTest {
        coordinator.attachWorkspace("trip-1")
        coordinator.onLocateClick(LocationPermissionSnapshot.from(setOf("fine", "coarse"), { it == "coarse" }, { false }))

        assertTrue(coordinator.effectFlow.first() is WorkspaceEffect.ShowCurrentLocation)
        assertNoEffect()
    }

    @Test fun fineGrantEmitsLocateOnce() = runTest {
        coordinator.attachWorkspace("trip-1")
        coordinator.onLocateClick(LocationPermissionSnapshot.from(setOf("fine", "coarse"), { it == "fine" }, { false }))

        assertTrue(coordinator.effectFlow.first() is WorkspaceEffect.ShowCurrentLocation)
        assertNoEffect()
    }

    @Test fun ordinaryDenialAllowsAnotherExplanation() = runTest {
        val generation = requestPermissionAndReadGeneration()
        coordinator.onPermissionLaunchStarted(generation)
        coordinator.onPermissionResult(generation, ordinaryDenial)
        coordinator.onLocateClick(ordinaryDenial)

        assertTrue(coordinator.uiState.value.explanationVisible)
        assertFalse(coordinator.uiState.value.permanentlyDenied)
    }

    @Test fun permanentDenialPublishesInlineSettingsState() = runTest {
        store.hasRequested = true
        coordinator.attachWorkspace("trip-1")
        coordinator.onLocateClick(deniedFirstRequest)

        assertFalse(coordinator.uiState.value.explanationVisible)
        assertTrue(coordinator.uiState.value.permanentlyDenied)
        assertNoEffect()
    }

    @Test fun stalePermissionCallbackIsIgnored() = runTest {
        val staleGeneration = requestPermissionAndReadGeneration()
        coordinator.onLocateClick(ordinaryDenial)
        coordinator.onPermissionResult(staleGeneration, granted)

        assertTrue(coordinator.uiState.value.explanationVisible)
        assertNoEffect()
    }

    @Test fun permissionGrantEmitsLocateExactlyOnce() = runTest {
        val generation = requestPermissionAndReadGeneration()
        coordinator.onPermissionResult(generation, granted)
        coordinator.onPermissionResult(generation, granted)

        assertEquals(WorkspaceEffect.ShowCurrentLocation(generation), coordinator.effectFlow.first())
        assertNoEffect()
    }

    @Test fun openSettingsCreatesRecoveryRequestForWorkspaceAndGeneration() = runTest {
        enterPermanentDenial()
        coordinator.requestApplicationSettings()

        val effect = coordinator.effectFlow.first() as WorkspaceEffect.OpenApplicationSettings
        assertEquals("trip-1", effect.workspaceId)
        assertTrue(effect.generation > 0)
    }

    @Test fun settingsLaunchFailureKeepsPermanentDenialAndShowsError() = runTest {
        val generation = requestSettingsAndReadGeneration()
        coordinator.onSettingsLaunchFailed(generation)

        assertTrue(coordinator.uiState.value.permanentlyDenied)
        assertFalse(coordinator.uiState.value.busy)
        assertEquals("无法打开应用设置，请重试", coordinator.uiState.value.error)
    }

    @Test fun settingsResumeWithGrantLocatesExactlyOnce() = runTest {
        val generation = requestSettingsAndReadGeneration()
        coordinator.onSettingsLaunchStarted(generation)
        coordinator.onWorkspaceResumed("trip-1", granted)
        coordinator.onWorkspaceResumed("trip-1", granted)

        assertEquals(WorkspaceEffect.ShowCurrentLocation(generation), coordinator.effectFlow.first())
        assertNoEffect()
    }

    @Test fun newLocateInvalidatesSettingsRecoveryAndStaleResumeCannotLocate() = runTest {
        val settingsGeneration = requestSettingsAndReadGeneration()
        coordinator.onLocateClick(ordinaryDenial)
        coordinator.onSettingsLaunchStarted(settingsGeneration)
        coordinator.onWorkspaceResumed("trip-1", granted)

        assertNoEffect()
    }

    @Test fun settingsResumeWithoutGrantKeepsPermanentDenial() = runTest {
        val generation = requestSettingsAndReadGeneration()
        coordinator.onSettingsLaunchStarted(generation)
        coordinator.onWorkspaceResumed("trip-1", deniedFirstRequest)

        assertTrue(coordinator.uiState.value.permanentlyDenied)
        assertNoEffect()
    }

    @Test fun ordinaryResumeDoesNotLocate() = runTest {
        coordinator.attachWorkspace("trip-1")
        coordinator.onWorkspaceResumed("trip-1", granted)

        assertNoEffect()
    }

    @Test fun workspaceChangeInvalidatesSettingsRecovery() = runTest {
        val generation = requestSettingsAndReadGeneration()
        coordinator.onSettingsLaunchStarted(generation)
        coordinator.attachWorkspace("trip-2")
        coordinator.onWorkspaceResumed("trip-2", granted)

        assertNoEffect()
    }

    @Test fun detachInvalidatesSettingsRecovery() = runTest {
        val generation = requestSettingsAndReadGeneration()
        coordinator.onSettingsLaunchStarted(generation)
        coordinator.detachWorkspace("trip-1")
        coordinator.attachWorkspace("trip-1")
        coordinator.onWorkspaceResumed("trip-1", granted)

        assertNoEffect()
    }

    private suspend fun requestPermissionAndReadGeneration(): Long {
        coordinator.attachWorkspace("trip-1")
        coordinator.onLocateClick(deniedFirstRequest)
        coordinator.confirmExplanation()
        return (coordinator.effectFlow.first() as WorkspaceEffect.RequestLocationPermission).generation
    }

    private fun enterPermanentDenial() {
        store.hasRequested = true
        coordinator.attachWorkspace("trip-1")
        coordinator.onLocateClick(deniedFirstRequest)
    }

    private suspend fun requestSettingsAndReadGeneration(): Long {
        enterPermanentDenial()
        coordinator.requestApplicationSettings()
        return (coordinator.effectFlow.first() as WorkspaceEffect.OpenApplicationSettings).generation
    }

    private suspend fun assertNoEffect() {
        assertNull(withTimeoutOrNull(1) { coordinator.effectFlow.first() })
    }

    private class RecordingRequestStore(initialValue: Boolean = false) : LocationPermissionRequestStore {
        private var value = initialValue
        var writeCount = 0
            private set

        override var hasRequested: Boolean
            get() = value
            set(value) {
                this.value = value
                writeCount++
            }
    }
}
