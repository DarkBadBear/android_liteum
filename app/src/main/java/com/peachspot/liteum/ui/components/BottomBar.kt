package com.peachspot.liteum.ui.components // 실제 프로젝트 패키지 경로에 맞게 수정하세요

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home // 예시, 실제 사용하는 아이콘으로 변경
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person // 예시
import androidx.compose.material.icons.filled.Search // 예시
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.scale
import androidx.compose.ui.graphics.vector.ImageVector // ImageVector import 추가
import androidx.compose.ui.unit.dp
import com.peachspot.liteum.ui.screens.BottomNavItem

// 하단 바 아이템을 정의하는 enum 또는 sealed class
// 이 enum 클래스는 HomeScreen.kt 또는 다른 공용 파일에 남아있거나,
// AppBottomNavigationBar.kt 파일 내부 또는 별도의 model 파일로 이동할 수 있습니다.
// 여기서는 AppBottomNavigationBar.kt 내부에 두는 것으로 가정합니다.
// 만약 다른 곳에 있다면 해당 경로에서 import 해야 합니다.


@Composable
fun AppBottomNavigationBar(
    selectedItem: BottomNavItem,
    onItemSelected: (BottomNavItem) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier // 외부에서 전달된 Modifier를 먼저 적용
            .shadow( // 그림자 효과 추가
                elevation = 8.dp, // 그림자의 높이 (Dp 단위)
                // spotColor = Color.Gray, // 그림자 색상 (선택적, 기본값은 주변 색상에 따라 결정됨)
                // ambientColor = Color.Black // 그림자 색상 (선택적, 기본값은 주변 색상에 따라 결정됨)
                // clip = false // 기본적으로 Shape에 따라 그림자가 잘리지만, false로 하면 영역 밖으로도 그림자가 그려질 수 있음 (보통 NavigationBar에는 기본값 true 유지)
                // shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp) // 특정 모양으로 그림자를 주고 싶을 때 (선택적)
            ),
        containerColor =  Color(0xFFFFFFFF),//MaterialTheme.colorScheme.surfaceVariant, // 또는 surfaceVariant
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        BottomNavItem.values().forEach { item ->
            val isSelected = selectedItem == item
            NavigationBarItem(
                modifier = modifier.scale(if (isSelected) 1.1f else 1.0f), // 선택 시 10% 확대 (주의해서 사용)
                icon = { Icon(  painter = item.icon(), contentDescription = item.label,modifier = Modifier.size(24.dp))     },
                label = { Text(item.label) },
                selected = selectedItem == item,
                onClick = { onItemSelected(item) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor =  Color(0xCCCCCCCC),
                    selectedTextColor =  MaterialTheme.colorScheme.primary,
                    unselectedTextColor =  Color(0xCCCCCCCC),
                    indicatorColor =Color.Transparent /// MaterialTheme.colorScheme.secondaryContainer // 선택된 아이템 배경색
                )
            )
        }
    }
}
   