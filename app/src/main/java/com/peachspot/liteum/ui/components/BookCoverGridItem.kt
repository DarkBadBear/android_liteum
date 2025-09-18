// com/peachspot/liteum/ui/components/BookCoverGridItem.kt (예시 경로)
package com.peachspot.liteum.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage // 또는 다른 이미지 로딩 라이브러리
import com.peachspot.liteum.data.model.FeedItem

@Composable
fun BookCoverGridItem(
    feedItem: FeedItem,
    onItemClick: (FeedItem) -> Unit,
    modifier: Modifier = Modifier
) {
    AsyncImage(
        model = feedItem.postImageUrl, // 책 표지 이미지 URL
        contentDescription = feedItem.bookTitle,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .aspectRatio(3f / 4f) // 책 표지 비율에 맞게 조절 (예시)
            .clickable { onItemClick(feedItem) }
    )
}