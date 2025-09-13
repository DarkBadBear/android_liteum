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
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyAppTopBar(
    title: String = "",
    onNotificationClick: () -> Unit = {},
    onTitleClick: () -> Unit,
    onMenuClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current

    // 오늘 날짜 가져오기
    val today = LocalDate.now()
    val month = today.monthValue
    val day = today.dayOfMonth
    val dayOfWeek = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.KOREAN) // "일요일"


    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onTitleClick) {
                    Text(
                        text = month.toString() + "월 " + day.toString() + "일 " + dayOfWeek,
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
                IconButton(onClick = onMenuClick) {
                    Icon(Icons.Default.Menu, contentDescription = "메뉴", tint = Color.White)
                }
            }
        },
        actions = {
            IconButton(
                onClick = {
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                    onNotificationClick()
                },
                modifier = Modifier.fillMaxHeight()
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
