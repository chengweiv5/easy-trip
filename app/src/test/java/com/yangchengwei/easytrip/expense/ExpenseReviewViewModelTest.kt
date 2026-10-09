package com.yangchengwei.easytrip.expense

import com.yangchengwei.easytrip.expense.ui.*
import java.time.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ExpenseReviewViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val clock = Clock.fixed(Instant.parse("2026-10-09T00:00:00Z"), ZoneOffset.UTC)
    @Before fun setup() = Dispatchers.setMain(dispatcher)
    @After fun cleanup() = Dispatchers.resetMain()

    @Test fun `first visit is current year and back preserves year month and category`() = runTest(dispatcher) {
        val store = Store()
        val model = ExpenseReviewViewModel(store, clock)
        advanceUntilIdle()
        assertEquals(ExpensePeriod.Year(2026), model.state.value.scope.period)
        model.selectPeriod(ExpensePeriod.Year(2025))
        model.selectPeriod(ExpensePeriod.Month(YearMonth.of(2025, 4)))
        model.selectCategory(ExpenseCategory.FOOD)
        assertEquals(ExpenseCategory.FOOD, model.state.value.scope.category)
        model.back()
        assertEquals(ExpensePeriod.Month(YearMonth.of(2025, 4)), model.state.value.scope.period)
        model.back()
        assertEquals(ExpensePeriod.Year(2025), model.state.value.scope.period)
    }

    @Test fun `single record edit keeps scope and failure retains draft until retry succeeds`() = runTest(dispatcher) {
        val store = Store()
        val record = ExpenseRecord(ExpenseKey(ExpenseSourceKind.PLACE, "food"), "trip", "day", "item", "酒店", LocalDate.of(2025,4,12), 6800, ExpenseCategory.FOOD, null)
        store.records.value = listOf(record)
        val model = ExpenseReviewViewModel(store, clock)
        advanceUntilIdle()
        model.selectPeriod(ExpensePeriod.Year(2025))
        model.selectCategory(ExpenseCategory.FOOD)
        model.editRecord(record)
        model.updateEditor(model.state.value.editor!!.row.copy(amount = "100", category = ExpenseCategory.LODGING))
        store.failWrite = true
        model.saveEditor(); advanceUntilIdle()
        assertEquals("100", model.state.value.editor?.row?.amount)
        assertNotNull(model.state.value.editor?.error)
        assertEquals(6800L, store.records.value.single().cents)
        store.failWrite = false
        model.saveEditor(); advanceUntilIdle()
        assertNull(model.state.value.editor)
        assertEquals(ExpenseCategory.FOOD, model.state.value.scope.category)
        assertTrue(model.state.value.scope.select(store.records.value).isEmpty())
        assertEquals(10000L, store.records.value.single().cents)
    }

    @Test fun `read failure and overflow never look like empty zero and retry retains scope`() = runTest(dispatcher) {
        val store = Store()
        store.failRead = true
        val model = ExpenseReviewViewModel(store, clock)
        model.selectPeriod(ExpensePeriod.Year(2025))
        advanceUntilIdle()
        assertTrue(model.state.value.load is ExpenseLoadState.Failed)
        store.failRead = false
        store.records.value = listOf(record("max", Long.MAX_VALUE), record("one", 1))
        model.retry(); advanceUntilIdle()
        assertTrue((model.state.value.load as ExpenseLoadState.Failed).message.contains("无法统计"))
        store.records.value = listOf(record("zero", 0))
        model.retry(); advanceUntilIdle()
        assertEquals(ExpensePeriod.Year(2025), model.state.value.scope.period)
        val ready = model.state.value.load as ExpenseLoadState.Ready
        assertEquals(ExpenseTotal(0, 1, 1), reviewExpenses(ready.records, ExpensePeriod.All))
    }

    @Test fun `undated records and whole trip selection do not pollute prior year scope`() = runTest(dispatcher) {
        val store = Store()
        store.records.value = listOf(record("dated", 30000), record("undated", 48650).copy(date = null, tripId = "other"), record("next", 70000).copy(date = LocalDate.of(2026,1,1)))
        val model = ExpenseReviewViewModel(store, clock)
        advanceUntilIdle()
        model.selectPeriod(ExpensePeriod.Year(2025)); model.selectTrip("trip")
        model.showWholeTrip()
        assertEquals(100000L, reviewExpenses(model.state.value.scope.select(store.records.value), ExpensePeriod.All).cents)
        model.back(); model.back()
        assertEquals(ExpensePeriod.Year(2025), model.state.value.scope.period)
        assertNull(model.state.value.scope.tripId)
        model.selectPeriod(ExpensePeriod.Undated)
        assertEquals(48650L, reviewExpenses(model.state.value.scope.select(store.records.value), ExpensePeriod.All).cents)
    }

    @Test fun `zero record deletion requires explicit confirmation and cancellation leaves record`() = runTest(dispatcher) {
        val store = Store(); store.records.value = listOf(record("zero", 0))
        val model = ExpenseReviewViewModel(store, clock); advanceUntilIdle()
        model.editRecord(store.records.value.single()); model.deleteEditor(); advanceUntilIdle()
        assertEquals(1, store.records.value.size)
        model.requestDelete(); model.keepEditor(); model.deleteEditor(); advanceUntilIdle()
        assertEquals(1, store.records.value.size)
        model.requestDelete(); model.deleteEditor(); advanceUntilIdle()
        assertTrue(store.records.value.isEmpty())
        assertNull(model.state.value.editor)
    }

    @Test fun `reentering trip destination preserves category history and scroll`() = runTest(dispatcher) {
        val model = ExpenseReviewViewModel(Store(), clock)
        model.openTrip("trip", "day")
        model.selectCategory(ExpenseCategory.FOOD)
        model.rememberScroll(model.state.value.scope, 420)
        model.openTrip("trip", "day")
        assertEquals(ExpenseCategory.FOOD, model.state.value.scope.category)
        assertEquals(420, model.scrollOffset(model.state.value.scope))
        assertTrue(model.back())
        assertEquals("day", model.state.value.scope.dayId)
    }

    private fun record(id: String, cents: Long) = ExpenseRecord(ExpenseKey(ExpenseSourceKind.PLACE,id), "trip", "day", "item", "酒店", LocalDate.of(2025,12,31), cents, ExpenseCategory.FOOD, null)

    private class Store : ExpenseRepository {
        var failRead = false
        var failWrite = false
        val records = MutableStateFlow<List<ExpenseRecord>>(emptyList())
        override fun observeRecords(): Flow<List<ExpenseRecord>> = flow { check(!failRead) { "读取失败" }; emitAll(records) }
        override suspend fun updatePlaceExpense(itemId: String, expected: ExpenseRecord, value: PlaceExpenseInput) {
            check(!failWrite) { "保存失败" }
            records.value = records.value.map { if (it.key == expected.key) it.copy(cents=value.cents, category=value.category, note=value.note) else it }
        }
        override suspend fun deletePlaceExpense(itemId: String, expected: ExpenseRecord) { records.value = records.value.filterNot { it.key == expected.key } }
    }
}
