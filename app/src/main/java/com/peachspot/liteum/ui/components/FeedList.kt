package com.peachspot.liteum.ui.components // FeedList.kt가 있는 패키지

import android.util.Log
import androidx.activity.result.launch
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
import com.peachspot.liteum.data.model.BookReview
import com.peachspot.liteum.data.model.FeedItem
import com.peachspot.liteum.viewmodel.FeedViewModel

@Composable
fun FeedList(
    feedItems: LazyPagingItems<FeedItem>,
    navController: NavController, // << NavController 파라미터 추가
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    onItemClick: (FeedItem?) -> Unit,
    onEditClickCallback: (feedItem: FeedItem, review: BookReview?) -> Unit,
    onDeleteClickCallback: (feedItem: FeedItem, review: BookReview?) -> Unit,
    feedViewModel: FeedViewModel
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
    contentPadding = PaddingValues(vertical = 0.dp)
) {
    items(
        count = feedItems.itemCount,
        key = feedItems.itemKey { it.id ?: java.util.UUID.randomUUID().toString() } // null-safe
    ) { index ->
        val item = feedItems[index] // null 가능
        item?.let { feedItem ->
            // FeedPostItem에 필요한 콜백 전달, null-safe 처리
            FeedPostItem(
                feedItem = feedItem,
                onItemClick = { feedItem?.let { onItemClick(it) } },
                modifier = modifier,
                onEditClick = { reviewFromFeedPost ->
                    try {
                        onEditClickCallback(feedItem, reviewFromFeedPost)
                    } catch (e: Exception) {
                        Log.e("FeedList", "onEditClick error: ${e.localizedMessage}")
                    }
                },
                onDeleteClick = { reviewFromFeedPost ->
                    try {
                        onDeleteClickCallback(feedItem, reviewFromFeedPost)
                    } catch (e: Exception) {
                        Log.e("FeedList", "onDeleteClick error: ${e.localizedMessage}")
                    }
                },
                feedViewModel=feedViewModel
            )

            HorizontalDivider(
                thickness = DividerDefaults.Thickness,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
            )
        } ?: run {
            // Placeholder UI (item이 null일 때)
            Box(modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)) // 예시 높이
        }
    }

    // --- 로딩 상태 처리 (LoadState) ---
    feedItems.loadState.apply {
        when {
            refresh is LoadState.Loading -> {
                item {
                    LoadingIndicator(modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp))
                }
            }
            append is LoadState.Loading -> {
                item {
                    LoadingIndicator(modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp))
                }
            }
            refresh is LoadState.Error -> {
                val e = refresh as LoadState.Error
                item {
                    ErrorMessageItem(
                        message = e.error?.localizedMessage ?: "데이터를 불러오는데 실패했어요",
                        onRetry = { feedItems.retry() },
                        modifier = Modifier
                            .fillParentMaxSize()
                            .padding(16.dp)
                    )
                }
            }
            append is LoadState.Error -> {
                val e = append as LoadState.Error
                item {
                    ErrorMessageItem(
                        message = e.error?.localizedMessage ?: "더 많은 데이터를 불러오는데 실패했어요",
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
