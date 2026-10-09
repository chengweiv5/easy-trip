package com.yangchengwei.easytrip.expense

import java.time.LocalDate
import java.time.YearMonth

/** Exact identity, source, amount and dates bind a confirmation to one transition. */
data class ExpensePeriodImpact(
    val key: ExpenseKey,
    val tripId: String,
    val sourceId: String,
    val sourceName: String,
    val cents: Long,
    val beforeDate: LocalDate?,
    val afterDate: LocalDate?,
    val beforeDayId: String,
    val afterDayId: String,
)

fun expensePeriodImpact(before: List<ExpenseRecord>, after: List<ExpenseRecord>): List<ExpensePeriodImpact> {
    val next = after.associateBy { it.key }
    return before.mapNotNull { old ->
        val new = next[old.key] ?: return@mapNotNull null
        if (old.date?.let(YearMonth::from) == new.date?.let(YearMonth::from)) null
        else ExpensePeriodImpact(old.key, old.tripId, old.sourceId, old.sourceName, old.cents,
            old.date, new.date, old.dayId, new.dayId)
    }
}

data class ExpensePeriodPreview(
    val changes: List<ExpensePeriodImpact>,
    val before: List<ExpenseRecord>,
    val after: List<ExpenseRecord>,
)
class ExpensePeriodRequired(val preview: ExpensePeriodPreview) : IllegalStateException("花费年月归属将变化，请先确认")
typealias ConfirmExpensePeriod = suspend (ExpensePeriodPreview) -> Boolean

class ExpensePeriodPrompter {
    data class Request(val preview: ExpensePeriodPreview, val answer: kotlinx.coroutines.CompletableDeferred<Boolean>)
    private val mutex = kotlinx.coroutines.sync.Mutex()
    private val mutable = kotlinx.coroutines.flow.MutableStateFlow<Request?>(null)
    val pending: kotlinx.coroutines.flow.StateFlow<Request?> = mutable
    suspend fun confirm(preview: ExpensePeriodPreview): Boolean {
        mutex.lock()
        val request = Request(preview, kotlinx.coroutines.CompletableDeferred())
        try {
            mutable.value = request
            return request.answer.await()
        } finally {
            if (mutable.value === request) mutable.value = null
            mutex.unlock()
        }
    }
    fun answer(request: Request, confirmed: Boolean) { if (mutable.value === request) request.answer.complete(confirmed) }
}
