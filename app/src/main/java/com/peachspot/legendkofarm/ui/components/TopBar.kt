package com.peachspot.legendkofarm.ui.components



//import androidx.compose.ui.graphics.Color // 직접 색상 지정 대신 MaterialTheme 사용 권장
import android.view.SoundEffectConstants
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView


// MyAppTopBar는 주로 뒤로가기 버튼 등이 있는 일반적인 상단 바에 사용될 수 있습니다.
// 현재 코드에서는 MyScreenWithSidebar에서 직접 사용되지 않으므로, 필요에 따라 수정하거나 유지합니다.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyAppTopBar(
    title: String = "",
     onNavIconClick: (() -> Unit)? = null, // 필요하다면 네비게이션 아이콘 클릭 콜백 추가
    onNotificationClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    onTitleClick: () -> Unit // 👉 타이틀 클릭 콜백 추가
) {

    val view = LocalView.current
    TopAppBar(
        title = { Text(
            text = title,
            modifier = Modifier.clickable {
                view.playSoundEffect(SoundEffectConstants.CLICK)
                onTitleClick() // 👉 타이틀 클릭 시 호출
            }
        ) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFFFFFFFF)// MaterialTheme 색상 사용 예시
            // titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
         navigationIcon = { // 예시: 뒤로가기 버튼
             if (onNavIconClick != null) {
                 IconButton(onClick = onNavIconClick) {
                     Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로 가기")
                 }
             }
         },
        actions = {
            IconButton(onClick = {
                view.playSoundEffect(SoundEffectConstants.CLICK)
                onNotificationClick()
            }) {
                Icon(Icons.Default.Notifications, contentDescription = "알림")
            }

        },
        modifier = modifier
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopAppBar(
    title: String = "",
    onMenuClick: () -> Unit = {}, // 이 부분이 중요!
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    TopAppBar(
        title = { Text(text = title) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFFf2f3f9)// MaterialTheme 색상 사용 예시
            // titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        navigationIcon = { // 메뉴 아이콘

        },
        actions = { // 알림 아이콘
            IconButton(onClick = {
                view.playSoundEffect(SoundEffectConstants.CLICK)
                onMenuClick() // 전달받은 onMenuClick 함수 실행
            }) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = "네비게이션 메뉴 열기"
                )
            }
        },
        modifier = modifier
    )
}

