package com.peachspot.liteum.ui.components // FeedList.kt가 있는 패키지

import androidx.compose.foundation.clickable // 클릭 가능한 Modifier를 위해
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
// Paging 라이브러리의 items 확장 함수를 사용하기 위한 import
// import androidx.paging.compose.items // 또는 아래와 같이 사용
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
// NavController는 FeedList에서 직접 사용하지 않으므로 제거 가능 (필요시 유지)
// import androidx.navigation.NavController
// BookReview는 FeedPostItem 내부에서 사용될 수 있으나, FeedList 시그니처에서는 직접 필요 X
// import com.peachspot.liteum.data.model.BookReview
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey // itemKey를 위한 import
import com.peachspot.liteum.data.model.FeedItem

@Composable
fun FeedList(
    feedItems: LazyPagingItems<FeedItem>,
    navController: NavController, // << NavController 파라미터 추가
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    onItemClick: (FeedItem?) -> Unit
) {
    // 로딩 완료 후 아이템이 없는지 확인하는 조건
    val showEmptyState = feedItems.loadState.refresh is LoadState.NotLoading &&
            feedItems.loadState.append.endOfPaginationReached &&
            feedItems.itemCount == 0

    if (showEmptyState) {
        // 아이템이 없을 때 메시지 표시
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center // Box 자체는 중앙 정렬
        ) {
            Column( // << Column을 사용하여 내부 요소들을 수직 정렬
                horizontalAlignment = Alignment.CenterHorizontally // Column 내부 아이템들 수평 중앙 정렬
            ) {
                Text(
                    text = "등록된 도서가 없습니다.",
                    style = MaterialTheme.typography.bodyLarge,textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp)) // << 간격 조절 (36dp는 좀 클 수 있습니다)
                Button(onClick = { navController.navigate("review") }) {
                    Text("새로운 책 등록하기")
                }
            }
        }

    } else {
        // 아이템이 있거나 로딩 중일 때 리스트 표시
        LazyColumn(
            modifier = modifier.fillMaxSize(), // 리스트가 전체 크기를 채우도록
            state = listState,
            contentPadding = PaddingValues(vertical = 0.dp) // 전체 리스트 패딩 (아이템별 패딩과 구분)
        ) {
            // Paging 라이브러리의 items 확장 함수 사용
            items(
                count = feedItems.itemCount,
                key = feedItems.itemKey { it.id }, // 아이템 고유 키, FeedItem에 id가 String 가정
                // contentType = { "feedPostItem" } // (선택 사항) 아이템 타입 명시
            ) { index ->
                val item = feedItems[index] // 아이템 가져오기 (null일 수 있음)
                // item이 null이 아닐 때만 실제 아이템 UI를 표시
                item?.let { feedItem ->
                    // FeedPostItem에 필요한 콜백들 전달
                    // FeedPostItem의 onItemClick은 이제 FeedItem 객체를 직접 받을 수 있도록 수정하는 것이 좋음
                    FeedPostItem(
                        feedItem = feedItem,
                        onItemClick = { onItemClick(feedItem) },
                        modifier = modifier
                    )
                    HorizontalDivider(
                        // modifier = Modifier.padding(horizontal = 16.dp) // 구분선에 좌우 패딩 추가
                        thickness = DividerDefaults.Thickness,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f) // 구분선 색상 약간 연하게
                    )
                } ?: run {
                    // (선택 사항) item이 null일 때 (예: 로딩 중인 플레이스홀더) 표시할 UI
                    // PlaceholderFeedPostItem()
                }
            }

            // --- 로딩 상태에 따른 UI 처리 ---
            feedItems.loadState.apply {
                when {
                    // 초기 로드 또는 새로고침 시 로딩
                    refresh is LoadState.Loading -> {
                        item {
                            LoadingIndicator(modifier = Modifier
                                .fillMaxWidth() // 현재 아이템의 너비를 채움
                                .padding(16.dp))
                        }
                    }
                    // 목록 끝에 추가 로드 시 로딩
                    append is LoadState.Loading -> {
                        item {
                            LoadingIndicator(modifier = Modifier
                                .fillMaxWidth() // 현재 아이템의 너비를 채움
                                .padding(16.dp))
                        }
                    }
                    // 초기 로드 또는 새로고침 시 에러
                    refresh is LoadState.Error -> {
                        val e = refresh as LoadState.Error
                        item {
                            ErrorMessageItem(
                                message = "데이터를 불러오는데 실패했어요: ${e.error.localizedMessage}",
                                onRetry = { feedItems.retry() },
                                modifier = Modifier
                                    .fillParentMaxSize()
                                    .padding(16.dp)
                            )
                        }
                    }
                    // 목록 끝에 추가 로드 시 에러
                    append is LoadState.Error -> {
                        val e = append as LoadState.Error
                        item {
                            ErrorMessageItem(
                                message = "더 많은 데이터를 불러오는데 실패했어요: ${e.error.localizedMessage}",
                                onRetry = { feedItems.retry() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// FeedPostItem, LoadingIndicator, ErrorMessageItem 컴포저블은 이 파일 내에 정의되어 있거나,
// 다른 파일에서 import 되어야 합니다. 아래는 예시 정의입니다.

@Composable
fun FeedPostItem(
    feedItem: FeedItem,
    onItemClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .clickable(onClick = onItemClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 예시: 사용자 프로필 이미지 (실제로는 AsyncImage 등 사용)
                // Box(modifier = Modifier.size(40.dp).background(Color.Gray, CircleShape))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = feedItem.userName, style = MaterialTheme.typography.titleSmall)
            }
            Spacer(modifier = Modifier.height(8.dp))
            // 예시: 책 이미지 (실제로는 AsyncImage 등 사용)
            // Box(modifier = Modifier.fillMaxWidth().height(150.dp).background(Color.LightGray))
            Text(text = feedItem.bookTitle, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = feedItem.caption, style = MaterialTheme.typography.bodyMedium, maxLines = 3) // 캡션 줄 수 제한 예시
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "좋아요 ${feedItem.likes}개", style = MaterialTheme.typography.bodySmall)
                if (feedItem.reviews.isNotEmpty()) {
                    Text(
                        text = "리뷰 ${feedItem.reviews.size}개",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { /* TODO: 리뷰 상세 보기 액션 */ }
                    )
                }
            }
        }
    }
}
