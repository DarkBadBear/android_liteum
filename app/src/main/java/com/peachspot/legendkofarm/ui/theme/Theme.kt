package com.peachspot.legendkofarm.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = Color(0xFF1A1A1A),
    onBackground = Color.White,
    surface = Color(0x33FFFFFF),
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = Color.White,
    surface = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black
)

@Composable
fun legendkofarmTheme(
    darkTheme: Boolean = true, // 강제 다크 모드
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            // dynamicColor가 활성화되면 darkTheme이 true라도 시스템 설정을 따릅니다.
            // 여기서는 darkTheme이 true로 고정되어 있으므로 이 분기는 사용되지 않습니다.
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // 상태바 배경색을 상단 바 색상과 동일하게 설정 (예: 0xFF535353)
            // 또는 앱의 기본 배경색 (DarkColorScheme의 background 또는 primary)과 일치시켜도 좋습니다.
            window.statusBarColor = Color(0xFF535353).toArgb() // TopAppBar 배경색과 동일하게 설정
            // 상태바 아이콘과 텍스트를 흰색으로 설정
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false // 이 값을 false로 변경
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}