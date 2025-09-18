//package com.peachspot.liteum.ui.components;
//
//import androidx.compose.runtime.Composable;
//import androidx.compose.ui.tooling.preview.Preview;// Preview를 위한 간단한 사용 예시
//@Preview(showBackground = true)
//@Composable
//fun DateInputTextFieldPreview() {
//    var showDialog by remember { mutableStateOf(false) }
//    var selectedDateText by remember { mutableStateOf("선택 안함") }
//
//    Column {
//        DateInputTextField(
//                label = "시작일",
//                date = selectedDateText,
//                onClick = {
//                        showDialog = true
//                        println("DateInputTextField clicked!") // 로그 확인
//                },
//                modifier = Modifier.fillMaxWidth()
//        )
//        if (showDialog) {
//            AlertDialog(
//                    onDismissRequest = { showDialog = false },
//                    title = { Text("날짜 선택") },
//                    text = { Text("달력이 여기에 표시됩니다.") },
//                    confirmButton = {
//                            TextButton(onClick = {
//                                    selectedDateText = "2024-07-31" // 예시 날짜
//                                    showDialog = false
//                            }) { Text("확인") }
//                    }
//            )
//        }
//    }
//}
