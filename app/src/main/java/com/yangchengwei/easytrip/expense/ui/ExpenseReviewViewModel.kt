package com.yangchengwei.easytrip.expense.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yangchengwei.easytrip.expense.*
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ExpenseScope(
    val period: ExpensePeriod,
    val tripId: String? = null,
    val dayId: String? = null,
    val category: ExpenseCategory? = null,
    val unclassified: Boolean = false,
)

sealed interface ExpenseLoadState {
    data object Loading : ExpenseLoadState
    data class Ready(val records: List<ExpenseRecord>) : ExpenseLoadState
    data class Failed(val message: String) : ExpenseLoadState
}

data class ExpenseReviewUiState(
    val scope: ExpenseScope,
    val load: ExpenseLoadState = ExpenseLoadState.Loading,
    val canGoBack: Boolean = false,
    val editor: ExpenseRecordDraft? = null,
)

data class ExpenseRecordDraft(
    val expected: ExpenseRecord,
    val row: ExpenseDraftRow = ExpenseDraftRow("record", expected.key.id, expenseInput(expected.cents), expected.category, expected.note.orEmpty()),
    val saving: Boolean = false,
    val error: String? = null,
    val confirmDiscard: Boolean = false,
    val confirmDelete: Boolean = false,
) {
    val original get() = PlaceExpenseInput(expected.key.id, expected.cents, expected.category, expected.note)
    val validation get() = validateExpenseDraft(listOf(row), listOf(original))
    val dirty get() = row != ExpenseRecordDraft(expected).row
}

class ExpenseReviewViewModel(
    private val repository: ExpenseRepository,
    clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {
    private val mutable = MutableStateFlow(ExpenseReviewUiState(ExpenseScope(ExpensePeriod.Year(LocalDate.now(clock).year))))
    val state = mutable.asStateFlow()
    private val history = mutableListOf<ExpenseScope>()
    private var read: Job? = null
    private var initialTrip: Pair<String, String?>? = null
    private val scrollOffsets = mutableMapOf<ExpenseScope, Int>()
    fun scrollOffset(scope: ExpenseScope): Int = scrollOffsets[scope] ?: 0
    fun rememberScroll(scope: ExpenseScope, offset: Int) { scrollOffsets[scope] = offset }
    init { retry() }

    fun retry() {
        read?.cancel()
        mutable.update { it.copy(load = ExpenseLoadState.Loading) }
        read = viewModelScope.launch {
            try {
                repository.observeRecords().collect { records ->
                    reviewExpenses(records, ExpensePeriod.All)
                    mutable.update { it.copy(load = ExpenseLoadState.Ready(records.distinctBy { r -> r.key })) }
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                mutable.update { it.copy(load = ExpenseLoadState.Failed(if (error is ArithmeticException) "费用合计超出支持范围，无法统计" else error.message ?: "无法读取花费，请重试")) }
            }
        }
    }

    fun editRecord(record: ExpenseRecord) {
        if (record.key.kind != ExpenseSourceKind.PLACE || state.value.editor != null) return
        mutable.update { it.copy(editor = ExpenseRecordDraft(record)) }
    }
    fun updateEditor(row: ExpenseDraftRow) {
        val editor = state.value.editor ?: return
        if (!editor.saving && row.key == editor.row.key && row.savedId == editor.row.savedId)
            mutable.update { it.copy(editor = editor.copy(row = row, error = null)) }
    }
    fun closeEditor() {
        val editor = state.value.editor ?: return
        if (!editor.saving) mutable.update { it.copy(editor = if (editor.dirty) editor.copy(confirmDiscard = true) else null) }
    }
    fun discardEditor() { if (state.value.editor?.saving == false) mutable.update { it.copy(editor = null) } }
    fun keepEditor() { mutable.update { it.copy(editor = it.editor?.copy(confirmDiscard = false, confirmDelete = false)) } }
    fun requestDelete() { if (state.value.editor?.saving == false) mutable.update { it.copy(editor = it.editor?.copy(confirmDelete = true)) } }
    fun saveEditor() = writeEditor(delete = false)
    fun deleteEditor() { if (state.value.editor?.confirmDelete == true) writeEditor(delete = true) }
    private fun writeEditor(delete: Boolean) {
        val editor = state.value.editor ?: return
        if (editor.saving) return
        val validation = editor.validation
        if (!delete && validation !is ExpenseDraftValidation.Valid) return
        mutable.update { it.copy(editor = editor.copy(saving = true, error = null, confirmDelete = false)) }
        viewModelScope.launch {
            try {
                if (delete) repository.deletePlaceExpense(editor.expected.sourceId, editor.expected)
                else repository.updatePlaceExpense(editor.expected.sourceId, editor.expected,
                    (validation as ExpenseDraftValidation.Valid).expenses.single())
                mutable.update { it.copy(editor = null) }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                mutable.update { it.copy(editor = editor.copy(error = error.message ?: "保存失败，请重试", saving = false, confirmDelete = false)) }
            }
        }
    }

    fun selectPeriod(period: ExpensePeriod) {
        val current = state.value.scope
        val sameLevel = (current.period is ExpensePeriod.Year && period is ExpensePeriod.Year) ||
            (current.period is ExpensePeriod.Month && period is ExpensePeriod.Month)
        go(current.copy(period = period), push = !sameLevel)
    }
    fun shiftPeriod(delta: Int) {
        val current = state.value.scope
        val next = current.period.adjacent(delta) ?: return
        go(current.copy(period = next), push = false)
    }
    fun selectCategory(category: ExpenseCategory?) = go(state.value.scope.copy(category = category, unclassified = category == null))
    fun showWholeTrip() = go(state.value.scope.copy(period = ExpensePeriod.All, dayId = null, category = null, unclassified = false))
    fun selectTrip(tripId: String) = go(state.value.scope.copy(tripId = tripId))
    fun openTrip(tripId: String, dayId: String? = null) {
        if (initialTrip == tripId to dayId) return
        initialTrip = tripId to dayId
        history.clear()
        mutable.update { it.copy(scope = ExpenseScope(ExpensePeriod.All, tripId, dayId), canGoBack = false) }
    }
    fun back(): Boolean {
        if (history.isEmpty()) return false
        val previous = history.removeAt(history.lastIndex)
        mutable.update { it.copy(scope = previous, canGoBack = history.isNotEmpty()) }
        return true
    }
    class Factory(private val repository: ExpenseRepository) : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ExpenseReviewViewModel(repository) as T
    }
    private fun go(scope: ExpenseScope, push: Boolean = true) {
        if (scope == state.value.scope) return
        if (push) history.add(state.value.scope)
        mutable.update { it.copy(scope = scope, canGoBack = history.isNotEmpty()) }
    }
}

/** Adjacent navigation preserves granularity and never wraps the supported calendar bounds. */
fun ExpensePeriod.adjacent(delta: Int): ExpensePeriod? = when (this) {
    is ExpensePeriod.Year -> (value.toLong() + delta).takeIf {
        it in LocalDate.MIN.year.toLong()..LocalDate.MAX.year.toLong()
    }?.let { ExpensePeriod.Year(it.toInt()) }
    is ExpensePeriod.Month -> runCatching { ExpensePeriod.Month(value.plusMonths(delta.toLong())) }.getOrNull()
    ExpensePeriod.All, ExpensePeriod.Undated -> null
}

fun ExpenseScope.select(records: List<ExpenseRecord>): List<ExpenseRecord> = records.filter { record ->
    (tripId == null || record.tripId == tripId) && (dayId == null || record.dayId == dayId) &&
        (!unclassified || record.category == null) && (category == null || record.category == category) &&
        when (val p = period) {
            ExpensePeriod.All -> true
            ExpensePeriod.Undated -> record.date == null
            is ExpensePeriod.Year -> record.date?.year == p.value
            is ExpensePeriod.Month -> record.date?.let(YearMonth::from) == p.value
        }
}
