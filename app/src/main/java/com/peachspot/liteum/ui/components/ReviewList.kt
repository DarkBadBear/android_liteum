package com.peachspot.liteum.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
// import androidx.compose.material.icons.Icons // Switch 사용 시 아이콘 불필요 시 제거
// import androidx.compose.material.icons.filled.Person // Switch 사용 시 아이콘 불필요 시 제거
import androidx.compose.material3.HorizontalDivider
// import androidx.compose.material3.Icon // Switch 사용 시 아이콘 불필요 시 제거
import androidx.compose.material3.MaterialTheme
// import androidx.compose.material3.OutlinedButton // OutlinedButton 대신 Switch 사용
import androidx.compose.material3.Switch // Switch 임포트
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.peachspot.liteum.data.model.BookReview
import com.peachspot.liteum.viewmodel.HomeViewModel

@Composable
fun ReviewList(
    bookTitle: String, // 사용되지 않는다면 제거 가능
    reviews: List<BookReview>,
    modifier: Modifier = Modifier,
    homeViewModel: HomeViewModel // HomeViewModel을 파라미터로 받음
) {
    // HomeViewModel에서 "내것만 보기" 상태와 인증 상태를 관찰
    val showOnlyMyReviews by homeViewModel.showOnlyMyReviews.collectAsState()
    val authUiState by homeViewModel.uiState.collectAsState() // uiState 관찰

    Column(modifier = modifier.background(MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(
                        alpha = 0.5f
                    )
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "리뷰",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f) // "리뷰" 텍스트가 남은 공간을 차지하도록
            )

            // "내것만 보기" 텍스트 레이블
            Text(
                text = "내것만 보기",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(end = 8.dp) // 스위치와의 간격
            )

            Switch(
                checked = showOnlyMyReviews,
                onCheckedChange = { isChecked ->
                    homeViewModel.setShowOnlyMyReviews(isChecked)
                }
                // SwitchDefaults.colors(...) 등을 사용하여 스위치 모양 커스터마이징 가능
            )
        }

        HorizontalDivider(thickness = Dp.Hairline)

        // 현재 사용자 ID 가져오기 (HomeViewModel의 uiState 사용)
        val currentUserId = if (authUiState.isUserLoggedIn) {
            authUiState.firebaseUid ?: authUiState.kakaoUid // Firebase UID 우선, 없으면 Kakao UID
        } else {
            null // 로그인하지 않은 경우
        }

        val displayedReviews = if (showOnlyMyReviews) {
            if (currentUserId != null) {
                reviews.filter { review ->
                    // BookReview 모델에 userId 필드가 있고, 현재 사용자의 ID와 일치하는지 확인
                    // 실제 BookReview 모델의 필드명에 맞게 'it.userId' 부분을 수정해야 할 수 있습니다.
                    review.userId == currentUserId
                }
            } else {
                // "내것만 보기"가 켜져 있지만 로그인하지 않은 경우 빈 리스트 표시
                emptyList()
            }
        } else {
            reviews
        }

        if (displayedReviews.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when {
                        showOnlyMyReviews && currentUserId == null -> "로그인이 필요합니다." // "내것만 보기" + 비로그인
                        showOnlyMyReviews && currentUserId != null -> "내가 작성한 리뷰가 없습니다." // "내것만 보기" + 로그인 + 리뷰 없음
                        else -> "아직 작성된 리뷰가 없습니다." // "모두보기" + 리뷰 없음
                    }
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(), // LazyColumn이 남은 공간을 채우도록
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(displayedReviews, key = { it.id }) { review ->
                    BookReviewItem(review) // BookReviewItem 컴포저블 호출
                }
            }
        }
    }
}

// 만약 BookReviewItem이 아직 없다면, 임시로 아래와 같이 정의할 수 있습니다.
// BookReview 모델에 userId 필드가 있다고 가정합니다. (실제 모델에 맞게 수정 필요)
@Composable
fun BookReviewItem(review: BookReview) {
    // 간단한 리뷰 항목 UI 예시
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // 예시: 리뷰 내용과 함께 작성자 ID (실제로는 ID 대신 닉네임 등을 표시)
        Text(text = "작성자 ID: ${review.userId ?: "알 수 없음"}", style = MaterialTheme.typography.labelSmall)
        Text(text = "리뷰 ID (DB): ${review.id}", style = MaterialTheme.typography.labelSmall)
        Text(text = review.content, style = MaterialTheme.typography.bodyMedium)
        // 여기에 평점, 작성자 정보 등 추가 가능
    }
}

