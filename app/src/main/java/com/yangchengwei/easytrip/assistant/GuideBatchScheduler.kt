package com.yangchengwei.easytrip.assistant

import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/** Process-wide extraction budget; map pacing and semantic resolution remain independent. */
class GuideBatchScheduler {
    companion object { private val slots = Semaphore(2); val shared = GuideBatchScheduler() }
    suspend fun <T> withSlot(block: suspend () -> T): T = slots.withPermit { block() }
}
