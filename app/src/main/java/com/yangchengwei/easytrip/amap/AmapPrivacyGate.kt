package com.yangchengwei.easytrip.amap

import android.content.Context
import com.amap.api.maps.MapsInitializer
import com.amap.api.services.core.ServiceSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ConsentSnapshot(val generation: Long, val active: Boolean)

class ConsentRegistry {
    val identity: Any = Any()
    private val mutableState = MutableStateFlow(ConsentSnapshot(0, false))
    val state: StateFlow<ConsentSnapshot> = mutableState.asStateFlow()
    @Synchronized fun decide(accepted: Boolean): ConsentSnapshot {
        val next = ConsentSnapshot(mutableState.value.generation + 1, accepted)
        mutableState.value = next
        return next
    }
}

private val processConsentRegistry = ConsentRegistry()

class AmapPrivacyStateMachine {
    private var shown = false
    fun markShown() { shown = true }
    fun requireShown() = check(shown) { "Privacy policy must be shown before a decision" }
    fun decide(accepted: Boolean): Boolean { requireShown(); return accepted }
}

class AmapConsentToken private constructor(
    private val registry: ConsentRegistry,
    private val generation: Long,
) {
    val active: StateFlow<ConsentSnapshot> get() = registry.state
    fun isActive(snapshot: ConsentSnapshot = registry.state.value) = snapshot.active && snapshot.generation == generation
    fun validateActive() {
        check(isActive()) { "AMap consent is not active" }
    }
    companion object {
        internal fun issue(registry: ConsentRegistry, generation: Long) = AmapConsentToken(registry, generation)
    }
}

class AmapPrivacyGate internal constructor(
    private val context: Context,
    private val state: AmapPrivacyStateMachine,
    private val registry: ConsentRegistry,
) {
    fun reportPrivacyShown() {
        MapsInitializer.updatePrivacyShow(context, true, true)
        ServiceSettings.updatePrivacyShow(context, true, true)
        state.markShown()
    }
    fun reportUserDecision(accepted: Boolean): AmapConsentToken? {
        state.requireShown()
        MapsInitializer.updatePrivacyAgree(context, accepted)
        ServiceSettings.updatePrivacyAgree(context, accepted)
        val snapshot = registry.decide(accepted)
        return if (accepted) AmapConsentToken.issue(registry, snapshot.generation) else null
    }
    companion object { fun create(context: Context) = AmapPrivacyGate(context.applicationContext, AmapPrivacyStateMachine(), processConsentRegistry) }
}

internal class TestConsentGate(
    private val registry: ConsentRegistry = ConsentRegistry(),
    private val state: AmapPrivacyStateMachine = AmapPrivacyStateMachine(),
) {
    fun show() = state.markShown()
    fun decide(value: Boolean): AmapConsentToken? {
        state.requireShown()
        val snapshot = registry.decide(value)
        return if (value) AmapConsentToken.issue(registry, snapshot.generation) else null
    }
}
