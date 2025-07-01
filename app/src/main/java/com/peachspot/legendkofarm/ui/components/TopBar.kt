package com.peachspot.legendkofarm.ui.components



//import androidx.compose.ui.graphics.Color // 직접 색상 지정 대신 MaterialTheme 사용 권장
import android.view.SoundEffectConstants
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp


// MyAppTopBar는 주로 뒤로가기 버튼 등이 있는 일반적인 상단 바에 사용될 수 있습니다.
// 현재 코드에서는 MyScreenWithSidebar에서 직접 사용되지 않으므로, 필요에 따라 수정하거나 유지합니다.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyAppTopBar(
    title: String = "",
    onNavIconClick: (() -> Unit)? = null, // 필요하다면 네비게이션 아이콘 클릭 콜백 추가
    onNotificationClick: () -> Unit = {},
    onRefreshClicked: () -> Unit,
    modifier: Modifier = Modifier,
    onTitleClick: () -> Unit ,

) {

    val view = LocalView.current
    TopAppBar(
        title = {
            // 기존 title 코드 유지
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFF535353),
            titleContentColor = Color.White
        ),
        navigationIcon = {
            Row(
                verticalAlignment = Alignment.CenterVertically, // Row 내 자식 요소들을 수직 중앙 정렬
                modifier = Modifier.padding(start = 4.dp) // Row 전체의 시작 패딩 조절
            ) {
                // 뒤로가기 버튼 로직
                if (onNavIconClick != null) {
                    IconButton(onClick = onNavIconClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로 가기", tint = Color.White)
                    }
                }

                // 새로고침 버튼을 감싸는 Box
                Box(
                    modifier = Modifier
                        .padding(start = 0.dp) // 뒤로가기 버튼과의 간격 조절
                        .fillMaxHeight(), // <--- Box가 TopAppBar의 전체 높이를 채우도록 합니다.
                    contentAlignment = Alignment.CenterStart // <--- Box 내의 콘텐츠(TextButton)를 수직 중앙, 수평 시작점에 정렬합니다.
                ) {
                    TextButton(
                        onClick = onRefreshClicked,
                        modifier = Modifier
                            .height(36.dp) // TextButton 자체의 높이
                            .border(
                                width = 1.dp,
                                color = Color(0xFF999999),
                                shape = RoundedCornerShape(8.dp)
                            )
                        // .padding(5.dp) // 이 외부 패딩은 제거하거나 Row의 패딩으로 조절하는게 좋습니다.
                        ,
                        shape = RoundedCornerShape(8.dp),
                        // contentPadding을 조절하여 버튼 내용물의 상하 여백 확보 (예: 6.dp)
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = Color.Transparent
                        )
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "화면 새로고침",
                            tint = Color.White,
                            modifier = Modifier
                                // Icon과 Text 내부의 개별 패딩은 제거하거나 최소화하는 것이 좋습니다.
                                // .padding(top = 2.dp)
                                .size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "화면 새로고침",
                            color = Color.White,
                            // .padding(bottom = 0.dp)
                        )
                    }
                }
            }
        },
        actions = {
            // 기존 알림 버튼 코드
            IconButton(onClick = {
                view.playSoundEffect(SoundEffectConstants.CLICK)
                onNotificationClick()
            }) {
                Icon(Icons.Default.Notifications, contentDescription = "알림", tint = Color.White ,
                    modifier = Modifier.padding(top = 7.dp) // 알림 아이콘의 상단 패딩은 시각적으로 조절 필요
                )
            }
        },
        modifier = modifier.height(54.dp), // AppBar 전체 높이
        windowInsets = WindowInsets(0),
    )
//    TopAppBar(
//        title = {
////            Text( text = title,
////            modifier = Modifier.clickable {
////                view.playSoundEffect(SoundEffectConstants.CLICK)
////                onTitleClick()
////            }.padding(top = 12.dp)  // 상단 패딩만 12dp 주기
////
////        )
//                }
//        ,
//        colors = TopAppBarDefaults.topAppBarColors(
//            containerColor = Color(0xFF535353),// MaterialTheme 색상 사용 예시
//                    titleContentColor = Color.White
//        ),
//         navigationIcon = { // 예시: 뒤로가기 버튼
//             if (onNavIconClick != null) {
//                 IconButton(onClick = onNavIconClick) {
//                     Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로 가기",tint = Color.White )
//                 }
//             }
//         },
//        actions = {
//            Box(modifier = Modifier.padding(top = 3.dp)) {
//                TextButton(
//                onClick = onRefreshClicked,
//                    modifier = Modifier.height(36.dp)
//                    .border(
//                        width = 1.dp,
//                        color = Color(0xFF999999),
//                        shape = RoundedCornerShape(8.dp)
//                    )
//                    .padding(0.dp), // 테두리 안쪽 여유공간
//                shape = RoundedCornerShape(8.dp),
//                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
//                colors = ButtonDefaults.textButtonColors(
//                    containerColor = Color.Transparent // 배경 없으면 필요
//                )
//            ) {
//                Icon(
//                    Icons.Default.Refresh,
//                    contentDescription = "화면 새로고침",
//                    tint = Color.White,
//                    modifier = Modifier.padding(top = 2.dp).size(16.dp)
//                )
//                Spacer(modifier = Modifier.width(4.dp))
//                Text(
//                    "화면 새로고침", color = Color.White,
//                    modifier = Modifier.padding(bottom =0.dp)
//                ) // 상단 패딩만 12dp 주기)
//
//            }
//
//
//        }
//
//
//
//            IconButton(onClick = {
//                view.playSoundEffect(SoundEffectConstants.CLICK)
//                onNotificationClick()
//            }) {
//                Icon(Icons.Default.Notifications, contentDescription = "알림",tint = Color.White ,
//                    modifier = Modifier.padding(top = 7.dp)  // 상단 패딩만 12dp 주기
//                    )
//            }
//
//        },
//        modifier = modifier.height(54.dp),
//        windowInsets = WindowInsets(0), //
//    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopAppBar(
    title: String = "",
    onMenuClick: () -> Unit = {}, // 이 부분이 중요!
    modifier: Modifier = Modifier,
    onTitleClick: () -> Unit ,
) {
    val view = LocalView.current
    TopAppBar(

        title = {
            Text( text = title,
                modifier = Modifier.clickable {
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                    onTitleClick()
                }.padding(top = 12.dp)

        ) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFF535353),// MaterialTheme 색상 사용 예시
            titleContentColor = Color.White
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
                    contentDescription = "네비게이션 메뉴 열기",
                    modifier = Modifier.padding(top = 7.dp)  // 상단 패딩만 12dp 주기
                )
            }
        },
        modifier = modifier.height(54.dp),
        windowInsets = WindowInsets(0), //
    )
}

