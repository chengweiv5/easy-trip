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

    @Test fun firstDeniedLocateDirectlyRequestsSystemPermissionWithoutRationale() = runTest {
        coordinator.attachWorkspace("trip-1")

        coordinator.onLocateClick(deniedFirstRequest)

        assertEquals(LocationPermissionPrompt.NONE, coordinator.uiState.value.prompt)
        assertTrue(coordinator.uiState.value.busy)
        assertFalse(store.hasRequested)
        assertTrue(coordinator.effectFlow.first() is WorkspaceEffect.RequestLocationPermission)
    }

    @Test fun confirmingExplanationClearsPromptAndPriorErrorWhileBusy() = runTest {
        val failedGeneration = requestPermissionAndReadGeneration()
        coordinator.onPermissionLaunchFailed(failedGeneration)

        coordinator.confirmExplanation()

        assertEquals(LocationPermissionPrompt.NONE, coordinator.uiState.value.prompt)
        assertTrue(coordinator.uiState.value.busy)
        assertNull(coordinator.uiState.value.error)
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

    @Test fun locateDuringPermissionRequestKeepsGenerationUntilLaunchFailure() = runTest {
        val generation = requestPermissionAndReadGeneration()
        val busyState = coordinator.uiState.value

        coordinator.onLocateClick(ordinaryDenial)

        assertEquals(busyState, coordinator.uiState.value)
        assertNoEffect()

        coordinator.onPermissionLaunchFailed(generation)

        assertTrue(coordinator.uiState.value.explanationVisible)
        assertFalse(coordinator.uiState.value.busy)
        assertEquals("无法打开系统权限请求，请重试", coordinator.uiState.value.error)

        coordinator.confirmExplanation()

        assertEquals(
            WorkspaceEffect.RequestLocationPermission(generation),
            coordinator.effectFlow.first(),
        )
    }

    @Test fun locateDuringPermissionRequestKeepsGenerationUntilPermissionResult() = runTest {
        val generation = requestPermissionAndReadGeneration()
        coordinator.onPermissionLaunchStarted(generation)
        val busyState = coordinator.uiState.value

        coordinator.onLocateClick(ordinaryDenial)

        assertEquals(busyState, coordinator.uiState.value)
        assertNoEffect()

        coordinator.onPermissionResult(generation, ordinaryDenial)

        assertEquals(LocationPermissionPrompt.NONE, coordinator.uiState.value.prompt)
        assertFalse(coordinator.uiState.value.busy)
        assertFalse(coordinator.uiState.value.permanentlyDenied)
        assertNull(coordinator.uiState.value.error)
        assertNoEffect()

        coordinator.onLocateClick(ordinaryDenial)
        coordinator.confirmExplanation()

        val retry = coordinator.effectFlow.first() as WorkspaceEffect.RequestLocationPermission
        assertTrue(retry.generation > generation)
    }

    @Test fun grantedLocateDuringPermissionRequestDoesNotReplaceActiveRequest() = runTest {
        val generation = requestPermissionAndReadGeneration()
        val busyState = coordinator.uiState.value

        coordinator.onLocateClick(granted)

        assertEquals(busyState, coordinator.uiState.value)
        assertNoEffect()

        coordinator.onPermissionResult(generation, granted)

        assertEquals(LocationPermissionUiState(), coordinator.uiState.value)
        assertEquals(WorkspaceEffect.ShowCurrentLocation(generation), coordinator.effectFlow.first())
        assertNoEffect()
    }

    @Test fun stalePermissionLaunchFailureCannotReleaseNewerRequest() = runTest {
        val staleGeneration = requestPermissionAndReadGeneration()
        coordinator.onPermissionLaunchStarted(staleGeneration)
        coordinator.onPermissionResult(staleGeneration, ordinaryDenial)
        coordinator.onLocateClick(ordinaryDenial)
        coordinator.confirmExplanation()
        val activeEffect = coordinator.effectFlow.first() as WorkspaceEffect.RequestLocationPermission
        val activeState = coordinator.uiState.value

        coordinator.onPermissionLaunchFailed(staleGeneration)

        assertEquals(activeState, coordinator.uiState.value)
        assertTrue(coordinator.uiState.value.busy)
        assertNull(coordinator.uiState.value.error)
        assertNoEffect()

        coordinator.confirmExplanation()

        assertNoEffect()
        coordinator.onPermissionResult(activeEffect.generation, ordinaryDenial)
    }

    @Test fun stalePermissionResultCannotReleaseNewerRequest() = runTest {
        val staleGeneration = requestPermissionAndReadGeneration()
        coordinator.onPermissionLaunchFailed(staleGeneration)
        coordinator.onLocateClick(ordinaryDenial)
        coordinator.confirmExplanation()
        val activeEffect = coordinator.effectFlow.first() as WorkspaceEffect.RequestLocationPermission
        val activeState = coordinator.uiState.value

        coordinator.onPermissionResult(staleGeneration, granted)

        assertEquals(activeState, coordinator.uiState.value)
        assertTrue(coordinator.uiState.value.busy)
        assertNull(coordinator.uiState.value.error)
        assertNoEffect()

        coordinator.confirmExplanation()

        assertNoEffect()
        coordinator.onPermissionResult(activeEffect.generation, ordinaryDenial)
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

    @Test fun ordinaryDenialAllowsAnotherDirectSystemRequest() = runTest {
        val generation = requestPermissionAndReadGeneration()
        coordinator.onPermissionLaunchStarted(generation)
        coordinator.onPermissionResult(generation, ordinaryDenial)

        coordinator.onLocateClick(ordinaryDenial)

        assertEquals(LocationPermissionPrompt.NONE, coordinator.uiState.value.prompt)
        assertTrue(coordinator.uiState.value.busy)
        assertFalse(coordinator.uiState.value.permanentlyDenied)
        assertTrue(coordinator.effectFlow.first() is WorkspaceEffect.RequestLocationPermission)
    }

    @Test fun permanentDenialPublishesInlineSettingsState() = runTest {
        store.hasRequested = true
        coordinator.attachWorkspace("trip-1")
        coordinator.onLocateClick(deniedFirstRequest)

        assertFalse(coordinator.uiState.value.explanationVisible)
        assertTrue(coordinator.uiState.value.permanentlyDenied)
        assertNoEffect()
    }

    @Test fun detachedWorkspacePermissionCallbackIsIgnored() = runTest {
        val staleGeneration = requestPermissionAndReadGeneration()
        coordinator.detachWorkspace("trip-1")
        coordinator.attachWorkspace("trip-1")
        coordinator.onLocateClick(ordinaryDenial)
        val activeEffect = coordinator.effectFlow.first() as WorkspaceEffect.RequestLocationPermission

        coordinator.onPermissionResult(staleGeneration, granted)

        assertEquals(LocationPermissionPrompt.NONE, coordinator.uiState.value.prompt)
        assertTrue(coordinator.uiState.value.busy)
        assertTrue(activeEffect.generation > staleGeneration)
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

    @Test fun repeatedSettingsRequestKeepsSingleGenerationAndEffect() = runTest {
        val generation = requestSettingsAndReadGeneration()

        coordinator.requestApplicationSettings()

        assertTrue(coordinator.uiState.value.busy)
        assertNoEffect()
        coordinator.onSettingsLaunchFailed(generation)
        assertFalse(coordinator.uiState.value.busy)
        assertEquals("无法打开应用设置，请重试", coordinator.uiState.value.error)
    }

    @Test fun settingsLaunchFailureAllowsRetryWithNewGenerationAndClearsError() = runTest {
        val generation = requestSettingsAndReadGeneration()
        coordinator.onSettingsLaunchFailed(generation)

        assertTrue(coordinator.uiState.value.permanentlyDenied)
        assertFalse(coordinator.uiState.value.busy)
        assertEquals("无法打开应用设置，请重试", coordinator.uiState.value.error)

        coordinator.requestApplicationSettings()

        val retry = coordinator.effectFlow.first() as WorkspaceEffect.OpenApplicationSettings
        assertTrue(retry.generation > generation)
        assertTrue(coordinator.uiState.value.busy)
        assertNull(coordinator.uiState.value.error)
    }

    @Test fun settingsLaunchStartedStaysBusyUntilResume() = runTest {
        val generation = requestSettingsAndReadGeneration()

        coordinator.onSettingsLaunchStarted(generation)

        assertTrue(coordinator.uiState.value.busy)
        assertNoEffect()
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
        val permissionRequest = coordinator.effectFlow.first()
        coordinator.onSettingsLaunchStarted(settingsGeneration)
        coordinator.onWorkspaceResumed("trip-1", granted)

        assertTrue(permissionRequest is WorkspaceEffect.RequestLocationPermission)
        assertNoEffect()
    }

    @Test fun dismissingStartedSettingsRequestInvalidatesResumeAndAllowsRetry() = runTest {
        val generation = requestSettingsAndReadGeneration()
        coordinator.onSettingsLaunchStarted(generation)

        coordinator.dismissSettings()
        coordinator.onWorkspaceResumed("trip-1", granted)

        assertEquals(LocationPermissionPrompt.NONE, coordinator.uiState.value.prompt)
        assertTrue(coordinator.uiState.value.permanentlyDenied)
        assertFalse(coordinator.uiState.value.busy)
        assertNoEffect()

        coordinator.onLocateClick(deniedFirstRequest)
        coordinator.requestApplicationSettings()

        val retry = coordinator.effectFlow.first() as WorkspaceEffect.OpenApplicationSettings
        assertTrue(retry.generation > generation)
    }

    @Test fun settingsResumeWithoutGrantKeepsPermanentDenialAndAllowsRetry() = runTest {
        val generation = requestSettingsAndReadGeneration()
        coordinator.onSettingsLaunchStarted(generation)
        coordinator.onWorkspaceResumed("trip-1", deniedFirstRequest)

        assertTrue(coordinator.uiState.value.permanentlyDenied)
        assertFalse(coordinator.uiState.value.busy)
        assertNoEffect()

        coordinator.requestApplicationSettings()

        val retry = coordinator.effectFlow.first() as WorkspaceEffect.OpenApplicationSettings
        assertTrue(retry.generation > generation)
    }

    @Test fun locateWithoutClickLeavesPromptNoneAndEmitsNoEffect() = runTest {
        coordinator.attachWorkspace("trip-1")

        assertEquals(LocationPermissionPrompt.NONE, coordinator.uiState.value.prompt)
        assertNoEffect()
    }

    @Test fun permanentDenialOpensSettingsPrompt() = runTest {
        store.hasRequested = true
        coordinator.attachWorkspace("trip-1")
        coordinator.onLocateClick(deniedFirstRequest)

        assertEquals(LocationPermissionPrompt.SETTINGS, coordinator.uiState.value.prompt)
        assertTrue(coordinator.uiState.value.permanentlyDenied)
        assertNoEffect()
    }

    @Test fun dismissingSettingsKeepsPermanentFactAndLocateReopensSettings() {
        store.hasRequested = true
        coordinator.attachWorkspace("trip-1")
        coordinator.onLocateClick(deniedFirstRequest)
        coordinator.dismissSettings()

        assertEquals(LocationPermissionPrompt.NONE, coordinator.uiState.value.prompt)
        assertTrue(coordinator.uiState.value.permanentlyDenied)

        coordinator.onLocateClick(deniedFirstRequest)

        assertEquals(LocationPermissionPrompt.SETTINGS, coordinator.uiState.value.prompt)
    }

    @Test fun staleSettingsResumeFromAnotherWorkspaceIsIgnored() = runTest {
        val generation = requestSettingsAndReadGeneration()
        coordinator.onSettingsLaunchStarted(generation)

        coordinator.onWorkspaceResumed("trip-2", granted)

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
