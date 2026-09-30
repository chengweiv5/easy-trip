package com.yangchengwei.easytrip.expense

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ExpenseRemovalDialog(prompter: ExpenseRemovalPrompter) {
    val pending by prompter.pending.collectAsStateWithLifecycle()
    pending?.let { request ->
        AlertDialog(onDismissRequest = { prompter.answer(request, false) },
            title = { Text("这次调整会删除已记花费") },
            text = { Text("将删除 ${request.entries.size} 笔已记花费，合计 ${formatExpense(expenseSummary(request.entries.map { it.cents }).cents)}。相关交通路线会重新生成，原花费不会转移到新路线。是否继续？") },
            confirmButton = { TextButton(onClick = { prompter.answer(request, true) }) { Text("删除花费并继续") } },
            dismissButton = { TextButton(onClick = { prompter.answer(request, false) }) { Text("取消调整") } })
    }
}
