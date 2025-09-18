package com.peachspot.liteum.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peachspot.liteum.data.model.BookReview // 분리된 BookReview 사용

@Composable
fun BookReviewItem(review: BookReview, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = review.reviewerName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "⭐️ ${"%.1f".format(review.rating)}", // 실제 별 아이콘 사용 권장
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = review.reviewText,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// BookReviewItem.kt

@Preview(showBackground = true)
@Composable
fun BookReviewItemPreview() {
    MaterialTheme {
        BookReviewItem(
            review = BookReview(
                id = "prev_rev_id_001",
                userId = "user_1234", // userId 추가됨
                reviewerName = "미리보기 리뷰어",
                reviewText = "이것은 reviewText 필드의 내용입니다.", // reviewText에 값 전달
                rating = 4.8f,
                content = "이것은 content 필드의 내용입니다.",   // content에 값 전달!
                // timestamp는 기본값이 있으므로 생략 가능
            )
        )
    }
}
