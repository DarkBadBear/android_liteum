package com.peachspot.liteum.ui.components



//import androidx.compose.ui.graphics.Color // 직접 색상 지정 대신 MaterialTheme 사용 권장
import android.view.SoundEffectConstants
import androidx.compose.foundation.layout.Row

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp

import androidx.compose.foundation.layout.Arrangement // 추가
import androidx.compose.foundation.layout.fillMaxWidth // 추가

// ... 다른 import 문들

// ...

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyAppTopBar(
    title: String = "",
    onNotificationClick: () -> Unit = {},
    onTitleClick: () -> Unit,
    onZoomInClick: () -> Unit,    // 확대 버튼 콜백 추가
    onZoomOutClick: () -> Unit,   // 축소 버튼 콜백 추가
    onMenuClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current

    TopAppBar(
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(), // 사용 가능한 전체 너비를 채우도록 설정
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center // 가로 방향으로 가운데 정렬
            ) {
                TextButton(onClick = onZoomInClick) {
                    Text(
                        text = "화면 확대",
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(2.dp)) // 버튼 사이 간격 추가 (원하는 크기로 조절)
                Text("/")
                Spacer(modifier = Modifier.width(2.dp)) // 버튼 사이 간격 추가 (원하는 크기로 조절)

                TextButton(onClick = onZoomOutClick) {
                    Text(
                        text = "축소",
                        color = Color.White
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFF2F2F2F).copy(alpha = 0.5f),
            titleContentColor = Color.White
        ),
        navigationIcon = {
            if (onMenuClick != null) {
                // IconButton의 기본 패딩을 줄이려면 contentPadding을 조절할 수 있습니다.
                // 또는 Modifier.size를 사용하여 아이콘 버튼 자체의 크기를 조절할 수도 있습니다.
                IconButton(onClick = onMenuClick) {
                    Icon(Icons.Default.Menu, contentDescription = "메뉴", tint = Color.White)
                }
            }
        },
        actions = {
            // IconButton의 기본 패딩을 줄이려면 contentPadding을 조절할 수 있습니다.
            IconButton(
                onClick = {
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                    onNotificationClick()
                },
                modifier = Modifier.fillMaxHeight() // fillMaxHeight()는 유지하거나 필요에 따라 조절
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "알림",
                    tint = Color.White
                )
            }
        },
        modifier = modifier.height(80.dp)
    )
}

