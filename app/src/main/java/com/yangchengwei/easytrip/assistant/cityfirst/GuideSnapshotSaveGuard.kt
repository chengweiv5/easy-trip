package com.yangchengwei.easytrip.assistant.cityfirst

import com.yangchengwei.easytrip.assistant.PlaceAssistantState
import kotlinx.coroutines.CancellationException

/** A visible save error is not new material. Do not re-encode/rewrite a failed snapshot forever. */
internal class GuideSnapshotSaveGuard {
    private var failed: Pair<String, PlaceAssistantState>? = null
    suspend fun attempt(identity: String, state: PlaceAssistantState, save: suspend () -> Unit) {
        val key = identity to state.copy(error = null)
        if (failed == key) return
        try {
            save()
            failed = null
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { failed = key; throw e }
    }
}
