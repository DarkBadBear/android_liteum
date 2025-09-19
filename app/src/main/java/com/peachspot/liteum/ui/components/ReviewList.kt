package com.peachspot.liteum.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.peachspot.liteum.data.model.BookReview // 분리된 BookReview 사용

@Composable
fun ReviewList(
    bookTitle: String,
    reviews: List<BookReview>,
    modifier: Modifier = Modifier,
    ) {
    Column(modifier = modifier.background(MaterialTheme.colorScheme.surface)) {
        Text(
            text = "다른 생각들",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(12.dp)
        )
        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
        if (reviews.isEmpty()) {
            Box(Modifier
                .fillMaxSize()
                .padding(16.dp), contentAlignment = Alignment.Center) {
                Text("아직 작성된 리뷰가 없습니다.")
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(reviews, key = { it.id }) { review ->
                    BookReviewItem(review) // BookReviewItem도 분리된 파일에서 import
                }
            }
        }
    }
}
    