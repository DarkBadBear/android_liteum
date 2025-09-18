package com.peachspot.liteum.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateInputTextField(
    label: String,
    date: String, // 화면에 표시될 날짜 문자열
    onClick: () -> Unit, // 이 콜백이 실행되어야 함
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() }, // 클릭 시 시각적 효과 (선택 사항)
                indication = null, // 기본 물결 효과 제거 (선택 사항, 커스텀 효과 원하면 추가)
                onClick = onClick // Box가 클릭되면 전달된 onClick 실행
            )
    ) {
        OutlinedTextField(
            value = date,
            onValueChange = { /* 아무것도 안 함, Box가 클릭을 처리 */ },
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(), // Box의 크기에 맞게 TextField 채우기
            readOnly = true, // 키보드 올라오지 않도록
            // TextField가 포커스를 받지 않도록 enabled = false 처리
            // 이렇게 하면 OutlinedTextField의 기본 클릭/포커스 로직이 비활성화됨
            enabled = false,
            // enabled = false 시 기본적으로 색상이 비활성화 상태로 보이므로,
            // 일반 상태와 유사하게 보이도록 색상 커스터마이징
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledContainerColor = MaterialTheme.colorScheme.surface, // 배경색
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                // 필요에 따라 다른 상태의 색상도 조절 가능
            )
        )
    }
}

