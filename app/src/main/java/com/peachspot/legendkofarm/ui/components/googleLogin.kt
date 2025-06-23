package com.peachspot.legendkofarm.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.peachspot.legendkofarm.util.Logger
import com.peachspot.legendkofarm.viewmodel.HomeUiState


@Composable
fun LoggedInUserProfile(uiState: HomeUiState, onSignOutClicked: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Logger.d("NICAP", uiState.toString());
        /*uiState.userPhotoUrl?.let {
            AsyncImage(
                model = it,
                contentDescription = "User Profile Photo",
                modifier = Modifier.size(100.dp)
            )
            Spacer(Modifier.height(16.dp))
        }
        uiState.userName?.let { Text("이름: $it", style = MaterialTheme.typography.headlineSmall) }
        uiState.userEmail?.let { Text("이메일: $it", style = MaterialTheme.typography.bodyLarge) }
        Spacer(Modifier.height(24.dp))*/

        Button(onClick = onSignOutClicked) { Text("Google 로그아웃") }
    }
}

@Composable
fun LoginPrompt(onSignInClicked: () -> Unit, enabled: Boolean = true) { // enabled 파라미터 추가 및 기본값 설정
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onSignInClicked,
            enabled = enabled // 버튼 활성화 상태를 파라미터로 제어
        ) {
            Text("Google 연동")
        }
    }
}
