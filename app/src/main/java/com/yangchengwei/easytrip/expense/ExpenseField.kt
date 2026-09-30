package com.yangchengwei.easytrip.expense

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun ExpenseField(value: String, onValueChange: (String) -> Unit, enabled: Boolean) {
    OutlinedTextField(value, onValueChange, Modifier.fillMaxWidth().testTag("expense-input"),
        label = { Text("花费（元）") }, placeholder = { Text("选填，实际花费") },
        supportingText = { Text(if (validExpense(value)) "留空表示未记录，最多两位小数" else "请输入非负金额，最多两位小数") },
        isError = !validExpense(value), enabled = enabled, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
}
