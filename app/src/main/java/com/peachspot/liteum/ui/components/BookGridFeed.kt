// com/peachspot/liteum/ui/components/BookGridFeed.kt
package com.peachspot.liteum.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Button
// Paging 라이브러리의 items 확장 함수를 사용하기 위한 import (Grid 용)
// 정확한 import 경로는 다를 수 있으나, 일반적으로 LazyPagingItems 확장으로 제공됨
// import androidx.paging.compose.items // 또는 아래와 같이 사용import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.peachspot.liteum.data.model.FeedItem
import com.peachspot.liteum.viewmodel.FeedViewModel

@Composable
fun BookGridFeed(
    feedItems: LazyPagingItems<FeedItem>, // << 타입 변경: List<FeedItem> -> LazyPagingItems<FeedItem>
    onItemClick: (FeedItem?) -> Unit,     // << 콜백 파라미터 타입 변경: FeedItem -> FeedItem?
    modifier: Modifier = Modifier,
    columns: Int = 3 ,
    feedViewModel: FeedViewModel
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier.fillMaxSize(), // 그리드가 전체 크기를 채우도록
        contentPadding = PaddingValues(8.dp), // 전체 그리드의 패딩
        verticalArrangement = Arrangement.spacedBy(8.dp),   // 아이템 간 수직 간격
        horizontalArrangement = Arrangement.spacedBy(8.dp) // 아이템 간 수평 간격
    ) {
        // Paging 라이브러리의 items 확장 함수 사용
        items(
            count = feedItems.itemCount,
            key = feedItems.itemKey { it.id } // 아이템 고유 키 (Paging 3.1.0+), FeedItem에 id가 String 가정
            // contentType = { "feedItem" } // (선택 사항) 아이템 타입 명시
        ) { index ->
            val item = feedItems[index] // 아이템 가져오기 (null일 수 있음)
            // item이 null이 아닐 때만 실제 아이템 UI를 표시
            item?.let { feedItem ->
                BookCoverGridItem( // 이 컴포저블은 FeedItem을 표시하는 역할
                    feedItem = feedItem,
                    onItemClick = { onItemClick(feedItem) } ,// null이 아닌 item 전달
                    feedViewModel=feedViewModel
                )
            } ?: run {
                // (선택 사항) item이 null일 때 (예: 로딩 중인 플레이스홀더) 표시할 UI
                // PlaceholderBookCoverGridItem()
            }
        }

        // --- 로딩 상태에 따른 UI 처리 ---
        feedItems.loadState.apply {
            when {
                // 초기 로드 또는 새로고침 시 로딩
                refresh is LoadState.Loading -> {
                    // 전체 화면 로딩 인디케이터 (span을 사용하여 전체 너비 차지)
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(columns) }) {
                        LoadingIndicator(modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp))
                    }
                }
                // 목록 끝에 추가 로드 시 로딩
                append is LoadState.Loading -> {
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(columns) }) {
                        LoadingIndicator(modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp))
                    }
                }
                // 초기 로드 또는 새로고침 시 에러
                refresh is LoadState.Error -> {
                    val e = refresh as LoadState.Error
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(columns) }) {
                        ErrorMessageItem(
                            message = "데이터를 불러오는데 실패했어요: ${e.error.localizedMessage}",
                            onRetry = { feedItems.retry() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        )
                    }
                }
                // 목록 끝에 추가 로드 시 에러
                append is LoadState.Error -> {
                    val e = append as LoadState.Error
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(columns) }) {
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

