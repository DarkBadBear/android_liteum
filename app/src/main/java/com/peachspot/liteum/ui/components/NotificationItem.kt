package com.peachspot.liteum.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.peachspot.liteum.data.db.NotificationEntity // NotificationEntity 경로에 맞게 수정 필요
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotificationItem(notification: NotificationEntity, modifier: Modifier = Modifier) {
    val dateFormat = remember { SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault()) }
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()) {
            notification.imgUrl?.takeIf { it.isNotBlank() }?.let { imageUrl ->
                AsyncImage(
                    model = imageUrl,
                    contentDescription = notification.title ?: "알림 이미지",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            notification.title?.let { title ->
                Text(text = title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(4.dp))
            }
            notification.body?.let { body ->
                Text(text = body, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
            }
            Text(text = dateFormat.format(Date(notification.receivedTimeMillis)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
fun NotificationItemPreview() {
    MaterialTheme {
        NotificationItem(
            notification = NotificationEntity(
                id = 1,
                title = "새로운 알림",
                body = "이것은 미리보기 알림 내용입니다. 중요한 정보를 포함하고 있을 수 있습니다.",
                receivedTimeMillis = System.currentTimeMillis(),
                imgUrl = "https://picsum.photos/seed/notification/400/200"
            )
        )
    }
}
    