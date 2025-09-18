package com.peachspot.liteum.ui.components

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.peachspot.liteum.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyDatePickerDialog(
    datePickerState: DatePickerState,
    onDismiss: () -> Unit,
    onDateSelected: (Long?) -> Unit,
    confirmEnabled: Boolean
) {
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onDateSelected(datePickerState.selectedDateMillis)
                },
                enabled = confirmEnabled
            ) {
                Text(stringResource(R.string.button_confirm)) // 문자열 리소스 확인
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.button_cancel)) // 문자열 리소스 확인
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}
