package com.yangchengwei.easytrip.expense

import androidx.room.withTransaction
import androidx.room.RoomDatabase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

/** Snapshot binds consent to exact recorded entries, not to a reusable destructive flag. */
data class RecordedExpense(val kind: String, val id: String, val cents: Long)
class ExpenseRemovalRequired(val entries: List<RecordedExpense>) : IllegalStateException("操作将删除已记录花费，请先确认")
private class ExpenseConsent(val entries: Set<RecordedExpense>) : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<ExpenseConsent>
}
typealias ConfirmExpenseRemoval = suspend (List<RecordedExpense>) -> Boolean

suspend fun requireExpenseRemovalConsent(entries: List<RecordedExpense>) {
    val recorded = entries.distinct()
    if (recorded.isNotEmpty() && !coroutineContext[ExpenseConsent]?.entries.orEmpty().containsAll(recorded)) {
        throw ExpenseRemovalRequired(recorded)
    }
}

/** Roll back before prompting; retry against fresh data. A changed amount requires fresh consent. */
suspend fun <T> expenseTransaction(
    db: RoomDatabase,
    confirm: ConfirmExpenseRemoval?,
    block: suspend () -> T,
): T {
    if (coroutineContext[ExpenseConsent] != null) return db.withTransaction { block() }
    var approved = emptySet<RecordedExpense>()
    while (true) {
        try {
            return withContext(ExpenseConsent(approved)) { db.withTransaction { block() } }
        } catch (required: ExpenseRemovalRequired) {
            if (confirm == null || !confirm(required.entries)) throw IllegalStateException("已取消，行程和花费未改变")
            approved = approved + required.entries
        }
    }
}

class ExpenseRemovalPrompter {
    data class Request(val entries: List<RecordedExpense>, val answer: CompletableDeferred<Boolean>)
    private val mutex = Mutex()
    private val mutable = MutableStateFlow<Request?>(null)
    val pending = mutable.asStateFlow()
    suspend fun confirm(entries: List<RecordedExpense>): Boolean = mutex.withLock {
        val request = Request(entries, CompletableDeferred())
        mutable.value = request
        try { request.answer.await() } finally { if (mutable.value === request) mutable.value = null }
    }
    fun answer(request: Request, confirmed: Boolean) { if (mutable.value === request) request.answer.complete(confirmed) }
}
