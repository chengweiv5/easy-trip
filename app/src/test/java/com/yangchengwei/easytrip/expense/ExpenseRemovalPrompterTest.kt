package com.yangchengwei.easytrip.expense

import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ExpenseRemovalPrompterTest {
    @Test fun staleConfirmationCannotApproveNextQueuedOperation() = runTest {
        val prompter = ExpenseRemovalPrompter()
        val a = async { prompter.confirm(listOf(RecordedExpense("交通","a",100))) }
        val b = async { prompter.confirm(listOf(RecordedExpense("交通","b",200))) }
        runCurrent()
        val first = prompter.pending.value!!
        prompter.answer(first, false)
        runCurrent()
        assertFalse(a.await())
        val second = prompter.pending.value!!
        prompter.answer(first, true)
        runCurrent()
        assertFalse(b.isCompleted)
        assertSame(second, prompter.pending.value)
        prompter.answer(second, true)
        assertTrue(b.await())
        assertNull(prompter.pending.value)
    }
}
