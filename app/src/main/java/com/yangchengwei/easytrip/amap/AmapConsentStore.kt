package com.yangchengwei.easytrip.amap

import android.content.SharedPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface AmapConsentFact {
    val generation: Long

    data class Undecided(override val generation: Long) : AmapConsentFact

    data class Accepted(
        override val generation: Long,
        val token: AmapConsentToken,
    ) : AmapConsentFact

    data class Declined(override val generation: Long) : AmapConsentFact
}

data class AmapConsentState(
    val fact: AmapConsentFact,
    val updating: Boolean = false,
    val error: String? = null,
)

interface AmapConsentPersistence {
    fun readDecision(): Boolean?
    fun writeDecision(accepted: Boolean)
}

class SharedPreferencesAmapConsentPersistence(
    private val preferences: SharedPreferences,
) : AmapConsentPersistence {
    override fun readDecision(): Boolean? =
        if (preferences.contains(AMAP_ACCEPTED)) preferences.getBoolean(AMAP_ACCEPTED, false) else null

    override fun writeDecision(accepted: Boolean) {
        preferences.edit().putBoolean(AMAP_ACCEPTED, accepted).apply()
    }

    private companion object {
        const val AMAP_ACCEPTED = "amap.accepted"
    }
}

interface AmapPrivacyReporter {
    suspend fun reportShown()
    suspend fun reportDecision(accepted: Boolean)
}

internal class AmapConsentDecisionSupersededException : IllegalStateException("AMap consent decision superseded")

class AmapConsentStore(
    private val persistence: AmapConsentPersistence,
    private val reporter: AmapPrivacyReporter,
    private val registry: ConsentRegistry,
) {
    private val lock = Any()
    private var generation = 0L
    private val mutableState = MutableStateFlow(AmapConsentState(restoreFact()))
    val state: StateFlow<AmapConsentState> = mutableState.asStateFlow()
    private val mutableShown = MutableStateFlow(false)
    val shown: StateFlow<Boolean> = mutableShown.asStateFlow()
    private val shownMutex = Mutex()
    private val decisionMutex = Mutex()

    suspend fun reportShown(): Result<Unit> = shownMutex.withLock {
        if (mutableShown.value) return@withLock Result.success(Unit)
        try {
            reporter.reportShown()
            mutableShown.value = true
            Result.success(Unit)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Result.failure(error)
        }
    }

    suspend fun decide(accepted: Boolean): Result<Unit> = decisionMutex.withLock {
        val requestGeneration = synchronized(lock) {
            (++generation).also {
                mutableState.value = mutableState.value.copy(updating = true, error = null)
            }
        }
        try {
            reporter.reportDecision(accepted)
            synchronized(lock) {
                persistence.writeDecision(accepted)
                mutableState.value = AmapConsentState(createFact(accepted, requestGeneration))
            }
            Result.success(Unit)
        } catch (error: CancellationException) {
            synchronized(lock) {
                mutableState.value = mutableState.value.copy(updating = false, error = null)
            }
            throw error
        } catch (error: Throwable) {
            synchronized(lock) {
                mutableState.value = mutableState.value.copy(
                    updating = false,
                    error = "地图授权更新失败，请重试",
                )
            }
            Result.failure(error)
        }
    }

    private fun restoreFact(): AmapConsentFact = when (persistence.readDecision()) {
        null -> AmapConsentFact.Undecided(generation)
        true -> createFact(true, generation)
        false -> createFact(false, generation)
    }

    private fun createFact(accepted: Boolean, generation: Long): AmapConsentFact {
        val snapshot = registry.decide(accepted)
        return if (accepted) {
            AmapConsentFact.Accepted(generation, AmapConsentToken.issue(registry, snapshot.generation))
        } else {
            AmapConsentFact.Declined(generation)
        }
    }
}
