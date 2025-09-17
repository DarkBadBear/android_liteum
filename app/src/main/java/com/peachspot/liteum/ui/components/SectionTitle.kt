package com.peachspot.liteum.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SectionTitle(
    text: String,
    @DrawableRes iconResId: Int? = null, // Drawable 리소스 ID를 받는 매개변수 추가
    imageVectorIcon: ImageVector? = null, // 기존 ImageVector 방식도 유지하려면 추가
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.primary // 아이콘 틴트색을 파라미터로 받을 수 있게 함
) {
    Row(
        modifier = modifier.padding(bottom = 0.dp, top = 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 우선순위: iconResId > imageVectorIcon
        val painter = iconResId?.let { painterResource(id = it) }

        if (painter != null) {
            Icon(
                painter = painter,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp) // 아이콘 크기 조절
            )
        } else if (imageVectorIcon != null) {
            Icon(
                imageVector = imageVectorIcon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp) // 아이콘 크기 조절
            )
        }

        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// SectionTitle 사용 예시:
// ...