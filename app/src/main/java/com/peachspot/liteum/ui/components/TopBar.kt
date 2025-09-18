package com.peachspot.liteum.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.peachspot.liteum.R // R 경로 확인 필요
import com.peachspot.liteum.ui.screens.ViewMode
import androidx.navigation.NavController // NavController import 추가

// ...
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBar(
    currentViewMode: ViewMode,             // 매개변수 추가
    onViewModeChange: (ViewMode) -> Unit,
    onCameraClick: () -> Unit,
    onDmClick: () -> Unit,
    onMenuClick: (() -> Unit)?,

) {
    TopAppBar(
        title = {
            // 앱 로고 등을 여기에 추가할 수 있습니다.
            // Text(stringResource(R.string.app_name))
        },
        navigationIcon = {
            onMenuClick?.let { clickAction ->
                IconButton(onClick = clickAction) {
                    Icon(Icons.Filled.Menu, contentDescription = "메뉴")
                }
            }
        },
        actions = {
            IconButton(onClick = {
                val newMode  = if (currentViewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
                onViewModeChange(newMode)
            }) {
                Icon(
                if (currentViewMode == ViewMode.LIST) painterResource(id = R.drawable.ic_grid) else painterResource(id = R.drawable.ic_list), // 적절한 아이콘 사용
                    contentDescription = "뷰 모드 변경",
                    modifier = Modifier.size(24.dp)
                )
            }

            IconButton(onClick = onCameraClick) { // onCameraClick을 직접 호출
                Icon(Icons.Filled.Add, contentDescription = "새 게시물")
            }

        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}
    