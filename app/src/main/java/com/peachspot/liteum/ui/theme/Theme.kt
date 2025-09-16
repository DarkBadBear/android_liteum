package com.peachspot.liteum.ui.theme

import android.app.Activity // Activity 임포트
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect // SideEffect 임포트
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView // LocalView 임포트
import androidx.core.view.WindowCompat // WindowCompat 임포트

// Color.kt 파일에 정의되어 있어야 할 색상들 (예시)
// private val Purple80 = Color(0xFFD0BCFF)
// private val PurpleGrey80 = Color(0xFFCCC2DC)
// private val Pink80 = Color(0xFFEFB8C8)
// private val Purple40 = Color(0xFF6650a4)
// private val PurpleGrey40 = Color(0xFF625b71)
// private val Pink40 = Color(0xFF7D5260)

// Type.kt 파일에 정의되어 있어야 할 Typography (예시)
// val Typography = Typography(...)

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
    /* Other colors for dark theme if needed */
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun liteumiTheme( // Composable 함수명은 PascalCase (대문자로 시작)
    useDarkTheme: Boolean = isSystemInDarkTheme(), // 더 명확한 파라미터 이름
    // Dynamic color is available on Android 12+
    useDynamicColor: Boolean = true, // 더 명확한 파라미터 이름
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        useDarkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current // 현재 뷰 가져오기
    if (!view.isInEditMode) { // 미리보기 모드가 아닐 때만 적용
        SideEffect { // Composable이 구성될 때마다 실행
            val window = (view.context as Activity).window

            // 앱 콘텐츠가 시스템 UI(상태바, 내비게이션바) 영역까지 확장되도록 설정합니다.
            // enableEdgeToEdge()를 호출했으므로 이 설정은 계속 유효합니다.
            WindowCompat.setDecorFitsSystemWindows(window, false)

            // !!! 중요: window.statusBarColor 설정은 더 이상 필요 없으며,
            // Android 15에서 지원 중단된 API를 사용합니다.
            // 대신 MaterialTheme의 colorScheme과 Scaffold가 상태 바 색상을 처리합니다.

            // 상태 바 아이콘 (시간, 와이파이 등)이 잘 보이도록 색상 설정
            // useDarkTheme가 true (어두운 테마)면 밝은 아이콘, useDarkTheme가 false (밝은 테마)면 어두운 아이콘
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !useDarkTheme

            // (선택 사항) 내비게이션 바도 투명하게 설정하고 싶다면
            // WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !useDarkTheme
            // 위 설정을 통해 내비게이션 바 아이콘 색상만 제어하고, 실제 바의 배경은 투명하게 됩니다.
            // navigationBarColor를 직접 설정하는 것은 대부분의 경우 필요하지 않습니다.
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Typography는 Type.kt 파일에 정의되어 있어야 합니다.
        content = content
    )
}