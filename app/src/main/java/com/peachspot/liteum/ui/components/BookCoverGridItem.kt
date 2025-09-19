// com/peachspot/liteum/ui/components/BookCoverGridItem.kt (예시 경로)
package com.peachspot.liteum.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage // 또는 다른 이미지 로딩 라이브러리
import coil.request.ImageRequest
import com.peachspot.liteum.data.model.FeedItem
import com.peachspot.liteum.viewmodel.FeedViewModel

@Composable
fun BookCoverGridItem(
    feedItem: FeedItem,
    onItemClick: () -> Unit, // 이제 FeedItem이 항상 non-null로 전달됨
    modifier: Modifier = Modifier,
    feedViewModel: FeedViewModel
) {
    Card(
        onClick = onItemClick,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.7f) // 책 표지 비율에 맞게 조절 (예시)
    ) {
        Column {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(feedItem.bookImageUrl)
                    .crossfade(true)
                    // .placeholder(R.drawable.placeholder_image) // 필요시 플레이스홀더 이미지
                    // .error(R.drawable.error_image) // 필요시 에러 이미지
                    .build(),
                contentDescription = feedItem.bookTitle,
                contentScale = ContentScale.Crop, // 또는 ContentScale.Fit 등
                modifier = Modifier
                    .weight(1f) // 이미지가 남은 공간을 채우도록
                    .fillMaxWidth()
            )
            // (선택 사항) 책 제목 등을 아래에 표시하고 싶다면 추가
            // Text(
            //    text = feedItem.bookTitle,
            //    style = MaterialTheme.typography.bodySmall,
            //    maxLines = 2,
            //    overflow = TextOverflow.Ellipsis,
            //    modifier = Modifier.padding(8.dp)
            // )
        }
    }
}
