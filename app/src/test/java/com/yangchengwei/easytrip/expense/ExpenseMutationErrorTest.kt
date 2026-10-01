package com.yangchengwei.easytrip.expense

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExpenseMutationErrorTest {
    @Test fun declinedConfirmationHasNoUserFacingErrorOrFallback() {
        assertNull(ExpenseRemovalCancelled().expenseMutationErrorOrNull("操作失败"))
    }

    @Test fun genuineFailuresRetainTheirMessagesAndFallback() {
        assertEquals("保存失败", IllegalStateException("保存失败").expenseMutationErrorOrNull("操作失败"))
        assertEquals("操作失败", IllegalStateException().expenseMutationErrorOrNull("操作失败"))
    }

    @Test fun missingConsentIsNotSilencedAsUserCancellation() {
        val required = ExpenseRemovalRequired(listOf(RecordedExpense("地点", "place", 1200)))
        assertEquals("操作将删除已记录花费，请先确认", required.expenseMutationErrorOrNull("操作失败"))
    }
}
