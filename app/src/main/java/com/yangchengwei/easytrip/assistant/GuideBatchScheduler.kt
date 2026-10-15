package com.yangchengwei.easytrip.assistant

import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/** Shared by guide parsers only; map pacing and other model operations remain independent. */
class GuideBatchScheduler {
    private val slots = Semaphore(2)
    suspend fun <T> withSlot(block: suspend () -> T): T = slots.withPermit { block() }
}
