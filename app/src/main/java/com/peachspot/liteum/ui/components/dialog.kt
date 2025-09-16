package com.peachspot.liteum.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun ConfirmationDialog(
    title: String,
    text: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmButtonText: String = "확인", // 기본값 설정
    dismissButtonText: String = "취소" // 기본값 설정
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) }, // 필요하다면 Column + verticalScroll 추가
        confirmButton = {
            TextButton(onClick = {
                onConfirm()
                onDismiss() // 일반적으로 확인 후 다이얼로그 닫음
            }) {
                Text(confirmButtonText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissButtonText)
            }
        }
    )
}
