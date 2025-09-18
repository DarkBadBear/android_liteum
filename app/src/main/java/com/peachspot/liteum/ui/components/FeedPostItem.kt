package com.peachspot.liteum.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.peachspot.liteum.R // R 경로 확인 필요
import com.peachspot.liteum.data.model.BookReview
import com.peachspot.liteum.data.model.FeedItem
import com.peachspot.liteum.util.formatTimestamp // 분리된 유틸리티 함수 사용
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FeedPostItem(
    feedItem: FeedItem,
    onItemClick: (FeedItem) -> Unit,
    modifier: Modifier = Modifier,
){
    var showReviewDialog by remember { mutableStateOf(false) }
    val currentReview = feedItem.reviews.firstOrNull() // 예시로 첫 번째 리뷰를 사용, 실제로는 어떤 리뷰를 보여줄지 로직 필요

    val pagerState = rememberPagerState(pageCount = { 2 }) // 0: 책 이미지, 1: 리뷰 목록
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
//            AsyncImage(
//                model = feedItem.userProfileImageUrl ?: R.drawable.default_profile_placeholder, // 플레이스홀더 리소스 필요
//                contentDescription = "${feedItem.userName} 프로필 사진",
//                modifier = Modifier
//                    .size(32.dp)
//                    .clip(CircleShape)
//                    .background(Color.LightGray),
//                contentScale = ContentScale.Crop
//            )
            //Spacer(modifier = Modifier.width(8.dp))
//            Text(
//                text = feedItem.userName,
//                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
//                modifier = Modifier.weight(1f)
//            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { /* TODO: 더보기 메뉴 */ }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "더보기")
            }
        }

        Column {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) { page ->
                when (page) {
                    0 -> {
                        AsyncImage(
                            model = feedItem.bookImageUrl,
                            contentDescription = "${feedItem.bookTitle} 표지",
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { onItemClick(feedItem) },
                            contentScale = ContentScale.Crop
                        )
                    }
                    1 -> {
                        ReviewList( // ReviewList도 분리된 파일에서 import
                            bookTitle = feedItem.bookTitle,
                            reviews = feedItem.reviews,
                            modifier = Modifier.fillMaxSize(),

                        )
                    }
                }
            }

            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                Tab(
                    selected = pagerState.currentPage == 0,
                    onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                 //   text = { Text("책 표지") }
                    modifier = Modifier.height(4.dp) // 탭의 높이를 0으로 설정

                )
                Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                   // text = { Text("리뷰 (${feedItem.reviews.size})") }
                    modifier = Modifier.height(4.dp) // 탭의 높이를 0으로 설정

                )
            }
        }


        Row(modifier = Modifier.padding(horizontal = 12.dp)) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    ) {
                Row {
                    Text(
                        text = feedItem.bookTitle,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Row {
//                    Text(
//                        text = feedItem.caption, // 또는 currentReview?.content
//                        style = MaterialTheme.typography.bodyMedium,
//                        maxLines = 2,
//                        overflow = TextOverflow.Ellipsis
//                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))

//        if (feedItem.comments > 0) {
//            Text(
//                text = "댓글 ${feedItem.comments}개 모두 보기",
//                style = MaterialTheme.typography.bodySmall,
//                color = Color.Gray,
//                modifier = Modifier.padding(horizontal = 12.dp)
//            )
//            Spacer(modifier = Modifier.height(4.dp))
//        }

        Text(
            text = formatTimestamp(feedItem.timestamp),
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )

    }
    }


